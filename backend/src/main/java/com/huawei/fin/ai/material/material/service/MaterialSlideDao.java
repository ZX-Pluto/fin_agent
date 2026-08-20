package com.huawei.fin.ai.material.material.service;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;

@Mapper
public interface MaterialSlideDao {

    @Insert("INSERT INTO ai_material_slide (material_id, slide_no, title, raw_text, structured_content, parse_status) "
            + "VALUES (#{materialId}, #{slideNo}, #{title}, #{rawText}, #{structuredContent}, #{parseStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MaterialSlideVO vo);

    @Select("SELECT * FROM ai_material_slide WHERE material_id = #{materialId} ORDER BY slide_no")
    List<MaterialSlideVO> selectByMaterialId(Long materialId);

    @Delete("DELETE FROM ai_material_slide WHERE material_id = #{materialId}")
    int deleteByMaterialId(Long materialId);
}
