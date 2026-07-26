package com.prompttraining.module.message.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class MessageSendRequest {

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
