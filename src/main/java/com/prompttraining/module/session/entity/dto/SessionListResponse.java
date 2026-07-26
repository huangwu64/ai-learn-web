package com.prompttraining.module.session.entity.dto;

import lombok.Data;

/**
 * 会话列表项响应
 */
@Data
public class SessionListResponse {

    private String id;
    private String title;
    private String lastMessage;
    private Integer messageCount;
    private String modelCode;
    private String updatedAt;
}
