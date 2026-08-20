package com.huawei.fin.ai.material.analysis.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AnalysisResultVO {
    private Long id;
    private Long materialId;
    private Long themeId;
    private Long packageId;
    private String resultType;
    private String verdict;
    private String resultJson;
    private String status;
    private Integer version;
    private String errorMessage;
    private LocalDateTime createTime;
}
