package com.huawei.fin.ai.material.material.agent;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.analysis.service.FactSourceService;
import com.huawei.fin.ai.material.material.service.IMaterialParseService;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.vo.MaterialParseResultVO;
import com.huawei.fin.ai.material.material.vo.MaterialStatus;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Component
public class MaterialParsingAgent {

    private final IMaterialService materialService;
    private final IMaterialParseService parseService;
    private final FactSourceService factSourceService;

    public MaterialParsingAgent(IMaterialService materialService,
                                IMaterialParseService parseService,
                                FactSourceService factSourceService) {
        this.materialService = materialService;
        this.parseService = parseService;
        this.factSourceService = factSourceService;
    }

    public void execute(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        materialService.updateStatus(materialId, MaterialStatus.PARSING.name());
        MaterialParseResultVO result = parseService.parse(material);
        String irJson = parseService.buildBusinessIr(material, result);
        materialService.updateIr(materialId, irJson, new BigDecimal("0.70"));
        factSourceService.generate(materialId);
        materialService.updateStatus(materialId, MaterialStatus.PARSED.name());
    }
}
