<template>
  <div v-if="progress" class="ai-process">
    <div class="head">
      <StatusTag :status="progress.status || 'WAITING'" />
      <span class="muted">{{ progress.currentAgent }}</span>
      <span class="muted message">{{ progress.message }}</span>
    </div>
    <el-progress
      :percentage="progress.progress || 0"
      :stroke-width="12"
      :status="done ? 'success' : progress?.status === 'FAILED' ? 'exception' : undefined"
    />
    <div class="stage-list">
      <div v-for="(line, i) in progress.eventLog || []" :key="i" class="stage-item">
        <el-icon class="check"><CircleCheckFilled /></el-icon>
        <span>{{ line }}</span>
      </div>
    </div>
  </div>
  <el-empty v-else description="暂无处理进度" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import StatusTag from '../common/StatusTag.vue'
import type { TaskProgress } from '../../types'

const props = defineProps<{ progress?: TaskProgress }>()

const done = computed(() => props.progress?.status === 'COMPLETED')
</script>

<style scoped>
.ai-process {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.message {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stage-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.stage-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #374151;
}
.check {
  color: var(--ok);
  flex: 0 0 auto;
}
</style>
