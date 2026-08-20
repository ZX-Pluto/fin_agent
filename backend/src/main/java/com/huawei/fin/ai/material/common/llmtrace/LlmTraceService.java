package com.huawei.fin.ai.material.common.llmtrace;

import java.util.List;

import org.springframework.stereotype.Service;

import com.huawei.fin.ai.material.common.client.LlmCallResult;

@Service
public class LlmTraceService implements ILlmTraceService {

    private final LlmTraceDao llmTraceDao;

    public LlmTraceService(LlmTraceDao llmTraceDao) {
        this.llmTraceDao = llmTraceDao;
    }

    @Override
    public void record(Long taskId, Long materialId, String agentName, String skillName,
                       String modelName, String provider, String prompt, LlmCallResult result) {
        LlmTraceVO vo = new LlmTraceVO();
        vo.setTaskId(taskId);
        vo.setMaterialId(materialId);
        vo.setAgentName(agentName);
        vo.setSkillName(skillName);
        vo.setModelName(modelName);
        vo.setProvider(provider);
        vo.setPrompt(prompt);
        vo.setResponse(result.content());
        vo.setInputTokens(result.inputTokens());
        vo.setOutputTokens(result.outputTokens());
        vo.setLatencyMs(result.latencyMs());
        vo.setStatus(result.success() ? "SUCCESS" : "FAILED");
        vo.setErrorMessage(result.success() ? null : result.message());
        llmTraceDao.insert(vo);
    }

    @Override
    public void recordSkipped(Long taskId, Long materialId, String agentName, String skillName, String message) {
        LlmTraceVO vo = new LlmTraceVO();
        vo.setTaskId(taskId);
        vo.setMaterialId(materialId);
        vo.setAgentName(agentName);
        vo.setSkillName(skillName);
        vo.setStatus("SKIPPED");
        vo.setErrorMessage(message);
        llmTraceDao.insert(vo);
    }

    @Override
    public List<LlmTraceVO> listByMaterial(Long materialId) {
        return llmTraceDao.selectByMaterialId(materialId);
    }

    @Override
    public List<LlmTraceVO> listByTask(Long taskId) {
        return llmTraceDao.selectByTaskId(taskId);
    }
}
