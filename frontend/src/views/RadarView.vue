<template>
  <div class="page">
    <PageHeader title="经营雷达" subtitle="跨代表处查看经营指标与 AI 发现的共性问题">
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
      <span class="muted">已完成 {{ filteredRows.length }} 份材料分析</span>
    </div>

    <div class="panel">
      <div class="panel-title">代表处经营概览</div>
      <el-table :data="filteredRows" v-loading="loading" border max-height="560">
        <el-table-column prop="region" label="地区部" min-width="120" />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="org" label="代表处" min-width="140" />
        <el-table-column label="收入" width="120">
          <template #default="{ row }">{{ fmt(row.revenue) }} <span class="muted">{{ row.revenueUnit }}</span></template>
        </el-table-column>
        <el-table-column label="收入同比" width="100">
          <template #default="{ row }">{{ fmt(row.revenueGrowth) }}%</template>
        </el-table-column>
        <el-table-column label="利润" width="120">
          <template #default="{ row }">{{ fmt(row.profit) }} <span class="muted">{{ row.profitUnit }}</span></template>
        </el-table-column>
        <el-table-column label="回款" width="120">
          <template #default="{ row }">{{ fmt(row.collection) }}</template>
        </el-table-column>
        <el-table-column label="DSO" width="100">
          <template #default="{ row }">{{ fmt(row.dso) }}天</template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template #default="{ row }"><StatusTag :status="row.riskLevel" /></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="$router.push(`/materials/${row.materialId}`)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
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
      <el-empty v-else description="暂无共性问题" :image-size="80" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatusTag from '../components/common/StatusTag.vue'
import { getAnalysis, getModelData, listMaterials } from '../api'
import type { AnalysisResult, Material, ModelData } from '../types'

interface RadarRow {
  materialId: number
  region: string
  period: string
  org: string
  revenue?: number
  revenueUnit?: string
  revenueGrowth?: number
  profit?: number
  profitUnit?: string
  collection?: number
  dso?: number
  riskLevel: string
  findings: Array<{ severity: string; message: string }>
}

const materials = ref<Material[]>([])
const rows = ref<RadarRow[]>([])
const loading = ref(false)
const regionFilter = ref('')
const periodFilter = ref('')

const regionOptions = computed(() => [...new Set(materials.value.map((m) => m.region || '默认地区部'))])
const periodOptions = computed(() => [...new Set(materials.value.map((m) => m.reportPeriod).filter(Boolean))] as string[])

const filteredRows = computed(() =>
  rows.value.filter(
    (r) =>
      (!regionFilter.value || r.region === regionFilter.value) &&
      (!periodFilter.value || r.period === periodFilter.value)
  )
)

const commonIssues = computed(() => {
  const map = new Map<string, { title: string; severity: string; materials: string[] }>()
  for (const row of rows.value) {
    for (const f of row.findings) {
      const key = f.message.slice(0, 24)
      const item = map.get(key) || { title: f.message, severity: f.severity, materials: [] }
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
  if (value === undefined || value === null) return '-'
  return Number(value).toLocaleString()
}

const valueOf = (data: ModelData[], code: string) => data.find((d) => d.fieldCode === code)

const load = async () => {
  loading.value = true
  try {
    materials.value = await listMaterials()
    const completed = materials.value.filter((m) => m.status === 'COMPLETED')
    const loaded = await Promise.all(
      completed.map(async (m) => {
        const [data, analysis] = await Promise.all([getModelData(m.id), getAnalysis(m.id)])
        const findings: Array<{ severity: string; message: string }> = []
        for (const result of analysis) {
          try {
            const parsed = JSON.parse(result.resultJson) as { findings?: Array<{ severity: string; message: string }> }
            findings.push(...(parsed.findings || []))
          } catch {
            // ignore
          }
        }
        return {
          materialId: m.id,
          region: m.region || '默认地区部',
          period: m.reportPeriod || '',
          org: m.organization || '',
          revenue: valueOf(data, 'revenue')?.fieldValue,
          revenueUnit: valueOf(data, 'revenue')?.unit,
          revenueGrowth: valueOf(data, 'revenue_growth')?.fieldValue,
          profit: valueOf(data, 'profit')?.fieldValue,
          profitUnit: valueOf(data, 'profit')?.unit,
          collection: valueOf(data, 'collection')?.fieldValue,
          dso: valueOf(data, 'dso')?.fieldValue,
          riskLevel: findings.some((f) => ['CRITICAL', 'HIGH'].includes(f.severity)) ? 'HIGH' : 'LOW',
          findings
        } as RadarRow
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
</style>
