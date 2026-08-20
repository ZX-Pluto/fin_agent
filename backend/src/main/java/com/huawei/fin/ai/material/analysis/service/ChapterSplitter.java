package com.huawei.fin.ai.material.analysis.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.common.client.AiGatewayClient;
import com.huawei.fin.ai.material.common.client.LlmCallResult;
import com.huawei.fin.ai.material.common.llmtrace.ILlmTraceService;
import com.huawei.fin.ai.material.common.modelconfig.IModelConfigService;
import com.huawei.fin.ai.material.common.modelconfig.ModelConfigVO;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.analysis.vo.ChapterVO;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;

@Component
public class ChapterSplitter {

    private static final List<String[]> CHAPTERS = List.of(
            new String[]{"经营概况", "概况", "总览"},
            new String[]{"收入", "营收"},
            new String[]{"利润", "盈利"},
            new String[]{"回款", "应收"},
            new String[]{"库存", "存货"},
            new String[]{"现金流", "现金"},
            new String[]{"费用"},
            new String[]{"风险"},
            new String[]{"下一步", "计划"});

    private final IModelConfigService modelConfigService;
    private final AiGatewayClient aiGatewayClient;
    private final ILlmTraceService llmTraceService;

    public ChapterSplitter(IModelConfigService modelConfigService,
                           AiGatewayClient aiGatewayClient,
                           ILlmTraceService llmTraceService) {
        this.modelConfigService = modelConfigService;
        this.aiGatewayClient = aiGatewayClient;
        this.llmTraceService = llmTraceService;
    }

    public List<ChapterVO> split(MaterialVO material, List<MaterialSlideVO> slides) {
        Map<Integer, String> aiChapters = splitByAi(material, slides);
        if (aiChapters != null && aiChapters.size() == slides.size()) {
            return groupByChapters(aiChapters, slides);
        }
        return splitByKeywords(slides);
    }

    private Map<Integer, String> splitByAi(MaterialVO material, List<MaterialSlideVO> slides) {
        Optional<ModelConfigVO> model = modelConfigService.findEnabledTextModel();
        if (model.isEmpty()) {
            llmTraceService.recordSkipped(material.getTaskId(), material.getId(),
                    "ChapterSplitter", "chapter-split", "未启用文本模型，按标题关键词/页码拆分");
            return null;
        }
        String prompt = buildAiPrompt(slides);
        LlmCallResult result = aiGatewayClient.chat(model.get(),
                "你是 PPT 章节归并专家，只输出 JSON。", prompt, 512);
        llmTraceService.record(material.getTaskId(), material.getId(),
                "ChapterSplitter", "chapter-split", model.get().getModelName(),
                model.get().getProvider(), prompt, result);
        if (!result.success()) {
            return null;
        }
        try {
            Map<String, Object> map = JsonUtil.fromJson(result.content(),
                    new TypeReference<Map<String, Object>>() {
                    });
            if (map == null || !(map.get("slides") instanceof List<?> items)) {
                return null;
            }
            Map<Integer, String> chapters = new LinkedHashMap<>();
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> row)) {
                    continue;
                }
                Object slideNoObj = row.get("slideNo");
                Object chapterObj = row.get("chapter");
                if (!(slideNoObj instanceof Number slideNo) || chapterObj == null) {
                    continue;
                }
                String chapter = String.valueOf(chapterObj).trim();
                if (!chapter.isBlank()) {
                    chapters.put(slideNo.intValue(), chapter);
                }
            }
            return chapters;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAiPrompt(List<MaterialSlideVO> slides) {
        StringBuilder sb = new StringBuilder("请根据以下 PPT 幻灯片标题归并章节，只输出 JSON：")
                .append("{\"slides\":[{\"slideNo\":1,\"chapter\":\"经营概况\"}]}\n");
        sb.append("归并原则：")
                .append("1. 标题能明确识别为经营概况、收入、利润、回款、库存、现金流、费用、风险、下一步时，必须保留该章节，不得合并到经营概况；")
                .append("2. 连续且属于同一业务章节的页面才归并；")
                .append("3. 无法识别章节的页面按\"第N页\"命名；")
                .append("4. 只输出 JSON，不要解释。\n");
        sb.append("可用章节：经营概况、收入、利润、回款、库存、现金流、费用、风险、下一步、其他。\n");
        sb.append("幻灯片：\n");
        for (MaterialSlideVO slide : slides) {
            sb.append(slide.getSlideNo()).append(". ")
                    .append(slide.getTitle() == null ? "" : slide.getTitle())
                    .append("\n");
        }
        return sb.toString();
    }

    private List<ChapterVO> splitByKeywords(List<MaterialSlideVO> slides) {
        List<ChapterVO> chapters = new ArrayList<>();
        for (MaterialSlideVO slide : slides) {
            String title = slide.getTitle() == null ? "" : slide.getTitle();
            String chapter = findChapter(title);
            if (chapter == null) {
                chapter = "第" + slide.getSlideNo() + "页";
            }
            appendChapter(chapters, chapter, slide.getSlideNo());
        }
        return chapters;
    }

    private List<ChapterVO> groupByChapters(Map<Integer, String> chapterMap, List<MaterialSlideVO> slides) {
        List<ChapterVO> chapters = new ArrayList<>();
        for (MaterialSlideVO slide : slides) {
            String chapter = chapterMap.get(slide.getSlideNo());
            if (chapter == null || chapter.isBlank()) {
                chapter = "第" + slide.getSlideNo() + "页";
            }
            appendChapter(chapters, chapter, slide.getSlideNo());
        }
        return chapters;
    }

    private void appendChapter(List<ChapterVO> chapters, String name, int slideNo) {
        if (!chapters.isEmpty()) {
            ChapterVO last = chapters.get(chapters.size() - 1);
            List<Integer> lastSlides = last.getSlideNos();
            if (last.getName().equals(name)
                    && !lastSlides.isEmpty()
                    && lastSlides.get(lastSlides.size() - 1) == slideNo - 1) {
                lastSlides.add(slideNo);
                return;
            }
        }
        ChapterVO vo = new ChapterVO(name);
        vo.getSlideNos().add(slideNo);
        chapters.add(vo);
    }

    private String findChapter(String title) {
        for (String[] keywords : CHAPTERS) {
            for (String keyword : keywords) {
                if (title.contains(keyword)) {
                    return keywords[0];
                }
            }
        }
        return null;
    }

}
