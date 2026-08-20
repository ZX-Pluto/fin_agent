package com.huawei.fin.ai.material.common.llmtrace;

import java.util.List;

import com.huawei.fin.ai.material.common.client.LlmCallResult;

public interface ILlmTraceService {

    void record(Long taskId, Long materialId, String agentName, String skillName,
                String modelName, String provider, String prompt, LlmCallResult result);

    void recordSkipped(Long taskId, Long materialId, String agentName, String skillName, String message);

    List<LlmTraceVO> listByMaterial(Long materialId);

    List<LlmTraceVO> listByTask(Long taskId);
}
