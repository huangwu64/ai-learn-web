package com.prompttraining.module.session.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型提供商配置实体 - V1 初始化 DeepSeek 记录
 */
@Data
@TableName("model_provider")
public class ModelProvider {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 提供商名称 */
    private String providerName;

    /** 模型编码 */
    private String modelCode;

    /** API 基础地址 */
    private String apiBaseUrl;

    /** 加密存储的 API Key */
    private String apiKeyEncrypted;

    /** 最大输出 token */
    private Integer maxTokens;

    /** 是否启用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
