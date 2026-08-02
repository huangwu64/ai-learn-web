package com.prompttraining.common;

/**
 * 系统常量
 */
public interface Constant {

    /** V1 默认用户 ID */
    Long DEFAULT_USER_ID = 1L;

    /** 默认模型编码 */
    String DEFAULT_MODEL_CODE = "deepseek-chat";

    /** 对话上下文最大轮数（V2 默认值，可通过配置覆盖） */
    int MAX_CONTEXT_ROUNDS = 10;

    /** 消息分页默认条数 */
    int DEFAULT_PAGE_SIZE = 50;

    /** AI 调用超时时间（秒） */
    int AI_TIMEOUT_SECONDS = 30;

    /** V2 默认系统提示词 */
    String DEFAULT_SYSTEM_PROMPT = "你是一个有帮助的AI助手，专注于帮助用户学习和优化提示词工程技能。";

    /** JWT 相关 */
    String TOKEN_PREFIX = "Bearer ";
    String ACCESS_TOKEN_KEY = "access_token";
    String REFRESH_TOKEN_KEY = "refresh_token";

    /** V3 角色常量 */
    String ROLE_USER = "USER";
    String ROLE_ADMIN = "ADMIN";

    /** V3 管理员哨兵用户 ID（管理员不在 user 表中） */
    Long ADMIN_USER_ID = 0L;

    /** V3 AI Provider 编码 */
    String AI_PROVIDER_CODE = "deepseek";
}
