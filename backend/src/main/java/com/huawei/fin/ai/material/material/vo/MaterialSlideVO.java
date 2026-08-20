package com.huawei.fin.ai.material.material.vo;

import lombok.Data;

@Data
public class MaterialSlideVO {

    private Long id;
    private Long materialId;
    private Integer slideNo;
    private String title;
    private String rawText;
    private String structuredContent;
    private String parseStatus;
}
