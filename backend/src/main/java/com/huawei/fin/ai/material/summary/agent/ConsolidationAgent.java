package com.huawei.fin.ai.material.summary.agent;

import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.summary.service.ISummaryService;
import com.huawei.fin.ai.material.summary.vo.SummaryVO;

@Component
public class ConsolidationAgent {

    private final ISummaryService summaryService;

    public ConsolidationAgent(ISummaryService summaryService) {
        this.summaryService = summaryService;
    }

    public SummaryVO execute(Long materialId) {
        return summaryService.generateAndSave(materialId);
    }
}
