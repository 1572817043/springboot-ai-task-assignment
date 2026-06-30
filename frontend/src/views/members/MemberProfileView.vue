<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  getMemberProfileDetail,
  saveMemberSkills,
  updateMemberProfile,
  type MemberProfileDetail,
  type MemberSkillItem,
} from '../../api/memberProfiles'
import { getSkillOptions, type SkillOption } from '../../api/skills'
import { syncMemberResume } from '../../api/aiKnowledge'

const route = useRoute()
const userId = Number(route.params.id)

const loading = ref(false)
const detail = ref<MemberProfileDetail | null>(null)
const errorMessage = ref('')
const skillOptions = ref<SkillOption[]>([])

// 编辑画像
const showProfileForm = ref(false)
const profileForm = reactive({ phone: '', resumeText: '', experienceSummary: '' })
const profileError = ref('')
const profileLoading = ref(false)

// 编辑技能
const showSkillForm = ref(false)
const skillItems = ref<MemberSkillItem[]>([])
const skillError = ref('')
const skillLoading = ref(false)

// 同步简历
const syncLoading = ref(false)
const syncMessage = ref('')

function formatRate(value: number) {
  return (value * 100).toFixed(0) + '%'
}

async function loadDetail() {
  loading.value = true
  errorMessage.value = ''
  try {
    detail.value = await getMemberProfileDetail(userId)
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
    // 不阻塞
  }
}

function openProfileForm() {
  if (!detail.value) return
  profileForm.phone = detail.value.phone || ''
  profileForm.resumeText = detail.value.resumeText || ''
  profileForm.experienceSummary = detail.value.experienceSummary || ''
  profileError.value = ''
  showProfileForm.value = true
}

async function handleSaveProfile() {
  profileLoading.value = true
  profileError.value = ''
  try {
    await updateMemberProfile(userId, {
      phone: profileForm.phone || undefined,
      resumeText: profileForm.resumeText || undefined,
      experienceSummary: profileForm.experienceSummary || undefined,
    })
    showProfileForm.value = false
    await loadDetail()
  } catch (e) {
    profileError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    profileLoading.value = false
  }
}

function openSkillForm() {
  if (!detail.value) return
  skillItems.value = detail.value.skills.map((s) => ({
    skillId: s.skillId,
    level: s.level,
    years: s.years,
    description: s.description || '',
  }))
  if (skillItems.value.length === 0) {
    skillItems.value.push({ skillId: 0, level: 1 })
  }
  skillError.value = ''
  showSkillForm.value = true
}

function addSkillItem() {
  skillItems.value.push({ skillId: 0, level: 1 })
}

function removeSkillItem(index: number) {
  skillItems.value.splice(index, 1)
}

async function handleSaveSkills() {
  const invalid = skillItems.value.some((s) => !s.skillId)
  if (invalid) {
    skillError.value = '请选择技能'
    return
  }
  skillLoading.value = true
  skillError.value = ''
  try {
    await saveMemberSkills(userId, skillItems.value)
    showSkillForm.value = false
    await loadDetail()
  } catch (e) {
    skillError.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    skillLoading.value = false
  }
}

async function handleSyncResume() {
  syncLoading.value = true
  syncMessage.value = ''
  try {
    await syncMemberResume(userId)
    syncMessage.value = '同步成功'
  } catch (e) {
    syncMessage.value = e instanceof Error ? e.message : '同步失败'
  } finally {
    syncLoading.value = false
  }
}

onMounted(() => {
  loadSkills()
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
            <h2 class="text-lg font-semibold text-slate-950">{{ detail.realName }}</h2>
            <p class="mt-1 text-sm text-slate-500">{{ detail.email || detail.username }}</p>
          </div>
          <div class="flex gap-2">
            <button
              class="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="openProfileForm"
            >
              编辑画像
            </button>
            <button
              class="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="openSkillForm"
            >
              编辑技能
            </button>
            <button
              class="rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
              type="button"
              :disabled="syncLoading"
              @click="handleSyncResume"
            >
              {{ syncLoading ? '同步中...' : '同步到知识库' }}
            </button>
          </div>
        </div>
        <p v-if="syncMessage" class="mt-2 text-sm" :class="syncMessage === '同步成功' ? 'text-emerald-600' : 'text-red-600'">
          {{ syncMessage }}
        </p>
      </section>

      <!-- 统计 -->
      <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="surface rounded-lg p-5 text-center">
          <div class="text-2xl font-semibold text-slate-950">{{ detail.currentWorkload }}</div>
          <div class="mt-1 text-sm text-slate-500">当前负载</div>
        </div>
        <div class="surface rounded-lg p-5 text-center">
          <div class="text-2xl font-semibold text-slate-950">{{ detail.completedTaskCount }}</div>
          <div class="mt-1 text-sm text-slate-500">已完成任务</div>
        </div>
        <div class="surface rounded-lg p-5 text-center">
          <div class="text-2xl font-semibold text-slate-950">{{ formatRate(detail.taskCompletionRate) }}</div>
          <div class="mt-1 text-sm text-slate-500">完成率</div>
        </div>
        <div class="surface rounded-lg p-5 text-center">
          <div class="text-2xl font-semibold text-slate-950">{{ formatRate(detail.overdueRate) }}</div>
          <div class="mt-1 text-sm text-slate-500">逾期率</div>
        </div>
      </section>

      <!-- 简历 & 经验 -->
      <section class="grid gap-4 lg:grid-cols-2">
        <div class="surface rounded-lg p-5">
          <h3 class="text-sm font-semibold text-slate-950">简历</h3>
          <p class="mt-2 text-sm leading-6 text-slate-600 whitespace-pre-wrap">
            {{ detail.resumeText || '暂无简历数据' }}
          </p>
        </div>
        <div class="surface rounded-lg p-5">
          <h3 class="text-sm font-semibold text-slate-950">经验总结</h3>
          <p class="mt-2 text-sm leading-6 text-slate-600 whitespace-pre-wrap">
            {{ detail.experienceSummary || '暂无经验总结' }}
          </p>
        </div>
      </section>

      <!-- 技能列表 -->
      <section class="surface rounded-lg">
        <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <h3 class="text-sm font-semibold text-slate-950">技能列表</h3>
          <button
            class="text-sm text-slate-600 hover:text-slate-950"
            type="button"
            @click="openSkillForm"
          >
            编辑
          </button>
        </div>
        <div v-if="detail.skills.length > 0" class="divide-y divide-slate-100">
          <div v-for="skill in detail.skills" :key="skill.skillId" class="flex items-center justify-between px-5 py-4">
            <div>
              <div class="text-sm font-medium text-slate-950">{{ skill.skillName }}</div>
              <div class="mt-0.5 text-xs text-slate-500">{{ skill.category }}</div>
            </div>
            <div class="flex items-center gap-4 text-sm text-slate-600">
              <span>等级 {{ skill.level }}</span>
              <span>{{ skill.years }} 年</span>
              <span v-if="skill.description" class="max-w-[200px] truncate text-slate-500">{{ skill.description }}</span>
            </div>
          </div>
        </div>
        <div v-else class="px-5 py-8 text-center text-sm text-slate-500">暂无技能数据</div>
      </section>
    </template>

    <!-- 编辑画像弹层 -->
    <div v-if="showProfileForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showProfileForm = false">
      <div class="w-full max-w-lg rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">编辑画像</h3>

        <form class="mt-5 space-y-4" @submit.prevent="handleSaveProfile">
          <label class="block">
            <span class="text-sm font-medium text-slate-700">手机号</span>
            <input
              v-model="profileForm.phone"
              class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="可选"
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">简历</span>
            <textarea
              v-model="profileForm.resumeText"
              class="mt-1 h-32 w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="个人简历内容"
            />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">经验总结</span>
            <textarea
              v-model="profileForm.experienceSummary"
              class="mt-1 h-24 w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
              placeholder="项目经验总结"
            />
          </label>

          <p v-if="profileError" class="text-sm text-red-600">{{ profileError }}</p>

          <div class="flex justify-end gap-3 pt-2">
            <button
              class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
              type="button"
              @click="showProfileForm = false"
            >
              取消
            </button>
            <button
              class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
              type="submit"
              :disabled="profileLoading"
            >
              {{ profileLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 编辑技能弹层 -->
    <div v-if="showSkillForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showSkillForm = false">
      <div class="w-full max-w-lg max-h-[80vh] overflow-y-auto rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">编辑技能</h3>

        <div class="mt-5 space-y-4">
          <div
            v-for="(item, index) in skillItems"
            :key="index"
            class="rounded-md border border-slate-200 p-4"
          >
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium text-slate-700">技能 {{ index + 1 }}</span>
              <button
                v-if="skillItems.length > 1"
                class="text-sm text-red-600 hover:text-red-800"
                type="button"
                @click="removeSkillItem(index)"
              >
                移除
              </button>
            </div>
            <div class="mt-3 grid gap-3 sm:grid-cols-2">
              <label class="block">
                <span class="text-xs text-slate-500">技能</span>
                <select
                  v-model="item.skillId"
                  class="mt-1 h-9 w-full rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400"
                >
                  <option :value="0" disabled>请选择</option>
                  <option v-for="skill in skillOptions" :key="skill.id" :value="skill.id">
                    {{ skill.skillName }}
                  </option>
                </select>
              </label>
              <label class="block">
                <span class="text-xs text-slate-500">等级 (1-5)</span>
                <select
                  v-model.number="item.level"
                  class="mt-1 h-9 w-full rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400"
                >
                  <option v-for="n in 5" :key="n" :value="n">{{ n }}</option>
                </select>
              </label>
              <label class="block">
                <span class="text-xs text-slate-500">年限</span>
                <input
                  v-model.number="item.years"
                  type="number"
                  min="0"
                  step="0.5"
                  class="mt-1 h-9 w-full rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400"
                />
              </label>
              <label class="block">
                <span class="text-xs text-slate-500">描述</span>
                <input
                  v-model="item.description"
                  class="mt-1 h-9 w-full rounded-md border border-slate-200 px-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
                  placeholder="可选"
                />
              </label>
            </div>
          </div>

          <button
            class="w-full rounded-md border border-dashed border-slate-300 py-2 text-sm text-slate-600 transition hover:border-slate-400 hover:text-slate-950"
            type="button"
            @click="addSkillItem"
          >
            + 添加技能
          </button>
        </div>

        <p v-if="skillError" class="mt-3 text-sm text-red-600">{{ skillError }}</p>

        <div class="mt-5 flex justify-end gap-3">
          <button
            class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
            type="button"
            @click="showSkillForm = false"
          >
            取消
          </button>
          <button
            class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50"
            type="button"
            :disabled="skillLoading"
            @click="handleSaveSkills"
          >
            {{ skillLoading ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
