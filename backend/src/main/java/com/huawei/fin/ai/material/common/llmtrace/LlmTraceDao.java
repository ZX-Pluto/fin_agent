package com.huawei.fin.ai.material.common.llmtrace;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LlmTraceDao {

    @Insert("INSERT INTO ai_llm_trace (task_id, material_id, agent_name, skill_name, model_name, provider, prompt, response, "
            + "input_tokens, output_tokens, latency_ms, status, error_message) "
            + "VALUES (#{taskId}, #{materialId}, #{agentName}, #{skillName}, #{modelName}, #{provider}, #{prompt}, #{response}, "
            + "#{inputTokens}, #{outputTokens}, #{latencyMs}, #{status}, #{errorMessage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LlmTraceVO vo);

    @Select("SELECT * FROM ai_llm_trace WHERE material_id = #{materialId} ORDER BY id")
    List<LlmTraceVO> selectByMaterialId(Long materialId);

    @Select("SELECT * FROM ai_llm_trace WHERE task_id = #{taskId} ORDER BY id")
    List<LlmTraceVO> selectByTaskId(Long taskId);
}
