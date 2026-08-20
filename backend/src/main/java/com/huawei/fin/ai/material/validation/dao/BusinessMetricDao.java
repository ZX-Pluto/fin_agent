package com.huawei.fin.ai.material.validation.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.huawei.fin.ai.material.validation.vo.BusinessMetricVO;

@Mapper
public interface BusinessMetricDao {

    @Insert("INSERT INTO ai_business_metric (material_id, task_id, slide_id, metric_name, normalized_name, value, unit, period, source_refs, confidence) "
            + "VALUES (#{materialId}, #{taskId}, #{slideId}, #{metricName}, #{normalizedName}, #{value}, #{unit}, #{period}, #{sourceRefs}, #{confidence})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BusinessMetricVO vo);

    @Select("SELECT * FROM ai_business_metric WHERE material_id = #{materialId} ORDER BY id")
    List<BusinessMetricVO> selectByMaterialId(Long materialId);

    @Delete("DELETE FROM ai_business_metric WHERE material_id = #{materialId}")
    int deleteByMaterialId(Long materialId);
}
