import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TaskListView from './TaskListView.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}))

const mockGetTaskList = vi.fn()
const mockCreateTask = vi.fn()
const mockUpdateTask = vi.fn()
const mockAssignTask = vi.fn()
const mockGetProjectList = vi.fn()
const mockGetProjectMembers = vi.fn()
const mockGetSkillOptions = vi.fn()

vi.mock('../../api/tasks', () => ({
  getTaskList: (...args: unknown[]) => mockGetTaskList(...args),
  createTask: (...args: unknown[]) => mockCreateTask(...args),
  updateTask: (...args: unknown[]) => mockUpdateTask(...args),
  assignTask: (...args: unknown[]) => mockAssignTask(...args),
}))

vi.mock('../../api/projects', () => ({
  getProjectList: (...args: unknown[]) => mockGetProjectList(...args),
  getProjectMembers: (...args: unknown[]) => mockGetProjectMembers(...args),
}))

vi.mock('../../api/skills', () => ({
  getSkillOptions: (...args: unknown[]) => mockGetSkillOptions(...args),
}))

function sampleTasks() {
  return {
    records: [
      {
        id: 1,
        projectId: 1,
        projectName: 'AI 平台',
        title: '开发登录功能',
        priority: 'HIGH',
        status: 'TODO',
        creatorId: 1,
        creatorName: '管理员',
        assigneeId: 3,
        assigneeName: '张三',
        deadline: '2026-07-10T18:00:00',
        estimatedHours: 8,
        createdAt: '2026-06-28T10:00:00',
        updatedAt: '2026-06-28T10:00:00',
      },
      {
        id: 2,
        projectId: 1,
        projectName: 'AI 平台',
        title: '设计数据库',
        priority: 'MEDIUM',
        status: 'IN_PROGRESS',
        creatorId: 1,
        creatorName: '管理员',
        assigneeId: 0,
        assigneeName: '',
        deadline: '',
        estimatedHours: 4,
        createdAt: '2026-06-28T10:00:00',
        updatedAt: '2026-06-28T10:00:00',
      },
    ],
    total: 2,
    size: 10,
    current: 1,
    pages: 1,
  }
}

function sampleProjects() {
  return {
    records: [{ id: 1, projectName: 'AI 平台', description: '', managerId: 2, managerName: '经理', status: 'IN_PROGRESS', startDate: '', endDate: '', memberCount: 3, createdAt: '', updatedAt: '' }],
    total: 1, size: 100, current: 1, pages: 1,
  }
}

describe('TaskListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetTaskList.mockResolvedValue(sampleTasks())
    mockGetProjectList.mockResolvedValue(sampleProjects())
    mockGetProjectMembers.mockResolvedValue([
      { userId: 3, username: 'zhangsan', realName: '张三', projectRole: '成员', joinedAt: '' },
    ])
    mockGetSkillOptions.mockResolvedValue([{ id: 1, skillName: 'Java', category: '后端' }])
    mockCreateTask.mockResolvedValue(undefined)
    mockUpdateTask.mockResolvedValue(undefined)
    mockAssignTask.mockResolvedValue(undefined)
  })

  it('loads and displays task list on mount', async () => {
    const wrapper = mount(TaskListView)
    await flushPromises()

    expect(mockGetTaskList).toHaveBeenCalled()
    expect(wrapper.text()).toContain('开发登录功能')
    expect(wrapper.text()).toContain('设计数据库')
    expect(wrapper.text()).toContain('AI 平台')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('高')
    expect(wrapper.text()).toContain('待办')
  })

  it('searches with keyword and reloads list', async () => {
    const wrapper = mount(TaskListView)
    await flushPromises()

    const keywordInput = wrapper.find('input[placeholder="搜索任务标题"]')
    await keywordInput.setValue('登录')

    const searchButton = wrapper.findAll('button').find((b) => b.text() === '搜索')!
    await searchButton.trigger('click')
    await flushPromises()

    expect(mockGetTaskList).toHaveBeenLastCalledWith(expect.objectContaining({
      keyword: '登录',
      page: 1,
    }))
  })

  it('creates a task and refreshes list', async () => {
    const wrapper = mount(TaskListView)
    await flushPromises()

    const addButton = wrapper.findAll('button').find((b) => b.text() === '新建任务')!
    await addButton.trigger('click')

    const nameInput = wrapper.find('.fixed input:not([type])')
    await nameInput.setValue('新任务')

    const saveButton = wrapper.findAll('.fixed button').find((b) => b.text() === '保存')!
    await saveButton.trigger('submit')
    await flushPromises()

    expect(mockCreateTask).toHaveBeenCalled()
    expect(mockGetTaskList).toHaveBeenCalledTimes(2)
  })

  it('opens assign dialog and assigns task', async () => {
    const wrapper = mount(TaskListView)
    await flushPromises()

    const assignButton = wrapper.findAll('button').find((b) => b.text() === '分配')!
    await assignButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('分配任务')

    const select = wrapper.find('.fixed select')
    await select.setValue(3)

    const confirmButton = wrapper.findAll('.fixed button').find((b) => b.text() === '确认分配')!
    await confirmButton.trigger('submit')
    await flushPromises()

    expect(mockAssignTask).toHaveBeenCalledWith(1, 3, undefined)
    expect(mockGetTaskList).toHaveBeenCalledTimes(2)
  })

  it('navigates to task detail', async () => {
    const wrapper = mount(TaskListView)
    await flushPromises()

    const viewButton = wrapper.findAll('button').find((b) => b.text() === '查看')!
    await viewButton.trigger('click')

    expect(mockPush).toHaveBeenCalledWith('/tasks/1')
  })

  it('shows empty state when no tasks', async () => {
    mockGetTaskList.mockResolvedValue({ records: [], total: 0, size: 10, current: 1, pages: 0 })

    const wrapper = mount(TaskListView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无任务数据')
  })

  it('shows error message when loading fails', async () => {
    mockGetTaskList.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(TaskListView)
    await flushPromises()

    expect(wrapper.text()).toContain('网络错误')
  })
})
