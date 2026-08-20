package com.huawei.fin.ai.material.common.client;

public record LlmCallResult(
        boolean success,
        String message,
        String content,
        long latencyMs,
        Integer inputTokens,
        Integer outputTokens) {

    public static LlmCallResult ok(String content, long latencyMs, Integer inputTokens, Integer outputTokens) {
        return new LlmCallResult(true, "ok", content, latencyMs, inputTokens, outputTokens);
    }

    public static LlmCallResult fail(String message, long latencyMs) {
        return new LlmCallResult(false, message, null, latencyMs, null, null);
    }
}
