package com.huawei.fin.ai.material.common.llmtrace;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LlmTraceController {

    private final ILlmTraceService llmTraceService;

    public LlmTraceController(ILlmTraceService llmTraceService) {
        this.llmTraceService = llmTraceService;
    }

    @GetMapping("/materials/{materialId}/traces")
    public List<LlmTraceVO> byMaterial(@PathVariable Long materialId) {
        return llmTraceService.listByMaterial(materialId);
    }

    @GetMapping("/tasks/{taskId}/traces")
    public List<LlmTraceVO> byTask(@PathVariable Long taskId) {
        return llmTraceService.listByTask(taskId);
    }
}
