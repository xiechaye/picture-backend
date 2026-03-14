package com.chaye.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 重置用户密码请求
 */
@Data
public class UserResetPasswordRequest implements Serializable {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 新密码
     */
    private String userPassword;

    private static final long serialVersionUID = 1L;
}
