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
import com.huawei.fin.ai.material.analysis.vo.FactSourceVO;
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
public class PreAuditService {

    private final FactSourceService factSourceService;
    private final RulePackageDao rulePackageDao;
    private final RuleItemDao ruleItemDao;
    private final AnalysisResultDao analysisResultDao;
    private final IMaterialService materialService;
    private final IModelConfigService modelConfigService;
    private final AiGatewayClient aiGatewayClient;
    private final ILlmTraceService llmTraceService;

    public PreAuditService(FactSourceService factSourceService,
                           RulePackageDao rulePackageDao,
                           RuleItemDao ruleItemDao,
                           AnalysisResultDao analysisResultDao,
                           IMaterialService materialService,
                           IModelConfigService modelConfigService,
                           AiGatewayClient aiGatewayClient,
                           ILlmTraceService llmTraceService) {
        this.factSourceService = factSourceService;
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
        List<FactSourceVO> factSources = factSourceService.listByMaterial(materialId);
        RulePackageVO pkg = rulePackageDao.selectByThemeId(themeId).stream()
                .filter(p -> "PRE_AUDIT".equals(p.getPackageType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("主题未配置预审规则包: " + themeId));
        List<RuleItemVO> rules = ruleItemDao.selectByPackageId(pkg.getId());

        String prompt = "你是经营材料预审专家。基于事实源与预审核规则，判断材料是否能作为后续分析的可靠输入。\n"
                + "事实源：\n" + compactFactSources(factSources)
                + "\n预审核规则：\n" + rules.stream()
                .map(r -> r.getRuleCode() + " " + r.getName() + "：" + r.getDescription())
                .collect(Collectors.joining("\n"))
                + "\n只输出 JSON：{\"verdict\":\"PASS|NEED_CONFIRM|REJECT\",\"findings\":[{\"severity\":\"HIGH|MEDIUM|LOW\",\"message\":\"...\",\"sourceIds\":[\"...\"],\"ruleId\":\"...\",\"suggestion\":\"...\"}]}";

        String resultJson = callLlm(material, "PreAuditAgent", "pre-audit", prompt);
        AnalysisResultVO vo = new AnalysisResultVO();
        vo.setMaterialId(materialId);
        vo.setThemeId(themeId);
        vo.setPackageId(pkg.getId());
        vo.setResultType("PRE_AUDIT");
        vo.setVerdict(extractVerdict(resultJson));
        vo.setResultJson(resultJson);
        vo.setStatus("ACTIVE");
        vo.setVersion(1);
        analysisResultDao.deleteByMaterialAndType(materialId, "PRE_AUDIT");
        analysisResultDao.insert(vo);
        return vo;
    }

    public List<AnalysisResultVO> list(Long materialId) {
        return analysisResultDao.selectByMaterialAndType(materialId, "PRE_AUDIT");
    }

    private String callLlm(MaterialVO material, String agent, String skill, String prompt) {
        Optional<ModelConfigVO> model = modelConfigService.findEnabledTextModel();
        if (model.isEmpty()) {
            llmTraceService.recordSkipped(material.getTaskId(), material.getId(), agent, skill,
                    "未启用文本模型，返回待确认");
            return JsonUtil.toJson(Map.of(
                    "verdict", "NEED_CONFIRM",
                    "findings", List.of(Map.of("severity", "MEDIUM", "message", "未启用模型，无法完成 AI 预审"))));
        }
        LlmCallResult result = aiGatewayClient.chat(model.get(),
                "你是经营材料预审专家，只输出 JSON。", prompt);
        llmTraceService.record(material.getTaskId(), material.getId(), agent, skill,
                model.get().getModelName(), model.get().getProvider(), prompt, result);
        return result.success() ? result.content() : JsonUtil.toJson(Map.of(
                "verdict", "NEED_CONFIRM",
                "findings", List.of(Map.of("severity", "MEDIUM", "message", "预审调用失败: " + result.message()))));
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

    private String compactFactSources(List<FactSourceVO> factSources) {
        return factSources.stream()
                .map(f -> {
                    String content = f.getParseJson() == null ? "" : f.getParseJson();
                    if (content.length() > 300) {
                        content = content.substring(0, 300);
                    }
                    return "章节[" + f.getChapter() + "] slideRange=" + f.getSlideRange() + " content=" + content;
                })
                .collect(Collectors.joining("\n"));
    }
}
