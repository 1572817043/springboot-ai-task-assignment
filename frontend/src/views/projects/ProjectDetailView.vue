<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  addProjectMember,
  getProjectDetail,
  getProjectMembers,
  removeProjectMember,
  type ProjectDetail,
  type ProjectMember,
} from '../../api/projects'
import { getUserList, type UserListItem } from '../../api/users'
import type { ProjectStatus } from '../../api/types'

const route = useRoute()
const projectId = Number(route.params.id)

const loading = ref(false)
const detail = ref<ProjectDetail | null>(null)
const errorMessage = ref('')

const members = ref<ProjectMember[]>([])
const users = ref<UserListItem[]>([])

// 添加成员
const showAddMember = ref(false)
const addUserId = ref(0)
const addRole = ref('成员')
const addError = ref('')
const addLoading = ref(false)

// 移除确认
const removeTarget = ref<ProjectMember | null>(null)
const removeLoading = ref(false)

const statusLabel: Record<ProjectStatus, string> = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  ARCHIVED: '已归档',
}

function formatDate(value: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

async function loadDetail() {
  loading.value = true
  errorMessage.value = ''
  try {
    detail.value = await getProjectDetail(projectId)
    members.value = detail.value.members || []
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  try {
    const result = await getUserList({ status: 'ENABLED', size: 100 })
    users.value = result.records
  } catch {
    // 不阻塞
  }
}

async function refreshMembers() {
  try {
    members.value = await getProjectMembers(projectId)
  } catch {
    // 不阻塞
  }
}

function openAddMember() {
  addUserId.value = users.value.length > 0 ? users.value[0].id : 0
  addRole.value = '成员'
  addError.value = ''
  showAddMember.value = true
}

async function handleAddMember() {
  if (!addUserId.value) {
    addError.value = '请选择成员'
    return
  }
  addLoading.value = true
  addError.value = ''
  try {
    await addProjectMember(projectId, addUserId.value, addRole.value)
    showAddMember.value = false
    await refreshMembers()
  } catch (e) {
    addError.value = e instanceof Error ? e.message : '添加失败'
  } finally {
    addLoading.value = false
  }
}

async function handleRemoveMember() {
  if (!removeTarget.value) return
  removeLoading.value = true
  try {
    await removeProjectMember(projectId, removeTarget.value.userId)
    removeTarget.value = null
    await refreshMembers()
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '移除失败'
    removeTarget.value = null
  } finally {
    removeLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
  loadDetail()
})
</script>

<template>
  <div class="space-y-4">
    <!-- 加载 / 错误 -->
    <div v-if="loading" class="surface rounded-lg px-5 py-12 text-center text-sm text-slate-500">
      加载中...
    </div>
    <p v-else-if="errorMessage" class="rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-700">
      {{ errorMessage }}
    </p>

    <template v-else-if="detail">
      <!-- 基本信息 -->
      <section class="surface rounded-lg p-5">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-lg font-semibold text-slate-950">{{ detail.projectName }}</h2>
            <p class="mt-1 text-sm text-slate-500">负责人：{{ detail.managerName }}</p>
          </div>
          <span
            :class="{
              'bg-slate-100 text-slate-600': detail.status === 'NOT_STARTED',
              'bg-blue-50 text-blue-700': detail.status === 'IN_PROGRESS',
              'bg-emerald-50 text-emerald-700': detail.status === 'COMPLETED',
              'bg-slate-100 text-slate-500': detail.status === 'ARCHIVED',
            }"
            class="inline-block rounded-md px-3 py-1 text-sm font-medium"
          >
            {{ statusLabel[detail.status] }}
          </span>
        </div>

        <div class="mt-4 grid gap-4 text-sm sm:grid-cols-3">
          <div>
            <span class="text-slate-500">开始时间：</span>
            <span class="text-slate-950">{{ formatDate(detail.startDate) }}</span>
          </div>
          <div>
            <span class="text-slate-500">结束时间：</span>
            <span class="text-slate-950">{{ formatDate(detail.endDate) }}</span>
          </div>
          <div>
            <span class="text-slate-500">创建时间：</span>
            <span class="text-slate-950">{{ formatDate(detail.createdAt) }}</span>
          </div>
        </div>

        <p v-if="detail.description" class="mt-4 text-sm leading-6 text-slate-600 whitespace-pre-wrap">
          {{ detail.description }}
        </p>
      </section>

      <!-- 成员列表 -->
      <section class="surface rounded-lg">
        <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h3 class="text-sm font-semibold text-slate-950">项目成员 ({{ members.length }})</h3>
          <button
            class="rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
            type="button"
            @click="openAddMember"
          >
            添加成员
          </button>
        </div>

        <div v-if="members.length > 0" class="divide-y divide-slate-100">
          <div
            v-for="member in members"
            :key="member.userId"
            class="flex items-center justify-between px-5 py-4"
          >
            <div>
              <div class="text-sm font-medium text-slate-950">{{ member.realName }}</div>
              <div class="mt-0.5 text-xs text-slate-500">{{ member.username }} · {{ member.projectRole }}</div>
            </div>
            <button
              class="text-sm text-red-600 hover:text-red-800"
              type="button"
              @click="removeTarget = member"
            >
              移除
            </button>
          </div>
        </div>
        <div v-else class="px-5 py-8 text-center text-sm text-slate-500">暂无成员</div>
      </section>
    </template>

    <!-- 添加成员弹层 -->
    <div v-if="showAddMember" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showAddMember = false">
      <div class="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">添加成员</h3>

        <form class="mt-5 space-y-4" @submit.prevent="handleAddMember">
          <label class="block">
            <span class="text-sm font-medium text-slate-700">选择用户</span>
            <select
              v-model="addUserId"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
            >
              <option :value="0" disabled>请选择</option>
              <option v-for="user in users" :key="user.id" :value="user.id">
                {{ user.realName }} ({{ user.username }})
              </option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">项目角色</span>
            <input
              v-model="addRole"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="如：成员、负责人"
            />
          </label>

          <p v-if="addError" class="text-sm text-red-600">{{ addError }}</p>

          <div class="flex justify-end gap-3 pt-2">
            <button
              class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="showAddMember = false"
            >
              取消
            </button>
            <button
              class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
              type="submit"
              :disabled="addLoading"
            >
              {{ addLoading ? '添加中...' : '确认添加' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 移除确认弹层 -->
    <div v-if="removeTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="removeTarget = null">
      <div class="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">确认移除</h3>
        <p class="mt-2 text-sm text-slate-600">
          确定要将「{{ removeTarget.realName }}」从项目中移除吗？
        </p>
        <div class="mt-5 flex justify-end gap-3">
          <button
            class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
            type="button"
            :disabled="removeLoading"
            @click="removeTarget = null"
          >
            取消
          </button>
          <button
            class="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:opacity-50"
            type="button"
            :disabled="removeLoading"
            @click="handleRemoveMember"
          >
            {{ removeLoading ? '移除中...' : '确认移除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
