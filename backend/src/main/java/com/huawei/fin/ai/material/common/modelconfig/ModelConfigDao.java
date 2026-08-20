package com.huawei.fin.ai.material.common.modelconfig;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ModelConfigDao {

    @Select("SELECT * FROM ai_model_config ORDER BY id DESC")
    List<ModelConfigVO> selectAll();

    @Select("SELECT * FROM ai_model_config WHERE id = #{id}")
    ModelConfigVO selectById(Long id);

    @Select("SELECT * FROM ai_model_config WHERE enabled = TRUE AND capabilities LIKE '%' || #{capability} || '%' ORDER BY id LIMIT 1")
    ModelConfigVO selectEnabledByCapability(String capability);

    @Insert("INSERT INTO ai_model_config (name, provider, base_url, api_key, model_name, capabilities, temperature, timeout_seconds, enabled) "
            + "VALUES (#{name}, #{provider}, #{baseUrl}, #{apiKey}, #{modelName}, #{capabilities}, #{temperature}, #{timeoutSeconds}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelConfigVO vo);

    @Update("UPDATE ai_model_config SET name=#{name}, provider=#{provider}, base_url=#{baseUrl}, api_key=#{apiKey}, "
            + "model_name=#{modelName}, capabilities=#{capabilities}, temperature=#{temperature}, timeout_seconds=#{timeoutSeconds}, "
            + "enabled=#{enabled}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(ModelConfigVO vo);

    @Delete("DELETE FROM ai_model_config WHERE id=#{id}")
    int delete(Long id);
}
