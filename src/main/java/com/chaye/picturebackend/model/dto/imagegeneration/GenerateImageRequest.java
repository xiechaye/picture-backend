package com.chaye.picturebackend.model.dto.imagegeneration;

import lombok.Data;

import java.io.Serializable;

/**
 * 图像生成请求
 */
@Data
public class GenerateImageRequest implements Serializable {

    /**
     * 图像描述（用户原始输入或优化后的 prompt）
     */
    private String prompt;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 图片尺寸（可选，格式："width,height"，如 "1024,1024"）
     * 如果不提供，使用默认尺寸
     */
    private String size;

    /**
     * 负面提示词（可选）
     * 如果不提供，不使用负面提示词
     */
    private String negativePrompt;

    private static final long serialVersionUID = 1L;
}
