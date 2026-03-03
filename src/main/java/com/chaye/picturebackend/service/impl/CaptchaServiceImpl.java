package com.chaye.picturebackend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.chaye.picturebackend.service.CaptchaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 */
@Service
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 验证码有效期（秒），默认 5 分钟
     */
    @Value("${captcha.expire-seconds:300}")
    private long captchaExpireSeconds;

    /**
     * 验证码宽度
     */
    @Value("${captcha.width:120}")
    private int captchaWidth;

    /**
     * 验证码高度
     */
    @Value("${captcha.height:40}")
    private int captchaHeight;

    /**
     * 验证码字符数
     */
    @Value("${captcha.code-count:4}")
    private int codeCount;

    /**
     * 验证码干扰线数
     */
    @Value("${captcha.line-count:5}")
    private int lineCount;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public com.chaye.picturebackend.model.vo.CaptchaResponse generateCaptcha() {
        // 创建线条干扰验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(captchaWidth, captchaHeight, codeCount, lineCount);

        // 获取验证码文本
        String captchaCode = lineCaptcha.getCode();

        // 生成唯一验证码 ID
        String captchaId = UUID.randomUUID().toString(true);

        // 存储到 Redis
        String redisKey = CAPTCHA_KEY_PREFIX + captchaId;
        stringRedisTemplate.opsForValue().set(redisKey, captchaCode, captchaExpireSeconds, TimeUnit.SECONDS);

        // 获取图片 Base64 编码
        String captchaImage = lineCaptcha.getImageBase64();

        log.info("生成验证码，captchaId: {}", captchaId);

        return new com.chaye.picturebackend.model.vo.CaptchaResponse(captchaId, captchaImage);
    }

    @Override
    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaCode)) {
            return false;
        }

        String redisKey = CAPTCHA_KEY_PREFIX + captchaId;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isBlank(storedCode)) {
            log.warn("验证码已过期或不存在，captchaId: {}", captchaId);
            return false;
        }

        // 验证成功后删除验证码（一次性使用）
        boolean isValid = storedCode.equalsIgnoreCase(captchaCode);
        if (isValid) {
            stringRedisTemplate.delete(redisKey);
            log.info("验证码校验成功并删除，captchaId: {}", captchaId);
        } else {
            log.warn("验证码校验失败，captchaId: {}, 输入: {}, 正确: {}", captchaId, captchaCode, storedCode);
        }

        return isValid;
    }
}
