package com.huawei.fin.ai.material.followup.dao;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.followup.vo.FollowUpVO;

@Mapper
public interface FollowUpDao {

    @Insert("INSERT INTO ai_follow_up (material_id, title, message, suggestion, source_label, status) "
            + "VALUES (#{materialId}, #{title}, #{message}, #{suggestion}, #{sourceLabel}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FollowUpVO vo);

    @Select("SELECT * FROM ai_follow_up ORDER BY create_time DESC")
    List<FollowUpVO> selectAll();

    @Select("SELECT * FROM ai_follow_up WHERE material_id = #{materialId} AND title = #{title} LIMIT 1")
    FollowUpVO selectByMaterialAndTitle(@Param("materialId") Long materialId, @Param("title") String title);

    @Update("UPDATE ai_follow_up SET status=#{status}, update_time=CURRENT_TIMESTAMP WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
