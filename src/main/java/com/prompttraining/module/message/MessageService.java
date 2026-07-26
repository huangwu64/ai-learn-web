package com.prompttraining.module.message;

import com.prompttraining.common.PageResult;
import com.prompttraining.module.message.entity.Message;
import com.prompttraining.module.message.entity.dto.MessageSendResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 同步发送消息并获取 AI 回复
     */
    MessageSendResponse sendMessage(String sessionId, String content);

    /**
     * 流式发送消息，返回 SSE 连接
     */
    SseEmitter sendMessageStream(String sessionId, String content);

    /**
     * 获取会话消息列表（游标分页，按时间正序）
     */
    PageResult<Message> getMessages(String sessionId, Long cursor, int limit);

    /**
     * 重新生成 AI 回复（V2 新增）
     */
    MessageSendResponse regenerateMessage(String sessionId, Long messageId);

    /**
     * 流式重新生成 AI 回复（V2 新增）
     */
    SseEmitter regenerateMessageStream(String sessionId, Long messageId);

    /**
     * 软删除单条消息（V2 新增）
     */
    void deleteMessage(String sessionId, Long messageId);

    /**
     * 清空会话所有消息（V2 新增）
     */
    int clearMessages(String sessionId);
}
