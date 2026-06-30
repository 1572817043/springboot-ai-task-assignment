<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getMemberProfileList, type MemberProfileListItem } from '../../api/memberProfiles'
import { getSkillOptions, type SkillOption } from '../../api/skills'
import type { PageResult } from '../../api/types'

const router = useRouter()

const keyword = ref('')
const skillId = ref<number | undefined>(undefined)
const page = ref(1)
const size = 12
const loading = ref(false)
const data = ref<PageResult<MemberProfileListItem> | null>(null)
const errorMessage = ref('')

const skillOptions = ref<SkillOption[]>([])

function formatRate(value: number) {
  return (value * 100).toFixed(0) + '%'
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    data.value = await getMemberProfileList({
      keyword: keyword.value || undefined,
      skillId: skillId.value || undefined,
      page: page.value,
      size,
    })
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadSkills() {
  try {
    skillOptions.value = await getSkillOptions()
  } catch {
    // 不阻塞页面
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

function goDetail(userId: number) {
  router.push(`/members/${userId}`)
}

onMounted(() => {
  loadSkills()
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <!-- 顶栏 -->
    <section class="surface rounded-lg">
      <div class="border-b border-slate-200 px-5 py-4">
        <h2 class="text-base font-semibold text-slate-950">成员画像</h2>
        <p class="mt-1 text-sm text-slate-500">查看团队成员的技能、负载和任务完成情况</p>
      </div>

      <!-- 筛选 -->
      <div class="flex flex-wrap items-center gap-3 px-5 py-3">
        <input
          v-model="keyword"
          class="h-9 w-52 rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
          placeholder="搜索姓名/用户名"
          @keyup.enter="handleSearch"
        />
        <select
          class="h-9 w-40 rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
          @change="skillId = ($event.target as HTMLSelectElement).value ? Number(($event.target as HTMLSelectElement).value) : undefined; handleSearch()"
        >
          <option value="">全部技能</option>
          <option v-for="skill in skillOptions" :key="skill.id" :value="skill.id">
            {{ skill.skillName }}
          </option>
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
    <section>
      <div v-if="loading" class="surface rounded-lg px-5 py-12 text-center text-sm text-slate-500">
        加载中...
      </div>

      <template v-else-if="data && data.records.length > 0">
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <article
            v-for="item in data.records"
            :key="item.userId"
            class="surface cursor-pointer rounded-lg p-5 transition hover:shadow-sm"
            @click="goDetail(item.userId)"
          >
            <div class="flex items-center justify-between">
              <div>
                <h3 class="text-sm font-semibold text-slate-950">{{ item.realName }}</h3>
                <p class="mt-0.5 text-xs text-slate-500">{{ item.email || item.username }}</p>
              </div>
              <button
                class="rounded-md border border-slate-200 px-2 py-1 text-xs text-slate-600 transition hover:bg-slate-50"
                type="button"
                @click.stop="goDetail(item.userId)"
              >
                查看详情
              </button>
            </div>

            <div class="mt-4 grid grid-cols-4 gap-3 text-center">
              <div>
                <div class="text-lg font-semibold text-slate-950">{{ item.currentWorkload }}</div>
                <div class="text-xs text-slate-500">当前负载</div>
              </div>
              <div>
                <div class="text-lg font-semibold text-slate-950">{{ item.completedTaskCount }}</div>
                <div class="text-xs text-slate-500">已完成</div>
              </div>
              <div>
                <div class="text-lg font-semibold text-slate-950">{{ item.overdueTaskCount }}</div>
                <div class="text-xs text-slate-500">逾期</div>
              </div>
              <div>
                <div class="text-lg font-semibold text-slate-950">{{ formatRate(item.taskCompletionRate) }}</div>
                <div class="text-xs text-slate-500">完成率</div>
              </div>
            </div>

            <div v-if="item.skills.length > 0" class="mt-4 flex flex-wrap gap-1.5">
              <span
                v-for="skill in item.skills"
                :key="skill"
                class="rounded-md border border-slate-200 px-2 py-0.5 text-xs text-slate-600"
              >
                {{ skill }}
              </span>
            </div>
          </article>
        </div>

        <!-- 分页 -->
        <div class="mt-4 flex items-center justify-between text-sm text-slate-500">
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

      <div v-else class="surface rounded-lg px-5 py-12 text-center text-sm text-slate-500">
        暂无成员数据
      </div>
    </section>
  </div>
</template>
