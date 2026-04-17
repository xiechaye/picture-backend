package com.chaye.picturebackend.api.aliyunai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * wan2.7-image-pro 图像生成请求
 * <p>
 * 支持文生图、图生图、图像编辑等统一格式
 * <p>
 * 官方文档：https://help.aliyun.com/zh/model-studio/wan-xiang-2.7-image-pro/developer-reference/api-reference
 */
@Data
public class Wan27ImageGenerationRequest implements Serializable {

    /**
     * 模型名称
     */
    private String model = "wan2.7-image-pro";

    /**
     * 输入参数
     */
    private Input input;

    /**
     * 生成参数
     */
    private Parameters parameters;

    /**
     * 输入参数
     */
    @Data
    public static class Input implements Serializable {
        /**
         * 消息列表
         */
        private List<Message> messages;
    }

    /**
     * 消息
     */
    @Data
    public static class Message implements Serializable {
        /**
         * 角色，固定为 user
         */
        private String role = "user";

        /**
         * 内容列表，支持文本和图片混合输入
         * <p>
         * 每个元素是一个 Map，包含 "text" 或 "image" 键
         */
        private List<Map<String, String>> content;
    }

    /**
     * 生成参数
     */
    @Data
    public static class Parameters implements Serializable {
        /**
         * 生成图片数量，默认 1
         * 文生图：1-4
         * 组图模式：1-12
         */
        private Integer n = 1;

        /**
         * 输出尺寸
         * 可选值：512, 1K, 2K, 4K（仅文生图支持4K）
         */
        private String size = "2K";

        /**
         * 是否添加水印
         */
        @JsonProperty("watermark")
        private Boolean watermark = false;

        /**
         * 框选区域列表（交互式编辑）
         * 格式：[[[x1, y1, x2, y2], ...], ...]
         * 最外层长度 = 输入图片数量
         * 每张图片最多支持 2 个框
         */
        @JsonProperty("bbox_list")
        private List<List<List<Integer>>> bboxList;

        /**
         * 随机种子
         */
        private Integer seed;

        /**
         * 颜色调色盘（3-10种颜色）
         */
        @JsonProperty("color_palette")
        private List<String> colorPalette;

        /**
         * 是否开启组图模式
         */
        @JsonProperty("enable_sequential")
        private Boolean enableSequential = false;

        /**
         * 思考模式（仅文生图有效，提升质量但增加耗时）
         */
        @JsonProperty("thinking_mode")
        private Boolean thinkingMode = true;
    }

    private static final long serialVersionUID = 1L;
}
