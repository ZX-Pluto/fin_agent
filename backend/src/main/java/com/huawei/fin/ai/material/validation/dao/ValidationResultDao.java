package com.huawei.fin.ai.material.validation.dao;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.validation.vo.ValidationResultVO;

@Mapper
public interface ValidationResultDao {

    @Insert("INSERT INTO ai_validation_result (task_id, material_id, rule_id, rule_code, category, severity, metric_name, "
            + "actual_value, expected_value, message, suggestion, source_refs, status) "
            + "VALUES (#{taskId}, #{materialId}, #{ruleId}, #{ruleCode}, #{category}, #{severity}, #{metricName}, "
            + "#{actualValue}, #{expectedValue}, #{message}, #{suggestion}, #{sourceRefs}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ValidationResultVO vo);

    @Select("<script>SELECT * FROM ai_validation_result "
            + "<where>"
            + "<if test='materialId != null'>AND material_id = #{materialId}</if>"
            + " <if test='taskId != null'>AND task_id = #{taskId}</if>"
            + " <if test='category != null and category != \"\"'>AND category = #{category}</if>"
            + " <if test='status != null and status != \"\"'>AND status = #{status}</if>"
            + " <if test='severity != null and severity != \"\"'>AND severity = #{severity}</if>"
            + "</where> ORDER BY CASE severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, id DESC</script>")
    List<ValidationResultVO> selectList(@Param("materialId") Long materialId,
                                        @Param("taskId") Long taskId,
                                        @Param("category") String category,
                                        @Param("status") String status,
                                        @Param("severity") String severity);

    @Select("SELECT * FROM ai_validation_result WHERE id = #{id}")
    ValidationResultVO selectById(Long id);

    @Update("UPDATE ai_validation_result SET status=#{status}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM ai_validation_result WHERE material_id = #{materialId}")
    int deleteByMaterialId(Long materialId);
}
