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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.huawei.fin.ai.material.analysis.service.RulePackageService;
import com.huawei.fin.ai.material.analysis.vo.RuleItemVO;
import com.huawei.fin.ai.material.analysis.vo.RulePackageVO;

@RestController
@RequestMapping("/api/rule-packages")
public class RulePackageController {

    private final RulePackageService rulePackageService;

    public RulePackageController(RulePackageService rulePackageService) {
        this.rulePackageService = rulePackageService;
    }

    @GetMapping
    public List<RulePackageVO> list(@RequestParam(required = false) Long themeId) {
        return rulePackageService.list(themeId);
    }

    @GetMapping("/{id}")
    public RulePackageVO get(@PathVariable Long id) {
        return rulePackageService.get(id);
    }

    @PostMapping
    public RulePackageVO create(@RequestBody RulePackageVO vo) {
        return rulePackageService.create(vo);
    }

    @PutMapping("/{id}")
    public RulePackageVO update(@PathVariable Long id, @RequestBody RulePackageVO vo) {
        return rulePackageService.update(id, vo);
    }

    @PostMapping("/{id}/toggle")
    public RulePackageVO toggle(@PathVariable Long id) {
        return rulePackageService.toggle(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        rulePackageService.delete(id);
        return Map.of("success", true);
    }

    @GetMapping("/{id}/items")
    public List<RuleItemVO> items(@PathVariable Long id) {
        return rulePackageService.items(id);
    }

    @PostMapping("/{id}/items")
    public RuleItemVO createItem(@PathVariable Long id, @RequestBody RuleItemVO vo) {
        return rulePackageService.createItem(id, vo);
    }

    @PutMapping("/{id}/items/{itemId}")
    public RuleItemVO updateItem(@PathVariable Long id, @PathVariable Long itemId, @RequestBody RuleItemVO vo) {
        return rulePackageService.updateItem(itemId, vo);
    }

    @PostMapping("/{id}/items/{itemId}/toggle")
    public RuleItemVO toggleItem(@PathVariable Long id, @PathVariable Long itemId) {
        return rulePackageService.toggleItem(itemId);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public Map<String, Object> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        rulePackageService.deleteItem(itemId);
        return Map.of("success", true);
    }
}
