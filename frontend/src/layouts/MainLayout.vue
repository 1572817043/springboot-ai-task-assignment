<script setup lang="ts">
import {
  Bell,
  Database,
  FolderKanban,
  LayoutDashboard,
  ListChecks,
  LogOut,
  Search,
  Settings,
  Sparkles,
  UserCircle,
  UsersRound,
} from '@lucide/vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showUserMenu = ref(false)

const navItems = [
  { label: '工作台', to: '/dashboard', icon: LayoutDashboard },
  { label: '项目管理', to: '/projects', icon: FolderKanban },
  { label: '任务列表', to: '/tasks', icon: ListChecks },
  { label: 'AI 分配', to: '/ai-assignment', icon: Sparkles },
  { label: 'AI 知识库', to: '/knowledge', icon: Database },
  { label: '成员画像', to: '/members', icon: UsersRound },
]

const settingItems = [
  { label: '用户设置', to: '/settings/users' },
  { label: '技能设置', to: '/settings/skills' },
]

const pageTitle = computed(() => String(route.meta.title ?? '工作台'))

const displayName = computed(() => authStore.user?.realName ?? '未登录')

function toggleUserMenu() {
  showUserMenu.value = !showUserMenu.value
}

function closeUserMenu() {
  showUserMenu.value = false
}

function handleLogout() {
  showUserMenu.value = false
  authStore.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="min-h-screen bg-[#f7f7f8] text-slate-950">
    <aside
      class="fixed inset-y-0 left-0 z-20 hidden w-64 border-r border-slate-200 bg-white lg:flex lg:flex-col"
    >
      <div class="flex h-16 items-center border-b border-slate-200 px-5">
        <div class="flex h-8 w-8 items-center justify-center rounded-md border border-slate-200 bg-slate-950 text-sm font-semibold text-white">
          AI
        </div>
        <div class="ml-3">
          <div class="text-sm font-semibold leading-5">任务分配平台</div>
          <div class="text-xs text-slate-500">Task Assignment</div>
        </div>
      </div>

      <nav class="flex-1 space-y-1 px-3 py-4">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="flex h-10 items-center gap-3 rounded-md px-3 text-sm text-slate-600 transition hover:bg-slate-100 hover:text-slate-950"
          active-class="bg-slate-950 text-white hover:bg-slate-950 hover:text-white"
        >
          <component :is="item.icon" class="h-4 w-4" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="border-t border-slate-200 px-3 py-4">
        <div class="mb-2 flex items-center gap-2 px-3 text-xs font-medium text-slate-500">
          <Settings class="h-3.5 w-3.5" aria-hidden="true" />
          系统
        </div>
        <RouterLink
          v-for="item in settingItems"
          :key="item.to"
          :to="item.to"
          class="flex h-9 items-center rounded-md px-3 text-sm text-slate-600 transition hover:bg-slate-100 hover:text-slate-950"
          active-class="bg-slate-100 text-slate-950"
        >
          {{ item.label }}
        </RouterLink>
      </div>
    </aside>

    <div class="lg:pl-64">
      <header
        class="sticky top-0 z-10 flex h-16 items-center justify-between border-b border-slate-200 bg-white/95 px-4 backdrop-blur lg:px-6"
      >
        <div>
          <h1 class="text-base font-semibold leading-6 text-slate-950">{{ pageTitle }}</h1>
          <p class="hidden text-xs text-slate-500 sm:block">AI 辅助推荐，项目经理最终确认</p>
        </div>

        <div class="flex items-center gap-2">
          <button
            class="hidden h-9 items-center gap-2 rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-500 transition hover:border-slate-300 hover:text-slate-950 md:flex"
            type="button"
          >
            <Search class="h-4 w-4" aria-hidden="true" />
            搜索任务
          </button>
          <button
            class="flex h-9 w-9 items-center justify-center rounded-md border border-slate-200 bg-white text-slate-500 transition hover:border-slate-300 hover:text-slate-950"
            type="button"
            aria-label="通知"
          >
            <Bell class="h-4 w-4" aria-hidden="true" />
          </button>
          <div class="relative">
            <button
              class="flex h-9 items-center gap-2 rounded-md border border-slate-200 bg-white px-2.5 text-sm text-slate-700 transition hover:border-slate-300 hover:text-slate-950"
              type="button"
              data-testid="user-menu-button"
              @click="toggleUserMenu"
            >
              <UserCircle class="h-4 w-4" aria-hidden="true" />
              <span class="hidden sm:inline">{{ displayName }}</span>
            </button>
            <div
              v-if="showUserMenu"
              class="fixed inset-0 z-30"
              @click="closeUserMenu"
            />
            <div
              v-if="showUserMenu"
              class="absolute right-0 top-11 z-40 w-40 rounded-md border border-slate-200 bg-white py-1 shadow-lg"
            >
              <button
                class="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-50"
                type="button"
                data-testid="logout-button"
                @click="handleLogout"
              >
                <LogOut class="h-4 w-4" aria-hidden="true" />
                退出登录
              </button>
            </div>
          </div>
        </div>
      </header>

      <nav class="flex gap-2 overflow-x-auto border-b border-slate-200 bg-white px-4 py-2 lg:hidden">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="flex h-9 shrink-0 items-center gap-2 rounded-md px-3 text-sm text-slate-600"
          active-class="bg-slate-950 text-white"
        >
          <component :is="item.icon" class="h-4 w-4" aria-hidden="true" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <main class="mx-auto w-full max-w-7xl px-4 py-6 lg:px-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
