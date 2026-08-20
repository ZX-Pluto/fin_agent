package com.huawei.fin.ai.material.analysis.vo;

import lombok.Data;

@Data
public class RuleItemVO {
    private Long id;
    private Long packageId;
    private String ruleCode;
    private String name;
    private String ruleType;
    private String scope;
    private String inputFields;
    private String executionStrategy;
    private String description;
    private String severity;
    private Boolean enabled;
}
