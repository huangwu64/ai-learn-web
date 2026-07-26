package com.prompttraining.ai.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompttraining.ai.*;
import com.prompttraining.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek AI Provider 实现
 * 
 * 支持两种调用方式：
 * 1. chatSync: 同步调用，返回完整回复
 * 2. chatStream: 流式调用（SSE），逐块返回内容
 * 
 * API 文档参考：https://platform.deepseek.com/api-docs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekProvider implements AiProvider {

    private final DeepSeekConfig config;
    private final ObjectMapper objectMapper;
    private WebClient webClient;

    /**
     * 获取或创建 WebClient 实例（懒加载）
     */
    private WebClient getWebClient() {
        if (webClient == null) {
            this.webClient = WebClient.builder()
                    .baseUrl(config.getApiBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return webClient;
    }

    @Override
    public String getModelCode() {
        return config.getModelCode();
    }

    @Override
    public AiResponse chatSync(AiRequest request) {
        log.info(">>> DeepSeek 同步调用: model={}, messagesCount={}, maxTokens={}",
                request.getModelCode(), request.getMessages().size(), request.getMaxTokens());
        try {
            Map<String, Object> body = buildRequestBody(request, false);

            // 调用 DeepSeek Chat API（非流式）
            String responseJson = getWebClient().post()
                    .uri("/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(500, "AI 返回结果为空");
            }

            JsonNode message = choices.get(0).get("message");
            String content = message.get("content").asText();
            int tokenCount = root.path("usage").path("total_tokens").asInt(0);

            AiResponse response = new AiResponse();
            response.setContent(content);
            response.setModelCode(config.getModelCode());
            response.setTokenCount(tokenCount);
            response.setFinishReason(choices.get(0).path("finish_reason").asText("stop"));
            log.info("<<< DeepSeek 同步响应: contentLength={}, tokenCount={}, finishReason={}",
                    content.length(), tokenCount, response.getFinishReason());
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            throw new BusinessException(500, "AI 服务暂不可用，请稍后重试");
        }
    }

    @Override
    public void chatStream(AiRequest request, StreamCallback callback) {
        log.info(">>> DeepSeek 流式调用: model={}, messagesCount={}",
                request.getModelCode(), request.getMessages().size());
        try {
            Map<String, Object> body = buildRequestBody(request, true);

            // 用数组持有 tokenCount，以便在 lambda 中修改
            final int[] totalTokens = {0};

            // 调用 DeepSeek Chat API（流式 SSE）
            getWebClient().post()
                    .uri("/v1/chat/completions")
                    .bodyValue(body)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .doOnNext(chunk -> {
                        log.debug("DeepSeek 原始响应块 (长度={}): {}", chunk.length(), chunk.substring(0, Math.min(500, chunk.length())));
                        // DeepSeek 流式响应通过 WebClient bodyToFlux 后，
                        // 每行是纯 JSON 或 "[DONE]"，不带 "data:" 前缀
                        for (String line : chunk.split("\n")) {
                            String jsonStr = line.trim();
                            if (jsonStr.isEmpty()) continue;

                            // 兼容 "data: {...}" 格式（如果有的话）
                            if (jsonStr.startsWith("data:")) {
                                jsonStr = jsonStr.substring(5).trim();
                            }
                            if (jsonStr.isEmpty() || "[DONE]".equals(jsonStr)) continue;

                            // 非 JSON 行跳过
                            if (!jsonStr.startsWith("{")) continue;

                            try {
                                JsonNode node = objectMapper.readTree(jsonStr);
                                // 检查是否有错误响应
                                JsonNode errorNode = node.get("error");
                                if (errorNode != null) {
                                    log.error("DeepSeek API 返回错误: {}", errorNode);
                                    return;
                                }
                                JsonNode choicesNode = node.get("choices");
                                if (choicesNode != null && !choicesNode.isEmpty()) {
                                    JsonNode delta = choicesNode.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        String content = delta.get("content").asText();
                                        if (!content.isEmpty()) {
                                            callback.onChunk(content);
                                        }
                                    }
                                }
                                // 提取 usage 信息（出现在最后一个 chunk 中）
                                JsonNode usageNode = node.get("usage");
                                if (usageNode != null && usageNode.has("total_tokens")) {
                                    totalTokens[0] = usageNode.get("total_tokens").asInt();
                                }
                            } catch (Exception e) {
                                log.warn("SSE 数据解析异常: {}", line, e);
                            }
                        }
                    })
                    .doOnComplete(() -> {
                        log.info("<<< DeepSeek 流式完成: tokenCount={}", totalTokens[0]);
                        AiResponse response = new AiResponse();
                        response.setContent("");
                        response.setModelCode(config.getModelCode());
                        response.setTokenCount(totalTokens[0]);
                        response.setFinishReason("stop");
                        callback.onComplete(response);
                    })
                    .doOnError(e -> {
                        log.error("DeepSeek 流式调用失败", e);
                        callback.onError(e);
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("DeepSeek 流式请求构建失败", e);
            callback.onError(e);
        }
    }

    @Override
    public boolean healthCheck() {
        try {
            Map<String, Object> body = buildRequestBody(
                    new AiRequest() {{
                        setModelCode(config.getModelCode());
                        setMessages(List.of(new AiRequest.MessageItem("user", "ping")));
                        setMaxTokens(1);
                    }},
                    false
            );
            getWebClient().post()
                    .uri("/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("DeepSeek 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建 DeepSeek API 请求体
     */
    private Map<String, Object> buildRequestBody(AiRequest request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelCode());
        body.put("messages", request.getMessages().stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList());
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens());
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : config.getTemperature());
        body.put("stream", stream);
        return body;
    }
}
