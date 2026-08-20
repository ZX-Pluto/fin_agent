package com.huawei.fin.ai.material.analysis.vo;

import java.util.List;

import lombok.Data;

@Data
public class ThemeVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private ModelVO model;
    private List<RulePackageVO> rulePackages;
}
