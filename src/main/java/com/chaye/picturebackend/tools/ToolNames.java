package com.chaye.picturebackend.tools;

/**
 * 工具名称常量
 *
 * 定义 Agent 可调用工具的名称常量，避免硬编码
 * 工具名称与 @Tool 注解的方法名一致
 *
 * @author chaye
 */
public final class ToolNames {

    private ToolNames() {
        // 私有构造函数，防止实例化
    }

    /**
     * Prompt 增强工具
     * 对应 PromptEnhancerTool.enhancePrompt()
     */
    public static final String ENHANCE_PROMPT = "enhancePrompt";

    /**
     * 尺寸推荐工具
     * 对应 AspectRatioTool.recommendSize()
     */
    public static final String RECOMMEND_SIZE = "recommendSize";

    /**
     * 负面提示词生成工具
     * 对应 NegativePromptTool.generateNegativePrompt()
     */
    public static final String GENERATE_NEGATIVE_PROMPT = "generateNegativePrompt";

    /**
     * 终止任务工具
     * 对应 TerminateTool.doTerminate()
     */
    public static final String DO_TERMINATE = "doTerminate";
}
