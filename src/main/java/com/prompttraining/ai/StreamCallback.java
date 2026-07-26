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
     * 流式传输完成
     */
    void onComplete(AiResponse response);

    /**
     * 发生错误
     */
    void onError(Throwable error);
}
