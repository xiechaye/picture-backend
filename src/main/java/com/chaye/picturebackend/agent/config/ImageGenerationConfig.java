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
}
