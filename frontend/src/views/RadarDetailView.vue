<template>
  <div class="page" v-loading="loading">
    <div class="radar-detail-head">
      <el-page-header :content="pageTitle" @back="$router.push('/radar')" />
      <div class="head-actions">
        <StatusTag :status="material?.status || ''" />
        <el-button @click="goProcess">处理详情</el-button>
        <el-button type="primary" @click="goMaterial">打开材料分析</el-button>
      </div>
    </div>

    <template v-if="material?.status === 'COMPLETED'">
      <BusinessBriefing :material-id="materialId" @view-detail="goProcess" />
    </template>
    <div v-else class="panel missing-panel">
      <el-empty :description="loadError ? '未找到该材料' : '该材料尚未生成经营简报'" :image-size="90">
        <el-button v-if="material" type="primary" @click="goMaterial">查看处理状态</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StatusTag from '../components/common/StatusTag.vue'
import BusinessBriefing from '../components/briefing/BusinessBriefing.vue'
import { getMaterial } from '../api'
import type { Material } from '../types'

const route = useRoute()
const router = useRouter()
const materialId = Number(route.params.materialId)

const material = ref<Material>()
const loading = ref(false)
const loadError = ref(false)

const pageTitle = computed(() =>
  `${material.value?.organization || '代表处'} · ${material.value?.reportPeriod || '经营简报'}`
)

const goProcess = () => router.push(`/materials/${materialId}?tab=process`)
const goMaterial = () => router.push(`/materials/${materialId}`)

onMounted(async () => {
  loading.value = true
  try {
    material.value = await getMaterial(materialId)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.radar-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 12px 16px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.missing-panel {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>