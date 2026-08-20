package com.huawei.fin.ai.material.task.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.agent.MaterialAgentOrchestrator;

@Service
public class TaskExecuteService implements ITaskExecuteService {

    private final MaterialAgentOrchestrator orchestrator;

    public TaskExecuteService(MaterialAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    @Async("taskExecutor")
    public void execute(Long taskId) {
        orchestrator.execute(taskId);
    }
}
