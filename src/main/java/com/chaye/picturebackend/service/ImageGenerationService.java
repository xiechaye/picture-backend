package com.chaye.picturebackend.service;

import com.chaye.picturebackend.model.dto.imagegeneration.GenerateImageRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.ImageGenerationResponse;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptRequest;
import com.chaye.picturebackend.model.dto.imagegeneration.OptimizePromptResponse;
import com.chaye.picturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 图像生成服务
 */
@Service
@Slf4j
public interface ImageGenerationService {

    /**
     * 同步生成图像
     *
     * @param request   生成请求
     * @param loginUser 登录用户
     * @return 生成结果
     */
    ImageGenerationResponse generateImage(GenerateImageRequest request, User loginUser);

    /**
     * 优化Prompt
     *
     * @param request 优化请求
     * @return 优化结果
     */
    OptimizePromptResponse optimizePrompt(OptimizePromptRequest request);

}
