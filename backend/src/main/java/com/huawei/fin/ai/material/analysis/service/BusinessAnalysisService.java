package com.huawei.fin.ai.material.analysis.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.analysis.dao.AnalysisResultDao;
import com.huawei.fin.ai.material.analysis.dao.RuleItemDao;
import com.huawei.fin.ai.material.analysis.dao.RulePackageDao;
import com.huawei.fin.ai.material.analysis.vo.AnalysisResultVO;
import com.huawei.fin.ai.material.analysis.vo.ModelDataVO;
import com.huawei.fin.ai.material.analysis.vo.RuleItemVO;
import com.huawei.fin.ai.material.analysis.vo.RulePackageVO;
import com.huawei.fin.ai.material.common.client.AiGatewayClient;
import com.huawei.fin.ai.material.common.client.LlmCallResult;
import com.huawei.fin.ai.material.common.llmtrace.ILlmTraceService;
import com.huawei.fin.ai.material.common.modelconfig.IModelConfigService;
import com.huawei.fin.ai.material.common.modelconfig.ModelConfigVO;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Service
public class BusinessAnalysisService {

    private final FactMappingService factMappingService;
    private final RulePackageDao rulePackageDao;
    private final RuleItemDao ruleItemDao;
    private final AnalysisResultDao analysisResultDao;
    private final IMaterialService materialService;
    private final IModelConfigService modelConfigService;
    private final AiGatewayClient aiGatewayClient;
    private final ILlmTraceService llmTraceService;

    public BusinessAnalysisService(FactMappingService factMappingService,
                                   RulePackageDao rulePackageDao,
                                   RuleItemDao ruleItemDao,
                                   AnalysisResultDao analysisResultDao,
                                   IMaterialService materialService,
                                   IModelConfigService modelConfigService,
                                   AiGatewayClient aiGatewayClient,
                                   ILlmTraceService llmTraceService) {
        this.factMappingService = factMappingService;
        this.rulePackageDao = rulePackageDao;
        this.ruleItemDao = ruleItemDao;
        this.analysisResultDao = analysisResultDao;
        this.materialService = materialService;
        this.modelConfigService = modelConfigService;
        this.aiGatewayClient = aiGatewayClient;
        this.llmTraceService = llmTraceService;
    }

    @Transactional
    public AnalysisResultVO run(Long materialId, Long themeId) {
        MaterialVO material = materialService.get(materialId);
        List<ModelDataVO> modelData = factMappingService.list(materialId);
        RulePackageVO pkg = rulePackageDao.selectByThemeId(themeId).stream()
                .filter(p -> "EXPERT".equals(p.getPackageType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("主题未配置专家规则包: " + themeId));
        List<RuleItemVO> rules = ruleItemDao.selectByPackageId(pkg.getId());

        String prompt = "你是经营分析专家。基于模型数据与专家经验规则进行经营质量分析，只输出 JSON："
                + "{\"verdict\":\"NORMAL|ABNORMAL|NEED_CONFIRM\",\"findings\":[{\"severity\":\"HIGH|MEDIUM|LOW\","
                + "\"subject\":\"...\",\"message\":\"...\",\"evidence\":[...],\"reason\":\"...\",\"suggestion\":\"...\"}],\"summary\":\"...\"}\n"
                + "模型数据：" + modelData.stream()
                .map(d -> d.getFieldCode() + "=" + d.getFieldValue() + d.getUnit())
                .collect(Collectors.joining("、"))
                + "\n专家经验规则：" + rules.stream()
                .map(r -> r.getRuleCode() + " " + r.getName() + "：" + r.getDescription())
                .collect(Collectors.joining("\n"));

        String json = callLlm(material, prompt);
        AnalysisResultVO vo = new AnalysisResultVO();
        vo.setMaterialId(materialId);
        vo.setThemeId(themeId);
        vo.setPackageId(pkg.getId());
        vo.setResultType("ANALYSIS");
        vo.setVerdict(extractVerdict(json));
        vo.setResultJson(json);
        vo.setStatus("ACTIVE");
        vo.setVersion(1);
        analysisResultDao.deleteByMaterialAndType(materialId, "ANALYSIS");
        analysisResultDao.insert(vo);
        return vo;
    }

    public List<AnalysisResultVO> list(Long materialId) {
        return analysisResultDao.selectByMaterialAndType(materialId, "ANALYSIS");
    }

    private String callLlm(MaterialVO material, String prompt) {
        Optional<ModelConfigVO> model = modelConfigService.findEnabledTextModel();
        if (model.isEmpty()) {
            llmTraceService.recordSkipped(material.getTaskId(), material.getId(), "BusinessAnalysisAgent",
                    "business-analysis", "未启用文本模型，返回待确认");
            return JsonUtil.toJson(Map.of(
                    "verdict", "NEED_CONFIRM",
                    "findings", List.of(),
                    "summary", "未启用模型，无法完成经营分析"));
        }
        LlmCallResult result = aiGatewayClient.chat(model.get(),
                "你是经营分析专家，只输出 JSON。", prompt);
        llmTraceService.record(material.getTaskId(), material.getId(), "BusinessAnalysisAgent",
                "business-analysis", model.get().getModelName(), model.get().getProvider(), prompt, result);
        return result.success() ? result.content() : JsonUtil.toJson(Map.of(
                "verdict", "NEED_CONFIRM",
                "findings", List.of(),
                "summary", "经营分析调用失败: " + result.message()));
    }

    private String extractVerdict(String json) {
        try {
            Map<String, Object> map = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
            });
            if (map != null && map.get("verdict") != null) {
                return String.valueOf(map.get("verdict"));
            }
        } catch (Exception ignored) {
        }
        return "NEED_CONFIRM";
    }
}
