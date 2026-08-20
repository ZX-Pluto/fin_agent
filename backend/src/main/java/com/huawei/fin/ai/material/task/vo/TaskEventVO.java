package com.huawei.fin.ai.material.task.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskEventVO {

    private Long taskId;
    private String type;
    private String status;
    private Integer progress;
    private String currentAgent;
    private String message;
    private LocalDateTime timestamp;
}
