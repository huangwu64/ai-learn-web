package com.prompttraining.ai;

import lombok.Data;

/**
 * AI 统一响应对象
 */
@Data
public class AiResponse {
    /** AI 回复内容 */
    private String content;
    /** 实际使用的模型 */
    private String modelCode;
    /** 消耗 token 数 */
    private Integer tokenCount;
    /** 结束原因：stop / length / error */
    private String finishReason;
}
