-- ============================================================
-- V3_2__user_review_and_avatar.sql
-- V3.2 增量迁移：用户资料变更审核表
-- ============================================================

-- 用户资料变更审核表（头像/昵称/用户名等修改需管理员审核后生效）
CREATE TABLE IF NOT EXISTS `profile_change_request` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`       BIGINT       NOT NULL COMMENT '发起变更的用户',
    `field_name`    VARCHAR(50)  NOT NULL COMMENT '变更字段：avatar/nickname/username',
    `old_value`     VARCHAR(500) DEFAULT NULL COMMENT '原值',
    `new_value`     VARCHAR(500) NOT NULL COMMENT '新值',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待审核 1=已通过 2=已拒绝',
    `review_remark` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
    `reviewed_at`   DATETIME     DEFAULT NULL COMMENT '审核时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料变更审核表';
