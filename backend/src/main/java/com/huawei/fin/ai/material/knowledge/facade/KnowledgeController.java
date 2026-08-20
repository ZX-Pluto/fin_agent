package com.huawei.fin.ai.material.knowledge.facade;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huawei.fin.ai.material.knowledge.service.IKnowledgeService;
import com.huawei.fin.ai.material.knowledge.vo.HighlightVO;
import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;
import com.huawei.fin.ai.material.knowledge.vo.RiskVO;
import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;

@RestController
@RequestMapping("/api/materials/{materialId}")
public class KnowledgeController {

    private final IKnowledgeService knowledgeService;

    public KnowledgeController(IKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/knowledge")
    public List<KnowledgeVO> knowledge(@PathVariable Long materialId,
                                       @RequestParam(value = "type", required = false) String type) {
        return knowledgeService.list(materialId, type);
    }

    @GetMapping("/metrics")
    public List<BusinessMetricVO> metrics(@PathVariable Long materialId) {
        return knowledgeService.listMetrics(materialId);
    }

    @GetMapping("/highlights")
    public List<HighlightVO> highlights(@PathVariable Long materialId) {
        return knowledgeService.listHighlights(materialId);
    }

    @GetMapping("/risks")
    public List<RiskVO> risks(@PathVariable Long materialId) {
        return knowledgeService.listRisks(materialId);
    }
}
