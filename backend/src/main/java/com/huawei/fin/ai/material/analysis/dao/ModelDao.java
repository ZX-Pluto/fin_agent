package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.analysis.vo.ModelVO;

@Mapper
public interface ModelDao {

    @Select("SELECT * FROM ai_model WHERE theme_id = #{themeId} ORDER BY version DESC")
    List<ModelVO> selectByThemeId(Long themeId);

    @Insert("INSERT INTO ai_model (code, name, theme_id, version, current_version) "
            + "VALUES (#{code}, #{name}, #{themeId}, #{version}, #{currentVersion})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ModelVO vo);

    @Update("UPDATE ai_model SET code=#{code}, name=#{name}, version=#{version}, current_version=#{currentVersion} "
            + "WHERE id=#{id}")
    int update(ModelVO vo);

    @Delete("DELETE FROM ai_model WHERE id = #{id}")
    int delete(Long id);

    @Delete("DELETE FROM ai_model WHERE theme_id = #{themeId}")
    int deleteByThemeId(Long themeId);
}
