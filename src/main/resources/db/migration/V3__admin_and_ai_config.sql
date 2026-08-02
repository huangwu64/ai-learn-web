-- ============================================================
-- V3__admin_and_ai_config.sql
-- V3 增量迁移：管理员动态 AI 配置表
-- ============================================================

-- 1. 新建 ai_config 表（管理员界面可动态修改，运行时即时生效）
CREATE TABLE IF NOT EXISTS `ai_config` (
    `id`                INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `provider_code`     VARCHAR(50)   NOT NULL DEFAULT 'deepseek' COMMENT 'Provider编码',
    `api_base_url`      VARCHAR(255)  NOT NULL DEFAULT 'https://api.deepseek.com' COMMENT 'API基础地址',
    `api_key`           VARCHAR(500)  NOT NULL DEFAULT '' COMMENT 'API Key（空则回退配置文件）',
    `model_code`        VARCHAR(100)  NOT NULL DEFAULT 'deepseek-chat' COMMENT '模型编码',
    `max_tokens`        INT           NOT NULL DEFAULT 4096 COMMENT '最大输出token',
    `temperature`       DECIMAL(3,2)  NOT NULL DEFAULT 0.70 COMMENT '温度',
    `top_p`             DECIMAL(3,2)  NOT NULL DEFAULT 1.00 COMMENT '核采样概率',
    `presence_penalty`  DECIMAL(3,2)  NOT NULL DEFAULT 0.00 COMMENT '话题新鲜度惩罚',
    `frequency_penalty` DECIMAL(3,2)  NOT NULL DEFAULT 0.00 COMMENT '频率惩罚',
    `system_prompt`     TEXT          NULL COMMENT '初始提示词',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_provider` (`provider_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 动态配置表';

-- 2. 初始化 DeepSeek 默认配置行（api_key 留空，运行时回退到 application-dev.yml 的 ai.deepseek.api-key）
INSERT INTO `ai_config`
    (`provider_code`, `api_base_url`, `api_key`, `model_code`, `max_tokens`, `temperature`, `top_p`, `presence_penalty`, `frequency_penalty`, `system_prompt`)
VALUES
    ('deepseek', 'https://api.deepseek.com', '', 'deepseek-chat', 4096, 0.70, 1.00, 0.00, 0.00, '你是一个有帮助的AI助手，专注于帮助用户学习和优化提示词工程技能。')
ON DUPLICATE KEY UPDATE `provider_code` = VALUES(`provider_code`);
