package com.huawei.fin.ai.material.task.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.task.vo.TaskProgressVO;

@Component
public class TaskStateStore {

    private final ConcurrentMap<Long, TaskProgressVO> store = new ConcurrentHashMap<>();

    public void init(TaskProgressVO vo) {
        store.put(vo.getTaskId(), vo);
    }

    public TaskProgressVO get(Long taskId) {
        return store.get(taskId);
    }

    public void update(Long taskId, Consumer<TaskProgressVO> updater) {
        TaskProgressVO vo = store.computeIfAbsent(taskId, key -> {
            TaskProgressVO created = new TaskProgressVO();
            created.setTaskId(key);
            return created;
        });
        updater.accept(vo);
    }
}
