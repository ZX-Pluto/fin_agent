package com.huawei.fin.ai.material.material.service;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Mapper
public interface MaterialDao {

    @Insert("INSERT INTO ai_material (task_id, theme_id, region, material_name, material_type, source_type, source_url, file_path, organization, report_period, status) "
            + "VALUES (#{taskId}, #{themeId}, #{region}, #{materialName}, #{materialType}, #{sourceType}, #{sourceUrl}, #{filePath}, #{organization}, #{reportPeriod}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MaterialVO vo);

    @Select("SELECT * FROM ai_material WHERE id = #{id}")
    MaterialVO selectById(Long id);

    @Select("SELECT * FROM ai_material WHERE task_id = #{taskId} LIMIT 1")
    MaterialVO selectByTaskId(Long taskId);

    @Select("SELECT COUNT(*) FROM ai_material WHERE theme_id = #{themeId}")
    long countByThemeId(Long themeId);

    @Select("<script>SELECT * FROM ai_material "
            + "<where>"
            + "<if test='status != null and status != \"\"'>AND status = #{status}</if>"
            + " <if test='organization != null and organization != \"\"'>AND organization = #{organization}</if>"
            + "</where> ORDER BY id DESC LIMIT 200</script>")
    List<MaterialVO> selectList(@Param("status") String status, @Param("organization") String organization);

    @Update("UPDATE ai_material SET status=#{status}, error_message=#{errorMessage}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("errorMessage") String errorMessage);

    @Update("UPDATE ai_material SET ir_json=#{irJson}, confidence=#{confidence}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateIr(@Param("id") Long id, @Param("irJson") String irJson, @Param("confidence") java.math.BigDecimal confidence);

    @Update("UPDATE ai_material SET summary_text=#{summaryText}, business_score=#{businessScore}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateSummary(@Param("id") Long id,
                      @Param("summaryText") String summaryText,
                      @Param("businessScore") java.math.BigDecimal businessScore);

    @Update("UPDATE ai_material SET task_id=#{taskId}, status=#{status}, error_message=#{errorMessage}, ir_json=NULL, confidence=NULL, "
            + "summary_text=NULL, business_score=NULL, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int resetForRetry(@Param("id") Long id, @Param("taskId") Long taskId, @Param("status") String status, @Param("errorMessage") String errorMessage);
}
