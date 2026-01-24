package com.chaye.picturebackend.controller;

import com.chaye.picturebackend.annotation.AuthCheck;
import com.chaye.picturebackend.common.BaseResponse;
import com.chaye.picturebackend.common.ResultUtils;
import com.chaye.picturebackend.model.dto.imagegeneration.GenerateImageRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.ImageGenerationResponse;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptResponse;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.service.ImageGenerationService;
import com.chaye.picturebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图像生成控制器
 */
@RestController
@RequestMapping("/image-generation")
@Slf4j
@Tag(name = "图像生成接口")
public class ImageGenerationController {

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private UserService userService;

    /**
     * 同步生成图像
     *
     * @param request        生成请求
     * @param servletRequest HTTP请求
     * @return 生成结果
     */
    @PostMapping("/generate")
    @AuthCheck(mustRole = "user")
    @Operation(summary = "同步生成图像",
               description = "使用 AI 生成图像并上传到临时存储。" +
                             "生成的图像需要用户后期自行保存到空间。" +
                             "可选参数：size（图片尺寸，格式：width,height）、negativePrompt（负面提示词）")
    public BaseResponse<ImageGenerationResponse> generateImage(
            @RequestBody GenerateImageRequest request,
            HttpServletRequest servletRequest) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(servletRequest);

        // 调用service生成
        ImageGenerationResponse response = imageGenerationService.generateImage(request, loginUser);

        return ResultUtils.success(response);
    }

    /**
     * 优化Prompt（不生成图像）
     *
     * @param request 优化请求
     * @return 优化结果
     */
    @PostMapping("/optimize-prompt")
    @AuthCheck(mustRole = "user")
    @Operation(summary = "优化Prompt", description = "只优化图像描述，不生成图像")
    public BaseResponse<OptimizePromptResponse> optimizePrompt(
            @RequestBody OptimizePromptRequest request) {
        // 调用service优化
        OptimizePromptResponse response = imageGenerationService.optimizePrompt(request);

        return ResultUtils.success(response);
    }
}
