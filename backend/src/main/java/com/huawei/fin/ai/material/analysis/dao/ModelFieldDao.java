package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.huawei.fin.ai.material.analysis.vo.ModelFieldVO;

@Mapper
public interface ModelFieldDao {

    @Select("SELECT * FROM ai_model_field WHERE model_id = #{modelId} ORDER BY seq_no")
    List<ModelFieldVO> selectByModelId(Long modelId);

    @Insert("INSERT INTO ai_model_field (model_id, field_code, field_name, field_type, unit, comment, seq_no) "
            + "VALUES (#{modelId}, #{fieldCode}, #{fieldName}, #{fieldType}, #{unit}, #{comment}, #{seqNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelFieldVO vo);

    @Delete("DELETE FROM ai_model_field WHERE model_id = #{modelId}")
    int deleteByModelId(Long modelId);
}
