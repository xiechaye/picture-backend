package com.chaye.picturebackend.api.imageEdit.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询图像编辑任务响应
 */
@Data
public class GetImageEditTaskResponse implements Serializable {

    /**
     * 输出信息
     */
    private Output output;

    /**
     * 请求ID
     */
    @com.fasterxml.jackson.annotation.JsonProperty("request_id")
    private String requestId;

    /**
     * 使用量统计
     */
    private Usage usage;

    /**
     * 输出信息
     */
    @Data
    public static class Output implements Serializable {
        /**
         * 任务ID
         */
        @com.fasterxml.jackson.annotation.JsonProperty("task_id")
        private String taskId;

        /**
         * 任务状态
         */
        @com.fasterxml.jackson.annotation.JsonProperty("task_status")
        private String taskStatus;

        /**
         * 提交时间
         */
        @com.fasterxml.jackson.annotation.JsonProperty("submit_time")
        private String submitTime;

        /**
         * 调度时间
         */
        @com.fasterxml.jackson.annotation.JsonProperty("scheduled_time")
        private String scheduledTime;

        /**
         * 结束时间
         */
        @com.fasterxml.jackson.annotation.JsonProperty("end_time")
        private String endTime;

        /**
         * 结果列表
         */
        private List<Result> results;

        /**
         * 任务指标
         */
        @com.fasterxml.jackson.annotation.JsonProperty("task_metrics")
        private TaskMetrics taskMetrics;

        /**
         * 错误码
         */
        private String code;

        /**
         * 错误信息
         */
        private String message;
    }

    /**
     * 结果
     */
    @Data
    public static class Result implements Serializable {
        /**
         * 图片URL
         */
        private String url;

        /**
         * 错误码
         */
        private String code;

        /**
         * 错误信息
         */
        private String message;
    }

    /**
     * 任务指标
     */
    @Data
    public static class TaskMetrics implements Serializable {
        /**
         * 总数
         */
        private Integer total;

        /**
         * 成功数
         */
        private Integer succeeded;

        /**
         * 失败数
         */
        private Integer failed;
    }

    /**
     * 使用量统计
     */
    @Data
    public static class Usage implements Serializable {
        /**
         * 图片数量
         */
        @com.fasterxml.jackson.annotation.JsonProperty("image_count")
        private Integer imageCount;
    }

    private static final long serialVersionUID = 1L;
}
