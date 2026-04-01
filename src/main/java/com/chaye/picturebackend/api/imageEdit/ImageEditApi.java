package com.chaye.picturebackend.api.imageEdit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.chaye.picturebackend.api.imageEdit.model.CreateImageEditTaskRequest;
import com.chaye.picturebackend.api.imageEdit.model.CreateImageEditTaskResponse;
import com.chaye.picturebackend.api.imageEdit.model.GetImageEditTaskResponse;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 阿里云万相图像编辑 API
 * 支持功能：抠图、去水印、图片增强、扩图等
 */
@Slf4j
@Component
public class ImageEditApi {

    /**
     * 创建图像编辑任务地址
     */
    public static final String CREATE_IMAGE_EDIT_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis";

    /**
     * 查询任务状态
     */
    public static final String GET_IMAGE_EDIT_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    /**
     * 创建图像编辑任务
     *
     * @param request 编辑请求
     * @return 任务响应
     */
    public CreateImageEditTaskResponse createTask(CreateImageEditTaskRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图像编辑参数为空");
        }

        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_IMAGE_EDIT_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                // 必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(request));

        // 处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("图像编辑请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像编辑失败");
            }

            CreateImageEditTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateImageEditTaskResponse.class);
            if (response.getCode() != null) {
                String errorMessage = response.getMessage();
                log.error("图像编辑请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像编辑失败，" + errorMessage);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图像编辑请求异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像编辑失败");
        }
    }

    /**
     * 查询图像编辑任务结果
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    public GetImageEditTaskResponse getTaskResult(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 ID 不能为空");
        }

        String url = String.format(GET_IMAGE_EDIT_TASK_URL, taskId);
        try (HttpResponse httpResponse = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("查询图像编辑任务异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图像编辑任务结果失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetImageEditTaskResponse.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询图像编辑任务异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图像编辑任务结果失败");
        }
    }
}
