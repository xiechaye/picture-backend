package com.chaye.picturebackend.service.impl;

import com.chaye.picturebackend.app.ImageGenerationApp;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.exception.ThrowUtils;
import com.chaye.picturebackend.manager.auth.SpaceUserAuthManager;
import com.chaye.picturebackend.model.dto.imagegeneration.GenerateImageRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.ImageGenerationResponse;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptResponse;
import com.chaye.picturebackend.model.entity.Space;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.service.SpaceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 图像生成服务
 */
@Service
@Slf4j
public class ImageGenerationServiceImpl {

    @Resource
    private ImageGenerationApp imageGenerationApp;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceService spaceService;

    /**
     * 同步生成图像
     *
     * @param request   生成请求
     * @param loginUser 登录用户
     * @return 生成结果
     */
    public ImageGenerationResponse generateImage(GenerateImageRequest request, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(request.getPrompt()),
                ErrorCode.PARAMS_ERROR, "图像描述不能为空");
        ThrowUtils.throwIf(request.getSpaceId() == null,
                ErrorCode.PARAMS_ERROR, "空间ID不能为空");

        // 2. 校验空间权限
        validateSpaceAccess(request.getSpaceId(), loginUser);

        log.info("用户{}开始同步生成图像，spaceId={}, prompt={}",
                loginUser.getId(), request.getSpaceId(), request.getPrompt());

        // 3. 调用App生成并阻塞等待
        ImageGenerationApp.ImageGenerationResult result =
                imageGenerationApp.generateImageWithResult(request.getPrompt());

        // 4. 转换为响应DTO
        ImageGenerationResponse response = new ImageGenerationResponse();
        response.setImageUrl(result.imageUrl());
        response.setCosKey(result.cosKey());
        response.setOptimizedPrompt(result.optimizedPrompt());
        response.setTotalTime(result.totalTime());
        response.setSpaceId(request.getSpaceId());

        log.info("用户{}图像生成成功，耗时{}ms, cosKey={}",
                loginUser.getId(), result.totalTime(), result.cosKey());

        return response;
    }

    /**
     * 优化Prompt
     *
     * @param request 优化请求
     * @return 优化结果
     */
    public OptimizePromptResponse optimizePrompt(OptimizePromptRequest request) {
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(request.getPrompt()),
                ErrorCode.PARAMS_ERROR, "图像描述不能为空");

        log.info("开始优化Prompt: {}", request.getPrompt());

        // 使用ChatClient优化（不需要空间权限）
        String chatId = UUID.randomUUID().toString();
        String optimized = imageGenerationApp.optimizeImagePrompt(chatId, request.getPrompt())
                .collectList()
                .block()
                .stream()
                .collect(Collectors.joining());

        OptimizePromptResponse response = new OptimizePromptResponse();
        response.setOriginalPrompt(request.getPrompt());
        response.setOptimizedPrompt(optimized);

        log.info("Prompt优化完成，长度: {} -> {}",
                request.getPrompt().length(), optimized.length());

        return response;
    }

    /**
     * 校验空间权限
     *
     * @param spaceId   空间ID
     * @param loginUser 登录用户
     */
    private void validateSpaceAccess(Long spaceId, User loginUser) {
        // 1. 检查空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

        // 2. 通过SpaceUserAuthManager获取权限列表
        java.util.List<String> permissions = spaceUserAuthManager.getPermissionList(space, loginUser);

        // 3. 权限列表为空表示无权限访问
        ThrowUtils.throwIf(permissions.isEmpty(), ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");

        log.info("用户{}通过空间{}权限校验", loginUser.getId(), spaceId);
    }
}
