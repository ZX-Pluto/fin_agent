package com.huawei.fin.ai.material.analysis.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huawei.fin.ai.material.analysis.dao.RuleItemDao;
import com.huawei.fin.ai.material.analysis.dao.RulePackageDao;
import com.huawei.fin.ai.material.analysis.vo.RuleItemVO;
import com.huawei.fin.ai.material.analysis.vo.RulePackageVO;
import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;

@Service
public class RulePackageService {

    private final RulePackageDao rulePackageDao;
    private final RuleItemDao ruleItemDao;

    public RulePackageService(RulePackageDao rulePackageDao, RuleItemDao ruleItemDao) {
        this.rulePackageDao = rulePackageDao;
        this.ruleItemDao = ruleItemDao;
    }

    public List<RulePackageVO> list(Long themeId) {
        List<RulePackageVO> packages = themeId == null
                ? rulePackageDao.selectAll()
                : rulePackageDao.selectByThemeId(themeId);
        packages.forEach(this::fillItems);
        return packages;
    }

    public RulePackageVO get(Long id) {
        RulePackageVO vo = requirePackage(id);
        fillItems(vo);
        return vo;
    }

    @Transactional
    public RulePackageVO create(RulePackageVO vo) {
        if (vo.getEnabled() == null) {
            vo.setEnabled(Boolean.TRUE);
        }
        rulePackageDao.insert(vo);
        return vo;
    }

    @Transactional
    public RulePackageVO update(Long id, RulePackageVO vo) {
        requirePackage(id);
        vo.setId(id);
        rulePackageDao.update(vo);
        return vo;
    }

    @Transactional
    public RulePackageVO toggle(Long id) {
        RulePackageVO vo = requirePackage(id);
        vo.setEnabled(!Boolean.TRUE.equals(vo.getEnabled()));
        rulePackageDao.update(vo);
        return vo;
    }

    @Transactional
    public void delete(Long id) {
        requirePackage(id);
        ruleItemDao.deleteByPackageId(id);
        rulePackageDao.delete(id);
    }

    public List<RuleItemVO> items(Long packageId) {
        requirePackage(packageId);
        return ruleItemDao.selectByPackageId(packageId);
    }

    public RuleItemVO getItem(Long id) {
        RuleItemVO vo = ruleItemDao.selectById(id);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "规则条目不存在: " + id);
        }
        return vo;
    }

    @Transactional
    public RuleItemVO createItem(Long packageId, RuleItemVO vo) {
        requirePackage(packageId);
        if (vo.getEnabled() == null) {
            vo.setEnabled(Boolean.TRUE);
        }
        if (vo.getSeverity() == null || vo.getSeverity().isBlank()) {
            vo.setSeverity("MEDIUM");
        }
        vo.setPackageId(packageId);
        ruleItemDao.insert(vo);
        return vo;
    }

    @Transactional
    public RuleItemVO updateItem(Long id, RuleItemVO vo) {
        requireItem(id);
        vo.setId(id);
        ruleItemDao.update(vo);
        return vo;
    }

    @Transactional
    public RuleItemVO toggleItem(Long id) {
        RuleItemVO vo = requireItem(id);
        vo.setEnabled(!Boolean.TRUE.equals(vo.getEnabled()));
        ruleItemDao.update(vo);
        return vo;
    }

    @Transactional
    public void deleteItem(Long id) {
        requireItem(id);
        ruleItemDao.delete(id);
    }

    private RulePackageVO requirePackage(Long id) {
        RulePackageVO vo = rulePackageDao.selectById(id);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "规则包不存在: " + id);
        }
        return vo;
    }

    private RuleItemVO requireItem(Long id) {
        RuleItemVO vo = ruleItemDao.selectById(id);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "规则条目不存在: " + id);
        }
        return vo;
    }

    private void fillItems(RulePackageVO vo) {
        vo.setItems(ruleItemDao.selectByPackageId(vo.getId()));
    }
}
