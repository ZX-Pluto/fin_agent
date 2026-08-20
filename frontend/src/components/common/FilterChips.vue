<template>
  <div class="filter-chips">
    <el-tag
      v-for="option in options"
      :key="option.value"
      :type="modelValue === option.value ? 'primary' : 'info'"
      effect="plain"
      class="filter-chip"
      @click="emit('update:modelValue', option.value)"
    >
      {{ option.label }}
    </el-tag>
  </div>
</template>

<script setup lang="ts">
export interface FilterOption {
  value: string
  label: string
}

defineProps<{ modelValue: string; options: FilterOption[] }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()
</script>

<style scoped>
.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.filter-chip {
  cursor: pointer;
  user-select: none;
}
</style>
