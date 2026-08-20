package com.huawei.fin.ai.material.material.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.huawei.fin.ai.material.common.client.FileServiceClient;
import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;
import com.huawei.fin.ai.material.common.util.FileUtil;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialStatus;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.knowledge.dao.KnowledgeDao;
import com.huawei.fin.ai.material.analysis.dao.FactSourceDao;
import com.huawei.fin.ai.material.analysis.dao.ThemeDao;
import com.huawei.fin.ai.material.analysis.vo.ThemeVO;
import com.huawei.fin.ai.material.validation.dao.BusinessMetricDao;
import com.huawei.fin.ai.material.validation.dao.ValidationResultDao;

@Service
public class MaterialService implements IMaterialService {

    private final MaterialDao materialDao;
    private final MaterialSlideDao materialSlideDao;
    private final FileServiceClient fileServiceClient;
    private final BusinessMetricDao businessMetricDao;
    private final ValidationResultDao validationResultDao;
    private final KnowledgeDao knowledgeDao;
    private final FactSourceDao factSourceDao;
    private final ThemeDao themeDao;

    public MaterialService(MaterialDao materialDao,
                           MaterialSlideDao materialSlideDao,
                           FileServiceClient fileServiceClient,
                           BusinessMetricDao businessMetricDao,
                           ValidationResultDao validationResultDao,
                           KnowledgeDao knowledgeDao,
                           FactSourceDao factSourceDao,
                           ThemeDao themeDao) {
        this.materialDao = materialDao;
        this.materialSlideDao = materialSlideDao;
        this.fileServiceClient = fileServiceClient;
        this.businessMetricDao = businessMetricDao;
        this.validationResultDao = validationResultDao;
        this.knowledgeDao = knowledgeDao;
        this.factSourceDao = factSourceDao;
        this.themeDao = themeDao;
    }

    @Override
    public MaterialVO createFromUpload(MultipartFile file, String region, String organization, String reportPeriod, Long taskId, Long themeId) {
        String filePath = fileServiceClient.save(file);
        MaterialVO vo = new MaterialVO();
        vo.setTaskId(taskId);
        vo.setThemeId(themeId);
        vo.setRegion(region);
        vo.setMaterialName(file.getOriginalFilename());
        vo.setMaterialType(FileUtil.extension(file.getOriginalFilename()).toUpperCase());
        vo.setSourceType("UPLOAD");
        vo.setFilePath(filePath);
        vo.setOrganization(organization);
        vo.setReportPeriod(reportPeriod);
        vo.setStatus(MaterialStatus.WAITING.name());
        materialDao.insert(vo);
        return vo;
    }

    @Override
    public MaterialVO get(Long materialId) {
        MaterialVO vo = materialDao.selectById(materialId);
        if (vo == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "材料不存在: " + materialId);
        }
        enrichThemeName(vo);
        return vo;
    }

    @Override
    public List<MaterialVO> list(String status, String organization) {
        List<MaterialVO> list = materialDao.selectList(status, organization);
        list.forEach(this::enrichThemeName);
        return list;
    }

    private void enrichThemeName(MaterialVO vo) {
        if (vo.getThemeId() == null) {
            return;
        }
        ThemeVO theme = themeDao.selectById(vo.getThemeId());
        if (theme != null) {
            vo.setThemeName(theme.getName());
        }
    }

    @Override
    public void updateStatus(Long materialId, String status) {
        updateStatusWithError(materialId, status, null);
    }

    @Override
    public void updateStatusWithError(Long materialId, String status, String errorMessage) {
        materialDao.updateStatus(materialId, status, errorMessage);
    }

    @Override
    public void updateIr(Long materialId, String irJson, BigDecimal confidence) {
        materialDao.updateIr(materialId, irJson, confidence);
    }

    @Override
    public void updateSummary(Long materialId, String summaryText, BigDecimal businessScore) {
        materialDao.updateSummary(materialId, summaryText, businessScore);
    }

    @Override
    public List<MaterialSlideVO> getSlides(Long materialId) {
        return materialSlideDao.selectByMaterialId(materialId);
    }

    @Override
    public Map<String, Object> getIr(Long materialId) {
        MaterialVO vo = get(materialId);
        return JsonUtil.fromJson(vo.getIrJson(), new TypeReference<Map<String, Object>>() {
        });
    }

    @Override
    public void resetForRetry(MaterialVO material) {
        materialSlideDao.deleteByMaterialId(material.getId());
        businessMetricDao.deleteByMaterialId(material.getId());
        validationResultDao.deleteByMaterialId(material.getId());
        knowledgeDao.deleteByMaterialId(material.getId());
        factSourceDao.deleteByMaterialId(material.getId());
    }

    @Override
    public void updateTaskId(MaterialVO material) {
        materialDao.resetForRetry(material.getId(), material.getTaskId(), MaterialStatus.WAITING.name(), null);
    }
}
