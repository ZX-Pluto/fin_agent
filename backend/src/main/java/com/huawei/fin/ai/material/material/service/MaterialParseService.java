package com.huawei.fin.ai.material.material.service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.common.client.FileServiceClient;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.common.util.TextUtil;
import com.huawei.fin.ai.material.material.tool.PptParseTool;
import com.huawei.fin.ai.material.material.vo.MaterialParseResultVO;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.validation.dao.BusinessMetricDao;
import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;

@Service
public class MaterialParseService implements IMaterialParseService {

    private static final List<MetricPattern> METRIC_PATTERNS = List.of(
            new MetricPattern(List.of("营业收入", "营收", "销售收入", "销售额"), "收入"),
            new MetricPattern(List.of("净利润", "毛利润", "利润总额", "利润"), "利润"),
            new MetricPattern(List.of("经营活动现金流", "经营现金流", "现金流"), "现金流"),
            new MetricPattern(List.of("库存"), "库存"),
            new MetricPattern(List.of("销售费用", "管理费用", "费用"), "费用"),
            new MetricPattern(List.of("毛利率"), "毛利率"),
            new MetricPattern(List.of("达成率", "预算完成率"), "达成率"));

    private final PptParseTool pptParseTool;
    private final MaterialSlideDao materialSlideDao;
    private final BusinessMetricDao businessMetricDao;
    private final FileServiceClient fileServiceClient;

    public MaterialParseService(PptParseTool pptParseTool,
                                MaterialSlideDao materialSlideDao,
                                BusinessMetricDao businessMetricDao,
                                FileServiceClient fileServiceClient) {
        this.pptParseTool = pptParseTool;
        this.materialSlideDao = materialSlideDao;
        this.businessMetricDao = businessMetricDao;
        this.fileServiceClient = fileServiceClient;
    }

    @Override
    public MaterialParseResultVO parse(MaterialVO material) {
        Path file = fileServiceClient.resolve(material.getFilePath());
        MaterialParseResultVO result = pptParseTool.parse(file);
        for (MaterialSlideVO slide : result.getSlides()) {
            slide.setMaterialId(material.getId());
            materialSlideDao.insert(slide);
        }
        return result;
    }

    @Override
    public String buildBusinessIr(MaterialVO material, MaterialParseResultVO result) {
        List<Map<String, Object>> slides = new ArrayList<>();
        for (MaterialSlideVO slide : result.getSlides()) {
            Map<String, Object> slideMap = new LinkedHashMap<>();
            slideMap.put("slideNo", slide.getSlideNo());
            slideMap.put("title", slide.getTitle());
            slideMap.put("rawText", slide.getRawText());
            slides.add(slideMap);
        }

        List<Map<String, Object>> metrics = extractMetrics(material, result.getSlides());
        Map<String, Object> ir = new LinkedHashMap<>();
        ir.put("irVersion", "1.0");
        ir.put("organization", material.getOrganization());
        ir.put("reportPeriod", material.getReportPeriod());
        ir.put("slides", slides);
        ir.put("metrics", metrics);
        ir.put("findings", List.of());
        ir.put("summary", Map.of());
        return JsonUtil.toJson(ir);
    }

    private List<Map<String, Object>> extractMetrics(MaterialVO material, List<MaterialSlideVO> slides) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        for (MaterialSlideVO slide : slides) {
            if (slide.getRawText() == null || slide.getRawText().isBlank()) {
                continue;
            }
            for (String line : slide.getRawText().split("\\r?\\n")) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.contains("同比") || line.contains("环比")) {
                    continue;
                }
                for (MetricPattern pattern : METRIC_PATTERNS) {
                    if (TextUtil.containsAny(line, pattern.aliases().toArray(String[]::new))) {
                        BigDecimal value = TextUtil.extractNumber(line);
                        if (value == null) {
                            continue;
                        }
                        String unit = extractUnit(line);
                        if (unit.isBlank() && !line.contains("%")) {
                            unit = "";
                        }
                        Map<String, Object> metric = new LinkedHashMap<>();
                        metric.put("metricName", line.split("[:：\\s]")[0]);
                        metric.put("normalizedName", pattern.normalized());
                        metric.put("value", value);
                        metric.put("unit", unit);
                        metric.put("period", material.getReportPeriod());
                        metric.put("slideNo", slide.getSlideNo());
                        metric.put("quote", line.trim());
                        metric.put("confidence", "0.70");
                        metrics.add(metric);

                        BusinessMetricVO metricVo = new BusinessMetricVO();
                        metricVo.setMaterialId(material.getId());
                        metricVo.setTaskId(material.getTaskId());
                        metricVo.setSlideId(slide.getId());
                        metricVo.setMetricName(metric.get("metricName").toString());
                        metricVo.setNormalizedName(pattern.normalized());
                        metricVo.setValue(value);
                        metricVo.setUnit(unit);
                        metricVo.setPeriod(material.getReportPeriod());
                        metricVo.setSourceRefs(JsonUtil.toJson(List.of(Map.of(
                                "slideNo", slide.getSlideNo(),
                                "quote", line.trim()))));
                        metricVo.setConfidence(new BigDecimal("0.70"));
                        businessMetricDao.insert(metricVo);
                        break;
                    }
                }
            }
        }
        return metrics;
    }

    private String extractUnit(String line) {
        if (line.contains("亿元")) {
            return "亿元";
        }
        if (line.contains("万元")) {
            return "万元";
        }
        if (line.contains("亿")) {
            return "亿";
        }
        if (line.contains("万")) {
            return "万";
        }
        if (line.contains("%")) {
            return "%";
        }
        return "";
    }

    private record MetricPattern(List<String> aliases, String normalized) {
    }
}
