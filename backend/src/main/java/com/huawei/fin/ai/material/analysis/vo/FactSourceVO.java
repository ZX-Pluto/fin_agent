package com.huawei.fin.ai.material.analysis.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FactSourceVO {
    private Long id;
    private Long materialId;
    private String organization;
    private String period;
    private String chapter;
    private String slideRange;
    private String structuredFacts;
    private String parseJson;
    private String status;
    private Integer version;
    private String errorMessage;
    private LocalDateTime createTime;
}
