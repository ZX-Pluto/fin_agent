package com.huawei.fin.ai.material.task.facade;

import java.io.IOException;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.huawei.fin.ai.material.common.sse.SseManager;
import com.huawei.fin.ai.material.task.service.ITaskService;
import com.huawei.fin.ai.material.task.vo.TaskVO;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ITaskService taskService;
    private final SseManager sseManager;

    public TaskController(ITaskService taskService, SseManager sseManager) {
        this.taskService = taskService;
        this.sseManager = sseManager;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaskVO createTask(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "region", required = false) String region,
                             @RequestParam(value = "organization", required = false) String organization,
                             @RequestParam(value = "reportPeriod", required = false) String reportPeriod,
                             @RequestParam(value = "themeId", required = false) Long themeId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的材料文件");
        }
        return taskService.createTask(file, region, organization, reportPeriod, themeId);
    }

    @GetMapping
    public List<TaskVO> list() {
        return taskService.listTasks();
    }

    @GetMapping("/{taskId}")
    public Map<String, Object> detail(@PathVariable Long taskId) {
        return taskService.getTaskDetail(taskId);
    }

    @GetMapping(value = "/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable Long taskId) {
        return sseManager.register(taskId);
    }

    @PostMapping("/{taskId}/cancel")
    public TaskVO cancel(@PathVariable Long taskId) {
        return taskService.cancel(taskId);
    }
}
