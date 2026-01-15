package com.chaye.picturebackend.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Prompt 增强工具
 *
 * 功能：将简单的图像描述转换为详细的 AI 图像生成 Prompt
 * 实现方式：调用阿里云通义千问 ChatClient 进行增强
 *
 * @author chaye
 */
@Slf4j
public class PromptEnhancerTool {

    private final ChatClient chatClient;

    /**
     * 构造函数，通过参数注入 ChatClient
     *
     * @param chatClient Spring AI ChatClient 实例
     */
    public PromptEnhancerTool(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 细节关键词列表
     * 用于判断输入是否已包含丰富细节
     */
    private static final String[] DETAIL_KEYWORDS = {
        // 颜色相关
        "color", "vibrant", "warm", "cool", "bright", "dark", "golden", "silver",
        "颜色", "鲜艳", "温暖", "冷色", "明亮", "黑暗", "金色", "银色",

        // 光线相关
        "lighting", "sunlight", "shadow", "glow", "illuminated", "bokeh", "ray",
        "光线", "阳光", "阴影", "发光", "照明", "散景", "光芒",

        // 风格相关
        "style", "cinematic", "realistic", "anime", "painting", "artistic", "professional",
        "风格", "电影感", "写实", "动漫", "绘画", "艺术", "专业",

        // 构图相关
        "composition", "foreground", "background", "perspective", "angle", "viewpoint",
        "构图", "前景", "背景", "透视", "角度", "视角",

        // 质量相关
        "detail", "high quality", "professional", "8k", "4k", "hd", "sharp", "crisp",
        "细节", "高质量", "专业", "清晰", "锐利"
    };

    /**
     * 增强 Prompt
     *
     * CRITICAL: @Tool description 是 AI 决策的关键
     * 必须清晰描述：
     * 1. 工具的功能
     * 2. 何时应该调用此工具
     * 3. 工具的输入输出
     */
    @Tool(description = """
        Enhance a simple image description into a detailed, high-quality AI image generation prompt.

        WHEN TO USE:
        - User provides a brief or vague description (e.g., "a cat", "sunset", "mountain")
        - User's input lacks visual details (colors, lighting, style, composition, quality indicators)
        - User explicitly asks to "optimize", "improve", or "enhance" the prompt
        - Input is simple and straightforward without technical terminology

        WHEN NOT TO USE:
        - User's input is already detailed (>100 characters with rich visual descriptions)
        - User's input contains professional photography or artistic terms
        - User's input already specifies style, lighting, and quality indicators

        HOW IT WORKS:
        This tool uses AI to transform simple descriptions into professional image generation prompts
        optimized for Stable Diffusion/DALL-E models.

        OUTPUT:
        A detailed English prompt including:
        - Rich visual details (colors, textures, mood)
        - Artistic style and technique
        - Lighting and composition
        - Quality indicators (8k, professional, detailed, etc.)

        EXAMPLE:
        Input: "一只猫"
        Output: "A fluffy orange tabby cat with bright green eyes, sitting gracefully on a wooden windowsill.
                 Soft golden hour sunlight streaming through the window, creating a warm bokeh effect in the background.
                 Professional pet photography style, high detail, 8K resolution."

        Input: "sunset"
        Output: "A breathtaking sunset over a calm ocean, vibrant orange and pink hues painting the sky.
                 Wispy clouds catching the golden light, gentle waves reflecting the colors.
                 Cinematic landscape photography, wide angle, professional composition, 8K detail."
        """)
    public String enhancePrompt(
            @ToolParam(description = "Simple image description to be enhanced, can be in any language")
            String userInput) {

        // 参数验证
        if (userInput == null || userInput.isBlank()) {
            log.warn("PromptEnhancerTool: Input is null or blank");
            return "Error: Input cannot be empty";
        }

        userInput = userInput.trim();
        log.info("PromptEnhancerTool: Processing input (length: {}): {}", userInput.length(), userInput);

        // 智能跳过逻辑：如果输入已经很详细，直接返回
        if (userInput.length() > 100 && containsRichDetails(userInput)) {
            log.info("PromptEnhancerTool: Input already detailed, skipping enhancement");
            return userInput; // 直接返回原输入
        }

        try {
            // System Prompt：引导 AI 进行 Prompt 增强
            String systemPrompt = """
                You are an expert AI Image Prompt Engineer. Your task is to transform simple descriptions
                into detailed, professional prompts optimized for AI image generation models (Stable Diffusion, DALL-E).

                Requirements:
                1. Rich visual details: Include colors, textures, lighting, mood, atmosphere
                2. Artistic terminology: Use professional photography/art terms (e.g., "cinematic lighting", "bokeh effect", "vibrant colors")
                3. Composition guidance: Specify perspective, angle, foreground/background elements
                4. Quality indicators: Add terms like "high detail", "8K resolution", "professional photography"
                5. Concise yet descriptive: Keep it between 50-150 words, focused and impactful
                6. Output ONLY in English: Even if input is in another language, output in English
                7. Output ONLY the enhanced prompt: No explanations, no meta-commentary

                If the input is already detailed and professional, return it as-is with minimal changes.

                Remember: The goal is to create a prompt that will generate a stunning, high-quality image.
                """;

            log.info("PromptEnhancerTool: Calling ChatClient to enhance prompt");

            // 调用 ChatClient 进行增强
            String enhancedPrompt = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userInput)
                    .call()
                    .content();

            // 验证输出
            if (enhancedPrompt == null || enhancedPrompt.isBlank()) {
                log.error("PromptEnhancerTool: ChatClient returned empty response");
                return "Error: Failed to enhance prompt - empty response from AI";
            }

            enhancedPrompt = enhancedPrompt.trim();
            log.info("PromptEnhancerTool: Successfully enhanced prompt (length: {}): {}",
                     enhancedPrompt.length(), enhancedPrompt);

            return enhancedPrompt;

        } catch (Exception e) {
            log.error("PromptEnhancerTool: Failed to enhance prompt: {}", e.getMessage(), e);
            // 返回错误信息而不是抛出异常，保证工具调用链路不中断
            return "Error enhancing prompt: " + e.getMessage();
        }
    }

    /**
     * 判断输入是否包含丰富细节
     *
     * 检测关键词：颜色、光线、风格、构图、质量等
     * 至少包含 3 个细节关键词才算详细
     *
     * @param input 用户输入
     * @return true if 输入包含丰富细节
     */
    private boolean containsRichDetails(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String lowerInput = input.toLowerCase();
        int detailCount = 0;

        // 统计关键词出现次数
        for (String keyword : DETAIL_KEYWORDS) {
            if (lowerInput.contains(keyword.toLowerCase())) {
                detailCount++;
                // 避免重复计数相同词根（如 "light" 和 "lighting"）
                if (detailCount >= 3) {
                    break;
                }
            }
        }

        boolean hasRichDetails = detailCount >= 3;
        log.debug("PromptEnhancerTool: Input detail check - keyword count: {}, hasRichDetails: {}",
                  detailCount, hasRichDetails);

        return hasRichDetails;
    }
}
