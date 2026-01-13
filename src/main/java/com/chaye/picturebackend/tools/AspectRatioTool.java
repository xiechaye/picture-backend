package com.chaye.picturebackend.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 纵横比/尺寸推荐工具
 *
 * 功能：根据使用场景推荐最佳图片尺寸
 * 实现方式：纯内存操作，基于关键词映射表
 * 支持场景：70+ 种中英文场景
 *
 * @author chaye
 */
@Slf4j
public class AspectRatioTool {

    /**
     * 场景到尺寸的映射表
     * 注意：阿里云 wanx-v1 支持的尺寸：1024*1024, 720*1280, 1280*720
     * 输出格式：width,height（逗号分隔）
     */
    private static final Map<String, String> SCENARIO_SIZE_MAP = new HashMap<>();

    static {
        // ========== 正方形 1:1 - 1024*1024 ==========
        // 头像类
        SCENARIO_SIZE_MAP.put("avatar", "1024,1024");
        SCENARIO_SIZE_MAP.put("头像", "1024,1024");
        SCENARIO_SIZE_MAP.put("profile picture", "1024,1024");
        SCENARIO_SIZE_MAP.put("profile photo", "1024,1024");
        SCENARIO_SIZE_MAP.put("pfp", "1024,1024");

        // Logo/图标类
        SCENARIO_SIZE_MAP.put("logo", "1024,1024");
        SCENARIO_SIZE_MAP.put("icon", "1024,1024");
        SCENARIO_SIZE_MAP.put("图标", "1024,1024");
        SCENARIO_SIZE_MAP.put("徽标", "1024,1024");
        SCENARIO_SIZE_MAP.put("标志", "1024,1024");

        // 正方形类
        SCENARIO_SIZE_MAP.put("square", "1024,1024");
        SCENARIO_SIZE_MAP.put("正方形", "1024,1024");

        // 社交媒体（正方形）
        SCENARIO_SIZE_MAP.put("instagram post", "1024,1024");
        SCENARIO_SIZE_MAP.put("instagram", "1024,1024");
        SCENARIO_SIZE_MAP.put("insta post", "1024,1024");
        SCENARIO_SIZE_MAP.put("social media", "1024,1024");
        SCENARIO_SIZE_MAP.put("社交媒体", "1024,1024");
        SCENARIO_SIZE_MAP.put("facebook post", "1024,1024");
        SCENARIO_SIZE_MAP.put("linkedin post", "1024,1024");

        // 其他正方形场景
        SCENARIO_SIZE_MAP.put("album cover", "1024,1024");
        SCENARIO_SIZE_MAP.put("专辑封面", "1024,1024");
        SCENARIO_SIZE_MAP.put("cd cover", "1024,1024");
        SCENARIO_SIZE_MAP.put("thumbnail", "1024,1024");
        SCENARIO_SIZE_MAP.put("缩略图", "1024,1024");
        SCENARIO_SIZE_MAP.put("card", "1024,1024");
        SCENARIO_SIZE_MAP.put("卡片", "1024,1024");

        // ========== 竖版 9:16 - 720*1280 ==========
        // 手机壁纸类
        SCENARIO_SIZE_MAP.put("mobile wallpaper", "720,1280");
        SCENARIO_SIZE_MAP.put("phone wallpaper", "720,1280");
        SCENARIO_SIZE_MAP.put("手机壁纸", "720,1280");
        SCENARIO_SIZE_MAP.put("手机背景", "720,1280");
        SCENARIO_SIZE_MAP.put("phone background", "720,1280");
        SCENARIO_SIZE_MAP.put("mobile background", "720,1280");
        SCENARIO_SIZE_MAP.put("锁屏壁纸", "720,1280");
        SCENARIO_SIZE_MAP.put("lock screen", "720,1280");

        // 竖版类
        SCENARIO_SIZE_MAP.put("vertical", "720,1280");
        SCENARIO_SIZE_MAP.put("竖版", "720,1280");
        SCENARIO_SIZE_MAP.put("portrait mode", "720,1280");
        SCENARIO_SIZE_MAP.put("portrait orientation", "720,1280");
        SCENARIO_SIZE_MAP.put("竖屏", "720,1280");
        SCENARIO_SIZE_MAP.put("tall", "720,1280");

        // 短视频平台（竖版）
        SCENARIO_SIZE_MAP.put("tiktok", "720,1280");
        SCENARIO_SIZE_MAP.put("抖音", "720,1280");
        SCENARIO_SIZE_MAP.put("douyin", "720,1280");
        SCENARIO_SIZE_MAP.put("instagram story", "720,1280");
        SCENARIO_SIZE_MAP.put("insta story", "720,1280");
        SCENARIO_SIZE_MAP.put("story", "720,1280");
        SCENARIO_SIZE_MAP.put("snapchat", "720,1280");
        SCENARIO_SIZE_MAP.put("reels", "720,1280");
        SCENARIO_SIZE_MAP.put("短视频", "720,1280");
        SCENARIO_SIZE_MAP.put("youtube shorts", "720,1280");
        SCENARIO_SIZE_MAP.put("shorts", "720,1280");

        // 海报类（通常竖版）
        SCENARIO_SIZE_MAP.put("poster", "720,1280");
        SCENARIO_SIZE_MAP.put("海报", "720,1280");
        SCENARIO_SIZE_MAP.put("flyer", "720,1280");
        SCENARIO_SIZE_MAP.put("传单", "720,1280");
        SCENARIO_SIZE_MAP.put("宣传单", "720,1280");

        // ========== 横版 16:9 - 1280*720 ==========
        // 电脑壁纸类
        SCENARIO_SIZE_MAP.put("desktop wallpaper", "1280,720");
        SCENARIO_SIZE_MAP.put("pc wallpaper", "1280,720");
        SCENARIO_SIZE_MAP.put("电脑桌面", "1280,720");
        SCENARIO_SIZE_MAP.put("电脑壁纸", "1280,720");
        SCENARIO_SIZE_MAP.put("桌面壁纸", "1280,720");
        SCENARIO_SIZE_MAP.put("desktop background", "1280,720");
        SCENARIO_SIZE_MAP.put("pc background", "1280,720");
        SCENARIO_SIZE_MAP.put("computer wallpaper", "1280,720");

        // 横版类
        SCENARIO_SIZE_MAP.put("landscape", "1280,720");
        SCENARIO_SIZE_MAP.put("horizontal", "1280,720");
        SCENARIO_SIZE_MAP.put("横版", "1280,720");
        SCENARIO_SIZE_MAP.put("横屏", "1280,720");
        SCENARIO_SIZE_MAP.put("landscape mode", "1280,720");
        SCENARIO_SIZE_MAP.put("landscape orientation", "1280,720");
        SCENARIO_SIZE_MAP.put("wide", "1280,720");
        SCENARIO_SIZE_MAP.put("宽屏", "1280,720");

        // Banner 类
        SCENARIO_SIZE_MAP.put("banner", "1280,720");
        SCENARIO_SIZE_MAP.put("横幅", "1280,720");
        SCENARIO_SIZE_MAP.put("header", "1280,720");
        SCENARIO_SIZE_MAP.put("网站横幅", "1280,720");
        SCENARIO_SIZE_MAP.put("web banner", "1280,720");

        // 视频缩略图类
        SCENARIO_SIZE_MAP.put("youtube thumbnail", "1280,720");
        SCENARIO_SIZE_MAP.put("video thumbnail", "1280,720");
        SCENARIO_SIZE_MAP.put("视频缩略图", "1280,720");
        SCENARIO_SIZE_MAP.put("youtube cover", "1280,720");
        SCENARIO_SIZE_MAP.put("视频封面", "1280,720");
        SCENARIO_SIZE_MAP.put("bilibili cover", "1280,720");
        SCENARIO_SIZE_MAP.put("b站封面", "1280,720");

        // 演示文稿类
        SCENARIO_SIZE_MAP.put("presentation", "1280,720");
        SCENARIO_SIZE_MAP.put("slide", "1280,720");
        SCENARIO_SIZE_MAP.put("ppt", "1280,720");
        SCENARIO_SIZE_MAP.put("演示文稿", "1280,720");
        SCENARIO_SIZE_MAP.put("幻灯片", "1280,720");

        // 封面类（通常横版）
        SCENARIO_SIZE_MAP.put("cover", "1280,720");
        SCENARIO_SIZE_MAP.put("封面", "1280,720");
        SCENARIO_SIZE_MAP.put("cover photo", "1280,720");
        SCENARIO_SIZE_MAP.put("header image", "1280,720");

        // 其他横版场景
        SCENARIO_SIZE_MAP.put("twitter header", "1280,720");
        SCENARIO_SIZE_MAP.put("facebook cover", "1280,720");
        SCENARIO_SIZE_MAP.put("linkedin banner", "1280,720");
    }

    /**
     * 推荐图片尺寸
     */
    @Tool(description = """
        Recommend optimal image dimensions based on the intended use case or scenario.

        WHEN TO USE:
        - User mentions a specific use case or platform (e.g., "mobile wallpaper", "YouTube thumbnail", "avatar")
        - User asks about image size, aspect ratio, or dimensions
        - Context indicates a preferred orientation (portrait, landscape, square)
        - User mentions a social media platform or device type

        SUPPORTED SCENARIOS (70+ cases):
        Square (1:1 - 1024x1024):
        - Avatar, profile picture, logo, icon
        - Instagram post, Facebook post, album cover
        - Thumbnail, card, social media post

        Vertical/Portrait (9:16 - 720x1280):
        - Mobile wallpaper, phone background, lock screen
        - TikTok, Instagram Story, Reels, Snapchat, YouTube Shorts
        - Poster, flyer, vertical content

        Horizontal/Landscape (16:9 - 1280x720):
        - Desktop wallpaper, PC background
        - YouTube thumbnail, video cover, presentation slide
        - Banner, header, website cover
        - Twitter header, Facebook cover

        MATCHING STRATEGY:
        1. Exact match (e.g., "mobile wallpaper" → "720,1280")
        2. Fuzzy match (e.g., "phone" contains "mobile" → "720,1280")
        3. Orientation inference (e.g., "vertical" → "720,1280")
        4. Default fallback (square: "1024,1024")

        OUTPUT FORMAT:
        Comma-separated dimensions: "width,height"
        Example: "1280,720" (NOT "1280*720" - use comma, not asterisk)

        EXAMPLES:
        Input: "mobile wallpaper" → Output: "720,1280"
        Input: "YouTube thumbnail" → Output: "1280,720"
        Input: "avatar" → Output: "1024,1024"
        Input: "手机壁纸" → Output: "720,1280"
        Input: "电脑桌面" → Output: "1280,720"
        """)
    public String recommendSize(
            @ToolParam(description = "Use case, scenario, or platform (e.g., 'mobile wallpaper', 'YouTube thumbnail', 'avatar', '手机壁纸')")
            String scenario) {

        // 参数验证
        if (scenario == null || scenario.isBlank()) {
            log.warn("AspectRatioTool: Scenario is null or blank, returning default size");
            return "1024,1024"; // 默认正方形
        }

        scenario = scenario.trim();
        log.info("AspectRatioTool: Recommending size for scenario: {}", scenario);

        // 转小写进行匹配（保留原值用于日志）
        String normalizedScenario = scenario.toLowerCase();

        // 1. 精确匹配
        if (SCENARIO_SIZE_MAP.containsKey(normalizedScenario)) {
            String size = SCENARIO_SIZE_MAP.get(normalizedScenario);
            log.info("AspectRatioTool: Exact match found - '{}' → {}", scenario, size);
            return size;
        }

        // 2. 模糊匹配（关键词包含）
        for (Map.Entry<String, String> entry : SCENARIO_SIZE_MAP.entrySet()) {
            String keyword = entry.getKey();
            String size = entry.getValue();

            // 双向包含检查
            if (normalizedScenario.contains(keyword) || keyword.contains(normalizedScenario)) {
                log.info("AspectRatioTool: Fuzzy match found - '{}' contains '{}' → {}",
                         scenario, keyword, size);
                return size;
            }
        }

        // 3. 基于方向关键词推断
        String inferredSize = inferSizeByOrientation(normalizedScenario);
        if (inferredSize != null) {
            log.info("AspectRatioTool: Orientation inferred - '{}' → {}", scenario, inferredSize);
            return inferredSize;
        }

        // 4. 默认返回正方形
        log.info("AspectRatioTool: No match found, returning default square size for '{}'", scenario);
        return "1024,1024";
    }

    /**
     * 根据方向关键词推断尺寸
     *
     * @param normalizedScenario 标准化后的场景描述（小写）
     * @return 推断的尺寸，如果无法推断返回 null
     */
    private String inferSizeByOrientation(String normalizedScenario) {
        // 竖版/纵向关键词
        String[] verticalKeywords = {
            "vertical", "portrait", "竖", "mobile", "phone", "手机",
            "tall", "long", "纵向", "竖屏"
        };

        for (String keyword : verticalKeywords) {
            if (normalizedScenario.contains(keyword)) {
                return "720,1280";
            }
        }

        // 横版/横向关键词
        String[] horizontalKeywords = {
            "horizontal", "landscape", "横", "desktop", "pc", "电脑",
            "wide", "宽", "横向", "横屏", "computer"
        };

        for (String keyword : horizontalKeywords) {
            if (normalizedScenario.contains(keyword)) {
                return "1280,720";
            }
        }

        // 正方形关键词
        String[] squareKeywords = {
            "square", "正方", "方形", "1:1"
        };

        for (String keyword : squareKeywords) {
            if (normalizedScenario.contains(keyword)) {
                return "1024,1024";
            }
        }

        return null; // 无法推断
    }

    /**
     * 将逗号分隔的尺寸格式转换为阿里云 API 所需的星号分隔格式
     *
     * @param dimensions 逗号分隔的尺寸（如 "1280,720"）
     * @return 星号分隔的尺寸（如 "1280*720"）
     */
    public static String convertToApiFormat(String dimensions) {
        if (dimensions == null || dimensions.isBlank()) {
            return "1024*1024";
        }
        return dimensions.replace(",", "*");
    }
}
