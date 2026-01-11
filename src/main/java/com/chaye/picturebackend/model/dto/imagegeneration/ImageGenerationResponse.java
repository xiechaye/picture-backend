package com.chaye.picturebackend.model.dto.imagegeneration;

import lombok.Data;

import java.io.Serializable;

/**
 * 图像生成响应
 */
@Data
public class ImageGenerationResponse implements Serializable {

    /**
     * 生成的图像URL
     */
    private String imageUrl;

    /**
     * COS存储路径
     */
    private String cosKey;

    /**
     * 优化后的prompt
     */
    private String optimizedPrompt;

    /**
     * 总耗时（毫秒）
     */
    private Long totalTime;

    /**
     * 空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
