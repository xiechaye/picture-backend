package com.chaye.picturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 图片抠图提示词配置类
 * <p>
 * 用于配置人像抠图和物品抠图的提示词及相关参数
 * 配置前缀: image.segment
 *
 * @author Claude Code
 * @since 2025-04-17
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "image.segment")
public class ImageSegmentPromptConfig {

    /**
     * 人像抠图提示词
     * <p>
     * 针对人像抠图场景优化的专业提示词，强调：
     * - 边缘平滑、抗锯齿处理
     * - 完整保留面部五官、头发细节、皮肤纹理
     * - 透明背景输出（RGBA PNG）
     * - 像素级精准，不变形、不丢失细节
     */
    private String humanPrompt = "在指定的框选区域（bbox）内进行精确的人物主体提取和背景移除操作：彻底移除框选区域外的所有背景内容，只保留人物完整主体。严格保留人物的所有细节，包括面部五官、眼睛高光、头发每一丝细节、发丝边缘、皮肤纹理、衣服褶皱和材质、光影与高光自然过渡。边缘处理必须平滑、抗锯齿、无锯齿状、无模糊、无残留背景像素。输出结果为带透明背景的 RGBA PNG 图像，人物姿态、比例、五官特征100%保持与输入一致，像素级精准，不变形、不丢失细节。";

    /**
     * 物品抠图提示词
     * <p>
     * 针对物品抠图场景优化的专业提示词，强调：
     * - 精确分割目标物体
     * - 保留纹理、边缘、自然光影
     * - 透明背景输出
     */
    private String objectPrompt = "框选区域内精确分割出目标物体，彻底移除背景。完整保留物体的所有细节、纹理、边缘和自然光影。边缘处理平滑、抗锯齿、无残留背景像素。输出透明背景的 PNG 图像，物体特征完全一致，像素级精准。";

    /**
     * 默认输出尺寸
     * <p>
     * 可选值: "2K", "1K", "512" 等
     */
    private String size = "2K";

    /**
     * 默认生成数量
     */
    private Integer n = 1;

    /**
     * 是否添加水印
     */
    private Boolean watermark = false;
}
