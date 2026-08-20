package com.huawei.fin.ai.material.analysis.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.analysis.dao.ModelDataDao;
import com.huawei.fin.ai.material.analysis.dao.ModelDao;
import com.huawei.fin.ai.material.analysis.dao.ModelFieldDao;
import com.huawei.fin.ai.material.analysis.vo.FactSourceVO;
import com.huawei.fin.ai.material.analysis.vo.ModelDataVO;
import com.huawei.fin.ai.material.analysis.vo.ModelFieldVO;
import com.huawei.fin.ai.material.analysis.vo.ModelVO;
import com.huawei.fin.ai.material.common.client.AiGatewayClient;
import com.huawei.fin.ai.material.common.client.LlmCallResult;
import com.huawei.fin.ai.material.common.llmtrace.ILlmTraceService;
import com.huawei.fin.ai.material.common.modelconfig.IModelConfigService;
import com.huawei.fin.ai.material.common.modelconfig.ModelConfigVO;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Service
public class FactMappingService {

    private final FactSourceService factSourceService;
    private final ModelDao modelDao;
    private final ModelFieldDao modelFieldDao;
    private final ModelDataDao modelDataDao;
    private final IMaterialService materialService;
    private final IModelConfigService modelConfigService;
    private final AiGatewayClient aiGatewayClient;
    private final ILlmTraceService llmTraceService;

    public FactMappingService(FactSourceService factSourceService,
                              ModelDao modelDao,
                              ModelFieldDao modelFieldDao,
                              ModelDataDao modelDataDao,
                              IMaterialService materialService,
                              IModelConfigService modelConfigService,
                              AiGatewayClient aiGatewayClient,
                              ILlmTraceService llmTraceService) {
        this.factSourceService = factSourceService;
        this.modelDao = modelDao;
        this.modelFieldDao = modelFieldDao;
        this.modelDataDao = modelDataDao;
        this.materialService = materialService;
        this.modelConfigService = modelConfigService;
        this.aiGatewayClient = aiGatewayClient;
        this.llmTraceService = llmTraceService;
    }

    @Transactional
    public List<ModelDataVO> map(Long materialId, Long themeId) {
        MaterialVO material = materialService.get(materialId);
        ModelVO model = modelDao.selectByThemeId(themeId).get(0);
        List<ModelFieldVO> fields = modelFieldDao.selectByModelId(model.getId());
        List<FactSourceVO> factSources = factSourceService.listByMaterial(materialId);

        String prompt = "你是经营事实映射专家。把事实源映射到模型字段，只输出 JSON："
                + "{\"mappings\":[{\"fieldCode\":\"revenue\",\"value\":756.7,\"unit\":\"亿元\"}]}\n"
                + "模型字段：" + fields.stream()
                .map(f -> f.getFieldCode() + "(" + f.getFieldName() + "," + f.getUnit() + ")")
                .collect(Collectors.joining("、"))
                + "\n事实源：" + factSources.stream()
                .map(f -> {
                    String content = f.getParseJson() == null ? "" : f.getParseJson();
                    if (content.length() > 300) {
                        content = content.substring(0, 300);
                    }
                    return "章节[" + f.getChapter() + "] " + content;
                })
                .collect(Collectors.joining("\n"));

        String json = callLlm(material, prompt);
        modelDataDao.deleteByMaterialId(materialId);
        saveMappings(material, model, json);
        return modelDataDao.selectByMaterialId(materialId);
    }

    public List<ModelDataVO> list(Long materialId) {
        return modelDataDao.selectByMaterialId(materialId);
    }

    private void saveMappings(MaterialVO material, ModelVO model, String json) {
        try {
            Map<String, Object> map = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
            });
            if (map == null || !(map.get("mappings") instanceof List<?> mappings)) {
                return;
            }
            for (Object item : mappings) {
                if (!(item instanceof Map<?, ?> m)) {
                    continue;
                }
                ModelDataVO vo = new ModelDataVO();
                vo.setMaterialId(material.getId());
                vo.setOrganization(material.getOrganization());
                vo.setPeriod(material.getReportPeriod());
                vo.setModelId(model.getId());
                vo.setModelVersion(model.getVersion());
                vo.setFieldCode(String.valueOf(m.get("fieldCode")));
                vo.setFieldValue(toDecimal(m.get("value")));
                vo.setUnit(m.get("unit") == null ? null : String.valueOf(m.get("unit")));
                vo.setStatus("ACTIVE");
                modelDataDao.insert(vo);
            }
        } catch (Exception e) {
            throw new IllegalStateException("模型映射解析失败: " + e.getMessage());
        }
    }

    private String callLlm(MaterialVO material, String prompt) {
        Optional<ModelConfigVO> model = modelConfigService.findEnabledTextModel();
        if (model.isEmpty()) {
            llmTraceService.recordSkipped(material.getTaskId(), material.getId(), "FactMappingAgent",
                    "fact-mapping", "未启用文本模型，模型数据为空");
            return JsonUtil.toJson(Map.of("mappings", List.of()));
        }
        LlmCallResult result = aiGatewayClient.chat(model.get(),
                "你是经营事实映射专家，只输出 JSON。", prompt);
        llmTraceService.record(material.getTaskId(), material.getId(), "FactMappingAgent",
                "fact-mapping", model.get().getModelName(), model.get().getProvider(), prompt, result);
        if (!result.success()) {
            throw new IllegalStateException("模型映射调用失败: " + result.message());
        }
        return result.content();
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
