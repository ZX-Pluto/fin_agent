package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.analysis.vo.ThemeVO;

@Mapper
public interface ThemeDao {

    @Select("SELECT id, code, name, description FROM ai_theme ORDER BY id")
    List<ThemeVO> selectAll();

    @Select("SELECT id, code, name, description FROM ai_theme WHERE id = #{id}")
    ThemeVO selectById(Long id);

    @Insert("INSERT INTO ai_theme (code, name, description) VALUES (#{code}, #{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ThemeVO vo);

    @Update("UPDATE ai_theme SET code=#{code}, name=#{name}, description=#{description} WHERE id=#{id}")
    int update(ThemeVO vo);

    @Delete("DELETE FROM ai_theme WHERE id = #{id}")
    int delete(Long id);
}
