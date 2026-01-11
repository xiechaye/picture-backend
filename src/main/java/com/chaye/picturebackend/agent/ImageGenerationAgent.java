package com.chaye.picturebackend.agent;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.chaye.picturebackend.advisor.MyLoggerAdvisor;
import com.chaye.picturebackend.agent.config.ImageGenerationConfig;
import com.chaye.picturebackend.agent.context.ImageGenerationContext;
import com.chaye.picturebackend.agent.exception.ImageGenerationException;
import com.chaye.picturebackend.api.aliyunai.ImageGenerationApi;
import com.chaye.picturebackend.api.aliyunai.model.CreateImageTaskRequest;
import com.chaye.picturebackend.api.aliyunai.model.CreateImageTaskResponse;
import com.chaye.picturebackend.api.aliyunai.model.GetImageTaskResponse;
import com.chaye.picturebackend.manager.CosManager;
import com.chaye.picturebackend.model.enums.AgentEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * 图像生成 Agent（优化版）
 *
 * 功能：
 * 1. 将用户输入的简单描述优化为详细的图像生成 prompt
 * 2. 调用阿里云图像生成 API 生成图像
 * 3. 下载并上传图像到腾讯云 COS
 *
 * 优化点：
 * - 配置化：支持通过配置文件自定义参数
 * - 异常处理：详细的异常分类和错误信息
 * - 重试机制：支持指数退避的轮询策略
 * - 性能监控：记录各步骤执行时间
 * - 参数校验：验证 Prompt 质量
 * - 上下文管理：使用专用上下文对象传递数据
 */
@Slf4j
@Component
public class ImageGenerationAgent extends BaseAgent {

    private final ImageGenerationApi imageGenerationApi;
    private final CosManager cosManager;
    private final ChatClient chatClient;
    private final ImageGenerationConfig config;

    /**
     * 图像生成上下文（线程安全）
     */
    private ThreadLocal<ImageGenerationContext> contextHolder = new ThreadLocal<>();

    /**
     * 获取当前线程的ImageGenerationContext
     * @return 图像生成上下文
     */
    public ImageGenerationContext getContext() {
        return contextHolder.get();
    }

    public ImageGenerationAgent(ImageGenerationApi imageGenerationApi,
                                CosManager cosManager,
                                ChatModel dashscopeChatModel,
                                ImageGenerationConfig config) {
        this.imageGenerationApi = imageGenerationApi;
        this.cosManager = cosManager;
        this.config = config;

        // 初始化 Agent 属性
        this.setName("imageGenerationAgent");

        String SYSTEM_PROMPT = """
                You are an expert AI Image Prompt Engineer. Your job is to transform user's simple image descriptions
                into detailed, high-quality prompts optimized for AI image generation.

                Rules:
                1. Enhance the user's input with rich visual details (colors, lighting, composition, style)
                2. Keep the prompt concise but descriptive (50-150 words)
                3. Use artistic terminology when appropriate (e.g., "cinematic lighting", "bokeh effect", "vibrant colors")
                4. Consider composition elements (foreground, background, perspective)
                5. Specify image quality indicators (e.g., "high detail", "8K resolution", "professional photography")
                6. ONLY output the optimized prompt, no explanations or additional text

                Example:
                User input: "a cat"
                Your output: "A fluffy orange tabby cat with bright green eyes, sitting gracefully on a wooden windowsill.
                Soft golden hour sunlight streams through the window, creating a warm glow on its fur. The background shows
                a blurred garden with bokeh effect. Professional pet photography, high detail, shallow depth of field."

                Now, optimize the following description for image generation:
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        this.setMaxStep(3); // Prompt优化 -> 图像生成 -> 上传

        // 初始化 ChatClient
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(this.chatClient);
    }

    @Override
    public String step() {
        try {
            // 初始化上下文
            if (contextHolder.get() == null) {
                ImageGenerationContext context = new ImageGenerationContext();
                // 获取用户输入
                org.springframework.ai.chat.messages.Message lastMessage = getMessageList().get(getMessageList().size() - 1);
                if (lastMessage instanceof org.springframework.ai.chat.messages.UserMessage) {
                    context.setUserInput(((org.springframework.ai.chat.messages.UserMessage) lastMessage).getText());
                }
                contextHolder.set(context);
            }

            int currentStep = getCurrentStep();
            String result;

            if (currentStep == 1) {
                // 第一步：使用 AI 优化用户输入的 prompt
                result = optimizePrompt();
            } else if (currentStep == 2) {
                // 第二步：调用图像生成 API
                result = generateImage();
            } else if (currentStep == 3) {
                // 第三步：下载并上传图像到 COS
                result = uploadImageToCos();
            } else {
                setAgentEnum(AgentEnum.FINISHED);
                ImageGenerationContext context = contextHolder.get();
                result = String.format("图像生成完成！总耗时: %dms，COS路径: %s",
                        context.getTotalTime(), context.getCosKey());
            }

            return result;

        } catch (ImageGenerationException e) {
            log.error("图像生成失败 [{}]: {}", e.getErrorType(), e.getMessage(), e);
            setAgentEnum(AgentEnum.ERROR);
            ImageGenerationContext context = contextHolder.get();
            if (context != null) {
                context.setErrorMessage(e.getMessage());
            }
            return String.format("步骤执行失败 [%s]: %s", e.getErrorType(), e.getMessage());
        } catch (Exception e) {
            log.error("未知错误: " + e.getMessage(), e);
            setAgentEnum(AgentEnum.ERROR);
            return "步骤执行失败: " + e.getMessage();
        }
    }

    /**
     * 使用 ChatModel 优化用户输入的 prompt
     * 优化点：
     * - 添加参数校验
     * - 记录执行时间
     * - 验证优化结果质量
     */
    private String optimizePrompt() {
        long startTime = System.currentTimeMillis();
        log.info("正在优化用户输入的 prompt...");

        ImageGenerationContext context = contextHolder.get();
        String userInput = context.getUserInput();

        // 参数校验
        validatePromptInput(userInput);

        try {
            // 使用 ChatClient 调用 AI 优化 prompt
            String optimizedPrompt = chatClient.prompt()
                    .system(getSystemPrompt())
                    .user(userInput)
                    .call()
                    .content();

            // 验证优化结果
            validateOptimizedPrompt(optimizedPrompt);

            long elapsedTime = System.currentTimeMillis() - startTime;
            context.setOptimizedPrompt(optimizedPrompt);
            context.setPromptOptimizationTime(elapsedTime);

            log.info("Prompt 优化完成，耗时: {}ms，长度: {} -> {}",
                    elapsedTime, userInput.length(), optimizedPrompt.length());
            log.info("优化后的 prompt: {}", optimizedPrompt);

            // 存储到消息列表供下一步使用
            getMessageList().add(new org.springframework.ai.chat.messages.AssistantMessage(optimizedPrompt));

            return String.format("✓ Prompt 优化完成（耗时 %dms）\n优化结果: %s",
                    elapsedTime, truncatePrompt(optimizedPrompt, 200));

        } catch (Exception e) {
            throw new ImageGenerationException(
                    ImageGenerationException.ErrorType.PROMPT_OPTIMIZATION_FAILED,
                    "Prompt 优化失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 调用阿里云图像生成 API（优化版）
     * 优化点：
     * - 使用配置化参数
     * - 指数退避重试策略
     * - 详细错误处理
     * - 性能监控
     */
    private String generateImage() {
        long startTime = System.currentTimeMillis();
        log.info("正在生成图像...");

        ImageGenerationContext context = contextHolder.get();
        String optimizedPrompt = context.getOptimizedPrompt();

        try {
            // 构建请求
            CreateImageTaskRequest request = new CreateImageTaskRequest();
            request.setModel(config.getDefaultModel());

            CreateImageTaskRequest.Input input = new CreateImageTaskRequest.Input();
            input.setPrompt(optimizedPrompt);
            request.setInput(input);

            CreateImageTaskRequest.Parameters parameters = new CreateImageTaskRequest.Parameters();
            parameters.setSize(config.getDefaultSize());
            parameters.setN(config.getDefaultImageCount());
            request.setParameters(parameters);

            // 创建任务
            CreateImageTaskResponse createResponse;
            try {
                createResponse = imageGenerationApi.createImageTask(request);
            } catch (Exception e) {
                throw new ImageGenerationException(
                        ImageGenerationException.ErrorType.TASK_CREATION_FAILED,
                        "创建图像生成任务失败: " + e.getMessage(),
                        e
                );
            }

            String taskId = createResponse.getOutput().getTaskId();
            context.setTaskId(taskId);
            log.info("图像生成任务已创建，任务ID: {}", taskId);

            // 轮询查询任务状态（支持指数退避）
            GetImageTaskResponse taskResponse = pollTaskStatus(taskId);

            // 获取生成的图像 URL
            List<GetImageTaskResponse.ImageResult> results = taskResponse.getOutput().getResults();
            if (results == null || results.isEmpty()) {
                throw new ImageGenerationException(
                        ImageGenerationException.ErrorType.TASK_FAILED,
                        "未能获取生成的图像"
                );
            }

            String imageUrl = results.get(0).getUrl();
            context.setImageUrl(imageUrl);

            long elapsedTime = System.currentTimeMillis() - startTime;
            context.setImageGenerationTime(elapsedTime);

            log.info("图像生成成功（耗时 {}ms），URL: {}", elapsedTime, imageUrl);

            return String.format("✓ 图像生成成功（耗时 %dms）", elapsedTime);

        } catch (ImageGenerationException e) {
            throw e; // 直接抛出已分类的异常
        } catch (Exception e) {
            throw new ImageGenerationException(
                    ImageGenerationException.ErrorType.API_CALL_FAILED,
                    "图像生成失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 轮询查询任务状态（支持指数退避策略）
     */
    private GetImageTaskResponse pollTaskStatus(String taskId) {
        int maxRetries = config.getMaxPollingRetries();
        long currentInterval = config.getInitialPollingInterval();
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                GetImageTaskResponse taskResponse = imageGenerationApi.getImageTask(taskId);
                String taskStatus = taskResponse.getOutput().getTaskStatus();

                log.info("任务状态: {} (第 {} 次查询)", taskStatus, retryCount + 1);

                if ("SUCCEEDED".equals(taskStatus)) {
                    return taskResponse;
                } else if ("FAILED".equals(taskStatus)) {
                    String errorMessage = taskResponse.getOutput().getMessage();
                    throw new ImageGenerationException(
                            ImageGenerationException.ErrorType.TASK_FAILED,
                            "图像生成任务失败: " + errorMessage
                    );
                }

                // 等待后重试
                Thread.sleep(currentInterval);

                // 指数退避策略
                if (config.getUseExponentialBackoff()) {
                    currentInterval = Math.min(
                            currentInterval * 2,
                            config.getMaxPollingInterval()
                    );
                }

                retryCount++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ImageGenerationException(
                        ImageGenerationException.ErrorType.TASK_TIMEOUT,
                        "任务查询被中断"
                );
            } catch (ImageGenerationException e) {
                throw e; // 直接抛出已分类的异常
            } catch (Exception e) {
                throw new ImageGenerationException(
                        ImageGenerationException.ErrorType.API_CALL_FAILED,
                        "查询任务状态失败: " + e.getMessage(),
                        e
                );
            }
        }

        throw new ImageGenerationException(
                ImageGenerationException.ErrorType.TASK_TIMEOUT,
                String.format("图像生成超时（已重试 %d 次）", maxRetries)
        );
    }

    /**
     * 下载图像并上传到 COS
     */
    private String uploadImageToCos() {
        long startTime = System.currentTimeMillis();
        log.info("正在上传图像到 COS...");

        ImageGenerationContext context = contextHolder.get();
        String imageUrl = context.getImageUrl();

        File tempFile = null;
        try {
            // 下载图像到临时文件
            tempFile = File.createTempFile("ai_generated_", ".png");
            HttpUtil.downloadFile(imageUrl, tempFile);
            log.info("图像已下载到临时文件: {}", tempFile.getAbsolutePath());

            // 生成唯一的文件名
            String fileName = "ai_generated_" + UUID.randomUUID() + ".png";
            String cosKey = "generated/" + fileName;

            // 上传到 COS
            cosManager.putPictureObject(cosKey, tempFile);
            log.info("图像已上传到 COS: {}", cosKey);

            long elapsedTime = System.currentTimeMillis() - startTime;
            context.setCosKey(cosKey);
            context.setUploadTime(elapsedTime);

            setAgentEnum(AgentEnum.FINISHED);
            return String.format("✓ 图像上传完成（耗时 %dms）\n文件路径: %s", elapsedTime, cosKey);

        } catch (Exception e) {
            log.error("上传图像到 COS 失败: " + e.getMessage(), e);
            setAgentEnum(AgentEnum.ERROR);
            throw new ImageGenerationException(
                    ImageGenerationException.ErrorType.IMAGE_UPLOAD_FAILED,
                    "上传图像到 COS 失败: " + e.getMessage(),
                    e
            );
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                FileUtil.del(tempFile);
            }
        }
    }

    /**
     * 验证用户输入的 Prompt
     */
    private void validatePromptInput(String userInput) {
        if (StringUtils.isBlank(userInput)) {
            throw new ImageGenerationException(
                    ImageGenerationException.ErrorType.VALIDATION_FAILED,
                    "用户输入不能为空"
            );
        }
        if (userInput.length() > 1000) {
            throw new ImageGenerationException(
                    ImageGenerationException.ErrorType.VALIDATION_FAILED,
                    "用户输入过长，最多1000个字符"
            );
        }
    }

    /**
     * 验证优化后的 Prompt
     */
    private void validateOptimizedPrompt(String optimizedPrompt) {
        if (StringUtils.isBlank(optimizedPrompt)) {
            throw new ImageGenerationException(
                    ImageGenerationException.ErrorType.PROMPT_OPTIMIZATION_FAILED,
                    "优化后的 Prompt 不能为空"
            );
        }
        if (optimizedPrompt.length() < 10) {
            throw new ImageGenerationException(
                    ImageGenerationException.ErrorType.PROMPT_OPTIMIZATION_FAILED,
                    "优化后的 Prompt 过短，可能优化失败"
            );
        }
    }

    /**
     * 截断 Prompt 用于日志输出
     */
    private String truncatePrompt(String prompt, int maxLength) {
        if (prompt == null) {
            return "";
        }
        if (prompt.length() <= maxLength) {
            return prompt;
        }
        return prompt.substring(0, maxLength) + "...";
    }

    /**
     * 清理资源（重写父类方法）
     */
    @Override
    protected void cleanup() {
        // 调用父类清理
        super.cleanup();

        // 不在这里清理 contextHolder，因为需要在外部获取 context 之后再清理
        log.debug("ImageGenerationAgent cleanup completed (contextHolder not removed)");
    }

    /**
     * 手动清理 ThreadLocal 上下文
     * 必须在获取 context 之后调用，防止内存泄漏
     */
    public void cleanupContext() {
        contextHolder.remove();
        log.debug("ImageGenerationAgent context cleaned up");
    }
}
