package com.chaye.picturebackend.listener;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.chaye.picturebackend.config.RabbitMQConfig;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.model.entity.Picture;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@Component
@Slf4j
public class ImageMqListener {

    private final VectorStore vectorStore;
    private final COSClient cosClient; // 注入 COS 客户端用于生成签名

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Value("${cos.client.bucket}")
    private String bucketName;

    public ImageMqListener(VectorStore vectorStore, COSClient cosClient) {
        this.vectorStore = vectorStore;
        this.cosClient = cosClient;
    }

    @RabbitListener(queues = RabbitMQConfig.IMAGE_QUEUE_NAME, ackMode = "MANUAL")
    public void onMessage(Picture picture, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        long pictureId = picture.getId();
        log.info("处理图片消息 ID: {}", pictureId);

        try {
            if (!StringUtils.hasText(picture.getUrl())) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 1. 生成预签名 URL (关键步骤)
            // 这个 URL 包含了鉴权信息，有效期内任何人都可访问（包括阿里千问），无需将桶设为公有
            String signedUrl = generatePresignedUrl(picture.getUrl());
            log.info("已生成临时授权 URL，准备请求 AI");

            // 2. 调用阿里原生 SDK 进行分析
            String aiDescription = callDashScopeSdk(signedUrl);
            log.info("AI 分析完成，长度: {}", aiDescription.length());

            // 3. 向量入库
            Document document = buildDocument(picture, aiDescription);
            vectorStore.add(List.of(document));

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理失败，图片ID: {}", pictureId, e);
            try {
                // 遇到 AI 报错不建议立刻重试，避免死循环，根据业务决定是 Nack 还是记录日志后 Ack
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }

    /**
     * 使用阿里官方 SDK 调用 Qwen-VL
     */
    private String callDashScopeSdk(String imageUrl) {
        try {
            MultiModalConversation conv = new MultiModalConversation();

            // 构建消息体：包含图片 URL 和 提示词
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("image", imageUrl);

            Map<String, Object> textMap = new HashMap<>();
            textMap.put("text", "请详细描述这张图片的内容，包括主体、背景、颜色和氛围。");

            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(imageMap, textMap))
                    .build();

            // 构建参数
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(dashScopeApiKey) // 显式传入 Key
                    .model("qwen-vl-max")    // 使用效果最好的模型
                    .messages(Collections.singletonList(userMessage))
                    .topP(0.8)               // 可选：生成参数微调
                    .build();

            // 发起调用
            MultiModalConversationResult result = conv.call(param);

            // 解析结果
            if (result != null && result.getOutput() != null && !result.getOutput().getChoices().isEmpty()) {
                // 官方 SDK 返回结构提取
                return (String) result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text");
            }

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 响应为空");

        } catch (ApiException | com.alibaba.dashscope.exception.NoApiKeyException | com.alibaba.dashscope.exception.UploadFileException e) {
            log.error("DashScope SDK 调用异常: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务异常: " + e.getMessage());
        }
    }

    /**
     * 生成腾讯云 COS 预签名 URL (有效期 1 小时)
     * 解决防盗链和私有桶无法直接访问的问题
     */
    private String generatePresignedUrl(String originalUrl) {
        try {
            URL url = new URL(originalUrl);
            String key = url.getPath();
            // 去掉开头的 /
            if (key.startsWith("/")) {
                key = key.substring(1);
            }

            // 设置过期时间
            Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);

            // 生成 URL
            URL signedUrl = cosClient.generatePresignedUrl(bucketName, key, expiration, HttpMethodName.GET);
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("生成签名 URL 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成签名 URL 失败");
        }
    }

    private Document buildDocument(Picture picture, String description) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", picture.getId());
        metadata.put("url", picture.getUrl());
        metadata.put("name", picture.getName());
        metadata.put("spaceId", picture.getSpaceId());
        return new Document(description, metadata);
    }
}