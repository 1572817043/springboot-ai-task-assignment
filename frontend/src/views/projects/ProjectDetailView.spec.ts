import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProjectDetailView from './ProjectDetailView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '5' } }),
}))

const mockGetProjectDetail = vi.fn()
const mockGetProjectMembers = vi.fn()
const mockAddProjectMember = vi.fn()
const mockRemoveProjectMember = vi.fn()
const mockGetUserList = vi.fn()

vi.mock('../../api/projects', () => ({
  getProjectDetail: (...args: unknown[]) => mockGetProjectDetail(...args),
  getProjectMembers: (...args: unknown[]) => mockGetProjectMembers(...args),
  addProjectMember: (...args: unknown[]) => mockAddProjectMember(...args),
  removeProjectMember: (...args: unknown[]) => mockRemoveProjectMember(...args),
}))

vi.mock('../../api/users', () => ({
  getUserList: (...args: unknown[]) => mockGetUserList(...args),
}))

function sampleDetail() {
  return {
    id: 5,
    projectName: 'AI 任务分配平台',
    description: '智能任务分配系统',
    managerId: 2,
    managerName: '项目经理',
    status: 'IN_PROGRESS',
    startDate: '2026-06-01',
    endDate: '2026-12-31',
    createdAt: '2026-06-01T10:00:00',
    updatedAt: '2026-06-28T10:00:00',
    members: [
      { userId: 2, username: 'manager', realName: '项目经理', projectRole: '负责人', joinedAt: '2026-06-01T10:00:00' },
      { userId: 3, username: 'zhangsan', realName: '张三', projectRole: '成员', joinedAt: '2026-06-02T10:00:00' },
    ],
  }
}

function sampleUsers() {
  return {
    records: [
      { id: 4, username: 'newuser', realName: '新用户', email: '', phone: '', status: 'ENABLED', roleCode: 'MEMBER', roleName: '成员', createdAt: '' },
    ],
    total: 1,
    size: 100,
    current: 1,
    pages: 1,
  }
}

describe('ProjectDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetProjectDetail.mockResolvedValue(sampleDetail())
    mockGetProjectMembers.mockResolvedValue(sampleDetail().members)
    mockGetUserList.mockResolvedValue(sampleUsers())
    mockAddProjectMember.mockResolvedValue(undefined)
    mockRemoveProjectMember.mockResolvedValue(undefined)
  })

  it('loads and displays project detail on mount', async () => {
    const wrapper = mount(ProjectDetailView)
    await flushPromises()

    expect(mockGetProjectDetail).toHaveBeenCalledWith(5)
    expect(wrapper.text()).toContain('AI 任务分配平台')
    expect(wrapper.text()).toContain('项目经理')
    expect(wrapper.text()).toContain('进行中')
    expect(wrapper.text()).toContain('2026-06-01')
    expect(wrapper.text()).toContain('2026-12-31')
    expect(wrapper.text()).toContain('智能任务分配系统')
  })

  it('displays project members', async () => {
    const wrapper = mount(ProjectDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('项目经理')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('负责人')
    expect(wrapper.text()).toContain('成员')
  })

  it('adds a member and refreshes member list', async () => {
    const wrapper = mount(ProjectDetailView)
    await flushPromises()

    const addButton = wrapper.findAll('button').find((b) => b.text() === '添加成员')!
    await addButton.trigger('click')
    await wrapper.vm.$nextTick()

    const select = wrapper.find('.fixed select')
    await select.setValue(4)

    const confirmButton = wrapper.findAll('.fixed button').find((b) => b.text() === '确认添加')!
    await confirmButton.trigger('submit')
    await flushPromises()

    expect(mockAddProjectMember).toHaveBeenCalledWith(5, 4, '成员')
    // refreshMembers calls getProjectMembers once after add
    expect(mockGetProjectMembers).toHaveBeenCalledTimes(1)
  })

  it('removes a member after confirmation', async () => {
    const wrapper = mount(ProjectDetailView)
    await flushPromises()

    // The first "移除" button corresponds to the first member (项目经理, userId=2)
    const removeButton = wrapper.findAll('button').find((b) => b.text() === '移除')!
    await removeButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('确认移除')
    expect(wrapper.text()).toContain('项目经理')

    const confirmButton = wrapper.findAll('.fixed button').find((b) => b.text() === '确认移除')!
    await confirmButton.trigger('click')
    await flushPromises()

    expect(mockRemoveProjectMember).toHaveBeenCalledWith(5, 2)
    // refreshMembers calls getProjectMembers once after remove
    expect(mockGetProjectMembers).toHaveBeenCalledTimes(1)
  })

  it('shows empty members state', async () => {
    const detail = sampleDetail()
    detail.members = []
    mockGetProjectDetail.mockResolvedValue(detail)

    const wrapper = mount(ProjectDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无成员')
  })

  it('shows error when loading fails', async () => {
    mockGetProjectDetail.mockRejectedValue(new Error('加载失败'))

    const wrapper = mount(ProjectDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('加载失败')
  })
})
