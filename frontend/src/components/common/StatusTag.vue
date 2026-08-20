<template>
  <el-tag :type="tagType" size="small">{{ text }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const statusMap: Record<string, { type: 'success' | 'danger' | 'warning' | 'info'; text: string }> = {
  COMPLETED: { type: 'success', text: '已完成' },
  SUCCESS: { type: 'success', text: '成功' },
  CONFIRMED: { type: 'success', text: '已确认' },
  FAILED: { type: 'danger', text: '失败' },
  FALSE_POSITIVE: { type: 'danger', text: '误报' },
  CANCELLED: { type: 'danger', text: '已取消' },
  CRITICAL: { type: 'danger', text: '重大' },
  HIGH: { type: 'warning', text: '高' },
  MEDIUM: { type: 'warning', text: '中' },
  LOW: { type: 'info', text: '低' },
  PENDING: { type: 'warning', text: '待确认' },
  WAITING: { type: 'warning', text: '待处理' },
  PARSING: { type: 'warning', text: '解析中' },
  VALIDATING: { type: 'warning', text: '校验中' },
  EXTRACTING: { type: 'warning', text: '提取中' },
  PARSED: { type: 'info', text: '已解析' },
  VALIDATED: { type: 'info', text: '已校验' },
  EXTRACTED: { type: 'info', text: '已提取' },
  SKIPPED: { type: 'info', text: '跳过' }
}

const tagType = computed(() => statusMap[props.status]?.type || 'info')
const text = computed(() => statusMap[props.status]?.text || props.status)
</script>
