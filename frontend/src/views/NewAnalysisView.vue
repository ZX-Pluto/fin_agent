<template>
  <div class="page">
    <PageHeader title="新建经营分析" subtitle="选择材料归属与主题，AI 将自动完成解析、预审、映射和分析">
      <template #actions>
        <el-button @click="$router.push('/materials')">返回经营材料</el-button>
      </template>
    </PageHeader>

    <div class="new-layout">
      <div class="panel">
        <div class="panel-title">① 材料归属</div>
        <el-form label-width="90px">
          <el-form-item label="地区部">
            <el-select v-model="region" allow-create filterable style="width: 100%">
              <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
              <el-option label="中国地区部" value="中国地区部" />
              <el-option label="亚太地区部" value="亚太地区部" />
              <el-option label="欧洲地区部" value="欧洲地区部" />
            </el-select>
          </el-form-item>
          <el-form-item label="代表处">
            <el-select v-model="organization" allow-create filterable style="width: 100%">
              <el-option v-for="o in orgOptions" :key="o" :label="o" :value="o" />
            </el-select>
          </el-form-item>
          <el-form-item label="期间">
            <el-select v-model="reportPeriod" allow-create filterable style="width: 100%">
              <el-option v-for="p in periodOptions" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <div class="panel">
        <div class="panel-title">② 上传材料</div>
        <el-upload drag :auto-upload="false" :limit="1" :on-change="onChange">
          <div class="upload-main">拖入 PPT / PDF / Word 文件</div>
          <div class="muted">每次新建分析上传一份材料</div>
        </el-upload>
        <div v-if="file" class="file-name">{{ file.name }}</div>
      </div>

      <div class="panel">
        <div class="panel-title">③ 分析主题</div>
        <el-select v-model="themeId" style="width: 100%">
          <el-option v-for="t in themes" :key="t.id" :label="`${t.name} · ${t.description || t.code}`" :value="t.id" />
        </el-select>
        <div v-if="selectedTheme" class="theme-desc">{{ selectedTheme.description }}</div>
        <el-button type="primary" size="large" style="width: 100%; margin-top: 18px" :loading="submitting" @click="submit">
          开始分析
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '../components/common/PageHeader.vue'
import { listMaterials, listThemes, uploadMaterial } from '../api'
import type { Material, Theme } from '../types'

const router = useRouter()
const materials = ref<Material[]>([])
const themes = ref<Theme[]>([])
const region = ref('中国地区部')
const organization = ref('')
const reportPeriod = ref('')
const themeId = ref<number>()
const file = ref<File>()
const submitting = ref(false)

const regionOptions = computed(() => [...new Set(materials.value.map((m) => m.region || '默认地区部'))])
const orgOptions = computed(() => [...new Set(materials.value.map((m) => m.organization).filter(Boolean))] as string[])
const periodOptions = computed(() => [...new Set(materials.value.map((m) => m.reportPeriod).filter(Boolean))] as string[])
const selectedTheme = computed(() => themes.value.find((t) => t.id === themeId.value))

const onChange = (item: any) => {
  file.value = item.raw as File
}

const submit = async () => {
  if (!file.value) {
    ElMessage.warning('请选择要分析的材料文件')
    return
  }
  if (!organization.value || !reportPeriod.value) {
    ElMessage.warning('请填写代表处和期间')
    return
  }
  if (!themeId.value) {
    ElMessage.warning('请选择分析主题')
    return
  }
  submitting.value = true
  try {
    const result = await uploadMaterial(file.value, region.value, organization.value, reportPeriod.value, themeId.value)
    ElMessage.success('分析任务已创建，AI 正在自动处理')
    router.push('/materials')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  materials.value = await listMaterials()
  themes.value = await listThemes()
  themeId.value = themes.value[0]?.id
  organization.value = orgOptions.value[0] || ''
  reportPeriod.value = periodOptions.value[0] || ''
})
</script>

<style scoped>
.new-layout {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  align-items: start;
}
.upload-main {
  font-size: 15px;
  color: #374151;
  margin-bottom: 6px;
}
.file-name {
  margin-top: 10px;
  font-size: 13px;
  color: var(--brand);
}
.theme-desc {
  margin-top: 10px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1439px) {
  .new-layout {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 1023px) {
  .new-layout {
    grid-template-columns: 1fr;
  }
}
</style>
