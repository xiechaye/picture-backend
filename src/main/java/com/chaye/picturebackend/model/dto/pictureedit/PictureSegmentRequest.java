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

    /**
     * 框选区域坐标
     * <p>
     * 可选参数，用于指定抠图的目标区域
     * 格式: [x1, y1, x2, y2]，其中 (x1, y1) 为左上角坐标，(x2, y2) 为右下角坐标
     * <p>
     * 使用建议：在主体边缘外留 15-40 像素的缓冲区，以获得更好的边缘处理效果
     */
    private java.util.List<Integer> bbox;

    private static final long serialVersionUID = 1L;
}
