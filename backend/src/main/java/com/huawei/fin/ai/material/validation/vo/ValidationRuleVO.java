package com.huawei.fin.ai.material.validation.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ValidationRuleVO {

    private Long id;
    private String ruleCode;
    private String name;
    private String category;
    private String description;
    private String severity;
    private String params;
    private Boolean builtin;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
