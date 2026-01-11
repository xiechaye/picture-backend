package com.chaye.picturebackend.model.dto.imagegeneration;

import lombok.Data;

import java.io.Serializable;

/**
 * 图像生成请求
 */
@Data
public class GenerateImageRequest implements Serializable {

    /**
     * 图像描述
     */
    private String prompt;

    /**
     * 空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
