package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.huawei.fin.ai.material.analysis.vo.AnalysisResultVO;

@Mapper
public interface AnalysisResultDao {

    @Insert("INSERT INTO ai_analysis_result (material_id, theme_id, package_id, result_type, verdict, result_json, status, version) "
            + "VALUES (#{materialId}, #{themeId}, #{packageId}, #{resultType}, #{verdict}, #{resultJson}, #{status}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalysisResultVO vo);

    @Select("SELECT * FROM ai_analysis_result WHERE material_id = #{materialId} AND result_type = #{resultType} ORDER BY id DESC")
    List<AnalysisResultVO> selectByMaterialAndType(Long materialId, String resultType);

    @Delete("DELETE FROM ai_analysis_result WHERE material_id = #{materialId} AND result_type = #{resultType}")
    int deleteByMaterialAndType(Long materialId, String resultType);
}
