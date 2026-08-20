package com.huawei.fin.ai.material.validation.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BusinessMetricVO {

    private Long id;
    private Long materialId;
    private Long taskId;
    private Long slideId;
    private String metricName;
    private String normalizedName;
    private BigDecimal value;
    private String unit;
    private String period;
    private String sourceRefs;
    private BigDecimal confidence;
    private LocalDateTime createTime;
}
