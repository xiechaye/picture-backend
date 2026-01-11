package com.chaye.picturebackend.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建图像生成任务响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateImageTaskResponse {

    private Output output;

    /**
     * 表示任务的输出信息
     */
    @Data
    public static class Output {

        /**
         * 任务 ID
         */
        private String taskId;

        /**
         * 任务状态
         * <ul>
         *     <li>PENDING：排队中</li>
         *     <li>RUNNING：处理中</li>
         *     <li>SUSPENDED：挂起</li>
         *     <li>SUCCEEDED：执行成功</li>
         *     <li>FAILED：执行失败</li>
         *     <li>UNKNOWN：任务不存在或状态未知</li>
         * </ul>
         */
        private String taskStatus;

        /**
         * 生成的图像结果列表
         */
        private List<ImageResult> results;
    }

    @Data
    public static class ImageResult {
        /**
         * 图像 URL
         */
        private String url;
    }

    /**
     * 接口错误码。
     * <p>接口成功请求不会返回该参数。</p>
     */
    private String code;

    /**
     * 接口错误信息。
     * <p>接口成功请求不会返回该参数。</p>
     */
    private String message;

    /**
     * 请求唯一标识。
     * <p>可用于请求明细溯源和问题排查。</p>
     */
    private String requestId;

}
