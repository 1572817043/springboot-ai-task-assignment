<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { getRoleOptions, type RoleOption } from '../../api/roles'
import {
  createUser,
  getUserList,
  updatePassword,
  updateUser,
  updateUserStatus,
  type UserCreatePayload,
  type UserListItem,
  type UserUpdatePayload,
} from '../../api/users'
import type { PageResult } from '../../api/types'

const keyword = ref('')
const statusFilter = ref('')
const page = ref(1)
const size = 10
const loading = ref(false)
const data = ref<PageResult<UserListItem> | null>(null)
const errorMessage = ref('')

const roles = ref<RoleOption[]>([])

// 表单弹层
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleId: 0,
})
const formError = ref('')
const formLoading = ref(false)

// 重置密码弹层
const resetTarget = ref<UserListItem | null>(null)
const resetPassword = ref('')
const resetError = ref('')
const resetLoading = ref(false)

function formatTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function roleLabel(code: string) {
  const map: Record<string, string> = { ADMIN: '管理员', MANAGER: '项目经理', MEMBER: '成员' }
  return map[code] || code
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    data.value = await getUserList({
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size,
    })
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    roles.value = await getRoleOptions()
  } catch {
    // 角色加载失败不阻塞页面
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
  form.username = ''
  form.password = ''
  form.realName = ''
  form.email = ''
  form.phone = ''
  form.roleId = roles.value.length > 0 ? roles.value[0].id : 0
  formError.value = ''
  showForm.value = true
}

function openEdit(item: UserListItem) {
  editingId.value = item.id
  form.username = item.username
  form.password = ''
  form.realName = item.realName
  form.email = item.email || ''
  form.phone = item.phone || ''
  const matched = roles.value.find((r) => r.roleCode === item.roleCode)
  form.roleId = matched ? matched.id : 0
  formError.value = ''
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  formError.value = ''
}

async function handleSubmit() {
  if (editingId.value === null && !form.username.trim()) {
    formError.value = '请输入用户名'
    return
  }
  if (editingId.value === null && !form.password.trim()) {
    formError.value = '请输入密码'
    return
  }
  if (!form.realName.trim()) {
    formError.value = '请输入姓名'
    return
  }
  if (!form.roleId) {
    formError.value = '请选择角色'
    return
  }
  formLoading.value = true
  formError.value = ''
  try {
    if (editingId.value !== null) {
      const payload: UserUpdatePayload = {
        realName: form.realName,
        email: form.email || undefined,
        phone: form.phone || undefined,
        roleId: form.roleId,
      }
      await updateUser(editingId.value, payload)
    } else {
      const payload: UserCreatePayload = {
        username: form.username,
        password: form.password,
        realName: form.realName,
        email: form.email || undefined,
        phone: form.phone || undefined,
        roleId: form.roleId,
      }
      await createUser(payload)
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

async function handleToggleStatus(item: UserListItem) {
  const newStatus = item.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await updateUserStatus(item.id, newStatus)
    await loadList()
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '操作失败'
  }
}

function openResetPassword(item: UserListItem) {
  resetTarget.value = item
  resetPassword.value = ''
  resetError.value = ''
}

async function handleResetPassword() {
  if (!resetTarget.value) return
  if (!resetPassword.value.trim()) {
    resetError.value = '请输入新密码'
    return
  }
  resetLoading.value = true
  resetError.value = ''
  try {
    await updatePassword(resetTarget.value.id, resetPassword.value)
    resetTarget.value = null
  } catch (e) {
    resetError.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    resetLoading.value = false
  }
}

onMounted(() => {
  loadRoles()
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <!-- 顶栏 -->
    <section class="surface rounded-lg">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <h2 class="text-base font-semibold text-slate-950">用户设置</h2>
        <button
          class="rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
          type="button"
          @click="openCreate"
        >
          新增用户
        </button>
      </div>

      <!-- 筛选 -->
      <div class="flex flex-wrap items-center gap-3 px-5 py-3">
        <input
          v-model="keyword"
          class="h-9 w-52 rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
          placeholder="搜索用户名/姓名"
          @keyup.enter="handleSearch"
        />
        <select
          v-model="statusFilter"
          class="h-9 w-36 rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
          @change="handleSearch"
        >
          <option value="">全部状态</option>
          <option value="ENABLED">启用</option>
          <option value="DISABLED">禁用</option>
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
          <table class="w-full min-w-[900px] text-left text-sm">
            <thead class="border-b border-slate-200 bg-slate-50 text-xs text-slate-500">
              <tr>
                <th class="px-5 py-3 font-medium">用户名</th>
                <th class="px-5 py-3 font-medium">姓名</th>
                <th class="px-5 py-3 font-medium">角色</th>
                <th class="px-5 py-3 font-medium">邮箱</th>
                <th class="px-5 py-3 font-medium">手机</th>
                <th class="px-5 py-3 font-medium">状态</th>
                <th class="px-5 py-3 font-medium">创建时间</th>
                <th class="px-5 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="item in data.records" :key="item.id">
                <td class="px-5 py-4 font-medium text-slate-950">{{ item.username }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.realName }}</td>
                <td class="px-5 py-4 text-slate-600">{{ roleLabel(item.roleCode) }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.email || '-' }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.phone || '-' }}</td>
                <td class="px-5 py-4">
                  <span
                    :class="item.status === 'ENABLED' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'"
                    class="inline-block rounded-md px-2 py-0.5 text-xs font-medium"
                  >
                    {{ item.status === 'ENABLED' ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="px-5 py-4 text-slate-500">{{ formatTime(item.createdAt) }}</td>
                <td class="px-5 py-4">
                  <div class="flex items-center gap-2">
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
                      {{ item.status === 'ENABLED' ? '禁用' : '启用' }}
                    </button>
                    <span class="text-slate-300">|</span>
                    <button
                      class="text-sm text-slate-600 hover:text-slate-950"
                      type="button"
                      @click="openResetPassword(item)"
                    >
                      重置密码
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

      <div v-else class="px-5 py-12 text-center text-sm text-slate-500">暂无用户数据</div>
    </section>

    <!-- 新增/编辑弹层 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="closeForm">
      <div class="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">
          {{ editingId !== null ? '编辑用户' : '新增用户' }}
        </h3>

        <form class="mt-5 space-y-4" @submit.prevent="handleSubmit">
          <label v-if="editingId === null" class="block">
            <span class="text-sm font-medium text-slate-700">用户名</span>
            <input
              v-model="form.username"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="登录账号"
            />
          </label>
          <label v-if="editingId === null" class="block">
            <span class="text-sm font-medium text-slate-700">密码</span>
            <input
              v-model="form.password"
              type="password"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="登录密码"
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">姓名</span>
            <input
              v-model="form.realName"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="真实姓名"
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">角色</span>
            <select
              v-model="form.roleId"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
            >
              <option :value="0" disabled>请选择角色</option>
              <option v-for="role in roles" :key="role.id" :value="role.id">
                {{ role.roleName }}
              </option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">邮箱</span>
            <input
              v-model="form.email"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="可选"
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">手机</span>
            <input
              v-model="form.phone"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="可选"
            />
          </label>

          <p v-if="formError" class="text-sm text-red-600">{{ formError }}</p>

          <div class="flex justify-end gap-3 pt-2">
            <button
              class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="closeForm"
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

    <!-- 重置密码弹层 -->
    <div v-if="resetTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="resetTarget = null">
      <div class="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">重置密码</h3>
        <p class="mt-2 text-sm text-slate-600">
          为用户「{{ resetTarget.realName }}」设置新密码
        </p>

        <div class="mt-4">
          <input
            v-model="resetPassword"
            type="password"
            class="h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
            placeholder="请输入新密码"
          />
        </div>

        <p v-if="resetError" class="mt-2 text-sm text-red-600">{{ resetError }}</p>

        <div class="mt-5 flex justify-end gap-3">
          <button
            class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
            type="button"
            :disabled="resetLoading"
            @click="resetTarget = null"
          >
            取消
          </button>
          <button
            class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
            type="button"
            :disabled="resetLoading"
            @click="handleResetPassword"
          >
            {{ resetLoading ? '提交中...' : '确认重置' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
