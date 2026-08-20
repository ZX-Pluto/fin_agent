package com.huawei.fin.ai.material.validation.service;

import java.util.List;

import com.huawei.fin.ai.material.validation.vo.ValidationResultVO;

public interface IValidationService {

    List<ValidationResultVO> list(Long materialId, Long taskId, String category, String status, String severity);

    ValidationResultVO get(Long id);

    ValidationResultVO confirm(Long id);

    ValidationResultVO ignore(Long id);
}
