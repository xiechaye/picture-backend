package com.chaye.picturebackend.agent;

import com.chaye.picturebackend.PictureBackendApplication;
import com.chaye.picturebackend.model.enums.AgentEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图像生成Agent测试类
 */
@SpringBootTest(classes = PictureBackendApplication.class)
class ImageGenerationAgentTest {

    @Autowired
    private ImageGenerationAgent imageGenerationAgent;

    /**
     * 测试Agent初始化
     */
    @Test
    void testAgentInitialization() {
        assertNotNull(imageGenerationAgent);
        assertEquals("imageGenerationAgent", imageGenerationAgent.getName());
        assertEquals(AgentEnum.IDLE, imageGenerationAgent.getAgentEnum());
        assertEquals(3, imageGenerationAgent.getMaxStep());
        assertNotNull(imageGenerationAgent.getSystemPrompt());
        assertTrue(imageGenerationAgent.getSystemPrompt().contains("AI Image Prompt Engineer"));
    }

    /**
     * 测试完整的图像生成流程
     * 注意：这是一个集成测试，会实际调用阿里云API，需要配置有效的API Key
     */
    @Test
    void testImageGenerationFlow() {
        // 用户的简单描述
        String userPrompt = "一只可爱的小猫咪";

        // 执行Agent并获取Flux流
        Flux<String> resultFlux = imageGenerationAgent.run(userPrompt);

        // 使用StepVerifier测试Flux流
        StepVerifier.create(resultFlux)
                .expectNextCount(3) // 期望3个步骤：提示词优化 -> 图像生成 -> 上传
                .expectComplete()
                .verify(Duration.ofSeconds(120));

        // 验证Agent状态
        AgentEnum finalStatus = imageGenerationAgent.getAgentEnum();
        assertTrue(
                finalStatus == AgentEnum.FINISHED || finalStatus == AgentEnum.ERROR,
                "Agent should be in FINISHED or ERROR state, current state: " + finalStatus
        );

        // 如果成功完成，验证步骤计数
        if (finalStatus == AgentEnum.FINISHED) {
            assertTrue(
                    imageGenerationAgent.getCurrentStep() <= imageGenerationAgent.getMaxStep(),
                    "Execution steps should not exceed maximum steps"
            );
        }
    }

    /**
     * 测试空输入处理
     */
    @Test
    void testEmptyPrompt() {
        Flux<String> resultFlux = imageGenerationAgent.run("");

        // 验证是否发出错误消息
        StepVerifier.create(resultFlux)
                .expectNext("Error: Cannot run agent with empty prompt.")
                .expectComplete()
                .verify(Duration.ofSeconds(5));

        // 验证Agent仍处于空闲状态（应拒绝空输入）
        assertEquals(AgentEnum.IDLE, imageGenerationAgent.getAgentEnum());
    }

    /**
     * 测试简单的图像生成（快速验证）
     * 使用简短的prompt减少测试时间
     */
    @Test
    void testQuickImageGeneration() {
        String userPrompt = "红色苹果";

        Flux<String> resultFlux = imageGenerationAgent.run(userPrompt);

        // 计数结果并打印
        AtomicInteger stepCount = new AtomicInteger(0);

        StepVerifier.create(resultFlux.doOnNext(result -> {
            stepCount.incrementAndGet();
            System.out.println("Step " + stepCount.get() + ": " + result);
        }))
                .expectNextCount(3)
                .expectComplete()
                .verify(Duration.ofSeconds(120));

        // 打印最终状态
        System.out.println("Final Agent status: " + imageGenerationAgent.getAgentEnum());
        System.out.println("Execution steps: " + imageGenerationAgent.getCurrentStep());
    }

    /**
     * 测试Agent状态管理
     */
    @Test
    void testAgentStateManagement() {
        // 初始状态应为IDLE
        assertEquals(AgentEnum.IDLE, imageGenerationAgent.getAgentEnum());

        // 运行Agent会改变状态
        Flux<String> resultFlux = imageGenerationAgent.run("测试");

        // 订阅以开始执行
        resultFlux.subscribe();

        // 短暂等待以确保状态改变
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 状态不应再是IDLE
        assertNotEquals(AgentEnum.IDLE, imageGenerationAgent.getAgentEnum());
    }

    /**
     * 测试详细的提示词优化
     * 这个测试展示了从简单描述到详细prompt的转换过程
     */
    @Test
    void testDetailedPromptOptimization() {
        StringBuilder allResults = new StringBuilder();

        String userPrompt = "夕阳下的海滩";

        Flux<String> resultFlux = imageGenerationAgent.run(userPrompt);

        // 打印每个步骤的结果
        StepVerifier.create(resultFlux.doOnNext(result -> {
            allResults.append(result).append("\n");
        }))
                .expectNextCount(3)
                .expectComplete()
                .verify(Duration.ofSeconds(120));

        System.out.println("\n=== Complete Execution Flow ===");
        System.out.println("Original input: " + userPrompt);
        System.out.println("\nStep results:");
        System.out.println(allResults);
        System.out.println("\nFinal status: " + imageGenerationAgent.getAgentEnum());
    }
}
