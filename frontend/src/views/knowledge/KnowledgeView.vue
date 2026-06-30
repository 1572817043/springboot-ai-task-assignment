<script setup lang="ts">
import { ref } from 'vue'

import {
  getKnowledgeDocument,
  getKnowledgeDocuments,
  updateDocumentIndexed,
  type KnowledgeDocument,
} from '../../api/aiKnowledge'
import type { PageResult } from '../../api/types'

const keyword = ref('')
const sourceTypeFilter = ref('')
const indexedFilter = ref('')
const page = ref(1)
const size = 20
const loading = ref(false)
const errorMessage = ref('')
const data = ref<PageResult<KnowledgeDocument> | null>(null)

// 详情
const selectedId = ref<number | null>(null)
const detail = ref<KnowledgeDocument | null>(null)
const detailLoading = ref(false)
const detailError = ref('')

// 索引切换
const togglingIndex = ref<number | null>(null)

const sourceTypeLabel: Record<string, string> = {
  RESUME: '简历画像',
  TASK_RESULT: '任务成果',
}

function formatDate(value: string) {
  if (!value) return '-'
  return value.slice(0, 16).replace('T', ' ')
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    data.value = await getKnowledgeDocuments({
      keyword: keyword.value || undefined,
      sourceType: sourceTypeFilter.value || undefined,
      indexed: indexedFilter.value !== '' ? Number(indexedFilter.value) : undefined,
      page: page.value,
      size,
    })
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadDetail(id: number) {
  selectedId.value = id
  detailLoading.value = true
  detailError.value = ''
  try {
    detail.value = await getKnowledgeDocument(id)
  } catch (e) {
    detailError.value = e instanceof Error ? e.message : '加载详情失败'
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

async function handleToggleIndexed(doc: KnowledgeDocument) {
  togglingIndex.value = doc.id
  try {
    const newIndexed = doc.indexed === 1 ? 0 : 1
    await updateDocumentIndexed(doc.id, newIndexed)
    // 刷新列表
    await loadList()
    // 如果当前详情是这篇文档，也刷新
    if (selectedId.value === doc.id) {
      await loadDetail(doc.id)
    }
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    togglingIndex.value = null
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

function closeDetail() {
  selectedId.value = null
  detail.value = null
}

loadList()
</script>

<template>
  <div class="space-y-4">
    <!-- 筛选栏 -->
    <section class="surface rounded-lg">
      <div class="border-b border-slate-200 px-5 py-4">
        <h2 class="text-sm font-semibold text-slate-950">AI 知识库</h2>
        <p class="mt-1 text-xs text-slate-500">管理同步到向量库的知识文档</p>
      </div>
      <div class="flex flex-wrap items-center gap-3 px-5 py-3">
        <input
          v-model="keyword"
          class="h-9 w-48 rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
          placeholder="搜索标题"
          @keyup.enter="handleSearch"
        />
        <select
          v-model="sourceTypeFilter"
          class="h-9 w-36 rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400"
          @change="handleSearch"
        >
          <option value="">全部来源</option>
          <option value="RESUME">简历画像</option>
          <option value="TASK_RESULT">任务成果</option>
        </select>
        <select
          v-model="indexedFilter"
          class="h-9 w-32 rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400"
          @change="handleSearch"
        >
          <option value="">全部状态</option>
          <option value="1">已索引</option>
          <option value="0">未索引</option>
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

    <p v-if="errorMessage" class="rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-700">{{ errorMessage }}</p>

    <!-- 列表 -->
    <section class="surface rounded-lg">
      <div v-if="loading" class="px-5 py-12 text-center text-sm text-slate-500">加载中...</div>

      <template v-else-if="data && data.records.length > 0">
        <div class="overflow-x-auto">
          <table class="w-full min-w-[800px] text-left text-sm">
            <thead class="border-b border-slate-200 bg-slate-50 text-xs text-slate-500">
              <tr>
                <th class="px-5 py-3 font-medium">标题</th>
                <th class="px-5 py-3 font-medium">来源类型</th>
                <th class="px-5 py-3 font-medium">来源 ID</th>
                <th class="px-5 py-3 font-medium">用户 ID</th>
                <th class="px-5 py-3 font-medium">索引状态</th>
                <th class="px-5 py-3 font-medium">更新时间</th>
                <th class="px-5 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr
                v-for="item in data.records"
                :key="item.id"
                class="transition hover:bg-slate-50"
                :class="{ 'bg-slate-50': selectedId === item.id }"
              >
                <td class="px-5 py-4 font-medium text-slate-950">
                  <button class="text-left hover:underline" type="button" @click="loadDetail(item.id)">
                    {{ item.title }}
                  </button>
                </td>
                <td class="px-5 py-4 text-slate-600">{{ sourceTypeLabel[item.sourceType] || item.sourceType }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.sourceId }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.userId }}</td>
                <td class="px-5 py-4">
                  <span
                    :class="item.indexed === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'"
                    class="inline-block rounded-md px-2 py-0.5 text-xs font-medium"
                  >
                    {{ item.indexed === 1 ? '已索引' : '未索引' }}
                  </span>
                </td>
                <td class="px-5 py-4 text-slate-500">{{ formatDate(item.updatedAt) }}</td>
                <td class="px-5 py-4">
                  <div class="flex items-center gap-2">
                    <button
                      class="text-sm text-slate-600 hover:text-slate-950"
                      type="button"
                      @click="loadDetail(item.id)"
                    >
                      查看
                    </button>
                    <span class="text-slate-300">|</span>
                    <button
                      class="text-sm text-slate-600 hover:text-slate-950 disabled:opacity-40"
                      type="button"
                      :disabled="togglingIndex === item.id"
                      @click="handleToggleIndexed(item)"
                    >
                      {{ togglingIndex === item.id ? '处理中...' : (item.indexed === 1 ? '取消索引' : '标记索引') }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

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
            <span>{{ data.current }} / {{ data.pages || 1 }}</span>
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

      <div v-else class="px-5 py-12 text-center text-sm text-slate-500">暂无知识文档</div>
    </section>

    <!-- 详情面板 -->
    <div
      v-if="selectedId"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/30"
      @click.self="closeDetail"
    >
      <div class="w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-lg border border-slate-200 bg-white p-6">
        <div class="flex items-center justify-between">
          <h3 class="text-base font-semibold text-slate-950">文档详情</h3>
          <button
            class="rounded-md border border-slate-200 px-2 py-1 text-sm text-slate-600 hover:bg-slate-50"
            type="button"
            @click="closeDetail"
          >
            关闭
          </button>
        </div>

        <div v-if="detailLoading" class="mt-6 py-8 text-center text-sm text-slate-500">加载中...</div>
        <p v-else-if="detailError" class="mt-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{{ detailError }}</p>
        <template v-else-if="detail">
          <div class="mt-4 space-y-3">
            <div class="text-sm">
              <span class="text-slate-500">标题：</span>
              <span class="font-medium text-slate-950">{{ detail.title }}</span>
            </div>
            <div class="flex gap-6 text-sm">
              <div>
                <span class="text-slate-500">来源：</span>
                <span class="text-slate-950">{{ sourceTypeLabel[detail.sourceType] || detail.sourceType }}</span>
              </div>
              <div>
                <span class="text-slate-500">来源 ID：</span>
                <span class="text-slate-950">{{ detail.sourceId }}</span>
              </div>
              <div>
                <span class="text-slate-500">用户 ID：</span>
                <span class="text-slate-950">{{ detail.userId }}</span>
              </div>
            </div>
            <div class="flex gap-6 text-sm">
              <div>
                <span class="text-slate-500">索引状态：</span>
                <span
                  :class="detail.indexed === 1 ? 'text-emerald-700' : 'text-slate-600'"
                  class="font-medium"
                >
                  {{ detail.indexed === 1 ? '已索引' : '未索引' }}
                </span>
              </div>
              <div>
                <span class="text-slate-500">更新时间：</span>
                <span class="text-slate-950">{{ formatDate(detail.updatedAt) }}</span>
              </div>
            </div>
            <div class="mt-3">
              <span class="text-xs text-slate-500">内容</span>
              <div class="mt-1.5 rounded-md border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700 whitespace-pre-wrap">
                {{ detail.content }}
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
