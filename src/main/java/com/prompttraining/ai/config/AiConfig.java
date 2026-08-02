package com.prompttraining.ai.config;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 动态配置实体（V3 新增）
 * 对应 ai_config 表，管理员可在管理界面动态修改，运行时即时生效
 */
@Data
@TableName("ai_config")
public class AiConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** Provider 编码（如 deepseek） */
    private String providerCode;

    /** API 基础地址 */
    private String apiBaseUrl;

    /** API Key（空则回退配置文件 ai.deepseek.api-key） */
    private String apiKey;

    /** 模型编码 */
    private String modelCode;

    /** 最大输出 token */
    private Integer maxTokens;

    /** 温度（0-2） */
    private Double temperature;

    /** 核采样概率（0-1） */
    private Double topP;

    /** 话题新鲜度惩罚（-2~2） */
    private Double presencePenalty;

    /** 频率惩罚（-2~2） */
    private Double frequencyPenalty;

    /** 初始提示词 */
    private String systemPrompt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
