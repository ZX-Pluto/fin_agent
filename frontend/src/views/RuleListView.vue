<template>
  <div class="page">
    <div class="panel">
      <div class="panel-head">
        <div class="panel-title">规则包</div>
        <div class="head-actions">
          <el-select v-model="themeId" style="width: 200px" @change="load">
            <el-option v-for="t in themes" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-button type="primary" @click="openCreatePackage">新增规则包</el-button>
        </div>
      </div>
      <el-table :data="packages" v-loading="loading" border>
        <el-table-column prop="code" label="编码" width="180" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ packageTypeText(row.packageType) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="togglePackage(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="selectPackage(row)">规则条目</el-button>
            <el-button size="small" type="primary" link @click="openEditPackage(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="removePackage(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="selectedPackage" class="panel">
      <div class="panel-head">
        <div class="panel-title">{{ selectedPackage.name }} · 规则条目</div>
        <el-button type="primary" @click="openCreateItem">新增规则条目</el-button>
      </div>
      <el-table :data="items" v-loading="itemLoading" border>
        <el-table-column prop="ruleCode" label="编码" width="150" />
        <el-table-column prop="name" label="名称" min-width="170" />
        <el-table-column prop="ruleType" label="规则类型" width="120" />
        <el-table-column prop="executionStrategy" label="执行策略" width="130" />
        <el-table-column prop="scope" label="适用范围" min-width="130" show-overflow-tooltip />
        <el-table-column label="等级" width="90">
          <template #default="{ row }"><StatusTag :status="row.severity" /></template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="toggleItem(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditItem(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!items.length" description="暂无规则条目" :image-size="80" />
    </div>

    <el-dialog v-model="packageDialog" :title="packageForm.id ? '编辑规则包' : '新增规则包'" width="560px">
      <el-form :model="packageForm" label-width="100px">
        <el-form-item label="编码">
          <el-input v-model="packageForm.code" :disabled="!!packageForm.id" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="packageForm.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="packageForm.packageType" style="width: 100%">
            <el-option label="预审规则" value="PRE_AUDIT" />
            <el-option label="专家经验" value="EXPERT" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="packageForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="packageForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="packageDialog = false">取消</el-button>
        <el-button type="primary" @click="savePackage">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialog" :title="itemForm.id ? '编辑规则条目' : '新增规则条目'" width="640px">
      <el-form :model="itemForm" label-width="110px">
        <el-form-item label="规则编码">
          <el-input v-model="itemForm.ruleCode" :disabled="!!itemForm.id" />
        </el-form-item>
        <el-form-item label="规则名称">
          <el-input v-model="itemForm.name" />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-input v-model="itemForm.ruleType" placeholder="PRE_AUDIT / EXPERT" />
        </el-form-item>
        <el-form-item label="适用范围">
          <el-input v-model="itemForm.scope" />
        </el-form-item>
        <el-form-item label="输入字段">
          <el-input v-model="itemForm.inputFields" placeholder="revenue,profit" />
        </el-form-item>
        <el-form-item label="执行策略">
          <el-select v-model="itemForm.executionStrategy" style="width: 100%">
            <el-option label="AI" value="AI" />
            <el-option label="AI + Deterministic" value="AI + Deterministic" />
            <el-option label="AI + Tool" value="AI + Tool" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="itemForm.severity" style="width: 100%">
            <el-option v-for="s in severities" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="itemForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="itemForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createRulePackage,
  createRulePackageItem,
  deleteRulePackage,
  deleteRulePackageItem,
  listRulePackages,
  listThemes,
  toggleRulePackage,
  toggleRulePackageItem,
  updateRulePackage,
  updateRulePackageItem
} from '../api'
import type { RulePackage, RulePackageItem, Theme } from '../types'
import StatusTag from '../components/common/StatusTag.vue'

const themes = ref<Theme[]>([])
const packages = ref<RulePackage[]>([])
const themeId = ref<number>()
const loading = ref(false)
const itemLoading = ref(false)
const selectedPackage = ref<RulePackage>()
const items = ref<RulePackageItem[]>([])
const packageDialog = ref(false)
const itemDialog = ref(false)
const packageForm = reactive<Partial<RulePackage>>({})
const itemForm = reactive<Partial<RulePackageItem>>({})
const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

const packageTypeText = (type?: string) => (type === 'PRE_AUDIT' ? '预审规则' : type === 'EXPERT' ? '专家经验' : type || '-')

const loadBase = async () => {
  themes.value = await listThemes()
  themeId.value = themeId.value ?? themes.value[0]?.id
}

const load = async () => {
  if (!themeId.value) return
  loading.value = true
  try {
    packages.value = await listRulePackages(themeId.value)
    selectedPackage.value = selectedPackage.value && packages.value.find((p) => p.id === selectedPackage.value?.id)
    if (selectedPackage.value) await loadItems()
  } finally {
    loading.value = false
  }
}

const loadItems = async () => {
  if (!selectedPackage.value) return
  itemLoading.value = true
  try {
    items.value = selectedPackage.value.items || []
  } finally {
    itemLoading.value = false
  }
}

const openCreatePackage = () => {
  Object.assign(packageForm, {
    id: undefined,
    code: '',
    name: '',
    themeId: themeId.value,
    packageType: 'PRE_AUDIT',
    description: '',
    enabled: true
  })
  packageDialog.value = true
}

const openEditPackage = (row: RulePackage) => {
  Object.assign(packageForm, row)
  packageDialog.value = true
}

const savePackage = async () => {
  if (!packageForm.code || !packageForm.name) {
    ElMessage.warning('请填写编码和名称')
    return
  }
  if (packageForm.id) {
    await updateRulePackage(packageForm.id, packageForm)
  } else {
    await createRulePackage(packageForm)
  }
  ElMessage.success('保存成功')
  packageDialog.value = false
  await load()
}

const togglePackage = async (row: RulePackage) => {
  await toggleRulePackage(row.id)
  await load()
}

const removePackage = async (row: RulePackage) => {
  await ElMessageBox.confirm(`确认删除规则包 ${row.name}？其下规则条目会一并删除`, '提示', { type: 'warning' })
  await deleteRulePackage(row.id)
  if (selectedPackage.value?.id === row.id) selectedPackage.value = undefined
  ElMessage.success('已删除')
  await load()
}

const selectPackage = (row: RulePackage) => {
  selectedPackage.value = row
  items.value = row.items || []
}

const openCreateItem = () => {
  Object.assign(itemForm, {
    id: undefined,
    ruleCode: '',
    name: '',
    ruleType: selectedPackage.value?.packageType === 'PRE_AUDIT' ? 'PRE_AUDIT' : 'EXPERT',
    scope: '经营情况',
    inputFields: '',
    executionStrategy: 'AI',
    description: '',
    severity: 'MEDIUM',
    enabled: true
  })
  itemDialog.value = true
}

const openEditItem = (row: RulePackageItem) => {
  Object.assign(itemForm, row)
  itemDialog.value = true
}

const saveItem = async () => {
  if (!selectedPackage.value || !itemForm.ruleCode || !itemForm.name) {
    ElMessage.warning('请填写规则编码和名称')
    return
  }
  if (itemForm.id) {
    await updateRulePackageItem(selectedPackage.value.id, itemForm.id, itemForm)
  } else {
    await createRulePackageItem(selectedPackage.value.id, itemForm)
  }
  ElMessage.success('保存成功')
  itemDialog.value = false
  await load()
}

const toggleItem = async (row: RulePackageItem) => {
  if (!selectedPackage.value) return
  await toggleRulePackageItem(selectedPackage.value.id, row.id!)
  await load()
}

const removeItem = async (row: RulePackageItem) => {
  if (!selectedPackage.value) return
  await ElMessageBox.confirm(`确认删除规则条目 ${row.ruleCode}?`, '提示', { type: 'warning' })
  await deleteRulePackageItem(selectedPackage.value.id, row.id!)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await loadBase()
  await load()
})
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
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
