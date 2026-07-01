<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  createProject,
  getProjectList,
  updateProject,
  updateProjectStatus,
  type ProjectCreatePayload,
  type ProjectListItem,
  type ProjectUpdatePayload,
} from '../../api/projects'
import { getUserList, type UserListItem } from '../../api/users'
import type { PageResult, ProjectStatus } from '../../api/types'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const currentUserId = computed(() => authStore.user?.id ?? 0)

const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const size = 10
const loading = ref(false)
const data = ref<PageResult<ProjectListItem> | null>(null)
const errorMessage = ref('')

const users = ref<UserListItem[]>([])

// 表单弹层
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  projectName: '',
  description: '',
  managerId: 0,
  status: 'NOT_STARTED' as ProjectStatus,
  startDate: '',
  endDate: '',
})
const formError = ref('')
const formLoading = ref(false)

const statusLabel: Record<ProjectStatus, string> = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  ARCHIVED: '已归档',
}

const statusOptions: ProjectStatus[] = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED']

function formatTime(value: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    data.value = await getProjectList({
      keyword: keyword.value || undefined,
      status: (statusFilter.value as ProjectStatus) || undefined,
      page: page.value,
      size,
    })
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  if (!isAdmin.value) {
    users.value = []
    return
  }
  try {
    const result = await getUserList({ status: 'ENABLED', size: 100 })
    users.value = result.records
  } catch {
    // 不阻塞
  }
}

function handleSearch() {
  page.value = 1
  loadList()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  loadList()
}

function openCreate() {
  editingId.value = null
  form.projectName = ''
  form.description = ''
  form.managerId = isAdmin.value ? (users.value.length > 0 ? users.value[0].id : 0) : currentUserId.value
  form.status = 'NOT_STARTED'
  form.startDate = ''
  form.endDate = ''
  formError.value = ''
  showForm.value = true
}

function openEdit(item: ProjectListItem) {
  editingId.value = item.id
  form.projectName = item.projectName
  form.description = item.description || ''
  form.managerId = isAdmin.value ? item.managerId : currentUserId.value
  form.status = item.status
  form.startDate = item.startDate || ''
  form.endDate = item.endDate || ''
  formError.value = ''
  showForm.value = true
}

async function handleSubmit() {
  if (!form.projectName.trim()) {
    formError.value = '请输入项目名称'
    return
  }
  if (isAdmin.value && !form.managerId) {
    formError.value = '请选择负责人'
    return
  }
  if (!isAdmin.value && !currentUserId.value) {
    formError.value = '当前用户信息失效，请重新登录'
    return
  }
  formLoading.value = true
  formError.value = ''
  try {
    const base: ProjectCreatePayload = {
      projectName: form.projectName,
      description: form.description || undefined,
      status: form.status,
      startDate: form.startDate || undefined,
      endDate: form.endDate || undefined,
    }
    if (isAdmin.value) {
      base.managerId = form.managerId
    }
    if (editingId.value !== null) {
      await updateProject(editingId.value, base as ProjectUpdatePayload)
    } else {
      await createProject(base as ProjectCreatePayload)
    }
    showForm.value = false
    page.value = 1
    await loadList()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    formLoading.value = false
  }
}

async function handleToggleStatus(item: ProjectListItem) {
  const next: Record<ProjectStatus, ProjectStatus> = {
    NOT_STARTED: 'IN_PROGRESS',
    IN_PROGRESS: 'COMPLETED',
    COMPLETED: 'ARCHIVED',
    ARCHIVED: 'NOT_STARTED',
  }
  try {
    await updateProjectStatus(item.id, next[item.status])
    await loadList()
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '操作失败'
  }
}

function goDetail(id: number) {
  router.push(`/projects/${id}`)
}

onMounted(() => {
  loadUsers()
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <!-- 顶栏 -->
    <section class="surface rounded-lg">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-sm font-semibold text-slate-950">项目管理</h2>
          <p class="mt-1 text-xs text-slate-500">维护项目、成员和进度</p>
        </div>
        <button
          class="rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
          type="button"
          @click="openCreate"
        >
          新建项目
        </button>
      </div>

      <!-- 筛选 -->
      <div class="flex flex-wrap items-center gap-3 px-5 py-3">
        <input
          v-model="keyword"
          class="h-9 w-52 rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
          placeholder="搜索项目名称"
          @keyup.enter="handleSearch"
        />
        <select
          v-model="statusFilter"
          class="h-9 w-36 rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
          @change="handleSearch"
        >
          <option value="">全部状态</option>
          <option v-for="s in statusOptions" :key="s" :value="s">{{ statusLabel[s] }}</option>
        </select>
        <button
          class="h-9 rounded-md border border-slate-200 px-3 text-sm text-slate-700 transition hover:bg-slate-50"
          type="button"
          @click="handleSearch"
        >
          搜索
        </button>
      </div>
    </section>

    <!-- 错误提示 -->
    <p v-if="errorMessage" class="rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-700">
      {{ errorMessage }}
    </p>

    <!-- 列表 -->
    <section class="surface rounded-lg">
      <div v-if="loading" class="px-5 py-12 text-center text-sm text-slate-500">加载中...</div>

      <template v-else-if="data && data.records.length > 0">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[860px] text-left text-sm">
            <thead class="border-b border-slate-200 bg-slate-50 text-xs text-slate-500">
              <tr>
                <th class="px-5 py-3 font-medium">项目名称</th>
                <th class="px-5 py-3 font-medium">状态</th>
                <th class="px-5 py-3 font-medium">负责人</th>
                <th class="px-5 py-3 font-medium">成员数</th>
                <th class="px-5 py-3 font-medium">开始时间</th>
                <th class="px-5 py-3 font-medium">结束时间</th>
                <th class="px-5 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="item in data.records" :key="item.id">
                <td class="px-5 py-4 font-medium text-slate-950">{{ item.projectName }}</td>
                <td class="px-5 py-4">
                  <span
                    :class="{
                      'bg-slate-100 text-slate-600': item.status === 'NOT_STARTED',
                      'bg-blue-50 text-blue-700': item.status === 'IN_PROGRESS',
                      'bg-emerald-50 text-emerald-700': item.status === 'COMPLETED',
                      'bg-slate-100 text-slate-500': item.status === 'ARCHIVED',
                    }"
                    class="inline-block rounded-md px-2 py-0.5 text-xs font-medium"
                  >
                    {{ statusLabel[item.status] }}
                  </span>
                </td>
                <td class="px-5 py-4 text-slate-600">{{ item.managerName }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.memberCount }}</td>
                <td class="px-5 py-4 text-slate-500">{{ formatTime(item.startDate) }}</td>
                <td class="px-5 py-4 text-slate-500">{{ formatTime(item.endDate) }}</td>
                <td class="px-5 py-4">
                  <div class="flex items-center gap-2">
                    <button
                      class="text-sm text-slate-600 hover:text-slate-950"
                      type="button"
                      @click="goDetail(item.id)"
                    >
                      查看
                    </button>
                    <span class="text-slate-300">|</span>
                    <button
                      class="text-sm text-slate-600 hover:text-slate-950"
                      type="button"
                      @click="openEdit(item)"
                    >
                      编辑
                    </button>
                    <span class="text-slate-300">|</span>
                    <button
                      class="text-sm text-slate-600 hover:text-slate-950"
                      type="button"
                      @click="handleToggleStatus(item)"
                    >
                      {{ statusLabel[({ NOT_STARTED: 'IN_PROGRESS', IN_PROGRESS: 'COMPLETED', COMPLETED: 'ARCHIVED', ARCHIVED: 'NOT_STARTED' } as Record<ProjectStatus, ProjectStatus>)[item.status]] }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="flex items-center justify-between border-t border-slate-200 px-5 py-3 text-sm text-slate-500">
          <span>共 {{ data.total }} 条</span>
          <div class="flex items-center gap-2">
            <button
              class="rounded-md border border-slate-200 px-2.5 py-1 text-sm disabled:opacity-40"
              type="button"
              :disabled="data.current <= 1"
              @click="handlePageChange(data.current - 1)"
            >
              上一页
            </button>
            <span class="text-sm">{{ data.current }} / {{ data.pages || 1 }}</span>
            <button
              class="rounded-md border border-slate-200 px-2.5 py-1 text-sm disabled:opacity-40"
              type="button"
              :disabled="data.current >= data.pages"
              @click="handlePageChange(data.current + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </template>

      <div v-else class="px-5 py-12 text-center text-sm text-slate-500">暂无项目数据</div>
    </section>

    <!-- 新建/编辑弹层 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showForm = false">
      <div class="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">
          {{ editingId !== null ? '编辑项目' : '新建项目' }}
        </h3>

        <form class="mt-5 space-y-4" @submit.prevent="handleSubmit">
          <label class="block">
            <span class="text-sm font-medium text-slate-700">项目名称</span>
            <input
              v-model="form.projectName"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="项目名称"
            />
          </label>
          <label v-if="isAdmin" class="block">
            <span class="text-sm font-medium text-slate-700">负责人</span>
            <select
              v-model="form.managerId"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
            >
              <option :value="0" disabled>请选择负责人</option>
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.realName }} ({{ user.username }})
              </option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">状态</span>
            <select
              v-model="form.status"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
            >
              <option v-for="s in statusOptions" :key="s" :value="s">{{ statusLabel[s] }}</option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">描述</span>
            <textarea
              v-model="form.description"
              class="mt-1 h-20 w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="可选"
            />
          </label>
          <div class="grid grid-cols-2 gap-3">
            <label class="block">
              <span class="text-sm font-medium text-slate-700">开始时间</span>
              <input
                v-model="form.startDate"
                type="date"
                class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-slate-700">结束时间</span>
              <input
                v-model="form.endDate"
                type="date"
                class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
              />
            </label>
          </div>

          <p v-if="formError" class="text-sm text-red-600">{{ formError }}</p>

          <div class="flex justify-end gap-3 pt-2">
            <button
              class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="showForm = false"
            >
              取消
            </button>
            <button
              class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
              type="submit"
              :disabled="formLoading"
            >
              {{ formLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
