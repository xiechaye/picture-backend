package com.chaye.picturebackend.tools;

import org.springframework.ai.chat.client.ChatClient;
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
     * 注册图像 Prompt 优化专用工具
     * 直接返回工具对象数组，由 Spring AI 自动识别 @Tool 注解
     *
     * @param chatClientBuilder ChatClient 构建器，用于创建 PromptEnhancerTool 所需的 ChatClient
     * @return 图像 Prompt 优化工具对象数组
     */
    @Bean
    public ToolCallback[] allTools(ChatClient.Builder chatClientBuilder) {
        // 构建 ChatClient 用于 PromptEnhancerTool
        ChatClient chatClient = chatClientBuilder.build();

        AspectRatioTool aspectRatioTool = new AspectRatioTool();
        NegativePromptTool negativePromptTool = new NegativePromptTool();
        PromptEnhancerTool promptEnhancerTool = new PromptEnhancerTool(chatClient);
        TerminateTool terminateTool = new TerminateTool();

        return ToolCallbacks.from(aspectRatioTool,
                negativePromptTool,
                promptEnhancerTool,
                terminateTool);
    }

}

