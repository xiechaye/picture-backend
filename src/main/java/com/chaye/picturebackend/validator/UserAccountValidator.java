package com.chaye.picturebackend.validator;

import com.chaye.picturebackend.annotation.UserAccount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 用户账号验证器
 */
public class UserAccountValidator implements ConstraintValidator<UserAccount, String> {

    /**
     * 账号正则：字母、数字、下划线
     */
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private int min;
    private int max;

    @Override
    public void initialize(UserAccount constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 值由 @NotBlank/@NotNull 处理
        if (value == null) {
            return true;
        }

        // 长度验证
        if (value.length() < min || value.length() > max) {
            return false;
        }

        // 格式验证：仅允许字母、数字、下划线
        return ACCOUNT_PATTERN.matcher(value).matches();
    }
}
