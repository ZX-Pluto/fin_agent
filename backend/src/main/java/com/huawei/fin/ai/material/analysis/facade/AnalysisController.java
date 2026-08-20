package com.huawei.fin.ai.material.analysis.facade;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huawei.fin.ai.material.analysis.service.FactSourceService;
import com.huawei.fin.ai.material.analysis.service.BusinessAnalysisService;
import com.huawei.fin.ai.material.analysis.service.FactMappingService;
import com.huawei.fin.ai.material.analysis.service.PreAuditService;
import com.huawei.fin.ai.material.analysis.vo.AnalysisResultVO;
import com.huawei.fin.ai.material.analysis.service.ThemeService;
import com.huawei.fin.ai.material.analysis.vo.FactSourceVO;
import com.huawei.fin.ai.material.analysis.vo.ModelDataVO;
import com.huawei.fin.ai.material.analysis.vo.ThemeVO;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final ThemeService themeService;
    private final FactSourceService factSourceService;
    private final PreAuditService preAuditService;
    private final FactMappingService factMappingService;
    private final BusinessAnalysisService businessAnalysisService;

    public AnalysisController(ThemeService themeService,
                              FactSourceService factSourceService,
                              PreAuditService preAuditService,
                              FactMappingService factMappingService,
                              BusinessAnalysisService businessAnalysisService) {
        this.themeService = themeService;
        this.factSourceService = factSourceService;
        this.preAuditService = preAuditService;
        this.factMappingService = factMappingService;
        this.businessAnalysisService = businessAnalysisService;
    }

    @GetMapping("/themes")
    public List<ThemeVO> themes() {
        return themeService.list();
    }

    @GetMapping("/themes/{id}")
    public ThemeVO theme(@PathVariable Long id) {
        return themeService.get(id);
    }

    @PostMapping("/themes")
    public ThemeVO createTheme(@RequestBody ThemeVO vo) {
        return themeService.create(vo);
    }

    @PutMapping("/themes/{id}")
    public ThemeVO updateTheme(@PathVariable Long id, @RequestBody ThemeVO vo) {
        return themeService.update(id, vo);
    }

    @DeleteMapping("/themes/{id}")
    public Map<String, Object> deleteTheme(@PathVariable Long id) {
        themeService.delete(id);
        return Map.of("success", true);
    }

    @GetMapping("/materials/{materialId}/fact-sources")
    public List<FactSourceVO> factSources(@PathVariable Long materialId) {
        return factSourceService.listByMaterial(materialId);
    }

    @PostMapping("/materials/{materialId}/fact-sources/generate")
    public List<FactSourceVO> generate(@PathVariable Long materialId) {
        return factSourceService.generate(materialId);
    }

    @PostMapping("/materials/{materialId}/pre-audit")
    public AnalysisResultVO preAudit(@PathVariable Long materialId,
                                     @org.springframework.web.bind.annotation.RequestParam Long themeId) {
        return preAuditService.run(materialId, themeId);
    }

    @GetMapping("/materials/{materialId}/pre-audit")
    public List<AnalysisResultVO> preAuditList(@PathVariable Long materialId) {
        return preAuditService.list(materialId);
    }

    @PostMapping("/materials/{materialId}/model-data/map")
    public List<ModelDataVO> map(@PathVariable Long materialId,
                                 @org.springframework.web.bind.annotation.RequestParam Long themeId) {
        return factMappingService.map(materialId, themeId);
    }

    @GetMapping("/materials/{materialId}/model-data")
    public List<ModelDataVO> modelData(@PathVariable Long materialId) {
        return factMappingService.list(materialId);
    }

    @PostMapping("/materials/{materialId}/analysis")
    public AnalysisResultVO analyze(@PathVariable Long materialId,
                                    @org.springframework.web.bind.annotation.RequestParam Long themeId) {
        return businessAnalysisService.run(materialId, themeId);
    }

    @GetMapping("/materials/{materialId}/analysis")
    public List<AnalysisResultVO> analysisList(@PathVariable Long materialId) {
        return businessAnalysisService.list(materialId);
    }
}
