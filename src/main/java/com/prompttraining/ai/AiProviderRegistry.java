package com.prompttraining.ai;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Provider 注册中心 - 管理所有已注册的 Provider 实例
 */
@Component
public class AiProviderRegistry {

    private final Map<String, AiProvider> providers = new ConcurrentHashMap<>();

    /**
     * 注册一个 Provider
     */
    public void register(AiProvider provider) {
        providers.put(provider.getModelCode(), provider);
    }

    /**
     * 根据模型编码获取 Provider，不存在则抛出异常
     */
    public AiProvider getProvider(String modelCode) {
        AiProvider provider = providers.get(modelCode);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的模型: " + modelCode);
        }
        return provider;
    }
}
