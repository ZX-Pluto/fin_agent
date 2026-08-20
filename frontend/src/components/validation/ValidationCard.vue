<template>
  <div class="validation-card" :class="`sev-${finding.severity.toLowerCase()}`">
    <div class="card-head">
      <span class="severity-dot"></span>
      <div class="head-text">
        <div class="title">{{ finding.message }}</div>
        <div class="meta">
          {{ orgText }} · {{ periodText }} · {{ categoryText(finding.category) }} · {{ finding.ruleCode }}
        </div>
      </div>
      <StatusTag :status="finding.status" />
    </div>
    <div class="values" v-if="finding.actualValue || finding.expectedValue">
      <div class="value-item"><span class="muted">实际</span>{{ finding.actualValue || '-' }}</div>
      <div class="value-item"><span class="muted">期望</span>{{ finding.expectedValue || '-' }}</div>
    </div>
    <div class="ai-note" v-if="finding.suggestion">AI 建议：{{ finding.suggestion }}</div>
    <div class="card-actions">
      <el-button size="small" link @click="emit('evidence', finding)">查看证据</el-button>
      <el-button v-if="materialId" size="small" link type="primary" @click="emit('material', finding)">查看材料</el-button>
      <template v-if="finding.status === 'PENDING'">
        <el-button size="small" type="success" link @click="emit('confirm', finding)">确认异常</el-button>
        <el-button size="small" type="danger" link @click="emit('ignore', finding)">标记误报</el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import StatusTag from '../common/StatusTag.vue'
import type { ValidationResult } from '../../types'

const props = defineProps<{
  finding: ValidationResult
  orgText?: string
  periodText?: string
  materialId?: number
}>()

const emit = defineEmits<{
  (e: 'evidence', finding: ValidationResult): void
  (e: 'material', finding: ValidationResult): void
  (e: 'confirm', finding: ValidationResult): void
  (e: 'ignore', finding: ValidationResult): void
}>()

const categoryText = (c: string) => {
  const map: Record<string, string> = {
    COMPLETENESS: '完整性',
    CREDIBILITY: '可信性',
    REASONABLENESS: '合理性',
    CONSISTENCY: '一致性'
  }
  return map[c] || c
}
</script>

<style scoped>
.validation-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-left: 3px solid var(--brand);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 16px;
}
.validation-card.sev-critical {
  border-left-color: var(--danger);
}
.validation-card.sev-high {
  border-left-color: var(--warning);
}
.validation-card.sev-medium {
  border-left-color: #3b82f6;
}
.card-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.severity-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--brand);
  margin-top: 5px;
  flex: 0 0 auto;
}
.sev-critical .severity-dot {
  background: var(--danger);
}
.sev-high .severity-dot {
  background: var(--warning);
}
.head-text {
  flex: 1;
  min-width: 0;
}
.title {
  font-weight: 600;
}
.meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}
.values {
  display: flex;
  gap: 24px;
  margin-top: 12px;
  font-size: 14px;
}
.value-item span {
  margin-right: 8px;
}
.ai-note {
  margin-top: 10px;
  font-size: 13px;
  color: #374151;
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 10px;
}
.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 10px;
  border-top: 1px solid var(--line);
  padding-top: 8px;
}
</style>
