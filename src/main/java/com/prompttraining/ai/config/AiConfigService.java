package com.prompttraining.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompttraining.ai.config.dto.AiConfigResponse;
import com.prompttraining.ai.config.dto.AiConfigUpdateRequest;
import com.prompttraining.ai.deepseek.DeepSeekConfig;
import com.prompttraining.common.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 动态配置服务（V3 新增）
 *
 * - 配置持久化在 ai_config 表，管理员界面可动态修改
 * - 运行时内存缓存，更新后立即生效，无需重启
 * - DB 中为空的值回退到配置文件（application-dev.yml 的 ai.deepseek）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiConfigMapper aiConfigMapper;
    private final DeepSeekConfig deepSeekConfig;

    @Value("${ai.context.system-prompt:你是一个有帮助的AI助手，专注于帮助用户学习和优化提示词工程技能。}")
    private String defaultSystemPrompt;

    /** 运行时缓存，volatile 保证多线程可见性 */
    private volatile AiConfig cachedConfig;

    /**
     * 获取当前 AI 配置（懒加载 + 缓存）
     */
    public AiConfig getConfig() {
        if (cachedConfig == null) {
            synchronized (this) {
                if (cachedConfig == null) {
                    cachedConfig = loadOrCreate();
                }
            }
        }
        return cachedConfig;
    }

    /**
     * 有效 API 地址：DB 为空则回退配置文件
     */
    public String getEffectiveApiBaseUrl() {
        AiConfig cfg = getConfig();
        return (cfg.getApiBaseUrl() == null || cfg.getApiBaseUrl().isBlank())
                ? deepSeekConfig.getApiBaseUrl()
                : cfg.getApiBaseUrl();
    }

    /**
     * 有效 API Key：DB 为空则回退配置文件
     */
    public String getEffectiveApiKey() {
        AiConfig cfg = getConfig();
        return (cfg.getApiKey() == null || cfg.getApiKey().isBlank())
                ? deepSeekConfig.getApiKey()
                : cfg.getApiKey();
    }

    /**
     * 有效模型编码：DB 为空则回退配置文件
     */
    public String getEffectiveModelCode() {
        AiConfig cfg = getConfig();
        return (cfg.getModelCode() == null || cfg.getModelCode().isBlank())
                ? deepSeekConfig.getModelCode()
                : cfg.getModelCode();
    }

    /**
     * 更新 AI 配置并刷新缓存（字段留空保持原值；apiKey 留空表示不修改）
     */
    public void updateConfig(AiConfigUpdateRequest req) {
        AiConfig cfg = getConfig();
        if (req.getApiBaseUrl() != null && !req.getApiBaseUrl().isBlank()) {
            cfg.setApiBaseUrl(req.getApiBaseUrl().trim());
        }
        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            cfg.setApiKey(req.getApiKey().trim());
        }
        if (req.getModelCode() != null && !req.getModelCode().isBlank()) {
            cfg.setModelCode(req.getModelCode().trim());
        }
        if (req.getMaxTokens() != null) cfg.setMaxTokens(req.getMaxTokens());
        if (req.getTemperature() != null) cfg.setTemperature(req.getTemperature());
        if (req.getTopP() != null) cfg.setTopP(req.getTopP());
        if (req.getPresencePenalty() != null) cfg.setPresencePenalty(req.getPresencePenalty());
        if (req.getFrequencyPenalty() != null) cfg.setFrequencyPenalty(req.getFrequencyPenalty());
        if (req.getSystemPrompt() != null) cfg.setSystemPrompt(req.getSystemPrompt());

        aiConfigMapper.updateById(cfg);
        cachedConfig = cfg; // 立即刷新缓存，新配置即时生效
        log.info("AI 配置已更新: modelCode={}, maxTokens={}, temperature={}, topP={}",
                cfg.getModelCode(), cfg.getMaxTokens(), cfg.getTemperature(), cfg.getTopP());
    }

    /**
     * 转换为脱敏响应对象
     */
    public AiConfigResponse toResponse() {
        AiConfig cfg = getConfig();
        AiConfigResponse resp = new AiConfigResponse();
        resp.setProviderCode(cfg.getProviderCode());
        resp.setApiBaseUrl(getEffectiveApiBaseUrl());
        String key = getEffectiveApiKey();
        boolean hasKey = key != null && !key.isBlank();
        resp.setHasApiKey(hasKey);
        resp.setApiKeyMasked(hasKey ? maskKey(key) : "");
        resp.setModelCode(getEffectiveModelCode());
        resp.setMaxTokens(cfg.getMaxTokens());
        resp.setTemperature(cfg.getTemperature());
        resp.setTopP(cfg.getTopP());
        resp.setPresencePenalty(cfg.getPresencePenalty());
        resp.setFrequencyPenalty(cfg.getFrequencyPenalty());
        resp.setSystemPrompt(cfg.getSystemPrompt());
        resp.setUpdatedAt(cfg.getUpdatedAt());
        return resp;
    }

    /**
     * 从数据库加载配置，不存在则创建默认行
     */
    private AiConfig loadOrCreate() {
        AiConfig cfg = aiConfigMapper.selectOne(
                new LambdaQueryWrapper<AiConfig>()
                        .eq(AiConfig::getProviderCode, Constant.AI_PROVIDER_CODE)
                        .last("LIMIT 1")
        );
        if (cfg == null) {
            cfg = new AiConfig();
            cfg.setProviderCode(Constant.AI_PROVIDER_CODE);
            cfg.setApiBaseUrl(deepSeekConfig.getApiBaseUrl());
            cfg.setApiKey(deepSeekConfig.getApiKey());
            cfg.setModelCode(deepSeekConfig.getModelCode());
            cfg.setMaxTokens(deepSeekConfig.getMaxTokens());
            cfg.setTemperature(deepSeekConfig.getTemperature());
            cfg.setTopP(1.0);
            cfg.setPresencePenalty(0.0);
            cfg.setFrequencyPenalty(0.0);
            cfg.setSystemPrompt(defaultSystemPrompt);
            aiConfigMapper.insert(cfg);
            log.info("未找到 AI 配置，已创建默认配置行: modelCode={}", cfg.getModelCode());
        }
        return cfg;
    }

    /**
     * API Key 脱敏：保留后 4 位
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 4) return "****";
        return "****" + key.substring(key.length() - 4);
    }
}
