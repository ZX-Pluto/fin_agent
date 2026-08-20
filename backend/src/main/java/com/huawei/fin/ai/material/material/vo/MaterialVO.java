package com.huawei.fin.ai.material.material.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MaterialVO {

    private Long id;
    private Long taskId;
    private Long themeId;
    private String themeName;
    private String materialName;
    private String materialType;
    private String sourceType;
    private String sourceUrl;
    private String filePath;
    private String region;
    private String organization;
    private String reportPeriod;
    private String status;
    private BigDecimal confidence;
    private String irJson;
    private String summaryText;
    private BigDecimal businessScore;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
