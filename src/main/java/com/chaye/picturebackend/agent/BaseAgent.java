package com.chaye.picturebackend.agent;

import com.chaye.picturebackend.model.enums.AgentEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Agent 基类
 */
@Slf4j
@Data
public abstract class BaseAgent {
    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态（默认为空闲）
    private AgentEnum agentEnum = AgentEnum.IDLE;

    // 执行控制
    private int currentStep = 0;
    private int maxStep = 10;

    // 大语言模型
    private ChatClient chatClient;

    // 内存（自维护的上下文）
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行智能体
     * @param userPrompt 用户输入的提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (this.agentEnum != AgentEnum.IDLE) {
            throw new RuntimeException("Cannot run agent from agentEnum: " + this.agentEnum);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 更改状态  
        agentEnum = AgentEnum.RUNNING;
        // 记录消息上下文  
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表  
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxStep && agentEnum != AgentEnum.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxStep);
                // 单步执行  
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制  
            if (currentStep >= maxStep) {
                agentEnum = AgentEnum.FINISHED;
                results.add("Terminated: Reached max steps (" + maxStep + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            agentEnum = AgentEnum.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 清理资源  
            this.cleanup();
        }
    }

    /**
     * 执行单个步骤
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 重置状态
        this.agentEnum = AgentEnum.IDLE;
        this.currentStep = 0;

        // 清理运行时数据
        // 注意：不清空 systemPrompt 和 nextStepPrompt，因为它们是配置性数据，在构造函数中设置

        // 清理消息列表（重新初始化而不是设为null，避免NPE）
        this.messageList = new ArrayList<>();
    }
}
