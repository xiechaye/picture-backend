package com.chaye.picturebackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chaye
 */
@Configuration
public class RabbitMQConfig {

    // 1. 定义交换机名称 (建议：业务名_exchange)
    public static final String IMAGE_EXCHANGE_NAME = "picture_exchange";

    // 2. 定义队列名称 (建议：业务名_queue)
    public static final String IMAGE_QUEUE_NAME = "image_process_queue";

    // 3. 定义路由键 (Routing Key)
    public static final String IMAGE_ROUTING_KEY = "image.process";

    /**
     * 声明交换机 (Direct模式：精准匹配)
     * durable(true): 重启后交换机还在
     */
    @Bean
    public DirectExchange imageExchange() {
        return new DirectExchange(IMAGE_EXCHANGE_NAME, true, false);
    }

    /**
     * 声明队列
     * durable(true): 重启后队列还在，消息不丢失
     */
    @Bean
    public Queue imageQueue() {
        return new Queue(IMAGE_QUEUE_NAME, true);
    }

    /**
     * 绑定：将队列绑定到交换机上
     * 只有 RoutingKey 符合 "image.process" 的消息，才会被投递到这个队列
     */
    @Bean
    public Binding bindingImageProcess() {
        return BindingBuilder.bind(imageQueue()).to(imageExchange()).with(IMAGE_ROUTING_KEY);
    }

    /**
     * 序列化
     */
    @Bean
    MessageConverter createMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
