<template>
  <div class="page">
    <PageHeader title="事项跟踪" subtitle="将 AI 分析发现转化为可持续跟踪的经营事项" />

    <div class="filter-row panel">
      <el-radio-group v-model="statusFilter">
        <el-radio-button label="ALL">全部 {{ items.length }}</el-radio-button>
        <el-radio-button label="TODO">待跟进 {{ countBy('TODO') }}</el-radio-button>
        <el-radio-button label="DOING">跟进中 {{ countBy('DOING') }}</el-radio-button>
        <el-radio-button label="DONE">已闭环 {{ countBy('DONE') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="item-list">
      <div v-for="item in filteredItems" :key="item.id" class="panel item-card">
        <div class="item-head">
          <div class="item-title">{{ item.title }}</div>
          <StatusTag :status="item.status" />
        </div>
        <div class="item-source">{{ item.sourceLabel }}</div>
        <div class="item-message">{{ item.message }}</div>
        <div v-if="item.suggestion" class="item-suggestion">建议：{{ item.suggestion }}</div>
        <div class="item-actions">
          <el-button v-if="item.status === 'TODO'" size="small" @click="setStatus(item, 'DOING')">开始跟进</el-button>
          <el-button v-if="item.status === 'DOING'" size="small" type="primary" @click="setStatus(item, 'DONE')">完成事项</el-button>
          <el-button v-if="item.status !== 'TODO'" size="small" @click="setStatus(item, 'TODO')">重新打开</el-button>
          <el-button size="small" type="primary" link @click="$router.push(`/materials/${item.materialId}`)">查看依据</el-button>
        </div>
      </div>
      <EmptyState v-if="!filteredItems.length" title="暂无事项" description="AI 发现高优先级问题后会自动沉淀为待跟进事项。" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatusTag from '../components/common/StatusTag.vue'
import EmptyState from '../components/common/EmptyState.vue'
import { listFollowUps, syncFollowUps, updateFollowUpStatus } from '../api'
import type { FollowUp } from '../types'

const items = ref<FollowUp[]>([])
const loading = ref(false)
const statusFilter = ref('ALL')

const filteredItems = computed(() =>
  statusFilter.value === 'ALL' ? items.value : items.value.filter((i) => i.status === statusFilter.value)
)

const countBy = (status: string) => items.value.filter((i) => i.status === status).length

const setStatus = async (item: FollowUp, status: string) => {
  await updateFollowUpStatus(item.id, status)
  await load()
}

const load = async () => {
  loading.value = true
  try {
    await syncFollowUps()
    items.value = await listFollowUps()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.filter-row {
  display: flex;
  align-items: center;
}
.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}
.item-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.item-title {
  font-weight: 700;
}
.item-source {
  color: var(--muted);
  font-size: 12px;
}
.item-message {
  line-height: 1.6;
  color: #374151;
}
.item-suggestion {
  background: var(--brand-weak);
  color: #1f4ea5;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
}
.item-actions {
  display: flex;
  gap: 8px;
  border-top: 1px solid var(--line);
  padding-top: 10px;
}
</style>
