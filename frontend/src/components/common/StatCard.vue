<template>
  <div class="stat-card">
    <div class="label">{{ label }}</div>
    <div class="value" :class="tone">{{ value }}</div>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: number | string
    tone?: 'ok' | 'warning' | 'danger' | 'brand'
    hint?: string
  }>(),
  { tone: 'brand' }
)
</script>

<style scoped>
.stat-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 16px;
}
.label {
  font-size: 12px;
  color: var(--muted);
}
.value {
  font-size: 26px;
  font-weight: 700;
  margin-top: 4px;
}
.value.ok {
  color: var(--ok);
}
.value.warning {
  color: var(--warning);
}
.value.danger {
  color: var(--danger);
}
.value.brand {
  color: var(--brand);
}
.hint {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
}
</style>
