package com.huawei.fin.ai.material.validation.agent;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.service.MaterialSlideDao;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialStatus;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.common.client.AiGatewayClient;
import com.huawei.fin.ai.material.common.client.LlmCallResult;
import com.huawei.fin.ai.material.common.llmtrace.ILlmTraceService;
import com.huawei.fin.ai.material.common.modelconfig.IModelConfigService;
import com.huawei.fin.ai.material.common.modelconfig.ModelConfigVO;
import com.huawei.fin.ai.material.validation.dao.BusinessMetricDao;
import com.huawei.fin.ai.material.validation.dao.ValidationResultDao;
import com.huawei.fin.ai.material.validation.service.IValidationRuleService;
import com.huawei.fin.ai.material.validation.tool.ValidationTool;
import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;
import com.huawei.fin.ai.material.validation.vo.ValidationResultVO;
import com.huawei.fin.ai.material.validation.vo.ValidationRuleVO;

@Component
public class DataValidationAgent {

    private final IMaterialService materialService;
    private final MaterialSlideDao materialSlideDao;
    private final BusinessMetricDao businessMetricDao;
    private final IValidationRuleService ruleService;
    private final ValidationTool validationTool;
    private final ValidationResultDao validationResultDao;
    private final IModelConfigService modelConfigService;
    private final AiGatewayClient aiGatewayClient;
    private final ILlmTraceService llmTraceService;

    public DataValidationAgent(IMaterialService materialService,
                               MaterialSlideDao materialSlideDao,
                               BusinessMetricDao businessMetricDao,
                               IValidationRuleService ruleService,
                               ValidationTool validationTool,
                               ValidationResultDao validationResultDao,
                               IModelConfigService modelConfigService,
                               AiGatewayClient aiGatewayClient,
                               ILlmTraceService llmTraceService) {
        this.materialService = materialService;
        this.materialSlideDao = materialSlideDao;
        this.businessMetricDao = businessMetricDao;
        this.ruleService = ruleService;
        this.validationTool = validationTool;
        this.validationResultDao = validationResultDao;
        this.modelConfigService = modelConfigService;
        this.aiGatewayClient = aiGatewayClient;
        this.llmTraceService = llmTraceService;
    }

    public void execute(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        materialService.updateStatus(materialId, MaterialStatus.VALIDATING.name());
        List<MaterialSlideVO> slides = materialSlideDao.selectByMaterialId(materialId);
        List<BusinessMetricVO> metrics = businessMetricDao.selectByMaterialId(materialId);
        List<ValidationRuleVO> rules = ruleService.listEnabled();
        List<ValidationResultVO> results = validationTool.run(material, slides, metrics, rules);
        List<ValidationResultVO> deduped = dedupe(results);
        for (ValidationResultVO result : deduped) {
            result.setMaterialId(materialId);
            result.setTaskId(material.getTaskId());
            validationResultDao.insert(result);
        }
        recordValidationExplanation(material, deduped);
        materialService.updateStatus(materialId, MaterialStatus.VALIDATED.name());
    }

    private List<ValidationResultVO> dedupe(List<ValidationResultVO> results) {
        Set<String> seen = new HashSet<>();
        List<ValidationResultVO> deduped = new ArrayList<>();
        for (ValidationResultVO result : results) {
            String key = result.getRuleCode() + "|"
                    + (result.getMetricName() == null ? "" : result.getMetricName()) + "|"
                    + result.getMessage();
            if (seen.add(key)) {
                deduped.add(result);
            }
        }
        return deduped;
    }

    private void recordValidationExplanation(MaterialVO material, List<ValidationResultVO> results) {
        Optional<ModelConfigVO> model = modelConfigService.findEnabledTextModel();
        if (model.isEmpty()) {
            llmTraceService.recordSkipped(material.getTaskId(), material.getId(), "DataValidationAgent",
                    "finding-explanation", "未启用文本模型，规则引擎结果由人工复核");
            return;
        }
        String issueText = results.stream()
                .limit(10)
                .map(r -> "[" + r.getSeverity() + "] " + r.getMessage())
                .collect(Collectors.joining("\n"));
        if (issueText.isBlank()) {
            issueText = "未发现问题";
        }
        String prompt = "校验问题：\n" + issueText;
        LlmCallResult result = aiGatewayClient.chat(model.get(),
                "你是经营数据预审助手，请基于以下校验问题给出简要复核与整改建议，200字以内。", prompt);
        llmTraceService.record(material.getTaskId(), material.getId(), "DataValidationAgent",
                "finding-explanation", model.get().getModelName(), model.get().getProvider(), prompt, result);
    }
}
