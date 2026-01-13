package com.chaye.picturebackend.config;

import com.chaye.picturebackend.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置类
 * 为工具类和其他组件提供全局 ChatClient bean
 *
 * @author chaye
 */
@Configuration
public class ChatClientConfig {

    /**
     * 创建全局 ChatClient bean
     *
     * @param chatModel Spring AI Alibaba 自动配置的 ChatModel
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }
}
