import { createRouter, createWebHashHistory } from 'vue-router'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
    { path: '/materials', name: 'materials', component: () => import('../views/MaterialsView.vue') },
    { path: '/materials/new', name: 'new-analysis', component: () => import('../views/NewAnalysisView.vue') },
    { path: '/materials/:id', name: 'workbench', component: () => import('../views/WorkbenchView.vue') },
    { path: '/radar', name: 'radar', component: () => import('../views/RadarView.vue') },
    { path: '/qa', name: 'qa', component: () => import('../views/QaView.vue') },
    { path: '/follow-ups', name: 'follow-ups', component: () => import('../views/FollowUpView.vue') },
    { path: '/tasks', name: 'tasks', component: () => import('../views/TaskListView.vue') },
    { path: '/pre-audit', name: 'pre-audit', component: () => import('../views/PreAuditView.vue') },
    { path: '/validations', name: 'validations', component: () => import('../views/ValidationView.vue') },
    { path: '/knowledge', name: 'knowledge', component: () => import('../views/KnowledgeView.vue') },
    { path: '/settings', redirect: '/themes' },
    { path: '/themes', name: 'themes', component: () => import('../views/ThemeManagementView.vue') },
    { path: '/rules', name: 'rules', component: () => import('../views/RuleListView.vue') },
    { path: '/models-def', name: 'models-def', component: () => import('../views/ModelDefinitionView.vue') },
    { path: '/models', name: 'models', component: () => import('../views/ModelConfigView.vue') }
  ]
})

export default router
