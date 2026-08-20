package com.huawei.fin.ai.material.briefing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.analysis.dao.AnalysisResultDao;
import com.huawei.fin.ai.material.analysis.dao.ModelDataDao;
import com.huawei.fin.ai.material.analysis.dao.ModelDao;
import com.huawei.fin.ai.material.analysis.dao.ModelFieldDao;
import com.huawei.fin.ai.material.analysis.dao.ThemeDao;
import com.huawei.fin.ai.material.analysis.service.FactSourceService;
import com.huawei.fin.ai.material.analysis.vo.AnalysisResultVO;
import com.huawei.fin.ai.material.analysis.vo.FactSourceVO;
import com.huawei.fin.ai.material.analysis.vo.ModelDataVO;
import com.huawei.fin.ai.material.analysis.vo.ModelFieldVO;
import com.huawei.fin.ai.material.analysis.vo.ModelVO;
import com.huawei.fin.ai.material.analysis.vo.ThemeVO;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingDimension;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingEvidence;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingFactSource;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingFinding;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingFollowUp;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingHeader;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingIndicator;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingMetric;
import com.huawei.fin.ai.material.briefing.BriefingVO.BriefingOverview;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.followup.dao.FollowUpDao;
import com.huawei.fin.ai.material.followup.vo.FollowUpVO;
import com.huawei.fin.ai.material.knowledge.dao.KnowledgeDao;
import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.service.MaterialSlideDao;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.summary.service.ISummaryService;
import com.huawei.fin.ai.material.summary.vo.SummaryVO;

@Service
public class BriefingService {

    private static final Map<String, String> FIELD_NAMES = fieldNames();

    private final IMaterialService materialService;
    private final ThemeDao themeDao;
    private final ModelDao modelDao;
    private final ModelFieldDao modelFieldDao;
    private final ModelDataDao modelDataDao;
    private final AnalysisResultDao analysisResultDao;
    private final FactSourceService factSourceService;
    private final KnowledgeDao knowledgeDao;
    private final FollowUpDao followUpDao;
    private final MaterialSlideDao materialSlideDao;
    private final ISummaryService summaryService;

    public BriefingService(IMaterialService materialService,
                           ThemeDao themeDao,
                           ModelDao modelDao,
                           ModelFieldDao modelFieldDao,
                           ModelDataDao modelDataDao,
                           AnalysisResultDao analysisResultDao,
                           FactSourceService factSourceService,
                           KnowledgeDao knowledgeDao,
                           FollowUpDao followUpDao,
                           MaterialSlideDao materialSlideDao,
                           ISummaryService summaryService) {
        this.materialService = materialService;
        this.themeDao = themeDao;
        this.modelDao = modelDao;
        this.modelFieldDao = modelFieldDao;
        this.modelDataDao = modelDataDao;
        this.analysisResultDao = analysisResultDao;
        this.factSourceService = factSourceService;
        this.knowledgeDao = knowledgeDao;
        this.followUpDao = followUpDao;
        this.materialSlideDao = materialSlideDao;
        this.summaryService = summaryService;
    }

    public BriefingVO get(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        ThemeVO theme = material.getThemeId() == null ? null : themeDao.selectById(material.getThemeId());
        List<ModelDataVO> modelData = modelDataDao.selectByMaterialId(materialId);
        Map<String, String> fieldNames = fieldNameMap(material.getThemeId());

        List<AnalysisResultVO> analysisList = analysisResultDao.selectByMaterialAndType(materialId, "ANALYSIS");
        List<AnalysisResultVO> preAuditList = analysisResultDao.selectByMaterialAndType(materialId, "PRE_AUDIT");
        AnalysisResultVO analysis = analysisList.isEmpty() ? null : analysisList.get(0);

        List<Map<String, Object>> findings = parseFindings(analysis);
        if (findings.isEmpty() && !preAuditList.isEmpty()) {
            findings = parseFindings(preAuditList.get(0));
        }

        List<KnowledgeVO> knowledge = knowledgeDao.selectByMaterial(materialId, null);
        List<FactSourceVO> factSources = factSourceService.listByMaterial(materialId);
        List<FollowUpVO> followUps = followUpDao.selectByMaterialId(materialId);
        List<MaterialSlideVO> slides = materialSlideDao.selectByMaterialId(materialId);
        SummaryVO summary = summaryService.build(materialId);

        BriefingVO vo = new BriefingVO();
        vo.setHeader(buildHeader(material, theme, slides.size()));
        vo.setOverview(buildOverview(analysis, findings, knowledge, summary, material));
        vo.setCoreIndicators(buildIndicators(modelData));
        vo.setDimensions(buildDimensions(modelData, findings, fieldNames));
        vo.setFindings(buildFindings(findings));
        vo.setFollowUps(buildFollowUps(followUps));
        vo.setEvidence(buildEvidence(material, modelData, factSources, slides.size(), fieldNames));
        return vo;
    }

    private BriefingHeader buildHeader(MaterialVO material, ThemeVO theme, int slideCount) {
        BriefingHeader header = new BriefingHeader();
        header.setOrganization(material.getOrganization());
        header.setReportPeriod(material.getReportPeriod());
        header.setMaterialName(material.getMaterialName());
        header.setThemeName(theme == null ? null : theme.getName());
        header.setSourceLabel(String.join(" · ",
                List.of(blankToDash(material.getRegion()), blankToDash(material.getOrganization()),
                        blankToDash(material.getReportPeriod()))));
        header.setCredibility(credibility(material));
        header.setSlideCount(slideCount);
        header.setGeneratedAt(java.time.LocalDateTime.now());
        return header;
    }

    private BriefingOverview buildOverview(AnalysisResultVO analysis,
                                           List<Map<String, Object>> findings,
                                           List<KnowledgeVO> knowledge,
                                           SummaryVO summary,
                                           MaterialVO material) {
        BriefingOverview overview = new BriefingOverview();
        String verdict = analysis == null ? null : analysis.getVerdict();
        overview.setVerdict(verdict);
        overview.setJudgment(judgmentLabel(verdict));

        String summaryText = extractAnalysisSummary(analysis);
        if (isBlank(summaryText)) {
            summaryText = summary.getSummaryText();
        }
        if (isBlank(summaryText)) {
            summaryText = overview.getJudgment();
        }
        overview.setSummaryText(summaryText);

        List<String> conclusions = new ArrayList<>();
        for (Map<String, Object> finding : findings) {
            String subject = str(finding.get("subject"));
            String message = str(finding.get("message"));
            String text = isBlank(subject) ? message : subject + "：" + message;
            if (!isBlank(text)) {
                conclusions.add(text);
            }
            if (conclusions.size() >= 3) {
                break;
            }
        }
        if (conclusions.isEmpty()) {
            for (KnowledgeVO item : knowledge) {
                if ("HIGHLIGHT".equals(item.getKnowledgeType()) && !isBlank(item.getContent())) {
                    conclusions.add(item.getContent());
                }
                if (conclusions.size() >= 3) {
                    break;
                }
            }
        }
        if (conclusions.isEmpty() && !isBlank(summaryText)) {
            conclusions.add(summaryText);
        }
        overview.setCoreConclusions(conclusions);
        overview.setBusinessScore(summary.getBusinessScore() == null ? material.getBusinessScore() : summary.getBusinessScore());
        return overview;
    }

    private List<BriefingIndicator> buildIndicators(List<ModelDataVO> data) {
        List<BriefingIndicator> list = new ArrayList<>();
        list.add(buildIndicator("order", "订货", data, new String[]{"order_amount"}, new String[]{"order_yoy"}, null, null, "%", false));
        list.add(buildIndicator("revenue", "收入", data,
                new String[]{"revenue_amount", "revenue"}, new String[]{"revenue_yoy", "revenue_growth"},
                "revenue_target_rate", "达成率", "%", false));
        list.add(buildIndicator("gross_profit", "毛利", data,
                new String[]{"gross_profit"}, new String[]{"gross_profit_yoy"},
                "gross_margin", "毛利率", "%", false));
        list.add(buildIndicator("collection", "回款", data,
                new String[]{"collection_amount", "collection"}, new String[]{"collection_yoy", "collection_growth"},
                "collection_rate", "回款率", "%", false));
        list.add(buildIndicator("dso", "DSO", data,
                new String[]{"dso"}, new String[]{"dso_yoy"}, null, null, "天", true));
        list.add(buildIndicator("net_profit", "净利润", data,
                new String[]{"net_profit", "profit"}, new String[]{"profit_growth"},
                "net_margin", "净利率", "%", false));
        return list;
    }

    private BriefingIndicator buildIndicator(String code,
                                             String name,
                                             List<ModelDataVO> data,
                                             String[] amountCodes,
                                             String[] changeCodes,
                                             String subCode,
                                             String subPrefix,
                                             String changeUnit,
                                             boolean reversed) {
        BriefingIndicator indicator = new BriefingIndicator();
        indicator.setCode(code);
        indicator.setName(name);
        ModelDataVO amount = firstData(data, amountCodes);
        ModelDataVO change = firstData(data, changeCodes);
        if (amount != null) {
            indicator.setValue(amount.getFieldValue());
            indicator.setUnit(amount.getUnit());
        }
        if (change != null) {
            indicator.setChange(change.getFieldValue());
            indicator.setChangeLabel("同比 " + signed(change.getFieldValue()) + changeUnit);
            indicator.setStatus(changeStatus(change.getFieldValue(), reversed));
        } else {
            indicator.setStatus("NONE");
        }
        ModelDataVO sub = subCode == null ? null : firstData(data, new String[]{subCode});
        if (sub != null && sub.getFieldValue() != null) {
            indicator.setSubLabel(subPrefix + " " + trimNumber(sub.getFieldValue()) + sub.getUnit());
        }
        return indicator;
    }

    private List<BriefingDimension> buildDimensions(List<ModelDataVO> data,
                                                    List<Map<String, Object>> findings,
                                                    Map<String, String> fieldNames) {
        List<DimensionDef> defs = List.of(
                new DimensionDef("REVENUE", "收入分析",
                        List.of("收入", "订货", "签约", "达成"),
                        List.of("order_amount", "order_yoy", "order_mom", "revenue_amount", "revenue",
                                "revenue_yoy", "revenue_growth", "revenue_mom", "revenue_target_rate",
                                "contract_amount", "contract_yoy")),
                new DimensionDef("PROFIT", "盈利分析",
                        List.of("毛利", "利润", "盈利", "净利", "销毛"),
                        List.of("gross_profit", "gross_margin", "gross_profit_yoy",
                                "sales_gross_profit", "sales_gross_margin", "sales_gross_profit_yoy",
                                "net_profit", "net_margin", "profit", "profit_growth", "profit_target_rate")),
                new DimensionDef("COLLECTION", "回款分析",
                        List.of("回款", "DSO", "应收"),
                        List.of("collection_amount", "collection", "collection_yoy", "collection_growth",
                                "collection_rate", "dso", "dso_yoy")),
                new DimensionDef("FORECAST", "预测分析",
                        List.of("预测", "H1", "全年"),
                        List.of("h1_order_forecast", "h1_revenue_forecast", "h1_gross_profit_forecast",
                                "fy_order_forecast", "fy_revenue_forecast", "fy_gross_profit_forecast")),
                new DimensionDef("RISK", "风险分析",
                        List.of("风险", "拨备", "库存", "异常"),
                        List.of("risk_amount", "risk_ratio", "provision_amount", "provision_rate",
                                "inventory", "inventory_growth")));

        List<BriefingDimension> dimensions = new ArrayList<>();
        for (DimensionDef def : defs) {
            dimensions.add(buildDimension(def, data, findings, fieldNames));
        }
        return dimensions;
    }

    private BriefingDimension buildDimension(DimensionDef def,
                                             List<ModelDataVO> data,
                                             List<Map<String, Object>> findings,
                                             Map<String, String> fieldNames) {
        BriefingDimension dimension = new BriefingDimension();
        dimension.setCode(def.code);
        dimension.setName(def.name);

        List<BriefingMetric> metrics = new ArrayList<>();
        for (String code : def.fieldCodes) {
            ModelDataVO row = firstData(data, code);
            if (row == null || row.getFieldValue() == null) {
                continue;
            }
            BriefingMetric metric = new BriefingMetric();
            metric.setFieldCode(row.getFieldCode());
            metric.setName(fieldNames.getOrDefault(row.getFieldCode(), FIELD_NAMES.getOrDefault(row.getFieldCode(), row.getFieldCode())));
            metric.setValue(row.getFieldValue());
            metric.setUnit(row.getUnit());
            metric.setChangeLabel(changeLabel(row.getFieldCode(), row.getFieldValue(), row.getUnit()));
            metrics.add(metric);
        }
        dimension.setMetrics(metrics);

        List<Map<String, Object>> matched = findings.stream()
                .filter(f -> containsKeyword(f, def.keywords))
                .toList();
        dimension.setAiJudgment(buildAiJudgment(def, matched, metrics));
        dimension.setAttention(buildAttention(matched));
        dimension.setLevel(buildLevel(matched, metrics));
        return dimension;
    }

    private String buildAiJudgment(DimensionDef def, List<Map<String, Object>> matched, List<BriefingMetric> metrics) {
        if (!matched.isEmpty()) {
            Map<String, Object> first = matched.get(0);
            String subject = str(first.get("subject"));
            String message = str(first.get("message"));
            if (isBlank(subject)) {
                return message;
            }
            return subject + "：" + message;
        }
        if (metrics.isEmpty()) {
            return "材料未提供该维度数据，暂无法形成判断。";
        }
        return "该维度数据齐全，未发现明显异常信号。";
    }

    private List<String> buildAttention(List<Map<String, Object>> matched) {
        List<String> attention = new ArrayList<>();
        for (int i = 1; i < matched.size() && attention.size() < 3; i++) {
            Map<String, Object> finding = matched.get(i);
            String subject = str(finding.get("subject"));
            String message = str(finding.get("message"));
            attention.add(isBlank(subject) ? message : subject + "：" + message);
        }
        return attention;
    }

    private String buildLevel(List<Map<String, Object>> matched, List<BriefingMetric> metrics) {
        if (matched.stream().anyMatch(f -> List.of("CRITICAL", "HIGH").contains(str(f.get("severity"))))) {
            return "RED";
        }
        if (matched.stream().anyMatch(f -> "MEDIUM".equals(str(f.get("severity"))))) {
            return "YELLOW";
        }
        return metrics.isEmpty() ? "NONE" : "GREEN";
    }

    private List<BriefingFinding> buildFindings(List<Map<String, Object>> findings) {
        List<Map<String, Object>> sorted = findings.stream()
                .sorted((a, b) -> severityOrder(str(a.get("severity"))) - severityOrder(str(b.get("severity"))))
                .toList();
        List<BriefingFinding> result = new ArrayList<>();
        for (Map<String, Object> item : sorted) {
            BriefingFinding finding = new BriefingFinding();
            finding.setSeverity(str(item.get("severity")));
            finding.setSubject(str(item.get("subject")));
            finding.setMessage(str(item.get("message")));
            finding.setReason(str(item.get("reason")));
            finding.setSuggestion(str(item.get("suggestion")));
            finding.setEvidence(stringList(item.get("evidence")));
            finding.setSourceIds(stringList(item.get("sourceIds")));
            result.add(finding);
        }
        return result;
    }

    private List<BriefingFollowUp> buildFollowUps(List<FollowUpVO> followUps) {
        return followUps.stream().map(f -> {
            BriefingFollowUp vo = new BriefingFollowUp();
            vo.setId(f.getId());
            vo.setTitle(f.getTitle());
            vo.setMessage(f.getMessage());
            vo.setSuggestion(f.getSuggestion());
            vo.setSourceLabel(f.getSourceLabel());
            vo.setStatus(f.getStatus());
            return vo;
        }).toList();
    }

    private BriefingEvidence buildEvidence(MaterialVO material,
                                           List<ModelDataVO> modelData,
                                           List<FactSourceVO> factSources,
                                           int slideCount,
                                           Map<String, String> fieldNames) {
        BriefingEvidence evidence = new BriefingEvidence();
        evidence.setCredibility(credibility(material));
        evidence.setSlideCount(slideCount);
        evidence.setAnalysisSummary(material.getSummaryText());
        List<BriefingMetric> metrics = modelData.stream().map(row -> {
            BriefingMetric metric = new BriefingMetric();
            metric.setFieldCode(row.getFieldCode());
            metric.setName(fieldNames.getOrDefault(row.getFieldCode(), FIELD_NAMES.getOrDefault(row.getFieldCode(), row.getFieldCode())));
            metric.setValue(row.getFieldValue());
            metric.setUnit(row.getUnit());
            metric.setChangeLabel(changeLabel(row.getFieldCode(), row.getFieldValue(), row.getUnit()));
            return metric;
        }).toList();
        evidence.setMetrics(metrics);
        evidence.setFactSources(factSources.stream().map(f -> {
            BriefingFactSource source = new BriefingFactSource();
            source.setChapter(f.getChapter());
            source.setSlideRange(f.getSlideRange());
            source.setPageCount(pageCount(f.getSlideRange()));
            source.setPreview(compact(f.getParseJson(), 240));
            return source;
        }).toList());
        return evidence;
    }

    private Map<String, String> fieldNameMap(Long themeId) {
        Map<String, String> map = new LinkedHashMap<>(FIELD_NAMES);
        if (themeId == null) {
            return map;
        }
        List<ModelVO> models = modelDao.selectByThemeId(themeId);
        if (models.isEmpty()) {
            return map;
        }
        for (ModelFieldVO field : modelFieldDao.selectByModelId(models.get(0).getId())) {
            map.put(field.getFieldCode(), field.getFieldName());
        }
        return map;
    }

    private List<Map<String, Object>> parseFindings(AnalysisResultVO result) {
        if (result == null || result.getResultJson() == null) {
            return List.of();
        }
        try {
            Map<String, Object> map = JsonUtil.fromJson(result.getResultJson(), new TypeReference<Map<String, Object>>() {
            });
            if (map == null || !(map.get("findings") instanceof List<?> list)) {
                return List.of();
            }
            List<Map<String, Object>> findings = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> raw) {
                    Map<String, Object> finding = new LinkedHashMap<>();
                    raw.forEach((k, v) -> finding.put(String.valueOf(k), v));
                    findings.add(finding);
                }
            }
            return findings;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String extractAnalysisSummary(AnalysisResultVO result) {
        if (result == null || result.getResultJson() == null) {
            return null;
        }
        try {
            Map<String, Object> map = JsonUtil.fromJson(result.getResultJson(), new TypeReference<Map<String, Object>>() {
            });
            return map == null ? null : str(map.get("summary"));
        } catch (Exception e) {
            return null;
        }
    }

    private ModelDataVO firstData(List<ModelDataVO> data, String... codes) {
        for (String code : codes) {
            if (code == null) {
                continue;
            }
            for (ModelDataVO row : data) {
                if (code.equals(row.getFieldCode()) && row.getFieldValue() != null) {
                    return row;
                }
            }
        }
        return null;
    }

    private String changeLabel(String fieldCode, BigDecimal value, String unit) {
        if (value == null) {
            return null;
        }
        if ("dso_yoy".equals(fieldCode)) {
            return "同比 " + signed(value) + "天";
        }
        if (fieldCode.endsWith("_yoy") || "revenue_growth".equals(fieldCode)
                || "profit_growth".equals(fieldCode) || "collection_growth".equals(fieldCode)
                || "inventory_growth".equals(fieldCode)) {
            return "同比 " + signed(value) + "%";
        }
        if (fieldCode.endsWith("_mom")) {
            return "环比 " + signed(value) + "%";
        }
        return null;
    }

    private String changeStatus(BigDecimal value, boolean reversed) {
        if (value == null) {
            return "NONE";
        }
        boolean positive = value.signum() >= 0;
        return positive == !reversed ? "OK" : "DANGER";
    }

    private boolean containsKeyword(Map<String, Object> finding, List<String> keywords) {
        String text = str(finding.get("subject")) + str(finding.get("message")) + str(finding.get("reason"));
        return keywords.stream().anyMatch(text::contains);
    }

    private int severityOrder(String severity) {
        return switch (severity == null ? "" : severity) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 9;
        };
    }

    private String judgmentLabel(String verdict) {
        return switch (verdict == null ? "" : verdict) {
            case "NORMAL" -> "整体经营稳中向好";
            case "ABNORMAL" -> "整体经营存在关注风险";
            case "REJECT" -> "材料质量不足，建议补充后再分析";
            case "PASS" -> "材料通过预审，经营分析完成";
            default -> "经营状态需人工确认";
        };
    }

    private int pageCount(String slideRange) {
        if (slideRange == null || slideRange.isBlank()) {
            return 0;
        }
        try {
            List<?> list = JsonUtil.fromJson(slideRange, new TypeReference<List<?>>() {
            });
            return list == null ? 0 : list.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String signed(BigDecimal value) {
        return value.signum() >= 0 ? "+" + trimNumber(value) : trimNumber(value);
    }

    private String trimNumber(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String compact(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private BigDecimal credibility(MaterialVO material) {
        if (material.getConfidence() == null) {
            return BigDecimal.valueOf(95);
        }
        BigDecimal confidence = material.getConfidence();
        if (confidence.compareTo(BigDecimal.ONE) <= 0) {
            return confidence.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP);
        }
        return confidence;
    }

    private String blankToDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    private static Map<String, String> fieldNames() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("revenue", "收入");
        map.put("revenue_growth", "收入同比");
        map.put("profit", "利润");
        map.put("profit_growth", "利润同比");
        map.put("collection", "回款");
        map.put("collection_growth", "回款同比");
        map.put("dso", "DSO");
        map.put("inventory", "存货");
        map.put("inventory_growth", "存货同比");
        map.put("gross_margin", "毛利率");
        map.put("revenue_target", "收入目标");
        map.put("revenue_target_rate", "收入目标达成率");
        map.put("profit_target", "利润目标");
        map.put("profit_target_rate", "利润目标达成率");
        map.put("order_amount", "订货金额");
        map.put("order_yoy", "订货同比");
        map.put("order_mom", "订货环比");
        map.put("revenue_amount", "收入金额");
        map.put("revenue_yoy", "收入同比");
        map.put("revenue_mom", "收入环比");
        map.put("contract_amount", "签约金额");
        map.put("contract_yoy", "签约同比");
        map.put("gross_profit", "毛利");
        map.put("gross_profit_yoy", "毛利同比变化");
        map.put("sales_gross_profit", "销售毛利");
        map.put("sales_gross_margin", "销毛率");
        map.put("sales_gross_profit_yoy", "销售毛利同比变化");
        map.put("net_profit", "净利润");
        map.put("net_margin", "净利率");
        map.put("collection_rate", "回款率");
        map.put("dso_yoy", "DSO同比变化");
        map.put("h1_order_forecast", "H1订货预测");
        map.put("h1_revenue_forecast", "H1收入预测");
        map.put("h1_gross_profit_forecast", "H1毛利预测");
        map.put("fy_order_forecast", "全年订货预测");
        map.put("fy_revenue_forecast", "全年收入预测");
        map.put("fy_gross_profit_forecast", "全年毛利预测");
        map.put("risk_amount", "收入风险金额");
        map.put("risk_ratio", "收入风险占比");
        map.put("provision_amount", "拨备金额");
        map.put("provision_rate", "拨备率");
        return map;
    }

    private static class DimensionDef {
        private final String code;
        private final String name;
        private final List<String> keywords;
        private final List<String> fieldCodes;

        private DimensionDef(String code, String name, List<String> keywords, List<String> fieldCodes) {
            this.code = code;
            this.name = name;
            this.keywords = keywords;
            this.fieldCodes = fieldCodes;
        }
    }
}
