package com.prompttraining.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 - V1 仅使用一条默认匿名用户记录
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    /** BCrypt 加密密码，V2 新增 */
    private String password;

    /** 账号状态：1=正常，0=禁用，V2 新增 */
    private Integer status;

    /** 最后登录时间，V2 新增 */
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
