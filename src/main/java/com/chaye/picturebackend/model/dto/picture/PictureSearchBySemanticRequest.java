package com.chaye.picturebackend.model.dto.picture;

import lombok.Data;
import java.io.Serializable;

/**
 * @author chaye
 */
@Data
public class PictureSearchBySemanticRequest implements Serializable {

    /**
     * 搜索关键词 (例如："下雪天的故宫")
     */
    private String searchText;

    /**
     * 空间 ID (用于数据隔离，可选)
     */
    private Long spaceId;

    /**
     * 返回结果数量（可选，默认 10）
     */
    private Integer topK = 10;

    /**
     * 相似度阈值（可选，默认 0.5，范围 0-1）
     */
    private Double similarityThreshold = 0.5;

    private static final long serialVersionUID = 1L;
}