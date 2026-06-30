import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AiAssignmentView from './AiAssignmentView.vue'

const mockGetTaskList = vi.fn()
const mockGetTaskDetail = vi.fn()
const mockRecommend = vi.fn()
const mockGetLatestRecommendations = vi.fn()
const mockAcceptCandidate = vi.fn()

vi.mock('../../api/tasks', () => ({
  getTaskList: (...args: unknown[]) => mockGetTaskList(...args),
  getTaskDetail: (...args: unknown[]) => mockGetTaskDetail(...args),
}))

vi.mock('../../api/aiAssignment', () => ({
  recommend: (...args: unknown[]) => mockRecommend(...args),
  getLatestRecommendations: (...args: unknown[]) => mockGetLatestRecommendations(...args),
  acceptCandidate: (...args: unknown[]) => mockAcceptCandidate(...args),
}))

function sampleTasks() {
  return {
    records: [
      { id: 1, projectId: 1, projectName: 'AI 平台', title: '开发登录功能', priority: 'HIGH', status: 'UNASSIGNED', creatorId: 1, creatorName: '管理员', assigneeId: 0, assigneeName: '', deadline: '2026-07-10', estimatedHours: 8, createdAt: '', updatedAt: '' },
      { id: 2, projectId: 1, projectName: 'AI 平台', title: '设计数据库', priority: 'MEDIUM', status: 'TODO', creatorId: 1, creatorName: '管理员', assigneeId: 3, assigneeName: '张三', deadline: '', estimatedHours: 4, createdAt: '', updatedAt: '' },
    ],
    total: 2, size: 20, current: 1, pages: 1,
  }
}

function sampleDetail() {
  return {
    id: 1, projectId: 1, projectName: 'AI 平台', title: '开发登录功能',
    description: '实现用户名密码登录接口', priority: 'HIGH', status: 'UNASSIGNED',
    creatorId: 1, creatorName: '管理员', assigneeId: 0, assigneeName: '',
    deadline: '2026-07-10T18:00:00', estimatedHours: 8,
    createdAt: '', updatedAt: '',
    requiredSkills: [
      { skillId: 1, skillName: 'Java', category: '后端', weight: 3 },
      { skillId: 2, skillName: 'Spring Boot', category: '后端', weight: 2 },
    ],
    statusLogs: [], latestResult: null,
  }
}

function sampleRecommendation() {
  return {
    batchId: 10,
    taskId: 1,
    candidates: [
      {
        candidateId: 101, candidateUserId: 3, candidateName: '张三', rankNo: 1,
        totalScore: 82, skillScore: 30, historyScore: 20, workloadScore: 15,
        completionScore: 12, deadlineRiskScore: 5, reason: '技能匹配度高，负载低', accepted: 0,
      },
      {
        candidateId: 102, candidateUserId: 4, candidateName: '李四', rankNo: 2,
        totalScore: 71, skillScore: 25, historyScore: 18, workloadScore: 12,
        completionScore: 10, deadlineRiskScore: 6, reason: '有相关经验', accepted: 0,
      },
    ],
  }
}

describe('AiAssignmentView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetTaskList.mockResolvedValue(sampleTasks())
    mockGetTaskDetail.mockResolvedValue(sampleDetail())
    mockRecommend.mockResolvedValue(sampleRecommendation())
    mockGetLatestRecommendations.mockRejectedValue(new Error('no data'))
    mockAcceptCandidate.mockResolvedValue(undefined)
  })

  it('loads task list on mount with UNASSIGNED status', async () => {
    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    expect(mockGetTaskList).toHaveBeenCalledWith(expect.objectContaining({
      status: 'UNASSIGNED',
      page: 1,
      size: 20,
    }))
    expect(wrapper.text()).toContain('开发登录功能')
    expect(wrapper.text()).toContain('设计数据库')
    expect(wrapper.text()).toContain('AI 平台')
  })

  it('shows error when task list loading fails', async () => {
    mockGetTaskList.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    expect(wrapper.text()).toContain('网络错误')
  })

  it('shows error when task detail loading fails', async () => {
    mockGetTaskDetail.mockRejectedValue(new Error('详情加载失败'))

    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    const taskButton = wrapper.findAll('button').find((b) => b.text().includes('开发登录功能'))!
    await taskButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('详情加载失败')
  })

  it('loads task detail and latest recommendations when selecting a task', async () => {
    mockGetLatestRecommendations.mockResolvedValue(sampleRecommendation())

    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    // 点击第一个任务
    const taskButton = wrapper.findAll('button').find((b) => b.text().includes('开发登录功能'))!
    await taskButton.trigger('click')
    await flushPromises()

    expect(mockGetTaskDetail).toHaveBeenCalledWith(1)
    expect(mockGetLatestRecommendations).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('实现用户名密码登录接口')
    expect(wrapper.text()).toContain('Java')
    expect(wrapper.text()).toContain('Spring Boot')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('82')
  })

  it('generates AI recommendation', async () => {
    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    // 选择任务
    const taskButton = wrapper.findAll('button').find((b) => b.text().includes('开发登录功能'))!
    await taskButton.trigger('click')
    await flushPromises()

    // 生成推荐
    const recommendButton = wrapper.findAll('button').find((b) => b.text() === '生成 AI 推荐')!
    await recommendButton.trigger('click')
    await flushPromises()

    expect(mockRecommend).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('李四')
    expect(wrapper.text()).toContain('82')
    expect(wrapper.text()).toContain('71')
    expect(wrapper.text()).toContain('技能匹配度高，负载低')
    expect(wrapper.text()).toContain('采纳')
  })

  it('accepts a candidate and refreshes', async () => {
    // 预加载推荐
    mockGetLatestRecommendations.mockResolvedValue(sampleRecommendation())

    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    // 选择任务
    const taskButton = wrapper.findAll('button').find((b) => b.text().includes('开发登录功能'))!
    await taskButton.trigger('click')
    await flushPromises()

    // 点击采纳
    const acceptButton = wrapper.findAll('button').find((b) => b.text() === '采纳')!
    await acceptButton.trigger('click')
    await flushPromises()

    expect(mockAcceptCandidate).toHaveBeenCalledWith(101)
    // 刷新推荐和详情
    expect(mockGetLatestRecommendations).toHaveBeenCalledTimes(2)
    expect(mockGetTaskDetail).toHaveBeenCalledTimes(2)
  })

  it('shows empty state when no recommendations', async () => {
    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    // 选择任务
    const taskButton = wrapper.findAll('button').find((b) => b.text().includes('开发登录功能'))!
    await taskButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('暂无推荐结果')
  })

  it('shows initial prompt when no task selected', async () => {
    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    expect(wrapper.text()).toContain('请先从左侧选择一个任务')
  })

  it('shows error when recommend fails', async () => {
    mockRecommend.mockRejectedValue(new Error('推荐服务异常'))

    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    const taskButton = wrapper.findAll('button').find((b) => b.text().includes('开发登录功能'))!
    await taskButton.trigger('click')
    await flushPromises()

    const recommendButton = wrapper.findAll('button').find((b) => b.text() === '生成 AI 推荐')!
    await recommendButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('推荐服务异常')
  })

  it('disables recommend button when no task selected', async () => {
    const wrapper = mount(AiAssignmentView)
    await flushPromises()

    const recommendButton = wrapper.findAll('button').find((b) => b.text() === '生成 AI 推荐')!
    expect(recommendButton.attributes()).toHaveProperty('disabled')
  })
})
