<template>
  <div class="page">
    <PageHeader title="经营雷达" subtitle="跨代表处汇总经营简报：核心指标、风险与 AI 结论">
      <template #actions>
        <el-button type="primary" @click="$router.push('/materials/new')">新建分析</el-button>
      </template>
    </PageHeader>

    <div class="panel toolbar">
      <el-select v-model="regionFilter" clearable placeholder="地区部" style="width: 200px">
        <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
      </el-select>
      <el-select v-model="periodFilter" clearable placeholder="期间" style="width: 180px">
        <el-option v-for="p in periodOptions" :key="p" :label="p" :value="p" />
      </el-select>
      <span class="muted">已完成 {{ filteredRows.length }} 份材料经营简报</span>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="label">覆盖代表处</div>
        <div class="value">{{ filteredRows.length }}</div>
      </div>
      <div class="stat-card">
        <div class="label">平均收入同比</div>
        <div class="value" :class="avgRevenueClass">{{ fmtPct(avgRevenueGrowth) }}</div>
      </div>
      <div class="stat-card">
        <div class="label">高风险代表处</div>
        <div class="value" :class="highRiskCount ? 'danger' : 'ok'">{{ highRiskCount }}</div>
      </div>
      <div class="stat-card">
        <div class="label">关注问题</div>
        <div class="value" :class="totalFindings ? 'warning' : 'ok'">{{ totalFindings }}</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">代表处经营简报卡片</div>
      <div v-if="filteredRows.length" class="office-grid">
        <div v-for="row in filteredRows" :key="row.materialId" class="office-card" :class="row.riskLevel === 'HIGH' ? 'risk-high' : 'risk-low'">
          <div class="office-head">
            <div>
              <div class="office-org">{{ row.org || '未命名代表处' }}</div>
              <div class="office-meta muted">{{ row.region }} · {{ row.period }}</div>
            </div>
            <StatusTag :status="row.riskLevel" />
          </div>
          <div class="office-indicators">
            <div v-for="ind in row.indicators" :key="ind.code" class="office-indicator">
              <span class="oi-name">{{ ind.name }}</span>
              <span class="oi-value">{{ fmt(ind.value) }}<i v-if="ind.unit" class="oi-unit">{{ ind.unit }}</i></span>
              <span class="oi-change" :class="ind.changeStatus">{{ ind.changeLabel || '暂无同比' }}</span>
            </div>
          </div>
          <div class="office-conclusion">{{ row.conclusion }}</div>
          <div class="office-foot">
            <el-button size="small" type="primary" link @click="$router.push(`/materials/${row.materialId}`)">查看经营简报</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无已完成的经营简报" :image-size="90" />
    </div>

    <div class="panel">
      <div class="panel-title">AI 发现的共性问题</div>
      <div v-if="commonIssues.length" class="issue-list">
        <div v-for="issue in commonIssues" :key="issue.title" class="issue-card">
          <div class="issue-head">
            <span class="severity-dot" :class="issue.severity.toLowerCase()"></span>
            <div class="issue-title">{{ issue.title }}</div>
            <StatusTag :status="issue.severity" />
          </div>
          <div class="issue-meta">涉及 {{ issue.materials.length }} 个代表处：{{ issue.materials.join('、') }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无跨代表处共性问题" :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { getBriefing, listMaterials } from '../api'
import type { Briefing, BriefingFinding, BriefingIndicator, Material } from '../types'

interface OfficeRow {
  materialId: number
  region: string
  period: string
  org: string
  riskLevel: string
  conclusion: string
  indicators: Array<BriefingIndicator & { changeStatus?: string }>
  findings: BriefingFinding[]
}

const materials = ref<Material[]>([])
const rows = ref<OfficeRow[]>([])
const loading = ref(false)
const regionFilter = ref('')
const periodFilter = ref('')

const regionOptions = computed(() => [...new Set(rows.value.map((r) => r.region))])
const periodOptions = computed(() => [...new Set(rows.value.map((r) => r.period).filter(Boolean))] as string[])

const filteredRows = computed(() =>
  rows.value.filter(
    (r) =>
      (!regionFilter.value || r.region === regionFilter.value) &&
      (!periodFilter.value || r.period === periodFilter.value)
  )
)

const highRiskCount = computed(() => filteredRows.value.filter((r) => r.riskLevel === 'HIGH').length)
const totalFindings = computed(() => filteredRows.value.reduce((sum, r) => sum + r.findings.length, 0))

const avgRevenueGrowth = computed(() => {
  const values = filteredRows.value
    .map((r) => r.indicators.find((i) => i.code === 'revenue')?.change)
    .filter((v): v is number => v !== undefined && v !== null)
  if (!values.length) return null
  return values.reduce((a, b) => a + b, 0) / values.length
})

const avgRevenueClass = computed(() => {
  if (avgRevenueGrowth.value === null) return ''
  return avgRevenueGrowth.value >= 0 ? 'ok' : 'danger'
})

const commonIssues = computed(() => {
  const map = new Map<string, { title: string; severity: string; materials: string[] }>()
  for (const row of rows.value) {
    for (const f of row.findings) {
      const key = (f.message || f.subject || '').slice(0, 24)
      if (!key) continue
      const item = map.get(key) || { title: f.message || f.subject || '', severity: f.severity || 'LOW', materials: [] }
      if (!item.materials.includes(row.org)) item.materials.push(row.org)
      map.set(key, item)
    }
  }
  return [...map.values()]
    .filter((item) => item.materials.length > 1)
    .sort((a, b) => severityOrder(a.severity) - severityOrder(b.severity))
    .slice(0, 6)
})

function severityOrder(s: string) {
  return { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }[s] ?? 9
}

function fmt(value?: number) {
  if (value === undefined || value === null || Number.isNaN(value)) return '-'
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}

function fmtPct(value?: number | null) {
  if (value === undefined || value === null || Number.isNaN(value)) return '-'
  return `${value >= 0 ? '+' : ''}${Number(value).toFixed(1)}%`
}

function buildIndicators(briefing: Briefing) {
  const order = ['order', 'revenue', 'gross_profit', 'collection', 'dso', 'net_profit']
  const source = new Map((briefing.coreIndicators || []).map((i) => [i.code, i]))
  return order
    .map((code) => {
      const ind = source.get(code)
      if (!ind) return null
      const changeStatus = ind.change === undefined || ind.change === null
        ? 'muted'
        : code === 'dso'
          ? ind.change > 0 ? 'danger' : 'ok'
          : ind.change >= 0 ? 'ok' : 'danger'
      return { ...ind, changeStatus }
    })
    .filter((i): i is BriefingIndicator & { changeStatus?: string } => i !== null)
}

const load = async () => {
  loading.value = true
  try {
    materials.value = await listMaterials()
    const completed = materials.value.filter((m) => m.status === 'COMPLETED')
    const loaded = await Promise.all(
      completed.map(async (m) => {
        const briefing = await getBriefing(m.id)
        const findings = briefing.findings || []
        const riskLevel = findings.some((f) => ['CRITICAL', 'HIGH'].includes(f.severity || ''))
          ? 'HIGH'
          : 'LOW'
        return {
          materialId: m.id,
          region: m.region || '默认地区部',
          period: m.reportPeriod || '',
          org: m.organization || '',
          riskLevel,
          conclusion: briefing.overview?.summaryText || briefing.overview?.judgment || '经营分析已完成',
          indicators: buildIndicators(briefing),
          findings
        } as OfficeRow
      })
    )
    rows.value = loaded
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.office-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.office-card {
  border: 1px solid var(--line);
  border-top: 4px solid var(--ok);
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.office-card.risk-high {
  border-top-color: var(--danger);
  background: #fffbfb;
}

.office-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.office-org {
  font-size: 16px;
  font-weight: 700;
}

.office-meta {
  font-size: 12px;
  margin-top: 4px;
}

.office-indicators {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.office-indicator {
  min-width: 0;
}

.oi-name {
  display: block;
  font-size: 12px;
  color: var(--muted);
}

.oi-value {
  display: block;
  font-size: 16px;
  font-weight: 700;
  margin-top: 3px;
  white-space: nowrap;
}

.oi-unit {
  font-style: normal;
  font-size: 11px;
  font-weight: 400;
  color: var(--muted);
  margin-left: 2px;
}

.oi-change {
  display: block;
  font-size: 12px;
  margin-top: 3px;
  color: var(--ok);
}

.oi-change.danger {
  color: var(--danger);
}

.oi-change.muted {
  color: var(--muted);
}

.office-conclusion {
  color: #374151;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.office-foot {
  border-top: 1px solid var(--line);
  padding-top: 10px;
  text-align: right;
}

.issue-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
}

.issue-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.issue-title {
  flex: 1;
  min-width: 0;
  font-weight: 600;
}

.issue-meta {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}

.severity-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  background: var(--muted);
}

.severity-dot.high,
.severity-dot.critical {
  background: var(--danger);
}

.severity-dot.medium {
  background: var(--warning);
}

@media (max-width: 1439px) {
  .office-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1023px) {
  .office-grid {
    grid-template-columns: 1fr;
  }
}
</style>
