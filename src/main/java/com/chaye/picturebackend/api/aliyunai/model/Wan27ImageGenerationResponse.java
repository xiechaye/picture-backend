package com.chaye.picturebackend.api.aliyunai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * wan2.7-image-pro 图像生成响应
 * <p>
 * 支持同步和异步调用响应
 */
@Data
public class Wan27ImageGenerationResponse implements Serializable {

    /**
     * 请求 ID
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * 输出结果
     */
    private Output output;

    /**
     * 使用情况（同步响应包含）
     */
    private Usage usage;

    /**
     * 状态码
     */
    @JsonProperty("status_code")
    private Integer statusCode;

    /**
     * 错误码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 输出结果
     */
    @Data
    public static class Output implements Serializable {
        /**
         * 任务 ID（异步响应）
         */
        @JsonProperty("task_id")
        private String taskId;

        /**
         * 任务状态（异步响应）：PENDING, PROCESSING, SUCCEEDED, FAILED
         */
        @JsonProperty("task_status")
        private String taskStatus;

        /**
         * 生成结果列表（同步响应或任务完成后）
         */
        private List<Choice> choices;

        /**
         * 任务是否完成
         */
        private Boolean finished;

        /**
         * 提交时间
         */
        @JsonProperty("submit_time")
        private String submitTime;

        /**
         * 调度时间
         */
        @JsonProperty("scheduled_time")
        private String scheduledTime;

        /**
         * 结束时间
         */
        @JsonProperty("end_time")
        private String endTime;

        /**
         * 结果消息（失败时）
         */
        private String message;
    }

    /**
     * 生成选项
     */
    @Data
    public static class Choice implements Serializable {
        /**
         * 结束原因
         */
        @JsonProperty("finish_reason")
        private String finishReason;

        /**
         * 消息
         */
        private Message message;
    }

    /**
     * 消息
     */
    @Data
    public static class Message implements Serializable {
        /**
         * 角色
         */
        private String role;

        /**
         * 内容列表
         */
        private List<ContentItem> content;
    }

    /**
     * 内容项
     */
    @Data
    public static class ContentItem implements Serializable {
        /**
         * 类型：image 或 text
         */
        private String type;

        /**
         * 图片 URL（当 type=image 时）
         */
        private String image;
    }

    /**
     * 使用情况
     */
    @Data
    public static class Usage implements Serializable {
        /**
         * 输入 token 数
         */
        @JsonProperty("input_tokens")
        private Integer inputTokens;

        /**
         * 输出 token 数
         */
        @JsonProperty("output_tokens")
        private Integer outputTokens;

        /**
         * 总 token 数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;

        /**
         * 图片数量
         */
        @JsonProperty("image_count")
        private Integer imageCount;

        /**
         * 图片尺寸
         */
        private String size;
    }

    /**
     * 判断是否为异步任务响应
     */
    public boolean isAsyncTask() {
        return output != null && output.getTaskId() != null && "PENDING".equals(output.getTaskStatus());
    }

    /**
     * 判断任务是否成功
     */
    public boolean isSuccess() {
        return statusCode != null && statusCode == 200
                && (code == null || code.isEmpty());
    }

    /**
     * 判断任务是否完成
     */
    public boolean isFinished() {
        return output != null && Boolean.TRUE.equals(output.getFinished())
                && "SUCCEEDED".equals(output.getTaskStatus());
    }

    /**
     * 获取首个图片 URL
     */
    public String getFirstImageUrl() {
        if (output != null && output.getChoices() != null && !output.getChoices().isEmpty()) {
            Choice choice = output.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                for (ContentItem item : choice.getMessage().getContent()) {
                    if ("image".equals(item.getType())) {
                        return item.getImage();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取所有图片 URL
     */
    public List<String> getAllImageUrls() {
        List<String> urls = new java.util.ArrayList<>();
        if (output != null && output.getChoices() != null && !output.getChoices().isEmpty()) {
            for (Choice choice : output.getChoices()) {
                if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                    for (ContentItem item : choice.getMessage().getContent()) {
                        if ("image".equals(item.getType()) && item.getImage() != null) {
                            urls.add(item.getImage());
                        }
                    }
                }
            }
        }
        return urls;
    }

    private static final long serialVersionUID = 1L;
}
