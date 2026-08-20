package com.huawei.fin.ai.material.task.service;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.huawei.fin.ai.material.task.vo.TaskVO;

@Mapper
public interface TaskDao {

    @Insert("INSERT INTO ai_material_task (task_name, task_type, status, progress, creator_id, params_json) "
            + "VALUES (#{taskName}, #{taskType}, #{status}, #{progress}, #{creatorId}, #{paramsJson})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskVO vo);

    @Select("SELECT * FROM ai_material_task WHERE id = #{id}")
    TaskVO selectById(Long id);

    @Select("SELECT * FROM ai_material_task ORDER BY id DESC LIMIT 200")
    List<TaskVO> selectAll();

    @Update("UPDATE ai_material_task SET status=#{status}, progress=#{progress}, current_agent=#{currentAgent}, "
            + "error_message=#{errorMessage}, finish_time=#{finishTime}, start_time=COALESCE(start_time, #{startTime}) WHERE id=#{id}")
    int updateState(TaskVO vo);
}
