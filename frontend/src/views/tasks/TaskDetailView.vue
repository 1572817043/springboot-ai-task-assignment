<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { syncTaskResult } from '../../api/aiKnowledge'
import {
  getTaskDetail,
  reviewTaskResult,
  submitTaskResult,
  updateTaskStatus,
  type TaskDetail,
} from '../../api/tasks'
import type { ReviewStatus, TaskStatus } from '../../api/types'

const route = useRoute()
const taskId = Number(route.params.id)

const loading = ref(false)
const detail = ref<TaskDetail | null>(null)
const errorMessage = ref('')

// 状态流转
const showStatusForm = ref(false)
const nextStatus = ref<TaskStatus>('IN_PROGRESS')
const statusRemark = ref('')
const statusError = ref('')
const statusLoading = ref(false)

// 提交成果
const showResultForm = ref(false)
const resultSummary = ref('')
const resultUrl = ref('')
const resultError = ref('')
const resultLoading = ref(false)

// 验收
const showReviewForm = ref(false)
const reviewStatus = ref<ReviewStatus>('APPROVED')
const reviewComment = ref('')
const reviewError = ref('')
const reviewLoading = ref(false)

// 同步
const syncLoading = ref(false)
const syncMessage = ref('')

const statusLabel: Record<TaskStatus, string> = {
  UNASSIGNED: '待分配',
  TODO: '待办',
  IN_PROGRESS: '进行中',
  WAIT_REVIEW: '待验收',
  COMPLETED: '已完成',
}

const priorityLabel: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '紧急',
}

function formatTime(value: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

async function loadDetail() {
  loading.value = true
  errorMessage.value = ''
  try {
    detail.value = await getTaskDetail(taskId)
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

// 可选的下一状态
function availableStatuses(current: TaskStatus): TaskStatus[] {
  const map: Record<TaskStatus, TaskStatus[]> = {
    UNASSIGNED: ['TODO'],
    TODO: ['IN_PROGRESS'],
    IN_PROGRESS: ['WAIT_REVIEW'],
    WAIT_REVIEW: ['COMPLETED', 'IN_PROGRESS'],
    COMPLETED: [],
  }
  return map[current] || []
}

function openStatusForm() {
  if (!detail.value) return
  const available = availableStatuses(detail.value.status)
  nextStatus.value = available[0] || 'IN_PROGRESS'
  statusRemark.value = ''
  statusError.value = ''
  showStatusForm.value = true
}

async function handleStatusChange() {
  statusLoading.value = true
  statusError.value = ''
  try {
    await updateTaskStatus(taskId, nextStatus.value, statusRemark.value || undefined)
    showStatusForm.value = false
    await loadDetail()
  } catch (e) {
    statusError.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    statusLoading.value = false
  }
}

function openResultForm() {
  resultSummary.value = ''
  resultUrl.value = ''
  resultError.value = ''
  showResultForm.value = true
}

async function handleSubmitResult() {
  if (!resultSummary.value.trim()) { resultError.value = '请输入成果摘要'; return }
  resultLoading.value = true
  resultError.value = ''
  try {
    await submitTaskResult(taskId, {
      resultSummary: resultSummary.value,
      resultUrl: resultUrl.value || undefined,
    })
    showResultForm.value = false
    await loadDetail()
  } catch (e) {
    resultError.value = e instanceof Error ? e.message : '提交失败'
  } finally {
    resultLoading.value = false
  }
}

function openReviewForm() {
  reviewStatus.value = 'APPROVED'
  reviewComment.value = ''
  reviewError.value = ''
  showReviewForm.value = true
}

async function handleReview() {
  reviewLoading.value = true
  reviewError.value = ''
  try {
    await reviewTaskResult(taskId, {
      reviewStatus: reviewStatus.value,
      reviewComment: reviewComment.value || undefined,
    })
    showReviewForm.value = false
    await loadDetail()
  } catch (e) {
    reviewError.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    reviewLoading.value = false
  }
}

async function handleSyncResult() {
  syncLoading.value = true
  syncMessage.value = ''
  try {
    await syncTaskResult(taskId)
    syncMessage.value = '同步成功'
  } catch (e) {
    syncMessage.value = e instanceof Error ? e.message : '同步失败'
  } finally {
    syncLoading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="space-y-4">
    <div v-if="loading" class="surface rounded-lg px-5 py-12 text-center text-sm text-slate-500">加载中...</div>
    <p v-else-if="errorMessage" class="rounded-md bg-red-50 px-4 py-2.5 text-sm text-red-700">{{ errorMessage }}</p>

    <template v-else-if="detail">
      <!-- 基本信息 -->
      <section class="surface rounded-lg p-5">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-lg font-semibold text-slate-950">{{ detail.title }}</h2>
            <p class="mt-1 text-sm text-slate-500">项目：{{ detail.projectName }} · 创建人：{{ detail.creatorName }}</p>
          </div>
          <div class="flex gap-2">
            <button
              v-if="availableStatuses(detail.status).length > 0"
              class="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="openStatusForm"
            >
              修改状态
            </button>
            <button
              v-if="detail.status === 'IN_PROGRESS' || detail.status === 'TODO'"
              class="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="openResultForm"
            >
              提交成果
            </button>
            <button
              v-if="detail.status === 'WAIT_REVIEW'"
              class="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="openReviewForm"
            >
              验收成果
            </button>
            <button
              class="rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
              type="button"
              :disabled="syncLoading"
              @click="handleSyncResult"
            >
              {{ syncLoading ? '同步中...' : '同步成果到知识库' }}
            </button>
          </div>
        </div>
        <p v-if="syncMessage" class="mt-2 text-sm" :class="syncMessage === '同步成功' ? 'text-emerald-600' : 'text-red-600'">{{ syncMessage }}</p>

        <div class="mt-4 grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <span class="text-slate-500">状态：</span>
            <span
              :class="{
                'bg-slate-100 text-slate-600': detail.status === 'UNASSIGNED' || detail.status === 'TODO',
                'bg-blue-50 text-blue-700': detail.status === 'IN_PROGRESS',
                'bg-amber-50 text-amber-700': detail.status === 'WAIT_REVIEW',
                'bg-emerald-50 text-emerald-700': detail.status === 'COMPLETED',
              }"
              class="ml-1 inline-block rounded-md px-2 py-0.5 text-xs font-medium"
            >{{ statusLabel[detail.status] }}</span>
          </div>
          <div>
            <span class="text-slate-500">优先级：</span>
            <span class="text-slate-950">{{ priorityLabel[detail.priority] }}</span>
          </div>
          <div>
            <span class="text-slate-500">负责人：</span>
            <span class="text-slate-950">{{ detail.assigneeName || '未分配' }}</span>
          </div>
          <div>
            <span class="text-slate-500">截止时间：</span>
            <span class="text-slate-950">{{ formatTime(detail.deadline) }}</span>
          </div>
        </div>

        <p v-if="detail.description" class="mt-4 text-sm leading-6 text-slate-600 whitespace-pre-wrap">{{ detail.description }}</p>
      </section>

      <!-- 所需技能 -->
      <section v-if="detail.requiredSkills.length > 0" class="surface rounded-lg p-5">
        <h3 class="text-sm font-semibold text-slate-950">所需技能</h3>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="skill in detail.requiredSkills"
            :key="skill.skillId"
            class="inline-flex items-center gap-1.5 rounded-md border border-slate-200 px-2.5 py-1 text-xs text-slate-700"
          >
            {{ skill.skillName }}
            <span class="text-slate-400">权重 {{ skill.weight }}</span>
          </span>
        </div>
      </section>

      <!-- 最新成果 -->
      <section v-if="detail.latestResult" class="surface rounded-lg p-5">
        <h3 class="text-sm font-semibold text-slate-950">最新成果</h3>
        <div class="mt-3 space-y-2 text-sm">
          <p class="text-slate-600 whitespace-pre-wrap">{{ detail.latestResult.resultSummary }}</p>
          <p v-if="detail.latestResult.resultUrl" class="text-slate-500">
            链接：<span class="text-blue-600">{{ detail.latestResult.resultUrl }}</span>
          </p>
          <div class="flex items-center gap-4 text-xs text-slate-500">
            <span>提交时间：{{ formatTime(detail.latestResult.submittedAt) }}</span>
            <span>验收状态：
              <span :class="{
                'text-amber-600': detail.latestResult.reviewStatus === 'PENDING',
                'text-emerald-600': detail.latestResult.reviewStatus === 'APPROVED',
                'text-red-600': detail.latestResult.reviewStatus === 'REJECTED',
              }">
                {{ detail.latestResult.reviewStatus === 'PENDING' ? '待验收' : detail.latestResult.reviewStatus === 'APPROVED' ? '已通过' : '已驳回' }}
              </span>
            </span>
            <span v-if="detail.latestResult.reviewedAt">验收时间：{{ formatTime(detail.latestResult.reviewedAt) }}</span>
          </div>
          <p v-if="detail.latestResult.reviewComment" class="text-slate-500">验收意见：{{ detail.latestResult.reviewComment }}</p>
        </div>
      </section>

      <!-- 状态日志 -->
      <section v-if="detail.statusLogs.length > 0" class="surface rounded-lg">
        <h3 class="border-b border-slate-200 px-5 py-4 text-sm font-semibold text-slate-950">状态日志</h3>
        <div class="divide-y divide-slate-100">
          <div v-for="(log, index) in detail.statusLogs" :key="index" class="flex items-center justify-between px-5 py-3 text-sm">
            <div>
              <span class="text-slate-500">{{ log.oldStatus || '-' }}</span>
              <span class="mx-2 text-slate-300">→</span>
              <span class="font-medium text-slate-950">{{ log.newStatus }}</span>
              <span v-if="log.remark" class="ml-2 text-slate-500">{{ log.remark }}</span>
            </div>
            <div class="text-xs text-slate-400">
              {{ log.operatorName }} · {{ formatTime(log.createdAt) }}
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- 修改状态弹层 -->
    <div v-if="showStatusForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showStatusForm = false">
      <div class="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">修改状态</h3>
        <form class="mt-5 space-y-4" @submit.prevent="handleStatusChange">
          <label class="block">
            <span class="text-sm font-medium text-slate-700">目标状态</span>
            <select v-model="nextStatus" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400">
              <option v-for="s in availableStatuses(detail!.status)" :key="s" :value="s">{{ statusLabel[s] }}</option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">备注</span>
            <input v-model="statusRemark" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="可选" />
          </label>
          <p v-if="statusError" class="text-sm text-red-600">{{ statusError }}</p>
          <div class="flex justify-end gap-3 pt-2">
            <button class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50" type="button" @click="showStatusForm = false">取消</button>
            <button class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50" type="submit" :disabled="statusLoading">{{ statusLoading ? '提交中...' : '确认' }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 提交成果弹层 -->
    <div v-if="showResultForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showResultForm = false">
      <div class="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">提交成果</h3>
        <form class="mt-5 space-y-4" @submit.prevent="handleSubmitResult">
          <label class="block">
            <span class="text-sm font-medium text-slate-700">成果摘要</span>
            <textarea v-model="resultSummary" class="mt-1 h-28 w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="描述完成的工作" />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">成果链接</span>
            <input v-model="resultUrl" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="可选，如 PR 地址" />
          </label>
          <p v-if="resultError" class="text-sm text-red-600">{{ resultError }}</p>
          <div class="flex justify-end gap-3 pt-2">
            <button class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50" type="button" @click="showResultForm = false">取消</button>
            <button class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50" type="submit" :disabled="resultLoading">{{ resultLoading ? '提交中...' : '提交' }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 验收弹层 -->
    <div v-if="showReviewForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showReviewForm = false">
      <div class="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">验收成果</h3>
        <form class="mt-5 space-y-4" @submit.prevent="handleReview">
          <div class="flex gap-4">
            <label class="flex items-center gap-2 text-sm">
              <input v-model="reviewStatus" type="radio" value="APPROVED" class="accent-emerald-600" />
              <span>通过</span>
            </label>
            <label class="flex items-center gap-2 text-sm">
              <input v-model="reviewStatus" type="radio" value="REJECTED" class="accent-red-600" />
              <span>驳回</span>
            </label>
          </div>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">验收意见</span>
            <textarea v-model="reviewComment" class="mt-1 h-20 w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="可选" />
          </label>
          <p v-if="reviewError" class="text-sm text-red-600">{{ reviewError }}</p>
          <div class="flex justify-end gap-3 pt-2">
            <button class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50" type="button" @click="showReviewForm = false">取消</button>
            <button class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50" type="submit" :disabled="reviewLoading">{{ reviewLoading ? '提交中...' : '确认' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
