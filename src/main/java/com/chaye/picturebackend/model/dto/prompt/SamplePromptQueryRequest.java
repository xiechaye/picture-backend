package com.chaye.picturebackend.model.dto.prompt;

import com.chaye.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询示例提示词请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SamplePromptQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题（模糊搜索）
     */
    private String title;

    /**
     * 提示词内容（模糊搜索）
     */
    private String prompt;

    /**
     * 分类
     */
    private String category;

    private static final long serialVersionUID = 1L;
}
