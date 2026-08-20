<template>
  <div class="page" v-loading="loading">
    <PageHeader title="AI 预审中心" subtitle="事实源 + 预审规则 → 模型数据 → 经营分析结论">
      <template #actions>
        <el-button type="primary" :loading="running" @click="runAll">执行完整预审</el-button>
      </template>
    </PageHeader>

    <div class="panel tool-panel">
      <div class="tool-row">
        <div class="field">
          <span class="label">经营材料</span>
          <el-select v-model="materialId" placeholder="选择材料" style="width: 280px" @change="loadResult">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.organization || '材料'} · ${m.reportPeriod || ''}`" :value="m.id" />
          </el-select>
        </div>
        <div class="field">
          <span class="label">分析主题</span>
          <el-select v-model="themeId" placeholder="选择主题" style="width: 200px">
            <el-option v-for="t in themes" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </div>
        <div class="tool-actions">
          <el-button :loading="preAuditRunning" @click="runPreAudit">材料预审</el-button>
          <el-button :loading="mapRunning" @click="runMap">模型映射</el-button>
          <el-button :loading="analysisRunning" @click="runAnalysis">经营分析</el-button>
        </div>
      </div>
    </div>

    <div class="stat-grid">
      <StatCard label="预审问题" :value="preAuditFindings.length" />
      <StatCard label="高风险" :value="highCount" tone="danger" />
      <StatCard label="模型字段" :value="modelData.length" tone="ok" />
      <StatCard label="分析结论" :value="analysisVerdict || '未生成'" />
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">材料预审</div>
        <StatusTag v-if="preAudit" :status="preAudit.verdict" />
      </div>
      <el-empty v-if="!preAuditFindings.length" description="暂无预审问题，可先执行材料预审" :image-size="80" />
      <div v-else class="finding-list">
        <div v-for="(f, index) in preAuditFindings" :key="index" class="finding-card">
          <div class="finding-head">
            <span class="severity-dot" :class="f.severity.toLowerCase()"></span>
            <div class="finding-title">{{ f.message }}</div>
            <StatusTag :status="f.severity" />
          </div>
          <div class="finding-meta">
            <span v-if="f.ruleId">规则：{{ f.ruleId }}</span>
            <span v-if="f.sourceIds?.length">来源章节：{{ f.sourceIds.join('、') }}</span>
          </div>
          <div v-if="f.suggestion" class="finding-suggestion">建议：{{ f.suggestion }}</div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">模型数据</div>
      <el-table :data="modelData" border max-height="360">
        <el-table-column prop="fieldCode" label="字段" min-width="160" />
        <el-table-column prop="fieldValue" label="数值" width="130" />
        <el-table-column prop="unit" label="单位" width="90" />
        <el-table-column prop="modelVersion" label="模型版本" width="100" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!modelData.length" description="暂无模型数据，先执行模型映射" :image-size="80" />
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">经营分析结论</div>
        <StatusTag v-if="analysis" :status="analysis.verdict" />
      </div>
      <p v-if="analysisSummary" class="summary-text">{{ analysisSummary }}</p>
      <div v-if="analysisFindings.length" class="finding-list">
        <div v-for="(f, index) in analysisFindings" :key="index" class="finding-card">
          <div class="finding-head">
            <span class="severity-dot" :class="f.severity.toLowerCase()"></span>
            <div class="finding-title">{{ f.subject || f.message }}</div>
            <StatusTag :status="f.severity" />
          </div>
          <div class="finding-message">{{ f.message }}</div>
          <div v-if="f.evidence?.length" class="finding-meta">证据：{{ f.evidence.join('；') }}</div>
          <div v-if="f.suggestion" class="finding-suggestion">建议：{{ f.suggestion }}</div>
        </div>
      </div>
      <el-empty v-if="!analysis" description="暂无经营分析结论，可先执行经营分析" :image-size="80" />
    </div>

    <div class="panel">
      <div class="panel-title">事实源章节</div>
      <el-table :data="factSources" border max-height="320">
        <el-table-column prop="chapter" label="章节" min-width="160" />
        <el-table-column prop="slideRange" label="slideRange" width="130" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" />
      </el-table>
      <el-empty v-if="!factSources.length" description="暂无事实源" :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import StatCard from '../components/common/StatCard.vue'
import StatusTag from '../components/common/StatusTag.vue'
import {
  getAnalysis,
  getModelData,
  getPreAudit,
  listFactSources,
  listMaterials,
  listThemes,
  mapModelData,
  runAnalysis as requestAnalysis,
  runPreAudit as requestPreAudit
} from '../api'
import type { AnalysisFinding, AnalysisResult, FactSource, Material, ModelData, Theme } from '../types'

const materials = ref<Material[]>([])
const themes = ref<Theme[]>([])
const materialId = ref<number>()
const themeId = ref<number>()
const factSources = ref<FactSource[]>([])
const preAudit = ref<AnalysisResult>()
const modelData = ref<ModelData[]>([])
const analysis = ref<AnalysisResult>()
const loading = ref(false)
const running = ref(false)
const preAuditRunning = ref(false)
const mapRunning = ref(false)
const analysisRunning = ref(false)

interface ParsedAnalysis {
  verdict?: string
  findings?: AnalysisFinding[]
  summary?: string
}

const parseAnalysis = (result?: AnalysisResult): ParsedAnalysis => {
  if (!result?.resultJson) return {}
  try {
    return JSON.parse(result.resultJson) as ParsedAnalysis
  } catch {
    return {}
  }
}

const preAuditFindings = computed(() => parseAnalysis(preAudit.value).findings || [])
const highCount = computed(() => preAuditFindings.value.filter((f) => f.severity === 'HIGH' || f.severity === 'CRITICAL').length)
const analysisFindings = computed(() => parseAnalysis(analysis.value).findings || [])
const analysisSummary = computed(() => parseAnalysis(analysis.value).summary || '')
const analysisVerdict = computed(() => analysis.value?.verdict ? verdictText(analysis.value.verdict) : '')

function verdictText(verdict: string) {
  const map: Record<string, string> = {
    PASS: '通过',
    NORMAL: '正常',
    NEED_CONFIRM: '待确认',
    REJECT: '退回',
    ABNORMAL: '异常'
  }
  return map[verdict] || verdict
}

const loadBase = async () => {
  const [materialList, themeList] = await Promise.all([listMaterials(), listThemes()])
  materials.value = materialList
  themes.value = themeList
  materialId.value = materialId.value ?? materialList[0]?.id
  themeId.value = themeId.value ?? themeList[0]?.id
}

const loadResult = async () => {
  if (!materialId.value) return
  loading.value = true
  try {
    const [sources, audits, data, results] = await Promise.all([
      listFactSources(materialId.value),
      getPreAudit(materialId.value),
      getModelData(materialId.value),
      getAnalysis(materialId.value)
    ])
    factSources.value = sources
    preAudit.value = audits[0]
    modelData.value = data
    analysis.value = results[0]
  } finally {
    loading.value = false
  }
}

const runPreAudit = async () => {
  if (!materialId.value || !themeId.value) return
  preAuditRunning.value = true
  try {
    preAudit.value = await requestPreAudit(materialId.value, themeId.value)
    ElMessage.success('材料预审完成')
    await loadResult()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '材料预审失败')
  } finally {
    preAuditRunning.value = false
  }
}

const runMap = async () => {
  if (!materialId.value || !themeId.value) return
  mapRunning.value = true
  try {
    modelData.value = await mapModelData(materialId.value, themeId.value)
    ElMessage.success('模型映射完成')
    await loadResult()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '模型映射失败')
  } finally {
    mapRunning.value = false
  }
}

const runAnalysis = async () => {
  if (!materialId.value || !themeId.value) return
  analysisRunning.value = true
  try {
    analysis.value = await requestAnalysis(materialId.value, themeId.value)
    ElMessage.success('经营分析完成')
    await loadResult()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '经营分析失败')
  } finally {
    analysisRunning.value = false
  }
}

const runAll = async () => {
  if (!materialId.value || !themeId.value) return
  running.value = true
  try {
    preAudit.value = await requestPreAudit(materialId.value, themeId.value)
    modelData.value = await mapModelData(materialId.value, themeId.value)
    analysis.value = await requestAnalysis(materialId.value, themeId.value)
    ElMessage.success('完整预审链路执行完成')
    await loadResult()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '完整预审执行失败')
  } finally {
    running.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadBase()
    await loadResult()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tool-panel {
  padding: 14px 18px;
}
.tool-row {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field .label {
  font-size: 12px;
  color: var(--muted);
}
.tool-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}
.finding-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.finding-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.finding-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.finding-title {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  line-height: 1.5;
}
.finding-message {
  color: var(--ink);
  line-height: 1.6;
}
.finding-meta {
  color: var(--muted);
  font-size: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.finding-suggestion {
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
.summary-text {
  line-height: 1.8;
  color: #374151;
  margin: 0 0 12px;
}
</style>
