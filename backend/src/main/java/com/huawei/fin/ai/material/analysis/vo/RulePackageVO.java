package com.huawei.fin.ai.material.analysis.vo;

import java.util.List;

import lombok.Data;

@Data
public class RulePackageVO {
    private Long id;
    private String code;
    private String name;
    private Long themeId;
    private String packageType;
    private String description;
    private Boolean enabled;
    private List<RuleItemVO> items;
}
