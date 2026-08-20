<template>
  <div class="page">
    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">模型配置管理</div>
        <el-button type="primary" @click="openCreate">新增模型</el-button>
      </div>
      <el-table :data="models" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="provider" label="Provider" width="160" />
        <el-table-column prop="modelName" label="模型" min-width="160" />
        <el-table-column prop="capabilities" label="能力" width="120" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="220" show-overflow-tooltip />
        <el-table-column prop="apiKey" label="API Key" width="110" show-overflow-tooltip />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="toggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="test(row)">测试</el-button>
            <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑模型' : '新增模型'" width="640px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Provider">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option label="OPENAI_COMPATIBLE" value="OPENAI_COMPATIBLE" />
            <el-option label="ANTHROPIC_COMPATIBLE" value="ANTHROPIC_COMPATIBLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="http://host/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="模型名称">
          <el-input v-model="form.modelName" placeholder="qwen-v3-32b" />
        </el-form-item>
        <el-form-item label="能力">
          <el-select v-model="form.capabilities" style="width: 100%">
            <el-option label="文本 TEXT" value="TEXT" />
            <el-option label="视觉 VISION" value="VISION" />
            <el-option label="文本+视觉" value="TEXT,VISION" />
          </el-select>
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="超时秒数">
          <el-input-number v-model="form.timeoutSeconds" :min="5" :max="600" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createModel, deleteModel, listModels, testModel, toggleModel, updateModel } from '../api'
import type { ModelConfig } from '../types'

const models = ref<ModelConfig[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<Partial<ModelConfig>>({})

const load = async () => {
  loading.value = true
  try {
    models.value = await listModels()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, {
    id: undefined,
    name: '',
    provider: 'OPENAI_COMPATIBLE',
    baseUrl: '',
    apiKey: '',
    modelName: '',
    capabilities: 'TEXT',
    temperature: 0.3,
    timeoutSeconds: 60,
    enabled: true
  })
  dialog.value = true
}

const openEdit = (row: ModelConfig) => {
  Object.assign(form, row)
  dialog.value = true
}

const save = async () => {
  if (!form.name || !form.baseUrl || !form.modelName) {
    ElMessage.warning('请填写名称、Base URL 和模型名称')
    return
  }
  if (form.id) {
    await updateModel(form.id, form)
  } else {
    await createModel(form)
  }
  ElMessage.success('保存成功')
  dialog.value = false
  await load()
}

const toggle = async (row: ModelConfig) => {
  await toggleModel(row.id)
  await load()
}

const test = async (row: ModelConfig) => {
  const result = await testModel(row.id)
  if (result.success) {
    ElMessage.success(result.message)
  } else {
    ElMessage.warning(result.message)
  }
}

const remove = async (row: ModelConfig) => {
  await ElMessageBox.confirm(`确认删除模型配置 ${row.name}?`, '提示', { type: 'warning' })
  await deleteModel(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
