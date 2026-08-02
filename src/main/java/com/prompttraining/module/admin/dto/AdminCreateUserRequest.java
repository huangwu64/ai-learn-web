package com.prompttraining.module.admin.dto;

import lombok.Data;

/**
 * 管理员创建用户请求（V3.2）
 */
@Data
public class AdminCreateUserRequest {

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 昵称（可选） */
    private String nickname;
}
