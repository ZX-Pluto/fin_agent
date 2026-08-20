<template>
  <div class="page">
    <PageHeader title="模型管理" subtitle="模型是 AI 事实映射的目标数据结构，随主题一起维护">
      <template #actions>
        <el-button type="primary" @click="$router.push('/themes')">在主题中编辑模型</el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="model-list">
      <div v-for="theme in themes" :key="theme.id" class="panel model-card">
        <div class="model-head">
          <div>
            <div class="model-name">{{ theme.model?.name || '未配置模型' }}</div>
            <div class="muted">{{ theme.name }} · {{ theme.model?.fields?.length || 0 }} 个字段</div>
          </div>
          <el-button size="small" type="primary" link @click="$router.push('/themes')">编辑</el-button>
        </div>
        <el-table :data="theme.model?.fields || []" border max-height="360" size="small">
          <el-table-column prop="fieldCode" label="编码" min-width="140" />
          <el-table-column prop="fieldName" label="名称" min-width="120" />
          <el-table-column prop="fieldType" label="类型" width="100" />
          <el-table-column prop="unit" label="单位" width="90" />
          <el-table-column prop="comment" label="说明" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import { listThemes } from '../api'
import type { Theme } from '../types'

const themes = ref<Theme[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    themes.value = await listThemes()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.model-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.model-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}
.model-name {
  font-size: 17px;
  font-weight: 700;
}
</style>
