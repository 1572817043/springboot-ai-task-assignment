<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getProjectList, getProjectMembers, type ProjectListItem, type ProjectMember } from '../../api/projects'
import { getSkillOptions, type SkillOption } from '../../api/skills'
import {
  assignTask,
  createTask,
  getTaskList,
  updateTask,
  type TaskCreatePayload,
  type TaskListItem,
  type TaskUpdatePayload,
} from '../../api/tasks'
import type { PageResult, TaskPriority, TaskStatus } from '../../api/types'

const router = useRouter()

const keyword = ref('')
const projectIdFilter = ref<number | undefined>(undefined)
const statusFilter = ref('')
const priorityFilter = ref('')
const page = ref(1)
const size = 10
const loading = ref(false)
const data = ref<PageResult<TaskListItem> | null>(null)
const errorMessage = ref('')

const projects = ref<ProjectListItem[]>([])
const skillOptions = ref<SkillOption[]>([])

// 表单弹层
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  projectId: 0,
  title: '',
  description: '',
  priority: 'MEDIUM' as TaskPriority,
  deadline: '',
  estimatedHours: undefined as number | undefined,
})
const formSkills = ref<{ skillId: number; weight: number }[]>([])
const formError = ref('')
const formLoading = ref(false)

// 分配弹层
const showAssign = ref(false)
const assignTaskId = ref(0)
const assignTaskTitle = ref('')
const assignProjectId = ref(0)
const assignMembers = ref<ProjectMember[]>([])
const assigneeId = ref(0)
const assignReason = ref('')
const assignError = ref('')
const assignLoading = ref(false)

const statusLabel: Record<TaskStatus, string> = {
  UNASSIGNED: '待分配',
  TODO: '待办',
  IN_PROGRESS: '进行中',
  WAIT_REVIEW: '待验收',
  COMPLETED: '已完成',
}

const priorityLabel: Record<TaskPriority, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '紧急',
}

const priorityOptions: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const statusOptions: TaskStatus[] = ['UNASSIGNED', 'TODO', 'IN_PROGRESS', 'WAIT_REVIEW', 'COMPLETED']

function formatDate(value: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    data.value = await getTaskList({
      keyword: keyword.value || undefined,
      projectId: projectIdFilter.value || undefined,
      status: (statusFilter.value as TaskStatus) || undefined,
      priority: (priorityFilter.value as TaskPriority) || undefined,
      page: page.value,
      size,
    })
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadProjects() {
  try {
    const result = await getProjectList({ size: 100 })
    projects.value = result.records
  } catch { /* 不阻塞 */ }
}

async function loadSkills() {
  try {
    skillOptions.value = await getSkillOptions()
  } catch { /* 不阻塞 */ }
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
  form.projectId = projects.value.length > 0 ? projects.value[0].id : 0
  form.title = ''
  form.description = ''
  form.priority = 'MEDIUM'
  form.deadline = ''
  form.estimatedHours = undefined
  formSkills.value = []
  formError.value = ''
  showForm.value = true
}

function openEdit(item: TaskListItem) {
  editingId.value = item.id
  form.projectId = item.projectId
  form.title = item.title
  form.description = ''
  form.priority = item.priority
  form.deadline = item.deadline ? item.deadline.replace(' ', 'T').slice(0, 16) : ''
  form.estimatedHours = item.estimatedHours || undefined
  formSkills.value = []
  formError.value = ''
  showForm.value = true
}

function addSkill() {
  formSkills.value.push({ skillId: 0, weight: 1 })
}

function removeSkill(index: number) {
  formSkills.value.splice(index, 1)
}

async function handleSubmit() {
  if (!form.projectId) { formError.value = '请选择项目'; return }
  if (!form.title.trim()) { formError.value = '请输入标题'; return }
  formLoading.value = true
  formError.value = ''
  try {
    const skills = formSkills.value.filter((s) => s.skillId > 0)
    const deadline = form.deadline ? form.deadline.replace('T', ' ') + ':00' : undefined
    if (editingId.value !== null) {
      const payload: TaskUpdatePayload = {
        title: form.title,
        description: form.description || undefined,
        priority: form.priority,
        deadline,
        estimatedHours: form.estimatedHours,
        requiredSkills: skills.length > 0 ? skills : undefined,
      }
      await updateTask(editingId.value, payload)
    } else {
      const payload: TaskCreatePayload = {
        projectId: form.projectId,
        title: form.title,
        description: form.description || undefined,
        priority: form.priority,
        deadline,
        estimatedHours: form.estimatedHours,
        requiredSkills: skills.length > 0 ? skills : undefined,
      }
      await createTask(payload)
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

async function openAssign(item: TaskListItem) {
  assignTaskId.value = item.id
  assignTaskTitle.value = item.title
  assignProjectId.value = item.projectId
  assigneeId.value = item.assigneeId || 0
  assignReason.value = ''
  assignError.value = ''
  showAssign.value = true
  try {
    assignMembers.value = await getProjectMembers(item.projectId)
  } catch {
    assignMembers.value = []
  }
}

async function handleAssign() {
  if (!assigneeId.value) { assignError.value = '请选择负责人'; return }
  assignLoading.value = true
  assignError.value = ''
  try {
    await assignTask(assignTaskId.value, assigneeId.value, assignReason.value || undefined)
    showAssign.value = false
    await loadList()
  } catch (e) {
    assignError.value = e instanceof Error ? e.message : '分配失败'
  } finally {
    assignLoading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/tasks/${id}`)
}

onMounted(() => {
  loadProjects()
  loadSkills()
  loadList()
})
</script>

<template>
  <div class="space-y-4">
    <!-- 顶栏 -->
    <section class="surface rounded-lg">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div>
          <h2 class="text-sm font-semibold text-slate-950">任务列表</h2>
          <p class="mt-1 text-xs text-slate-500">管理所有任务的分配、进度和成果</p>
        </div>
        <button
          class="rounded-md bg-slate-950 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
          type="button"
          @click="openCreate"
        >
          新建任务
        </button>
      </div>

      <!-- 筛选 -->
      <div class="flex flex-wrap items-center gap-3 px-5 py-3">
        <input
          v-model="keyword"
          class="h-9 w-48 rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400"
          placeholder="搜索任务标题"
          @keyup.enter="handleSearch"
        />
        <select
          class="h-9 w-40 rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
          @change="projectIdFilter = ($event.target as HTMLSelectElement).value ? Number(($event.target as HTMLSelectElement).value) : undefined; handleSearch()"
        >
          <option value="">全部项目</option>
          <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.projectName }}</option>
        </select>
        <select v-model="statusFilter" class="h-9 w-32 rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400" @change="handleSearch">
          <option value="">全部状态</option>
          <option v-for="s in statusOptions" :key="s" :value="s">{{ statusLabel[s] }}</option>
        </select>
        <select v-model="priorityFilter" class="h-9 w-28 rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400" @change="handleSearch">
          <option value="">全部优先级</option>
          <option v-for="p in priorityOptions" :key="p" :value="p">{{ priorityLabel[p] }}</option>
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
          <table class="w-full min-w-[1000px] text-left text-sm">
            <thead class="border-b border-slate-200 bg-slate-50 text-xs text-slate-500">
              <tr>
                <th class="px-5 py-3 font-medium">标题</th>
                <th class="px-5 py-3 font-medium">项目</th>
                <th class="px-5 py-3 font-medium">优先级</th>
                <th class="px-5 py-3 font-medium">状态</th>
                <th class="px-5 py-3 font-medium">负责人</th>
                <th class="px-5 py-3 font-medium">截止时间</th>
                <th class="px-5 py-3 font-medium">预计工时</th>
                <th class="px-5 py-3 font-medium">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="item in data.records" :key="item.id">
                <td class="px-5 py-4 font-medium text-slate-950">{{ item.title }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.projectName }}</td>
                <td class="px-5 py-4">
                  <span
                    :class="{
                      'bg-slate-100 text-slate-600': item.priority === 'LOW',
                      'bg-blue-50 text-blue-700': item.priority === 'MEDIUM',
                      'bg-orange-50 text-orange-700': item.priority === 'HIGH',
                      'bg-red-50 text-red-700': item.priority === 'URGENT',
                    }"
                    class="inline-block rounded-md px-2 py-0.5 text-xs font-medium"
                  >{{ priorityLabel[item.priority] }}</span>
                </td>
                <td class="px-5 py-4">
                  <span
                    :class="{
                      'bg-slate-100 text-slate-600': item.status === 'UNASSIGNED',
                      'bg-slate-200 text-slate-700': item.status === 'TODO',
                      'bg-blue-50 text-blue-700': item.status === 'IN_PROGRESS',
                      'bg-amber-50 text-amber-700': item.status === 'WAIT_REVIEW',
                      'bg-emerald-50 text-emerald-700': item.status === 'COMPLETED',
                    }"
                    class="inline-block rounded-md px-2 py-0.5 text-xs font-medium"
                  >{{ statusLabel[item.status] }}</span>
                </td>
                <td class="px-5 py-4 text-slate-600">{{ item.assigneeName || '未分配' }}</td>
                <td class="px-5 py-4 text-slate-500">{{ formatDate(item.deadline) }}</td>
                <td class="px-5 py-4 text-slate-600">{{ item.estimatedHours ? item.estimatedHours + 'h' : '-' }}</td>
                <td class="px-5 py-4">
                  <div class="flex items-center gap-2">
                    <button class="text-sm text-slate-600 hover:text-slate-950" type="button" @click="goDetail(item.id)">查看</button>
                    <span class="text-slate-300">|</span>
                    <button class="text-sm text-slate-600 hover:text-slate-950" type="button" @click="openEdit(item)">编辑</button>
                    <span class="text-slate-300">|</span>
                    <button class="text-sm text-slate-600 hover:text-slate-950" type="button" @click="openAssign(item)">分配</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex items-center justify-between border-t border-slate-200 px-5 py-3 text-sm text-slate-500">
          <span>共 {{ data.total }} 条</span>
          <div class="flex items-center gap-2">
            <button class="rounded-md border border-slate-200 px-2.5 py-1 text-sm disabled:opacity-40" type="button" :disabled="data.current <= 1" @click="handlePageChange(data.current - 1)">上一页</button>
            <span>{{ data.current }} / {{ data.pages || 1 }}</span>
            <button class="rounded-md border border-slate-200 px-2.5 py-1 text-sm disabled:opacity-40" type="button" :disabled="data.current >= data.pages" @click="handlePageChange(data.current + 1)">下一页</button>
          </div>
        </div>
      </template>

      <div v-else class="px-5 py-12 text-center text-sm text-slate-500">暂无任务数据</div>
    </section>

    <!-- 新建/编辑弹层 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showForm = false">
      <div class="w-full max-w-lg max-h-[85vh] overflow-y-auto rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">{{ editingId !== null ? '编辑任务' : '新建任务' }}</h3>

        <form class="mt-5 space-y-4" @submit.prevent="handleSubmit">
          <label v-if="editingId === null" class="block">
            <span class="text-sm font-medium text-slate-700">所属项目</span>
            <select v-model="form.projectId" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400">
              <option :value="0" disabled>请选择项目</option>
              <option v-for="p in projects" :key="p.id" :value="p.id">{{ p.projectName }}</option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">任务标题</span>
            <input v-model="form.title" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="任务标题" />
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">描述</span>
            <textarea v-model="form.description" class="mt-1 h-20 w-full rounded-md border border-slate-200 px-3 py-2 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="可选" />
          </label>
          <div class="grid grid-cols-2 gap-3">
            <label class="block">
              <span class="text-sm font-medium text-slate-700">优先级</span>
              <select v-model="form.priority" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400">
                <option v-for="p in priorityOptions" :key="p" :value="p">{{ priorityLabel[p] }}</option>
              </select>
            </label>
            <label class="block">
              <span class="text-sm font-medium text-slate-700">预计工时 (h)</span>
              <input v-model.number="form.estimatedHours" type="number" min="0" step="0.5" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="可选" />
            </label>
          </div>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">截止时间</span>
            <input v-model="form.deadline" type="datetime-local" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400" />
          </label>

          <!-- 所需技能 -->
          <div>
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium text-slate-700">所需技能</span>
              <button class="text-sm text-slate-600 hover:text-slate-950" type="button" @click="addSkill">+ 添加</button>
            </div>
            <div v-if="formSkills.length > 0" class="mt-2 space-y-2">
              <div v-for="(skill, index) in formSkills" :key="index" class="flex items-center gap-2">
                <select v-model="skill.skillId" class="h-9 flex-1 rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400">
                  <option :value="0" disabled>选择技能</option>
                  <option v-for="s in skillOptions" :key="s.id" :value="s.id">{{ s.skillName }}</option>
                </select>
                <input v-model.number="skill.weight" type="number" min="0.1" max="10" step="0.1" class="h-9 w-20 rounded-md border border-slate-200 px-2 text-sm outline-none focus:border-slate-400" placeholder="权重" />
                <button class="text-sm text-red-600 hover:text-red-800" type="button" @click="removeSkill(index)">移除</button>
              </div>
            </div>
          </div>

          <p v-if="formError" class="text-sm text-red-600">{{ formError }}</p>

          <div class="flex justify-end gap-3 pt-2">
            <button class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50" type="button" @click="showForm = false">取消</button>
            <button class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50" type="submit" :disabled="formLoading">{{ formLoading ? '保存中...' : '保存' }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 分配弹层 -->
    <div v-if="showAssign" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30" @click.self="showAssign = false">
      <div class="w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6">
        <h3 class="text-base font-semibold text-slate-950">分配任务</h3>
        <p class="mt-1 text-sm text-slate-500">{{ assignTaskTitle }}</p>

        <form class="mt-5 space-y-4" @submit.prevent="handleAssign">
          <label class="block">
            <span class="text-sm font-medium text-slate-700">负责人</span>
            <select v-model="assigneeId" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none focus:border-slate-400">
              <option :value="0" disabled>请选择</option>
              <option v-for="m in assignMembers" :key="m.userId" :value="m.userId">{{ m.realName }} ({{ m.projectRole }})</option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-medium text-slate-700">分配理由</span>
            <input v-model="assignReason" class="mt-1 h-10 w-full rounded-md border border-slate-200 px-3 text-sm outline-none placeholder:text-slate-400 focus:border-slate-400" placeholder="可选" />
          </label>

          <p v-if="assignError" class="text-sm text-red-600">{{ assignError }}</p>

          <div class="flex justify-end gap-3 pt-2">
            <button class="rounded-md border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:bg-slate-50" type="button" @click="showAssign = false">取消</button>
            <button class="rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-50" type="submit" :disabled="assignLoading">{{ assignLoading ? '分配中...' : '确认分配' }}</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
