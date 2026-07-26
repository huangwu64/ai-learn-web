package com.prompttraining.ai.deepseek;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 配置属性
 * 对应 application-dev.yml 中的 ai.deepseek 配置段
 *
 * 注意：api-key 是敏感信息，请勿提交到版本控制
 * 获取地址：https://platform.deepseek.com/api_keys
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekConfig {

    /** DeepSeek API 基础地址 */
    private String apiBaseUrl = "https://api.deepseek.com";

    /** DeepSeek API Key - 请在 application-dev.yml 中配置 */
    private String apiKey;

    /** 默认模型编码 */
    private String modelCode = "deepseek-chat";

    /** 最大输出 token 数 */
    private Integer maxTokens = 4096;

    /** 默认温度参数（0-1） */
    private Double temperature = 0.7;
}
