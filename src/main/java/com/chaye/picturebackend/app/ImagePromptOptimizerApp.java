package com.chaye.picturebackend.app;

import com.chaye.picturebackend.agent.ImagePromptOptimizerAgent;
import com.chaye.picturebackend.agent.context.ImageGenerationContext;
import com.chaye.picturebackend.tools.ToolNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 图像 Prompt 优化应用
 */
@Component
@Slf4j
public class ImagePromptOptimizerApp {

    /**
     * ImagePromptOptimizerAgent 提供者，用于获取原型作用域的 Agent 实例
     * 每次调用 getObject() 都会创建新实例，避免并发问题
     */
    private final ObjectProvider<ImagePromptOptimizerAgent> agentProvider;

    public ImagePromptOptimizerApp(ObjectProvider<ImagePromptOptimizerAgent> agentProvider) {
        this.agentProvider = agentProvider;
    }


    /**
     * Agent 参数优化结果
     */
    public record ParameterOptimizationResult(
            String optimizedPrompt,      // 优化后的 Prompt (必有)
            String recommendedSize,       // 推荐尺寸 "width,height" (可选)
            String negativePrompt,        // 负面提示词 (可选)
            long optimizationTime         // 优化耗时 (毫秒)
    ) {}

    /**
     * 通过 ImagePromptOptimizerAgent 优化参数
     *
     * @param userPrompt 用户描述
     * @return 优化后的参数
     */
    public ParameterOptimizationResult optimizeParameters(String userPrompt) {
        long startTime = System.currentTimeMillis();

        // 每次调用获取新的 Agent 实例，避免并发问题
        ImagePromptOptimizerAgent agent = agentProvider.getObject();

        try {
            // 收集 Agent 执行的所有步骤输出
            java.util.List<String> outputs = new java.util.ArrayList<>();
            agent.run(userPrompt)
                    .doOnNext(outputs::add)
                    .blockLast();

            // 检查是否发生错误
            if (agent.getAgentEnum() == com.chaye.picturebackend.model.enums.AgentEnum.ERROR) {
                log.warn("Agent 优化失败，使用原始 Prompt");
                return new ParameterOptimizationResult(
                        userPrompt,  // 使用原始输入
                        null,        // 无推荐尺寸
                        null,        // 无负面提示词
                        0L
                );
            }

            // 从 Agent 的上下文中提取优化结果
            ImageGenerationContext context = extractOptimizationContext(agent);

            long optimizationTime = System.currentTimeMillis() - startTime;

            // 如果没有优化 Prompt，使用原始输入
            String finalPrompt = context.getOptimizedPrompt() != null
                    ? context.getOptimizedPrompt()
                    : userPrompt;

            log.info("参数优化完成，耗时: {}ms, hasOptimizedPrompt: {}, hasSize: {}, hasNegative: {}",
                    optimizationTime,
                    context.getOptimizedPrompt() != null,
                    context.getRecommendedSize() != null,
                    context.getNegativePrompt() != null);

            return new ParameterOptimizationResult(
                    finalPrompt,
                    context.getRecommendedSize(),
                    context.getNegativePrompt(),
                    optimizationTime
            );

        } catch (Exception e) {
            log.error("参数优化异常: {}", e.getMessage(), e);
            // 降级：返回原始输入
            return new ParameterOptimizationResult(userPrompt, null, null, 0L);
        }
    }

    /**
     * 从 Agent 的 messageList 中提取优化上下文
     *
     * @param agent Agent 实例
     * @return 优化上下文
     */
    private ImageGenerationContext extractOptimizationContext(ImagePromptOptimizerAgent agent) {
        ImageGenerationContext context = new ImageGenerationContext();

        // 从 Agent 的 messageList 中获取 ToolResponseMessage
        java.util.List<Message> messageList = agent.getMessageList();

        for (Message message : messageList) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (var response : toolResponseMessage.getResponses()) {
                    String toolName = response.name();
                    String toolOutput = response.responseData();  // 工具返回值

                    switch (toolName) {
                        case ToolNames.ENHANCE_PROMPT:
                            context.setOptimizedPrompt(toolOutput);
                            log.debug("提取到增强的 Prompt，长度: {}", toolOutput.length());
                            break;
                        case ToolNames.RECOMMEND_SIZE:
                            context.setRecommendedSize(toolOutput);
                            log.debug("提取到推荐尺寸: {}", toolOutput);
                            break;
                        case ToolNames.GENERATE_NEGATIVE_PROMPT:
                            context.setNegativePrompt(toolOutput);
                            log.debug("提取到负面提示词，长度: {}", toolOutput.length());
                            break;
                    }
                }
            }
        }

        return context;
    }


}
