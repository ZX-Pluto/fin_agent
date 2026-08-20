package com.huawei.fin.ai.material.knowledge.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class KnowledgeVO {

    private Long id;
    private Long materialId;
    private Long taskId;
    private String knowledgeType;
    private String content;
    private String sourceRefs;
    private BigDecimal confidence;
    private LocalDateTime createTime;
}
