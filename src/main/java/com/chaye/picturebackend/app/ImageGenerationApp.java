package com.chaye.picturebackend.app;

import com.chaye.picturebackend.advisor.MyLoggerAdvisor;
import com.chaye.picturebackend.advisor.ReReadingAdvisor;
import com.chaye.picturebackend.agent.ImageGenerationAgent;
import com.chaye.picturebackend.agent.context.ImageGenerationContext;
import com.chaye.picturebackend.tools.ToolNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 图像生成应用
 */
@Component
@Slf4j
public class ImageGenerationApp {

    private final ChatClient chatClient;

    /**
     * ImageGenerationAgent 提供者，用于获取原型作用域的 Agent 实例
     * 每次调用 getObject() 都会创建新实例，避免并发问题
     */
    private final ObjectProvider<ImageGenerationAgent> agentProvider;


    private static final String SYSTEM_PROMPT = "You are a professional AI image generation assistant. Your task is to help users generate high-quality images. " +
            "When users describe the images they want to generate, you will: " +
            "1. Understand the user's needs and intentions " +
            "2. Optimize the image description prompt by adding more details (such as colors, lighting, composition, style, etc.) " +
            "3. Call the image generation service to create images " +
            "4. Return the generated image URL and related information";

    public ImageGenerationApp(ChatModel dashscopeChatModel,
                               ObjectProvider<ImageGenerationAgent> agentProvider) {
        this.agentProvider = agentProvider;

        // 初始化基于文件的聊天记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        // 基于文件的持久化存储
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 初始化基于内存的聊天记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 默认基于文件的持久化存储
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志顾问
                        new MyLoggerAdvisor(),
                        // 自定义AI推理增强顾问
                        new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * 与AI进行对话
     * @param chatId 会话ID
     * @param message 用户输入的消息
     * @return 流式响应
     */
    public Flux<String> doChat(String chatId, String message) {

        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * Agent 参数优化结果
     */
    public record ParameterOptimizationResult(
            String optimizedPrompt,      // 优化后的 Prompt (必有)
            String recommendedSize,       // 推荐尺寸 "width,height" (可选)
            String negativePrompt,        // 负面提示词 (可选)
            long optimizationTime         // 优化耗时 (毫秒)
    ) {}

    /**
     * 图像生成结果记录（已废弃，使用 ParameterOptimizationResult）
     * @deprecated 使用 ParameterOptimizationResult 代替
     */
    @Deprecated
    public record ImageGenerationResult(String imageUrl, String cosKey, String optimizedPrompt, long totalTime){}

    /**
     * 通过 ImageGenerationAgent 优化参数
     *
     * @param userPrompt 用户描述
     * @return 优化后的参数
     */
    public ParameterOptimizationResult optimizeParameters(String userPrompt) {
        long startTime = System.currentTimeMillis();

        // 每次调用获取新的 Agent 实例，避免并发问题
        ImageGenerationAgent agent = agentProvider.getObject();

        try {
            // 收集 Agent 执行的所有步骤输出
            java.util.List<String> outputs = new java.util.ArrayList<>();
            agent.run(userPrompt)
                    .doOnNext(outputs::add)
                    .blockLast();

            // 检查是否发生错误
            if (agent.getAgentEnum() == com.chaye.picturebackend.model.enums.AgentEnum.ERROR) {
                log.warn("Agent 优化失败，使用原始 Prompt");
                return new ParameterOptimizationResult(
                        userPrompt,  // 使用原始输入
                        null,        // 无推荐尺寸
                        null,        // 无负面提示词
                        0L
                );
            }

            // 从 Agent 的上下文中提取优化结果
            ImageGenerationContext context = extractOptimizationContext(agent);

            long optimizationTime = System.currentTimeMillis() - startTime;

            // 如果没有优化 Prompt，使用原始输入
            String finalPrompt = context.getOptimizedPrompt() != null
                    ? context.getOptimizedPrompt()
                    : userPrompt;

            log.info("参数优化完成，耗时: {}ms, hasOptimizedPrompt: {}, hasSize: {}, hasNegative: {}",
                    optimizationTime,
                    context.getOptimizedPrompt() != null,
                    context.getRecommendedSize() != null,
                    context.getNegativePrompt() != null);

            return new ParameterOptimizationResult(
                    finalPrompt,
                    context.getRecommendedSize(),
                    context.getNegativePrompt(),
                    optimizationTime
            );

        } catch (Exception e) {
            log.error("参数优化异常: {}", e.getMessage(), e);
            // 降级：返回原始输入
            return new ParameterOptimizationResult(userPrompt, null, null, 0L);
        }
    }

    /**
     * 从 Agent 的 messageList 中提取优化上下文
     *
     * @param agent Agent 实例
     * @return 优化上下文
     */
    private ImageGenerationContext extractOptimizationContext(ImageGenerationAgent agent) {
        ImageGenerationContext context = new ImageGenerationContext();

        // 从 Agent 的 messageList 中获取 ToolResponseMessage
        java.util.List<Message> messageList = agent.getMessageList();

        for (Message message : messageList) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (var response : toolResponseMessage.getResponses()) {
                    String toolName = response.name();
                    String toolOutput = response.responseData();  // 工具返回值

                    switch (toolName) {
                        case ToolNames.ENHANCE_PROMPT:
                            context.setOptimizedPrompt(toolOutput);
                            log.debug("提取到增强的 Prompt，长度: {}", toolOutput.length());
                            break;
                        case ToolNames.RECOMMEND_SIZE:
                            context.setRecommendedSize(toolOutput);
                            log.debug("提取到推荐尺寸: {}", toolOutput);
                            break;
                        case ToolNames.GENERATE_NEGATIVE_PROMPT:
                            context.setNegativePrompt(toolOutput);
                            log.debug("提取到负面提示词，长度: {}", toolOutput.length());
                            break;
                    }
                }
            }
        }

        return context;
    }

    /**
     * 通过ImageGenerationAgent生成图像并返回完整结果
     * @deprecated 使用 optimizeParameters() 代替，图像生成逻辑已移到 Service 层
     * @param userPrompt 用户描述的图像内容
     * @return 图像生成结果
     */
    @Deprecated
    public ImageGenerationResult generateImageWithResult(String userPrompt) {
        // 每次调用获取新的 Agent 实例
        ImageGenerationAgent agent = agentProvider.getObject();

        try {
            // 收集 Agent 执行的所有步骤输出
            java.util.List<String> outputs = new java.util.ArrayList<>();
            agent.run(userPrompt)
                    .doOnNext(outputs::add)
                    .blockLast();

            // 检查是否发生错误
            if (agent.getAgentEnum() == com.chaye.picturebackend.model.enums.AgentEnum.ERROR) {
                throw new RuntimeException("Image generation failed");
            }

            // 从输出中解析结果（GenerateAndUploadImageTool 的返回值）
            String finalOutput = findGenerateAndUploadOutput(outputs);
            if (finalOutput == null || !finalOutput.contains("✓ 图像生成并上传成功")) {
                throw new RuntimeException("Failed to find image generation result in outputs");
            }

            // 解析 COS 路径
            String cosKey = extractCosKey(finalOutput);
            if (cosKey == null) {
                throw new RuntimeException("Failed to extract COS key from output");
            }

            // 构建返回结果（注意：由于简化架构，不再返回 imageUrl 和 optimizedPrompt）
            // 可以通过 COS 路径构建 URL，或者让工具返回更多信息
            ImageGenerationResult result = new ImageGenerationResult(
                    null,  // imageUrl - 如需要可以通过 COS 构建
                    cosKey,
                    userPrompt,  // 使用原始输入作为 prompt
                    extractTotalTime(finalOutput)
            );

            log.info("Image generation completed successfully, cosKey: {}", cosKey);
            return result;

        } catch (Exception e) {
            log.error("Image generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Image generation failed", e);
        }
    }

    /**
     * 从输出列表中查找 GenerateAndUploadImageTool 的返回值
     */
    private String findGenerateAndUploadOutput(java.util.List<String> outputs) {
        for (String output : outputs) {
            if (output.contains("✓ 图像生成并上传成功")) {
                return output;
            }
        }
        return null;
    }

    /**
     * 从输出中提取 COS 路径
     */
    private String extractCosKey(String output) {
        // 格式："COS 路径: generated/ai_generated_xxx.png"
        String[] lines = output.split("\\n");
        for (String line : lines) {
            if (line.trim().startsWith("COS 路径:")) {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }
        return null;
    }

    /**
     * 从输出中提取总耗时
     */
    private long extractTotalTime(String output) {
        // 格式："耗时: 15234ms"
        String[] lines = output.split("\\n");
        for (String line : lines) {
            if (line.trim().startsWith("耗时:")) {
                String timeStr = line.substring(line.indexOf(":") + 1).trim();
                timeStr = timeStr.replace("ms", "").trim();
                try {
                    return Long.parseLong(timeStr);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse total time: {}", timeStr);
                    return 0;
                }
            }
        }
        return 0;
    }


    /**
     * 使用ChatClient优化图像描述（不生成图像）
     * @param chatId 会话ID
     * @param message 用户输入的图像描述
     * @return 优化后的图像描述prompt流式响应
     */
    public Flux<String> optimizeImagePrompt(String chatId, String message) {
        // 改进后的提示词：让AI识别输入语言并保持输出语言一致
        String optimizationPrompt = "You are an AI image generation prompt optimizer. " +
                "Please optimize the following image description into a more detailed and vivid prompt. " +
                "Add rich visual details such as colors, lighting, composition, style, atmosphere, etc. " +
                "\n\n**IMPORTANT**: " +
                "- If the input is in Chinese (中文), respond ONLY in Chinese. " +
                "- If the input is in English, respond ONLY in English. " +
                "- Maintain the same language as the input throughout your response." +
                "\n\nUser's description:\n" + message;

        return chatClient.prompt()
                .user(optimizationPrompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }
}
