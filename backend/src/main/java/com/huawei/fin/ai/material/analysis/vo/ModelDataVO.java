package com.huawei.fin.ai.material.analysis.vo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ModelDataVO {
    private Long id;
    private Long materialId;
    private String organization;
    private String period;
    private Long modelId;
    private Integer modelVersion;
    private Long factSourceId;
    private String fieldCode;
    private BigDecimal fieldValue;
    private String unit;
    private String status;
}
