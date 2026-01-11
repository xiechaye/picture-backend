package com.chaye.picturebackend.app;

import com.chaye.picturebackend.advisor.MyLoggerAdvisor;
import com.chaye.picturebackend.advisor.ReReadingAdvisor;
import com.chaye.picturebackend.agent.ImageGenerationAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 图像生成应用
 */
@Component
@Slf4j
public class ImageGenerationApp {

    private final ChatClient chatClient;

    // ImageGenerationAgent 实例
    @Resource
    private ImageGenerationAgent imageGenerationAgent;


    private static final String SYSTEM_PROMPT = "You are a professional AI image generation assistant. Your task is to help users generate high-quality images. " +
            "When users describe the images they want to generate, you will: " +
            "1. Understand the user's needs and intentions " +
            "2. Optimize the image description prompt by adding more details (such as colors, lighting, composition, style, etc.) " +
            "3. Call the image generation service to create images " +
            "4. Return the generated image URL and related information";

    public ImageGenerationApp(ChatModel dashscopeChatModel) {
        // 初始化基于文件的聊天记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        // 基于文件的持久化存储
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 初始化基于内存的聊天记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 默认基于文件的持久化存储
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志顾问
                        new MyLoggerAdvisor(),
                        // 自定义AI推理增强顾问
                        new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * 与AI进行对话
     * @param chatId 会话ID
     * @param message 用户输入的消息
     * @return 流式响应
     */
    public Flux<String> doChat(String chatId, String message) {

        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 图像生成结果记录
     */
    public record ImageGenerationResult(String imageUrl, String cosKey, String optimizedPrompt, long totalTime){}

    /**
     * 通过ImageGenerationAgent生成图像并返回完整结果
     * @param userPrompt 用户描述的图像内容
     * @return 图像生成结果
     */
    public ImageGenerationResult generateImageWithResult(String userPrompt) {
        try {
            // 执行图像生成（Flux流）
            imageGenerationAgent.run(userPrompt).blockLast();

            // 检查是否发生错误
            if (imageGenerationAgent.getAgentEnum() == com.chaye.picturebackend.model.enums.AgentEnum.ERROR) {
                com.chaye.picturebackend.agent.context.ImageGenerationContext errorContext = imageGenerationAgent.getContext();
                String errorMessage = errorContext != null && errorContext.getErrorMessage() != null
                        ? errorContext.getErrorMessage()
                        : "Unknown error";
                throw new RuntimeException("Image generation failed: " + errorMessage);
            }

            // 获取生成结果的上下文（必须在手动清理之前）
            com.chaye.picturebackend.agent.context.ImageGenerationContext context = imageGenerationAgent.getContext();
            if (context == null) {
                throw new RuntimeException("Cannot get image generation context");
            }

            // 构建返回结果
            ImageGenerationResult result = new ImageGenerationResult(
                    context.getImageUrl(),
                    context.getCosKey(),
                    context.getOptimizedPrompt(),
                    context.getTotalTime()
            );

            // 手动清理 ThreadLocal（在获取 context 之后）
            imageGenerationAgent.cleanupContext();

            return result;

        } catch (Exception e) {
            // 异常情况下也要清理 ThreadLocal，防止内存泄漏
            try {
                imageGenerationAgent.cleanupContext();
            } catch (Exception cleanupException) {
                log.warn("Failed to cleanup context: {}", cleanupException.getMessage());
            }
            log.error("Image generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Image generation failed", e);
        }
    }


    /**
     * 使用ChatClient优化图像描述（不生成图像）
     * @param chatId 会话ID
     * @param message 用户输入的图像描述
     * @return 优化后的图像描述prompt流式响应
     */
    public Flux<String> optimizeImagePrompt(String chatId, String message) {
        // 改进后的提示词：让AI识别输入语言并保持输出语言一致
        String optimizationPrompt = "You are an AI image generation prompt optimizer. " +
                "Please optimize the following image description into a more detailed and vivid prompt. " +
                "Add rich visual details such as colors, lighting, composition, style, atmosphere, etc. " +
                "\n\n**IMPORTANT**: " +
                "- If the input is in Chinese (中文), respond ONLY in Chinese. " +
                "- If the input is in English, respond ONLY in English. " +
                "- Maintain the same language as the input throughout your response." +
                "\n\nUser's description:\n" + message;

        return chatClient.prompt()
                .user(optimizationPrompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }
}
