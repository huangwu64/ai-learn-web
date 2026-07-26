package com.prompttraining.module.message.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发送消息的完整响应（包含用户消息、AI回复、会话标题）
 */
@Data
@AllArgsConstructor
public class MessageSendResponse {

    private MessageResponse userMessage;
    private MessageResponse assistantMessage;
    private String sessionTitle;
}
