package com.chaye.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 示例提示词视图对象（用户端返回）
 */
@Data
public class SamplePromptVO implements Serializable {

    /**
     * 短中文标题
     */
    private String title;

    /**
     * 用于AI的长英文提示词
     */
    private String prompt;

    private static final long serialVersionUID = 1L;
}
