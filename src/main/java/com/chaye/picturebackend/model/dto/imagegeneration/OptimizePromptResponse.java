package com.chaye.picturebackend.model.dto.imagegeneration;

import lombok.Data;

import java.io.Serializable;

/**
 * Prompt优化响应
 */
@Data
public class OptimizePromptResponse implements Serializable {

    /**
     * 是否优化成功
     */
    private Boolean success = true;

    /**
     * 错误信息（当 success 为 false 时）
     */
    private String errorMessage;

    /**
     * 原始输入
     */
    private String originalPrompt;

    /**
     * 优化结果
     */
    private String optimizedPrompt;

    /**
     * 推荐尺寸（可选，格式："width,height"）
     */
    private String recommendedSize;

    /**
     * 负面提示词（可选）
     */
    private String negativePrompt;

    private static final long serialVersionUID = 1L;
}
