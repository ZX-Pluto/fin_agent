package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.analysis.vo.RulePackageVO;

@Mapper
public interface RulePackageDao {

    @Select("SELECT * FROM ai_rule_package WHERE id = #{id}")
    RulePackageVO selectById(Long id);

    @Select("SELECT * FROM ai_rule_package ORDER BY theme_id, id")
    List<RulePackageVO> selectAll();

    @Select("SELECT * FROM ai_rule_package WHERE theme_id = #{themeId} ORDER BY id")
    List<RulePackageVO> selectByThemeId(Long themeId);

    @Insert("INSERT INTO ai_rule_package (code, name, theme_id, package_type, description, enabled) "
            + "VALUES (#{code}, #{name}, #{themeId}, #{packageType}, #{description}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RulePackageVO vo);

    @Update("UPDATE ai_rule_package SET code=#{code}, name=#{name}, theme_id=#{themeId}, "
            + "package_type=#{packageType}, description=#{description}, enabled=#{enabled} WHERE id=#{id}")
    int update(RulePackageVO vo);

    @Delete("DELETE FROM ai_rule_package WHERE id = #{id}")
    int delete(Long id);
}
