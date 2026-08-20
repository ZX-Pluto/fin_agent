<template>
  <div class="page" v-loading="loading">
    <div class="workbench-head">
      <div>
        <div class="workbench-title">{{ materialLabel }}</div>
        <div class="muted">{{ material?.materialName }}</div>
      </div>
      <div class="head-actions">
        <StatusTag :status="material?.status || ''" />
        <el-button :loading="retrying" @click="retry">重新处理</el-button>
        <el-button @click="showDetails = !showDetails">处理详情</el-button>
      </div>
    </div>

    <div class="workbench-grid">
      <div class="panel progress-panel">
        <div class="panel-title">处理进度</div>
        <div v-for="step in steps" :key="step.name" class="step-row">
          <span class="step-icon" :class="step.state">{{ stepIcon(step.state) }}</span>
          <span class="step-name" :class="{ active: step.state === 'active' }">{{ step.name }}</span>
        </div>
      </div>

      <div class="panel task-panel">
        <div class="panel-title">当前任务</div>
        <template v-if="currentFindings.length">
          <div class="finding-count">发现 {{ currentFindings.length }} 个需要关注的问题</div>
          <div v-for="(f, i) in currentFindings" :key="i" class="problem-card">
            <div class="problem-head">
              <span class="severity-dot" :class="(f.severity || 'low').toLowerCase()"></span>
              <div class="problem-title">{{ f.subject || f.message }}</div>
              <StatusTag :status="f.severity" />
            </div>
            <div class="problem-message">{{ f.message }}</div>
            <div v-if="f.evidence?.length" class="problem-evidence">证据：{{ f.evidence.join('；') }}</div>
            <div v-if="f.suggestion" class="problem-suggestion">建议：{{ f.suggestion }}</div>
          </div>
        </template>
        <template v-else-if="material?.status === 'COMPLETED'">
          <div class="muted">AI 未发现需要人工处理的问题，分析已完成。</div>
        </template>
        <template v-else>
          <div class="muted">AI 正在自动处理，无需人工介入。</div>
        </template>
      </div>

      <div class="panel evidence-panel">
        <div class="panel-title">证据 / 详情</div>
        <el-tabs v-model="evidenceTab">
          <el-tab-pane label="模型数据" name="model">
            <el-table :data="modelData" border max-height="420" size="small">
              <el-table-column prop="fieldCode" label="字段" min-width="130" />
              <el-table-column prop="fieldValue" label="数值" width="110" />
              <el-table-column prop="unit" label="单位" width="80" />
            </el-table>
            <div v-if="!modelData.length" class="muted">暂无模型数据</div>
          </el-tab-pane>
          <el-tab-pane label="PPT 原文" name="slides">
            <el-table :data="slides" border max-height="420" size="small">
              <el-table-column prop="slideNo" label="页" width="60" />
              <el-table-column prop="title" label="标题" min-width="140" />
              <el-table-column prop="rawText" label="原文" min-width="320" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="AI 依据" name="traces">
            <div v-if="traces.length" class="trace-summary">
              <div v-for="t in traces.slice(-6).reverse()" :key="t.id" class="trace-item">
                <span class="trace-agent">{{ t.agentName }}</span>
                <span class="muted">{{ t.skillName }}</span>
                <StatusTag :status="t.status" />
              </div>
            </div>
            <div v-else class="muted">暂无 AI 依据记录</div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <div v-if="showDetails" class="panel detail-panel">
      <div class="panel-title">处理详情</div>
      <AiTrace :traces="traces" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StatusTag from '../components/common/StatusTag.vue'
import AiTrace from '../components/ai/AiTrace.vue'
import {
  getAnalysis,
  getMaterial,
  getMetrics,
  getModelData,
  getPreAudit,
  getSlides,
  getSummary,
  getTask,
  getTaskTraces,
  listValidations,
  retryMaterial
} from '../api'
import type { AnalysisFinding, AnalysisResult, LlmTrace, Material, MaterialSlide, Metric, ModelData, Summary, TaskProgress, ValidationResult } from '../types'

const route = useRoute()
const router = useRouter()
const materialId = Number(route.params.id)

const loading = ref(false)
const retrying = ref(false)
const showDetails = ref(false)
const evidenceTab = ref('model')
const material = ref<Material>()
const slides = ref<MaterialSlide[]>([])
const metrics = ref<Metric[]>([])
const modelData = ref<ModelData[]>([])
const preAudit = ref<AnalysisResult>()
const analysis = ref<AnalysisResult>()
const findings = ref<ValidationResult[]>([])
const summary = ref<Summary>()
const progress = ref<TaskProgress>()
const traces = ref<LlmTrace[]>([])
let timer: ReturnType<typeof setInterval> | null = null

const materialLabel = computed(() =>
  `${material.value?.region || '默认地区部'} · ${material.value?.organization || '代表处'} · ${material.value?.reportPeriod || '未标注期间'}`
)

const processingStatuses = ['WAITING', 'PARSING', 'VALIDATING', 'EXTRACTING']
const processing = computed(() => material.value ? processingStatuses.includes(material.value.status) : false)

const steps = computed(() => {
  const p = progress.value?.progress ?? material.value?.status === 'COMPLETED' ? 100 : 0
  const defs = [
    { name: '材料解析', at: 40 },
    { name: '材料预审', at: 96 },
    { name: '模型映射', at: 98 },
    { name: '经营分析', at: 99 },
    { name: '分析完成', at: 100 }
  ]
  return defs.map((d) => ({
    name: d.name,
    state: p >= d.at ? 'done' : p >= d.at - 2 ? 'active' : 'todo'
  }))
})

function stepIcon(state: string) {
  if (state === 'done') return '✓'
  if (state === 'active') return '●'
  return '○'
}

const currentFindings = computed<AnalysisFinding[]>(() => {
  const list: AnalysisFinding[] = []
  if (preAudit.value?.resultJson) {
    try {
      const parsed = JSON.parse(preAudit.value.resultJson) as { findings?: AnalysisFinding[] }
      list.push(...(parsed.findings || []))
    } catch {
      // ignore
    }
  }
  if (analysis.value?.resultJson) {
    try {
      const parsed = JSON.parse(analysis.value.resultJson) as { findings?: AnalysisFinding[] }
      list.push(...(parsed.findings || []))
    } catch {
      // ignore
    }
  }
  return list
})

const load = async () => {
  loading.value = true
  try {
    material.value = await getMaterial(materialId)
    slides.value = await getSlides(materialId)
    metrics.value = await getMetrics(materialId)
    summary.value = await getSummary(materialId)
    findings.value = await listValidations({ materialId })
    preAudit.value = (await getPreAudit(materialId))[0]
    analysis.value = (await getAnalysis(materialId))[0]
    modelData.value = await getModelData(materialId)
    if (material.value.taskId) {
      const detail = await getTask(material.value.taskId)
      progress.value = detail.progress
      traces.value = await getTaskTraces(material.value.taskId)
    }
  } finally {
    loading.value = false
  }
}

const retry = async () => {
  retrying.value = true
  try {
    await retryMaterial(materialId)
    await load()
    if (timer) clearInterval(timer)
    timer = setInterval(load, 5000)
  } finally {
    retrying.value = false
  }
}

onMounted(async () => {
  await load()
  if (processing.value) {
    timer = setInterval(load, 5000)
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.workbench-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.workbench-title {
  font-size: 20px;
  font-weight: 700;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.workbench-grid {
  display: grid;
  grid-template-columns: 220px 1fr 1fr;
  gap: 14px;
  align-items: start;
}
.step-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
}
.step-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: var(--muted);
  font-size: 13px;
}
.step-icon.done {
  background: var(--ok);
  color: #fff;
}
.step-icon.active {
  background: var(--brand-weak);
  color: var(--brand);
}
.step-name.active {
  color: var(--brand);
  font-weight: 600;
}
.finding-count {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}
.problem-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.problem-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.problem-title {
  flex: 1;
  min-width: 0;
  font-weight: 600;
}
.problem-message {
  margin-top: 8px;
  line-height: 1.6;
  color: #374151;
}
.problem-evidence {
  margin-top: 8px;
  font-size: 12px;
  color: var(--muted);
}
.problem-suggestion {
  margin-top: 8px;
  background: var(--brand-weak);
  color: #1f4ea5;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
}
.severity-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  flex: 0 0 auto;
  background: var(--muted);
}
.severity-dot.high,
.severity-dot.critical {
  background: var(--danger);
}
.severity-dot.medium {
  background: var(--warning);
}
.severity-dot.low {
  background: #3b82f6;
}
.trace-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.trace-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.trace-agent {
  font-weight: 600;
}
.detail-panel {
  margin-top: 14px;
}

@media (max-width: 1439px) {
  .workbench-grid {
    grid-template-columns: 200px 1fr;
  }
  .evidence-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1023px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }
  .evidence-panel {
    grid-column: auto;
  }
}
</style>
