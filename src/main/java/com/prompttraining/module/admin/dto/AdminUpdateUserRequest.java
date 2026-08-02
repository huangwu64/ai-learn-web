package com.prompttraining.module.admin.dto;

import lombok.Data;

/**
 * 管理员更新用户请求（V3.2） - 管理员修改直接生效，无需审核
 */
@Data
public class AdminUpdateUserRequest {

    /** 昵称（可选） */
    private String nickname;

    /** 头像 URL（可选） */
    private String avatarUrl;

    /** 账号状态 1=正常 0=禁用（可选） */
    private Integer status;
}
