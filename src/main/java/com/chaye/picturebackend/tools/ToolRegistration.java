package com.chaye.picturebackend.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具集中注册
 *
 * @author chaye
 */
@Configuration
public class ToolRegistration {

    /**
     * 注册图像生成专用工具
     * 根据配置动态注册工具
     *
     * @return 图像生成工具回调集合
     */
    @Bean("imageGenerationTools")
    public ToolCallback[] imageGenerationTools() {
        // 使用 ToolCallbacks.from() 创建 ToolCallback 数组
        AspectRatioTool aspectRatioTool = new AspectRatioTool();
        NegativePromptTool negativePromptTool = new NegativePromptTool();
        PromptEnhancerTool promptEnhancerTool = new PromptEnhancerTool();
        return ToolCallbacks.from(aspectRatioTool, negativePromptTool, promptEnhancerTool);
    }

}

