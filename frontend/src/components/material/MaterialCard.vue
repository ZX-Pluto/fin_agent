<template>
  <div class="material-card">
    <div class="card-top">
      <div class="org">{{ material.organization || '未命名代表处' }}</div>
      <StatusTag :status="material.status" />
    </div>
    <div class="period">{{ material.reportPeriod || '未标注期间' }} 经营汇报 · {{ material.themeName || '未选主题' }}</div>
    <div class="metrics">
      <span>{{ summary?.slideCount ?? '-' }} 页</span>
      <span>{{ summary?.metricCount ?? '-' }} 项指标</span>
      <span :class="{ danger: (summary?.findingCount || 0) > 0 }">{{ summary?.findingCount ?? '-' }} 个问题</span>
      <span>{{ material.confidence ? (material.confidence * 100).toFixed(0) + '%' : '-' }} 可信度</span>
    </div>
    <div class="insights" v-if="summary">
      <span class="ok">● {{ summary.highlightCount }} 亮点</span>
      <span class="danger">● {{ summary.riskCount }} 风险</span>
    </div>
    <div v-if="summary?.summaryText" class="summary">{{ summaryTextShort }}</div>
    <div class="card-footer">
      <span class="muted date">{{ material.updateTime?.slice(0, 10) || material.createTime?.slice(0, 10) }}</span>
      <el-button type="primary" link @click="$router.push(`/materials/${material.id}`)">查看分析</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import StatusTag from '../common/StatusTag.vue'
import { computed } from 'vue'
import type { Material, Summary } from '../../types'

const props = defineProps<{ material: Material; summary?: Summary }>()

const summaryTextShort = computed(() => {
  const text = props.summary?.summaryText
  if (!text) return ''
  return text.length > 90 ? text.slice(0, 90) + '…' : text
})
</script>

<style scoped>
.material-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.org {
  font-size: 16px;
  font-weight: 700;
}
.period {
  color: var(--muted);
  font-size: 13px;
}
.metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--muted);
}
.metrics .danger {
  color: var(--danger);
}
.insights {
  display: flex;
  gap: 14px;
  font-size: 12px;
}
.insights .ok {
  color: var(--ok);
}
.insights .danger {
  color: var(--danger);
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--line);
  padding-top: 10px;
}
.summary {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.6;
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.date {
  font-size: 12px;
}
</style>
