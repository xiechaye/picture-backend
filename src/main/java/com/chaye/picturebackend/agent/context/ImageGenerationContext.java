package com.chaye.picturebackend.agent.context;

import lombok.Data;

/**
 * 图像生成上下文（存储优化过程中的中间结果）
 *
 * @author chaye
 */
@Data
public class ImageGenerationContext {
    /**
     * 优化后的 Prompt
     */
    private String optimizedPrompt;

    /**
     * 推荐尺寸（格式："width,height"）
     */
    private String recommendedSize;

    /**
     * 负面提示词
     */
    private String negativePrompt;
}
