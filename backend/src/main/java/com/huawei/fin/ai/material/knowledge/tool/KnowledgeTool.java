package com.huawei.fin.ai.material.knowledge.tool;

import java.util.List;

import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.knowledge.dao.KnowledgeDao;
import com.huawei.fin.ai.material.knowledge.vo.KnowledgeVO;

@Component
public class KnowledgeTool {

    private final KnowledgeDao knowledgeDao;

    public KnowledgeTool(KnowledgeDao knowledgeDao) {
        this.knowledgeDao = knowledgeDao;
    }

    public int saveKnowledge(KnowledgeVO vo) {
        return knowledgeDao.insert(vo);
    }

    public List<KnowledgeVO> queryKnowledge(Long materialId, String type) {
        return knowledgeDao.selectByMaterial(materialId, type);
    }
}
