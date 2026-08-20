package com.huawei.fin.ai.material.validation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;
import com.huawei.fin.ai.material.validation.dao.ValidationResultDao;
import com.huawei.fin.ai.material.validation.vo.ValidationResultVO;

@Service
public class ValidationService implements IValidationService {

    private final ValidationResultDao resultDao;

    public ValidationService(ValidationResultDao resultDao) {
        this.resultDao = resultDao;
    }

    @Override
    public List<ValidationResultVO> list(Long materialId, Long taskId, String category, String status, String severity) {
        return resultDao.selectList(materialId, taskId, category, status, severity);
    }

    @Override
    public ValidationResultVO get(Long id) {
        ValidationResultVO vo = resultDao.selectById(id);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "校验结果不存在: " + id);
        }
        return vo;
    }

    @Override
    public ValidationResultVO confirm(Long id) {
        ValidationResultVO vo = get(id);
        resultDao.updateStatus(id, "CONFIRMED");
        vo.setStatus("CONFIRMED");
        return vo;
    }

    @Override
    public ValidationResultVO ignore(Long id) {
        ValidationResultVO vo = get(id);
        resultDao.updateStatus(id, "FALSE_POSITIVE");
        vo.setStatus("FALSE_POSITIVE");
        return vo;
    }
}
