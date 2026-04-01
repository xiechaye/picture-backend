package com.chaye.picturebackend.api.imageEdit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建图像编辑任务响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateImageEditTaskResponse {

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
     * 错误码
     */
    private String code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 输出信息
     */
    @Data
    public static class Output {
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
    }
}
