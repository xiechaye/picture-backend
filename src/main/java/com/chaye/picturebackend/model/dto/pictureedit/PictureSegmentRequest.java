package com.chaye.picturebackend.model.dto.pictureedit;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片抠图请求
 */
@Data
public class PictureSegmentRequest implements Serializable {

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 抠图类型：human-人像, object-物体
     */
    private String type;

    private static final long serialVersionUID = 1L;
}
