package com.chaye.picturebackend.model.dto.pictureedit;

import lombok.Data;

import java.io.Serializable;

/**
 * 去除水印请求
 */
@Data
public class PictureRemoveWatermarkRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 水印区域（可选，指定水印区域）
     */
    private WatermarkArea watermarkArea;

    /**
     * 水印区域信息
     */
    @Data
    public static class WatermarkArea implements Serializable {
        /**
         * X坐标
         */
        private Integer x;
        /**
         * Y坐标
         */
        private Integer y;
        /**
         * 宽度
         */
        private Integer width;
        /**
         * 高度
         */
        private Integer height;
    }

    private static final long serialVersionUID = 1L;
}
