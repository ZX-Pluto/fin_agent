package com.huawei.fin.ai.material.knowledge.vo;

import lombok.Data;

@Data
public class HighlightVO {

    private Long id;
    private Long materialId;
    private String content;
    private String sourceRefs;
}
