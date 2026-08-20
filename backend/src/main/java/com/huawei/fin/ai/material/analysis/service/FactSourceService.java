package com.huawei.fin.ai.material.analysis.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huawei.fin.ai.material.analysis.dao.FactSourceDao;
import com.huawei.fin.ai.material.analysis.vo.ChapterVO;
import com.huawei.fin.ai.material.analysis.vo.FactSourceVO;
import com.huawei.fin.ai.material.common.util.JsonUtil;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.service.MaterialSlideDao;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;

@Service
public class FactSourceService {

    private final FactSourceDao factSourceDao;
    private final IMaterialService materialService;
    private final MaterialSlideDao materialSlideDao;
    private final ChapterSplitter chapterSplitter;

    public FactSourceService(FactSourceDao factSourceDao,
                             IMaterialService materialService,
                             MaterialSlideDao materialSlideDao,
                             ChapterSplitter chapterSplitter) {
        this.factSourceDao = factSourceDao;
        this.materialService = materialService;
        this.materialSlideDao = materialSlideDao;
        this.chapterSplitter = chapterSplitter;
    }

    @Transactional
    public List<FactSourceVO> generate(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        factSourceDao.deleteByMaterialId(materialId);
        List<MaterialSlideVO> slides = materialSlideDao.selectByMaterialId(materialId);
        List<ChapterVO> chapters = chapterSplitter.split(material, slides);
        List<FactSourceVO> result = new ArrayList<>();
        for (ChapterVO chapter : chapters) {
            FactSourceVO vo = new FactSourceVO();
            vo.setMaterialId(materialId);
            vo.setOrganization(material.getOrganization());
            vo.setPeriod(material.getReportPeriod());
            vo.setChapter(chapter.getName());
            vo.setSlideRange(JsonUtil.toJson(chapter.getSlideNos()));
            vo.setStructuredFacts("[]");
            List<Map<String, Object>> pageJson = new ArrayList<>();
            for (MaterialSlideVO s : slides) {
                if (chapter.getSlideNos().contains(s.getSlideNo())) {
                    Map<String, Object> page = new LinkedHashMap<>();
                    page.put("slideNo", s.getSlideNo());
                    page.put("title", s.getTitle());
                    page.put("rawText", s.getRawText());
                    pageJson.add(page);
                }
            }
            vo.setParseJson(JsonUtil.toJson(pageJson));
            vo.setStatus("VALID");
            vo.setVersion(1);
            factSourceDao.insert(vo);
            result.add(vo);
        }
        return result;
    }

    public List<FactSourceVO> listByMaterial(Long materialId) {
        return factSourceDao.selectByMaterialId(materialId);
    }
}
