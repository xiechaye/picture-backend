package com.chaye.picturebackend.validator;

import com.chaye.picturebackend.annotation.Password;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 密码验证器
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

    /**
     * 字母正则（包含大小写）
     */
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");

    /**
     * 数字正则
     */
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    private int min;
    private int max;

    @Override
    public void initialize(Password constraintAnnotation) {
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

        // 必须包含字母
        if (!LETTER_PATTERN.matcher(value).matches()) {
            return false;
        }

        // 必须包含数字
        if (!DIGIT_PATTERN.matcher(value).matches()) {
            return false;
        }

        return true;
    }
}
