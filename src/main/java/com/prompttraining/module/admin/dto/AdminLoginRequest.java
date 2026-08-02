package com.prompttraining.module.admin.dto;

import lombok.Data;

/**
 * 管理员登录请求（V3）
 */
@Data
public class AdminLoginRequest {

    /** 管理员账号 */
    private String username;

    /** 管理员密码 */
    private String password;
}
