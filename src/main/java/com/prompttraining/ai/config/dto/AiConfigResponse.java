package com.prompttraining.ai.config.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 配置响应（V3）
 * apiKey 以脱敏形式返回，避免明文泄露
 */
@Data
public class AiConfigResponse {

    /** Provider 编码 */
    private String providerCode;

    /** API 基础地址 */
    private String apiBaseUrl;

    /** API Key 脱敏值（保留后 4 位） */
    private String apiKeyMasked;

    /** 是否已配置 API Key（含配置文件回退值） */
    private Boolean hasApiKey;

    /** 模型编码 */
    private String modelCode;

    /** 最大输出 token */
    private Integer maxTokens;

    /** 温度 */
    private Double temperature;

    /** 核采样概率 */
    private Double topP;

    /** 话题新鲜度惩罚 */
    private Double presencePenalty;

    /** 频率惩罚 */
    private Double frequencyPenalty;

    /** 初始提示词 */
    private String systemPrompt;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
