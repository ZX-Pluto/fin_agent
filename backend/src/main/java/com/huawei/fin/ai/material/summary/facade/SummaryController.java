package com.huawei.fin.ai.material.summary.facade;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huawei.fin.ai.material.summary.service.ISummaryService;
import com.huawei.fin.ai.material.summary.vo.SummaryVO;

@RestController
@RequestMapping("/api/materials/{materialId}/summary")
public class SummaryController {

    private final ISummaryService summaryService;

    public SummaryController(ISummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public SummaryVO summary(@PathVariable Long materialId) {
        return summaryService.build(materialId);
    }
}
