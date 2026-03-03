package com.chaye.picturebackend.service;

import com.chaye.picturebackend.model.vo.CaptchaResponse;

/**
 * 验证码服务
 */
public interface CaptchaService {

    /**
     * 生成验证码
     *
     * @return 验证码响应（包含验证码 ID 和 Base64 图片）
     */
    CaptchaResponse generateCaptcha();

    /**
     * 校验验证码
     *
     * @param captchaId   验证码 ID
     * @param captchaCode 用户输入的验证码
     * @return 校验是否通过
     */
    boolean verifyCaptcha(String captchaId, String captchaCode);
}
