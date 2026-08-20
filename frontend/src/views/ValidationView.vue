<template>
  <div class="page">
    <PageHeader title="智能预审" subtitle="规则引擎 + 大模型语义解释，异常发现与证据定位">
      <template #actions>
        <span v-if="materialId" class="current-material">当前材料：{{ currentMaterialLabel }}</span>
        <el-button v-if="materialId" @click="$router.push(`/materials/${materialId}`)">返回材料</el-button>
        <el-select v-model="materialId" clearable placeholder="全部材料" style="width: 220px" @change="onMaterialChange">
          <el-option v-for="m in materials" :key="m.id" :label="`${m.organization} ${m.reportPeriod}`" :value="m.id" />
        </el-select>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard label="当前问题总数" :value="filteredFindings.length" />
      <StatCard label="待人工确认" :value="countStatus('PENDING')" tone="warning" />
      <StatCard label="已确认异常" :value="countStatus('CONFIRMED')" tone="ok" />
      <StatCard label="误报" :value="countStatus('FALSE_POSITIVE')" tone="danger" />
    </div>

    <div class="panel">
      <FilterChips v-model="filter" :options="chips" />
    </div>

    <div class="finding-list" v-loading="loading">
      <ValidationCard
        v-for="f in filteredFindings"
        :key="f.id"
        :finding="f"
        :org-text="orgText(f)"
        :period-text="periodText(f)"
        :material-id="f.materialId"
        @evidence="showEvidence"
        @material="goMaterial"
        @confirm="confirm"
        @ignore="ignore"
      />
      <div v-if="!filteredFindings.length" class="panel empty-state">暂无匹配的预审问题</div>
    </div>

    <ValidationEvidence v-model="evidenceDialog" :source-refs="evidenceRefs" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../components/common/PageHeader.vue'
import StatCard from '../components/common/StatCard.vue'
import FilterChips from '../components/common/FilterChips.vue'
import ValidationCard from '../components/validation/ValidationCard.vue'
import ValidationEvidence from '../components/validation/ValidationEvidence.vue'
import { confirmValidation, ignoreValidation, listMaterials, listValidations } from '../api'
import type { Material, ValidationResult } from '../types'

const route = useRoute()
const router = useRouter()
const materials = ref<Material[]>([])
const findings = ref<ValidationResult[]>([])
const loading = ref(false)
const materialId = ref<number>()
const filter = ref('ALL')
const evidenceDialog = ref(false)
const evidenceRefs = ref('')

const chips = [
  { value: 'ALL', label: '全部' },
  { value: 'HIGH_RISK', label: '高风险' },
  { value: 'COMPLETENESS', label: '完整性检查' },
  { value: 'CREDIBILITY', label: '可信性校验' },
  { value: 'REASONABLENESS', label: '合理性' },
  { value: 'CONSISTENCY', label: '数据一致性' },
  { value: 'PENDING', label: '待人工确认' }
]

const load = async () => {
  loading.value = true
  try {
    findings.value = await listValidations({
      materialId: materialId.value || undefined
    })
  } finally {
    loading.value = false
  }
}

const filteredFindings = computed(() => {
  const base = uniqueFindings.value
  if (filter.value === 'ALL') return base
  if (filter.value === 'HIGH_RISK') return base.filter((f) => ['CRITICAL', 'HIGH'].includes(f.severity))
  if (filter.value === 'PENDING') return base.filter((f) => f.status === 'PENDING')
  return base.filter((f) => f.category === filter.value)
})

const uniqueFindings = computed(() => {
  const seen = new Set<string>()
  return findings.value.filter((f) => {
    const key = `${f.ruleCode || ''}|${f.metricName || ''}|${f.message}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})

const countStatus = (status: string) => filteredFindings.value.filter((f) => f.status === status).length
const currentMaterialLabel = computed(() => {
  const m = materials.value.find((x) => x.id === materialId.value)
  return m ? `${m.organization || '材料'} · ${m.reportPeriod || ''}` : ''
})

const orgText = (f: ValidationResult) => {
  const m = materials.value.find((x) => x.id === f.materialId)
  return m?.organization || `材料 ${f.materialId}`
}
const periodText = (f: ValidationResult) => {
  const m = materials.value.find((x) => x.id === f.materialId)
  return m?.reportPeriod || ''
}

const onMaterialChange = (value: number | undefined) => {
  materialId.value = value
  router.replace({ path: route.path, query: value ? { materialId: String(value) } : {} })
  load()
}

const confirm = async (f: ValidationResult) => {
  await confirmValidation(f.id)
  await load()
}

const ignore = async (f: ValidationResult) => {
  await ignoreValidation(f.id)
  await load()
}

const showEvidence = (f: ValidationResult) => {
  evidenceRefs.value = f.sourceRefs || ''
  evidenceDialog.value = true
}

const goMaterial = (f: ValidationResult) => {
  router.push(`/materials/${f.materialId}`)
}

onMounted(async () => {
  materials.value = await listMaterials()
  const queryId = Number(route.query.materialId)
  if (queryId) {
    materialId.value = queryId
  }
  await load()
})
</script>

<style scoped>
.current-material {
  font-size: 13px;
  color: var(--muted);
  margin-right: 4px;
}
.finding-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
