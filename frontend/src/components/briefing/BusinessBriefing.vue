<template>
  <div class="briefing" v-loading="loading">
    <template v-if="briefing">
      <div class="briefing-hero">
        <div class="hero-main">
          <div class="hero-kicker">代表处经营简报</div>
          <h1 class="hero-title">{{ header?.organization || '代表处' }} · {{ header?.reportPeriod || '—' }}</h1>
          <div class="hero-meta">
            <span>数据来源：{{ header?.materialName }}</span>
            <span>分析主题：{{ header?.themeName }}</span>
            <span>数据可信度：{{ header?.credibility ?? 95 }}%</span>
          </div>
        </div>
        <div class="hero-actions">
          <el-button @click="scrollToEvidence">查看原始材料</el-button>
          <el-button type="primary" @click="$emit('view-detail')">处理详情</el-button>
        </div>
      </div>

      <div class="overview-band" :class="overviewClass">
        <div class="overview-left">
          <div class="overview-verdict">{{ overview?.judgment || '分析完成' }}</div>
          <div v-if="overview?.businessScore != null" class="overview-score">
            <span class="score-value">{{ overview.businessScore }}</span>
            <span class="score-label">经营评分</span>
          </div>
        </div>
        <div class="overview-main">
          <div class="overview-text">{{ overview?.summaryText }}</div>
          <div v-if="overview?.coreConclusions?.length" class="overview-conclusions">
            <div v-for="(c, i) in overview.coreConclusions" :key="i" class="conclusion-item">
              <span class="conclusion-index">{{ i + 1 }}</span>
              <span>{{ c }}</span>
            </div>
          </div>
        </div>
      </div>

      <section class="briefing-section">
        <div class="section-head">
          <h3>核心经营指标</h3>
          <span class="muted">本期值 / 同比 / 达成情况</span>
        </div>
        <div class="indicator-grid">
          <div
            v-for="ind in indicators"
            :key="ind.code"
            class="indicator-card"
            :class="indicatorStatus(ind.status)"
          >
            <div class="indicator-name">{{ ind.name }}</div>
            <div class="indicator-value">
              {{ fmt(ind.value) }}<span v-if="ind.unit" class="indicator-unit">{{ ind.unit }}</span>
            </div>
            <div v-if="ind.changeLabel" class="indicator-change">{{ ind.changeLabel }}</div>
            <div v-else class="indicator-change muted">暂无同比</div>
            <div v-if="ind.subLabel" class="indicator-sub">{{ ind.subLabel }}</div>
            <div v-else class="indicator-sub muted">&nbsp;</div>
          </div>
        </div>
      </section>

      <section v-for="dim in dimensions" :key="dim.code" class="briefing-section dimension-section">
        <div class="section-head">
          <h3>{{ dim.name }}</h3>
          <span class="level-badge" :class="levelClass(dim.level)">{{ levelText(dim.level) }}</span>
        </div>
        <div class="dimension-layout">
          <div class="metric-list">
            <div v-for="m in dim.metrics" :key="m.fieldCode" class="metric-row">
              <span class="metric-name">{{ m.name }}</span>
              <span class="metric-value">
                {{ fmt(m.value) }}<span v-if="m.unit" class="metric-unit">{{ m.unit }}</span>
              </span>
              <span v-if="m.changeLabel" class="metric-change">{{ m.changeLabel }}</span>
              <span v-else class="metric-change muted">—</span>
            </div>
            <div v-if="!dim.metrics?.length" class="empty-dim">材料未提供该维度数据</div>
          </div>
          <div class="dimension-ai">
            <div class="ai-judgment" :class="levelClass(dim.level)">{{ dim.aiJudgment }}</div>
            <div v-for="(a, i) in dim.attention" :key="i" class="attention-item">
              <el-icon><Warning /></el-icon>
              <span>{{ a }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="briefing-section">
        <div class="section-head">
          <h3>AI 重点发现</h3>
          <span class="muted">按严重程度排序</span>
        </div>
        <div v-if="findings.length" class="finding-list">
          <div v-for="(f, i) in findings" :key="i" class="finding-card" :class="severityClass(f.severity)">
            <div class="finding-index">{{ String(i + 1).padStart(2, '0') }}</div>
            <div class="finding-body">
              <div class="finding-title">{{ f.subject || f.message }}</div>
              <div v-if="f.subject && f.message && f.message !== f.subject" class="finding-message">{{ f.message }}</div>
              <div v-if="f.reason" class="finding-reason">可能原因：{{ f.reason }}</div>
              <div class="finding-meta">
                <span v-if="f.evidence?.length" class="finding-evidence">证据：{{ f.evidence.join('；') }}</span>
                <el-button size="small" link type="primary" @click="scrollToEvidence">查看原文</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无 AI 重点发现" :image-size="70" />
      </section>

      <section class="briefing-section">
        <div class="section-head">
          <h3>重点事项</h3>
          <span class="muted">建议会后闭环跟进</span>
        </div>
        <div v-if="followUps.length" class="followup-grid">
          <div v-for="fu in followUps" :key="fu.id" class="followup-card">
            <div class="followup-title">{{ fu.title }}</div>
            <div v-if="fu.message" class="followup-message">{{ fu.message }}</div>
            <div class="followup-meta">
              <StatusTag :status="fu.status" />
              <span v-if="fu.sourceLabel" class="muted">{{ fu.sourceLabel }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无重点事项" :image-size="70" />
      </section>

      <section ref="evidenceRef" class="briefing-section">
        <div class="section-head">
          <h3>数据与证据</h3>
          <span class="muted">AI 结论可追溯链路</span>
        </div>
        <div class="evidence-grid">
          <div class="evidence-block">
            <div class="evidence-title">模型数据</div>
            <el-table :data="evidence?.metrics || []" border size="small" max-height="360">
              <el-table-column prop="name" label="字段" min-width="130" />
              <el-table-column label="数值" width="130">
                <template #default="{ row }">{{ fmt(row.value) }} {{ row.unit }}</template>
              </el-table-column>
              <el-table-column prop="changeLabel" label="变化" min-width="110" />
            </el-table>
            <div v-if="!evidence?.metrics?.length" class="empty-dim">暂无模型数据</div>
          </div>
          <div class="evidence-block">
            <div class="evidence-title">事实源章节</div>
            <div v-for="(fs, i) in evidence?.factSources || []" :key="i" class="fact-source-item">
              <div class="fact-source-head">
                <span class="fact-source-chapter">{{ fs.chapter }}</span>
                <span class="muted">{{ fs.pageCount ? `第 ${fs.pageCount} 页` : '' }}</span>
              </div>
              <div class="fact-source-preview">{{ fs.preview }}</div>
            </div>
            <div v-if="!evidence?.factSources?.length" class="empty-dim">暂无事实源</div>
          </div>
        </div>
        <div v-if="evidence?.slideCount != null" class="evidence-foot muted">
          数据可信度 {{ evidence?.credibility ?? 95 }}% · 材料共 {{ evidence.slideCount }} 页
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import StatusTag from '../common/StatusTag.vue'
import { getBriefing } from '../../api'
import type {
  Briefing,
  BriefingDimension,
  BriefingEvidence,
  BriefingFinding,
  BriefingFollowUp,
  BriefingHeader,
  BriefingIndicator,
  BriefingOverview
} from '../../types'

const props = defineProps<{ materialId: number }>()
defineEmits<{ (e: 'view-detail'): void }>()

const loading = ref(false)
const briefing = ref<Briefing>()
const evidenceRef = ref<HTMLElement>()

const header = computed<BriefingHeader | undefined>(() => briefing.value?.header)
const overview = computed<BriefingOverview | undefined>(() => briefing.value?.overview)
const indicators = computed<BriefingIndicator[]>(() => briefing.value?.coreIndicators || [])
const dimensions = computed<BriefingDimension[]>(() => briefing.value?.dimensions || [])
const findings = computed<BriefingFinding[]>(() => briefing.value?.findings || [])
const followUps = computed<BriefingFollowUp[]>(() => briefing.value?.followUps || [])
const evidence = computed<BriefingEvidence | undefined>(() => briefing.value?.evidence)

const overviewClass = computed(() => {
  const verdict = overview.value?.verdict
  if (verdict === 'NORMAL') return 'is-normal'
  if (verdict === 'ABNORMAL' || verdict === 'REJECT') return 'is-abnormal'
  return 'is-confirm'
})

const load = async () => {
  loading.value = true
  try {
    briefing.value = await getBriefing(props.materialId)
  } finally {
    loading.value = false
  }
}

const scrollToEvidence = () => {
  evidenceRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function fmt(value?: number) {
  if (value === undefined || value === null || Number.isNaN(value)) return '-'
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function indicatorStatus(status?: string) {
  if (status === 'DANGER') return 'is-danger'
  if (status === 'WARN') return 'is-warn'
  if (status === 'OK') return 'is-ok'
  return ''
}

function levelClass(level?: string) {
  if (level === 'RED') return 'level-red'
  if (level === 'YELLOW') return 'level-yellow'
  if (level === 'GREEN') return 'level-green'
  return 'level-none'
}

function levelText(level?: string) {
  if (level === 'RED') return '重点关注'
  if (level === 'YELLOW') return '关注'
  if (level === 'GREEN') return '正常'
  return '暂无判断'
}

function severityClass(severity?: string) {
  if (severity === 'CRITICAL' || severity === 'HIGH') return 'sev-danger'
  if (severity === 'MEDIUM') return 'sev-warn'
  return 'sev-ok'
}

watch(
  () => props.materialId,
  () => load()
)

load()
</script>

<style scoped>
.briefing {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.briefing-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  background: #0f172a;
  color: #fff;
  border-radius: 10px;
  padding: 22px 24px;
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 1px;
  color: #93c5fd;
}

.hero-title {
  margin: 8px 0 10px;
  font-size: 24px;
  font-weight: 700;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  font-size: 13px;
  color: #cbd5e1;
}

.hero-actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}

.overview-band {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 18px;
  background: #fff;
  border: 1px solid var(--line);
  border-left: 4px solid var(--warning);
  border-radius: 10px;
  padding: 18px 20px;
}

.overview-band.is-normal {
  border-left-color: var(--ok);
}

.overview-band.is-abnormal {
  border-left-color: var(--danger);
}

.overview-left {
  display: flex;
  align-items: center;
  gap: 14px;
  border-right: 1px solid var(--line);
  padding-right: 18px;
}

.overview-verdict {
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
}

.overview-score {
  text-align: center;
  flex: 0 0 auto;
}

.score-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--brand);
}

.score-label {
  font-size: 12px;
  color: var(--muted);
}

.overview-main {
  min-width: 0;
}

.overview-text {
  line-height: 1.7;
  color: #374151;
}

.overview-conclusions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.conclusion-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #4b5563;
}

.conclusion-index {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--brand-weak);
  color: var(--brand);
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.briefing-section {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 18px 20px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.indicator-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.indicator-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
  min-width: 0;
}

.indicator-card.is-danger {
  border-color: #fecaca;
  background: #fef2f2;
}

.indicator-card.is-warn {
  border-color: #fde68a;
  background: #fffbeb;
}

.indicator-card.is-ok {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.indicator-name {
  font-size: 13px;
  color: var(--muted);
}

.indicator-value {
  font-size: 22px;
  font-weight: 700;
  margin-top: 6px;
  white-space: nowrap;
}

.indicator-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--muted);
  margin-left: 3px;
}

.indicator-change {
  font-size: 12px;
  margin-top: 6px;
  color: var(--ok);
  font-weight: 600;
}

.indicator-card.is-danger .indicator-change {
  color: var(--danger);
}

.indicator-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #374151;
}

.dimension-section {
  scroll-margin-top: 12px;
}

.level-badge {
  font-size: 12px;
  font-weight: 600;
  border-radius: 20px;
  padding: 3px 10px;
}

.level-green {
  color: #15803d;
  background: #dcfce7;
}

.level-yellow {
  color: #b45309;
  background: #fef3c7;
}

.level-red {
  color: #b91c1c;
  background: #fee2e2;
}

.level-none {
  color: var(--muted);
  background: #f3f4f6;
}

.dimension-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.metric-list {
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
}

.metric-row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 0.8fr;
  gap: 10px;
  align-items: center;
  padding: 9px 12px;
  border-bottom: 1px solid var(--line);
  font-size: 13px;
}

.metric-row:last-child {
  border-bottom: none;
}

.metric-name {
  color: #374151;
}

.metric-value {
  font-weight: 600;
  text-align: right;
}

.metric-unit {
  color: var(--muted);
  font-weight: 400;
  margin-left: 2px;
}

.metric-change {
  text-align: right;
  color: var(--ok);
}

.dimension-ai {
  min-width: 0;
}

.ai-judgment {
  border-radius: 8px;
  padding: 12px 14px;
  line-height: 1.7;
  background: #f3f4f6;
  color: #374151;
  font-size: 14px;
}

.ai-judgment.level-green {
  background: #f0fdf4;
  color: #166534;
}

.ai-judgment.level-yellow {
  background: #fffbeb;
  color: #92400e;
}

.ai-judgment.level-red {
  background: #fef2f2;
  color: #991b1b;
}

.attention-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 10px;
  font-size: 13px;
  color: #92400e;
  line-height: 1.5;
}

.attention-item .el-icon {
  margin-top: 2px;
  flex: 0 0 auto;
}

.finding-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.finding-card {
  display: flex;
  gap: 14px;
  border: 1px solid var(--line);
  border-left: 4px solid var(--danger);
  border-radius: 8px;
  padding: 12px 14px;
}

.finding-card.sev-warn {
  border-left-color: var(--warning);
}

.finding-card.sev-ok {
  border-left-color: var(--ok);
}

.finding-index {
  font-size: 20px;
  font-weight: 700;
  color: var(--muted);
  flex: 0 0 auto;
}

.finding-body {
  min-width: 0;
  flex: 1;
}

.finding-title {
  font-weight: 700;
  line-height: 1.5;
}

.finding-message {
  margin-top: 6px;
  color: #374151;
  line-height: 1.6;
}

.finding-reason {
  margin-top: 8px;
  background: #f9fafb;
  border-radius: 6px;
  padding: 8px 10px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
}

.finding-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.finding-evidence {
  color: var(--muted);
  font-size: 13px;
}

.followup-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.followup-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 14px;
}

.followup-title {
  font-weight: 600;
  line-height: 1.5;
}

.followup-message {
  margin-top: 6px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.5;
}

.followup-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 12px;
}

.evidence-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  align-items: start;
}

.evidence-block {
  min-width: 0;
}

.evidence-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.fact-source-item {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}

.fact-source-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.fact-source-chapter {
  font-weight: 600;
  font-size: 13px;
}

.fact-source-preview {
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.evidence-foot {
  margin-top: 12px;
  font-size: 13px;
}

.empty-dim {
  color: var(--muted);
  font-size: 13px;
  padding: 16px 0;
  text-align: center;
}

@media (max-width: 1439px) {
  .indicator-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1023px) {
  .briefing-hero,
  .overview-band,
  .dimension-layout,
  .evidence-grid {
    grid-template-columns: 1fr;
  }

  .briefing-hero {
    flex-direction: column;
  }

  .overview-left {
    border-right: none;
    border-bottom: 1px solid var(--line);
    padding-right: 0;
    padding-bottom: 12px;
  }

  .indicator-grid,
  .followup-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .indicator-grid,
  .followup-grid {
    grid-template-columns: 1fr;
  }
}
</style>
