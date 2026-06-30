import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MemberProfileView from './MemberProfileView.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '3' } }),
}))

const mockGetMemberProfileDetail = vi.fn()
const mockUpdateMemberProfile = vi.fn()
const mockSaveMemberSkills = vi.fn()
const mockGetSkillOptions = vi.fn()
const mockSyncMemberResume = vi.fn()

vi.mock('../../api/memberProfiles', () => ({
  getMemberProfileDetail: (...args: unknown[]) => mockGetMemberProfileDetail(...args),
  updateMemberProfile: (...args: unknown[]) => mockUpdateMemberProfile(...args),
  saveMemberSkills: (...args: unknown[]) => mockSaveMemberSkills(...args),
}))

vi.mock('../../api/skills', () => ({
  getSkillOptions: (...args: unknown[]) => mockGetSkillOptions(...args),
}))

vi.mock('../../api/aiKnowledge', () => ({
  syncMemberResume: (...args: unknown[]) => mockSyncMemberResume(...args),
}))

function sampleDetail() {
  return {
    userId: 3,
    username: 'zhangsan',
    realName: '张三',
    email: 'zhang@test.com',
    phone: '13800000000',
    resumeText: '5年Java开发经验',
    experienceSummary: '参与多个企业级项目',
    currentWorkload: 2,
    completedTaskCount: 10,
    overdueTaskCount: 1,
    taskCompletionRate: 0.9,
    overdueRate: 0.1,
    skills: [
      {
        skillId: 1,
        skillName: 'Java',
        category: '后端',
        level: 4,
        years: 5,
        description: '主力语言',
      },
      {
        skillId: 2,
        skillName: 'Vue',
        category: '前端',
        level: 2,
        years: 1,
        description: '',
      },
    ],
  }
}

function sampleSkillOptions() {
  return [
    { id: 1, skillName: 'Java', category: '后端' },
    { id: 2, skillName: 'Vue', category: '前端' },
    { id: 3, skillName: 'React', category: '前端' },
  ]
}

describe('MemberProfileView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetMemberProfileDetail.mockResolvedValue(sampleDetail())
    mockGetSkillOptions.mockResolvedValue(sampleSkillOptions())
    mockUpdateMemberProfile.mockResolvedValue(undefined)
    mockSaveMemberSkills.mockResolvedValue(undefined)
    mockSyncMemberResume.mockResolvedValue(undefined)
  })

  it('loads and displays member detail on mount', async () => {
    const wrapper = mount(MemberProfileView)
    await flushPromises()

    expect(mockGetMemberProfileDetail).toHaveBeenCalledWith(3)
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.text()).toContain('zhang@test.com')
    expect(wrapper.text()).toContain('5年Java开发经验')
    expect(wrapper.text()).toContain('参与多个企业级项目')
    expect(wrapper.text()).toContain('当前负载')
    expect(wrapper.text()).toContain('完成率')
    expect(wrapper.text()).toContain('Java')
    expect(wrapper.text()).toContain('等级 4')
    expect(wrapper.text()).toContain('5 年')
  })

  it('saves profile and refreshes detail', async () => {
    const wrapper = mount(MemberProfileView)
    await flushPromises()

    // 打开编辑画像弹层
    const editButton = wrapper.findAll('button').find((b) => b.text() === '编辑画像')!
    await editButton.trigger('click')
    await wrapper.vm.$nextTick()

    // 修改手机号
    const phoneInput = wrapper.find('.fixed input')
    await phoneInput.setValue('13900000000')

    // 保存
    const saveButton = wrapper.findAll('.fixed button').find((b) => b.text() === '保存')!
    await saveButton.trigger('submit')
    await flushPromises()

    expect(mockUpdateMemberProfile).toHaveBeenCalledWith(3, {
      phone: '13900000000',
      resumeText: '5年Java开发经验',
      experienceSummary: '参与多个企业级项目',
    })
    expect(mockGetMemberProfileDetail).toHaveBeenCalledTimes(2)
  })

  it('saves skills and refreshes detail', async () => {
    const wrapper = mount(MemberProfileView)
    await flushPromises()

    // 打开编辑技能弹层
    const editSkillButton = wrapper.findAll('button').find((b) => b.text() === '编辑技能')!
    await editSkillButton.trigger('click')
    await wrapper.vm.$nextTick()

    // 保存技能
    const saveButton = wrapper.findAll('.fixed button').find((b) => b.text() === '保存')!
    await saveButton.trigger('click')
    await flushPromises()

    expect(mockSaveMemberSkills).toHaveBeenCalledWith(3, [
      { skillId: 1, level: 4, years: 5, description: '主力语言' },
      { skillId: 2, level: 2, years: 1, description: '' },
    ])
    expect(mockGetMemberProfileDetail).toHaveBeenCalledTimes(2)
  })

  it('syncs resume to knowledge base', async () => {
    const wrapper = mount(MemberProfileView)
    await flushPromises()

    const syncButton = wrapper.findAll('button').find((b) => b.text() === '同步到知识库')!
    await syncButton.trigger('click')
    await flushPromises()

    expect(mockSyncMemberResume).toHaveBeenCalledWith(3)
    expect(wrapper.text()).toContain('同步成功')
  })

  it('shows error when sync fails', async () => {
    mockSyncMemberResume.mockRejectedValue(new Error('同步失败'))

    const wrapper = mount(MemberProfileView)
    await flushPromises()

    const syncButton = wrapper.findAll('button').find((b) => b.text() === '同步到知识库')!
    await syncButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('同步失败')
  })

  it('shows empty skills state', async () => {
    const detail = sampleDetail()
    detail.skills = []
    mockGetMemberProfileDetail.mockResolvedValue(detail)

    const wrapper = mount(MemberProfileView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无技能数据')
  })

  it('shows error when loading fails', async () => {
    mockGetMemberProfileDetail.mockRejectedValue(new Error('加载失败'))

    const wrapper = mount(MemberProfileView)
    await flushPromises()

    expect(wrapper.text()).toContain('加载失败')
  })
})
