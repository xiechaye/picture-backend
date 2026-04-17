package com.chaye.picturebackend.model.dto.pictureedit;

import lombok.Data;

import java.io.Serializable;

/**
 * 局部重绘请求
 */
@Data
public class PictureInpaintingRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 掩码图片URL（白色区域为需要重绘的部分）
     */
    private String maskImageUrl;

    /**
     * 重绘描述
     */
    private String prompt;

    private static final long serialVersionUID = 1L;
}
