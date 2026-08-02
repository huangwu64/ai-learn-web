package com.prompttraining.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 管理员登录响应（V3）
 */
@Data
@AllArgsConstructor
public class AdminLoginResponse {

    /** 管理员 JWT Token（role=ADMIN） */
    private String token;

    /** 有效期（秒） */
    private Long expiresIn;
}
