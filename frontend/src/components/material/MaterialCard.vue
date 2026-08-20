<template>
  <div class="material-card" :class="`tone-${cardTone}`" role="button" tabindex="0" @click="goDetail" @keydown.enter="goDetail">
    <div class="card-top">
      <div class="org-avatar">{{ orgInitial }}</div>
      <div class="head-main">
        <div class="org-row">
          <span class="org-name">{{ material.organization || '未命名代表处' }}</span>
          <StatusTag :status="material.status" />
        </div>
        <div class="meta-row">
          <span>{{ material.reportPeriod || '未标注期间' }}</span>
          <span class="meta-dot">·</span>
          <span>{{ material.themeName || '未选主题' }}</span>
        </div>
      </div>
    </div>

    <div class="metric-grid">
      <div class="metric-cell">
        <el-icon class="metric-icon"><Files /></el-icon>
        <div class="metric-text">
          <span class="metric-value">{{ summary?.slideCount ?? '-' }}</span>
          <span class="metric-label">页</span>
        </div>
      </div>
      <div class="metric-cell">
        <el-icon class="metric-icon"><TrendCharts /></el-icon>
        <div class="metric-text">
          <span class="metric-value">{{ summary?.metricCount ?? '-' }}</span>
          <span class="metric-label">项指标</span>
        </div>
      </div>
      <div class="metric-cell" :class="{ warn: findingCount > 0 }">
        <el-icon class="metric-icon"><WarningFilled /></el-icon>
        <div class="metric-text">
          <span class="metric-value">{{ findingCount }}</span>
          <span class="metric-label">问题</span>
        </div>
      </div>
    </div>

    <div v-if="confidenceText" class="confidence-row">
      <span class="confidence-label">可信度</span>
      <div class="confidence-track">
        <div class="confidence-fill" :class="confidenceTone" :style="{ width: `${confidencePct}%` }"></div>
      </div>
      <span class="confidence-value">{{ confidenceText }}</span>
    </div>

    <div v-if="summaryTextShort" class="summary-box">{{ summaryTextShort }}</div>

    <div class="card-foot">
      <span class="date-text">{{ dateText }}</span>
      <span class="open-link">查看分析 <el-icon><ArrowRight /></el-icon></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Files, TrendCharts, WarningFilled } from '@element-plus/icons-vue'
import StatusTag from '../common/StatusTag.vue'
import type { Material, Summary } from '../../types'

const props = defineProps<{ material: Material; summary?: Summary }>()
const router = useRouter()

const runningStatuses = ['WAITING', 'PARSING', 'VALIDATING', 'EXTRACTING']

const cardTone = computed(() => {
  const status = props.material.status
  if (status === 'COMPLETED') return 'ok'
  if (status === 'FAILED' || status === 'CANCELLED') return 'danger'
  if (runningStatuses.includes(status)) return 'brand'
  return 'neutral'
})

const orgInitial = computed(() => (props.material.organization || '未').slice(0, 1))
const findingCount = computed(() => props.summary?.findingCount ?? 0)

const confidencePct = computed(() => {
  const raw = props.material.confidence
  if (raw === undefined || raw === null || Number.isNaN(raw)) return 0
  return Math.min(100, Math.round(raw > 1 ? raw : raw * 100))
})
const confidenceText = computed(() => (confidencePct.value ? `${confidencePct.value}%` : ''))
const confidenceTone = computed(() => {
  const pct = confidencePct.value
  return pct >= 80 ? 'high' : pct >= 60 ? 'mid' : 'low'
})

const summaryTextShort = computed(() => {
  const text = props.summary?.summaryText
  if (!text) return ''
  return text.length > 96 ? text.slice(0, 96) + '…' : text
})

const dateText = computed(() => (props.material.updateTime || props.material.createTime || '').slice(0, 10))

const goDetail = () => {
  router.push(`/materials/${props.material.id}`)
}
</script>

<style scoped>
.material-card {
  position: relative;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 16px 16px 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  outline: none;
  transition: box-shadow 0.18s ease, transform 0.18s ease, border-color 0.18s ease;
}

.material-card:hover,
.material-card:focus-visible {
  box-shadow: 0 8px 20px rgba(16, 24, 40, 0.1);
  transform: translateY(-2px);
  border-color: #bcd0fb;
}

.material-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--muted);
}

.material-card.tone-ok::before { background: var(--ok); }
.material-card.tone-brand::before { background: var(--brand); }
.material-card.tone-danger::before { background: var(--danger); }

.card-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.org-avatar {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eaf2ff, #dbeafe);
  color: #2563eb;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.head-main {
  min-width: 0;
  flex: 1;
}

.org-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.org-name {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-dot {
  color: #cbd5e1;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  border-top: 1px dashed var(--line);
  padding-top: 12px;
}

.metric-cell {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 8px 9px;
  border-radius: 8px;
  background: #f8fafc;
}

.metric-icon {
  font-size: 16px;
  color: var(--muted);
  flex: 0 0 auto;
}

.metric-text {
  display: flex;
  align-items: baseline;
  gap: 3px;
  min-width: 0;
}

.metric-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}

.metric-label {
  font-size: 11px;
  color: var(--muted);
  white-space: nowrap;
}

.metric-cell.warn .metric-icon,
.metric-cell.warn .metric-value {
  color: var(--danger);
}

.confidence-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  align-items: center;
  font-size: 12px;
}

.confidence-label,
.confidence-value {
  color: var(--muted);
  white-space: nowrap;
}

.confidence-track {
  height: 6px;
  border-radius: 99px;
  background: #eef1f5;
  overflow: hidden;
}

.confidence-fill {
  height: 100%;
  border-radius: 99px;
  background: var(--warning);
}

.confidence-fill.high { background: var(--ok); }
.confidence-fill.mid { background: var(--brand); }
.confidence-fill.low { background: var(--danger); }

.summary-box {
  background: #f8fafc;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  padding: 9px 11px;
  font-size: 12px;
  color: #4b5563;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--line);
  padding-top: 10px;
}

.date-text {
  font-size: 12px;
  color: var(--muted);
}

.open-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand);
}
</style>