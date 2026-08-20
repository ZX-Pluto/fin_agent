package com.huawei.fin.ai.material.validation.service;

import java.util.List;

import com.huawei.fin.ai.material.validation.vo.ValidationRuleVO;

public interface IValidationRuleService {

    List<ValidationRuleVO> list();

    List<ValidationRuleVO> listEnabled();

    ValidationRuleVO get(Long id);

    ValidationRuleVO create(ValidationRuleVO vo);

    ValidationRuleVO update(Long id, ValidationRuleVO vo);

    void delete(Long id);

    ValidationRuleVO toggle(Long id);
}
