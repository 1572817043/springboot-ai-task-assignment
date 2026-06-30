import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '../stores/auth'
import MainLayout from '../layouts/MainLayout.vue'
import AiAssignmentView from '../views/ai-assignment/AiAssignmentView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import LoginView from '../views/login/LoginView.vue'
import MemberListView from '../views/members/MemberListView.vue'
import MemberProfileView from '../views/members/MemberProfileView.vue'
import ProjectDetailView from '../views/projects/ProjectDetailView.vue'
import ProjectListView from '../views/projects/ProjectListView.vue'
import SkillSettingsView from '../views/settings/SkillSettingsView.vue'
import UserSettingsView from '../views/settings/UserSettingsView.vue'
import KnowledgeView from '../views/knowledge/KnowledgeView.vue'
import TaskDetailView from '../views/tasks/TaskDetailView.vue'
import TaskListView from '../views/tasks/TaskListView.vue'

const routes = [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
          meta: { title: '工作台' },
        },
        {
          path: 'projects',
          name: 'projects',
          component: ProjectListView,
          meta: { title: '项目管理' },
        },
        {
          path: 'projects/:id',
          name: 'project-detail',
          component: ProjectDetailView,
          meta: { title: '项目详情' },
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: TaskListView,
          meta: { title: '任务列表' },
        },
        {
          path: 'tasks/:id',
          name: 'task-detail',
          component: TaskDetailView,
          meta: { title: '任务详情' },
        },
        {
          path: 'ai-assignment',
          name: 'ai-assignment',
          component: AiAssignmentView,
          meta: { title: 'AI 分配' },
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: KnowledgeView,
          meta: { title: 'AI 知识库' },
        },
        {
          path: 'members',
          name: 'members',
          component: MemberListView,
          meta: { title: '成员画像' },
        },
        {
          path: 'members/:id',
          name: 'member-profile',
          component: MemberProfileView,
          meta: { title: '成员详情' },
        },
        {
          path: 'settings/users',
          name: 'user-settings',
          component: UserSettingsView,
          meta: { title: '用户设置' },
        },
        {
          path: 'settings/skills',
          name: 'skill-settings',
          component: SkillSettingsView,
          meta: { title: '技能设置' },
        },
      ],
    },
  ]

export function createAppRouter() {
  const router = createRouter({
    history: createWebHistory(),
    routes,
  })

  router.beforeEach((to) => {
    const authStore = useAuthStore()
    const isPublicPage = Boolean(to.meta.public)

    if (!isPublicPage && !authStore.isAuthenticated) {
      return {
        path: '/login',
        query: {
          redirect: to.fullPath,
        },
      }
    }

    if (to.path === '/login' && authStore.isAuthenticated) {
      return '/dashboard'
    }

    return true
  })

  return router
}

const router = createAppRouter()

export default router
