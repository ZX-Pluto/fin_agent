package com.huawei.fin.ai.material.analysis.vo;

import lombok.Data;

@Data
public class ModelFieldVO {
    private Long id;
    private Long modelId;
    private String fieldCode;
    private String fieldName;
    private String fieldType;
    private String unit;
    private String comment;
    private Integer seqNo;
}
