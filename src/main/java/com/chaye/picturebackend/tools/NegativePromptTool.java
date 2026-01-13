package com.chaye.picturebackend.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 负面提示词工具
 *
 * 功能：根据图片类别生成负面提示词（Negative Prompt）
 * 作用：告诉 AI 避免生成的元素，提升图像质量
 * 实现方式：纯内存操作，基于类别映射表 + 智能推断
 *
 * @author chaye
 */
@Slf4j
public class NegativePromptTool {

    /**
     * 类别到负面提示词的映射表
     * 支持中英文类别
     */
    private static final Map<String, String> CATEGORY_NEGATIVE_PROMPT_MAP = new HashMap<>();

    /**
     * 通用负面提示词（适用于所有类别）
     */
    private static final String UNIVERSAL_NEGATIVE_PROMPT =
        "low quality, low resolution, blurry, pixelated, jpeg artifacts, " +
        "watermark, signature, text, username, logo, bad lighting, noise, grainy";

    static {
        // ========== 人像类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("portrait",
            "deformed face, disfigured, bad anatomy, extra limbs, missing limbs, " +
            "mutated hands, poorly drawn hands, poorly drawn face, mutation, ugly, " +
            "bad proportions, cloned face, extra fingers, missing fingers, " +
            "blurry, low quality, watermark, distorted features");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("人像",
            "面部畸形, 面部扭曲, 肢体异常, 多余的肢体, 缺失的肢体, " +
            "手部畸形, 多余的手指, 缺失的手指, 面部绘制差, 比例失调, " +
            "克隆面孔, 变形, 丑陋, 模糊, 低质量, 水印, 特征扭曲");

        // ========== 风景类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("landscape",
            "blurry, overexposed, underexposed, noise, grain, low quality, " +
            "distorted horizon, unnatural colors, artifacts, watermark, " +
            "bad composition, flat lighting, washed out, muddy colors");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("风景",
            "模糊, 曝光过度, 曝光不足, 噪点, 颗粒感, 低质量, " +
            "地平线扭曲, 不自然的颜色, 瑕疵, 水印, " +
            "构图差, 平光, 褪色, 浑浊的颜色");

        // ========== 动漫类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("anime",
            "realistic, photorealistic, 3d render, real photo, ugly, bad anatomy, " +
            "extra fingers, missing fingers, poorly drawn hands, mutation, deformed, " +
            "blurry, low quality, watermark, western cartoon style");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("动漫",
            "写实, 真实照片, 3D渲染, 实拍, 丑陋, 解剖错误, " +
            "多余手指, 缺失手指, 手绘差, 变形, 畸形, " +
            "模糊, 低质量, 水印, 西方卡通风格");

        // ========== 写实类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("realistic",
            "cartoon, anime, illustration, painting, sketch, drawing, artistic, " +
            "low quality, blurry, unnatural, artificial, CGI, 3d render, " +
            "watermark, stylized, non-photorealistic");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("写实",
            "卡通, 动漫, 插画, 绘画, 素描, 绘制, 艺术化, " +
            "低质量, 模糊, 不自然, 人工感, CGI, 3D渲染, " +
            "水印, 风格化, 非照片级");

        // ========== 抽象类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("abstract",
            "realistic, photorealistic, detailed, concrete, literal, representational, " +
            "low quality, blurry, watermark, muddy colors, boring composition");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("抽象",
            "写实, 真实感, 具体的, 字面的, 表现主义的, " +
            "低质量, 模糊, 水印, 浑浊的颜色, 无趣的构图");

        // ========== 建筑类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("architecture",
            "distorted perspective, warped lines, unnatural proportions, tilted, " +
            "crooked, blurry, low quality, noise, artifacts, watermark, " +
            "bad geometry, impossible structure");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("建筑",
            "透视畸形, 线条扭曲, 比例失调, 倾斜, 歪斜, " +
            "模糊, 低质量, 噪点, 瑕疵, 水印, " +
            "几何错误, 不可能的结构");

        // ========== 动物类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("animal",
            "deformed, mutated, extra limbs, missing limbs, bad anatomy, " +
            "unnatural pose, incorrect anatomy, blurry, low quality, watermark, " +
            "distorted features, extra legs, missing legs");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("动物",
            "变形, 突变, 多余肢体, 缺失肢体, 解剖错误, " +
            "不自然姿势, 解剖不正确, 模糊, 低质量, 水印, " +
            "特征扭曲, 多余的腿, 缺失的腿");

        // ========== 食物类 ==========
        CATEGORY_NEGATIVE_PROMPT_MAP.put("food",
            "unappetizing, spoiled, moldy, rotten, bad presentation, " +
            "blurry, bad lighting, low quality, noise, watermark, " +
            "unnatural colors, poorly plated, messy");

        CATEGORY_NEGATIVE_PROMPT_MAP.put("食物",
            "不新鲜, 腐烂, 发霉, 变质, 摆盘差, " +
            "模糊, 光线差, 低质量, 噪点, 水印, " +
            "不自然的颜色, 摆放差, 凌乱");
    }

    /**
     * 生成负面提示词
     */
    @Tool(description = """
        Generate negative prompts based on image category to improve generation quality.

        WHAT ARE NEGATIVE PROMPTS:
        Negative prompts tell the AI what to AVOID generating, improving image quality
        by excluding unwanted elements (e.g., blurry, deformed, low quality, watermark).
        They act as guardrails to prevent common AI image generation issues.

        WHEN TO USE:
        - User specifies or implies an image category (portrait, landscape, anime, realistic, etc.)
        - User mentions quality concerns or wants professional results
        - Context suggests a specific image type that benefits from category-specific exclusions
        - User asks to "avoid" certain issues or "improve quality"

        SUPPORTED CATEGORIES (8 major types):
        1. portrait/人像: Avoid facial deformations, limb issues, distorted features
        2. landscape/风景: Avoid exposure issues, noise, color problems
        3. anime/动漫: Avoid realistic styles, western cartoon style
        4. realistic/写实: Avoid cartoon styles, artistic rendering
        5. abstract/抽象: Avoid literal interpretations, representational art
        6. architecture/建筑: Avoid perspective distortions, impossible geometry
        7. animal/动物: Avoid anatomical errors, extra/missing limbs
        8. food/食物: Avoid unappetizing appearance, poor presentation

        MATCHING STRATEGY:
        1. Exact category match (e.g., "portrait" → portrait-specific negatives)
        2. Fuzzy keyword match (e.g., input contains "cat" → animal category)
        3. Intelligent inference from context (e.g., "person" → portrait, "mountain" → landscape)
        4. Universal fallback (generic quality-improvement negatives)

        OUTPUT FORMAT:
        Comma-separated negative prompt keywords (in English or Chinese)
        These will be appended to the positive prompt to guide the AI.

        EXAMPLES:
        Input: "portrait"
        Output: "deformed face, bad anatomy, extra limbs, blurry, low quality"

        Input: "风景"
        Output: "模糊, 曝光过度, 曝光不足, 噪点, 低质量"

        Input: "a cute cat" (inferred as 'animal')
        Output: "deformed, extra limbs, bad anatomy, blurry, low quality"

        Input: unknown category
        Output: "low quality, blurry, pixelated, watermark, noise" (universal negatives)
        """)
    public String generateNegativePrompt(
            @ToolParam(description = "Image category or context (e.g., 'portrait', 'landscape', 'anime', '人像', '风景', or a description like 'a cat')")
            String categoryOrContext) {

        // 参数验证
        if (categoryOrContext == null || categoryOrContext.isBlank()) {
            log.info("NegativePromptTool: No category provided, returning universal negative prompt");
            return UNIVERSAL_NEGATIVE_PROMPT;
        }

        categoryOrContext = categoryOrContext.trim();
        log.info("NegativePromptTool: Generating negative prompt for: {}", categoryOrContext);

        // 标准化输入（小写）
        String normalizedCategory = categoryOrContext.toLowerCase();

        // 1. 精确匹配
        if (CATEGORY_NEGATIVE_PROMPT_MAP.containsKey(normalizedCategory)) {
            String negativePrompt = CATEGORY_NEGATIVE_PROMPT_MAP.get(normalizedCategory);
            log.info("NegativePromptTool: Exact match found - '{}' → {}", categoryOrContext, negativePrompt);
            return negativePrompt;
        }

        // 2. 模糊匹配（关键词包含）
        for (Map.Entry<String, String> entry : CATEGORY_NEGATIVE_PROMPT_MAP.entrySet()) {
            String keyword = entry.getKey();
            String negativePrompt = entry.getValue();

            // 双向包含检查
            if (normalizedCategory.contains(keyword) || keyword.contains(normalizedCategory)) {
                log.info("NegativePromptTool: Fuzzy match found - '{}' contains '{}' → {}",
                         categoryOrContext, keyword, negativePrompt);
                return negativePrompt;
            }
        }

        // 3. 智能推断类别
        String inferredCategory = inferCategory(normalizedCategory);
        if (inferredCategory != null) {
            String negativePrompt = CATEGORY_NEGATIVE_PROMPT_MAP.get(inferredCategory);
            log.info("NegativePromptTool: Inferred category - '{}' → '{}' → {}",
                     categoryOrContext, inferredCategory, negativePrompt);
            return negativePrompt;
        }

        // 4. 默认返回通用负面提示词
        log.info("NegativePromptTool: No specific match, returning universal negative prompt for '{}'",
                 categoryOrContext);
        return UNIVERSAL_NEGATIVE_PROMPT;
    }

    /**
     * 从上下文推断图片类别
     *
     * 分析用户输入，检测关键词来推断图片类别
     *
     * @param context 上下文描述（已标准化为小写）
     * @return 推断的类别，如果无法推断返回 null
     */
    private String inferCategory(String context) {
        // 人像类关键词
        String[] portraitKeywords = {
            "person", "face", "character", "portrait", "selfie", "人", "脸", "角色",
            "肖像", "自拍", "人物", "美女", "帅哥", "女孩", "男孩", "人像"
        };

        for (String keyword : portraitKeywords) {
            if (context.contains(keyword)) {
                return "portrait";
            }
        }

        // 风景类关键词
        String[] landscapeKeywords = {
            "nature", "scenery", "mountain", "ocean", "forest", "sky", "sunset",
            "自然", "风景", "山", "海洋", "森林", "天空", "日落", "日出",
            "landscape", "vista", "view"
        };

        for (String keyword : landscapeKeywords) {
            if (context.contains(keyword)) {
                return "landscape";
            }
        }

        // 动物类关键词
        String[] animalKeywords = {
            "cat", "dog", "pet", "bird", "animal", "猫", "狗", "宠物", "鸟",
            "动物", "野生动物", "wildlife", "horse", "马", "rabbit", "兔子"
        };

        for (String keyword : animalKeywords) {
            if (context.contains(keyword)) {
                return "animal";
            }
        }

        // 食物类关键词
        String[] foodKeywords = {
            "food", "meal", "dish", "cuisine", "dessert", "cake", "pizza",
            "食物", "菜", "美食", "甜点", "蛋糕", "披萨", "料理"
        };

        for (String keyword : foodKeywords) {
            if (context.contains(keyword)) {
                return "food";
            }
        }

        // 建筑类关键词
        String[] architectureKeywords = {
            "building", "house", "architecture", "城市", "建筑", "房子",
            "city", "urban", "structure", "tower", "塔", "桥", "bridge"
        };

        for (String keyword : architectureKeywords) {
            if (context.contains(keyword)) {
                return "architecture";
            }
        }

        // 动漫类关键词
        String[] animeKeywords = {
            "anime", "manga", "cartoon", "动漫", "漫画", "二次元",
            "卡通", "动画"
        };

        for (String keyword : animeKeywords) {
            if (context.contains(keyword)) {
                return "anime";
            }
        }

        // 写实类关键词
        String[] realisticKeywords = {
            "realistic", "photorealistic", "photo", "photograph",
            "写实", "真实", "照片", "摄影"
        };

        for (String keyword : realisticKeywords) {
            if (context.contains(keyword)) {
                return "realistic";
            }
        }

        // 抽象类关键词
        String[] abstractKeywords = {
            "abstract", "抽象", "非具象", "非写实"
        };

        for (String keyword : abstractKeywords) {
            if (context.contains(keyword)) {
                return "abstract";
            }
        }

        return null; // 无法推断
    }
}
