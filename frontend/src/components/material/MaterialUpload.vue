<template>
  <el-dialog v-model="visible" title="导入经营材料" width="560px" destroy-on-close>
    <el-upload drag :auto-upload="false" :limit="10" multiple :on-change="onChange" style="margin-bottom: 14px">
      <div class="upload-main">拖入 PPT / PDF / Word / Excel 文件</div>
      <div class="muted">支持批量上传，可一次导入多份材料</div>
    </el-upload>
    <div v-if="files.length" class="file-list">
      <div v-for="(f, i) in files" :key="i" class="file-item">
        <span class="file-name">{{ f.name }}</span>
        <el-button text type="danger" @click="files.splice(i, 1)">移除</el-button>
      </div>
    </div>
    <el-form label-width="90px">
      <el-form-item label="地区部">
        <el-select v-model="region" allow-create filterable default-first-option placeholder="如 中国地区部" style="width: 100%">
          <el-option label="中国地区部" value="中国地区部" />
          <el-option label="亚太地区部" value="亚太地区部" />
          <el-option label="欧洲地区部" value="欧洲地区部" />
          <el-option label="默认地区部" value="默认地区部" />
        </el-select>
      </el-form-item>
      <el-form-item label="代表处">
        <el-input v-model="organization" placeholder="自动识别或手动填写" />
      </el-form-item>
      <el-form-item label="报告期间">
        <el-input v-model="reportPeriod" placeholder="如 2026Q2" />
      </el-form-item>
      <el-form-item label="分析主题">
        <el-select v-model="themeId" placeholder="选择分析主题" style="width: 100%">
          <el-option v-for="t in themes" :key="t.id" :label="`${t.name}（${t.description || t.code}）`" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理方式">
        <div class="checks">
          <el-checkbox :model-value="true">数据一致性检查</el-checkbox>
          <el-checkbox :model-value="true">经营信息提取</el-checkbox>
          <el-checkbox :model-value="true">风险与亮点识别</el-checkbox>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" :disabled="!files.length" @click="submit">开始智能处理</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { Theme } from '../../types'

const props = defineProps<{ modelValue: boolean; loading?: boolean; themes: Theme[] }>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'upload', payload: { files: File[]; region: string; organization: string; reportPeriod: string; themeId: number }): void
}>()

const visible = ref(props.modelValue)
watch(
  () => props.modelValue,
  (v) => {
    visible.value = v
  }
)
watch(visible, (v) => emit('update:modelValue', v))

const files = ref<File[]>([])
const region = ref('中国地区部')
const organization = ref('')
const reportPeriod = ref('')
const themeId = ref<number>()

watch(
  () => props.themes,
  (list) => {
    if (!themeId.value && list.length) {
      themeId.value = list[0].id
    }
  },
  { immediate: true }
)

const onChange = (item: any) => {
  const raw = item.raw as File
  if (raw && !files.value.some((f) => f.name === raw.name)) {
    files.value.push(raw)
  }
}

const close = () => {
  visible.value = false
}

const submit = () => {
  if (!themeId.value) {
    ElMessage.warning('请选择分析主题')
    return
  }
  emit('upload', {
    files: [...files.value],
    region: region.value,
    organization: organization.value,
    reportPeriod: reportPeriod.value,
    themeId: themeId.value
  })
}
</script>

<style scoped>
.upload-main {
  font-size: 15px;
  color: #374151;
  margin-bottom: 6px;
}
.file-list {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  max-height: 160px;
  overflow: auto;
}
.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  padding: 4px 0;
}
.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.checks {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
