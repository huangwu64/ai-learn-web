package com.prompttraining.security;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token 实体
 */
@Data
@TableName("refresh_token")
public class RefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    /** Refresh Token 值 */
    private String token;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 是否已撤销 */
    private Integer isRevoked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
