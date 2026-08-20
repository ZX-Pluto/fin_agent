package com.huawei.fin.ai.material.knowledge.agent;

import java.util.List;

import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.knowledge.service.IKnowledgeExtractionService;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.service.MaterialSlideDao;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialStatus;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Component
public class KnowledgeExtractionAgent {

    private final IMaterialService materialService;
    private final MaterialSlideDao materialSlideDao;
    private final IKnowledgeExtractionService extractionService;

    public KnowledgeExtractionAgent(IMaterialService materialService,
                                    MaterialSlideDao materialSlideDao,
                                    IKnowledgeExtractionService extractionService) {
        this.materialService = materialService;
        this.materialSlideDao = materialSlideDao;
        this.extractionService = extractionService;
    }

    public void execute(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        materialService.updateStatus(materialId, MaterialStatus.EXTRACTING.name());
        List<MaterialSlideVO> slides = materialSlideDao.selectByMaterialId(materialId);
        extractionService.extract(material, slides);
        materialService.updateStatus(materialId, MaterialStatus.EXTRACTED.name());
    }
}
