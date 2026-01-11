package com.chaye.picturebackend.agent.context;

import lombok.Data;

/**
 * 图像生成上下文
 * 用于在不同步骤之间传递数据
 */
@Data
public class ImageGenerationContext {

    /**
     * 用户原始输入
     */
    private String userInput;

    /**
     * AI 优化后的 Prompt
     */
    private String optimizedPrompt;

    /**
     * 图像生成任务 ID
     */
    private String taskId;

    /**
     * 生成的图像 URL
     */
    private String imageUrl;

    /**
     * COS 存储路径
     */
    private String cosKey;

    /**
     * 各步骤执行时间（毫秒）
     */
    private Long promptOptimizationTime;
    private Long imageGenerationTime;
    private Long uploadTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private Long startTime = System.currentTimeMillis();

    /**
     * 获取总执行时间
     */
    public Long getTotalTime() {
        return System.currentTimeMillis() - startTime;
    }
}
