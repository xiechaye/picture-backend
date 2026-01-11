package com.chaye.picturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.chaye.picturebackend.api.aliyunai.model.CreateImageTaskRequest;
import com.chaye.picturebackend.api.aliyunai.model.CreateImageTaskResponse;
import com.chaye.picturebackend.api.aliyunai.model.GetImageTaskResponse;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImageGenerationApi {

    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建图像生成任务地址
    public static final String CREATE_IMAGE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

    // 查询任务状态
    public static final String GET_IMAGE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";


    /**
     * 创建图像生成任务
     *
     * @param createImageTaskRequest
     * @return
     */
    public CreateImageTaskResponse createImageTask(CreateImageTaskRequest createImageTaskRequest) {
        if (createImageTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图像生成参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_IMAGE_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createImageTaskRequest));
        // 处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像生成失败");
            }
            CreateImageTaskResponse createImageTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateImageTaskResponse.class);
            if (createImageTaskResponse.getCode() != null) {
                String errorMessage = createImageTaskResponse.getMessage();
                log.error("请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像生成失败，" + errorMessage);
            }
            return createImageTaskResponse;
        }
    }

    /**
     * 查询图像生成任务结果
     *
     * @param taskId
     * @return
     */
    public GetImageTaskResponse getImageTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }
        // 处理响应
        String url = String.format(GET_IMAGE_TASK_URL, taskId);
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetImageTaskResponse.class);
        }
    }
}
