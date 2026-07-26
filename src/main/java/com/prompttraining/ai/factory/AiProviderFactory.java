package com.prompttraining.ai.factory;

import com.prompttraining.ai.AiProvider;
import com.prompttraining.ai.AiProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI Provider 工厂 - 应用启动就绪后自动注册所有 Provider 实现
 * 使用 ApplicationReadyEvent 确保所有 Bean（包括 DeepSeekProvider）已完全初始化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderFactory {

    private final List<AiProvider> providers;
    private final AiProviderRegistry registry;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("开始注册 AI Provider，共 {} 个实现", providers.size());
        providers.forEach(p -> {
            registry.register(p);
            log.info("已注册 AI Provider: {}", p.getModelCode());
        });
    }
}
