package com.huawei.fin.ai.material.summary.service;

import com.huawei.fin.ai.material.summary.vo.SummaryVO;

public interface ISummaryService {

    SummaryVO build(Long materialId);

    SummaryVO generateAndSave(Long materialId);
}
