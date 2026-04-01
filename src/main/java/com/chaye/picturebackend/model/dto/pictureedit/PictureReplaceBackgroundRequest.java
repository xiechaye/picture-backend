package com.chaye.picturebackend.model.dto.pictureedit;

import lombok.Data;

import java.io.Serializable;

/**
 * 背景替换请求
 */
@Data
public class PictureReplaceBackgroundRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 背景类型：color-纯色, image-图片, transparent-透明
     */
    private String backgroundType;

    /**
     * 背景值（颜色值或图片URL）
     */
    private String backgroundValue;

    private static final long serialVersionUID = 1L;
}
