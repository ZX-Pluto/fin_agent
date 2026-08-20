package com.huawei.fin.ai.material.analysis.vo;

import java.util.List;

import lombok.Data;

@Data
public class ModelVO {
    private Long id;
    private String code;
    private String name;
    private Long themeId;
    private Integer version;
    private Boolean currentVersion;
    private List<ModelFieldVO> fields;
}
