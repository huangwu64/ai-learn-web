package com.prompttraining.module.admin.dto;

import lombok.Data;

/**
 * 管理员重置用户密码请求（V3.2）
 */
@Data
public class AdminResetPasswordRequest {

    /** 新密码 */
    private String password;
}
