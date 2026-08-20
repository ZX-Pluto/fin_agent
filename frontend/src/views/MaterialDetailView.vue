<template>
  <div class="page" v-loading="loading">
    <div class="detail-head">
      <el-page-header :content="`${material?.organization || '材料'} · ${material?.reportPeriod || '未标注期间'} · ${material?.themeName || '未选主题'}`" @back="$router.push('/materials')" />
      <div class="head-actions">
        <el-button :loading="retrying" @click="retry">重新处理</el-button>
      </div>
    </div>

    <div v-if="processing" class="panel process-panel">
      <div class="panel-title">AI 正在处理这份材料</div>
      <AiProcess :progress="progress" />
      <div class="trace-entry">
        <AiTrace :traces="traces" />
      </div>
    </div>

    <div class="stat-grid">
      <StatCard label="可信度" :value="confidenceText" />
      <StatCard label="经营指标" :value="summary?.metricCount ?? '-'" />
      <StatCard label="风险问题" :value="summary?.findingCount ?? '-'" tone="danger" />
      <StatCard label="经营要点" :value="insightTotal" tone="ok" />
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">AI 预审结论</div>
        <StatusTag v-if="preAuditVerdict" :status="preAuditVerdict" />
      </div>
      <p class="summary-text">{{ previewConclusion }}</p>
      <div v-if="preAuditFindings.length" class="preaudit-list">
        <div v-for="(f, index) in preAuditFindings.slice(0, 5)" :key="index" class="preaudit-item">
          <span class="preaudit-dot" :class="f.severity.toLowerCase()"></span>
          <div class="preaudit-body">
            <div class="preaudit-message">{{ f.message }}</div>
            <div class="muted" v-if="f.ruleId || f.sourceIds?.length">规则：{{ f.ruleId }} · 来源：{{ (f.sourceIds || []).join('、') }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">模型数据</div>
      <el-table v-if="modelData.length" :data="modelData" border max-height="320">
        <el-table-column prop="fieldCode" label="字段" min-width="170" />
        <el-table-column prop="fieldValue" label="数值" width="130" />
        <el-table-column prop="unit" label="单位" width="90" />
        <el-table-column prop="modelVersion" label="模型版本" width="100" />
      </el-table>
      <div v-else class="muted">暂无模型数据，可到 AI 预审中心执行模型映射。</div>
    </div>

    <div class="panel">
      <div class="panel-title">AI 经营摘要</div>
      <p v-if="analysisSummaryText" class="summary-text">{{ analysisSummaryText }}</p>
      <p v-if="summary?.summaryText" class="summary-text">{{ summary.summaryText }}</p>
      <EmptyState v-if="!analysisSummaryText && !summary?.summaryText" title="AI 经营摘要尚未生成" description="材料完成 AI 处理后，将在这里展示经营摘要。" />
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">需要关注的问题</div>
        <el-button v-if="uniqueFindings.length" link type="primary" @click="$router.push({ path: '/validations', query: { materialId } })">
          查看全部 {{ uniqueFindings.length }} 个问题
        </el-button>
      </div>
      <div v-if="findings.length" class="finding-list">
        <ValidationCard
          v-for="f in uniqueFindings.slice(0, 6)"
          :key="f.id"
          :finding="f"
          :org-text="material?.organization"
          :period-text="material?.reportPeriod"
          @evidence="showEvidence"
          @confirm="confirmFinding"
          @ignore="ignoreFinding"
        />
      </div>
      <EmptyState v-else title="暂无预审问题" description="当前材料未发现需要关注的数据或内容异常。" />
    </div>

    <div class="panel">
      <div class="panel-title">经营洞察</div>
      <div class="insight-cols">
        <div class="insight-col">
          <div class="col-title ok">经营亮点</div>
          <InsightCard
            v-for="h in highlights"
            :key="h.id"
            type="HIGHLIGHT"
            :content="h.content"
            :source-refs="h.sourceRefs"
            :material-id="materialId"
            :material-label="materialLabel"
          />
          <div v-if="!highlights.length" class="muted">暂无亮点</div>
        </div>
        <div class="insight-col">
          <RiskCard :risks="risks" :material-id="materialId" :material-label="materialLabel" />
        </div>
        <div class="insight-col">
          <div class="col-title warning">重点事项</div>
          <InsightCard
            v-for="e in events"
            :key="e.id"
            type="EVENT"
            :content="e.content"
            :source-refs="e.sourceRefs"
            :material-id="materialId"
            :material-label="materialLabel"
          />
          <div v-if="!events.length" class="muted">暂无事项</div>
        </div>
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="tab">
        <el-tab-pane label="页面解析" name="slides">
          <el-table :data="slides" border max-height="480">
            <el-table-column prop="slideNo" label="页码" width="70" />
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="rawText" label="原文" min-width="420" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="指标" name="metrics">
          <div v-if="metrics.length" class="metric-grid">
            <MetricCard v-for="metric in metrics" :key="metric.id" :metric="metric" />
          </div>
          <el-empty v-else description="暂无指标" />
        </el-tab-pane>

        <el-tab-pane label="校验问题" name="findings">
          <el-table :data="uniqueFindings" border max-height="480">
            <el-table-column prop="ruleCode" label="规则" width="100" />
            <el-table-column label="类别" width="120">
              <template #default="{ row }">{{ categoryText(row.category) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="90">
              <template #default="{ row }"><StatusTag :status="row.severity" /></template>
            </el-table-column>
            <el-table-column prop="message" label="问题" min-width="340" show-overflow-tooltip />
            <el-table-column label="状态" width="110">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="AI 处理过程" name="process">
          <AiProcess :progress="progress" />
          <div class="trace-entry">
            <AiTrace :traces="traces" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <ValidationEvidence v-model="evidenceDialog" :source-refs="evidenceRefs" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StatCard from '../components/common/StatCard.vue'
import StatusTag from '../components/common/StatusTag.vue'
import EmptyState from '../components/common/EmptyState.vue'
import AiProcess from '../components/ai/AiProcess.vue'
import AiTrace from '../components/ai/AiTrace.vue'
import InsightCard from '../components/knowledge/InsightCard.vue'
import MetricCard from '../components/knowledge/MetricCard.vue'
import RiskCard from '../components/knowledge/RiskCard.vue'
import ValidationCard from '../components/validation/ValidationCard.vue'
import ValidationEvidence from '../components/validation/ValidationEvidence.vue'
import {
  confirmValidation,
  getAnalysis,
  getKnowledge,
  getMaterial,
  getMetrics,
  getModelData,
  getPreAudit,
  getSlides,
  getSummary,
  getTask,
  getTaskTraces,
  ignoreValidation,
  listValidations,
  retryMaterial
} from '../api'
import type { AnalysisFinding, AnalysisResult, Knowledge, LlmTrace, Material, MaterialSlide, Metric, ModelData, Summary, TaskProgress, ValidationResult } from '../types'

const route = useRoute()
const router = useRouter()
const materialId = Number(route.params.id)

const loading = ref(true)
const retrying = ref(false)
const tab = ref('slides')
const material = ref<Material>()
const slides = ref<MaterialSlide[]>([])
const metrics = ref<Metric[]>([])
const knowledge = ref<Knowledge[]>([])
const findings = ref<ValidationResult[]>([])
const preAudit = ref<AnalysisResult>()
const analysis = ref<AnalysisResult>()
const modelData = ref<ModelData[]>([])
const summary = ref<Summary>()
const progress = ref<TaskProgress>()
const traces = ref<LlmTrace[]>([])
const evidenceDialog = ref(false)
const evidenceRefs = ref('')
let eventSource: EventSource | null = null

const runningStatuses = ['WAITING', 'PARSING', 'VALIDATING', 'EXTRACTING']
const processing = computed(() => material.value ? runningStatuses.includes(material.value.status) : false)
const confidenceText = computed(() =>
  material.value?.confidence ? (material.value.confidence * 100).toFixed(0) + '%' : '-'
)
const materialLabel = computed(() => `${material.value?.organization || '材料'} · ${material.value?.reportPeriod || ''}`)
const uniqueKnowledge = computed(() => {
  const seen = new Set<string>()
  return knowledge.value.filter((k) => {
    const key = `${k.knowledgeType}|${k.content}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})
const highlights = computed(() => uniqueKnowledge.value.filter((k) => k.knowledgeType === 'HIGHLIGHT'))
const risks = computed(() => uniqueKnowledge.value.filter((k) => k.knowledgeType === 'RISK'))
const events = computed(() => uniqueKnowledge.value.filter((k) => k.knowledgeType === 'EVENT'))
const insightTotal = computed(() => highlights.value.length + risks.value.length + events.value.length)
const uniqueFindings = computed(() => {
  const seen = new Set<string>()
  return findings.value.filter((f) => {
    const key = `${f.ruleCode || ''}|${f.metricName || ''}|${f.message}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})
const previewConclusion = computed(() => {
  if (preAudit.value) {
    const verdictMap: Record<string, string> = {
      PASS: '材料预审通过，可作为后续经营分析的可靠输入。',
      REJECT: '材料预审未通过，存在需要退回补充的关键问题。',
      NEED_CONFIRM: '材料预审发现需要人工确认的问题，建议核对后再进入经营分析。'
    }
    return verdictMap[preAudit.value.verdict] || '材料预审结论已生成，请查看下方问题明细。'
  }
  if (!uniqueFindings.value.length) {
    return '整体材料未发现需要关注的数据或内容异常，AI 预审结论良好。'
  }
  const high = uniqueFindings.value.filter((f) => ['CRITICAL', 'HIGH'].includes(f.severity)).length
  const top = uniqueFindings.value.slice(0, 3).map((f) => f.message).join('；')
  return `材料发现 ${uniqueFindings.value.length} 个预审问题，其中高风险 ${high} 个。主要问题：${top}`
})
const preAuditVerdict = computed(() => preAudit.value?.verdict)
const preAuditFindings = computed(() => {
  if (!preAudit.value?.resultJson) return []
  try {
    return (JSON.parse(preAudit.value.resultJson) as { findings?: AnalysisFinding[] }).findings || []
  } catch {
    return []
  }
})
const analysisSummaryText = computed(() => {
  if (!analysis.value?.resultJson) return ''
  try {
    return (JSON.parse(analysis.value.resultJson) as { summary?: string }).summary || ''
  } catch {
    return ''
  }
})

const load = async () => {
  loading.value = true
  try {
    material.value = await getMaterial(materialId)
    if (route.query.processing === '1') {
      router.replace({ path: route.path, query: {} })
    }
    slides.value = await getSlides(materialId)
    metrics.value = await getMetrics(materialId)
    knowledge.value = await getKnowledge(materialId)
    findings.value = await listValidations({ materialId })
    summary.value = await getSummary(materialId)
    preAudit.value = (await getPreAudit(materialId))[0]
    analysis.value = (await getAnalysis(materialId))[0]
    modelData.value = await getModelData(materialId)
    if (material.value.taskId) {
      const detail = await getTask(material.value.taskId)
      progress.value = detail.progress
      traces.value = await getTaskTraces(material.value.taskId)
    }
    startSseIfNeeded()
  } finally {
    loading.value = false
  }
}

const startSseIfNeeded = () => {
  closeSse()
  if (!material.value?.taskId || !processing.value) return
  eventSource = new EventSource(`/api/tasks/${material.value.taskId}/events`)
  eventSource.onmessage = (event) => {
    if (!event.data) return
    try {
      const data = JSON.parse(event.data)
      progress.value = { ...progress.value, ...data }
      if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(data.status)) {
        closeSse()
        load()
      }
    } catch {
      // ignore
    }
  }
}

const retry = async () => {
  retrying.value = true
  try {
    await retryMaterial(materialId)
    await load()
  } finally {
    retrying.value = false
  }
}

const confirmFinding = async (finding: ValidationResult) => {
  await confirmValidation(finding.id)
  findings.value = await listValidations({ materialId })
}

const ignoreFinding = async (finding: ValidationResult) => {
  await ignoreValidation(finding.id)
  findings.value = await listValidations({ materialId })
}

const showEvidence = (finding: ValidationResult) => {
  evidenceRefs.value = finding.sourceRefs || ''
  evidenceDialog.value = true
}

const categoryText = (c: string) => {
  const map: Record<string, string> = {
    COMPLETENESS: '完整性',
    CREDIBILITY: '可信性',
    REASONABLENESS: '合理性',
    CONSISTENCY: '一致性'
  }
  return map[c] || c
}

const closeSse = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

onMounted(load)
onBeforeUnmount(closeSse)
</script>

<style scoped>
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.head-actions {
  display: flex;
  gap: 8px;
}
.summary-text {
  line-height: 1.8;
  color: #374151;
  margin: 0;
}
.finding-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.insight-cols {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.insight-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}
.col-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}
.col-title.ok {
  color: var(--ok);
}
.col-title.warning {
  color: var(--warning);
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.trace-entry {
  margin-top: 14px;
}
.preaudit-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}
.preaudit-item {
  display: flex;
  gap: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 12px;
}
.preaudit-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 6px;
  flex: 0 0 auto;
  background: var(--muted);
}
.preaudit-dot.high,
.preaudit-dot.critical {
  background: var(--danger);
}
.preaudit-dot.medium {
  background: var(--warning);
}
.preaudit-dot.low {
  background: #3b82f6;
}
.preaudit-body {
  min-width: 0;
}
.preaudit-message {
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: 4px;
}

@media (max-width: 1439px) {
  .insight-cols {
    grid-template-columns: 1fr 1fr;
  }
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1023px) {
  .insight-cols,
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
