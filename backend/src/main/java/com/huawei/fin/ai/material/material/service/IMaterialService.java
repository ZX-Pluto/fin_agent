package com.huawei.fin.ai.material.material.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

public interface IMaterialService {

    MaterialVO createFromUpload(MultipartFile file, String region, String organization, String reportPeriod, Long taskId, Long themeId);

    MaterialVO get(Long materialId);

    List<MaterialVO> list(String status, String organization);

    void updateStatus(Long materialId, String status);

    void updateStatusWithError(Long materialId, String status, String errorMessage);

    void updateIr(Long materialId, String irJson, BigDecimal confidence);

    void updateSummary(Long materialId, String summaryText, BigDecimal businessScore);

    List<MaterialSlideVO> getSlides(Long materialId);

    Map<String, Object> getIr(Long materialId);

    void resetForRetry(MaterialVO material);

    void updateTaskId(MaterialVO material);
}
