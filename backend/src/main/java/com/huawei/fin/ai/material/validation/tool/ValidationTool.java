package com.huawei.fin.ai.material.validation.tool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.common.util.TextUtil;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;
import com.huawei.fin.ai.material.validation.vo.ValidationResultVO;
import com.huawei.fin.ai.material.validation.vo.ValidationRuleVO;

@Component
public class ValidationTool {

    private static final Pattern PERIOD_PATTERN =
            Pattern.compile("20\\d{2}\\s*[年/\\-]?\\s*[Qq]?[1-4]|\\d{4}\\s*年.*季度");
    private static final Pattern GROWTH_PATTERN = Pattern.compile("(同比|环比)\\s*[+\\-]?\\s*(\\d+(?:\\.\\d+)?)%");
    private static final Pattern LAST_YEAR_PATTERN = Pattern.compile("上年同期[^0-9\\-]{0,10}(-?\\d+(?:\\.\\d+)?)");

    public List<ValidationResultVO> run(MaterialVO material,
                                        List<MaterialSlideVO> slides,
                                        List<BusinessMetricVO> metrics,
                                        List<ValidationRuleVO> rules) {
        List<ValidationResultVO> results = new ArrayList<>();
        for (ValidationRuleVO rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }
            Map<String, Object> params = JsonUtil.fromJson(rule.getParams(), new TypeReference<Map<String, Object>>() {
            });
            if (params == null) {
                params = Map.of();
            }
            switch (rule.getRuleCode()) {
                case "R-C01" -> checkRequiredSections(material, slides, rule, params, results);
                case "R-C02" -> checkRequiredMetrics(material, metrics, rule, params, results);
                case "R-C03" -> checkPeriod(material, slides, rule, results);
                case "R-C04" -> checkPageCount(material, slides, rule, params, results);
                case "R-C05" -> checkAttachment(material, slides, rule, results);
                case "R-T01" -> checkGrowthRecalc(material, slides, metrics, rule, params, results, "同比");
                case "R-T02" -> checkGrowthRecalc(material, slides, metrics, rule, params, results, "环比");
                case "R-T03" -> checkGrowthConsistency(material, slides, metrics, rule, results);
                case "R-T06" -> checkUnitConsistency(material, metrics, rule, results);
                case "R-R01" -> checkAbnormalFluctuation(material, slides, metrics, rule, params, results);
                case "R-R03" -> checkContradiction(material, slides, metrics, rule, results);
                case "R-R04" -> checkRiskSignal(material, metrics, rule, results);
                case "R-R05" -> checkAchievement(material, metrics, rule, params, results);
                case "R-S02" -> checkCrossSlideConsistency(material, metrics, rule, results);
                default -> {
                }
            }
        }
        return results;
    }

    private void checkRequiredSections(MaterialVO material, List<MaterialSlideVO> slides,
                                       ValidationRuleVO rule, Map<String, Object> params,
                                       List<ValidationResultVO> results) {
        List<String> required = toStringList(params.get("sections"));
        if (required.isEmpty()) {
            return;
        }
        String allText = allText(slides);
        List<String> missing = required.stream().filter(section -> !allText.contains(section)).toList();
        if (!missing.isEmpty()) {
            results.add(build(material, rule, null,
                    String.join("、", missing),
                    String.join("、", required),
                    "缺失必选章节: " + String.join("、", missing),
                    "请按模板补充对应章节内容", List.of()));
        }
    }

    private void checkRequiredMetrics(MaterialVO material, List<BusinessMetricVO> metrics,
                                      ValidationRuleVO rule, Map<String, Object> params,
                                      List<ValidationResultVO> results) {
        List<String> required = toStringList(params.get("metrics"));
        if (required.isEmpty()) {
            return;
        }
        List<String> found = metrics.stream().map(BusinessMetricVO::getNormalizedName).toList();
        List<String> missing = required.stream().filter(name -> !found.contains(name)).toList();
        if (!missing.isEmpty()) {
            results.add(build(material, rule, null,
                    "已提取: " + String.join("、", found),
                    "必填: " + String.join("、", required),
                    "缺失必填指标: " + String.join("、", missing),
                    "请补充缺失指标数据", List.of()));
        }
    }

    private void checkPeriod(MaterialVO material, List<MaterialSlideVO> slides,
                             ValidationRuleVO rule, List<ValidationResultVO> results) {
        String allText = allText(slides);
        boolean present = material.getReportPeriod() != null && !material.getReportPeriod().isBlank()
                || PERIOD_PATTERN.matcher(allText).find();
        if (!present) {
            results.add(build(material, rule, null, "未识别到期间", "如 2026Q2",
                    "材料未标注经营期间", "请在材料中标注本期，如 2026Q2", List.of()));
        }
    }

    private void checkPageCount(MaterialVO material, List<MaterialSlideVO> slides,
                                ValidationRuleVO rule, Map<String, Object> params,
                                List<ValidationResultVO> results) {
        int minPages = params.get("minPages") instanceof Number n ? n.intValue() : 15;
        int count = slides.size();
        if (count < minPages) {
            results.add(build(material, rule, null, String.valueOf(count), String.valueOf(minPages),
                    "材料页数偏少: " + count + " 页，低于阈值 " + minPages + " 页",
                    "请确认材料是否完整", List.of()));
        }
    }

    private void checkAttachment(MaterialVO material, List<MaterialSlideVO> slides,
                                 ValidationRuleVO rule, List<ValidationResultVO> results) {
        String allText = allText(slides);
        if (allText.contains("附件") || allText.contains("详见附件")) {
            results.add(build(material, rule, null, "正文提到附件", "已提供附件",
                    "正文提到附件但未检测到附件文件", "请核对附件是否已随材料提交", List.of()));
        }
    }

    private void checkGrowthRecalc(MaterialVO material, List<MaterialSlideVO> slides,
                                   List<BusinessMetricVO> metrics, ValidationRuleVO rule,
                                   Map<String, Object> params, List<ValidationResultVO> results,
                                   String keyword) {
        BigDecimal maxDeviation = toBigDecimal(params.get("maxDeviationPct"), "0.5");
        for (BusinessMetricVO metric : metrics) {
            String slideText = slideText(slides, metric.getSlideId(), metric.getSourceRefs());
            Matcher claimMatcher = GROWTH_PATTERN.matcher(slideText == null ? "" : slideText);
            BigDecimal claim = null;
            while (claimMatcher.find()) {
                if (keyword.equals(claimMatcher.group(1))) {
                    claim = new BigDecimal(claimMatcher.group(2));
                    break;
                }
            }
            Matcher lastMatcher = LAST_YEAR_PATTERN.matcher(slideText == null ? "" : slideText);
            if (claim == null || !lastMatcher.find()) {
                continue;
            }
            BigDecimal lastValue = new BigDecimal(lastMatcher.group(1));
            BigDecimal expected = new BusinessMetricTool().calculateGrowth(metric.getValue(), lastValue);
            if (expected == null) {
                continue;
            }
            BigDecimal deviation = expected.subtract(claim).abs();
            if (deviation.compareTo(maxDeviation) > 0) {
                results.add(build(material, rule, metric.getNormalizedName(),
                        "实算 " + expected + "%, 声称 " + claim + "%",
                        "偏差不超过 " + maxDeviation + "pct",
                        metric.getNormalizedName() + " " + keyword + "复算不一致: 实算 " + expected + "%, 声称 " + claim + "%",
                        "请核对口径与原始数据", refs(metric)));
            }
        }
    }

    private void checkGrowthConsistency(MaterialVO material, List<MaterialSlideVO> slides,
                                        List<BusinessMetricVO> metrics, ValidationRuleVO rule,
                                        List<ValidationResultVO> results) {
        Map<String, List<String>> claimsByMetric = new HashMap<>();
        for (BusinessMetricVO metric : metrics) {
            String slideText = slideText(slides, metric.getSlideId(), metric.getSourceRefs());
            Matcher matcher = GROWTH_PATTERN.matcher(slideText == null ? "" : slideText);
            while (matcher.find()) {
                claimsByMetric.computeIfAbsent(metric.getNormalizedName(), k -> new ArrayList<>())
                        .add(matcher.group(1) + matcher.group(2) + "%");
            }
        }
        claimsByMetric.forEach((name, claims) -> {
            List<String> distinct = claims.stream().distinct().toList();
            if (distinct.size() > 1) {
                results.add(build(material, rule, name, String.join("、", claims),
                        "各页声称一致",
                        name + " 在不同页面声称的增长率不一致: " + String.join("、", distinct),
                        "请统一口径并核对增速", List.of()));
            }
        });
    }

    private void checkUnitConsistency(MaterialVO material, List<BusinessMetricVO> metrics,
                                      ValidationRuleVO rule, List<ValidationResultVO> results) {
        Map<String, List<String>> units = new HashMap<>();
        for (BusinessMetricVO metric : metrics) {
            units.computeIfAbsent(metric.getMetricName(), k -> new ArrayList<>())
                    .add(metric.getUnit() == null || metric.getUnit().isBlank() ? "无单位" : metric.getUnit());
        }
        units.forEach((name, unitList) -> {
            List<String> distinct = unitList.stream().distinct().toList();
            if (distinct.size() > 1) {
                results.add(build(material, rule, name, String.join("、", distinct),
                        "统一单位",
                        name + " 存在单位混用: " + String.join("、", distinct),
                        "请统一单位为亿元或万元", List.of()));
            }
        });
    }

    private void checkAbnormalFluctuation(MaterialVO material, List<MaterialSlideVO> slides,
                                          List<BusinessMetricVO> metrics, ValidationRuleVO rule,
                                          Map<String, Object> params, List<ValidationResultVO> results) {
        BigDecimal qoqThreshold = toBigDecimal(params.get("qoqThreshold"), "30");
        BigDecimal yoyThreshold = toBigDecimal(params.get("yoyThreshold"), "50");
        for (BusinessMetricVO metric : metrics) {
            String slideText = slideText(slides, metric.getSlideId(), metric.getSourceRefs());
            Matcher matcher = GROWTH_PATTERN.matcher(slideText == null ? "" : slideText);
            while (matcher.find()) {
                BigDecimal rate = new BigDecimal(matcher.group(2));
                BigDecimal threshold = "同比".equals(matcher.group(1)) ? yoyThreshold : qoqThreshold;
                if (rate.abs().compareTo(threshold) <= 0) {
                    continue;
                }
                boolean hasExplanation = TextUtil.containsAny(slideText, "解释", "原因", "因为", "由于", "受", "影响", "主要");
                if (!hasExplanation) {
                    results.add(build(material, rule, metric.getNormalizedName(),
                            metric.getNormalizedName() + " " + matcher.group(1) + rate + "%",
                            "波动不超过 " + threshold + "% 或有解释",
                            metric.getNormalizedName() + " " + matcher.group(1) + rate + "% 波动异常且未提供解释",
                            "请补充波动原因说明", refs(metric)));
                }
            }
        }
    }

    private void checkContradiction(MaterialVO material, List<MaterialSlideVO> slides,
                                    List<BusinessMetricVO> metrics, ValidationRuleVO rule,
                                    List<ValidationResultVO> results) {
        for (BusinessMetricVO metric : metrics) {
            if (metric.getValue() == null || metric.getValue().compareTo(BigDecimal.ZERO) >= 0) {
                continue;
            }
            String slideText = slideText(slides, metric.getSlideId(), metric.getSourceRefs());
            if (TextUtil.containsAny(slideText, "增长", "提升", "上涨", "向好", "改善")) {
                results.add(build(material, rule, metric.getNormalizedName(),
                        "数值为负: " + metric.getValue(),
                        "文字方向与数据一致",
                        metric.getNormalizedName() + " 为负，但页面文字表述为增长/向好",
                        "请核对结论与数据方向", refs(metric)));
            }
        }
    }

    private void checkRiskSignal(MaterialVO material, List<BusinessMetricVO> metrics,
                                 ValidationRuleVO rule, List<ValidationResultVO> results) {
        for (BusinessMetricVO metric : metrics) {
            if ("现金流".equals(metric.getNormalizedName())
                    && metric.getValue() != null
                    && metric.getValue().compareTo(BigDecimal.ZERO) < 0) {
                results.add(build(material, rule, "现金流",
                        "现金流为负: " + metric.getValue() + metric.getUnit(),
                        "现金流非负",
                        "现金流为负，存在经营风险信号",
                        "请说明现金流为负的原因及应对措施", refs(metric)));
            }
        }
    }

    private void checkAchievement(MaterialVO material, List<BusinessMetricVO> metrics,
                                  ValidationRuleVO rule, Map<String, Object> params,
                                  List<ValidationResultVO> results) {
        BigDecimal min = toBigDecimal(params.get("minRate"), "60");
        BigDecimal max = toBigDecimal(params.get("maxRate"), "150");
        for (BusinessMetricVO metric : metrics) {
            if (!"达成率".equals(metric.getNormalizedName()) || metric.getValue() == null) {
                continue;
            }
            if (metric.getValue().compareTo(min) < 0 || metric.getValue().compareTo(max) > 0) {
                results.add(build(material, rule, "达成率",
                        metric.getValue() + "%",
                        min + "%~" + max + "%",
                        "目标达成率 " + metric.getValue() + "% 超出合理区间",
                        "请关注目标达成情况", refs(metric)));
            }
        }
    }

    private void checkCrossSlideConsistency(MaterialVO material, List<BusinessMetricVO> metrics,
                                            ValidationRuleVO rule, List<ValidationResultVO> results) {
        Map<String, List<BusinessMetricVO>> grouped = new LinkedHashMap<>();
        for (BusinessMetricVO metric : metrics) {
            grouped.computeIfAbsent(metric.getMetricName(), k -> new ArrayList<>()).add(metric);
        }
        grouped.forEach((name, list) -> {
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    BusinessMetricVO a = list.get(i);
                    BusinessMetricVO b = list.get(j);
                    if (a.getValue() == null || b.getValue() == null || a.getValue().compareTo(b.getValue()) == 0) {
                        continue;
                    }
                    List<Map<String, Object>> refs = new ArrayList<>();
                    refs.addAll(refs(a));
                    refs.addAll(refs(b));
                    results.add(build(material, rule, name,
                            a.getValue() + a.getUnit() + " vs " + b.getValue() + b.getUnit(),
                            "同一指标数值一致",
                            name + " 在不同页面数值不一致",
                            "请核对并统一数据", refs));
                    return;
                }
            }
        });
    }

    private ValidationResultVO build(MaterialVO material, ValidationRuleVO rule, String metricName,
                                     String actual, String expected, String message, String suggestion,
                                     List<Map<String, Object>> refs) {
        ValidationResultVO vo = new ValidationResultVO();
        vo.setTaskId(material.getTaskId());
        vo.setMaterialId(material.getId());
        vo.setRuleId(rule.getId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setCategory(rule.getCategory());
        vo.setSeverity(rule.getSeverity());
        vo.setMetricName(metricName);
        vo.setActualValue(actual);
        vo.setExpectedValue(expected);
        vo.setMessage(message);
        vo.setSuggestion(suggestion);
        vo.setSourceRefs(JsonUtil.toJson(refs));
        vo.setStatus("PENDING");
        return vo;
    }

    private List<Map<String, Object>> refs(BusinessMetricVO metric) {
        if (metric.getSourceRefs() == null || metric.getSourceRefs().isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> refs = JsonUtil.fromJson(metric.getSourceRefs(),
                new TypeReference<List<Map<String, Object>>>() {
                });
        return refs == null ? List.of() : refs;
    }

    private String slideText(List<MaterialSlideVO> slides, Long slideId, String sourceRefs) {
        if (slideId != null) {
            for (MaterialSlideVO slide : slides) {
                if (slide.getId() != null && slide.getId().equals(slideId)) {
                    return slide.getRawText();
                }
            }
        }
        if (sourceRefs != null && !sourceRefs.isBlank()) {
            List<Map<String, Object>> refs = JsonUtil.fromJson(sourceRefs,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            if (refs != null && !refs.isEmpty() && refs.get(0).get("quote") != null) {
                return String.valueOf(refs.get(0).get("quote"));
            }
        }
        return "";
    }

    private String allText(List<MaterialSlideVO> slides) {
        StringBuilder sb = new StringBuilder();
        for (MaterialSlideVO slide : slides) {
            if (slide.getTitle() != null) {
                sb.append(slide.getTitle()).append('\n');
            }
            if (slide.getRawText() != null) {
                sb.append(slide.getRawText()).append('\n');
            }
        }
        return sb.toString();
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private BigDecimal toBigDecimal(Object value, String defaultValue) {
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return new BigDecimal(defaultValue);
        }
    }
}
