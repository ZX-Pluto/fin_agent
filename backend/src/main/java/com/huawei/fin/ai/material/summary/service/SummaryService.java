package com.huawei.fin.ai.material.summary.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.common.client.AiGatewayClient;
import com.huawei.fin.ai.material.common.client.LlmCallResult;
import com.huawei.fin.ai.material.common.llmtrace.ILlmTraceService;
import com.huawei.fin.ai.material.common.modelconfig.IModelConfigService;
import com.huawei.fin.ai.material.common.modelconfig.ModelConfigVO;
import com.huawei.fin.ai.material.knowledge.dao.KnowledgeDao;
import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.service.MaterialSlideDao;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.summary.vo.SummaryVO;
import com.huawei.fin.ai.material.validation.dao.BusinessMetricDao;
import com.huawei.fin.ai.material.validation.dao.ValidationResultDao;
import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;
import com.huawei.fin.ai.material.validation.vo.ValidationResultVO;

@Service
public class SummaryService implements ISummaryService {

    private final IMaterialService materialService;
    private final MaterialSlideDao materialSlideDao;
    private final BusinessMetricDao businessMetricDao;
    private final ValidationResultDao validationResultDao;
    private final KnowledgeDao knowledgeDao;
    private final IModelConfigService modelConfigService;
    private final AiGatewayClient aiGatewayClient;
    private final ILlmTraceService llmTraceService;

    public SummaryService(IMaterialService materialService,
                          MaterialSlideDao materialSlideDao,
                          BusinessMetricDao businessMetricDao,
                          ValidationResultDao validationResultDao,
                          KnowledgeDao knowledgeDao,
                          IModelConfigService modelConfigService,
                          AiGatewayClient aiGatewayClient,
                          ILlmTraceService llmTraceService) {
        this.materialService = materialService;
        this.materialSlideDao = materialSlideDao;
        this.businessMetricDao = businessMetricDao;
        this.validationResultDao = validationResultDao;
        this.knowledgeDao = knowledgeDao;
        this.modelConfigService = modelConfigService;
        this.aiGatewayClient = aiGatewayClient;
        this.llmTraceService = llmTraceService;
    }

    @Override
    public SummaryVO build(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        SummaryVO summary = compute(materialId, material);
        if (material.getBusinessScore() != null) {
            summary.setBusinessScore(material.getBusinessScore());
        }
        if (material.getSummaryText() != null && !material.getSummaryText().isBlank()) {
            summary.setSummaryText(material.getSummaryText());
        }
        return summary;
    }

    @Override
    public SummaryVO generateAndSave(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        SummaryVO summary = compute(materialId, material);
        fillSummaryWithLlm(summary, material, validationResultDao.selectList(materialId, null, null, null, null));
        materialService.updateSummary(materialId, summary.getSummaryText(), summary.getBusinessScore());
        return summary;
    }

    private SummaryVO compute(Long materialId, MaterialVO material) {
        List<MaterialSlideVO> slides = materialSlideDao.selectByMaterialId(materialId);
        List<BusinessMetricVO> metrics = businessMetricDao.selectByMaterialId(materialId);
        List<ValidationResultVO> findings = validationResultDao.selectList(materialId, null, null, null, null);
        List<KnowledgeVO> risks = knowledgeDao.selectByMaterial(materialId, "RISK");
        List<KnowledgeVO> highlights = knowledgeDao.selectByMaterial(materialId, "HIGHLIGHT");

        SummaryVO summary = new SummaryVO();
        summary.setMaterialId(materialId);
        summary.setOrganization(material.getOrganization());
        summary.setReportPeriod(material.getReportPeriod());
        summary.setMaterialName(material.getMaterialName());
        summary.setSlideCount(slides.size());
        summary.setMetricCount(metrics.size());
        summary.setFindingCount(findings.size());
        summary.setCriticalCount(count(findings, "CRITICAL"));
        summary.setHighCount(count(findings, "HIGH"));
        summary.setMediumCount(count(findings, "MEDIUM"));
        summary.setLowCount(count(findings, "LOW"));
        summary.setRiskCount(risks.size());
        summary.setHighlightCount(highlights.size());
        summary.setBusinessScore(calculateScore(findings));
        summary.setSummaryText(buildSummaryText(highlights, risks));
        return summary;
    }

    private void fillSummaryWithLlm(SummaryVO summary, MaterialVO material, List<ValidationResultVO> findings) {
        Optional<ModelConfigVO> model = modelConfigService.findEnabledTextModel();
        if (model.isEmpty()) {
            llmTraceService.recordSkipped(material.getTaskId(), material.getId(), "ConsolidationAgent",
                    "summary", "未启用文本模型，使用确定性摘要");
            return;
        }
        String issueText = findings.stream()
                .limit(6)
                .map(f -> "[" + f.getSeverity() + "] " + f.getMessage())
                .collect(Collectors.joining("\n"));
        String prompt = "请基于以下经营材料汇总数据生成不超过150字的经营摘要，突出亮点、风险与待办：\n"
                + "组织: " + summary.getOrganization() + "\n"
                + "期间: " + summary.getReportPeriod() + "\n"
                + "页面数: " + summary.getSlideCount() + "，指标数: " + summary.getMetricCount() + "\n"
                + "问题数: " + summary.getFindingCount() + "\n"
                + (issueText.isBlank() ? "未发现问题" : issueText);
        LlmCallResult result = aiGatewayClient.chat(model.get(),
                "你是经营分析助手，只输出摘要文本，不要输出其他内容。", prompt);
        llmTraceService.record(material.getTaskId(), material.getId(), "ConsolidationAgent",
                "summary", model.get().getModelName(), model.get().getProvider(), prompt, result);
        if (result.success() && result.content() != null && !result.content().isBlank()) {
            summary.setSummaryText(result.content().trim());
        }
    }

    private int count(List<ValidationResultVO> findings, String severity) {
        return (int) findings.stream().filter(f -> severity.equals(f.getSeverity())).count();
    }

    private BigDecimal calculateScore(List<ValidationResultVO> findings) {
        BigDecimal score = BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(count(findings, "CRITICAL")).multiply(BigDecimal.TEN))
                .subtract(BigDecimal.valueOf(count(findings, "HIGH")).multiply(BigDecimal.valueOf(5)))
                .subtract(BigDecimal.valueOf(count(findings, "MEDIUM")).multiply(BigDecimal.valueOf(2)))
                .subtract(BigDecimal.valueOf(count(findings, "LOW")).multiply(BigDecimal.valueOf(0.5)));
        return score.max(BigDecimal.ZERO);
    }

    private String buildSummaryText(List<KnowledgeVO> highlights, List<KnowledgeVO> risks) {
        StringBuilder sb = new StringBuilder();
        if (!highlights.isEmpty()) {
            sb.append("亮点: ").append(highlights.get(0).getContent());
        }
        if (!risks.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append("风险: ").append(risks.get(0).getContent());
        }
        return sb.toString();
    }
}
