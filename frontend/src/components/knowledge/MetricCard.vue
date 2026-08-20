<template>
  <div class="metric-card">
    <div class="name">{{ metric.metricName }}</div>
    <div class="value">{{ metric.value ?? '-' }}<span class="unit">{{ metric.unit }}</span></div>
    <div class="meta">
      <span>{{ metric.normalizedName }}</span>
      <span>{{ metric.confidence ? (metric.confidence * 100).toFixed(0) + '%' : '' }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Metric } from '../../types'

defineProps<{ metric: Metric }>()
</script>

<style scoped>
.metric-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 14px;
}
.name {
  font-size: 13px;
  color: var(--muted);
}
.value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 6px;
}
.unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--muted);
  margin-left: 4px;
}
.meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted);
  margin-top: 8px;
}
</style>
