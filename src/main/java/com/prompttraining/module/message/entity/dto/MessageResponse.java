package com.prompttraining.module.message.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息响应
 */
@Data
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private String role;
    private String content;
    private Integer tokenCount;
    private String modelCode;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
