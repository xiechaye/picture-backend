package com.chaye.picturebackend.model.dto.imagegeneration;

import lombok.Data;

import java.io.Serializable;

/**
 * Prompt优化响应
 */
@Data
public class OptimizePromptResponse implements Serializable {

    /**
     * 原始输入
     */
    private String originalPrompt;

    /**
     * 优化结果
     */
    private String optimizedPrompt;

    private static final long serialVersionUID = 1L;
}
