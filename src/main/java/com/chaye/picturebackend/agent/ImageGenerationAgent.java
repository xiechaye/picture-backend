package com.chaye.picturebackend.agent;

import com.chaye.picturebackend.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 图像生成 Agent
 *
 * 架构：纯工具调用模式
 * - AI 智能决定调用哪些工具优化参数
 * - 最后调用 generateAndUpload 工具完成图像生成和上传
 * - 完全依赖父类 ToolCallAgent 的 think() 和 act() 循环
 *
 * 可用工具：
 * - enhancePrompt：增强简单描述为详细 Prompt
 * - recommendSize：根据使用场景推荐图片尺寸
 * - generateNegativePrompt：生成负面提示词以提升质量
 * - generateAndUpload：生成图像并上传到 COS（核心工具）
 * - doTerminate：完成任务
 *
 * 注意：使用原型作用域，每次请求创建新实例，避免并发问题
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImageGenerationAgent extends ToolCallAgent {

    public ImageGenerationAgent(ChatModel dashscopeChatModel,
                                @Qualifier("imageGenerationTools") ToolCallback[] imageGenerationTools) {
        // 调用父类构造函数
        super(imageGenerationTools, null);

        // 初始化 ChatClient
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);

        // 设置 Agent 属性
        this.setName("imageGenerationAgent");
        this.setMaxStep(20); //

        // 设置系统提示词（引导 AI 正确使用工具）
        String systemPrompt = """
            You are an AI Image Generation Parameter Optimizer.
            Your ONLY responsibility is to optimize parameters for image generation.
            You do NOT generate images - you only prepare optimal parameters.

            YOUR ROLE:
            - Analyze user's image description
            - Intelligently call optimization tools to enhance parameters
            - Return optimized parameters for image generation
            - The Service layer will handle actual image generation using your optimized parameters

            WORKFLOW:
            1. Analyze user input to determine which tools are needed
            2. Call necessary tools to optimize parameters
            3. After optimization is complete, call doTerminate
            4. The Service layer will handle actual image generation
            """;
        this.setSystemPrompt(systemPrompt);

        // 设置下一步提示词（引导 AI 的思考）
        String nextStepPrompt = """
            Based on the current context, decide your next action:
            - If user input needs optimization, call appropriate tools (enhancePrompt/recommendSize/generateNegativePrompt)
            - If optimization is complete OR input is already optimal, call doTerminate
            - Remember: You are a parameter optimizer, NOT an image generator
            """;
        this.setNextStepPrompt(nextStepPrompt);

        log.info("ImageGenerationAgent initialized successfully (parameter optimizer mode)");
    }
}
