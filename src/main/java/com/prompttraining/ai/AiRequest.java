package com.prompttraining.ai;

import lombok.Data;

import java.util.List;

/**
 * AI 统一请求对象
 */
@Data
public class AiRequest {
    /** 模型编码 */
    private String modelCode;
    /** 对话上下文 */
    private List<MessageItem> messages;
    /** 最大输出 token */
    private Integer maxTokens;
    /** 温度参数（0-1） */
    private Double temperature;
    /** 系统级指令（V2 新增） */
    private String systemPrompt;

    @Data
    public static class MessageItem {
        /** 角色：user / assistant / system */
        private String role;
        /** 消息正文 */
        private String content;

        public MessageItem() {}

        public MessageItem(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
