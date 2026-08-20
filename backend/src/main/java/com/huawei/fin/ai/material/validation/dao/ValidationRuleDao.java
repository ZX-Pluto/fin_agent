package com.huawei.fin.ai.material.validation.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.validation.vo.ValidationRuleVO;

@Mapper
public interface ValidationRuleDao {

    @Select("SELECT * FROM ai_rule_config ORDER BY category, rule_code")
    List<ValidationRuleVO> selectAll();

    @Select("SELECT * FROM ai_rule_config WHERE enabled = TRUE ORDER BY category, rule_code")
    List<ValidationRuleVO> selectEnabled();

    @Select("SELECT * FROM ai_rule_config WHERE id = #{id}")
    ValidationRuleVO selectById(Long id);

    @Select("SELECT * FROM ai_rule_config WHERE rule_code = #{ruleCode}")
    ValidationRuleVO selectByCode(String ruleCode);

    @Insert("INSERT INTO ai_rule_config (rule_code, name, category, description, severity, params, builtin, enabled) "
            + "VALUES (#{ruleCode}, #{name}, #{category}, #{description}, #{severity}, #{params}, #{builtin}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ValidationRuleVO vo);

    @Update("UPDATE ai_rule_config SET rule_code=#{ruleCode}, name=#{name}, category=#{category}, description=#{description}, "
            + "severity=#{severity}, params=#{params}, builtin=#{builtin}, enabled=#{enabled}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(ValidationRuleVO vo);

    @Delete("DELETE FROM ai_rule_config WHERE id=#{id}")
    int delete(Long id);
}
