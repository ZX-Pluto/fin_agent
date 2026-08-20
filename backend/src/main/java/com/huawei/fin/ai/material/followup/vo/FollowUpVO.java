package com.huawei.fin.ai.material.followup.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FollowUpVO {
    private Long id;
    private Long materialId;
    private String title;
    private String message;
    private String suggestion;
    private String sourceLabel;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
