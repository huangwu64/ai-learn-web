-- V2__add_login_and_model_fields.sql
-- V2 增量迁移：用户登录模块字段 + Refresh Token 表

-- 1. user 表新增字段
ALTER TABLE `user`
    ADD COLUMN `password` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'BCrypt加密密码' AFTER `nickname`,
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态 1=正常 0=禁用' AFTER `password`,
    ADD COLUMN `last_login_at` DATETIME NULL COMMENT '最后登录时间' AFTER `status`;

-- 2. 新建 refresh_token 表
CREATE TABLE IF NOT EXISTS `refresh_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '所属用户',
    `token` VARCHAR(500) NULL UNIQUE COMMENT 'Refresh Token（先插入再更新，故允许NULL）',
    `expires_at` DATETIME NOT NULL COMMENT '过期时间',
    `is_revoked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已撤销',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Refresh Token 表';
