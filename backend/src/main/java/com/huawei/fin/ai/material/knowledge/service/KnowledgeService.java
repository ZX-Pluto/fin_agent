package com.huawei.fin.ai.material.knowledge.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.knowledge.dao.KnowledgeDao;
import com.huawei.fin.ai.material.knowledge.vo.HighlightVO;
import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;
import com.huawei.fin.ai.material.knowledge.vo.RiskVO;
import com.huawei.fin.ai.material.validation.dao.BusinessMetricDao;
import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;

@Service
public class KnowledgeService implements IKnowledgeService {

    private final KnowledgeDao knowledgeDao;
    private final BusinessMetricDao businessMetricDao;

    public KnowledgeService(KnowledgeDao knowledgeDao, BusinessMetricDao businessMetricDao) {
        this.knowledgeDao = knowledgeDao;
        this.businessMetricDao = businessMetricDao;
    }

    @Override
    public List<KnowledgeVO> list(Long materialId, String type) {
        return knowledgeDao.selectByMaterial(materialId, type);
    }

    @Override
    public List<BusinessMetricVO> listMetrics(Long materialId) {
        return businessMetricDao.selectByMaterialId(materialId);
    }

    @Override
    public List<HighlightVO> listHighlights(Long materialId) {
        return knowledgeDao.selectByMaterial(materialId, "HIGHLIGHT").stream().map(vo -> {
            HighlightVO highlight = new HighlightVO();
            highlight.setId(vo.getId());
            highlight.setMaterialId(vo.getMaterialId());
            highlight.setContent(vo.getContent());
            highlight.setSourceRefs(vo.getSourceRefs());
            return highlight;
        }).toList();
    }

    @Override
    public List<RiskVO> listRisks(Long materialId) {
        return knowledgeDao.selectByMaterial(materialId, "RISK").stream().map(vo -> {
            RiskVO risk = new RiskVO();
            risk.setId(vo.getId());
            risk.setMaterialId(vo.getMaterialId());
            risk.setContent(vo.getContent());
            risk.setSourceRefs(vo.getSourceRefs());
            return risk;
        }).toList();
    }
}
