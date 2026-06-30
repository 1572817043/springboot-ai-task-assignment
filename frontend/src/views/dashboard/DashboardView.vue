<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { healthCheck, type HealthStatus } from '../../api/system'

const summaries = [
  { label: '项目总数', value: '8', hint: '进行中 5 个' },
  { label: '任务总数', value: '126', hint: '本周新增 18 个' },
  { label: '待分配任务', value: '14', hint: '等待经理确认' },
  { label: '逾期任务', value: '3', hint: '需要优先处理' },
]

const recentTasks = [
  { title: '用户登录接口开发', project: '协同办公系统', status: '待分配', priority: '高' },
  { title: '任务看板列表优化', project: 'AI 任务平台', status: '进行中', priority: '中' },
  { title: '成员技能画像录入', project: 'AI 任务平台', status: '待验收', priority: '中' },
]

const recommendations = [
  { task: '开发登录注册接口', member: '张三', score: 82 },
  { task: '设计项目统计接口', member: '李四', score: 76 },
  { task: '整理任务成果模板', member: '王五', score: 71 },
]

const health = ref<HealthStatus | null>(null)
const healthError = ref(false)

onMounted(async () => {
  try {
    health.value = await healthCheck()
  } catch {
    healthError.value = true
  }
})
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-lg font-semibold text-slate-950">工作台</h1>
      <span
        v-if="health"
        class="inline-flex items-center gap-1.5 rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700"
      >
        <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
        后端服务 {{ health.status }}
      </span>
      <span
        v-else-if="healthError"
        class="inline-flex items-center gap-1.5 rounded-md bg-red-50 px-2.5 py-1 text-xs font-medium text-red-700"
      >
        <span class="h-1.5 w-1.5 rounded-full bg-red-500"></span>
        后端未连接
      </span>
    </div>

    <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <article v-for="item in summaries" :key="item.label" class="surface rounded-lg p-5">
        <div class="text-sm text-slate-500">{{ item.label }}</div>
        <div class="mt-3 text-3xl font-semibold tracking-normal text-slate-950">{{ item.value }}</div>
        <div class="mt-2 text-xs text-slate-500">{{ item.hint }}</div>
      </article>
    </section>

    <section class="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
      <div class="surface rounded-lg">
        <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h2 class="text-sm font-semibold text-slate-950">最近任务</h2>
          <button class="text-sm text-slate-500 hover:text-slate-950" type="button">查看全部</button>
        </div>
        <div class="divide-y divide-slate-100">
          <div
            v-for="task in recentTasks"
            :key="task.title"
            class="grid gap-3 px-5 py-4 sm:grid-cols-[1fr_auto_auto]"
          >
            <div>
              <div class="text-sm font-medium text-slate-950">{{ task.title }}</div>
              <div class="mt-1 text-xs text-slate-500">{{ task.project }}</div>
            </div>
            <span class="h-6 w-fit rounded-md bg-slate-100 px-2 text-xs leading-6 text-slate-600">
              {{ task.status }}
            </span>
            <span class="h-6 w-fit rounded-md border border-slate-200 px-2 text-xs leading-6 text-slate-600">
              {{ task.priority }}
            </span>
          </div>
        </div>
      </div>

      <div class="surface rounded-lg">
        <div class="border-b border-slate-200 px-5 py-4">
          <h2 class="text-sm font-semibold text-slate-950">AI 推荐记录</h2>
        </div>
        <div class="space-y-3 p-5">
          <div
            v-for="item in recommendations"
            :key="item.task"
            class="rounded-md border border-slate-200 p-4"
          >
            <div class="text-sm font-medium text-slate-950">{{ item.task }}</div>
            <div class="mt-2 flex items-center justify-between text-xs text-slate-500">
              <span>推荐 {{ item.member }}</span>
              <span>{{ item.score }} 分</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
