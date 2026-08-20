package com.huawei.fin.ai.material.common.modelconfig;

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

@RestController
@RequestMapping("/api/models")
public class ModelConfigController {

    private final IModelConfigService modelConfigService;

    public ModelConfigController(IModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @GetMapping
    public List<ModelConfigVO> list() {
        return modelConfigService.list();
    }

    @GetMapping("/{id}")
    public ModelConfigVO get(@PathVariable Long id) {
        return modelConfigService.get(id);
    }

    @PostMapping
    public ModelConfigVO create(@RequestBody ModelConfigVO vo) {
        return modelConfigService.create(vo);
    }

    @PutMapping("/{id}")
    public ModelConfigVO update(@PathVariable Long id, @RequestBody ModelConfigVO vo) {
        return modelConfigService.update(id, vo);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        modelConfigService.delete(id);
        return Map.of("success", true);
    }

    @PostMapping("/{id}/toggle")
    public ModelConfigVO toggle(@PathVariable Long id) {
        return modelConfigService.toggle(id);
    }

    @PostMapping("/{id}/test")
    public Map<String, Object> test(@PathVariable Long id) {
        return modelConfigService.test(id);
    }
}
