package com.chaye.picturebackend.annotation;

import com.chaye.picturebackend.validator.PasswordValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 密码验证注解
 * 验证规则：
 * 1. 长度 8-32 个字符
 * 2. 必须包含字母和数字
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface Password {

    /**
     * 最小长度
     */
    int min() default 8;

    /**
     * 最大长度
     */
    int max() default 32;

    /**
     * 验证失败时的错误消息
     */
    String message() default "密码长度必须在 {min}-{max} 个字符之间，且必须包含字母和数字";

    /**
     * 分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
}
