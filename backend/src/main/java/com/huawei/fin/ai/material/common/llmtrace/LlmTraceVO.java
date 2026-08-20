package com.huawei.fin.ai.material.common.llmtrace;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LlmTraceVO {

    private Long id;
    private Long taskId;
    private Long materialId;
    private String agentName;
    private String skillName;
    private String modelName;
    private String provider;
    private String prompt;
    private String response;
    private Integer inputTokens;
    private Integer outputTokens;
    private Long latencyMs;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
}
