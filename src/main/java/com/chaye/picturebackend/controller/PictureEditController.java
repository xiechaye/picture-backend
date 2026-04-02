package com.chaye.picturebackend.controller;

import com.chaye.picturebackend.common.BaseResponse;
import com.chaye.picturebackend.common.ResultUtils;
import com.chaye.picturebackend.manager.auth.SpaceUserAuthManager;
import com.chaye.picturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.chaye.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.chaye.picturebackend.model.dto.pictureedit.PictureEnhanceRequest;
import com.chaye.picturebackend.model.dto.pictureedit.PictureRemoveWatermarkRequest;
import com.chaye.picturebackend.model.dto.pictureedit.PictureSegmentRequest;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.model.vo.PictureEditTaskVO;
import com.chaye.picturebackend.service.PictureEditService;
import com.chaye.picturebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 图片编辑接口
 */
@Slf4j
@RestController
@RequestMapping("/picture/edit")
@Tag(name = "图片编辑", description = "AI 图片编辑相关接口")
public class PictureEditController {

    @Resource
    private PictureEditService pictureEditService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 智能抠图
     */
    @PostMapping("/segment")
    @Operation(summary = "智能抠图", description = "AI 智能抠图，支持人像和物体抠图")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<PictureEditTaskVO> segmentImage(
            @RequestBody PictureSegmentRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        PictureEditTaskVO taskVO = pictureEditService.segmentImage(request, loginUser);
        return ResultUtils.success(taskVO);
    }

    /**
     * 去除水印
     */
    @PostMapping("/remove-watermark")
    @Operation(summary = "去除水印", description = "AI 去除图片中的文字水印")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<PictureEditTaskVO> removeWatermark(
            @RequestBody PictureRemoveWatermarkRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        PictureEditTaskVO taskVO = pictureEditService.removeWatermark(request, loginUser);
        return ResultUtils.success(taskVO);
    }

    /**
     * 图片增强
     */
    @PostMapping("/enhance")
    @Operation(summary = "图片增强", description = "AI 图片增强，支持质量提升、降噪、锐化等")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<PictureEditTaskVO> enhanceImage(
            @RequestBody PictureEnhanceRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        PictureEditTaskVO taskVO = pictureEditService.enhanceImage(request, loginUser);
        return ResultUtils.success(taskVO);
    }

    /**
     * 查询编辑任务状态
     */
    @GetMapping("/task/{taskId}")
    @Operation(summary = "查询编辑任务", description = "查询图片编辑任务的状态和结果")
    public BaseResponse<PictureEditTaskVO> getTaskStatus(
            @PathVariable("taskId") String taskId) {
        PictureEditTaskVO taskVO = pictureEditService.getTaskStatus(taskId);
        return ResultUtils.success(taskVO);
    }
}
