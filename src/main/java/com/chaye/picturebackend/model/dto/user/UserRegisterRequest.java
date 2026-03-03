package com.chaye.picturebackend.model.dto.user;

import com.chaye.picturebackend.annotation.Password;
import com.chaye.picturebackend.annotation.UserAccount;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 8735650154179439661L;

    /**
     * 账号
     * 验证规则：4-20字符，仅字母数字下划线
     */
    @UserAccount
    @NotBlank(message = "账号不能为空")
    private String userAccount;

    /**
     * 密码
     * 验证规则：8-32字符，必须包含字母和数字
     */
    @Password
    @NotBlank(message = "密码不能为空")
    private String userPassword;

    /**
     * 确认密码
     * 注意：密码一致性验证在业务层处理
     */
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;

    /**
     * 验证码 ID
     */
    @NotBlank(message = "验证码 ID 不能为空")
    private String captchaId;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

}
