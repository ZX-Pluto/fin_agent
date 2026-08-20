<template>
  <div class="page">
    <PageHeader title="经营雷达" subtitle="跨代表处汇总经营简报：先看列表，点击代表处查看完整简报">
      <template #actions>
        <el-button type="primary" @click="$router.push('/materials/new')">新建分析</el-button>
      </template>
    </PageHeader>

    <div class="radar-overview">
      <div class="overview-item">
        <span class="overview-label">覆盖代表处</span>
        <span class="overview-value">{{ filteredRows.length }}</span>
        <span class="overview-hint">已完成经营简报</span>
      </div>
      <div class="overview-item">
        <span class="overview-label">平均收入同比</span>
        <span class="overview-value" :class="avgRevenueClass">{{ fmtPct(avgRevenueGrowth) }}</span>
        <span class="overview-hint">本期 vs 同期</span>
      </div>
      <div class="overview-item" :class="highRiskCount ? 'is-danger' : ''">
        <span class="overview-label">高风险代表处</span>
        <span class="overview-value">{{ highRiskCount }}</span>
        <span class="overview-hint">存在重大/高风险发现</span>
      </div>
      <div class="overview-item">
        <span class="overview-label">关注问题</span>
        <span class="overview-value" :class="totalFindings ? 'is-warn' : ''">{{ totalFindings }}</span>
        <span class="overview-hint">跨材料问题合计</span>
      </div>
    </div>

    <div class="panel toolbar">
      <el-select v-model="regionFilter" clearable placeholder="地区部" style="width: 200px">
        <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
      </el-select>
      <el-select v-model="periodFilter" clearable placeholder="期间" style="width: 180px">
        <el-option v-for="p in periodOptions" :key="p" :label="p" :value="p" />
      </el-select>
      <span class="muted">已完成 {{ filteredRows.length }} 份材料经营简报</span>
    </div>

    <div v-loading="loading">
      <div v-if="filteredRows.length" class="office-list">
        <div
          v-for="row in filteredRows"
          :key="row.materialId"
          class="office-card"
          :class="row.riskLevel === 'HIGH' ? 'risk-high' : 'risk-low'"
          role="button"
          tabindex="0"
          @click="openDetail(row)"
          @keydown.enter="openDetail(row)"
        >
          <div class="office-identity">
            <div class="office-avatar">{{ (row.org || '未').slice(0, 1) }}</div>
            <div class="identity-main">
              <div class="office-org">{{ row.org || '未命名代表处' }}</div>
              <div class="office-meta">{{ row.region }}<template v-if="row.period"> · {{ row.period }}</template></div>
            </div>
            <StatusTag :status="row.riskLevel" />
          </div>

          <div class="office-main">
            <div class="office-conclusion">{{ row.conclusion }}</div>
            <div class="mini-indicators">
              <div v-for="ind in row.indicators.slice(0, 4)" :key="ind.code" class="mini-indicator">
                <span class="mi-name">{{ ind.name }}</span>
                <span class="mi-value">{{ fmt(ind.value) }}<i v-if="ind.unit" class="mi-unit">{{ ind.unit }}</i></span>
                <span class="mi-change" :class="ind.changeStatus">{{ ind.changeLabel || '暂无同比' }}</span>
              </div>
            </div>
          </div>

          <div class="office-side">
            <div class="side-stat">
              <span class="side-value" :class="row.findings.length ? 'warn' : ''">{{ row.findings.length }}</span>
              <span class="side-label">问题</span>
            </div>
            <div class="side-stat">
              <span class="side-value">{{ row.credibility != null ? `${row.credibility}%` : '-' }}</span>
              <span class="side-label">可信度</span>
            </div>
            <el-button type="primary" size="small" class="open-btn" @click.stop="openDetail(row)">
              查看简报 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无已完成的经营简报" :image-size="90" />
    </div>

    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">AI 发现的共性问题</div>
        <span class="muted">跨代表处出现相同问题时自动汇总</span>
      </div>
      <div v-if="commonIssues.length" class="issue-list">
        <div v-for="issue in commonIssues" :key="issue.title" class="issue-card">
          <span class="severity-dot" :class="issue.severity.toLowerCase()"></span>
          <div class="issue-body">
            <div class="issue-title">{{ issue.title }}</div>
            <div class="issue-meta">涉及 {{ issue.materials.length }} 个代表处：{{ issue.materials.join('、') }}</div>
          </div>
          <StatusTag :status="issue.severity" />
        </div>
      </div>
      <el-empty v-else description="暂无跨代表处共性问题" :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
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
  credibility?: number
  pageCount?: number
}

const router = useRouter()
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

const openDetail = (row: OfficeRow) => {
  router.push(`/radar/${row.materialId}`)
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
          findings,
          credibility: briefing.header?.credibility,
          pageCount: briefing.header?.slideCount ?? briefing.evidence?.slideCount
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
.radar-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.overview-label {
  font-size: 12px;
  color: var(--muted);
}

.overview-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--ink);
}

.overview-value.ok { color: var(--ok); }
.overview-value.danger { color: var(--danger); }
.overview-value.is-warn { color: var(--warning); }
.overview-item.is-danger { border-color: #fecaca; background: #fffbfb; }

.overview-hint {
  font-size: 12px;
  color: var(--muted);
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.office-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.office-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 16px;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 130px;
  gap: 18px;
  align-items: center;
  cursor: pointer;
  transition: box-shadow 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

.office-card:hover,
.office-card:focus-visible {
  box-shadow: 0 8px 20px rgba(16, 24, 40, 0.1);
  transform: translateY(-2px);
  border-color: #f4c1c1;
}

.office-card.risk-low:hover,
.office-card.risk-low:focus-visible {
  border-color: #bcd0fb;
}

.office-identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.office-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.risk-low .office-avatar {
  background: #eaf2ff;
  color: #2563eb;
}

.identity-main {
  min-width: 0;
}

.office-org {
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.office-meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.office-main {
  min-width: 0;
}

.office-conclusion {
  color: #374151;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mini-indicators {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.mini-indicator {
  border: 1px solid #eef1f5;
  border-radius: 8px;
  background: #fafbfc;
  padding: 8px 10px;
  min-width: 0;
}

.mi-name {
  display: block;
  font-size: 11px;
  color: var(--muted);
}

.mi-value {
  display: block;
  font-size: 15px;
  font-weight: 700;
  margin-top: 3px;
  white-space: nowrap;
}

.mi-unit {
  font-style: normal;
  font-size: 11px;
  font-weight: 400;
  color: var(--muted);
  margin-left: 2px;
}

.mi-change {
  display: block;
  font-size: 11px;
  margin-top: 3px;
  color: var(--ok);
}

.mi-change.danger { color: var(--danger); }
.mi-change.muted { color: var(--muted); }

.office-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  border-left: 1px solid var(--line);
  padding-left: 16px;
}

.side-stat {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.side-value {
  font-size: 20px;
  font-weight: 700;
}

.side-value.warn { color: var(--warning); }

.side-label {
  font-size: 12px;
  color: var(--muted);
}

.open-btn {
  width: 100%;
}

.issue-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
}

.issue-body {
  flex: 1;
  min-width: 0;
}

.issue-title {
  font-weight: 600;
  line-height: 1.5;
}

.issue-meta {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.severity-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 5px;
  background: var(--muted);
  flex: 0 0 auto;
}

.severity-dot.high,
.severity-dot.critical { background: var(--danger); }
.severity-dot.medium { background: var(--warning); }

@media (max-width: 1439px) {
  .radar-overview {
    grid-template-columns: repeat(2, 1fr);
  }
  .office-card {
    grid-template-columns: 190px minmax(0, 1fr);
  }
  .office-side {
    grid-column: 1 / -1;
    flex-direction: row;
    justify-content: flex-end;
    border-left: none;
    border-top: 1px solid var(--line);
    padding-left: 0;
    padding-top: 12px;
  }
  .open-btn {
    width: auto;
  }
}

@media (max-width: 1023px) {
  .radar-overview,
  .mini-indicators {
    grid-template-columns: 1fr 1fr;
  }
  .office-card {
    grid-template-columns: 1fr;
  }
}
</style>