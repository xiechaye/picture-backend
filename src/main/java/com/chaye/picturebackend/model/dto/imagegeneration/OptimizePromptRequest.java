package com.chaye.picturebackend.model.dto.imagegeneration;

import lombok.Data;

import java.io.Serializable;

/**
 * Prompt优化请求
 */
@Data
public class OptimizePromptRequest implements Serializable {

    /**
     * 图像描述
     */
    private String prompt;

    private static final long serialVersionUID = 1L;
}
