import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MemberListView from './MemberListView.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}))

const mockGetMemberProfileList = vi.fn()
const mockGetSkillOptions = vi.fn()

vi.mock('../../api/memberProfiles', () => ({
  getMemberProfileList: (...args: unknown[]) => mockGetMemberProfileList(...args),
}))

vi.mock('../../api/skills', () => ({
  getSkillOptions: (...args: unknown[]) => mockGetSkillOptions(...args),
}))

function sampleMembers() {
  return {
    records: [
      {
        userId: 1,
        username: 'zhangsan',
        realName: '张三',
        email: 'zhang@test.com',
        currentWorkload: 2,
        completedTaskCount: 10,
        overdueTaskCount: 1,
        taskCompletionRate: 0.9,
        overdueRate: 0.1,
        skills: ['Java', 'Spring Boot'],
      },
      {
        userId: 2,
        username: 'lisi',
        realName: '李四',
        email: 'li@test.com',
        currentWorkload: 4,
        completedTaskCount: 8,
        overdueTaskCount: 0,
        taskCompletionRate: 1.0,
        overdueRate: 0,
        skills: ['Vue'],
      },
    ],
    total: 2,
    size: 12,
    current: 1,
    pages: 1,
  }
}

function sampleSkillOptions() {
  return [
    { id: 1, skillName: 'Java', category: '后端' },
    { id: 2, skillName: 'Vue', category: '前端' },
  ]
}

describe('MemberListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetMemberProfileList.mockResolvedValue(sampleMembers())
    mockGetSkillOptions.mockResolvedValue(sampleSkillOptions())
  })

  it('loads and displays member list on mount', async () => {
    const wrapper = mount(MemberListView)
    await flushPromises()

    expect(mockGetMemberProfileList).toHaveBeenCalled()
    expect(mockGetSkillOptions).toHaveBeenCalled()
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('李四')
    expect(wrapper.text()).toContain('zhang@test.com')
    expect(wrapper.text()).toContain('Java')
    expect(wrapper.text()).toContain('Spring Boot')
  })

  it('searches with keyword and reloads list', async () => {
    const wrapper = mount(MemberListView)
    await flushPromises()

    const keywordInput = wrapper.find('input[placeholder="搜索姓名/用户名"]')
    await keywordInput.setValue('张三')

    const searchButton = wrapper.findAll('button').find((b) => b.text() === '搜索')!
    await searchButton.trigger('click')
    await flushPromises()

    expect(mockGetMemberProfileList).toHaveBeenLastCalledWith({
      keyword: '张三',
      skillId: undefined,
      page: 1,
      size: 12,
    })
  })

  it('filters by skillId when selecting a skill', async () => {
    const wrapper = mount(MemberListView)
    await flushPromises()

    const select = wrapper.find('select')
    await select.setValue('1')
    await flushPromises()

    expect(mockGetMemberProfileList).toHaveBeenLastCalledWith({
      keyword: undefined,
      skillId: 1,
      page: 1,
      size: 12,
    })
  })

  it('navigates to member detail on click', async () => {
    const wrapper = mount(MemberListView)
    await flushPromises()

    const card = wrapper.find('article')
    await card.trigger('click')

    expect(mockPush).toHaveBeenCalledWith('/members/1')
  })

  it('navigates to member detail on button click', async () => {
    const wrapper = mount(MemberListView)
    await flushPromises()

    const detailButton = wrapper.findAll('button').find((b) => b.text() === '查看详情')!
    await detailButton.trigger('click')

    expect(mockPush).toHaveBeenCalledWith('/members/1')
  })

  it('shows empty state when no members', async () => {
    mockGetMemberProfileList.mockResolvedValue({
      records: [],
      total: 0,
      size: 12,
      current: 1,
      pages: 0,
    })

    const wrapper = mount(MemberListView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无成员数据')
  })

  it('shows error message when loading fails', async () => {
    mockGetMemberProfileList.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(MemberListView)
    await flushPromises()

    expect(wrapper.text()).toContain('网络错误')
  })
})
