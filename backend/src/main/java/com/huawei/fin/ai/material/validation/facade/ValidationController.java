package com.huawei.fin.ai.material.validation.facade;

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

import com.huawei.fin.ai.material.validation.service.IValidationRuleService;
import com.huawei.fin.ai.material.validation.service.IValidationService;
import com.huawei.fin.ai.material.validation.vo.ValidationResultVO;
import com.huawei.fin.ai.material.validation.vo.ValidationRuleVO;

@RestController
@RequestMapping("/api")
public class ValidationController {

    private final IValidationService validationService;
    private final IValidationRuleService ruleService;

    public ValidationController(IValidationService validationService, IValidationRuleService ruleService) {
        this.validationService = validationService;
        this.ruleService = ruleService;
    }

    @GetMapping("/validations")
    public List<ValidationResultVO> list(@RequestParam(value = "materialId", required = false) Long materialId,
                                         @RequestParam(value = "taskId", required = false) Long taskId,
                                         @RequestParam(value = "category", required = false) String category,
                                         @RequestParam(value = "status", required = false) String status,
                                         @RequestParam(value = "severity", required = false) String severity) {
        return validationService.list(materialId, taskId, category, status, severity);
    }

    @GetMapping("/validations/{id}")
    public ValidationResultVO get(@PathVariable Long id) {
        return validationService.get(id);
    }

    @PostMapping("/validations/{id}/confirm")
    public ValidationResultVO confirm(@PathVariable Long id) {
        return validationService.confirm(id);
    }

    @PostMapping("/validations/{id}/ignore")
    public ValidationResultVO ignore(@PathVariable Long id) {
        return validationService.ignore(id);
    }

    @GetMapping("/rules")
    public List<ValidationRuleVO> rules() {
        return ruleService.list();
    }

    @PostMapping("/rules")
    public ValidationRuleVO createRule(@RequestBody ValidationRuleVO vo) {
        return ruleService.create(vo);
    }

    @PutMapping("/rules/{id}")
    public ValidationRuleVO updateRule(@PathVariable Long id, @RequestBody ValidationRuleVO vo) {
        return ruleService.update(id, vo);
    }

    @DeleteMapping("/rules/{id}")
    public Map<String, Object> deleteRule(@PathVariable Long id) {
        ruleService.delete(id);
        return Map.of("success", true);
    }

    @PostMapping("/rules/{id}/toggle")
    public ValidationRuleVO toggleRule(@PathVariable Long id) {
        return ruleService.toggle(id);
    }
}
