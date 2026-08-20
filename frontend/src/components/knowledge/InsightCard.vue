<template>
  <div class="insight-card" :class="type.toLowerCase()">
    <div class="icon">{{ icon }}</div>
    <div class="body">
      <div class="content">{{ content }}</div>
      <div class="meta-row">
        <span v-if="materialLabel || sourceText" class="source muted">{{ materialLabel ? `来源：${materialLabel}` : '' }}{{ materialLabel && sourceText ? ' · ' : '' }}{{ sourceText }}</span>
        <el-button v-if="materialId" size="small" link type="primary" @click="$router.push(`/materials/${materialId}`)">查看材料</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ type: string; content: string; sourceRefs?: string; materialId?: number; materialLabel?: string }>()

const icon = computed(() => (props.type === 'RISK' ? '⚠' : props.type === 'EVENT' ? '●' : '↑'))
const sourceText = computed(() => {
  try {
    const rows = JSON.parse(props.sourceRefs || '[]')
    const first = Array.isArray(rows) ? rows[0] : rows
    return first?.slideNo ? `来源：第 ${first.slideNo} 页` : '来源：LLM 提取'
  } catch {
    return '来源：材料'
  }
})
</script>

<style scoped>
.insight-card {
  display: flex;
  gap: 12px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 14px;
}
.icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: var(--ok);
  color: #fff;
  flex: 0 0 auto;
}
.insight-card.risk .icon {
  background: var(--danger);
}
.insight-card.event .icon {
  background: var(--warning);
}
.body {
  flex: 1;
  min-width: 0;
}
.content {
  font-size: 14px;
  line-height: 1.6;
}
.source {
  font-size: 12px;
  margin-top: 6px;
}
</style>
