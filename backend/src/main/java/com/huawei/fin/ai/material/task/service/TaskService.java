package com.huawei.fin.ai.material.task.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.huawei.fin.ai.material.common.exception.MaterialErrorCode;
import com.huawei.fin.ai.material.common.exception.MaterialException;
import com.huawei.fin.ai.material.analysis.dao.ThemeDao;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.vo.MaterialStatus;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.task.vo.TaskProgressVO;
import com.huawei.fin.ai.material.task.vo.TaskStatus;
import com.huawei.fin.ai.material.task.vo.TaskVO;

@Service
public class TaskService implements ITaskService {

    private final TaskDao taskDao;
    private final IMaterialService materialService;
    private final ITaskExecuteService taskExecuteService;
    private final TaskStateStore taskStateStore;
    private final ThemeDao themeDao;

    public TaskService(TaskDao taskDao,
                       IMaterialService materialService,
                       ITaskExecuteService taskExecuteService,
                       TaskStateStore taskStateStore,
                       ThemeDao themeDao) {
        this.taskDao = taskDao;
        this.materialService = materialService;
        this.taskExecuteService = taskExecuteService;
        this.taskStateStore = taskStateStore;
        this.themeDao = themeDao;
    }

    @Override
    @Transactional
    public TaskVO createTask(MultipartFile file, String region, String organization, String reportPeriod, Long themeId) {
        requireTheme(themeId);
        TaskVO task = new TaskVO();
        task.setTaskName(buildTaskName(organization, reportPeriod));
        task.setTaskType("MATERIAL_PARSE");
        task.setStatus(TaskStatus.CREATED.name());
        task.setProgress(0);
        task.setCreatorId("system");
        taskDao.insert(task);

        materialService.createFromUpload(file, region, organization, reportPeriod, task.getId(), themeId);
        initProgress(task.getId());
        afterCommit(() -> taskExecuteService.execute(task.getId()));
        return task;
    }

    @Override
    @Transactional
    public TaskVO createTaskFromMaterial(Long materialId) {
        MaterialVO material = materialService.get(materialId);
        if (material == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "材料不存在: " + materialId);
        }
        material.setStatus(MaterialStatus.WAITING.name());
        material.setErrorMessage(null);
        materialService.resetForRetry(material);

        TaskVO task = new TaskVO();
        task.setTaskName(buildTaskName(material.getOrganization(), material.getReportPeriod()) + " 重试");
        task.setTaskType("MATERIAL_PARSE");
        task.setStatus(TaskStatus.CREATED.name());
        task.setProgress(0);
        task.setCreatorId("system");
        taskDao.insert(task);

        material.setTaskId(task.getId());
        materialService.updateTaskId(material);
        initProgress(task.getId());
        afterCommit(() -> taskExecuteService.execute(task.getId()));
        return task;
    }

    @Override
    public List<TaskVO> listTasks() {
        return taskDao.selectAll();
    }

    @Override
    public Map<String, Object> getTaskDetail(Long taskId) {
        TaskVO task = taskDao.selectById(taskId);
        if (task == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "任务不存在: " + taskId);
        }
        TaskProgressVO progress = taskStateStore.get(taskId);
        Map<String, Object> result = new HashMap<>();
        result.put("task", task);
        result.put("progress", progress);
        return result;
    }

    @Override
    public TaskVO cancel(Long taskId) {
        TaskVO task = taskDao.selectById(taskId);
        if (task == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "任务不存在: " + taskId);
        }
        if (TaskStatus.COMPLETED.name().equals(task.getStatus()) || TaskStatus.FAILED.name().equals(task.getStatus())) {
            return task;
        }
        task.setStatus(TaskStatus.CANCELLED.name());
        task.setFinishTime(LocalDateTime.now());
        taskDao.updateState(task);
        taskStateStore.update(taskId, progress -> progress.setStatus(TaskStatus.CANCELLED.name()));
        return task;
    }

    private String buildTaskName(String organization, String reportPeriod) {
        return (organization == null ? "" : organization)
                + (reportPeriod == null ? "" : reportPeriod)
                + "经营材料处理";
    }

    private void requireTheme(Long themeId) {
        if (themeId == null) {
            throw new MaterialException(MaterialErrorCode.PARAM_ERROR, "请选择分析主题");
        }
        if (themeDao.selectById(themeId) == null) {
            throw new MaterialException(MaterialErrorCode.NOT_FOUND, "分析主题不存在: " + themeId);
        }
    }

    private void initProgress(Long taskId) {
        TaskProgressVO progress = new TaskProgressVO();
        progress.setTaskId(taskId);
        progress.setStatus(TaskStatus.CREATED.name());
        progress.setProgress(0);
        progress.setCurrentAgent("TaskService");
        progress.appendLog("任务已创建，等待执行");
        taskStateStore.init(progress);
    }

    private void afterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
