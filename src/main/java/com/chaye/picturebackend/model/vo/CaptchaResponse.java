package com.chaye.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 验证码响应
 */
@Data
public class CaptchaResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码 ID（用于校验时查找）
     */
    private String captchaId;

    /**
     * 验证码图片（Base64 编码，格式：data:image/png;base64,{图片数据}）
     */
    private String captchaImage;

    public CaptchaResponse(String captchaId, String captchaImage) {
        this.captchaId = captchaId;
        this.captchaImage = captchaImage;
    }
}