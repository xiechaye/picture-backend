package com.chaye.picturebackend.api.imageEdit.model;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 创建图像编辑任务请求
 */
@Data
public class CreateImageEditTaskRequest implements Serializable {

    /**
     * 模型名称
     */
    private String model = "wanx2.1-imageedit";

    /**
     * 输入参数
     */
    private Input input;

    /**
     * 处理参数
     */
    private Parameters parameters;

    /**
     * 输入参数
     */
    @Data
    public static class Input implements Serializable {
        /**
         * 功能类型
         * - remove_watermark: 去水印
         * - super_resolution: 图像超分
         * - colorization: 图像上色
         * - description_edit: 指令编辑
         * - expand: 扩图
         */
        @Alias("function")
        private String function;

        /**
         * 提示词
         */
        private String prompt;

        /**
         * 原图URL（公网可访问或Base64）
         */
        @Alias("base_image_url")
        private String baseImageUrl;

        /**
         * 掩码图像URL（仅局部重绘需要）
         */
        @Alias("mask_image_url")
        private String maskImageUrl;
    }

    /**
     * 处理参数
     */
    @Data
    public static class Parameters implements Serializable {
        /**
         * 生成图片数量，默认1
         */
        private Integer n = 1;

        /**
         * 超分倍数（仅超分功能）
         */
        @Alias("upscale_factor")
        private Integer upscaleFactor;

        /**
         * 扩图比例（仅扩图功能）
         */
        @Alias("top_scale")
        @JsonProperty("topScale")
        private Float topScale;

        @Alias("bottom_scale")
        @JsonProperty("bottomScale")
        private Float bottomScale;

        @Alias("left_scale")
        @JsonProperty("leftScale")
        private Float leftScale;

        @Alias("right_scale")
        @JsonProperty("rightScale")
        private Float rightScale;

        /**
         * 随机种子
         */
        private Integer seed;

        /**
         * 是否添加水印
         */
        private Boolean watermark = false;

        /**
         * 自定义参数
         */
        private Map<String, Object> customParams = new HashMap<>();
    }

    private static final long serialVersionUID = 1L;
}
