package com.chaye.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片编辑任务
 * @TableName picture_edit_task
 */
@TableName(value = "picture_edit_task")
@Data
public class PictureEditTask implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID（外部系统返回的任务ID）
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
     * 编辑类型：SEGMENT/REMOVE_WATERMARK/ENHANCE
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
     * 请求参数JSON
     */
    private String requestParams;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
