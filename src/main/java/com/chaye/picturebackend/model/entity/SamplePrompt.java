package com.chaye.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 示例提示词
 * @TableName sample_prompt
 */
@TableName(value = "sample_prompt")
@Data
public class SamplePrompt implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 短中文标题
     */
    private String title;

    /**
     * 用于AI的长英文提示词
     */
    private String prompt;

    /**
     * 分类（如 Scenery, Anime, Cyberpunk）
     */
    private String category;

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
