package com.chaye.picturebackend.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图像生成 Agent 配置类
 * 用于配置图像生成相关参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.image-generation")
public class ImageGenerationConfig {

    /**
     * 默认图像模型
     */
    private String defaultModel = "wanx-v1";

    /**
     * 默认图像尺寸
     */
    private String defaultSize = "1024*1024";

    /**
     * 生成图像数量
     */
    private Integer defaultImageCount = 1;

    /**
     * 任务轮询最大重试次数
     */
    private Integer maxPollingRetries = 60;

    /**
     * 轮询初始间隔（毫秒）
     */
    private Long initialPollingInterval = 1000L;

    /**
     * 轮询最大间隔（毫秒）
     */
    private Long maxPollingInterval = 5000L;

    /**
     * 是否使用指数退避策略
     */
    private Boolean useExponentialBackoff = true;

    /**
     * 下载超时时间（毫秒）
     */
    private Integer downloadTimeout = 30000;

    /**
     * 上传文件前缀
     */
    private String uploadPrefix = "generated/";

    /**
     * 临时文件前缀
     */
    private String tempFilePrefix = "ai_generated_";

    /**
     * 临时文件后缀
     */
    private String tempFileSuffix = ".png";

    /**
     * 最小 Prompt 长度
     */
    private Integer minPromptLength = 10;

    /**
     * 最大 Prompt 长度
     */
    private Integer maxPromptLength = 2000;

    // ========== 工具调用阶段配置 ==========

    /**
     * 工具调用阶段最大步数
     * 默认：3
     * 说明：AI 最多调用 3 次工具后自动进入图像生成阶段
     */
    private Integer maxToolCallSteps = 3;

    /**
     * 是否启用 PromptEnhancerTool
     * 默认：true
     */
    private Boolean usePromptEnhancerTool = true;

    /**
     * 是否启用 AspectRatioTool
     * 默认：true
     */
    private Boolean useAspectRatioTool = true;

    /**
     * 是否启用 NegativePromptTool
     * 默认：true
     */
    private Boolean useNegativePromptTool = true;

    /**
     * Prompt 增强工具的长度阈值
     * 默认：100
     * 说明：输入长度 > 此值时，不调用 PromptEnhancerTool
     */
    private Integer promptEnhancerThreshold = 100;

    /**
     * 负面提示词格式模板
     * 占位符：{prompt} - 优化后的 Prompt
     *        {negative} - 负面提示词
     * 默认："{prompt}\n\nAvoid: {negative}"
     */
    private String negativePromptFormat = "{prompt}\n\nAvoid: {negative}";

    /**
     * 是否启用智能尺寸检测
     * 默认：true
     * 说明：从用户输入中检测尺寸关键词（如"手机壁纸"），自动调用 AspectRatioTool
     */
    private Boolean enableSmartAspectRatio = true;
}
