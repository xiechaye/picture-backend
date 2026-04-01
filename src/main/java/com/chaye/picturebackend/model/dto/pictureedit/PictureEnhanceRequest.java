package com.chaye.picturebackend.model.dto.pictureedit;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片增强请求
 */
@Data
public class PictureEnhanceRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 增强类型：quality-质量提升, denoise-降噪, sharpen-锐化
     */
    private String enhanceType;

    private static final long serialVersionUID = 1L;
}
