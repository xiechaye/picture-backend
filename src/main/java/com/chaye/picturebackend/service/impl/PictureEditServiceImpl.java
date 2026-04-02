package com.chaye.picturebackend.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chaye.picturebackend.api.imageEdit.ImageEditApi;
import com.chaye.picturebackend.api.imageEdit.model.CreateImageEditTaskRequest;
import com.chaye.picturebackend.api.imageEdit.model.CreateImageEditTaskResponse;
import com.chaye.picturebackend.api.imageEdit.model.GetImageEditTaskResponse;
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
import java.util.Date;
import java.util.List;

/**
 * 图片编辑服务实现
 */
@Slf4j
@Service
public class PictureEditServiceImpl extends ServiceImpl<PictureEditTaskMapper, PictureEditTask>
        implements PictureEditService {

    @Resource
    private ImageEditApi imageEditApi;

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

        // 构建请求
        CreateImageEditTaskRequest apiRequest = buildBaseRequest();
        apiRequest.getInput().setFunction("description_edit");
        String prompt = buildSegmentPrompt(request.getType());
        apiRequest.getInput().setPrompt(prompt);
        apiRequest.getInput().setBaseImageUrl(imageUrl);

        // 创建任务
        return createEditTask(apiRequest, picture.getId(), loginUser.getId(),
                PictureEditTypeEnum.SEGMENT.getValue(), request);
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

        // 构建请求
        CreateImageEditTaskRequest apiRequest = buildBaseRequest();
        apiRequest.getInput().setFunction("remove_watermark");
        apiRequest.getInput().setPrompt("去除图像中的文字");
        apiRequest.getInput().setBaseImageUrl(imageUrl);

        // 创建任务
        return createEditTask(apiRequest, picture.getId(), loginUser.getId(),
                PictureEditTypeEnum.REMOVE_WATERMARK.getValue(), request);
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

        // 构建请求
        CreateImageEditTaskRequest apiRequest = buildBaseRequest();
        String function = buildEnhanceFunction(request.getEnhanceType());
        apiRequest.getInput().setFunction(function);
        apiRequest.getInput().setPrompt(buildEnhancePrompt(request.getEnhanceType()));
        apiRequest.getInput().setBaseImageUrl(imageUrl);

        // 设置超分倍数
        if ("super_resolution".equals(function)) {
            apiRequest.getParameters().setUpscaleFactor(2);
        }

        // 创建任务
        return createEditTask(apiRequest, picture.getId(), loginUser.getId(),
                PictureEditTypeEnum.ENHANCE.getValue(), request);
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
            // 查询阿里云任务状态
            GetImageEditTaskResponse response = imageEditApi.getTaskResult(taskId);
            GetImageEditTaskResponse.Output output = response.getOutput();

            if (output == null) {
                return false;
            }

            String taskStatus = output.getTaskStatus();

            // 判断任务状态
            if ("SUCCEEDED".equals(taskStatus)) {
                // 任务成功，获取结果URL
                if (output.getResults() != null && !output.getResults().isEmpty()) {
                    String resultUrl = output.getResults().get(0).getUrl();

                    // 下载并上传到 COS
                    String cosUrl = downloadAndUploadToCos(resultUrl, task.getEditType());

                    // 更新任务状态
                    task.setStatus(PictureEditTaskStatusEnum.SUCCESS.getValue());
                    task.setResultUrl(cosUrl);
                    updateById(task);
                    return true;
                }
            } else if ("FAILED".equals(taskStatus)) {
                // 任务失败
                task.setStatus(PictureEditTaskStatusEnum.FAILED.getValue());
                task.setErrorMessage(output.getMessage());
                updateById(task);
                return true;
            }

            // 任务还在处理中
            return false;
        } catch (Exception e) {
            log.error("查询编辑任务状态失败，taskId: {}", taskId, e);
            return false;
        }
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
     * 构建基础请求
     */
    private CreateImageEditTaskRequest buildBaseRequest() {
        CreateImageEditTaskRequest request = new CreateImageEditTaskRequest();
        request.setModel("wanx2.1-imageedit");

        CreateImageEditTaskRequest.Input input = new CreateImageEditTaskRequest.Input();
        request.setInput(input);

        CreateImageEditTaskRequest.Parameters parameters = new CreateImageEditTaskRequest.Parameters();
        request.setParameters(parameters);

        return request;
    }

    /**
     * 构建抠图提示词
     */
    private String buildSegmentPrompt(String type) {
        if ("human".equals(type)) {
            return "抠出人像，去除背景";
        } else if ("object".equals(type)) {
            return "抠出主体物体，去除背景";
        }
        return "抠出主体，去除背景";
    }

    /**
     * 构建增强功能类型
     */
    private String buildEnhanceFunction(String enhanceType) {
        switch (enhanceType) {
            case "quality":
            case "denoise":
            case "sharpen":
                return "super_resolution";
            case "color":
                return "colorization";
            default:
                return "super_resolution";
        }
    }

    /**
     * 构建增强提示词
     */
    private String buildEnhancePrompt(String enhanceType) {
        switch (enhanceType) {
            case "quality":
                return "图像超分，提升图像质量";
            case "denoise":
                return "图像降噪，去除噪点";
            case "sharpen":
                return "图像锐化，增强细节";
            case "color":
                return "图像上色，让色彩更鲜艳";
            default:
                return "图像增强";
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
        // 否则拼接COS前缀
        return cosPrefix + picture.getUrl();
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
     * 创建编辑任务
     */
    private PictureEditTaskVO createEditTask(CreateImageEditTaskRequest apiRequest,
                                              Long pictureId,
                                              Long userId,
                                              String editType,
                                              Object requestDto) {
        // 调用阿里云API创建任务
        CreateImageEditTaskResponse response = imageEditApi.createTask(apiRequest);

        // 创建任务记录
        PictureEditTask task = new PictureEditTask();
        task.setTaskId(response.getOutput().getTaskId());
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
