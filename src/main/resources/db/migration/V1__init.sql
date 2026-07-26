-- ============================================================
-- 提示词训练系统 V1.0 - 数据库初始化脚本
-- 使用方法：在 MySQL 中创建 prompt_training 数据库后执行此脚本
   CREATE DATABASE IF NOT EXISTS prompt_training DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   USE prompt_training;
--   SOURCE V1__init.sql;
-- ============================================================

-- ----------------------------
-- 用户表（V1 仅初始化一条默认记录）
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `avatar_url`  VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 会话表
-- ----------------------------
DROP TABLE IF EXISTS `session`;
CREATE TABLE `session` (
    `id`            VARCHAR(32)  NOT NULL COMMENT '主键，雪花ID或UUID',
    `user_id`       BIGINT       NOT NULL COMMENT '所属用户',
    `title`         VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    `model_code`    VARCHAR(50)  NOT NULL COMMENT '使用的模型编码',
    `message_count` INT          NOT NULL DEFAULT 0 COMMENT '消息总数（冗余）',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_updated` (`user_id`, `updated_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ----------------------------
-- 消息表
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id`  VARCHAR(32)  NOT NULL COMMENT '所属会话',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色：user / assistant',
    `content`     TEXT         NOT NULL COMMENT '消息内容',
    `token_count` INT          NOT NULL DEFAULT 0 COMMENT '本条消息消耗的 token 数',
    `model_code`  VARCHAR(50)  DEFAULT NULL COMMENT '生成此消息的模型（用户消息为 NULL）',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记：0-未删除，1-已删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_time` (`session_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ----------------------------
-- 模型提供商配置表（V1 初始化一条 DeepSeek 记录）
-- ----------------------------
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider` (
    `id`               INT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `provider_name`    VARCHAR(100) NOT NULL COMMENT '提供商名称',
    `model_code`       VARCHAR(50)  NOT NULL COMMENT '模型编码',
    `api_base_url`     VARCHAR(255) NOT NULL COMMENT 'API 基础地址',
    `api_key_encrypted` VARCHAR(500) NOT NULL COMMENT '加密存储的 API Key',
    `max_tokens`       INT          NOT NULL DEFAULT 4096 COMMENT '最大输出 token',
    `is_active`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_code` (`model_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型提供商配置表';

-- ----------------------------
-- 初始化默认数据
-- ----------------------------

-- 默认用户（V1 只使用此匿名用户）
INSERT INTO `user` (`id`, `username`, `nickname`) VALUES (1, 'anonymous', '匿名用户');

-- DeepSeek 默认配置（api_key_encrypted 占位，实际运行时从 application-dev.yml 读取）
INSERT INTO `model_provider` (`provider_name`, `model_code`, `api_base_url`, `api_key_encrypted`, `max_tokens`, `is_active`)
VALUES ('DeepSeek', 'deepseek-chat', 'https://api.deepseek.com', 'PLACEHOLDER', 4096, 1);
