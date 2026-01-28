package com.chaye.picturebackend.annotation;

import com.chaye.picturebackend.validator.UserAccountValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 用户账号验证注解
 * 验证规则：
 * 1. 长度 4-20 个字符
 * 2. 仅允许字母、数字、下划线
 */
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = UserAccountValidator.class)
@Documented
public @interface UserAccount {

    /**
     * 最小长度
     */
    int min() default 4;

    /**
     * 最大长度
     */
    int max() default 20;

    /**
     * 验证失败时的错误消息
     */
    String message() default "用户账号长度必须在 {min}-{max} 个字符之间，且仅包含字母、数字、下划线";

    /**
     * 分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
}
