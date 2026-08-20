package com.huawei.fin.ai.material.material.facade;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.vo.MaterialSlideVO;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.task.service.ITaskService;
import com.huawei.fin.ai.material.task.vo.TaskVO;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final IMaterialService materialService;
    private final ITaskService taskService;

    public MaterialController(IMaterialService materialService, ITaskService taskService) {
        this.materialService = materialService;
        this.taskService = taskService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "region", required = false) String region,
                                      @RequestParam(value = "organization", required = false) String organization,
                                      @RequestParam(value = "reportPeriod", required = false) String reportPeriod,
                                      @RequestParam(value = "themeId", required = false) Long themeId) {
        TaskVO task = taskService.createTask(file, region, organization, reportPeriod, themeId);
        return Map.of("taskId", task.getId(), "task", task);
    }

    @GetMapping
    public List<MaterialVO> list(@RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "organization", required = false) String organization) {
        return materialService.list(status, organization);
    }

    @GetMapping("/{materialId}")
    public MaterialVO detail(@PathVariable Long materialId) {
        return materialService.get(materialId);
    }

    @GetMapping("/{materialId}/slides")
    public List<MaterialSlideVO> slides(@PathVariable Long materialId) {
        return materialService.getSlides(materialId);
    }

    @GetMapping("/{materialId}/ir")
    public Map<String, Object> ir(@PathVariable Long materialId) {
        return materialService.getIr(materialId);
    }

    @PostMapping("/{materialId}/retry")
    public TaskVO retry(@PathVariable Long materialId) {
        return taskService.createTaskFromMaterial(materialId);
    }
}
