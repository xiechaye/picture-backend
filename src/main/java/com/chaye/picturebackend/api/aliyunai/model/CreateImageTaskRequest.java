package com.chaye.picturebackend.api.aliyunai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建图像生成任务请求
 */
@Data
public class CreateImageTaskRequest implements Serializable {

    /**
     * 模型，wanx-v1
     */
    private String model = "wanx-v1";

    /**
     * 输入信息
     */
    private Input input;

    /**
     * 图像生成参数
     */
    private Parameters parameters;

    @Data
    public static class Input implements Serializable {
        /**
         * 必选，文本描述，用于生成图像
         */
        private String prompt;
    }

    @Data
    public static class Parameters implements Serializable {
        /**
         * 可选，图像风格
         * <auto>: 默认
         * <3d cartoon>: 3D卡通
         * <anime>: 动漫
         * <oil painting>: 油画
         * <watercolor>: 水彩
         * <sketch>: 素描
         * <chinese painting>: 中国画
         * <flat illustration>: 扁平插画
         */
        private String style;

        /**
         * 可选，生成图像的尺寸
         * 1024*1024, 720*1280, 1280*720
         * 默认值为1024*1024
         */
        private String size;

        /**
         * 可选，生成图像的数量
         * 目前支持1-4张，默认值为1
         */
        private Integer n;

        /**
         * 可选，随机种子
         */
        private Integer seed;

        /**
         * 可选，参考图
         */
        @JsonProperty("ref_img")
        private String refImg;
    }
}
