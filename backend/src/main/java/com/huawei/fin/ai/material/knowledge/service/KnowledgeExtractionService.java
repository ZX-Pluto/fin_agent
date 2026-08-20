package com.huawei.fin.ai.material.knowledge.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.common.client.AiGatewayClient;
import com.huawei.fin.ai.material.common.client.LlmCallResult;
import com.huawei.fin.ai.material.common.llmtrace.ILlmTraceService;
import com.huawei.fin.ai.material.common.modelconfig.IModelConfigService;
import com.huawei.fin.ai.material.common.modelconfig.ModelConfigVO;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.common.util.TextUtil;
import com.huawei.fin.ai.material.knowledge.tool.KnowledgeTool;
import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Service
public class KnowledgeExtractionService implements IKnowledgeExtractionService {

    private static final String SYSTEM_PROMPT = "你是经营材料分析助手。只输出 JSON，不要输出解释。"
            + "JSON 格式：{\"items\":[{\"type\":\"HIGHLIGHT|RISK|EVENT\",\"content\":\"经营事实\"}]}";
    private static final Set<String> TYPES = Set.of("HIGHLIGHT", "RISK", "EVENT");

    private final KnowledgeTool knowledgeTool;
    private final IModelConfigService modelConfigService;
    private final AiGatewayClient aiGatewayClient;
    private final ILlmTraceService llmTraceService;

    public KnowledgeExtractionService(KnowledgeTool knowledgeTool,
                                      IModelConfigService modelConfigService,
                                      AiGatewayClient aiGatewayClient,
                                      ILlmTraceService llmTraceService) {
        this.knowledgeTool = knowledgeTool;
        this.modelConfigService = modelConfigService;
        this.aiGatewayClient = aiGatewayClient;
        this.llmTraceService = llmTraceService;
    }

    @Override
    public int extract(MaterialVO material, List<MaterialSlideVO> slides) {
        Set<String> seen = new HashSet<>();
        Optional<ModelConfigVO> model = modelConfigService.findEnabledTextModel();
        if (model.isPresent()) {
            String prompt = buildPrompt(slides);
            LlmCallResult result = aiGatewayClient.chat(model.get(), SYSTEM_PROMPT, prompt);
            llmTraceService.record(material.getTaskId(), material.getId(), "KnowledgeExtractionAgent",
                    "knowledge-extraction", model.get().getModelName(), model.get().getProvider(), prompt, result);
            int llmCount = saveLlmItems(material, result.content(), seen);
            if (llmCount > 0) {
                return llmCount;
            }
            return extractDeterministic(material, slides, seen);
        }
        llmTraceService.recordSkipped(material.getTaskId(), material.getId(), "KnowledgeExtractionAgent",
                "knowledge-extraction", "未启用文本模型，使用确定性规则提取");
        return extractDeterministic(material, slides, seen);
    }

    private int saveLlmItems(MaterialVO material, String content, Set<String> seen) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        String json = content.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) {
                json = json.substring(start + 1, end);
            }
        }
        int count = 0;
        try {
            Map<String, Object> map = JsonUtil.fromJson(json, new TypeReference<Map<String, Object>>() {
            });
            if (map != null && map.get("items") instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> m) {
                        Object typeObj = m.get("type");
                        Object contentObj = m.get("content");
                        String type = typeObj == null ? "" : String.valueOf(typeObj).toUpperCase();
                        String text = contentObj == null ? "" : String.valueOf(contentObj).trim();
                        if (TYPES.contains(type) && text.length() >= 4) {
                            if (seen.add(type + "|" + text)) {
                                saveKnowledge(material, type, text,
                                        JsonUtil.toJson(List.of(Map.of("source", "LLM 提取"))));
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return count;
    }

    private int extractDeterministic(MaterialVO material, List<MaterialSlideVO> slides, Set<String> seen) {
        int count = 0;
        for (MaterialSlideVO slide : slides) {
            if (slide.getRawText() == null || slide.getRawText().isBlank()) {
                continue;
            }
            for (String line : slide.getRawText().split("\\r?\\n")) {
                String content = line.trim();
                if (content.isBlank() || content.length() < 6) {
                    continue;
                }
                String type = classify(content);
                if (type == null) {
                    continue;
                }
                if (seen.add(type + "|" + content)) {
                    saveKnowledge(material, type, content,
                            JsonUtil.toJson(List.of(Map.of("slideNo", slide.getSlideNo(), "quote", content))));
                    count++;
                }
            }
        }
        return count;
    }

    private void saveKnowledge(MaterialVO material, String type, String content, String sourceRefs) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setMaterialId(material.getId());
        vo.setTaskId(material.getTaskId());
        vo.setKnowledgeType(type);
        vo.setContent(content);
        vo.setSourceRefs(sourceRefs);
        vo.setConfidence(new BigDecimal("0.60"));
        knowledgeTool.saveKnowledge(vo);
    }

    private String buildPrompt(List<MaterialSlideVO> slides) {
        String text = slides.stream()
                .map(s -> {
                    String raw = s.getRawText() == null ? "" : s.getRawText();
                    if (raw.length() > 200) {
                        raw = raw.substring(0, 200);
                    }
                    return "第" + s.getSlideNo() + "页: " + raw;
                })
                .collect(Collectors.joining("\n"));
        if (text.length() > 4000) {
            text = text.substring(0, 4000);
        }
        return "请从以下材料页面文本中提取经营亮点、风险、事项：\n" + text;
    }

    private String classify(String line) {
        if (TextUtil.containsAny(line, "风险", "下降", "下滑", "不足", "紧张", "逾期", "延期", "亏损", "负增长", "压力")) {
            return "RISK";
        }
        if (TextUtil.containsAny(line, "事项", "整改", "推进", "计划", "交付", "项目", "措施")) {
            return "EVENT";
        }
        if (TextUtil.containsAny(line, "亮点", "增长", "突破", "第一", "领先", "新增", "完成", "提升", "改善")) {
            return "HIGHLIGHT";
        }
        return null;
    }
}
