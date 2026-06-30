<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import {
  createSkill,
  deleteSkill,
  getSkillList,
  updateSkill,
  type SkillCreatePayload,
  type SkillListItem,
} from '../../api/skills'
import type { PageResult } from '../../api/types'

const keyword = ref('')
const category = ref('')
const page = ref(1)
const size = 10
const loading = ref(false)
const data = ref<PageResult<SkillListItem> | null>(null)
const errorMessage = ref('')

// 表单弹层
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<SkillCreatePayload>({
  skillName: '',
  category: '',
  description: '',
})
const formError = ref('')
const formLoading = ref(false)

// 删除确认
const deleteTarget = ref<SkillListItem | null>(null)
const deleteLoading = ref(false)

function formatTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    data.value = await getSkillList({
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      page: page.value,
      size,
    })
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
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
  form.skillName = ''
  form.category = ''
  form.description = ''
  formError.value = ''
  showForm.value = true
}

function openEdit(item: SkillListItem) {
  editingId.value = item.id
  form.skillName = item.skillName
  form.category = item.category
  form.description = item.description || ''
  formError.value = ''
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  formError.value = ''
}

async function handleSubmit() {
  if (!form.skillName.trim()) {
    formError.value = '请输入技能名称'
    return
  }
  if (!form.category.trim()) {
    formError.value = '请输入分类'
    return
  }
  formLoading.value = true
  formError.value = ''
  try {
    if (editingId.value !== null) {
      await updateSkill(editingId.value, {
        skillName: form.skillName,
        category: form.category,
        description: form.description || undefined,
      })
    } else {
      await createSkill({
        skillName: form.skillName,
        category: form.category,
        description: form.description || undefined,
      })
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

function confirmDelete(item: SkillListItem) {
  deleteTarget.value = item
}

async function handleDelete() {
  if (!deleteTarget.value) return
  deleteLoading.value = true
  try {
    await deleteSkill(deleteTarget.value.id)
    deleteTarget.value = null
    await loadList()
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '删除失败'
    deleteTarget.value = null
  } finally {
    deleteLoading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="space-y-4">
    <!-- 顶栏 -->
    <section class="surface rounded-lg">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <h2 class="text-base font-semibold text-slate-950">技能设置</h2>
        <button
          class="rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
          type="button"
          @click="openCreate"
        >
          新增技能
        </button>
      </div>

      <!-- 筛选 -->
      <div class="flex flex-wrap items-center gap-3 px-5 py-3">
        <input
          v-model="keyword"
          class="h-9 w-52 rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
          placeholder="搜索技能名称"
          @keyup.enter="handleSearch"
        />
        <input
          v-model="category"
          class="h-9 w-40 rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
          placeholder="筛选分类"
          @keyup.enter="handleSearch"
        />
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
          <table class="w-full min-w-[640px] text-left text-sm">
            <thead class="border-b border-slate-200 bg-slate-50 text-xs text-slate-500">
              <tr>
                <th class="px-5 py-3 font-medium">技能名称</th>
                <th class="px-5 py-3 font-medium">分类</th>
                <th class="px-5 py-3 font-medium">描述</th>
                <th class="px-5 py-3 font-medium">更新时间</th>
                <th class="px-5 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="item in data.records" :key="item.id">
                <td class="px-5 py-4 font-medium text-slate-950">{{ item.skillName }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.category }}</td>
                <td class="max-w-[240px] truncate px-5 py-4 text-slate-600">{{ item.description || '-' }}</td>
                <td class="px-5 py-4 text-slate-500">{{ formatTime(item.updatedAt) }}</td>
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
                      class="text-sm text-red-600 hover:text-red-800"
                      type="button"
                      @click="confirmDelete(item)"
                    >
                      删除
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

      <div v-else class="px-5 py-12 text-center text-sm text-slate-500">暂无技能数据</div>
    </section>

    <!-- 新增/编辑弹层 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="closeForm">
      <div class="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">
          {{ editingId !== null ? '编辑技能' : '新增技能' }}
        </h3>

        <form class="mt-5 space-y-4" @submit.prevent="handleSubmit">
          <label class="block">
            <span class="text-sm font-medium text-slate-700">技能名称</span>
            <input
              v-model="form.skillName"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="如：Java、React"
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">分类</span>
            <input
              v-model="form.category"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="如：后端、前端"
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">描述</span>
            <textarea
              v-model="form.description"
              class="mt-1 h-20 w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
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

    <!-- 删除确认弹层 -->
    <div v-if="deleteTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="deleteTarget = null">
      <div class="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">确认删除</h3>
        <p class="mt-2 text-sm text-slate-600">
          确定要删除技能「{{ deleteTarget.skillName }}」吗？此操作不可撤销。
        </p>
        <div class="mt-5 flex justify-end gap-3">
          <button
            class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
            type="button"
            :disabled="deleteLoading"
            @click="deleteTarget = null"
          >
            取消
          </button>
          <button
            class="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:opacity-50"
            type="button"
            :disabled="deleteLoading"
            @click="handleDelete"
          >
            {{ deleteLoading ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
