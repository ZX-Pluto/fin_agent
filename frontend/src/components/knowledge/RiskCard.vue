<template>
  <div class="risk-card">
    <div class="head">⚠ 经营风险</div>
    <div v-if="!risks.length" class="muted">暂无风险</div>
    <InsightCard
      v-for="r in risks"
      :key="r.id"
      type="RISK"
      :content="r.content"
      :source-refs="r.sourceRefs"
      :material-id="materialId"
      :material-label="materialLabel"
    />
  </div>
</template>

<script setup lang="ts">
import InsightCard from './InsightCard.vue'
import type { Knowledge } from '../../types'

defineProps<{ risks: Knowledge[]; materialId?: number; materialLabel?: string }>()
</script>

<style scoped>
.risk-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.head {
  font-size: 14px;
  font-weight: 600;
}
</style>
