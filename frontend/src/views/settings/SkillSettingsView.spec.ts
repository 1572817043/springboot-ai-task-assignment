import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import SkillSettingsView from './SkillSettingsView.vue'

const mockGetSkillList = vi.fn()
const mockCreateSkill = vi.fn()
const mockUpdateSkill = vi.fn()
const mockDeleteSkill = vi.fn()

vi.mock('../../api/skills', () => ({
  getSkillList: (...args: unknown[]) => mockGetSkillList(...args),
  createSkill: (...args: unknown[]) => mockCreateSkill(...args),
  updateSkill: (...args: unknown[]) => mockUpdateSkill(...args),
  deleteSkill: (...args: unknown[]) => mockDeleteSkill(...args),
}))

function samplePage(records = [
  {
    id: 1,
    skillName: 'Java',
    category: '后端',
    description: '服务端开发语言',
    createdAt: '2026-06-01T10:00:00',
    updatedAt: '2026-06-28T12:00:00',
  },
  {
    id: 2,
    skillName: 'React',
    category: '前端',
    description: 'UI 框架',
    createdAt: '2026-06-01T10:00:00',
    updatedAt: '2026-06-27T09:00:00',
  },
]) {
  return {
    records,
    total: records.length,
    size: 10,
    current: 1,
    pages: 1,
  }
}

describe('SkillSettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetSkillList.mockResolvedValue(samplePage())
    mockCreateSkill.mockResolvedValue(undefined)
    mockDeleteSkill.mockResolvedValue(undefined)
  })

  it('loads and displays skill list on mount', async () => {
    const wrapper = mount(SkillSettingsView)
    await flushPromises()

    expect(mockGetSkillList).toHaveBeenCalledWith({
      keyword: undefined,
      category: undefined,
      page: 1,
      size: 10,
    })
    expect(wrapper.text()).toContain('Java')
    expect(wrapper.text()).toContain('React')
    expect(wrapper.text()).toContain('后端')
    expect(wrapper.text()).toContain('前端')
  })

  it('searches with keyword and reloads list', async () => {
    const wrapper = mount(SkillSettingsView)
    await flushPromises()

    const keywordInput = wrapper.findAll('input')[0]
    await keywordInput.setValue('Java')

    const searchButton = wrapper.findAll('button').find((b) => b.text() === '搜索')!
    await searchButton.trigger('click')
    await flushPromises()

    expect(mockGetSkillList).toHaveBeenLastCalledWith({
      keyword: 'Java',
      category: undefined,
      page: 1,
      size: 10,
    })
  })

  it('creates a skill and refreshes list', async () => {
    const wrapper = mount(SkillSettingsView)
    await flushPromises()

    // 打开新增弹层
    const addButton = wrapper.findAll('button').find((b) => b.text() === '新增技能')!
    await addButton.trigger('click')

    // 填写表单
    const inputs = wrapper.findAll('.fixed input')
    await inputs[0].setValue('Vue')
    await inputs[1].setValue('前端')

    // 提交
    const saveButton = wrapper.findAll('.fixed button').find((b) => b.text() === '保存')!
    await saveButton.trigger('submit')
    await flushPromises()

    expect(mockCreateSkill).toHaveBeenCalledWith({
      skillName: 'Vue',
      category: '前端',
      description: undefined,
    })
    // 创建后重新加载列表（第2次调用是 loadList after create）
    expect(mockGetSkillList).toHaveBeenCalledTimes(2)
  })

  it('confirms before deleting a skill', async () => {
    const wrapper = mount(SkillSettingsView)
    await flushPromises()

    // 点击删除按钮
    const deleteButton = wrapper.findAll('button').find((b) => b.text() === '删除')!
    await deleteButton.trigger('click')
    await wrapper.vm.$nextTick()

    // 确认弹层出现
    expect(wrapper.text()).toContain('确认删除')
    expect(wrapper.text()).toContain('Java')

    // 确认删除
    const confirmButton = wrapper.findAll('.fixed button').find((b) => b.text() === '删除')!
    await confirmButton.trigger('click')
    await flushPromises()

    expect(mockDeleteSkill).toHaveBeenCalledWith(1)
    // 删除后重新加载列表
    expect(mockGetSkillList).toHaveBeenCalledTimes(2)
  })

  it('shows empty state when no skills', async () => {
    mockGetSkillList.mockResolvedValue(samplePage([]))

    const wrapper = mount(SkillSettingsView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无技能数据')
  })

  it('shows error message when loading fails', async () => {
    mockGetSkillList.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(SkillSettingsView)
    await flushPromises()

    expect(wrapper.text()).toContain('网络错误')
  })
})
