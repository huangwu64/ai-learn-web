package com.prompttraining.module.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料变更审核记录响应（V3.2）
 */
@Data
public class ProfileChangeRequestResponse {

    private Long id;

    /** 发起用户 ID */
    private Long userId;

    /** 发起用户账号名 */
    private String username;

    /** 发起用户昵称 */
    private String nickname;

    /** 变更字段 */
    private String fieldName;

    /** 原值 */
    private String oldValue;

    /** 新值 */
    private String newValue;

    /** 状态：0=待审核 1=已通过 2=已拒绝 */
    private Integer status;

    /** 审核备注 */
    private String reviewRemark;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
