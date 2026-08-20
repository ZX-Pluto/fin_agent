package com.huawei.fin.ai.material.common.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.common.util.JsonUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.huawei.fin.ai.material.common.modelconfig.ModelConfigVO;

@Component
public class AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayClient.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int DEFAULT_TIMEOUT_MS = 300_000;
    private static final int MAX_RETRY = 2;

    private final RestClient.Builder restClientBuilder;

    public AiGatewayClient(RestClient.Builder builder) {
        this.restClientBuilder = builder;
    }

    public LlmCallResult chat(ModelConfigVO model, String systemPrompt, String userPrompt) {
        return chat(model, systemPrompt, userPrompt, null);
    }

    public LlmCallResult chat(ModelConfigVO model, String systemPrompt, String userPrompt, Integer maxTokens) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            long start = System.currentTimeMillis();
            try {
                LlmCallResult result = "ANTHROPIC_COMPATIBLE".equalsIgnoreCase(model.getProvider())
                        ? chatAnthropic(model, systemPrompt, userPrompt, maxTokens, start)
                        : chatOpenAi(model, systemPrompt, userPrompt, maxTokens, start);
                if (!result.success() && retryable(result.message())) {
                    log.warn("LLM 调用可重试失败（第{}次）: {}", attempt, result.message());
                } else {
                    return result;
                }
            } catch (RestClientResponseException e) {
                String body = e.getResponseBodyAsString();
                String detail = body == null || body.isBlank() ? e.getMessage() : body;
                log.warn("LLM 调用失败: HTTP {} {}", e.getStatusCode().value(), detail);
                return LlmCallResult.fail("HTTP " + e.getStatusCode().value() + ": " + detail,
                        System.currentTimeMillis() - start);
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                log.warn("LLM 调用失败: {}", message);
                if (!retryable(message) || attempt == MAX_RETRY) {
                    return LlmCallResult.fail(message, System.currentTimeMillis() - start);
                }
            }
            try {
                Thread.sleep(1500L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return LlmCallResult.fail("LLM 调用重试被中断", System.currentTimeMillis() - start);
            }
        }
        return LlmCallResult.fail("LLM 调用重试失败", 0L);
    }

    private boolean retryable(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("read timed out")
                || lower.contains("connect timed out")
                || lower.contains("connection reset")
                || lower.contains("timeout");
    }

    private LlmCallResult chatAnthropic(ModelConfigVO model, String systemPrompt, String userPrompt,
                                        Integer maxTokens, long start) {
        RestClient restClient = restClientFor(model);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model.getModelName());
        body.put("max_tokens", maxTokens != null
                ? maxTokens
                : model.getTimeoutSeconds() != null
                ? Math.max(512, model.getTimeoutSeconds() * 4)
                : 512);
        body.put("thinking", Map.of("type", "disabled"));
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }
        body.put("messages", List.of(Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));

        String raw = restClient.post()
                .uri(model.getBaseUrl() + "/v1/messages")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("x-api-key", model.getApiKey() == null ? "" : model.getApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .body(body)
                .exchange((request, response) -> {
                    try (InputStream in = response.getBody()) {
                        byte[] bytes = in == null ? new byte[0] : in.readAllBytes();
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                });
        Map<String, Object> response = parseResponse(raw, start);
        if (response == null) {
            return LlmCallResult.fail("模型未返回内容", System.currentTimeMillis() - start);
        }

        StringBuilder content = new StringBuilder();
        Object contentObj = response.get("content");
        if (contentObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m && "text".equals(m.get("type")) && m.get("text") != null) {
                    content.append(m.get("text"));
                }
            }
        }
        if (content.isEmpty()) {
            return LlmCallResult.fail("模型未返回文本内容", System.currentTimeMillis() - start);
        }
        return LlmCallResult.ok(content.toString(), System.currentTimeMillis() - start,
                extractInt(response, "usage", "input_tokens"), extractInt(response, "usage", "output_tokens"));
    }

    private LlmCallResult chatOpenAi(ModelConfigVO model, String systemPrompt, String userPrompt,
                                     Integer maxTokens, long start) {
        RestClient restClient = restClientFor(model);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model.getModelName());
        if (supportsThinkingDisabled(model)) {
            body.put("thinking", Map.of("type", "disabled"));
        }
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));
        if (model.getTemperature() != null) {
            body.put("temperature", model.getTemperature().doubleValue());
        }
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }

        String raw = restClient.post()
                .uri(model.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + (model.getApiKey() == null ? "" : model.getApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> {
                    try (InputStream in = response.getBody()) {
                        byte[] bytes = in == null ? new byte[0] : in.readAllBytes();
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                });
        Map<String, Object> response = parseResponse(raw, start);
        if (response == null) {
            return LlmCallResult.fail("模型未返回内容", System.currentTimeMillis() - start);
        }
        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> choice) {
                Object message = choice.get("message");
                if (message instanceof Map<?, ?> m && m.get("content") != null) {
                    String content = String.valueOf(m.get("content"));
                    return LlmCallResult.ok(content, System.currentTimeMillis() - start,
                            extractInt(response, "usage", "prompt_tokens"),
                            extractInt(response, "usage", "completion_tokens"));
                }
            }
        }
        return LlmCallResult.fail("模型未返回文本内容", System.currentTimeMillis() - start);
    }

    private boolean supportsThinkingDisabled(ModelConfigVO model) {
        String baseUrl = model.getBaseUrl() == null ? "" : model.getBaseUrl().toLowerCase();
        String modelName = model.getModelName() == null ? "" : model.getModelName().toLowerCase();
        return baseUrl.contains("volces.com")
                || baseUrl.contains("ark")
                || modelName.contains("doubao");
    }

    private RestClient restClientFor(ModelConfigVO model) {
        int timeoutSeconds = model.getTimeoutSeconds() == null || model.getTimeoutSeconds() < 1
                ? DEFAULT_TIMEOUT_MS / 1000
                : model.getTimeoutSeconds();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return restClientBuilder.clone().requestFactory(factory).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String raw, long start) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("模型返回空内容");
        }
        try {
            Map<String, Object> map = JsonUtil.fromJson(raw, new TypeReference<Map<String, Object>>() {
            });
            if (map == null) {
                throw new IllegalStateException("模型返回内容无法解析为 JSON");
            }
            return map;
        } catch (Exception e) {
            throw new IllegalStateException("模型响应解析失败: " + e.getMessage() + "，原始内容: " + abbreviate(raw));
        }
    }

    private String abbreviate(String raw) {
        return raw.length() > 300 ? raw.substring(0, 300) + "..." : raw;
    }

    @SuppressWarnings("unchecked")
    private Integer extractInt(Map<String, Object> response, String outerKey, String innerKey) {
        try {
            Object outer = response.get(outerKey);
            if (outer instanceof Map<?, ?> map && map.get(innerKey) instanceof Number number) {
                return number.intValue();
            }
        } catch (Exception e) {
            log.debug("Extract usage failed: {}", e.getMessage());
        }
        return null;
    }
}
