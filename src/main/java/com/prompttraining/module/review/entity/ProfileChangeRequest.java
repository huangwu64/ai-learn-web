package com.prompttraining.module.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料变更审核实体（V3.2）
 * 用户修改头像/昵称/用户名后进入审核队列，管理员通过后才同步到用户资料
 */
@Data
@TableName("profile_change_request")
public class ProfileChangeRequest {

    /** 审核状态：待审核 */
    public static final int STATUS_PENDING = 0;
    /** 审核状态：已通过 */
    public static final int STATUS_APPROVED = 1;
    /** 审核状态：已拒绝 */
    public static final int STATUS_REJECTED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起变更的用户 ID */
    private Long userId;

    /** 变更字段：avatar / nickname / username */
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
