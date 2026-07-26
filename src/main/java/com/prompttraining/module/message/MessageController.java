package com.prompttraining.module.message;

import com.prompttraining.common.PageResult;
import com.prompttraining.common.Result;
import com.prompttraining.module.message.entity.Message;
import com.prompttraining.module.message.entity.dto.MessageResponse;
import com.prompttraining.module.message.entity.dto.MessageSendRequest;
import com.prompttraining.module.message.entity.dto.MessageSendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息管理接口 - V2：增加重新生成、删除、清空功能
 */
@Slf4j
@Tag(name = "消息管理")
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "发送消息（同步）")
    @PostMapping
    public Result<MessageSendResponse> sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody MessageSendRequest request) {
        log.info(">>> [同步] 收到消息请求: sessionId={}, contentLength={}", sessionId, request.getContent().length());
        MessageSendResponse response = messageService.sendMessage(sessionId, request.getContent());
        log.info("<<< [同步] 消息处理完成: sessionId={}, assistantMsgLength={}", sessionId,
                response.getAssistantMessage().getContent().length());
        return Result.ok(response);
    }

    @Operation(summary = "发送消息（SSE流式）")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @PathVariable String sessionId,
            @Valid @RequestBody MessageSendRequest request) {
        log.info(">>> [SSE流式] 收到消息请求: sessionId={}, content=\"{}\"", sessionId, request.getContent());
        SseEmitter emitter = messageService.sendMessageStream(sessionId, request.getContent());
        log.info("<<< [SSE流式] SseEmitter 已返回: sessionId={}", sessionId);
        return emitter;
    }

    @Operation(summary = "获取消息历史（游标分页）")
    @GetMapping
    public Result<PageResult<MessageResponse>> getMessages(
            @PathVariable String sessionId,
            @Parameter(description = "游标，取小于此ID的消息") @RequestParam(required = false) Long cursor,
            @Parameter(description = "每页条数，默认50") @RequestParam(required = false, defaultValue = "50") int limit) {
        log.debug("获取消息历史: sessionId={}, cursor={}, limit={}", sessionId, cursor, limit);
        PageResult<Message> page = messageService.getMessages(sessionId, cursor, limit);
        List<MessageResponse> list = page.getList().stream()
                .map(m -> new MessageResponse(
                        m.getId(), m.getRole(), m.getContent(),
                        m.getTokenCount(), m.getModelCode(), m.getCreatedAt()))
                .collect(Collectors.toList());
        return Result.ok(new PageResult<>(list, page.getNextCursor(), page.getHasMore()));
    }

    @Operation(summary = "重新生成 AI 回复（同步，V2 新增）")
    @PostMapping("/{messageId}/regenerate")
    public Result<MessageSendResponse> regenerateMessage(
            @PathVariable String sessionId,
            @PathVariable Long messageId) {
        log.info(">>> 重新生成消息: sessionId={}, messageId={}", sessionId, messageId);
        MessageSendResponse response = messageService.regenerateMessage(sessionId, messageId);
        return Result.ok(response);
    }

    @Operation(summary = "重新生成 AI 回复（SSE流式，V2 新增）")
    @PostMapping(value = "/{messageId}/regenerate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter regenerateMessageStream(
            @PathVariable String sessionId,
            @PathVariable Long messageId) {
        log.info(">>> 流式重新生成消息: sessionId={}, messageId={}", sessionId, messageId);
        return messageService.regenerateMessageStream(sessionId, messageId);
    }

    @Operation(summary = "删除单条消息（V2 新增）")
    @DeleteMapping("/{messageId}")
    public Result<?> deleteMessage(
            @PathVariable String sessionId,
            @PathVariable Long messageId) {
        messageService.deleteMessage(sessionId, messageId);
        return Result.ok(null, "删除成功");
    }

    @Operation(summary = "清空会话所有消息（V2 新增）")
    @DeleteMapping
    public Result<?> clearMessages(@PathVariable String sessionId) {
        int count = messageService.clearMessages(sessionId);
        return Result.ok(Map.of("deletedCount", count), "已清空会话所有消息");
    }
}
