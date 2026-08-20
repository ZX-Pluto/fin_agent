package com.huawei.fin.ai.material.briefing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class BriefingVO {

    private BriefingHeader header;
    private BriefingOverview overview;
    private List<BriefingIndicator> coreIndicators;
    private List<BriefingDimension> dimensions;
    private List<BriefingFinding> findings;
    private List<BriefingFollowUp> followUps;
    private BriefingEvidence evidence;

    @Data
    public static class BriefingHeader {
        private String organization;
        private String reportPeriod;
        private String materialName;
        private String themeName;
        private String sourceLabel;
        private BigDecimal credibility;
        private Integer slideCount;
        private LocalDateTime generatedAt;
    }

    @Data
    public static class BriefingOverview {
        private String verdict;
        private String judgment;
        private String summaryText;
        private List<String> coreConclusions;
        private BigDecimal businessScore;
    }

    @Data
    public static class BriefingIndicator {
        private String code;
        private String name;
        private BigDecimal value;
        private String unit;
        private BigDecimal change;
        private String changeLabel;
        private String subLabel;
        private String status;
    }

    @Data
    public static class BriefingMetric {
        private String fieldCode;
        private String name;
        private BigDecimal value;
        private String unit;
        private String changeLabel;
    }

    @Data
    public static class BriefingDimension {
        private String code;
        private String name;
        private String level;
        private List<BriefingMetric> metrics;
        private String aiJudgment;
        private List<String> attention;
    }

    @Data
    public static class BriefingFinding {
        private String severity;
        private String subject;
        private String message;
        private String reason;
        private List<String> evidence;
        private String suggestion;
        private List<String> sourceIds;
    }

    @Data
    public static class BriefingFollowUp {
        private Long id;
        private String title;
        private String message;
        private String suggestion;
        private String sourceLabel;
        private String status;
    }

    @Data
    public static class BriefingEvidence {
        private BigDecimal credibility;
        private List<BriefingMetric> metrics;
        private List<BriefingFactSource> factSources;
        private Integer slideCount;
        private String analysisSummary;
    }

    @Data
    public static class BriefingFactSource {
        private String chapter;
        private String slideRange;
        private Integer pageCount;
        private String preview;
    }
}
