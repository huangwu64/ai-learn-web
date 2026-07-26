package com.prompttraining.module.session.entity.dto;

import lombok.Data;

/**
 * 创建会话请求
 */
@Data
public class SessionCreateRequest {

    /** 模型编码，可选，默认 deepseek-chat */
    private String modelCode;
}
