import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TaskDetailView from './TaskDetailView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' } }),
}))

const mockGetTaskDetail = vi.fn()
const mockUpdateTaskStatus = vi.fn()
const mockSubmitTaskResult = vi.fn()
const mockReviewTaskResult = vi.fn()
const mockSyncTaskResult = vi.fn()

vi.mock('../../api/tasks', () => ({
  getTaskDetail: (...args: unknown[]) => mockGetTaskDetail(...args),
  updateTaskStatus: (...args: unknown[]) => mockUpdateTaskStatus(...args),
  submitTaskResult: (...args: unknown[]) => mockSubmitTaskResult(...args),
  reviewTaskResult: (...args: unknown[]) => mockReviewTaskResult(...args),
}))

vi.mock('../../api/aiKnowledge', () => ({
  syncTaskResult: (...args: unknown[]) => mockSyncTaskResult(...args),
}))

function sampleDetail() {
  return {
    id: 7,
    projectId: 1,
    projectName: 'AI 平台',
    title: '开发登录功能',
    description: '实现用户名密码登录',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    creatorId: 1,
    creatorName: '管理员',
    assigneeId: 3,
    assigneeName: '张三',
    deadline: '2026-07-10T18:00:00',
    estimatedHours: 8,
    createdAt: '2026-06-28T10:00:00',
    updatedAt: '2026-06-29T10:00:00',
    requiredSkills: [
      { skillId: 1, skillName: 'Java', category: '后端', weight: 3 },
      { skillId: 2, skillName: 'Spring Boot', category: '后端', weight: 2 },
    ],
    statusLogs: [
      { oldStatus: null, newStatus: 'UNASSIGNED', operatorName: '管理员', remark: '', createdAt: '2026-06-28T10:00:00' },
      { oldStatus: 'UNASSIGNED', newStatus: 'TODO', operatorName: '管理员', remark: '', createdAt: '2026-06-28T11:00:00' },
      { oldStatus: 'TODO', newStatus: 'IN_PROGRESS', operatorName: '张三', remark: '开始开发', createdAt: '2026-06-29T09:00:00' },
    ],
    latestResult: null,
  }
}

function sampleDetailWithResult() {
  return {
    ...sampleDetail(),
    status: 'WAIT_REVIEW',
    latestResult: {
      resultSummary: '已完成登录接口',
      resultUrl: 'http://github.com/pr/1',
      reviewStatus: 'PENDING',
      reviewComment: '',
      submittedAt: '2026-06-29T15:00:00',
      reviewedAt: '',
    },
  }
}

describe('TaskDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetTaskDetail.mockResolvedValue(sampleDetail())
    mockUpdateTaskStatus.mockResolvedValue(undefined)
    mockSubmitTaskResult.mockResolvedValue(undefined)
    mockReviewTaskResult.mockResolvedValue(undefined)
    mockSyncTaskResult.mockResolvedValue(undefined)
  })

  it('loads and displays task detail on mount', async () => {
    const wrapper = mount(TaskDetailView)
    await flushPromises()

    expect(mockGetTaskDetail).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('开发登录功能')
    expect(wrapper.text()).toContain('AI 平台')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('进行中')
    expect(wrapper.text()).toContain('高')
    expect(wrapper.text()).toContain('实现用户名密码登录')
    expect(wrapper.text()).toContain('Java')
    expect(wrapper.text()).toContain('权重 3')
  })

  it('displays status logs', async () => {
    const wrapper = mount(TaskDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('状态日志')
    expect(wrapper.text()).toContain('UNASSIGNED')
    expect(wrapper.text()).toContain('开始开发')
  })

  it('changes status and refreshes detail', async () => {
    const wrapper = mount(TaskDetailView)
    await flushPromises()

    const statusButton = wrapper.findAll('button').find((b) => b.text() === '修改状态')!
    await statusButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('修改状态')

    const confirmButton = wrapper.findAll('.fixed button').find((b) => b.text() === '确认')!
    await confirmButton.trigger('submit')
    await flushPromises()

    expect(mockUpdateTaskStatus).toHaveBeenCalledWith(7, 'WAIT_REVIEW', undefined)
    expect(mockGetTaskDetail).toHaveBeenCalledTimes(2)
  })

  it('submits task result and refreshes detail', async () => {
    const wrapper = mount(TaskDetailView)
    await flushPromises()

    const resultButton = wrapper.findAll('button').find((b) => b.text() === '提交成果')!
    await resultButton.trigger('click')
    await wrapper.vm.$nextTick()

    const textarea = wrapper.find('.fixed textarea')
    await textarea.setValue('完成了登录功能开发')

    const submitButton = wrapper.findAll('.fixed button').find((b) => b.text() === '提交')!
    await submitButton.trigger('submit')
    await flushPromises()

    expect(mockSubmitTaskResult).toHaveBeenCalledWith(7, {
      resultSummary: '完成了登录功能开发',
      resultUrl: undefined,
    })
    expect(mockGetTaskDetail).toHaveBeenCalledTimes(2)
  })

  it('reviews task result', async () => {
    mockGetTaskDetail.mockResolvedValue(sampleDetailWithResult())
    const wrapper = mount(TaskDetailView)
    await flushPromises()

    const reviewButton = wrapper.findAll('button').find((b) => b.text() === '验收成果')!
    await reviewButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('验收成果')

    const confirmButton = wrapper.findAll('.fixed button').find((b) => b.text() === '确认')!
    await confirmButton.trigger('submit')
    await flushPromises()

    expect(mockReviewTaskResult).toHaveBeenCalledWith(7, {
      reviewStatus: 'APPROVED',
      reviewComment: undefined,
    })
    expect(mockGetTaskDetail).toHaveBeenCalledTimes(2)
  })

  it('syncs task result to knowledge base', async () => {
    const wrapper = mount(TaskDetailView)
    await flushPromises()

    const syncButton = wrapper.findAll('button').find((b) => b.text() === '同步成果到知识库')!
    await syncButton.trigger('click')
    await flushPromises()

    expect(mockSyncTaskResult).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('同步成功')
  })

  it('displays latest result when present', async () => {
    mockGetTaskDetail.mockResolvedValue(sampleDetailWithResult())
    const wrapper = mount(TaskDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('最新成果')
    expect(wrapper.text()).toContain('已完成登录接口')
    expect(wrapper.text()).toContain('http://github.com/pr/1')
    expect(wrapper.text()).toContain('待验收')
  })

  it('shows error when loading fails', async () => {
    mockGetTaskDetail.mockRejectedValue(new Error('加载失败'))

    const wrapper = mount(TaskDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('加载失败')
  })
})
