package com.chaye.picturebackend.listener;

import com.chaye.picturebackend.PictureBackendApplication;
import com.chaye.picturebackend.config.RabbitMQConfig;
import com.chaye.picturebackend.model.entity.Picture;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PictureBackendApplication.class)
class ImageMqListenerTest {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void testSendMessage() throws InterruptedException {
        // 1. 构造一个模拟的 Picture 对象
        Picture picture = new Picture();
        picture.setId(8888L);
        picture.setUrl("https://picture-1327683584.cos.ap-guangzhou.myqcloud.com/space/2008460319105581057/2026-01-06_MDMIbJ28F3JqDhn6.webp");
        picture.setName("单元测试图片");
        picture.setSpaceId(1L);

        // 2. 发送消息到交换机
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.IMAGE_EXCHANGE_NAME, // 交换机名称
                RabbitMQConfig.IMAGE_ROUTING_KEY,   // 路由键
                picture                             // 消息体
        );

        System.out.println("消息已发送，请观察控制台日志...");

        // 3. 阻塞一会儿，因为消息处理是异步的。
        // 如果不加这个，测试主线程结束，程序就关闭了，Listener 可能还没来得及跑。
        Thread.sleep(10000); // 等待 10 秒
    }
}