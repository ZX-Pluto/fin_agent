<template>
  <div class="ai-trace">
    <el-button text type="primary" @click="open = !open">
      {{ open ? '收起 AI 执行详情' : '查看 AI 执行详情' }}
    </el-button>
    <div v-show="open" class="trace-body">
      <el-table :data="traces" border size="small" class="trace-table">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="trace-block"><b>Agent</b><p>{{ row.agentName }}</p></div>
            <div class="trace-block"><b>Skill</b><p>{{ row.skillName }}</p></div>
            <div class="trace-block"><b>Prompt</b><p>{{ row.prompt }}</p></div>
            <div class="trace-block"><b>Response</b><p>{{ row.response || row.errorMessage }}</p></div>
          </template>
        </el-table-column>
        <el-table-column prop="agentName" label="Agent" min-width="140" />
        <el-table-column prop="skillName" label="Skill" min-width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="modelName" label="模型" min-width="140" show-overflow-tooltip />
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.latencyMs ? (row.latencyMs / 1000).toFixed(1) + 's' : '-' }}</template>
        </el-table-column>
        <el-table-column label="Token" width="100">
          <template #default="{ row }">{{ row.inputTokens || 0 }} / {{ row.outputTokens || 0 }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import StatusTag from '../common/StatusTag.vue'
import type { LlmTrace } from '../../types'

defineProps<{ traces: LlmTrace[] }>()
const open = ref(false)
</script>

<style scoped>
.ai-trace {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.trace-body {
  border-top: 1px solid var(--line);
  padding-top: 12px;
}
</style>
