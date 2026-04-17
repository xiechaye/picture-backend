package com.chaye.picturebackend.api.imageEdit;

import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.chaye.picturebackend.api.aliyunai.model.Wan27ImageGenerationRequest;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云 wan2.7-image-pro 图像编辑 API（使用官方 SDK）
 * <p>
 * 使用 DashScope SDK 调用，避免 HTTP 请求格式问题
 */
@Slf4j
@Component
public class ImageEditApiWan27 {

    static {
        // 设置北京地域 URL
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
    }

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    /**
     * 使用 SDK 创建图像编辑任务
     *
     * @param request 编辑请求
     * @return SDK 原始响应
     */
    public ImageGenerationResult createTask(Wan27ImageGenerationRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图像编辑参数为空");
        }

        try {
            // 构建 SDK 参数
            ImageGenerationParam param = buildSdkParam(request);

            log.info("=== wan2.7-image-pro SDK 调用 ===");
            log.info("模型: {}", param.getModel());

            // 使用 SDK 调用
            ImageGeneration imageGeneration = new ImageGeneration();
            return imageGeneration.asyncCall(param);

        } catch (NoApiKeyException e) {
            log.error("API Key 未配置", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "API Key 未配置");
        } catch (ApiException e) {
            log.error("wan2.7-image-pro API 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像编辑失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("wan2.7-image-pro 调用异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像编辑失败");
        }
    }

    /**
     * 查询任务结果
     */
    public ImageGenerationResult getTaskResult(String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }

        try {
            ImageGeneration imageGeneration = new ImageGeneration();
            return imageGeneration.wait(taskId, apiKey);
        } catch (ApiException e) {
            log.error("查询任务状态失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
        } catch (Exception e) {
            log.error("查询任务状态异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
        }
    }

    /**
     * 从 SDK 响应中提取任务 ID
     */
    public String getTaskId(ImageGenerationResult result) {
        if (result != null && result.getOutput() != null) {
            return result.getOutput().getTaskId();
        }
        return null;
    }

    /**
     * 从 SDK 响应中提取任务状态
     */
    public String getTaskStatus(ImageGenerationResult result) {
        if (result != null && result.getOutput() != null) {
            return result.getOutput().getTaskStatus();
        }
        return null;
    }

    /**
     * 从 SDK 响应中提取首个图片 URL
     */
    public String getFirstImageUrl(ImageGenerationResult result) {
        if (result != null && result.getOutput() != null
                && result.getOutput().getChoices() != null
                && !result.getOutput().getChoices().isEmpty()) {

            var choice = result.getOutput().getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                for (Map<String, Object> contentItem : choice.getMessage().getContent()) {
                    if ("image".equals(contentItem.get("type"))) {
                        return (String) contentItem.get("image");
                    }
                }
            }
        }
        return null;
    }

    /**
     * 判断任务是否成功
     */
    public boolean isSuccessful(ImageGenerationResult result) {
        return result != null && result.getStatusCode() == 200;
    }

    /**
     * 判断任务是否失败
     */
    public boolean isFailed(ImageGenerationResult result) {
        String status = getTaskStatus(result);
        return "FAILED".equals(status);
    }

    /**
     * 判断任务是否完成
     */
    public boolean isFinished(ImageGenerationResult result) {
        String status = getTaskStatus(result);
        return "SUCCEEDED".equals(status) || "FAILED".equals(status);
    }

    /**
     * 获取错误消息
     */
    public String getErrorMessage(ImageGenerationResult result) {
        if (result != null) {
            if (result.getMessage() != null && !result.getMessage().isEmpty()) {
                return result.getMessage();
            }
            if (result.getOutput() != null && result.getOutput().getTaskStatus() != null) {
                return result.getOutput().getTaskStatus();
            }
        }
        return "未知错误";
    }

    /**
     * 构建 SDK 参数
     */
    private ImageGenerationParam buildSdkParam(Wan27ImageGenerationRequest request) {
        ImageGenerationParam.ImageGenerationParamBuilder<?, ?> builder = ImageGenerationParam.builder()
                .apiKey(apiKey)
                .model(request.getModel() != null ? request.getModel() : "wan2.7-image-pro");

        // 设置 parameters
        if (request.getParameters() != null) {
            Wan27ImageGenerationRequest.Parameters params = request.getParameters();
            if (params.getN() != null) {
                builder.n(params.getN());
            }
            if (params.getSize() != null) {
                builder.size(params.getSize());
            }
            if (params.getWatermark() != null) {
                builder.watermark(params.getWatermark());
            }
            if (params.getSeed() != null) {
                builder.seed(params.getSeed());
            }
        }

        // 转换 messages 格式
        if (request.getInput() != null && request.getInput().getMessages() != null) {
            List<com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage> messages = new ArrayList<>();
            for (Wan27ImageGenerationRequest.Message msg : request.getInput().getMessages()) {
                com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage sdkMsg =
                        com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage.builder()
                                .role(msg.getRole() != null ? msg.getRole() : "user")
                                .content(convertContent(msg.getContent()))
                                .build();
                messages.add(sdkMsg);
            }
            builder.messages(messages);
        }

        return builder.build();
    }

    /**
     * 转换 content 格式
     * 从 List<Map<String, String>> 转为 List<Map<String, Object>>
     */
    private List<Map<String, Object>> convertContent(List<Map<String, String>> content) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (content != null) {
            for (Map<String, String> item : content) {
                Map<String, Object> newItem = new HashMap<>(item);
                result.add(newItem);
            }
        }
        return result;
    }
}
