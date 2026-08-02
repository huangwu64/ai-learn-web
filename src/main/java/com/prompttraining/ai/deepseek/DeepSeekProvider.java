package com.prompttraining.ai.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompttraining.ai.*;
import com.prompttraining.ai.config.AiConfigService;
import com.prompttraining.ai.config.dto.AiConfigUpdateRequest;
import com.prompttraining.common.BusinessException;
import com.prompttraining.common.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DeepSeek AI Provider 实现
 *
 * V3 改造：API 地址 / Key / 模型编码 / 参数均从 ai_config 动态配置读取，
 * 管理员界面修改后即时生效，无需重启服务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekProvider implements AiProvider {

    private final AiConfigService aiConfigService;
    private final ObjectMapper objectMapper;

    private volatile WebClient webClient;
    private volatile String cachedBaseUrl;
    private volatile String cachedApiKey;

    /**
     * 获取或重建 WebClient（懒加载 + 配置变更时自动重建）
     */
    private WebClient getWebClient() {
        String baseUrl = aiConfigService.getEffectiveApiBaseUrl();
        String apiKey = aiConfigService.getEffectiveApiKey();

        WebClient client = webClient;
        if (client == null || !Objects.equals(cachedBaseUrl, baseUrl) || !Objects.equals(cachedApiKey, apiKey)) {
            synchronized (this) {
                client = webClient;
                if (client == null || !Objects.equals(cachedBaseUrl, baseUrl) || !Objects.equals(cachedApiKey, apiKey)) {
                    client = WebClient.builder()
                            .baseUrl(baseUrl)
                            .defaultHeader("Authorization", "Bearer " + apiKey)
                            .defaultHeader("Content-Type", "application/json")
                            .build();
                    webClient = client;
                    cachedBaseUrl = baseUrl;
                    cachedApiKey = apiKey;
                    log.info("DeepSeek WebClient 已重建: baseUrl={}", baseUrl);
                }
            }
        }
        return client;
    }

    @Override
    public String getModelCode() {
        return Constant.AI_PROVIDER_CODE;
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
            // 推理模型可能 content 为 null，兜底取 reasoning_content
            JsonNode contentNode = message.get("content");
            String content = (contentNode != null && contentNode.isTextual())
                    ? contentNode.asText()
                    : message.path("reasoning_content").asText("");
            int tokenCount = root.path("usage").path("total_tokens").asInt(0);

            AiResponse response = new AiResponse();
            response.setContent(content);
            response.setModelCode(getEffectiveModel(request));
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
                        log.debug("DeepSeek 原始响应块 (长度={})", chunk.length());
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
                                    if (delta != null) {
                                        // 正式回答内容（推理模型在推理阶段 content 为 null，需跳过）
                                        JsonNode contentNode = delta.get("content");
                                        if (contentNode != null && contentNode.isTextual()) {
                                            String content = contentNode.asText();
                                            if (!content.isEmpty()) {
                                                callback.onChunk(content);
                                            }
                                        }
                                        // 推理内容（deepseek-v4-flash 等推理模型的思考过程）
                                        JsonNode reasoningNode = delta.get("reasoning_content");
                                        if (reasoningNode != null && reasoningNode.isTextual()) {
                                            String reasoning = reasoningNode.asText();
                                            if (!reasoning.isEmpty()) {
                                                callback.onReasoning(reasoning);
                                            }
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
                        response.setModelCode(getEffectiveModel(request));
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

    /**
     * 获取当前账号可用的模型列表（V3：调用 OpenAI 兼容的 GET /models 接口）
     *
     * @return 模型编码列表；获取失败时返回空列表
     */
    public List<String> listModels() {
        try {
            String json = getWebClient().get()
                    .uri("/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                List<String> models = new ArrayList<>();
                for (JsonNode node : data) {
                    String id = node.path("id").asText("");
                    if (!id.isEmpty()) {
                        models.add(id);
                    }
                }
                log.info("获取可用模型列表成功: {}", models);
                return models;
            }
            return List.of();
        } catch (Exception e) {
            log.warn("获取模型列表失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean healthCheck() {
        return healthCheck(null);
    }

    /**
     * 健康检查（V3）：可用待保存的配置覆盖值测试连接
     */
    public boolean healthCheck(AiConfigUpdateRequest override) {
        String baseUrl = override != null && override.getApiBaseUrl() != null && !override.getApiBaseUrl().isBlank()
                ? override.getApiBaseUrl().trim() : aiConfigService.getEffectiveApiBaseUrl();
        String apiKey = override != null && override.getApiKey() != null && !override.getApiKey().isBlank()
                ? override.getApiKey().trim() : aiConfigService.getEffectiveApiKey();
        String modelCode = override != null && override.getModelCode() != null && !override.getModelCode().isBlank()
                ? override.getModelCode().trim() : aiConfigService.getEffectiveModelCode();
        try {
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelCode);
            body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
            body.put("max_tokens", 1);
            body.put("stream", false);
            client.post()
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
     * 构建 DeepSeek API 请求体（V3：从动态配置取值，请求级参数优先）
     */
    private Map<String, Object> buildRequestBody(AiRequest request, boolean stream) {
        var cfg = aiConfigService.getConfig();
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModelCode() != null ? request.getModelCode() : aiConfigService.getEffectiveModelCode());
        body.put("messages", request.getMessages().stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .toList());
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : cfg.getMaxTokens());
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : cfg.getTemperature());
        Double topP = request.getTopP() != null ? request.getTopP() : cfg.getTopP();
        if (topP != null) body.put("top_p", topP);
        Double presencePenalty = request.getPresencePenalty() != null ? request.getPresencePenalty() : cfg.getPresencePenalty();
        if (presencePenalty != null) body.put("presence_penalty", presencePenalty);
        Double frequencyPenalty = request.getFrequencyPenalty() != null ? request.getFrequencyPenalty() : cfg.getFrequencyPenalty();
        if (frequencyPenalty != null) body.put("frequency_penalty", frequencyPenalty);
        body.put("stream", stream);
        return body;
    }

    /**
     * 获取实际生效的模型编码（请求级优先，否则取动态配置）
     */
    private String getEffectiveModel(AiRequest request) {
        return request.getModelCode() != null ? request.getModelCode() : aiConfigService.getEffectiveModelCode();
    }
}
