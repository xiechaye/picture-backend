package com.chaye.picturebackend.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.chaye.picturebackend.model.enums.AgentEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用代理
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class ToolCallAgent extends ReActAgent{
    // 可调用工具
    private final ToolCallback[] availableTools;

    // 工具调用结果
    private ChatResponse toolCallChatResponse;

    // 工具调用管理器
    private final ToolCallingManager toolCallingManager;

//    //  可调用的MCP服务
//    private ToolCallbackProvider toolCallbackProvider;

    // 思考文本
    private String thinkText;

    // 禁用工具自动调用，手动管理上下文
    private ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools, ToolCallbackProvider toolCallbackProvider) {
        super();
        // 设置可调用工具
        this.availableTools = availableTools;
//        this.toolCallbackProvider = toolCallbackProvider;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用Spring AI的工具自动调用，自己维护选项消息和上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        // 判断ai是否有对下一步的提示词
        if(getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            getMessageList().add(new UserMessage(getNextStepPrompt()));
        }
        List<Message> messageList = getMessageList();

        Prompt prompt = new Prompt(messageList, chatOptions);

        try {
            // 调用ai进行思考，是否需要使用工具
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .tools(availableTools)
//                    .tools(toolCallbackProvider)
                    .call()
                    .chatResponse();

            // 记录ai的思考结果
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

            // 记录日志
            String text = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
            log.info(getName() + "思考结果: " + text);
            log.info(getName() + "决定调用"+ toolCalls.size() + "个工具");
            // 记录思考结果
            thinkText = text;
            String toolCallInfo = toolCalls.stream()
                    .map(toolCall -> String.format("工具名称: %s, 参数: %s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);

            // 如果不需要调用工具就直接将结果添加到上下文
            if(toolCalls.isEmpty()) {
                getMessageList().add(assistantMessage);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("思考时出现错误: " + e.getMessage());
            // 发生异常，添加异常信息到上下文
            getMessageList().add(new UserMessage("思考时出现错误: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        // 判断是否有工具可以调用
        if(!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }

        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        // 将工具调用结果添加到上下文
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String result = toolResponseMessage.getResponses().stream()
                .map(response -> String.format("调用工具：%s", response.name()))
                .collect(Collectors.joining("\n"));
        log.info(result);

        // 是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));

        // 如果调用了终止工具，设置代理状态为FINISHED
        if(terminateToolCalled) {
            setAgentEnum(AgentEnum.FINISHED);
        }

        return thinkText;
    }
}
