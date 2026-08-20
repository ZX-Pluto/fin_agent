package com.huawei.fin.ai.material.validation.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ValidationResultVO {

    private Long id;
    private Long taskId;
    private Long materialId;
    private Long ruleId;
    private String ruleCode;
    private String category;
    private String severity;
    private String metricName;
    private String actualValue;
    private String expectedValue;
    private String message;
    private String suggestion;
    private String sourceRefs;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
