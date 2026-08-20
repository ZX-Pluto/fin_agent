<template>
  <div class="page">
    <PageHeader title="经营洞察" subtitle="AI 从材料中提取的指标、亮点、风险与重点事项">
      <template #actions>
        <span v-if="materialId" class="current-material">当前材料：{{ materialLabel }}</span>
        <el-select v-model="materialId" style="width: 260px" @change="load">
          <el-option v-for="m in materials" :key="m.id" :label="`${m.organization} ${m.reportPeriod}`" :value="m.id" />
        </el-select>
      </template>
    </PageHeader>

    <div class="panel">
      <div class="panel-title">经营指标</div>
      <div v-if="metrics.length" class="metric-grid">
        <MetricCard v-for="metric in metrics" :key="metric.id" :metric="metric" />
      </div>
      <div v-else class="empty-state">暂无指标</div>
    </div>

    <div class="insight-grid">
      <div class="panel">
        <div class="panel-title ok-title">↑ 经营亮点</div>
        <div class="insight-col">
          <InsightCard
            v-for="h in highlights"
            :key="h.id"
            type="HIGHLIGHT"
            :content="h.content"
            :source-refs="h.sourceRefs"
            :material-id="materialId"
            :material-label="materialLabel"
          />
          <div v-if="!highlights.length" class="empty-state">暂无亮点</div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-title danger-title">⚠ 风险事项</div>
        <RiskCard :risks="risks" :material-id="materialId" :material-label="materialLabel" />
      </div>
      <div class="panel">
        <div class="panel-title warning-title">● 重点事项</div>
        <div class="insight-col">
          <InsightCard
            v-for="e in events"
            :key="e.id"
            type="EVENT"
            :content="e.content"
            :source-refs="e.sourceRefs"
            :material-id="materialId"
            :material-label="materialLabel"
          />
          <div v-if="!events.length" class="empty-state">暂无事项</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import InsightCard from '../components/knowledge/InsightCard.vue'
import MetricCard from '../components/knowledge/MetricCard.vue'
import RiskCard from '../components/knowledge/RiskCard.vue'
import { getKnowledge, getMetrics, listMaterials } from '../api'
import type { Knowledge, Material, Metric } from '../types'

const materials = ref<Material[]>([])
const materialId = ref<number>()
const metrics = ref<Metric[]>([])
const knowledge = ref<Knowledge[]>([])

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
const materialLabel = computed(() => {
  const m = materials.value.find((x) => x.id === materialId.value)
  return `${m?.organization || '材料'} · ${m?.reportPeriod || ''}`
})

const load = async () => {
  if (!materialId.value) return
  metrics.value = await getMetrics(materialId.value)
  knowledge.value = await getKnowledge(materialId.value)
}

onMounted(async () => {
  materials.value = await listMaterials()
  if (materials.value.length > 0) {
    materialId.value = materials.value[0].id
    await load()
  }
})
</script>

<style scoped>
.current-material {
  font-size: 13px;
  color: var(--muted);
  margin-right: 4px;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.insight-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ok-title {
  color: var(--ok);
}
.danger-title {
  color: var(--danger);
}
.warning-title {
  color: var(--warning);
}

@media (max-width: 1439px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1023px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
