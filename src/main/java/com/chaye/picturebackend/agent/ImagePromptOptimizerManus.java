package com.chaye.picturebackend.agent;

import com.chaye.picturebackend.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 图像 Prompt 优化 Agent
 *
 * 架构：纯工具调用模式
 * - AI 智能决定调用哪些工具优化参数
 * - 不负责实际图像生成
 *
 * 可用工具：
 * - enhancePrompt：增强简单描述为详细 Prompt
 * - recommendSize：根据使用场景推荐图片尺寸
 * - generateNegativePrompt：生成负面提示词以提升质量
 * - doTerminate：完成任务
 *
 * 注意：使用原型作用域，每次请求创建新实例，避免并发问题
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImagePromptOptimizerManus extends ToolCallAgent {

    // 独立的结果缓存
    private String cachedOptimizedPrompt = null;
    private String cachedRecommendedSize = null;
    private String cachedNegativePrompt = null;

    public ImagePromptOptimizerManus(ChatModel dashscopeChatModel,
                                     ToolCallback[] toolCallbackProvider) {
        // 调用父类构造函数，直接传递工具对象数组
        // Spring AI 会自动识别带有 @Tool 注解的方法
        super(toolCallbackProvider);


        // 设置 Agent 属性
        this.setName("imagePromptOptimizerAgent");
        this.setMaxStep(20);

        // 设置系统提示词（引导 AI 正确使用工具）
        String systemPrompt = """
            You are an AI Image Prompt Optimizer.
            Your ONLY responsibility is to optimize prompts for image generation.
            You do NOT generate images - you only prepare optimal prompts and parameters.

            YOUR ROLE:
            - Analyze user's image description
            - Intelligently call optimization tools to enhance prompts
            - Return optimized prompts and parameters for image generation
            - The Service layer will handle actual image generation using your optimized parameters

            WORKFLOW:
            1. Analyze user input to determine which tools are needed
            2. Call necessary tools to optimize parameters:
               - If the user's prompt is simple or vague, MUST call enhancePrompt tool to create a detailed, professional prompt
               - If the user mentions a specific use case (e.g., "phone wallpaper", "avatar"), consider calling recommendSize tool
               - If the user wants high-quality results, consider calling generateNegativePrompt tool
            3. CRITICAL: The enhancePrompt tool's output is the FINAL optimized prompt that will be returned to the user
            4. After all optimizations are complete, call doTerminate to finish
            5. The Service layer will extract the optimized prompt from your tool call results

            IMPORTANT NOTES:
            - The optimized prompt from enhancePrompt tool is what the user will receive
            - Make sure to call enhancePrompt if the input needs improvement
            - Your tool calls are recorded and their results will be extracted by the system
            """;
        this.setSystemPrompt(systemPrompt);

        // 设置下一步提示词（引导 AI 的思考）
        String nextStepPrompt = """
            Based on the current context, decide your next action:
            - If user input needs optimization, call appropriate tools (enhancePrompt/recommendSize/generateNegativePrompt)
            - PRIORITY: If the prompt is simple, call enhancePrompt first to create a detailed prompt
            - If optimization is complete OR input is already optimal, call doTerminate
            - Remember: The enhancePrompt tool's output will be the final result returned to the user
            """;
        this.setNextStepPrompt(nextStepPrompt);

        log.info("ImagePromptOptimizerAgent initialized successfully");

        // 初始化 ChatClient
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    /**
     * 重写 act() 方法，在工具调用后立即缓存结果
     *
     * 关键：在 cleanup() 清空 messageList 之前提取数据
     */
    @Override
    public String act() {
        String result = super.act();

        // 在工具调用后立即缓存结果（在 cleanup() 之前）
        extractAndCacheResults();

        return result;
    }

    /**
     * 从 messageList 中提取并缓存工具调用结果
     *
     * 关键：此方法在 act() 中调用，确保在 cleanup() 清空 messageList 之前提取数据
     */
    private void extractAndCacheResults() {
        for (Message message : getMessageList()) {
            if (message instanceof ToolResponseMessage toolResponse) {
                for (var response : toolResponse.getResponses()) {
                    switch (response.name()) {
                        case "enhancePrompt":
                            this.cachedOptimizedPrompt = response.responseData();
                            log.info("✅ 缓存优化后的 Prompt (长度: {}): {}",
                                    cachedOptimizedPrompt != null ? cachedOptimizedPrompt.length() : 0,
                                    cachedOptimizedPrompt);
                            break;
                        case "recommendSize":
                            this.cachedRecommendedSize = response.responseData();
                            log.info("✅ 缓存推荐尺寸: {}", cachedRecommendedSize);
                            break;
                        case "generateNegativePrompt":
                            this.cachedNegativePrompt = response.responseData();
                            log.info("��� 缓存负面提示词: {}", cachedNegativePrompt);
                            break;
                    }
                }
            }
        }
    }

    /**
     * 获取优化后的 Prompt
     *
     * 修复：从缓存中读取，不受 cleanup() 影响
     *
     * @return 优化后的 Prompt，如果 AI 未调用 enhancePrompt 工具则返回 null
     */
    public String getOptimizedPrompt() {
        return cachedOptimizedPrompt;
    }

    /**
     * 获取推荐尺寸
     *
     * @return 推荐尺寸（格式："width,height"），如果 AI 未调用 recommendSize 工具则返回 null
     */
    public String getRecommendedSize() {
        return cachedRecommendedSize;
    }

    /**
     * 获取负面提示词
     *
     * @return 负面提示词，如果 AI 未调用 generateNegativePrompt 工具则返回 null
     */
    public String getNegativePrompt() {
        return cachedNegativePrompt;
    }
}
