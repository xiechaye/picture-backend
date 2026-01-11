package com.chaye.picturebackend.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具集中注册
 *
 * @author chaye
 */
@Configuration
public class ToolRegistration {
    // TODO: Fix ToolCallbacks initialization for ToolCallAgent
    // 暂时注释掉，因为新的 ImageGenerationAgent 不需要工具回调
    /*
    @Bean
    public ToolCallbacks allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminateTool terminateTool = new TerminateTool();

        return new ToolCallbacks();
    }
    */
}

