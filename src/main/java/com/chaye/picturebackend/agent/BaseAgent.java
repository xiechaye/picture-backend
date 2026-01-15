package com.chaye.picturebackend.agent;

import com.chaye.picturebackend.model.enums.AgentEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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
     * 运行智能体并返回Flux流
     * @param userPrompt 用户输入的提示词
     * @return 执行结果的Flux流
     */
    public Flux<String> run(String userPrompt) {
        return Flux.create(sink -> {
            // 检查智能体是否空闲
            if(agentEnum != AgentEnum.IDLE) {
                sink.next("Error: Cannot run agent from state: " + agentEnum);
                sink.complete();
                return;
            }

            // 检查用户提示词是否为空
            if(StringUtils.isBlank(userPrompt)) {
                sink.next("Error: Cannot run agent with empty prompt.");
                sink.complete();
                return;
            }

            // 将用户消息添加到上下文
            messageList.add(new UserMessage(userPrompt));

            // 设置状态为运行中
            agentEnum = AgentEnum.RUNNING;

            try {
                while(currentStep < maxStep && agentEnum != AgentEnum.FINISHED) {
                    currentStep += 1;
                    log.info("Executing step: " + currentStep + "/" + maxStep);
                    String result = step();
                    log.info("Step result: " + result);

                    // 为每个步骤发出结果
                    sink.next(result);
                }

                // 达到最大步数
                if(currentStep >= maxStep) {
                    agentEnum = AgentEnum.FINISHED;
                    sink.next("Terminated: Reached max steps (" + maxStep + ")");
                }

                sink.complete();
            } catch (Exception e) {
                agentEnum = AgentEnum.ERROR;
                log.error("Error during agent execution", e);
                sink.error(e);
            } finally {
                cleanup();
            }
        });
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
