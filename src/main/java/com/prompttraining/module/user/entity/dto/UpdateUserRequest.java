package com.prompttraining.module.user.entity.dto;

import lombok.Data;

/**
 * 更新用户信息请求（V2 新增）
 */
@Data
public class UpdateUserRequest {

    /** 新昵称，最长 64 位 */
    private String nickname;

    /** 新头像 URL，最长 255 位 */
    private String avatarUrl;
}
