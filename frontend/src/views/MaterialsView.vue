<template>
  <div class="page">
    <PageHeader title="经营材料" subtitle="按地区部归集各代表处经营汇报材料，点击卡片查看分析结果">
      <template #actions>
        <el-button type="primary" @click="$router.push('/materials/new')">新建分析</el-button>
      </template>
    </PageHeader>

    <div class="summary-grid">
      <StatCard label="材料总数" :value="materials.length" tone="brand" hint="全部经营材料" />
      <StatCard label="已完成" :value="countBy('COMPLETED')" tone="ok" hint="可查看经营简报" />
      <StatCard label="处理中" :value="countRunning()" tone="warning" hint="AI 流水线执行中" />
      <StatCard label="异常" :value="countBy('FAILED')" tone="danger" hint="需要重新处理" />
    </div>

    <div class="panel toolbar">
      <FilterChips v-model="statusFilter" :options="statusChips" />
      <div class="search-row">
        <el-input v-model="keyword" clearable placeholder="搜索代表处 / 材料 / 主题" style="width: 260px" />
        <el-select v-model="regionFilter" clearable placeholder="地区部" style="width: 180px">
          <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
        </el-select>
        <el-select v-model="periodFilter" clearable placeholder="期间" style="width: 160px">
          <el-option v-for="p in periodOptions" :key="p" :label="p" :value="p" />
        </el-select>
        <el-button @click="clearFilters">清除筛选</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="regionGroups.length" class="region-list">
        <section v-for="region in regionGroups" :key="region.name" class="region-block">
          <div class="region-head">
            <div class="region-title">
              <span class="region-name">{{ region.name }}</span>
              <span class="muted">{{ region.count }} 份材料</span>
            </div>
            <div class="region-status">
              <span class="status-pill ok">已完成 {{ region.completed }}</span>
              <span class="status-pill running">处理中 {{ region.running }}</span>
              <span class="status-pill danger">失败 {{ region.failed }}</span>
            </div>
          </div>
          <div class="material-grid">
            <MaterialCard v-for="m in region.materials" :key="m.id" :material="m" :summary="summaries[m.id]" />
          </div>
        </section>
      </div>
      <EmptyState
        v-else
        title="暂无经营材料"
        description="新建分析后，材料会按地区部自动归集。"
      >
        <el-button type="primary" @click="$router.push('/materials/new')">新建分析</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import FilterChips from '../components/common/FilterChips.vue'
import StatCard from '../components/common/StatCard.vue'
import EmptyState from '../components/common/EmptyState.vue'
import MaterialCard from '../components/material/MaterialCard.vue'
import { getSummary, listMaterials } from '../api'
import type { Material, Summary } from '../types'

interface RegionGroup {
  name: string
  count: number
  completed: number
  running: number
  failed: number
  materials: Material[]
}

const materials = ref<Material[]>([])
const summaries = ref<Record<number, Summary>>({})
const loading = ref(false)
const keyword = ref('')
const regionFilter = ref('')
const periodFilter = ref('')
const statusFilter = ref('ALL')

const regionOptions = computed(() => [...new Set(materials.value.map((m) => m.region || '默认地区部'))])
const periodOptions = computed(() => [...new Set(materials.value.map((m) => m.reportPeriod).filter(Boolean))] as string[])
const runningStatuses = ['WAITING', 'PARSING', 'VALIDATING', 'EXTRACTING']

const statusChips = computed(() => [
  { value: 'ALL', label: `全部 ${materials.value.length}` },
  { value: 'RUNNING', label: `处理中 ${countRunning()}` },
  { value: 'COMPLETED', label: `已完成 ${countBy('COMPLETED')}` },
  { value: 'FAILED', label: `失败 ${countBy('FAILED')}` }
])

const filteredMaterials = computed(() => {
  let list = materials.value
  if (statusFilter.value === 'RUNNING') list = list.filter((m) => runningStatuses.includes(m.status))
  if (statusFilter.value === 'COMPLETED') list = list.filter((m) => m.status === 'COMPLETED')
  if (statusFilter.value === 'FAILED') list = list.filter((m) => m.status === 'FAILED')
  if (regionFilter.value) list = list.filter((m) => (m.region || '默认地区部') === regionFilter.value)
  if (periodFilter.value) list = list.filter((m) => m.reportPeriod === periodFilter.value)
  const q = keyword.value.trim().toLowerCase()
  if (q) {
    list = list.filter(
      (m) =>
        (m.organization || '').toLowerCase().includes(q) ||
        (m.materialName || '').toLowerCase().includes(q) ||
        (m.themeName || '').toLowerCase().includes(q)
    )
  }
  return list
})

const regionGroups = computed<RegionGroup[]>(() => {
  const groups: RegionGroup[] = []
  for (const m of filteredMaterials.value) {
    const regionName = m.region || '默认地区部'
    let group = groups.find((g) => g.name === regionName)
    if (!group) {
      group = { name: regionName, count: 0, completed: 0, running: 0, failed: 0, materials: [] }
      groups.push(group)
    }
    group.materials.push(m)
    group.count += 1
    if (m.status === 'COMPLETED') group.completed += 1
    else if (runningStatuses.includes(m.status)) group.running += 1
    else if (m.status === 'FAILED') group.failed += 1
  }
  return groups
})

function countBy(status: string) {
  return materials.value.filter((m) => m.status === status).length
}

function countRunning() {
  return materials.value.filter((m) => runningStatuses.includes(m.status)).length
}

function clearFilters() {
  keyword.value = ''
  regionFilter.value = ''
  periodFilter.value = ''
  statusFilter.value = 'ALL'
}

const load = async () => {
  loading.value = true
  try {
    materials.value = await listMaterials()
    const completed = materials.value.filter((m) => m.status === 'COMPLETED')
    await Promise.all(
      completed.slice(0, 40).map(async (m) => {
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

onMounted(load)
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

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

.region-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.region-block {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 16px;
}

.region-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.region-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.region-name {
  font-size: 17px;
  font-weight: 700;
}

.region-status {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-pill {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 99px;
  background: #f3f4f6;
  color: var(--muted);
}

.status-pill.ok { background: #ecfdf5; color: #15803d; }
.status-pill.running { background: #eff6ff; color: #2563eb; }
.status-pill.danger { background: #fef2f2; color: #b91c1c; }

.material-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1439px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .material-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1023px) {
  .summary-grid,
  .material-grid {
    grid-template-columns: 1fr;
  }
}
</style>