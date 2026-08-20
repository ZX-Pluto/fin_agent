package com.huawei.fin.ai.material.task.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.huawei.fin.ai.material.task.vo.TaskVO;

public interface ITaskService {

    TaskVO createTask(MultipartFile file, String region, String organization, String reportPeriod, Long themeId);

    TaskVO createTaskFromMaterial(Long materialId);

    List<TaskVO> listTasks();

    Map<String, Object> getTaskDetail(Long taskId);

    TaskVO cancel(Long taskId);
}
