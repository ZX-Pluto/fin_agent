<template>
  <div class="app-shell" :class="{ collapsed }">
    <header class="app-header">
      <div class="header-left">
        <el-button text class="collapse-btn" @click="collapsed = !collapsed">
          <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        </el-button>
        <div>
          <div class="app-brand">AI 经营材料智能处理</div>
          <div class="app-subtitle">经营材料 · 经营雷达 · 经营追问 · 事项闭环</div>
        </div>
      </div>
      <div class="app-status">
        <span class="dot"></span>
        <span class="status-text">AI 服务运行中</span>
      </div>
    </header>
    <div class="app-body">
      <aside class="app-side">
        <div class="menu-group">业务</div>
        <el-menu :default-active="activeMenu" router class="side-menu">
          <el-menu-item index="/materials">
            <el-icon><Files /></el-icon>
            <span class="menu-label">经营材料</span>
          </el-menu-item>
          <el-menu-item index="/radar">
            <el-icon><DataAnalysis /></el-icon>
            <span class="menu-label">经营雷达</span>
          </el-menu-item>
          <el-menu-item index="/qa">
            <el-icon><Lightning /></el-icon>
            <span class="menu-label">经营追问</span>
          </el-menu-item>
          <el-menu-item index="/follow-ups">
            <el-icon><List /></el-icon>
            <span class="menu-label">事项跟踪</span>
          </el-menu-item>
        </el-menu>
        <div class="menu-group">配置中心</div>
        <el-menu :default-active="activeMenu" router class="side-menu">
          <el-sub-menu index="/settings">
            <template #title><el-icon><Setting /></el-icon><span class="menu-label">配置中心</span></template>
            <el-menu-item index="/themes"><el-icon><Collection /></el-icon><span class="menu-label">主题管理</span></el-menu-item>
            <el-menu-item index="/rules"><el-icon><Document /></el-icon><span class="menu-label">规则包管理</span></el-menu-item>
            <el-menu-item index="/models-def"><el-icon><Cpu /></el-icon><span class="menu-label">模型管理</span></el-menu-item>
          </el-sub-menu>
        </el-menu>
        <div class="menu-group">辅助</div>
        <el-menu :default-active="activeMenu" router class="side-menu">
          <el-menu-item index="/tasks">
            <el-icon><List /></el-icon>
            <span class="menu-label">处理记录</span>
          </el-menu-item>
        </el-menu>
      </aside>
      <main class="app-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Collection, Cpu, DataAnalysis, Document, Expand, Files, Fold, Lightning, List, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const collapsed = ref(false)

const activeMenu = computed(() => {
  if (route.path.startsWith('/materials')) return '/materials'
  if (route.path.startsWith('/radar')) return '/radar'
  if (route.path.startsWith('/qa')) return '/qa'
  if (route.path.startsWith('/follow-ups')) return '/follow-ups'
  if (route.path.startsWith('/rules')) return '/rules'
  if (route.path.startsWith('/themes')) return '/themes'
  if (route.path.startsWith('/models-def')) return '/models-def'
  return route.path
})
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.app-header {
  height: 60px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 0 0 auto;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.collapse-btn {
  font-size: 18px;
  color: var(--muted);
}
.app-brand {
  font-size: 17px;
  font-weight: 700;
}
.app-subtitle {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}
.app-status {
  font-size: 13px;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ok);
}
.app-body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.app-side {
  width: 220px;
  background: #fff;
  border-right: 1px solid var(--line);
  padding: 10px 0;
  overflow: auto;
  flex: 0 0 auto;
  transition: width 0.2s ease;
}
.collapsed .app-side {
  width: 64px;
}
.collapsed .menu-label {
  display: none;
}
.collapsed .menu-group {
  text-align: center;
  padding-left: 0;
  padding-right: 0;
}
.menu-group {
  font-size: 12px;
  color: var(--muted);
  padding: 12px 16px 4px;
}
.side-menu {
  border-right: none;
}
.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
}
.side-menu :deep(.el-menu-item.is-active) {
  background: var(--brand-weak);
  color: var(--brand);
  font-weight: 600;
}
.side-link {
  display: block;
  color: var(--muted);
  text-decoration: none;
  font-size: 13px;
  padding: 8px 16px;
}
.side-link:hover {
  color: var(--brand);
}
.app-main {
  flex: 1;
  overflow: auto;
  padding: 24px;
}
.app-main :deep(.page),
.app-main :deep(> *) {
  max-width: 1440px;
  margin-left: auto;
  margin-right: auto;
}

@media (max-width: 1023px) {
  .app-side {
    width: 64px;
  }
  .menu-label {
    display: none;
  }
  .menu-group {
    text-align: center;
    padding-left: 0;
    padding-right: 0;
  }
}
</style>
