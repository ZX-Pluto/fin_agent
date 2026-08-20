<template>
  <div class="page">
    <PageHeader title="材料中心" subtitle="集中管理各代表处经营汇报材料">
      <template #actions>
        <el-button @click="$router.push('/tasks')">处理记录</el-button>
        <el-button type="primary" @click="uploadOpen = true">导入材料</el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <div class="toolbar">
        <FilterChips v-model="filter" :options="chips" />
        <div class="search-row">
          <el-input v-model="searchText" clearable placeholder="搜索材料名称 / 代表处" style="width: 240px" />
          <el-select v-model="orgFilter" clearable placeholder="代表处" style="width: 160px">
            <el-option v-for="org in orgOptions" :key="org" :label="org" :value="org" />
          </el-select>
          <el-select v-model="periodFilter" clearable placeholder="报告期间" style="width: 150px">
            <el-option v-for="p in periodOptions" :key="p" :label="p" :value="p" />
          </el-select>
          <el-button @click="clearFilters">清除筛选</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="filteredMaterials.length" class="material-grid">
        <MaterialCard v-for="m in filteredMaterials" :key="m.id" :material="m" :summary="summaries[m.id]" />
      </div>
      <EmptyState
        v-else
        title="暂无经营材料"
        description="导入代表处经营汇报材料后，AI 将自动进行解析与预审。"
      >
        <el-button type="primary" @click="uploadOpen = true">导入材料</el-button>
      </EmptyState>
    </div>

    <MaterialUpload v-model="uploadOpen" :loading="uploading" :themes="themes" @upload="handleUpload" />

    <el-dialog v-model="batchDialog" title="批量导入完成" width="440px">
      <div class="batch-result">
        <div class="batch-count">已提交 {{ batchCount }} 份材料</div>
        <div v-if="batchStats.completed || batchStats.running || batchStats.failed" class="batch-lines">
          <div v-if="batchStats.completed" class="ok">✓ {{ batchStats.completed }} 份已完成</div>
          <div v-if="batchStats.running" class="running">● {{ batchStats.running }} 份处理中</div>
          <div v-if="batchStats.failed" class="danger">✕ {{ batchStats.failed }} 份失败</div>
        </div>
        <div class="muted">材料已进入 AI 处理流水线，可在材料中心和处理记录中查看进度</div>
      </div>
      <template #footer>
        <el-button @click="batchDialog = false">查看全部材料</el-button>
        <el-button type="primary" @click="goTasks">查看处理记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../components/common/PageHeader.vue'
import FilterChips from '../components/common/FilterChips.vue'
import EmptyState from '../components/common/EmptyState.vue'
import MaterialCard from '../components/material/MaterialCard.vue'
import MaterialUpload from '../components/material/MaterialUpload.vue'
import { getSummary, listMaterials, listThemes, uploadMaterial } from '../api'
import type { Material, Summary, Theme } from '../types'

const router = useRouter()
const materials = ref<Material[]>([])
const themes = ref<Theme[]>([])
const summaries = ref<Record<number, Summary>>({})
const uploadOpen = ref(false)
const uploading = ref(false)
const loading = ref(false)
const filter = ref('ALL')
const searchText = ref('')
const orgFilter = ref('')
const periodFilter = ref('')
const batchDialog = ref(false)
const batchCount = ref(0)
const batchTaskIds = ref<number[]>([])

const countBy = (status: string) => materials.value.filter((m) => m.status === status).length
const countRunning = () =>
  materials.value.filter((m) => ['WAITING', 'PARSING', 'VALIDATING', 'EXTRACTING'].includes(m.status)).length

const chips = computed(() => [
  { value: 'ALL', label: `全部 ${materials.value.length}` },
  { value: 'RUNNING', label: `处理中 ${countRunning()}` },
  { value: 'COMPLETED', label: `已完成 ${countBy('COMPLETED')}` },
  { value: 'FAILED', label: `异常 ${countBy('FAILED')}` }
])

const orgOptions = computed(() => [...new Set(materials.value.map((m) => m.organization).filter(Boolean))] as string[])
const periodOptions = computed(() => [...new Set(materials.value.map((m) => m.reportPeriod).filter(Boolean))] as string[])

const filteredMaterials = computed(() => {
  let list = materials.value
  if (filter.value === 'COMPLETED') list = list.filter((m) => m.status === 'COMPLETED')
  if (filter.value === 'RUNNING') list = list.filter((m) => ['WAITING', 'PARSING', 'VALIDATING', 'EXTRACTING'].includes(m.status))
  if (filter.value === 'FAILED') list = list.filter((m) => m.status === 'FAILED')
  const keyword = searchText.value.trim().toLowerCase()
  if (keyword) {
    list = list.filter(
      (m) =>
        (m.materialName || '').toLowerCase().includes(keyword) ||
        (m.organization || '').toLowerCase().includes(keyword)
    )
  }
  if (orgFilter.value) list = list.filter((m) => m.organization === orgFilter.value)
  if (periodFilter.value) list = list.filter((m) => m.reportPeriod === periodFilter.value)
  return list
})

const batchStats = computed(() => {
  const matched = materials.value.filter((m) => batchTaskIds.value.includes(m.taskId ?? 0))
  return {
    completed: matched.filter((m) => m.status === 'COMPLETED').length,
    running: matched.filter((m) => ['WAITING', 'PARSING', 'VALIDATING', 'EXTRACTING'].includes(m.status)).length,
    failed: matched.filter((m) => m.status === 'FAILED').length
  }
})

const clearFilters = () => {
  filter.value = 'ALL'
  searchText.value = ''
  orgFilter.value = ''
  periodFilter.value = ''
}

const load = async () => {
  loading.value = true
  try {
    materials.value = await listMaterials()
    const completed = materials.value.filter((m) => m.status === 'COMPLETED')
    await Promise.all(
      completed.slice(0, 20).map(async (m) => {
        try {
          summaries.value[m.id] = await getSummary(m.id)
        } catch {
          // ignore
        }
      })
    )
  } finally {
    loading.value = false
  }
}

const handleUpload = async (payload: { files: File[]; region: string; organization: string; reportPeriod: string; themeId: number }) => {
  uploading.value = true
  const taskIds: number[] = []
  try {
    for (const file of payload.files) {
      const result = await uploadMaterial(file, payload.region, payload.organization, payload.reportPeriod, payload.themeId)
      taskIds.push(result.taskId)
    }
    await load()
    batchTaskIds.value = taskIds
    batchCount.value = materials.value.filter((m) => taskIds.includes(m.taskId ?? 0)).length
    uploadOpen.value = false
    batchDialog.value = true
  } finally {
    uploading.value = false
  }
}

const goTasks = () => {
  batchDialog.value = false
  router.push('/tasks')
}

onMounted(async () => {
  themes.value = await listThemes()
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.search-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.material-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 14px;
}
.batch-result {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.batch-count {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand);
}
.batch-lines {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.batch-lines .ok {
  color: var(--ok);
}
.batch-lines .running {
  color: var(--brand);
}
.batch-lines .danger {
  color: var(--danger);
}

@media (max-width: 1439px) {
  .material-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1023px) {
  .material-grid {
    grid-template-columns: 1fr;
  }
}
</style>
