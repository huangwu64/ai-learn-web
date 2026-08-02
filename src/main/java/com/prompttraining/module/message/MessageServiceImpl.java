package com.prompttraining.module.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prompttraining.ai.*;
import com.prompttraining.ai.config.AiConfig;
import com.prompttraining.ai.config.AiConfigService;
import com.prompttraining.common.BusinessException;
import com.prompttraining.common.Constant;
import com.prompttraining.common.PageResult;
import com.prompttraining.module.message.entity.Message;
import com.prompttraining.module.message.entity.dto.MessageResponse;
import com.prompttraining.module.message.entity.dto.MessageSendResponse;
import com.prompttraining.module.session.SessionMapper;
import com.prompttraining.module.session.SessionService;
import com.prompttraining.module.session.entity.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final SessionMapper sessionMapper;
    private final SessionService sessionService;
    private final AiProviderRegistry aiProviderRegistry;
    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;
    private final AiConfigService aiConfigService;

    @Value("${ai.context.max-messages:20}")
    private int maxContextMessages;

    /** 是否向 AI 注入模型身份（V3.1），使 AI 能如实回答"你是什么模型" */
    @Value("${ai.context.inject-model-identity:true}")
    private boolean injectModelIdentity;

    /** AI 流式调用专用线程池，避免占用 ForkJoinPool.commonPool() */
    private ThreadPoolTaskExecutor aiStreamExecutor;

    {
        aiStreamExecutor = new ThreadPoolTaskExecutor();
        aiStreamExecutor.setCorePoolSize(4);
        aiStreamExecutor.setMaxPoolSize(10);
        aiStreamExecutor.setQueueCapacity(50);
        aiStreamExecutor.setThreadNamePrefix("ai-stream-");
        aiStreamExecutor.initialize();
    }

    @Override
    @Transactional
    public MessageSendResponse sendMessage(String sessionId, String content) {
        Session session = sessionService.getById(sessionId);

        // 1. 保存用户消息
        Message userMsg = new Message();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setTokenCount(0);
        messageMapper.insert(userMsg);

        // 2. 构建对话上下文
        AiRequest aiRequest = buildAiRequest(session, content);

        // 3. 调用 AI Provider
        AiProvider provider = aiProviderRegistry.getActiveProvider();
        AiResponse aiResponse;
        try {
            aiResponse = provider.chatSync(aiRequest);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            throw new BusinessException(500, "AI 响应超时，请重试");
        }

        // 4. 保存 AI 回复
        Message assistantMsg = new Message();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse.getContent());
        assistantMsg.setTokenCount(aiResponse.getTokenCount() != null ? aiResponse.getTokenCount() : 0);
        assistantMsg.setModelCode(aiConfigService.getEffectiveModelCode());
        messageMapper.insert(assistantMsg);

        // 5. 更新会话消息数
        session.setMessageCount(session.getMessageCount() + 2);
        sessionMapper.updateById(session);

        // 6. 首轮对话时自动生成标题
        if (session.getMessageCount() <= 4) {
            String title = generateTitle(content, aiResponse.getContent());
            sessionService.updateTitle(sessionId, title);
            session.setTitle(title);
        }

        // 7. 构建响应
        MessageResponse userResp = new MessageResponse(
                userMsg.getId(), userMsg.getRole(), userMsg.getContent(),
                userMsg.getTokenCount(), userMsg.getModelCode(), userMsg.getCreatedAt()
        );
        MessageResponse assistantResp = new MessageResponse(
                assistantMsg.getId(), assistantMsg.getRole(), assistantMsg.getContent(),
                assistantMsg.getTokenCount(), assistantMsg.getModelCode(), assistantMsg.getCreatedAt()
        );

        return new MessageSendResponse(userResp, assistantResp, session.getTitle());
    }

    @Override
    public PageResult<Message> getMessages(String sessionId, Long cursor, int limit) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId);
        // 游标分页：取小于游标ID的消息
        if (cursor != null) {
            wrapper.lt(Message::getId, cursor);
        }
        wrapper.orderByDesc(Message::getId)
               .last("LIMIT " + (limit + 1)); // 多查一条判断是否有更多

        List<Message> messages = messageMapper.selectList(wrapper);
        boolean hasMore = messages.size() > limit;
        if (hasMore) {
            messages = messages.subList(0, limit);
        }

        // 反转为正序
        List<Message> orderedList = new ArrayList<>(messages);
        java.util.Collections.reverse(orderedList);

        Long nextCursor = orderedList.isEmpty() ? null : orderedList.get(0).getId();
        return new PageResult<>(orderedList, nextCursor, hasMore);
    }

    /**
     * 流式发送消息 - 返回 SSE 连接
     */
    public SseEmitter sendMessageStream(String sessionId, String content) {
        Session session = sessionService.getById(sessionId);
        SseEmitter emitter = new SseEmitter(0L); // 0 表示无超时

        // 1. 保存用户消息
        Message userMsg = new Message();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setTokenCount(0);
        messageMapper.insert(userMsg);

        // 2. 构建 AI 请求
        AiRequest aiRequest = buildAiRequest(session, content);

        // 3. 异步流式调用
        doStreamChat(session, emitter, aiRequest, content);

        return emitter;
    }

    @Override
    @Transactional
    public MessageSendResponse regenerateMessage(String sessionId, Long messageId) {
        Session session = sessionService.getById(sessionId);

        // 查找要重新生成的消息
        Message targetMsg = messageMapper.selectById(messageId);
        if (targetMsg == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!"assistant".equals(targetMsg.getRole())) {
            throw new BusinessException(400, "只能重新生成 AI 回复消息");
        }
        if (!targetMsg.getSessionId().equals(sessionId)) {
            throw new BusinessException(400, "消息不属于该会话");
        }

        // 软删除该 AI 回复
        messageMapper.deleteById(messageId);
        session.setMessageCount(Math.max(0, session.getMessageCount() - 1));
        sessionMapper.updateById(session);

        // 构建 AI 请求（使用该会话的现有消息历史，不包括被删除的消息）
        AiRequest aiRequest = buildAiRequest(session, null);

        // 调用 AI
        AiProvider provider = aiProviderRegistry.getActiveProvider();
        AiResponse aiResponse;
        try {
            aiResponse = provider.chatSync(aiRequest);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            throw new BusinessException(500, "AI 响应超时，请重试");
        }

        // 保存新的 AI 回复
        Message assistantMsg = new Message();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse.getContent());
        assistantMsg.setTokenCount(aiResponse.getTokenCount() != null ? aiResponse.getTokenCount() : 0);
        assistantMsg.setModelCode(aiConfigService.getEffectiveModelCode());
        messageMapper.insert(assistantMsg);

        session.setMessageCount(session.getMessageCount() + 1);
        sessionMapper.updateById(session);

        MessageResponse resp = new MessageResponse(
                assistantMsg.getId(), assistantMsg.getRole(), assistantMsg.getContent(),
                assistantMsg.getTokenCount(), assistantMsg.getModelCode(), assistantMsg.getCreatedAt()
        );

        return new MessageSendResponse(null, resp, null);
    }

    @Override
    public SseEmitter regenerateMessageStream(String sessionId, Long messageId) {
        Session session = sessionService.getById(sessionId);

        // 查找要重新生成的消息
        Message targetMsg = messageMapper.selectById(messageId);
        if (targetMsg == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!"assistant".equals(targetMsg.getRole())) {
            throw new BusinessException(400, "只能重新生成 AI 回复消息");
        }
        if (!targetMsg.getSessionId().equals(sessionId)) {
            throw new BusinessException(400, "消息不属于该会话");
        }

        // 软删除该 AI 回复
        messageMapper.deleteById(messageId);
        session.setMessageCount(Math.max(0, session.getMessageCount() - 1));
        sessionMapper.updateById(session);

        // 构建 AI 请求（不含刚删除的消息）
        AiRequest aiRequest = buildAiRequest(session, null);

        SseEmitter emitter = new SseEmitter(0L);
        doStreamChat(session, emitter, aiRequest, null);

        return emitter;
    }

    @Override
    @Transactional
    public void deleteMessage(String sessionId, Long messageId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!msg.getSessionId().equals(sessionId)) {
            throw new BusinessException(400, "消息不属于该会话");
        }

        messageMapper.deleteById(messageId);

        Session session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMessageCount(Math.max(0, session.getMessageCount() - 1));
            sessionMapper.updateById(session);
        }
    }

    @Override
    @Transactional
    public int clearMessages(String sessionId) {
        Session session = sessionService.getById(sessionId);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId);
        int count = messageMapper.selectCount(wrapper).intValue();

        messageMapper.delete(wrapper);

        session.setMessageCount(0);
        sessionMapper.updateById(session);

        return count;
    }

    /**
     * 构建 AI 请求上下文
     * 使用可配置的上下文窗口大小，支持 System Prompt
     */
    private AiRequest buildAiRequest(Session session, String userContent) {
        // 加载最近 N 条消息（N = maxContextMessages，默认20）
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, session.getId())
               .orderByDesc(Message::getCreatedAt)
               .last("LIMIT " + (maxContextMessages + 1)); // +1 容错
        List<Message> recentMsgs = messageMapper.selectList(wrapper);

        // 过滤掉刚保存的当前用户消息（按 content 和时间近似匹配）
        if (!recentMsgs.isEmpty() && userContent != null) {
            Message newest = recentMsgs.get(0);
            if ("user".equals(newest.getRole()) && userContent.equals(newest.getContent())) {
                recentMsgs.remove(0);
            }
        }

        java.util.Collections.reverse(recentMsgs);

        // 智能上下文裁剪：如果消息数超过窗口，保留最近的消息
        if (recentMsgs.size() > maxContextMessages) {
            recentMsgs = recentMsgs.subList(recentMsgs.size() - maxContextMessages, recentMsgs.size());
        }

        // V3：AI 配置从 ai_config 动态读取（管理员界面可调）
        AiConfig cfg = aiConfigService.getConfig();
        String systemPrompt = cfg.getSystemPrompt() != null && !cfg.getSystemPrompt().isBlank()
                ? cfg.getSystemPrompt() : null;

        // V3.1：向 AI 注入模型身份，使其能如实回答"你是什么模型"
        if (injectModelIdentity) {
            String identity = "你当前被调用的模型编码是 " + aiConfigService.getEffectiveModelCode()
                    + "（DeepSeek 大模型）。当用户询问你的模型名称/身份时，请如实告知。";
            systemPrompt = (identity + (systemPrompt != null ? "\n\n" + systemPrompt : "")).trim();
        }

        List<AiRequest.MessageItem> items = new ArrayList<>();

        // 添加 System Prompt（V3：从管理员动态配置读取）
        if (systemPrompt != null) {
            items.add(new AiRequest.MessageItem("system", systemPrompt));
        }

        for (Message msg : recentMsgs) {
            items.add(new AiRequest.MessageItem(msg.getRole(), msg.getContent()));
        }
        // 追加当前用户消息
        if (userContent != null) {
            items.add(new AiRequest.MessageItem("user", userContent));
        }

        log.debug("构建 AI 请求: 历史消息 {} 条, systemPrompt={}, 总消息 {} 条",
                recentMsgs.size(), systemPrompt != null, items.size());

        AiRequest request = new AiRequest();
        request.setModelCode(aiConfigService.getEffectiveModelCode());
        request.setMessages(items);
        request.setSystemPrompt(systemPrompt);
        request.setMaxTokens(cfg.getMaxTokens());
        request.setTemperature(cfg.getTemperature());
        request.setTopP(cfg.getTopP());
        request.setPresencePenalty(cfg.getPresencePenalty());
        request.setFrequencyPenalty(cfg.getFrequencyPenalty());
        return request;
    }

    /**
     * 执行流式 AI 对话（通用方法，发送消息和重新生成共用）
     */
    private void doStreamChat(Session session, SseEmitter emitter, AiRequest aiRequest, String userContent) {
        String sessionId = session.getId();
        String modelCode = session.getModelCode();

        CompletableFuture.runAsync(() -> {
            StringBuilder fullContent = new StringBuilder();

            try {
                AiProvider provider = aiProviderRegistry.getActiveProvider();
                provider.chatStream(aiRequest, new StreamCallback() {
                    @Override
                    public void onChunk(String chunk) {
                        fullContent.append(chunk);
                        try {
                            Map<String, Object> eventData = new LinkedHashMap<>();
                            eventData.put("type", "content");
                            eventData.put("content", chunk);
                            String json = objectMapper.writeValueAsString(eventData);
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(json));
                        } catch (IOException e) {
                            log.error("SSE 推送失败", e);
                        }
                    }

                    @Override
                    public void onReasoning(String reasoning) {
                        try {
                            Map<String, Object> eventData = new LinkedHashMap<>();
                            eventData.put("type", "reasoning");
                            eventData.put("content", reasoning);
                            String json = objectMapper.writeValueAsString(eventData);
                            emitter.send(SseEmitter.event()
                                    .name("reasoning")
                                    .data(json));
                        } catch (IOException e) {
                            log.error("SSE 推理内容推送失败", e);
                        }
                    }

                    @Override
                    public void onComplete(AiResponse response) {
                        // 保存 AI 消息
                        Message assistantMsg = new Message();
                        assistantMsg.setSessionId(sessionId);
                        assistantMsg.setRole("assistant");
                        assistantMsg.setContent(fullContent.toString());
                        assistantMsg.setTokenCount(response.getTokenCount() != null ? response.getTokenCount() : 0);
                        assistantMsg.setModelCode(aiConfigService.getEffectiveModelCode());
                        messageMapper.insert(assistantMsg);

                        // 更新会话
                        int increment = userContent != null ? 2 : 1;
                        session.setMessageCount(session.getMessageCount() + increment);
                        sessionMapper.updateById(session);

                        // 首轮对话时生成标题
                        String title = session.getTitle();
                        if (session.getMessageCount() <= 4 && userContent != null) {
                            title = generateTitle(userContent, fullContent.toString());
                            sessionService.updateTitle(sessionId, title);
                        }

                        // 发送完成事件
                        try {
                            Map<String, Object> eventData = new LinkedHashMap<>();
                            eventData.put("type", "done");
                            eventData.put("userMessageId", userContent != null ? 0 : 0);
                            eventData.put("messageId", assistantMsg.getId());
                            eventData.put("tokenCount", response.getTokenCount() != null ? response.getTokenCount() : 0);
                            eventData.put("sessionTitle", title);
                            String json = objectMapper.writeValueAsString(eventData);
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(json));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("SSE 完成事件发送失败", e);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("AI 流式调用出错", error);
                        try {
                            Map<String, Object> eventData = new LinkedHashMap<>();
                            eventData.put("type", "error");
                            eventData.put("message", "AI 响应出错，请重试");
                            String json = objectMapper.writeValueAsString(eventData);
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(json));
                            emitter.complete();
                        } catch (IOException e) {
                            log.error("SSE 错误事件发送失败", e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("AI 流式调用异常", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }, aiStreamExecutor.getThreadPoolExecutor());
    }

    /**
     * 基于首轮对话内容生成标题（截取用户消息的前30字）
     */
    private String generateTitle(String userContent, String aiContent) {
        String source = userContent.trim();
        if (source.length() > 30) {
            source = source.substring(0, 30);
        }
        return source;
    }
}
