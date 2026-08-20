package com.huawei.fin.ai.material.task.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TaskProgressVO {

    private Long taskId;
    private String status;
    private Integer progress = 0;
    private String currentAgent;
    private String message;
    private List<String> eventLog = new ArrayList<>();

    public void appendLog(String line) {
        if (eventLog.size() > 200) {
            eventLog.remove(0);
        }
        eventLog.add(line);
        message = line;
    }
}
