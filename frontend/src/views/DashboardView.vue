<template>
  <div class="page">
    <PageHeader title="工作台" subtitle="经营材料处理与 AI 预审概览">
      <template #actions>
        <el-button type="primary" @click="$router.push('/materials')">导入经营材料</el-button>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard label="全部材料" :value="materials.length" hint="本期材料" />
      <StatCard label="已完成" :value="completedCount" tone="ok" :hint="`${completedPercent}%`" />
      <StatCard label="待确认问题" :value="pendingCount" tone="warning" hint="需要人工确认" />
      <StatCard label="AI 发现风险" :value="riskItems.length" tone="danger" :hint="`${highFindingCount} 个高风险`" />
    </div>

    <div class="dash-grid">
      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">最近处理材料</div>
          <el-button link type="primary" @click="$router.push('/materials')">查看全部</el-button>
        </div>
        <div v-if="recentMaterials.length" class="recent-grid">
          <MaterialCard v-for="m in recentMaterials" :key="m.id" :material="m" :summary="summaries[m.id]" />
        </div>
        <EmptyState
          v-else
          title="暂无经营材料"
          description="导入代表处经营汇报材料后，AI 将自动进行解析与预审。"
        >
          <el-button type="primary" @click="$router.push('/materials')">导入材料</el-button>
        </EmptyState>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div class="panel-title">AI 预审概览</div>
          <el-button link type="primary" @click="$router.push('/validations')">查看全部</el-button>
        </div>
        <div class="sev-list">
          <div class="sev-row"><span class="muted">问题总数</span><b>{{ allFindings.length }}</b></div>
          <div class="sev-row"><span class="dot critical"></span>高风险 <b>{{ highFindingCount }}</b></div>
          <div class="sev-row"><span class="dot medium"></span>中风险 <b>{{ mediumFindingCount }}</b></div>
          <div class="sev-row"><span class="dot pending"></span>待确认 <b>{{ pendingCount }}</b></div>
        </div>
        <div class="top-problems" v-if="topProblems.length">
          <div
            v-for="p in topProblems"
            :key="p.key"
            class="problem-item"
            @click="$router.push({ path: '/validations', query: { materialId: p.materialId } })"
          >
            <span class="problem-dot" :class="p.severity.toLowerCase()"></span>
            <div class="problem-body">
              <div class="problem-title">{{ p.message }}</div>
              <div class="muted">{{ p.materialLabel }}</div>
            </div>
          </div>
        </div>
        <div v-else class="muted">暂无预审问题</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">AI 经营洞察</div>
      <div class="insight-cols">
        <div class="insight-col">
          <div class="col-title ok">经营亮点</div>
          <InsightCard v-for="h in topHighlights" :key="h.key" type="HIGHLIGHT" :content="h.content" :source-refs="h.sourceRefs" :material-id="h.materialId" :material-label="h.materialLabel" />
          <div v-if="!topHighlights.length" class="muted">暂无经营亮点</div>
        </div>
        <div class="insight-col">
          <div class="col-title danger">经营风险</div>
          <InsightCard v-for="r in topRisks" :key="r.key" type="RISK" :content="r.content" :source-refs="r.sourceRefs" :material-id="r.materialId" :material-label="r.materialLabel" />
          <div v-if="!topRisks.length" class="muted">暂无经营风险</div>
        </div>
        <div class="insight-col">
          <div class="col-title warning">重点事项</div>
          <InsightCard v-for="e in topEvents" :key="e.key" type="EVENT" :content="e.content" :source-refs="e.sourceRefs" :material-id="e.materialId" :material-label="e.materialLabel" />
          <div v-if="!topEvents.length" class="muted">暂无重点事项</div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">AI 处理成效</div>
      <div class="value-grid">
        <div><div class="big">{{ completedCount }}</div><div class="muted">AI 已自动处理材料</div></div>
        <div><div class="big">{{ metricTotal }}</div><div class="muted">指标检查</div></div>
        <div><div class="big">{{ findingTotal }}</div><div class="muted">校验问题</div></div>
        <div><div class="big">{{ insightTotal }}</div><div class="muted">经营要点</div></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatCard from '../components/common/StatCard.vue'
import EmptyState from '../components/common/EmptyState.vue'
import MaterialCard from '../components/material/MaterialCard.vue'
import InsightCard from '../components/knowledge/InsightCard.vue'
import { getKnowledge, getSummary, listMaterials, listValidations } from '../api'
import type { Knowledge, Material, Summary, ValidationResult } from '../types'

interface InsightItem {
  key: string
  type: string
  content: string
  sourceRefs?: string
  materialId: number
  materialLabel: string
}

const materials = ref<Material[]>([])
const summaries = ref<Record<number, Summary>>({})
const allFindings = ref<ValidationResult[]>([])
const knowledgeItems = ref<InsightItem[]>([])

const completedCount = computed(() => materials.value.filter((m) => m.status === 'COMPLETED').length)
const completedPercent = computed(() =>
  materials.value.length ? Math.round((completedCount.value / materials.value.length) * 100) : 0
)
const pendingCount = computed(() => allFindings.value.filter((f) => f.status === 'PENDING').length)
const highFindingCount = computed(() => allFindings.value.filter((f) => ['CRITICAL', 'HIGH'].includes(f.severity)).length)
const mediumFindingCount = computed(() => allFindings.value.filter((f) => f.severity === 'MEDIUM').length)
const findingTotal = computed(() => allFindings.value.length)
const recentMaterials = computed(() => materials.value.slice(0, 3))
const metricTotal = computed(() => Object.values(summaries.value).reduce((sum, s) => sum + (s.metricCount || 0), 0))
const riskItems = computed(() => knowledgeItems.value.filter((k) => k.type === 'RISK'))
const insightTotal = computed(() => knowledgeItems.value.length)

const materialLabelMap = computed(() => {
  const map: Record<number, string> = {}
  materials.value.forEach((m) => {
    map[m.id] = `${m.organization || '材料'} · ${m.reportPeriod || ''}`
  })
  return map
})

const uniqueFindings = computed(() => {
  const seen = new Set<string>()
  return allFindings.value.filter((f) => {
    const key = `${f.ruleCode || ''}|${f.metricName || ''}|${f.message}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})

const severityOrder: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }
const topProblems = computed(() =>
  [...uniqueFindings.value]
    .sort((a, b) => (severityOrder[a.severity] ?? 9) - (severityOrder[b.severity] ?? 9))
    .slice(0, 3)
    .map((f) => ({
      key: `${f.ruleCode}|${f.metricName || ''}|${f.message}`,
      severity: f.severity,
      message: f.message,
      materialId: f.materialId,
      materialLabel: materialLabelMap.value[f.materialId] || `材料 ${f.materialId}`
    }))
)

const dedupeInsights = (type: string) => {
  const seen = new Set<string>()
  return knowledgeItems.value
    .filter((k) => k.type === type)
    .filter((k) => {
      const key = `${k.type}|${k.content}`
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    .slice(0, 3)
}

const topHighlights = computed(() => dedupeInsights('HIGHLIGHT'))
const topRisks = computed(() => dedupeInsights('RISK'))
const topEvents = computed(() => dedupeInsights('EVENT'))

onMounted(async () => {
  materials.value = await listMaterials()
  allFindings.value = await listValidations({})
  const completed = materials.value.filter((m) => m.status === 'COMPLETED')
  await Promise.all(
    completed.map(async (m) => {
      try {
        summaries.value[m.id] = await getSummary(m.id)
      } catch {
        // ignore
      }
      try {
        const knowledge = await getKnowledge(m.id)
        knowledge.forEach((k: Knowledge) => {
          knowledgeItems.value.push({
            key: `${k.id}`,
            type: k.knowledgeType,
            content: k.content,
            sourceRefs: k.sourceRefs,
            materialId: m.id,
            materialLabel: materialLabelMap.value[m.id] || `${m.organization || '材料'} · ${m.reportPeriod || ''}`
          })
        })
      } catch {
        // ignore
      }
    })
  )
})
</script>

<style scoped>
.dash-grid {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 16px;
}
.recent-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.sev-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 12px;
}
.sev-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sev-row b {
  margin-left: auto;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: 0 0 auto;
}
.dot.critical {
  background: var(--danger);
}
.dot.medium {
  background: #3b82f6;
}
.dot.pending {
  background: var(--warning);
}
.top-problems {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--line);
  padding-top: 12px;
}
.problem-item {
  display: flex;
  gap: 8px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid var(--line);
  transition: border-color 0.2s;
}
.problem-item:hover {
  border-color: var(--brand);
}
.problem-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex: 0 0 auto;
  background: var(--warning);
}
.problem-dot.critical {
  background: var(--danger);
}
.problem-dot.high {
  background: var(--warning);
}
.problem-body {
  min-width: 0;
}
.problem-title {
  font-size: 13px;
  line-height: 1.5;
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
}
.col-title.ok {
  color: var(--ok);
}
.col-title.danger {
  color: var(--danger);
}
.col-title.warning {
  color: var(--warning);
}
.value-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.value-grid > div {
  text-align: center;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 14px;
}
.big {
  font-size: 28px;
  font-weight: 700;
  color: var(--brand);
}

@media (max-width: 1439px) {
  .dash-grid {
    grid-template-columns: 1fr;
  }
  .recent-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .insight-cols {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1023px) {
  .recent-grid,
  .insight-cols,
  .value-grid {
    grid-template-columns: 1fr;
  }
}
</style>
