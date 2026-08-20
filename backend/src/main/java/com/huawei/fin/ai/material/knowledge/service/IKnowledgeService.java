package com.huawei.fin.ai.material.knowledge.service;

import java.util.List;

import com.huawei.fin.ai.material.knowledge.vo.HighlightVO;
import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;
import com.huawei.fin.ai.material.knowledge.vo.RiskVO;
import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;

public interface IKnowledgeService {

    List<KnowledgeVO> list(Long materialId, String type);

    List<BusinessMetricVO> listMetrics(Long materialId);

    List<HighlightVO> listHighlights(Long materialId);

    List<RiskVO> listRisks(Long materialId);
}
