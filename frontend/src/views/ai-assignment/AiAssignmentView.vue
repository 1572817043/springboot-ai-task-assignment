<script setup lang="ts">
import { ref } from 'vue'

import { acceptCandidate, getLatestRecommendations, recommend, type AiCandidate, type AiRecommendation } from '../../api/aiAssignment'
import { getTaskDetail, getTaskList, type TaskDetail, type TaskListItem } from '../../api/tasks'
import type { PageResult, TaskStatus } from '../../api/types'

// 任务列表
const keyword = ref('')
const statusFilter = ref('UNASSIGNED')
const taskLoading = ref(false)
const taskError = ref('')
const taskPage = ref<PageResult<TaskListItem> | null>(null)
const selectedTaskId = ref<number | null>(null)

// 任务详情
const taskDetail = ref<TaskDetail | null>(null)
const detailLoading = ref(false)
const detailError = ref('')

// 推荐
const recommendation = ref<AiRecommendation | null>(null)
const recommendLoading = ref(false)
const recommendError = ref('')
const latestLoading = ref(false)

// 采纳
const acceptLoading = ref<number | null>(null)

const priorityLabel: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高', URGENT: '紧急' }
const statusLabel: Record<string, string> = {
  UNASSIGNED: '待分配', TODO: '待办', IN_PROGRESS: '进行中',
  WAIT_REVIEW: '待验收', COMPLETED: '已完成',
}

function formatDate(value: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

async function loadTasks() {
  taskLoading.value = true
  taskError.value = ''
  try {
    taskPage.value = await getTaskList({
      keyword: keyword.value || undefined,
      status: (statusFilter.value as TaskStatus) || undefined,
      page: 1,
      size: 20,
    })
  } catch (e) {
    taskError.value = e instanceof Error ? e.message : '加载任务失败'
  } finally {
    taskLoading.value = false
  }
}

async function selectTask(id: number) {
  selectedTaskId.value = id
  detailLoading.value = true
  detailError.value = ''
  recommendError.value = ''
  try {
    taskDetail.value = await getTaskDetail(id)
  } catch (e) {
    detailError.value = e instanceof Error ? e.message : '加载详情失败'
  } finally {
    detailLoading.value = false
  }
  // 尝试加载最新推荐
  latestLoading.value = true
  try {
    recommendation.value = await getLatestRecommendations(id)
  } catch {
    recommendation.value = null
  } finally {
    latestLoading.value = false
  }
}

async function handleRecommend() {
  if (!selectedTaskId.value) return
  recommendLoading.value = true
  recommendError.value = ''
  try {
    recommendation.value = await recommend(selectedTaskId.value)
  } catch (e) {
    recommendError.value = e instanceof Error ? e.message : '推荐失败'
  } finally {
    recommendLoading.value = false
  }
}

async function handleAccept(candidate: AiCandidate) {
  acceptLoading.value = candidate.candidateId
  try {
    await acceptCandidate(candidate.candidateId)
    // 刷新推荐和任务详情
    if (selectedTaskId.value) {
      const [rec, detail] = await Promise.all([
        getLatestRecommendations(selectedTaskId.value),
        getTaskDetail(selectedTaskId.value),
      ])
      recommendation.value = rec
      taskDetail.value = detail
    }
  } catch (e) {
    recommendError.value = e instanceof Error ? e.message : '采纳失败'
  } finally {
    acceptLoading.value = null
  }
}

function handleSearch() {
  loadTasks()
}

// 初始加载
loadTasks()
</script>

<template>
  <div class="grid gap-4 xl:grid-cols-[1fr_1.6fr]">
    <!-- 左侧：任务选择 + 任务详情 -->
    <div class="space-y-4">
      <!-- 任务选择 -->
      <section class="surface rounded-lg">
        <div class="border-b border-slate-200 px-5 py-4">
          <h2 class="text-sm font-semibold text-slate-950">选择任务</h2>
        </div>
        <div class="flex flex-wrap items-center gap-2 border-b border-slate-100 px-5 py-3">
          <input
            v-model="keyword"
            class="h-9 flex-1 min-w-[120px] rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
            placeholder="搜索任务标题"
            @keyup.enter="handleSearch"
          />
          <select
            v-model="statusFilter"
            class="h-9 w-28 rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400"
            @change="handleSearch"
          >
            <option value="">全部</option>
            <option value="UNASSIGNED">待分配</option>
            <option value="TODO">待办</option>
            <option value="IN_PROGRESS">进行中</option>
            <option value="WAIT_REVIEW">待验收</option>
          </select>
          <button
            class="h-9 rounded-md border border-slate-200 px-3 text-sm text-slate-700 transition hover:bg-slate-50"
            type="button"
            @click="handleSearch"
          >
            搜索
          </button>
        </div>

        <div v-if="taskLoading" class="px-5 py-8 text-center text-sm text-slate-500">加载中...</div>
        <p v-else-if="taskError" class="mx-5 my-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{{ taskError }}</p>
        <div v-else-if="taskPage && taskPage.records.length > 0" class="max-h-[400px] overflow-y-auto divide-y divide-slate-100">
          <button
            v-for="item in taskPage.records"
            :key="item.id"
            class="w-full px-5 py-3 text-left transition hover:bg-slate-50"
            :class="{ 'bg-slate-50': selectedTaskId === item.id }"
            type="button"
            @click="selectTask(item.id)"
          >
            <div class="text-sm font-medium text-slate-950">{{ item.title }}</div>
            <div class="mt-1 flex items-center gap-2 text-xs text-slate-500">
              <span>{{ item.projectName }}</span>
              <span class="text-slate-300">·</span>
              <span
                :class="{
                  'bg-slate-100 text-slate-600': item.priority === 'LOW',
                  'bg-blue-50 text-blue-700': item.priority === 'MEDIUM',
                  'bg-orange-50 text-orange-700': item.priority === 'HIGH',
                  'bg-red-50 text-red-700': item.priority === 'URGENT',
                }"
                class="rounded px-1.5 py-0.5 text-xs"
              >{{ priorityLabel[item.priority] }}</span>
              <span class="text-slate-300">·</span>
              <span>{{ statusLabel[item.status] }}</span>
              <span v-if="item.deadline" class="text-slate-300">·</span>
              <span v-if="item.deadline">截止 {{ formatDate(item.deadline) }}</span>
            </div>
          </button>
        </div>
        <div v-else class="px-5 py-8 text-center text-sm text-slate-500">暂无任务</div>
      </section>

      <!-- 任务详情 -->
      <section v-if="selectedTaskId" class="surface rounded-lg p-5">
        <div v-if="detailLoading" class="py-4 text-center text-sm text-slate-500">加载中...</div>
        <p v-else-if="detailError" class="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{{ detailError }}</p>
        <template v-else-if="taskDetail">
          <h3 class="text-sm font-semibold text-slate-950">{{ taskDetail.title }}</h3>
          <p class="mt-1 text-xs text-slate-500">{{ taskDetail.projectName }} · {{ priorityLabel[taskDetail.priority] }} · {{ statusLabel[taskDetail.status] }}</p>

          <p v-if="taskDetail.description" class="mt-3 text-sm leading-6 text-slate-600 whitespace-pre-wrap">{{ taskDetail.description }}</p>

          <div class="mt-3 text-sm">
            <span class="text-slate-500">负责人：</span>
            <span class="text-slate-950">{{ taskDetail.assigneeName || '未分配' }}</span>
          </div>
          <div class="mt-1 text-sm">
            <span class="text-slate-500">截止时间：</span>
            <span class="text-slate-950">{{ formatDate(taskDetail.deadline) }}</span>
          </div>

          <div v-if="taskDetail.requiredSkills.length > 0" class="mt-3">
            <span class="text-xs text-slate-500">所需技能</span>
            <div class="mt-1.5 flex flex-wrap gap-1.5">
              <span
                v-for="skill in taskDetail.requiredSkills"
                :key="skill.skillId"
                class="rounded-md border border-slate-200 px-2 py-0.5 text-xs text-slate-600"
              >
                {{ skill.skillName }} ({{ skill.weight }})
              </span>
            </div>
          </div>
        </template>
      </section>
    </div>

    <!-- 右侧：推荐结果 -->
    <div class="space-y-4">
      <!-- 操作栏 -->
      <section class="surface rounded-lg p-5">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-sm font-semibold text-slate-950">AI 推荐分配</h2>
            <p class="mt-1 text-xs text-slate-500">
              {{ selectedTaskId ? '为当前任务生成智能推荐' : '请先选择一个任务' }}
            </p>
          </div>
          <button
            class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
            type="button"
            :disabled="!selectedTaskId || recommendLoading"
            @click="handleRecommend"
          >
            {{ recommendLoading ? '生成中...' : '生成 AI 推荐' }}
          </button>
        </div>
        <p v-if="recommendError" class="mt-2 text-sm text-red-600">{{ recommendError }}</p>
      </section>

      <!-- 加载中 -->
      <div v-if="latestLoading" class="surface rounded-lg px-5 py-8 text-center text-sm text-slate-500">
        加载推荐中...
      </div>

      <!-- 推荐列表 -->
      <template v-else-if="recommendation && recommendation.candidates.length > 0">
        <section class="surface rounded-lg">
          <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
            <h3 class="text-sm font-semibold text-slate-950">
              推荐候选人 ({{ recommendation.candidates.length }})
            </h3>
            <span class="text-xs text-slate-500">批次 #{{ recommendation.batchId }}</span>
          </div>

          <div class="divide-y divide-slate-100">
            <div
              v-for="candidate in recommendation.candidates"
              :key="candidate.candidateId"
              class="px-5 py-4"
            >
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <span
                    class="flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold"
                    :class="candidate.rankNo <= 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'"
                  >
                    {{ candidate.rankNo }}
                  </span>
                  <div>
                    <div class="text-sm font-medium text-slate-950">{{ candidate.candidateName }}</div>
                    <div class="mt-0.5 text-xs text-slate-500">{{ candidate.reason }}</div>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <span
                    class="text-lg font-semibold"
                    :class="candidate.rankNo <= 1 ? 'text-emerald-700' : 'text-slate-950'"
                  >
                    {{ candidate.totalScore }}
                  </span>
                  <button
                    v-if="candidate.accepted === 0"
                    class="rounded-md bg-slate-950 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
                    type="button"
                    :disabled="acceptLoading === candidate.candidateId"
                    @click="handleAccept(candidate)"
                  >
                    {{ acceptLoading === candidate.candidateId ? '采纳中...' : '采纳' }}
                  </button>
                  <span v-else class="rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700">
                    已采纳
                  </span>
                </div>
              </div>

              <!-- 评分明细 -->
              <div class="mt-3 grid grid-cols-5 gap-2">
                <div class="text-center">
                  <div class="text-xs text-slate-500">技能</div>
                  <div class="mt-0.5 text-sm font-medium text-slate-950">{{ candidate.skillScore }}</div>
                </div>
                <div class="text-center">
                  <div class="text-xs text-slate-500">历史</div>
                  <div class="mt-0.5 text-sm font-medium text-slate-950">{{ candidate.historyScore }}</div>
                </div>
                <div class="text-center">
                  <div class="text-xs text-slate-500">负载</div>
                  <div class="mt-0.5 text-sm font-medium text-slate-950">{{ candidate.workloadScore }}</div>
                </div>
                <div class="text-center">
                  <div class="text-xs text-slate-500">完成率</div>
                  <div class="mt-0.5 text-sm font-medium text-slate-950">{{ candidate.completionScore }}</div>
                </div>
                <div class="text-center">
                  <div class="text-xs text-slate-500">截止风险</div>
                  <div class="mt-0.5 text-sm font-medium text-slate-950">{{ candidate.deadlineRiskScore }}</div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </template>

      <!-- 空状态 -->
      <div v-else-if="selectedTaskId && !recommendLoading && !latestLoading" class="surface rounded-lg px-5 py-12 text-center text-sm text-slate-500">
        暂无推荐结果，点击"生成 AI 推荐"开始
      </div>
      <div v-else-if="!selectedTaskId" class="surface rounded-lg px-5 py-12 text-center text-sm text-slate-500">
        请先从左侧选择一个任务
      </div>
    </div>
  </div>
</template>
