package com.prompttraining.ai;

/**
 * 流式回调接口 - 用于接收 AI 逐块返回的内容
 */
public interface StreamCallback {

    /**
     * 收到一个新的内容片段
     */
    void onChunk(String content);

    /**
     * 收到一段推理内容（reasoning_content，V3.1 新增）
     * 推理模型（如 deepseek-v4-flash）在输出正式回答前会先输出推理过程，
     * 该内容独立于正式回答，调用方可按需展示。
     * 默认空实现，兼容不需要展示推理内容的调用方。
     */
    default void onReasoning(String reasoning) {
        // 默认不处理
    }

    /**
     * 流式传输完成
     */
    void onComplete(AiResponse response);

    /**
     * 发生错误
     */
    void onError(Throwable error);
}
