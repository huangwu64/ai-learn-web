package com.prompttraining.module.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视图的用户信息（V3.2）
 */
@Data
public class AdminUserResponse {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    /** 1=正常 0=禁用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;
}
