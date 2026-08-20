package com.huawei.fin.ai.material.validation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;
import com.huawei.fin.ai.material.validation.dao.ValidationRuleDao;
import com.huawei.fin.ai.material.validation.vo.ValidationRuleVO;

@Service
public class ValidationRuleService implements IValidationRuleService {

    private final ValidationRuleDao ruleDao;

    public ValidationRuleService(ValidationRuleDao ruleDao) {
        this.ruleDao = ruleDao;
    }

    @Override
    public List<ValidationRuleVO> list() {
        return ruleDao.selectAll();
    }

    @Override
    public List<ValidationRuleVO> listEnabled() {
        return ruleDao.selectEnabled();
    }

    @Override
    public ValidationRuleVO get(Long id) {
        ValidationRuleVO vo = ruleDao.selectById(id);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "规则不存在: " + id);
        }
        return vo;
    }

    @Override
    public ValidationRuleVO create(ValidationRuleVO vo) {
        if (vo.getBuiltin() == null) {
            vo.setBuiltin(Boolean.FALSE);
        }
        if (vo.getEnabled() == null) {
            vo.setEnabled(Boolean.TRUE);
        }
        ruleDao.insert(vo);
        return vo;
    }

    @Override
    public ValidationRuleVO update(Long id, ValidationRuleVO vo) {
        get(id);
        vo.setId(id);
        ruleDao.update(vo);
        return vo;
    }

    @Override
    public void delete(Long id) {
        get(id);
        ruleDao.delete(id);
    }

    @Override
    public ValidationRuleVO toggle(Long id) {
        ValidationRuleVO vo = get(id);
        vo.setEnabled(!Boolean.TRUE.equals(vo.getEnabled()));
        ruleDao.update(vo);
        return vo;
    }
}
