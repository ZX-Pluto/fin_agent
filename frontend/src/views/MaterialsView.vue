<template>
  <div class="page">
    <PageHeader title="经营材料" subtitle="按地区部 → 期间 → 代表处归集材料与经营分析结果">
      <template #actions>
        <el-button type="primary" @click="$router.push('/materials/new')">新建分析</el-button>
      </template>
    </PageHeader>

    <div class="panel toolbar">
      <FilterChips v-model="statusFilter" :options="statusChips" />
      <div class="search-row">
        <el-input v-model="keyword" clearable placeholder="搜索代表处 / 材料" style="width: 240px" />
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
        <div v-for="region in regionGroups" :key="region.name" class="region-block">
          <div class="region-head" @click="toggleRegion(region.name)">
            <span class="collapse-icon">{{ expandedRegions[region.name] ? '▾' : '▸' }}</span>
            <span class="region-name">{{ region.name }}</span>
            <span class="muted">{{ region.count }} 份材料</span>
            <span class="region-stats muted">{{ regionStatusText(region.name) }}</span>
          </div>
          <div v-show="expandedRegions[region.name]" class="region-body">
            <div v-for="period in region.periods" :key="period.name" class="period-block">
              <div class="period-head" @click="togglePeriod(periodKey(region.name, period.name))">
                <span class="collapse-icon">{{ expandedPeriods[periodKey(region.name, period.name)] ? '▾' : '▸' }}</span>
                <span class="period-name">{{ period.name }}</span>
                <span class="muted">{{ period.count }} 份材料</span>
              </div>
              <div v-show="expandedPeriods[periodKey(region.name, period.name)]" class="period-body">
                <div v-for="org in period.orgs" :key="org.name" class="org-block">
                  <div class="org-head">{{ org.name }}</div>
                  <div class="material-grid">
                    <MaterialCard
                      v-for="m in org.materials"
                      :key="m.id"
                      :material="m"
                      :summary="summaries[m.id]"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <EmptyState
        v-else
        title="暂无经营材料"
        description="新建分析后，材料会按地区部与期间自动归集。"
      >
        <el-button type="primary" @click="$router.push('/materials/new')">新建分析</el-button>
      </EmptyState>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import FilterChips from '../components/common/FilterChips.vue'
import EmptyState from '../components/common/EmptyState.vue'
import MaterialCard from '../components/material/MaterialCard.vue'
import { getSummary, listMaterials } from '../api'
import type { Material, Summary } from '../types'

interface OrgGroup {
  name: string
  materials: Material[]
}

interface PeriodGroup {
  name: string
  count: number
  orgs: OrgGroup[]
}

interface RegionGroup {
  name: string
  count: number
  periods: PeriodGroup[]
}

const materials = ref<Material[]>([])
const summaries = ref<Record<number, Summary>>({})
const loading = ref(false)
const keyword = ref('')
const regionFilter = ref('')
const periodFilter = ref('')
const statusFilter = ref('ALL')
const expandedRegions = reactive<Record<string, boolean>>({})
const expandedPeriods = reactive<Record<string, boolean>>({})

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
    const periodName = m.reportPeriod || '未标注期间'
    const orgName = m.organization || '未命名代表处'
    let region = groups.find((g) => g.name === regionName)
    if (!region) {
      region = { name: regionName, count: 0, periods: [] }
      groups.push(region)
    }
    let period = region.periods.find((p) => p.name === periodName)
    if (!period) {
      period = { name: periodName, count: 0, orgs: [] }
      region.periods.push(period)
    }
    let org = period.orgs.find((o) => o.name === orgName)
    if (!org) {
      org = { name: orgName, materials: [] }
      period.orgs.push(org)
    }
    org.materials.push(m)
    period.count += 1
    region.count += 1
  }
  return groups
})

function countBy(status: string) {
  return materials.value.filter((m) => m.status === status).length
}

function countRunning() {
  return materials.value.filter((m) => runningStatuses.includes(m.status)).length
}

function regionStatusText(region: string) {
  const list = filteredMaterials.value.filter((m) => (m.region || '默认地区部') === region)
  const completed = list.filter((m) => m.status === 'COMPLETED').length
  const running = list.filter((m) => runningStatuses.includes(m.status)).length
  const failed = list.filter((m) => m.status === 'FAILED').length
  return `已完成 ${completed} · 处理中 ${running} · 失败 ${failed}`
}

function periodKey(region: string, period: string) {
  return `${region}|${period}`
}

function toggleRegion(name: string) {
  expandedRegions[name] = !expandedRegions[name]
}

function togglePeriod(key: string) {
  expandedPeriods[key] = !expandedPeriods[key]
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
    for (const region of regionGroups.value) {
      if (expandedRegions[region.name] === undefined) expandedRegions[region.name] = true
      for (const period of region.periods) {
        const key = periodKey(region.name, period.name)
        if (expandedPeriods[key] === undefined) expandedPeriods[key] = true
      }
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
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
.region-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 14px;
}
.region-block,
.period-block,
.org-block {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 14px;
}
.region-head,
.period-head {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.region-name {
  font-size: 17px;
  font-weight: 700;
}
.period-name {
  font-size: 15px;
  font-weight: 600;
}
.collapse-icon {
  color: var(--muted);
  width: 16px;
}
.region-stats {
  margin-left: auto;
  font-size: 12px;
}
.region-body,
.period-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}
.org-head {
  font-size: 14px;
  font-weight: 600;
  color: var(--muted);
  margin-bottom: 10px;
}
.material-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
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
