package com.chaye.picturebackend.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.chaye.picturebackend.agent.ImagePromptOptimizerManus;
import com.chaye.picturebackend.agent.config.ImageGenerationConfig;
import com.chaye.picturebackend.api.aliyunai.ImageGenerationApi;
import com.chaye.picturebackend.api.aliyunai.model.CreateImageTaskRequest;
import com.chaye.picturebackend.api.aliyunai.model.CreateImageTaskResponse;
import com.chaye.picturebackend.api.aliyunai.model.GetImageTaskResponse;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.exception.ThrowUtils;
import com.chaye.picturebackend.manager.CosManager;
import com.chaye.picturebackend.manager.auth.SpaceUserAuthManager;
import com.chaye.picturebackend.model.dto.imagegeneration.GenerateImageRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.ImageGenerationResponse;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptResponse;
import com.chaye.picturebackend.model.entity.Space;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.service.ImageGenerationService;
import com.chaye.picturebackend.service.SpaceService;
import com.chaye.picturebackend.tools.AspectRatioTool;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * 图像生成服务
 */
@Service
@Slf4j
@AllArgsConstructor
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private final ChatModel dashscopeChatModel;

    private final ToolCallback[] allTools;

    private final SpaceService spaceService;

    private final SpaceUserAuthManager spaceUserAuthManager;

    private final ImageGenerationApi imageGenerationApi;  // 阿里云图像生成 API

    private final CosManager cosManager;  // 腾讯云 COS 管理器

    private final ImageGenerationConfig config;  // 配置类

    /**
     * 同步生成图像
     *
     * @param request   生成请求
     * @param loginUser 登录用户
     * @return 生成结果
     */
    public ImageGenerationResponse generateImage(GenerateImageRequest request, User loginUser) {
        long totalStartTime = System.currentTimeMillis();

        // 1. 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(request.getPrompt()),
                ErrorCode.PARAMS_ERROR, "图像描述不能为空");
        ThrowUtils.throwIf(request.getSpaceId() == null,
                ErrorCode.PARAMS_ERROR, "空间ID不能为空");

        // 2. 校验空间权限
        validateSpaceAccess(request.getSpaceId(), loginUser);

        log.info("用户 {} 开始同步生成图像，spaceId={}, prompt={}",
                loginUser.getId(), request.getSpaceId(), request.getPrompt());

        //  移除 Agent 优化，直接使用用户提供的参数

        // 构建最终 Prompt（如果用户提供了负面提示词，则合并）
        String finalPrompt = request.getPrompt();
        if (StringUtils.isNotBlank(request.getNegativePrompt())) {
            finalPrompt = buildFinalPrompt(request.getPrompt(), request.getNegativePrompt());
        }

        // 使用用户提供的尺寸，如果没有则使用默认尺寸
        String size = StringUtils.isNotBlank(request.getSize())
                ? request.getSize()
                : null;

        // 创建图像生成任务
        CreateImageTaskResponse createResponse = createImageGenerationTask(finalPrompt, size);
        String taskId = createResponse.getOutput().getTaskId();

        log.info("图像生成任务创建成功，taskId: {}", taskId);

        // 轮询任务状态
        GetImageTaskResponse taskResponse = pollTaskStatus(taskId);
        List<GetImageTaskResponse.ImageResult> results = taskResponse.getOutput().getResults();

        ThrowUtils.throwIf(results == null || results.isEmpty(),
                ErrorCode.OPERATION_ERROR, "图像生成失败：未返回结果");

        String imageUrl = results.get(0).getUrl();
        log.info("图像生成成功，imageUrl: {}", imageUrl);

        // 下载并上传到 COS
        String cosKey = downloadAndUploadImage(imageUrl);

        long totalTime = System.currentTimeMillis() - totalStartTime;

        log.info("用户 {} 图像生成完成，总耗时: {}ms, cosKey: {}",
                loginUser.getId(), totalTime, cosKey);

        // 构建响应
        ImageGenerationResponse response = new ImageGenerationResponse();
        response.setImageUrl(imageUrl);
        response.setCosKey(cosKey);
        response.setOptimizedPrompt(request.getPrompt()); // 返回用户使用的 prompt
        response.setTotalTime(totalTime);
        response.setSpaceId(request.getSpaceId());

        return response;
    }

    /**
     * 优化Prompt
     *
     * @param request 优化请求
     * @return 优化结果
     */
    public OptimizePromptResponse optimizePrompt(OptimizePromptRequest request) {
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(request.getPrompt()),
                ErrorCode.PARAMS_ERROR, "图像描述不能为空");

        log.info("开始优化Prompt: {}", request.getPrompt());

        long startTime = System.currentTimeMillis();

        // 创建响应对象
        OptimizePromptResponse response = new OptimizePromptResponse();
        response.setOriginalPrompt(request.getPrompt());

        try {
            // 创建 Agent 并执行优化
            ImagePromptOptimizerManus imagePromptOptimizerManus = new ImagePromptOptimizerManus(dashscopeChatModel, allTools);
            String runResult = imagePromptOptimizerManus.run(request.getPrompt());  // 执行优化流程

            // 检查 Agent 执行结果是否包含错误
            if (runResult != null && runResult.startsWith("执行错误")) {
                log.error("Agent执行失败: {}", runResult);
                response.setSuccess(false);
                response.setErrorMessage("Prompt优化失败: " + runResult);
                response.setOptimizedPrompt(request.getPrompt());  // 使用原始输入作为回退
                return response;
            }

            // 提取所有优化结果
            String optimizedPrompt = imagePromptOptimizerManus.getOptimizedPrompt();
            String recommendedSize = imagePromptOptimizerManus.getRecommendedSize();
            String negativePrompt = imagePromptOptimizerManus.getNegativePrompt();

            // 检查优化结果是否包含错误信息（工具调用可能返回错误字符串）
            if (optimizedPrompt != null && optimizedPrompt.startsWith("Error")) {
                log.warn("Prompt优化工具返回错误: {}", optimizedPrompt);
                response.setSuccess(false);
                response.setErrorMessage(optimizedPrompt);
                response.setOptimizedPrompt(request.getPrompt());  // 使用原始输入作为回退
                return response;
            }

            // 如果没有获取到优化结果，使用原始输入
            if (optimizedPrompt == null || optimizedPrompt.isEmpty()) {
                log.warn("未获取到优化结果，使用原始输入");
                optimizedPrompt = request.getPrompt();
            }

            long optimizationTime = System.currentTimeMillis() - startTime;

            // 设置响应数据
            response.setSuccess(true);
            response.setOptimizedPrompt(optimizedPrompt);
            response.setRecommendedSize(recommendedSize);
            response.setNegativePrompt(negativePrompt);

            log.info("Prompt优化完成，长度: {} -> {}, 耗时: {}ms",
                    request.getPrompt().length(),
                    optimizedPrompt.length(),
                    optimizationTime);

            // 输出详细的优化结果
            if (recommendedSize != null) {
                log.info("推荐尺寸: {}", recommendedSize);
            }
            if (negativePrompt != null) {
                log.info("负面提示词: {}", negativePrompt);
            }

            return response;

        } catch (Exception e) {
            log.error("Prompt优化过程发生异常", e);
            response.setSuccess(false);
            response.setErrorMessage("Prompt优化失败: " + e.getMessage());
            response.setOptimizedPrompt(request.getPrompt());  // 使用原始输入作为回退
            return response;
        }
    }

    /**
     * 校验空间权限
     *
     * @param spaceId   空间ID
     * @param loginUser 登录用户
     */
    private void validateSpaceAccess(Long spaceId, User loginUser) {
        // 1. 检查空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 2. 通过SpaceUserAuthManager获取权限列表
        java.util.List<String> permissions = spaceUserAuthManager.getPermissionList(space, loginUser);

        // 3. 权限列表为空表示无权限访问
        ThrowUtils.throwIf(permissions.isEmpty(), ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");

        log.info("用户{}通过空间{}权限校验", loginUser.getId(), spaceId);
    }

    /**
     * 构建最终 Prompt（合并负面提示词）
     *
     * @param prompt         优化后的 Prompt
     * @param negativePrompt 负面提示词（可为 null）
     * @return 最终 Prompt
     */
    private String buildFinalPrompt(String prompt, String negativePrompt) {
        if (negativePrompt != null && !negativePrompt.isBlank()) {
            // 使用配置的格式合并
            String format = config.getNegativePromptFormat();
            return format.replace("{prompt}", prompt)
                    .replace("{negative}", negativePrompt.trim());
        }
        return prompt;
    }

    /**
     * 创建图像生成任务
     *
     * @param finalPrompt     最终 Prompt
     * @param recommendedSize 推荐尺寸（格式："width,height"，可为 null）
     * @return 任务创建响应
     */
    private CreateImageTaskResponse createImageGenerationTask(String finalPrompt, String recommendedSize) {
        CreateImageTaskRequest request = new CreateImageTaskRequest();
        request.setModel(config.getDefaultModel());

        CreateImageTaskRequest.Input input = new CreateImageTaskRequest.Input();
        input.setPrompt(finalPrompt);
        request.setInput(input);

        CreateImageTaskRequest.Parameters parameters = new CreateImageTaskRequest.Parameters();

        // 处理尺寸参数
        String apiSize;
        if (recommendedSize != null && !recommendedSize.isBlank()) {
            // 转换格式：从 "width,height" 到 "width*height"
            apiSize = AspectRatioTool.convertToApiFormat(recommendedSize.trim());
            log.info("使用推荐尺寸: {}", apiSize);
        } else {
            apiSize = config.getDefaultSize();
            log.info("使用默认尺寸: {}", apiSize);
        }

        parameters.setSize(apiSize);
        parameters.setN(config.getDefaultImageCount());

        request.setParameters(parameters);

        return imageGenerationApi.createImageTask(request);
    }

    /**
     * 轮询查询任务状态（支持指数退避策略）
     *
     * @param taskId 任务 ID
     * @return 任务响应
     */
    private GetImageTaskResponse pollTaskStatus(String taskId) {
        int maxRetries = config.getMaxPollingRetries();
        long currentInterval = config.getInitialPollingInterval();
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                GetImageTaskResponse taskResponse = imageGenerationApi.getImageTask(taskId);
                String taskStatus = taskResponse.getOutput().getTaskStatus();

                log.info("任务状态: {} (执行 {}/{})", taskStatus, retryCount + 1, maxRetries);

                if ("SUCCEEDED".equals(taskStatus)) {
                    return taskResponse;
                } else if ("FAILED".equals(taskStatus)) {
                    String errorMessage = taskResponse.getOutput().getMessage();
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,
                            "图像生成任务失败: " + errorMessage);
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
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "轮询任务状态被中断");
            }
        }

        throw new BusinessException(ErrorCode.OPERATION_ERROR,
                String.format("图像生成超时（已重试 %d 次）", maxRetries));
    }

    /**
     * 下载图像并上传到 COS
     *
     * @param imageUrl 阿里云图像 URL
     * @return COS 存储路径
     */
    private String downloadAndUploadImage(String imageUrl) {
        File tempFile = null;
        try {
            // 下载图像到临时文件
            tempFile = File.createTempFile(
                    config.getTempFilePrefix(),
                    config.getTempFileSuffix()
            );
            HttpUtil.downloadFile(imageUrl, tempFile);

            log.info("图像已下载到临时文件: {}", tempFile.getAbsolutePath());

            // 生成唯一的文件名
            String fileName = config.getTempFilePrefix()
                    + UUID.randomUUID()
                    + config.getTempFileSuffix();
            String cosKey = config.getUploadPrefix() + fileName;

            // 上传到 COS
            cosManager.putPictureObject(cosKey, tempFile);

            log.info("图像已上传到 COS: {}", cosKey);

            return cosKey;

        } catch (Exception e) {
            log.error("下载或上传图像失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "下载或上传图像失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                FileUtil.del(tempFile);
                log.debug("临时文件已删除");
            }
        }
    }
}
