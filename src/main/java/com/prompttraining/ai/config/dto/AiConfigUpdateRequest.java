package com.prompttraining.ai.config.dto;

import lombok.Data;

/**
 * AI 配置更新请求（V3）
 * 字段留空表示保持原值；apiKey 留空表示不修改原有 Key
 */
@Data
public class AiConfigUpdateRequest {

    /** API 基础地址 */
    private String apiBaseUrl;

    /** API Key（留空表示不修改） */
    private String apiKey;

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
}
