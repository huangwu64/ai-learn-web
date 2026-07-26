package com.prompttraining.ai;

/**
 * AI Provider 统一接口 - 屏蔽不同厂商差异，支持同步和流式两种调用方式
 */
public interface AiProvider {

    /**
     * 同步方式调用 AI，返回完整回复
     *
     * @param request 统一请求对象
     * @return AI 完整回复
     */
    AiResponse chatSync(AiRequest request);

    /**
     * 流式方式调用 AI，通过回调逐块返回内容
     *
     * @param request  统一请求对象
     * @param callback 流式回调
     */
    void chatStream(AiRequest request, StreamCallback callback);

    /**
     * 返回该 Provider 支持的模型编码
     */
    String getModelCode();

    /**
     * 健康检查，验证 API Key 是否有效
     */
    boolean healthCheck();
}
