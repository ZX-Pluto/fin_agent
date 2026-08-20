<template>
  <div class="page">
    <PageHeader title="主题管理" subtitle="主题包含模型字段与规则包，材料上传时按主题执行预审和分析">
      <template #actions>
        <el-button type="primary" @click="openCreate">新建主题</el-button>
      </template>
    </PageHeader>

    <div class="panel">
      <el-table :data="themes" v-loading="loading" border>
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="280" show-overflow-tooltip />
        <el-table-column label="模型字段" width="110">
          <template #default="{ row }">{{ row.model?.fields?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="规则包" width="100">
          <template #default="{ row }">{{ row.rulePackages?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!themes.length" description="暂无主题，请先新建" :image-size="80" />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑主题' : '新建主题'" width="960px" top="4vh">
      <el-tabs v-model="tab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="form" label-width="90px">
            <el-form-item label="主题编码">
              <el-input v-model="form.code" placeholder="如 OPERATION" />
            </el-form-item>
            <el-form-item label="主题名称">
              <el-input v-model="form.name" placeholder="如 经营情况" />
            </el-form-item>
            <el-form-item label="主题描述">
              <el-input v-model="form.description" type="textarea" :rows="3" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="模型" name="model">
          <div class="model-form">
            <el-input v-model="form.model.code" placeholder="模型编码，如 OPERATION_MODEL" style="width: 260px" />
            <el-input v-model="form.model.name" placeholder="模型名称，如 经营情况模型" style="width: 260px" />
          </div>
          <div class="section-head">
            <span>模型字段</span>
            <el-button size="small" type="primary" @click="addField">添加字段</el-button>
          </div>
          <el-table :data="form.model.fields" border max-height="360">
            <el-table-column label="字段编码" min-width="140">
              <template #default="{ row }"><el-input v-model="row.fieldCode" placeholder="revenue" /></template>
            </el-table-column>
            <el-table-column label="字段名称" min-width="120">
              <template #default="{ row }"><el-input v-model="row.fieldName" placeholder="收入" /></template>
            </el-table-column>
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-select v-model="row.fieldType" style="width: 100%">
                  <el-option label="number" value="number" />
                  <el-option label="string" value="string" />
                  <el-option label="date" value="date" />
                  <el-option label="boolean" value="boolean" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="90">
              <template #default="{ row }"><el-input v-model="row.unit" placeholder="亿元" /></template>
            </el-table-column>
            <el-table-column label="序号" width="80">
              <template #default="{ row }"><el-input-number v-model="row.seqNo" :min="1" controls-position="right" style="width: 100%" /></template>
            </el-table-column>
            <el-table-column label="说明" min-width="200">
              <template #default="{ row }"><el-input v-model="row.comment" placeholder="字段说明" /></template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ $index }">
                <el-button size="small" type="danger" link @click="form.model.fields.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="规则包" name="packages">
          <div class="section-head">
            <span>规则包</span>
            <el-button size="small" type="primary" @click="addPackage">添加规则包</el-button>
          </div>
          <div v-for="(pkg, pi) in form.rulePackages" :key="pi" class="pkg-box">
            <div class="pkg-head">
              <span class="pkg-title">规则包 {{ pi + 1 }}</span>
              <el-button size="small" type="danger" link @click="form.rulePackages.splice(pi, 1)">删除规则包</el-button>
            </div>
            <div class="pkg-form">
              <el-input v-model="pkg.code" placeholder="编码，如 PRE_AUDIT_OPERATION" />
              <el-input v-model="pkg.name" placeholder="名称，如 经营材料预审规则包" />
              <el-select v-model="pkg.packageType" style="width: 160px">
                <el-option label="预审规则" value="PRE_AUDIT" />
                <el-option label="专家经验" value="EXPERT" />
              </el-select>
            </div>
            <el-input v-model="pkg.description" placeholder="规则包说明" style="margin: 10px 0" />
            <div class="section-head">
              <span>规则条目</span>
              <el-button size="small" type="primary" @click="addItem(pi)">添加条目</el-button>
            </div>
            <el-table :data="pkg.items" border max-height="300">
              <el-table-column label="编码" min-width="130">
                <template #default="{ row }"><el-input v-model="row.ruleCode" placeholder="PRE_AUDIT_R01" /></template>
              </el-table-column>
              <el-table-column label="名称" min-width="130">
                <template #default="{ row }"><el-input v-model="row.name" placeholder="核心指标完整性" /></template>
              </el-table-column>
              <el-table-column label="类型" width="110">
                <template #default="{ row }"><el-input v-model="row.ruleType" placeholder="PRE_AUDIT" /></template>
              </el-table-column>
              <el-table-column label="等级" width="100">
                <template #default="{ row }">
                  <el-select v-model="row.severity" style="width: 100%">
                    <el-option v-for="s in severities" :key="s" :label="s" :value="s" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="执行策略" width="150">
                <template #default="{ row }">
                  <el-select v-model="row.executionStrategy" style="width: 100%">
                    <el-option label="AI" value="AI" />
                    <el-option label="AI + Deterministic" value="AI + Deterministic" />
                    <el-option label="AI + Tool" value="AI + Tool" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="说明" min-width="200">
                <template #default="{ row }"><el-input v-model="row.description" placeholder="规则说明" /></template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ $index }">
                  <el-button size="small" type="danger" link @click="pkg.items.splice($index, 1)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-if="!form.rulePackages.length" description="暂无规则包" :image-size="80" />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import { createTheme, deleteTheme, listThemes, updateTheme } from '../api'
import type { RulePackage, RulePackageItem, Theme, ThemeModelField } from '../types'

interface ThemeForm {
  id?: number
  code: string
  name: string
  description: string
  model: { code: string; name: string; fields: ThemeModelField[] }
  rulePackages: RulePackage[]
}

const themes = ref<Theme[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const tab = ref('basic')
const form = reactive<ThemeForm>(emptyForm())
const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

function emptyForm(): ThemeForm {
  return {
    code: '',
    name: '',
    description: '',
    model: {
      code: '',
      name: '',
      fields: []
    },
    rulePackages: []
  }
}

const load = async () => {
  loading.value = true
  try {
    themes.value = await listThemes()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, emptyForm())
  addField()
  tab.value = 'basic'
  dialog.value = true
}

const openEdit = (theme: Theme) => {
  Object.assign(form, {
    id: theme.id,
    code: theme.code,
    name: theme.name,
    description: theme.description || '',
    model: {
      code: theme.model?.code || '',
      name: theme.model?.name || '',
      fields: (theme.model?.fields || []).map((f) => ({ ...f }))
    },
    rulePackages: (theme.rulePackages || []).map((pkg) => ({
      ...pkg,
      items: (pkg.items || []).map((item) => ({ ...item }))
    }))
  })
  if (!form.model.fields.length) addField()
  tab.value = 'basic'
  dialog.value = true
}

const addField = () => {
  form.model.fields.push({
    id: undefined,
    modelId: undefined,
    fieldCode: '',
    fieldName: '',
    fieldType: 'number',
    unit: '',
    comment: '',
    seqNo: form.model.fields.length + 1
  })
}

const addPackage = () => {
  form.rulePackages.push({
    id: undefined,
    code: '',
    name: '',
    themeId: form.id,
    packageType: 'PRE_AUDIT',
    description: '',
    enabled: true,
    items: []
  })
}

const addItem = (pi: number) => {
  form.rulePackages[pi].items = form.rulePackages[pi].items || []
  form.rulePackages[pi].items.push({
    id: undefined,
    packageId: undefined,
    ruleCode: '',
    name: '',
    ruleType: 'PRE_AUDIT',
    scope: '经营情况',
    inputFields: '',
    executionStrategy: 'AI',
    description: '',
    severity: 'MEDIUM',
    enabled: true
  } as RulePackageItem)
}

const save = async () => {
  if (!form.code || !form.name) {
    ElMessage.warning('请填写主题编码和名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      code: form.code,
      name: form.name,
      description: form.description,
      model: {
        code: form.model.code,
        name: form.model.name,
        fields: form.model.fields.filter((f) => f.fieldCode && f.fieldName)
      },
      rulePackages: form.rulePackages.map((pkg) => ({
        code: pkg.code,
        name: pkg.name,
        packageType: pkg.packageType,
        description: pkg.description,
        enabled: true,
        items: (pkg.items || []).filter((item) => item.ruleCode && item.name)
      }))
    }
    if (form.id) {
      await updateTheme(form.id, payload)
    } else {
      await createTheme(payload)
    }
    ElMessage.success('主题保存成功')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const remove = async (theme: Theme) => {
  await ElMessageBox.confirm(`确认删除主题 ${theme.name}？`, '提示', { type: 'warning' })
  await deleteTheme(theme.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.model-form {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 14px 0 10px;
  font-weight: 600;
}
.pkg-box {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 14px;
}
.pkg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.pkg-title {
  font-weight: 600;
}
.pkg-form {
  display: flex;
  gap: 10px;
}
</style>
