package com.huawei.fin.ai.material.task.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TaskVO {

    private Long id;
    private String taskName;
    private String taskType;
    private String status;
    private Integer progress;
    private String currentAgent;
    private String creatorId;
    private String errorMessage;
    private String paramsJson;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}
