package com.chaye.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片编辑任务响应
 */
@Data
public class PictureEditTaskVO implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 原图ID
     */
    private Long pictureId;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 编辑类型：SEGMENT/REMOVE_WATERMARK/ENHANCE/REPLACE_BACKGROUND
     */
    private String editType;

    /**
     * 任务状态：PROCESSING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 处理后图片URL
     */
    private String resultUrl;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
