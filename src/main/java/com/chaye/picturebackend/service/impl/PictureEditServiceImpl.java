package com.chaye.picturebackend.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chaye.picturebackend.api.aliyunai.model.Wan27ImageGenerationRequest;
import com.chaye.picturebackend.api.aliyunai.model.Wan27ImageGenerationRequest.Input;
import com.chaye.picturebackend.api.aliyunai.model.Wan27ImageGenerationRequest.Message;
import com.chaye.picturebackend.api.aliyunai.model.Wan27ImageGenerationRequest.Parameters;
import com.chaye.picturebackend.api.aliyunai.model.Wan27ImageGenerationResponse;
import com.chaye.picturebackend.api.imageEdit.ImageEditApiWan27;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.exception.ThrowUtils;
import com.chaye.picturebackend.manager.CosManager;
import com.chaye.picturebackend.manager.auth.SpaceUserAuthManager;
import com.chaye.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.chaye.picturebackend.mapper.PictureEditTaskMapper;
import com.chaye.picturebackend.mapper.PictureMapper;
import com.chaye.picturebackend.model.dto.pictureedit.PictureEnhanceRequest;
import com.chaye.picturebackend.model.dto.pictureedit.PictureRemoveWatermarkRequest;
import com.chaye.picturebackend.model.dto.pictureedit.PictureSegmentRequest;
import com.chaye.picturebackend.model.entity.Picture;
import com.chaye.picturebackend.model.entity.PictureEditTask;
import com.chaye.picturebackend.model.entity.Space;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.model.enums.PictureEditTaskStatusEnum;
import com.chaye.picturebackend.model.enums.PictureEditTypeEnum;
import com.chaye.picturebackend.model.vo.PictureEditTaskVO;
import com.chaye.picturebackend.service.PictureEditService;
import com.chaye.picturebackend.service.SpaceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 图片编辑服务实现
 * <p>
 * 已升级支持 wan2.7-image-pro 统一模型
 */
@Slf4j
@Service
public class PictureEditServiceImpl extends ServiceImpl<PictureEditTaskMapper, PictureEditTask>
        implements PictureEditService {

    @Resource
    private ImageEditApiWan27 imageEditApiWan27;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Value("${cos.client.host:https://picture-xxx.cos.ap-guangzhou.myqcloud.com}")
    private String cosPrefix;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PictureEditTaskVO segmentImage(PictureSegmentRequest request, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPictureId() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");

        // 获取原图信息
        Picture picture = pictureMapper.selectById(request.getPictureId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 权限校验
        checkEditPermission(picture, loginUser);

        // 获取空间信息
        Space space = null;
        if (picture.getSpaceId() != null) {
            space = spaceService.getById(picture.getSpaceId());
        }

        // 生成预签名URL（如果图片是私有的）
        String imageUrl = getImageUrl(picture);

        // 使用 wan2.7-image-pro 进行抠图
        return segmentImageWan27(request, picture, imageUrl, loginUser);
    }

    /**
     * 使用 wan2.7-image-pro 进行抠图
     */
    private PictureEditTaskVO segmentImageWan27(PictureSegmentRequest request, Picture picture,
                                                String imageUrl, User loginUser) {
        // 构建请求
        Wan27ImageGenerationRequest apiRequest = buildWan27Request();

        // 构建消息内容 - 使用 Map 格式确保正确的 JSON 序列化
        Wan27ImageGenerationRequest.Input input = new Wan27ImageGenerationRequest.Input();

        Wan27ImageGenerationRequest.Message message = new Wan27ImageGenerationRequest.Message();

        // 构建 content 列表，每个元素是一个 Map
        List<java.util.Map<String, String>> contentList = new ArrayList<>();

        // 注意：content 数组中，图片（image）必须放在前面，提示词（text）必须放在后面
        // 使用公网 URL 方式上传图片（参考 suggestion.md 官方文档）
        contentList.add(java.util.Collections.singletonMap("image", imageUrl));

        // 再添加文本提示词
        String prompt = buildSegmentPromptWan27(request.getType(), request.getBbox());
        contentList.add(java.util.Collections.singletonMap("text", prompt));

        message.setContent(contentList);
        input.setMessages(java.util.Collections.singletonList(message));
        apiRequest.setInput(input);

        // 设置框选区域（如果有）
        if (request.getBbox() != null && request.getBbox().size() == 4) {
            List<List<List<Integer>>> bboxList = new ArrayList<>();
            List<List<Integer>> singleImageBboxes = new ArrayList<>();
            singleImageBboxes.add(new ArrayList<>(request.getBbox()));
            bboxList.add(singleImageBboxes);
            apiRequest.getParameters().setBboxList(bboxList);
        }

        // 记录请求 JSON 用于调试
        String requestJson = JSONUtil.toJsonStr(apiRequest);
        log.info("wan2.7-image-pro 请求 JSON: {}", requestJson);

        // 创建任务
        return createEditTaskWan27(apiRequest, picture.getId(), loginUser.getId(),
                PictureEditTypeEnum.SEGMENT.getValue(), request);
    }

    /**
     * 使用 wan2.7-image-pro 进行去水印
     */
    private PictureEditTaskVO removeWatermarkWan27(PictureRemoveWatermarkRequest request, Picture picture,
                                                    String imageUrl, User loginUser) {
        // 构建请求
        Wan27ImageGenerationRequest apiRequest = buildWan27Request();

        Wan27ImageGenerationRequest.Input input = new Wan27ImageGenerationRequest.Input();
        Wan27ImageGenerationRequest.Message message = new Wan27ImageGenerationRequest.Message();

        // 构建消息内容 - 图片在前，提示词在后
        List<java.util.Map<String, String>> contentList = new ArrayList<>();
        contentList.add(java.util.Collections.singletonMap("image", imageUrl));

        // 构建 prompt（使用自然语言描述去水印操作）
        String prompt = buildRemoveWatermarkPromptWan27();
        contentList.add(java.util.Collections.singletonMap("text", prompt));

        message.setContent(contentList);
        input.setMessages(java.util.Collections.singletonList(message));
        apiRequest.setInput(input);

        // 记录请求 JSON 用于调试
        log.info("wan2.7-image-pro 去水印请求 JSON: {}", JSONUtil.toJsonStr(apiRequest));

        // 创建任务
        return createEditTaskWan27(apiRequest, picture.getId(), loginUser.getId(),
                PictureEditTypeEnum.REMOVE_WATERMARK.getValue(), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PictureEditTaskVO removeWatermark(PictureRemoveWatermarkRequest request, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPictureId() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");

        // 获取原图信息
        Picture picture = pictureMapper.selectById(request.getPictureId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 权限校验
        checkEditPermission(picture, loginUser);

        // 获取图片URL
        String imageUrl = getImageUrl(picture);

        // 使用 wan2.7-image-pro 进行去水印
        return removeWatermarkWan27(request, picture, imageUrl, loginUser);
    }

    /**
     * 使用 wan2.7-image-pro 进行图像增强
     */
    private PictureEditTaskVO enhanceImageWan27(PictureEnhanceRequest request, Picture picture,
                                                String imageUrl, User loginUser) {
        // 构建请求
        Wan27ImageGenerationRequest apiRequest = buildWan27Request();

        Wan27ImageGenerationRequest.Input input = new Wan27ImageGenerationRequest.Input();
        Wan27ImageGenerationRequest.Message message = new Wan27ImageGenerationRequest.Message();

        // 构建消息内容 - 图片在前，提示词在后
        List<java.util.Map<String, String>> contentList = new ArrayList<>();
        contentList.add(java.util.Collections.singletonMap("image", imageUrl));

        // 根据增强类型构建详细 prompt
        String prompt = buildEnhancePromptWan27(request.getEnhanceType());
        contentList.add(java.util.Collections.singletonMap("text", prompt));

        message.setContent(contentList);
        input.setMessages(java.util.Collections.singletonList(message));
        apiRequest.setInput(input);

        // 记录请求 JSON 用于调试
        log.info("wan2.7-image-pro 增强请求 JSON: {}", JSONUtil.toJsonStr(apiRequest));

        // 创建任务
        return createEditTaskWan27(apiRequest, picture.getId(), loginUser.getId(),
                PictureEditTypeEnum.ENHANCE.getValue(), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PictureEditTaskVO enhanceImage(PictureEnhanceRequest request, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPictureId() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getEnhanceType()), ErrorCode.PARAMS_ERROR, "增强类型不能为空");

        // 获取原图信息
        Picture picture = pictureMapper.selectById(request.getPictureId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 权限校验
        checkEditPermission(picture, loginUser);

        // 获取图片URL
        String imageUrl = getImageUrl(picture);

        // 使用 wan2.7-image-pro 进行图像增强
        return enhanceImageWan27(request, picture, imageUrl, loginUser);
    }

    @Override
    public PictureEditTaskVO getTaskStatus(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        // 查询任务记录
        PictureEditTask task = lambdaQuery()
                .eq(PictureEditTask::getTaskId, taskId)
                .one();

        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        // 如果任务还在处理中，尝试更新状态
        if (PictureEditTaskStatusEnum.PROCESSING.getValue().equals(task.getStatus())) {
            pollAndUpdateTaskStatus(taskId);
            task = getById(task.getId());
        }

        return convertToVO(task);
    }

    @Override
    public boolean pollAndUpdateTaskStatus(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");

        // 查询任务记录
        PictureEditTask task = lambdaQuery()
                .eq(PictureEditTask::getTaskId, taskId)
                .one();

        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "任务不存在");

        // 如果任务已完成，不再查询
        if (!PictureEditTaskStatusEnum.PROCESSING.getValue().equals(task.getStatus())) {
            return true;
        }

        try {
            // 使用 wan2.7-image-pro SDK 查询任务状态
            return pollAndUpdateTaskStatusWan27(taskId, task);
        } catch (Exception e) {
            log.error("查询编辑任务状态失败，taskId: {}", taskId, e);
            return false;
        }
    }

    /**
     * 查询任务状态（wan2.7-image-pro 新版，使用 SDK）
     */
    private boolean pollAndUpdateTaskStatusWan27(String taskId, PictureEditTask task) {
        com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult sdkResult = imageEditApiWan27.getTaskResult(taskId);

        // 检查任务状态
        if (imageEditApiWan27.isFinished(sdkResult)) {
            if (imageEditApiWan27.isFailed(sdkResult)) {
                // 任务失败
                task.setStatus(PictureEditTaskStatusEnum.FAILED.getValue());
                task.setErrorMessage(imageEditApiWan27.getErrorMessage(sdkResult));
                updateById(task);
                return true;
            }

            // 任务成功，获取结果URL
            String resultUrl = imageEditApiWan27.getFirstImageUrl(sdkResult);
            if (resultUrl != null) {
                // 下载并上传到 COS
                String cosUrl = downloadAndUploadToCos(resultUrl, task.getEditType());

                // 更新任务状态
                task.setStatus(PictureEditTaskStatusEnum.SUCCESS.getValue());
                task.setResultUrl(cosUrl);
                updateById(task);
                return true;
            }
        }

        // 任务还在处理中
        return false;
    }

    /**
     * 校验编辑权限
     */
    private void checkEditPermission(Picture picture, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Space space = null;
        if (picture.getSpaceId() != null) {
            space = spaceService.getById(picture.getSpaceId());
        }

        // 获取权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        ThrowUtils.throwIf(!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT),
                ErrorCode.NO_AUTH_ERROR, "无编辑权限");
    }

    /**
     * 构建基础请求（wan2.7-image-pro 新版格式）
     */
    private Wan27ImageGenerationRequest buildWan27Request() {
        Wan27ImageGenerationRequest request = new Wan27ImageGenerationRequest();
        request.setModel("wan2.7-image-pro");

        Wan27ImageGenerationRequest.Input input = new Wan27ImageGenerationRequest.Input();
        request.setInput(input);

        Wan27ImageGenerationRequest.Parameters parameters = new Wan27ImageGenerationRequest.Parameters();
        parameters.setSize("2K");
        parameters.setN(1);
        parameters.setWatermark(false);
        request.setParameters(parameters);

        return request;
    }

    /**
     * 构建抠图提示词（wan2.7-image-pro 版本）
     */
    private String buildSegmentPromptWan27(String type, List<Integer> bbox) {
        String basePrompt;
        if ("human".equals(type)) {
            basePrompt = "在指定的框选区域内进行精确的人物主体提取和背景移除操作。";
        } else if ("object".equals(type)) {
            basePrompt = "框选区域内精确分割出目标物体，彻底移除背景。";
        } else {
            basePrompt = "在框选区域内抠出主体，彻底移除背景。";
        }

        // 如果有框选区域，添加更详细的说明
        if (bbox != null && bbox.size() == 4) {
            return basePrompt + String.format(
                    " 框选区域坐标：[%d, %d, %d, %d]。" +
                    "要求：精确抠出主体，输出带透明背景的PNG，保留头发丝、衣服细节、光影自然，边缘抗锯齿，像素级精准。",
                    bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3));
        }
        return basePrompt;
    }

    /**
     * 构建去水印提示词（wan2.7-image-pro 版本）
     */
    private String buildRemoveWatermarkPromptWan27() {
        return "检测并移除图像中的所有文字水印、半透明文字、logo水印。保持图像背景、主体内容、光影和细节完全不变，仅移除水印元素。水印移除后的区域应与周围背景无缝融合，保持自然的纹理、颜色过渡和光影一致性。边缘处理平滑、抗锯齿，不产生明显的修复痕迹。输出高质量PNG图像。";
    }

    /**
     * 构建增强提示词（wan2.7-image-pro 版本）
     */
    private String buildEnhancePromptWan27(String enhanceType) {
        switch (enhanceType) {
            case "quality":
                return "提升图像整体质量：增强清晰度、优化对比度、改善色彩饱和度。保持图像自然风格，不改变主体内容，不产生过度处理效果。边缘处理平滑、自然。输出2K分辨率的高质量PNG图像。";
            case "denoise":
                return "移除图像中的噪点和颗粒，同时保留重要的细节和纹理。边缘保持清晰，不产生模糊或伪影。保持图像自然观感。输出高质量PNG图像。";
            case "sharpen":
                return "增强图像的清晰度和细节表现，使边缘更加锐利。避免过度锐化产生的光晕效应和伪影，保持自然观感。输出高质量PNG图像。";
            case "color":
                return "优化图像色彩表现：增强色彩饱和度、改善色彩平衡、使色彩更鲜艳生动。保持自然色调，避免过度饱和。输出高质量PNG图像。";
            default:
                return "图像质量增强：提升清晰度、优化对比度、改善色彩。保持自然风格。输出高质量PNG图像。";
        }
    }

    /**
     * 获取图片URL（如果是私有的，生成预签名URL）
     */
    private String getImageUrl(Picture picture) {
        // 如果图片URL已经是完整的公网URL，直接返回
        if (picture.getUrl().startsWith("http://") || picture.getUrl().startsWith("https://")) {
            return picture.getUrl();
        }

        // 否则生成预签名URL
        String key = picture.getUrl().startsWith("/") ? picture.getUrl().substring(1) : picture.getUrl();
        return cosManager.generatePresignedUrl(key);
    }

    /**
     * 下载并上传到COS
     */
    private String downloadAndUploadToCos(String imageUrl, String editType) {
        try {
            // 这里需要实现下载图片的逻辑
            // 由于需要从URL下载，可以使用Hutool的HttpUtil
            // 然后上传到COS
            // 简化处理：直接返回阿里云的URL（24小时有效）
            // 实际生产环境建议下载后上传到自己的COS
            log.info("图像编辑结果URL: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("下载并上传图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理编辑结果失败");
        }
    }

    /**
     * 创建编辑任务（wan2.7-image-pro 新版，使用 SDK）
     */
    private PictureEditTaskVO createEditTaskWan27(Wan27ImageGenerationRequest apiRequest,
                                                  Long pictureId,
                                                  Long userId,
                                                  String editType,
                                                  Object requestDto) {
        // 使用 SDK 调用阿里云 API 创建任务
        com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult sdkResult = imageEditApiWan27.createTask(apiRequest);

        // 提取任务 ID
        String taskId = imageEditApiWan27.getTaskId(sdkResult);
        if (taskId == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建任务失败，未获取到任务 ID");
        }

        // 创建任务记录
        PictureEditTask task = new PictureEditTask();
        task.setTaskId(taskId);
        task.setPictureId(pictureId);
        task.setUserId(userId);
        task.setEditType(editType);
        task.setStatus(PictureEditTaskStatusEnum.PROCESSING.getValue());
        task.setRequestParams(JSONUtil.toJsonStr(requestDto));
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());

        // 保存到数据库
        save(task);

        return convertToVO(task);
    }

    /**
     * 转换为VO
     */
    private PictureEditTaskVO convertToVO(PictureEditTask task) {
        if (task == null) {
            return null;
        }
        PictureEditTaskVO vo = new PictureEditTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
