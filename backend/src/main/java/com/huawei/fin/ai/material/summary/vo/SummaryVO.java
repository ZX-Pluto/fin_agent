package com.huawei.fin.ai.material.summary.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class SummaryVO {

    private Long materialId;
    private String organization;
    private String reportPeriod;
    private String materialName;
    private Integer slideCount;
    private Integer metricCount;
    private Integer findingCount;
    private Integer criticalCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
    private Integer riskCount;
    private Integer highlightCount;
    private BigDecimal businessScore;
    private String summaryText;
}
