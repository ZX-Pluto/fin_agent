package com.huawei.fin.ai.material.agent;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.huawei.fin.ai.material.common.sse.SseManager;
import com.huawei.fin.ai.material.knowledge.agent.KnowledgeExtractionAgent;
import com.huawei.fin.ai.material.analysis.service.BusinessAnalysisService;
import com.huawei.fin.ai.material.analysis.service.FactMappingService;
import com.huawei.fin.ai.material.analysis.service.PreAuditService;
import com.huawei.fin.ai.material.material.agent.MaterialParsingAgent;
import com.huawei.fin.ai.material.material.service.IMaterialService;
import com.huawei.fin.ai.material.material.service.MaterialDao;
import com.huawei.fin.ai.material.material.vo.MaterialStatus;
import com.huawei.fin.ai.material.material.vo.MaterialVO;
import com.huawei.fin.ai.material.summary.agent.ConsolidationAgent;
import com.huawei.fin.ai.material.task.service.TaskDao;
import com.huawei.fin.ai.material.task.service.TaskStateStore;
import com.huawei.fin.ai.material.task.vo.TaskEventVO;
import com.huawei.fin.ai.material.task.vo.TaskStatus;
import com.huawei.fin.ai.material.task.vo.TaskVO;
import com.huawei.fin.ai.material.validation.agent.DataValidationAgent;

@Component
public class MaterialAgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MaterialAgentOrchestrator.class);

    private final TaskDao taskDao;
    private final MaterialDao materialDao;
    private final TaskStateStore taskStateStore;
    private final SseManager sseManager;
    private final IMaterialService materialService;
    private final MaterialParsingAgent parsingAgent;
    private final DataValidationAgent validationAgent;
    private final KnowledgeExtractionAgent knowledgeAgent;
    private final ConsolidationAgent consolidationAgent;
    private final PreAuditService preAuditService;
    private final FactMappingService factMappingService;
    private final BusinessAnalysisService businessAnalysisService;

    public MaterialAgentOrchestrator(TaskDao taskDao,
                                     MaterialDao materialDao,
                                     TaskStateStore taskStateStore,
                                     SseManager sseManager,
                                     IMaterialService materialService,
                                     MaterialParsingAgent parsingAgent,
                                     DataValidationAgent validationAgent,
                                     KnowledgeExtractionAgent knowledgeAgent,
                                     ConsolidationAgent consolidationAgent,
                                     PreAuditService preAuditService,
                                     FactMappingService factMappingService,
                                     BusinessAnalysisService businessAnalysisService) {
        this.taskDao = taskDao;
        this.materialDao = materialDao;
        this.taskStateStore = taskStateStore;
        this.sseManager = sseManager;
        this.materialService = materialService;
        this.parsingAgent = parsingAgent;
        this.validationAgent = validationAgent;
        this.knowledgeAgent = knowledgeAgent;
        this.consolidationAgent = consolidationAgent;
        this.preAuditService = preAuditService;
        this.factMappingService = factMappingService;
        this.businessAnalysisService = businessAnalysisService;
    }

    public void execute(Long taskId) {
        TaskVO task = taskDao.selectById(taskId);
        MaterialVO material = materialDao.selectByTaskId(taskId);
        if (task == null || material == null) {
            log.warn("Task or material not found: taskId={}", taskId);
            return;
        }
        try {
            update(task, TaskStatus.PARSING, 10, "MaterialParsingAgent", "开始解析材料");
            parsingAgent.execute(material.getId());
            if (cancelled(task)) {
                return;
            }
            update(task, TaskStatus.PARSED, 40, "MaterialParsingAgent", "材料解析完成，生成 Business IR");

            update(task, TaskStatus.VALIDATING, 55, "DataValidationAgent", "开始数据校验");
            validationAgent.execute(material.getId());
            if (cancelled(task)) {
                return;
            }
            update(task, TaskStatus.VALIDATED, 75, "DataValidationAgent", "数据校验完成");

            update(task, TaskStatus.EXTRACTING, 85, "KnowledgeExtractionAgent", "开始知识提取");
            knowledgeAgent.execute(material.getId());
            if (cancelled(task)) {
                return;
            }
            update(task, TaskStatus.EXTRACTED, 95, "KnowledgeExtractionAgent", "知识提取完成");

            update(task, TaskStatus.EXTRACTED, 96, "PreAuditAgent", "开始材料预审");
            Long themeId = material.getThemeId();
            if (themeId == null) {
                throw new IllegalStateException("材料未选择分析主题，无法执行预审");
            }
            runAnalysisAgents(material.getId(), themeId, task);

            consolidationAgent.execute(material.getId());
            materialService.updateStatus(material.getId(), MaterialStatus.COMPLETED.name());
            update(task, TaskStatus.COMPLETED, 100, "ConsolidationAgent", "处理完成");
        } catch (Exception e) {
            log.error("Material task failed: taskId={}", taskId, e);
            materialService.updateStatusWithError(material.getId(), MaterialStatus.FAILED.name(), e.getMessage());
            task.setStatus(TaskStatus.FAILED.name());
            task.setProgress(100);
            task.setErrorMessage(e.getMessage());
            task.setFinishTime(LocalDateTime.now());
            taskDao.updateState(task);
            taskStateStore.update(taskId, p -> {
                p.setStatus(TaskStatus.FAILED.name());
                p.setProgress(100);
                p.setCurrentAgent("MaterialAgentOrchestrator");
                p.appendLog("处理失败: " + e.getMessage());
            });
            sseManager.send(taskId, event(task, TaskStatus.FAILED, 100, "MaterialAgentOrchestrator", "处理失败: " + e.getMessage()));
            sseManager.complete(taskId);
        }
    }

    private boolean cancelled(TaskVO task) {
        TaskVO latest = taskDao.selectById(task.getId());
        return latest != null && TaskStatus.CANCELLED.name().equals(latest.getStatus());
    }

    private void update(TaskVO task, TaskStatus status, int progress, String agent, String message) {
        task.setStatus(status.name());
        task.setProgress(progress);
        task.setCurrentAgent(agent);
        task.setErrorMessage(null);
        task.setStartTime(task.getStartTime() == null ? LocalDateTime.now() : task.getStartTime());
        task.setFinishTime(status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ? LocalDateTime.now() : null);
        taskDao.updateState(task);
        taskStateStore.update(task.getId(), p -> {
            p.setStatus(status.name());
            p.setProgress(progress);
            p.setCurrentAgent(agent);
            p.appendLog(message);
        });
        sseManager.send(task.getId(), event(task, status, progress, agent, message));
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
            sseManager.complete(task.getId());
        }
    }

    private TaskEventVO event(TaskVO task, TaskStatus status, int progress, String agent, String message) {
        return new TaskEventVO(task.getId(), "PROGRESS", status.name(), progress, agent, message, LocalDateTime.now());
    }

    private void runAnalysisAgents(Long materialId, Long themeId, TaskVO task) {
        try {
            preAuditService.run(materialId, themeId);
            taskStateStore.update(task.getId(), p -> {
                p.setStatus(TaskStatus.EXTRACTED.name());
                p.setProgress(96);
                p.setCurrentAgent("PreAuditAgent");
                p.appendLog("材料预审完成");
            });
            update(task, TaskStatus.EXTRACTED, 97, "PreAuditAgent", "材料预审完成");
        } catch (Exception e) {
            log.warn("Pre-audit failed for material {}: {}", materialId, e.getMessage());
            taskStateStore.update(task.getId(), p -> {
                p.setStatus(TaskStatus.EXTRACTED.name());
                p.setProgress(96);
                p.setCurrentAgent("PreAuditAgent");
                p.appendLog("材料预审失败: " + e.getMessage());
            });
        }
        try {
            factMappingService.map(materialId, themeId);
            update(task, TaskStatus.EXTRACTED, 98, "FactMappingAgent", "事实映射完成");
        } catch (Exception e) {
            log.error("Fact mapping failed for material {}: {}", materialId, e.getMessage(), e);
            throw new IllegalStateException("事实映射失败: " + e.getMessage(), e);
        }
        try {
            businessAnalysisService.run(materialId, themeId);
            update(task, TaskStatus.EXTRACTED, 99, "BusinessAnalysisAgent", "经营分析完成");
        } catch (Exception e) {
            log.error("Business analysis failed for material {}: {}", materialId, e.getMessage(), e);
            throw new IllegalStateException("经营分析失败: " + e.getMessage(), e);
        }
    }
}
