package com.huawei.fin.ai.material.analysis.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.analysis.vo.RuleItemVO;

@Mapper
public interface RuleItemDao {

    @Select("SELECT * FROM ai_rule_item WHERE id = #{id}")
    RuleItemVO selectById(Long id);

    @Select("SELECT * FROM ai_rule_item WHERE package_id = #{packageId} ORDER BY id")
    List<RuleItemVO> selectByPackageId(Long packageId);

    @Insert("INSERT INTO ai_rule_item (package_id, rule_code, name, rule_type, scope, input_fields, "
            + "execution_strategy, description, severity, enabled) "
            + "VALUES (#{packageId}, #{ruleCode}, #{name}, #{ruleType}, #{scope}, #{inputFields}, "
            + "#{executionStrategy}, #{description}, #{severity}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RuleItemVO vo);

    @Update("UPDATE ai_rule_item SET rule_code=#{ruleCode}, name=#{name}, rule_type=#{ruleType}, "
            + "scope=#{scope}, input_fields=#{inputFields}, execution_strategy=#{executionStrategy}, "
            + "description=#{description}, severity=#{severity}, enabled=#{enabled} WHERE id=#{id}")
    int update(RuleItemVO vo);

    @Delete("DELETE FROM ai_rule_item WHERE id = #{id}")
    int delete(Long id);

    @Delete("DELETE FROM ai_rule_item WHERE package_id = #{packageId}")
    int deleteByPackageId(Long packageId);
}
