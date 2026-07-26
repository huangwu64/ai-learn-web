package com.prompttraining.module.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 流式响应管理服务
 * 管理与前端的 Server-Sent Events 连接
 */
@Slf4j
@Service
public class SseEmitterService {

    /** 存储活跃的 SSE 连接（sessionId -> emitter） */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册 SSE 连接
     */
    public void register(String sessionId, SseEmitter emitter) {
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成: sessionId={}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE 连接超时: sessionId={}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onError(e -> {
            log.warn("SSE 连接异常: sessionId={}", sessionId, e);
            emitters.remove(sessionId);
        });
    }

    /**
     * 获取指定会话的 SSE 连接
     */
    public SseEmitter getEmitter(String sessionId) {
        return emitters.get(sessionId);
    }
}
