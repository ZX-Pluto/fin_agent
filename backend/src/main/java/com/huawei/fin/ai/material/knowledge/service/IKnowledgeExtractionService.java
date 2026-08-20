package com.huawei.fin.ai.material.knowledge.service;

import java.util.List;

import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

public interface IKnowledgeExtractionService {

    int extract(MaterialVO material, List<MaterialSlideVO> slides);
}
