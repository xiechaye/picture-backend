package com.chaye.picturebackend.agent.exception;

/**
 * 图像生成异常
 */
public class ImageGenerationException extends RuntimeException {

    private final ErrorType errorType;

    public ImageGenerationException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ImageGenerationException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        /**
         * Prompt 优化失败
         */
        PROMPT_OPTIMIZATION_FAILED,

        /**
         * API 调用失败
         */
        API_CALL_FAILED,

        /**
         * 任务创建失败
         */
        TASK_CREATION_FAILED,

        /**
         * 任务超时
         */
        TASK_TIMEOUT,

        /**
         * 任务失败
         */
        TASK_FAILED,

        /**
         * 图像下载失败
         */
        IMAGE_DOWNLOAD_FAILED,

        /**
         * 图像上传失败
         */
        IMAGE_UPLOAD_FAILED,

        /**
         * 参数验证失败
         */
        VALIDATION_FAILED,

        /**
         * 网络异常
         */
        NETWORK_ERROR,

        /**
         * 未知错误
         */
        UNKNOWN_ERROR
    }
}
