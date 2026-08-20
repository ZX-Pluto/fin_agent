package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.huawei.fin.ai.material.analysis.vo.FactSourceVO;

@Mapper
public interface FactSourceDao {

    @Insert("INSERT INTO ai_fact_source (material_id, organization, period, chapter, slide_range, structured_facts, parse_json, status, version) "
            + "VALUES (#{materialId}, #{organization}, #{period}, #{chapter}, #{slideRange}, #{structuredFacts}, #{parseJson}, #{status}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FactSourceVO vo);

    @Select("SELECT * FROM ai_fact_source WHERE material_id = #{materialId} ORDER BY id")
    List<FactSourceVO> selectByMaterialId(Long materialId);

    @Delete("DELETE FROM ai_fact_source WHERE material_id = #{materialId}")
    int deleteByMaterialId(Long materialId);
}
