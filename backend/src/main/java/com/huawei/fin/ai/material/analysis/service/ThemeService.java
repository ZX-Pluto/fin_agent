package com.huawei.fin.ai.material.analysis.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.analysis.dao.ModelDao;
import com.huawei.fin.ai.material.analysis.dao.ModelFieldDao;
import com.huawei.fin.ai.material.analysis.dao.RuleItemDao;
import com.huawei.fin.ai.material.analysis.dao.RulePackageDao;
import com.huawei.fin.ai.material.analysis.dao.ThemeDao;
import com.huawei.fin.ai.material.analysis.vo.ModelFieldVO;
import com.huawei.fin.ai.material.analysis.vo.ModelVO;
import com.huawei.fin.ai.material.analysis.vo.RuleItemVO;
import com.huawei.fin.ai.material.analysis.vo.RulePackageVO;
import com.huawei.fin.ai.material.analysis.vo.ThemeVO;
import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;
import com.huawei.fin.ai.material.material.service.MaterialDao;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThemeService {

    private final ThemeDao themeDao;
    private final ModelDao modelDao;
    private final ModelFieldDao modelFieldDao;
    private final RulePackageDao rulePackageDao;
    private final RuleItemDao ruleItemDao;
    private final MaterialDao materialDao;

    public ThemeService(ThemeDao themeDao,
                        ModelDao modelDao,
                        ModelFieldDao modelFieldDao,
                        RulePackageDao rulePackageDao,
                        RuleItemDao ruleItemDao,
                        MaterialDao materialDao) {
        this.themeDao = themeDao;
        this.modelDao = modelDao;
        this.modelFieldDao = modelFieldDao;
        this.rulePackageDao = rulePackageDao;
        this.ruleItemDao = ruleItemDao;
        this.materialDao = materialDao;
    }

    public List<ThemeVO> list() {
        List<ThemeVO> themes = themeDao.selectAll();
        return themes.stream().map(this::enrich).toList();
    }

    public ThemeVO get(Long id) {
        ThemeVO theme = themeDao.selectById(id);
        if (theme == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "主题不存在: " + id);
        }
        return enrich(theme);
    }

    @Transactional
    public ThemeVO create(ThemeVO vo) {
        validate(vo);
        themeDao.insert(vo);
        if (vo.getModel() != null) {
            insertModel(vo.getId(), vo.getModel());
        }
        insertPackages(vo.getId(), vo.getRulePackages());
        return get(vo.getId());
    }

    @Transactional
    public ThemeVO update(Long id, ThemeVO vo) {
        ThemeVO old = requireTheme(id);
        validate(vo);
        vo.setId(id);
        themeDao.update(vo);
        replaceModelTree(id, vo.getModel());
        replacePackageTree(id, vo.getRulePackages());
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        requireTheme(id);
        if (materialDao.countByThemeId(id) > 0) {
            throw new MaterialException(MaterialErrorCode.STATE_ERROR, "主题已被材料使用，不能删除");
        }
        replacePackageTree(id, null);
        replaceModelTree(id, null);
        themeDao.delete(id);
    }

    private ThemeVO requireTheme(Long id) {
        ThemeVO vo = themeDao.selectById(id);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "主题不存在: " + id);
        }
        return vo;
    }

    private void validate(ThemeVO vo) {
        if (vo.getCode() == null || vo.getCode().isBlank() || vo.getName() == null || vo.getName().isBlank()) {
            throw new MaterialException(MaterialErrorCode.PARAM_ERROR, "主题编码和名称不能为空");
        }
    }

    private void replaceModelTree(Long themeId, ModelVO model) {
        for (ModelVO old : modelDao.selectByThemeId(themeId)) {
            modelFieldDao.deleteByModelId(old.getId());
            modelDao.delete(old.getId());
        }
        if (model != null) {
            insertModel(themeId, model);
        }
    }

    private void insertModel(Long themeId, ModelVO model) {
        model.setThemeId(themeId);
        model.setVersion(model.getVersion() == null ? 1 : model.getVersion());
        model.setCurrentVersion(model.getCurrentVersion() == null ? Boolean.TRUE : model.getCurrentVersion());
        modelDao.insert(model);
        if (model.getFields() == null) {
            return;
        }
        int seq = 1;
        for (ModelFieldVO field : model.getFields()) {
            field.setModelId(model.getId());
            if (field.getFieldCode() == null || field.getFieldCode().isBlank()
                    || field.getFieldName() == null || field.getFieldName().isBlank()) {
                continue;
            }
            field.setSeqNo(field.getSeqNo() == null ? seq++ : field.getSeqNo());
            modelFieldDao.insert(field);
        }
    }

    private void replacePackageTree(Long themeId, List<RulePackageVO> packages) {
        for (RulePackageVO old : rulePackageDao.selectByThemeId(themeId)) {
            ruleItemDao.deleteByPackageId(old.getId());
            rulePackageDao.delete(old.getId());
        }
        insertPackages(themeId, packages);
    }

    private void insertPackages(Long themeId, List<RulePackageVO> packages) {
        if (packages == null) {
            return;
        }
        for (RulePackageVO pkg : packages) {
            if (pkg.getCode() == null || pkg.getCode().isBlank() || pkg.getName() == null || pkg.getName().isBlank()) {
                continue;
            }
            pkg.setThemeId(themeId);
            pkg.setEnabled(pkg.getEnabled() == null ? Boolean.TRUE : pkg.getEnabled());
            rulePackageDao.insert(pkg);
            insertItems(pkg);
        }
    }

    private void insertItems(RulePackageVO pkg) {
        if (pkg.getItems() == null) {
            return;
        }
        for (RuleItemVO item : pkg.getItems()) {
            if (item.getRuleCode() == null || item.getRuleCode().isBlank()
                    || item.getName() == null || item.getName().isBlank()) {
                continue;
            }
            item.setPackageId(pkg.getId());
            item.setEnabled(item.getEnabled() == null ? Boolean.TRUE : item.getEnabled());
            item.setSeverity(item.getSeverity() == null || item.getSeverity().isBlank() ? "MEDIUM" : item.getSeverity());
            item.setExecutionStrategy(item.getExecutionStrategy() == null || item.getExecutionStrategy().isBlank()
                    ? "AI" : item.getExecutionStrategy());
            ruleItemDao.insert(item);
        }
    }

    private ThemeVO enrich(ThemeVO theme) {
        List<ModelVO> models = modelDao.selectByThemeId(theme.getId());
        if (!models.isEmpty()) {
            ModelVO model = models.get(0);
            model.setFields(modelFieldDao.selectByModelId(model.getId()));
            theme.setModel(model);
        }
        List<RulePackageVO> packages = new ArrayList<>();
        for (RulePackageVO pkg : rulePackageDao.selectByThemeId(theme.getId())) {
            pkg.setItems(ruleItemDao.selectByPackageId(pkg.getId()));
            packages.add(pkg);
        }
        theme.setRulePackages(packages);
        return theme;
    }
}
