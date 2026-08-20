package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.huawei.fin.ai.material.analysis.vo.ModelDataVO;

@Mapper
public interface ModelDataDao {

    @Insert("INSERT INTO ai_model_data (material_id, organization, period, model_id, model_version, fact_source_id, field_code, field_value, unit, status) "
            + "VALUES (#{materialId}, #{organization}, #{period}, #{modelId}, #{modelVersion}, #{factSourceId}, #{fieldCode}, #{fieldValue}, #{unit}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelDataVO vo);

    @Select("SELECT * FROM ai_model_data WHERE material_id = #{materialId} ORDER BY id")
    List<ModelDataVO> selectByMaterialId(Long materialId);

    @Delete("DELETE FROM ai_model_data WHERE material_id = #{materialId}")
    int deleteByMaterialId(Long materialId);
}
