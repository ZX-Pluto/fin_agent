package com.huawei.fin.ai.material.knowledge.dao;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;

@Mapper
public interface KnowledgeDao {

    @Insert("INSERT INTO ai_knowledge (material_id, task_id, knowledge_type, content, source_refs, confidence) "
            + "VALUES (#{materialId}, #{taskId}, #{knowledgeType}, #{content}, #{sourceRefs}, #{confidence})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeVO vo);

    @Select("<script>SELECT * FROM ai_knowledge WHERE material_id = #{materialId} "
            + "<if test='type != null and type != \"\"'>AND knowledge_type = #{type}</if> "
            + "ORDER BY id</script>")
    List<KnowledgeVO> selectByMaterial(@Param("materialId") Long materialId, @Param("type") String type);

    @Delete("DELETE FROM ai_knowledge WHERE material_id = #{materialId}")
    int deleteByMaterialId(Long materialId);
}
