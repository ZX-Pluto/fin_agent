package com.huawei.fin.ai.material.followup.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.analysis.dao.AnalysisResultDao;
import com.huawei.fin.ai.material.analysis.vo.AnalysisResultVO;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.followup.dao.FollowUpDao;
import com.huawei.fin.ai.material.followup.vo.FollowUpVO;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Service
public class FollowUpService {

    private final FollowUpDao followUpDao;
    private final IMaterialService materialService;
    private final AnalysisResultDao analysisResultDao;

    public FollowUpService(FollowUpDao followUpDao,
                           IMaterialService materialService,
                           AnalysisResultDao analysisResultDao) {
        this.followUpDao = followUpDao;
        this.materialService = materialService;
        this.analysisResultDao = analysisResultDao;
    }

    public List<FollowUpVO> list() {
        return followUpDao.selectAll();
    }

    @Transactional
    public FollowUpVO create(FollowUpVO vo) {
        if (vo.getStatus() == null || vo.getStatus().isBlank()) {
            vo.setStatus("TODO");
        }
        followUpDao.insert(vo);
        return vo;
    }

    @Transactional
    public FollowUpVO updateStatus(Long id, String status) {
        followUpDao.updateStatus(id, status);
        return list().stream().filter(v -> v.getId().equals(id)).findFirst().orElse(null);
    }

    @Transactional
    public int syncFromAnalysis() {
        int created = 0;
        for (MaterialVO material : materialService.list(null, null)) {
            if (!"COMPLETED".equals(material.getStatus())) {
                continue;
            }
            List<AnalysisResultVO> results = analysisResultDao.selectByMaterialAndType(material.getId(), "ANALYSIS");
            for (AnalysisResultVO result : results) {
                if (result.getResultJson() == null) {
                    continue;
                }
                try {
                    Map<String, Object> map = JsonUtil.fromJson(result.getResultJson(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    if (map == null || !(map.get("findings") instanceof List<?> findings)) {
                        continue;
                    }
                    for (Object item : findings) {
                        if (!(item instanceof Map<?, ?> finding)) {
                            continue;
                        }
                        String severity = String.valueOf(finding.get("severity"));
                        if (!List.of("CRITICAL", "HIGH", "MEDIUM").contains(severity)) {
                            continue;
                        }
                        String subject = finding.get("subject") == null ? "" : String.valueOf(finding.get("subject"));
                        String message = finding.get("message") == null ? "" : String.valueOf(finding.get("message"));
                        String title = subject.isBlank() ? message : subject;
                        if (title.isBlank()) {
                            continue;
                        }
                        if (followUpDao.selectByMaterialAndTitle(material.getId(), title) != null) {
                            continue;
                        }
                        FollowUpVO vo = new FollowUpVO();
                        vo.setMaterialId(material.getId());
                        vo.setTitle(title.length() > 255 ? title.substring(0, 255) : title);
                        vo.setMessage(message);
                        vo.setSuggestion(finding.get("suggestion") == null ? null : String.valueOf(finding.get("suggestion")));
                        vo.setSourceLabel(material.getRegion() + " · " + material.getOrganization() + " · " + material.getReportPeriod());
                        vo.setStatus("TODO");
                        followUpDao.insert(vo);
                        created++;
                    }
                } catch (Exception ignored) {
                    // ignore malformed analysis result
                }
            }
        }
        return created;
    }
}
