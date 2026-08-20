<template>
  <div class="page">
    <PageHeader title="处理记录" subtitle="材料智能处理的执行过程与 AI 调用详情">
      <template #actions>
        <el-button @click="$router.push('/materials')">返回材料中心</el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <el-table :data="tasks" v-loading="loading" border @row-click="openProgress">
        <el-table-column prop="taskName" label="材料" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" />
          </template>
        </el-table-column>
        <el-table-column prop="currentAgent" label="当前阶段" width="180" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="finishTime" label="结束时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="danger" link :disabled="isFinished(row.status)" @click.stop="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="drawer" title="处理过程" size="560px">
      <template v-if="progress">
        <AiProcess :progress="progress" />
        <div class="trace-entry">
          <AiTrace :traces="traces" />
        </div>
      </template>
      <el-empty v-else description="暂无进度" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import StatusTag from '../components/common/StatusTag.vue'
import AiProcess from '../components/ai/AiProcess.vue'
import AiTrace from '../components/ai/AiTrace.vue'
import { cancelTask, getTask, getTaskTraces, listTasks } from '../api'
import type { LlmTrace, Task, TaskProgress } from '../types'

const tasks = ref<Task[]>([])
const loading = ref(false)
const drawer = ref(false)
const progress = ref<TaskProgress>()
const traces = ref<LlmTrace[]>([])
let eventSource: EventSource | null = null

const load = async () => {
  loading.value = true
  try {
    tasks.value = await listTasks()
  } finally {
    loading.value = false
  }
}

const openProgress = async (row: Task) => {
  drawer.value = true
  closeEventSource()
  const detail = await getTask(row.id)
  progress.value = detail.progress
  traces.value = await getTaskTraces(row.id)
  eventSource = new EventSource(`/api/tasks/${row.id}/events`)
  eventSource.onmessage = (event) => {
    if (event.data) {
      try {
        const data = JSON.parse(event.data)
        progress.value = { ...progress.value, ...data }
        if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(data.status)) {
          closeEventSource()
          load()
        }
      } catch {
        // ignore
      }
    }
  }
}

const cancel = async (row: Task) => {
  await cancelTask(row.id)
  await load()
}

const closeEventSource = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

const isFinished = (status?: string) => ['COMPLETED', 'FAILED', 'CANCELLED'].includes(status || '')

onMounted(load)
onBeforeUnmount(closeEventSource)
</script>

<style scoped>
.trace-entry {
  margin-top: 16px;
}
</style>
