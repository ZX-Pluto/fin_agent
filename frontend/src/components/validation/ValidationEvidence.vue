<template>
  <el-dialog v-model="visible" title="异常证据" width="620px">
    <div v-if="!rows.length" class="muted">无结构化证据</div>
    <div v-for="(row, i) in rows" :key="i" class="evidence-item">
      <div class="evidence-label">来源 {{ i + 1 }}</div>
      <pre class="evidence-json">{{ JSON.stringify(row, null, 2) }}</pre>
    </div>
    <pre v-if="raw && !rows.length" class="evidence-json">{{ raw }}</pre>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{ modelValue: boolean; sourceRefs?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const visible = ref(props.modelValue)
watch(
  () => props.modelValue,
  (v) => (visible.value = v)
)
watch(visible, (v) => emit('update:modelValue', v))

const raw = computed(() => props.sourceRefs || '')
const rows = computed(() => {
  if (!props.sourceRefs) return []
  try {
    const parsed = JSON.parse(props.sourceRefs)
    return Array.isArray(parsed) ? parsed : [parsed]
  } catch {
    return []
  }
})
</script>

<style scoped>
.evidence-item {
  margin-bottom: 10px;
}
.evidence-label {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
}
.evidence-json {
  white-space: pre-wrap;
  word-break: break-all;
  background: #f8fafc;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 10px;
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
}
</style>
