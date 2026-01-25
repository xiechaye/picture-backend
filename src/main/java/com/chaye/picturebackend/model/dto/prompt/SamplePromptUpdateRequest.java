package com.chaye.picturebackend.model.dto.prompt;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新示例提示词请求
 */
@Data
public class SamplePromptUpdateRequest implements Serializable {

    /**
     * id
     */
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

    private static final long serialVersionUID = 1L;
}
