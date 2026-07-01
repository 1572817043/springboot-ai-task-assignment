import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProjectListView from './ProjectListView.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}))

const mockCurrentUser = vi.fn()
vi.mock('../../stores/auth', () => ({
  useAuthStore: () => ({ user: mockCurrentUser() }),
}))

const mockGetProjectList = vi.fn()
const mockCreateProject = vi.fn()
const mockUpdateProject = vi.fn()
const mockUpdateProjectStatus = vi.fn()
const mockGetUserList = vi.fn()

vi.mock('../../api/projects', () => ({
  getProjectList: (...args: unknown[]) => mockGetProjectList(...args),
  createProject: (...args: unknown[]) => mockCreateProject(...args),
  updateProject: (...args: unknown[]) => mockUpdateProject(...args),
  updateProjectStatus: (...args: unknown[]) => mockUpdateProjectStatus(...args),
}))

vi.mock('../../api/users', () => ({
  getUserList: (...args: unknown[]) => mockGetUserList(...args),
}))

function sampleProjects() {
  return {
    records: [
      {
        id: 1,
        projectName: 'AI 任务分配平台',
        description: '智能任务分配',
        managerId: 2,
        managerName: '项目经理',
        status: 'IN_PROGRESS',
        startDate: '2026-06-01',
        endDate: '2026-12-31',
        memberCount: 5,
        createdAt: '2026-06-01T10:00:00',
        updatedAt: '2026-06-28T10:00:00',
      },
      {
        id: 2,
        projectName: '协同办公系统',
        description: '',
        managerId: 3,
        managerName: '李经理',
        status: 'NOT_STARTED',
        startDate: '',
        endDate: '',
        memberCount: 0,
        createdAt: '2026-06-10T10:00:00',
        updatedAt: '2026-06-10T10:00:00',
      },
    ],
    total: 2,
    size: 10,
    current: 1,
    pages: 1,
  }
}

function sampleUsers() {
  return {
    records: [
      { id: 2, username: 'manager', realName: '项目经理', email: '', phone: '', status: 'ENABLED', roleCode: 'MANAGER', roleName: '项目经理', createdAt: '' },
      { id: 3, username: 'li', realName: '李经理', email: '', phone: '', status: 'ENABLED', roleCode: 'MANAGER', roleName: '项目经理', createdAt: '' },
    ],
    total: 2,
    size: 100,
    current: 1,
    pages: 1,
  }
}

describe('ProjectListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockCurrentUser.mockReturnValue({ id: 1, username: 'admin', realName: '管理员', role: 'ADMIN' })
    mockGetProjectList.mockResolvedValue(sampleProjects())
    mockGetUserList.mockResolvedValue(sampleUsers())
    mockCreateProject.mockResolvedValue(undefined)
    mockUpdateProject.mockResolvedValue(undefined)
    mockUpdateProjectStatus.mockResolvedValue(undefined)
  })

  it('loads and displays project list on mount', async () => {
    const wrapper = mount(ProjectListView)
    await flushPromises()

    expect(mockGetProjectList).toHaveBeenCalled()
    expect(wrapper.text()).toContain('AI 任务分配平台')
    expect(wrapper.text()).toContain('协同办公系统')
    expect(wrapper.text()).toContain('项目经理')
    expect(wrapper.text()).toContain('进行中')
    expect(wrapper.text()).toContain('未开始')
  })

  it('searches with keyword and reloads list', async () => {
    const wrapper = mount(ProjectListView)
    await flushPromises()

    const keywordInput = wrapper.find('input[placeholder="搜索项目名称"]')
    await keywordInput.setValue('AI')

    const searchButton = wrapper.findAll('button').find((b) => b.text() === '搜索')!
    await searchButton.trigger('click')
    await flushPromises()

    expect(mockGetProjectList).toHaveBeenLastCalledWith({
      keyword: 'AI',
      status: undefined,
      page: 1,
      size: 10,
    })
  })

  it('creates a project and refreshes list', async () => {
    const wrapper = mount(ProjectListView)
    await flushPromises()

    const addButton = wrapper.findAll('button').find((b) => b.text() === '新建项目')!
    await addButton.trigger('click')

    // projectName is the first text input in the form
    const nameInput = wrapper.find('.fixed input:not([type])')
    await nameInput.setValue('新项目')

    const select = wrapper.findAll('.fixed select')[0]
    await select.setValue(2)

    const saveButton = wrapper.findAll('.fixed button').find((b) => b.text() === '保存')!
    await saveButton.trigger('submit')
    await flushPromises()

    expect(mockCreateProject).toHaveBeenCalled()
    expect(mockGetProjectList).toHaveBeenCalledTimes(2)
  })

  it('allows manager project creation without user list permission', async () => {
    mockCurrentUser.mockReturnValue({ id: 2, username: 'manager', realName: '项目经理', role: 'MANAGER' })
    mockGetUserList.mockRejectedValue(new Error('无权限访问'))
    const wrapper = mount(ProjectListView)
    await flushPromises()

    const addButton = wrapper.findAll('button').find((b) => b.text() === '新建项目')!
    await addButton.trigger('click')

    const nameInput = wrapper.find('.fixed input:not([type])')
    await nameInput.setValue('经理项目')

    const saveButton = wrapper.findAll('.fixed button').find((b) => b.text() === '保存')!
    await saveButton.trigger('submit')
    await flushPromises()

    const payload = mockCreateProject.mock.calls[0][0]
    expect(payload).toMatchObject({ projectName: '经理项目' })
    expect(payload).not.toHaveProperty('managerId')
    expect(mockGetUserList).not.toHaveBeenCalled()
  })

  it('navigates to project detail', async () => {
    const wrapper = mount(ProjectListView)
    await flushPromises()

    const viewButton = wrapper.findAll('button').find((b) => b.text() === '查看')!
    await viewButton.trigger('click')

    expect(mockPush).toHaveBeenCalledWith('/projects/1')
  })

  it('shows empty state when no projects', async () => {
    mockGetProjectList.mockResolvedValue({
      records: [],
      total: 0,
      size: 10,
      current: 1,
      pages: 0,
    })

    const wrapper = mount(ProjectListView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无项目数据')
  })

  it('shows error message when loading fails', async () => {
    mockGetProjectList.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(ProjectListView)
    await flushPromises()

    expect(wrapper.text()).toContain('网络错误')
  })
})
