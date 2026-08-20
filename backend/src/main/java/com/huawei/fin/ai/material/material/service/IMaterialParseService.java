package com.huawei.fin.ai.material.material.service;

import com.huawei.fin.ai.material.material.vo.MaterialParseResultVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

public interface IMaterialParseService {

    MaterialParseResultVO parse(MaterialVO material);

    String buildBusinessIr(MaterialVO material, MaterialParseResultVO result);
}
