<template>
  <div class="empty-state">
    <div class="icon">○</div>
    <div class="title">{{ title }}</div>
    <div class="desc">{{ description }}</div>
    <div class="action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; description: string }>()
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 48px 20px;
  color: var(--muted);
}
.icon {
  font-size: 28px;
  color: #c0c7d1;
  margin-bottom: 10px;
}
.title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 6px;
}
.desc {
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
