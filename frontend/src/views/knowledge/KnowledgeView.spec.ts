import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import KnowledgeView from './KnowledgeView.vue'

const mockGetKnowledgeDocuments = vi.fn()
const mockGetKnowledgeDocument = vi.fn()
const mockUpdateDocumentIndexed = vi.fn()

vi.mock('../../api/aiKnowledge', () => ({
  getKnowledgeDocuments: (...args: unknown[]) => mockGetKnowledgeDocuments(...args),
  getKnowledgeDocument: (...args: unknown[]) => mockGetKnowledgeDocument(...args),
  updateDocumentIndexed: (...args: unknown[]) => mockUpdateDocumentIndexed(...args),
}))

function sampleDocuments() {
  return {
    records: [
      {
        id: 1, userId: 3, sourceType: 'RESUME', sourceId: 3,
        title: '张三的简历', content: '擅长 Java 和 Spring Boot', indexed: 1,
        createdAt: '2026-06-28T10:00:00', updatedAt: '2026-06-29T10:00:00',
      },
      {
        id: 2, userId: 4, sourceType: 'TASK_RESULT', sourceId: 10,
        title: '登录功能成果', content: '完成了 JWT 登录接口开发', indexed: 0,
        createdAt: '2026-06-29T08:00:00', updatedAt: '2026-06-29T15:00:00',
      },
    ],
    total: 2, size: 20, current: 1, pages: 1,
  }
}

function sampleDocument() {
  return {
    id: 1, userId: 3, sourceType: 'RESUME', sourceId: 3,
    title: '张三的简历', content: '擅长 Java 和 Spring Boot，有 5 年开发经验', indexed: 1,
    createdAt: '2026-06-28T10:00:00', updatedAt: '2026-06-29T10:00:00',
  }
}

describe('KnowledgeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetKnowledgeDocuments.mockResolvedValue(sampleDocuments())
    mockGetKnowledgeDocument.mockResolvedValue(sampleDocument())
    mockUpdateDocumentIndexed.mockResolvedValue(undefined)
  })

  it('loads and displays knowledge documents on mount', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    expect(mockGetKnowledgeDocuments).toHaveBeenCalledWith(expect.objectContaining({
      page: 1,
      size: 20,
    }))
    expect(wrapper.text()).toContain('张三的简历')
    expect(wrapper.text()).toContain('登录功能成果')
    expect(wrapper.text()).toContain('已索引')
    expect(wrapper.text()).toContain('未索引')
  })

  it('searches with keyword', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const keywordInput = wrapper.find('input[placeholder="搜索标题"]')
    await keywordInput.setValue('张三')

    const searchButton = wrapper.findAll('button').find((b) => b.text() === '搜索')!
    await searchButton.trigger('click')
    await flushPromises()

    expect(mockGetKnowledgeDocuments).toHaveBeenLastCalledWith(expect.objectContaining({
      keyword: '张三',
      page: 1,
    }))
  })

  it('filters by source type', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const selects = wrapper.findAll('select')
    await selects[0].setValue('TASK_RESULT')

    await flushPromises()

    expect(mockGetKnowledgeDocuments).toHaveBeenLastCalledWith(expect.objectContaining({
      sourceType: 'TASK_RESULT',
      page: 1,
    }))
  })

  it('filters by RESUME source type', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const selects = wrapper.findAll('select')
    await selects[0].setValue('RESUME')

    await flushPromises()

    expect(mockGetKnowledgeDocuments).toHaveBeenLastCalledWith(expect.objectContaining({
      sourceType: 'RESUME',
      page: 1,
    }))
  })

  it('filters by indexed status', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const selects = wrapper.findAll('select')
    await selects[1].setValue('1')

    await flushPromises()

    expect(mockGetKnowledgeDocuments).toHaveBeenLastCalledWith(expect.objectContaining({
      indexed: 1,
      page: 1,
    }))
  })

  it('loads document detail when clicking view', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const viewButton = wrapper.findAll('button').find((b) => b.text() === '查看')!
    await viewButton.trigger('click')
    await flushPromises()

    expect(mockGetKnowledgeDocument).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('文档详情')
    expect(wrapper.text()).toContain('张三的简历')
    expect(wrapper.text()).toContain('擅长 Java 和 Spring Boot，有 5 年开发经验')
    expect(wrapper.text()).toContain('简历画像')
  })

  it('toggles indexed status and refreshes', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    // 点击第一行的"取消索引"按钮（第一行 indexed=1）
    const toggleButton = wrapper.findAll('button').find((b) => b.text() === '取消索引')!
    await toggleButton.trigger('click')
    await flushPromises()

    expect(mockUpdateDocumentIndexed).toHaveBeenCalledWith(1, 0)
    // 刷新列表
    expect(mockGetKnowledgeDocuments).toHaveBeenCalledTimes(2)
  })

  it('marks as indexed when currently not indexed', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const toggleButton = wrapper.findAll('button').find((b) => b.text() === '标记索引')!
    await toggleButton.trigger('click')
    await flushPromises()

    expect(mockUpdateDocumentIndexed).toHaveBeenCalledWith(2, 1)
  })

  it('shows empty state when no documents', async () => {
    mockGetKnowledgeDocuments.mockResolvedValue({ records: [], total: 0, size: 20, current: 1, pages: 0 })

    const wrapper = mount(KnowledgeView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无知识文档')
  })

  it('shows error when loading fails', async () => {
    mockGetKnowledgeDocuments.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(KnowledgeView)
    await flushPromises()

    expect(wrapper.text()).toContain('网络错误')
  })

  it('shows error when detail loading fails', async () => {
    mockGetKnowledgeDocument.mockRejectedValue(new Error('详情加载失败'))

    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const viewButton = wrapper.findAll('button').find((b) => b.text() === '查看')!
    await viewButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('详情加载失败')
  })

  it('closes detail panel', async () => {
    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const viewButton = wrapper.findAll('button').find((b) => b.text() === '查看')!
    await viewButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('文档详情')

    const closeButton = wrapper.findAll('button').find((b) => b.text() === '关闭')!
    await closeButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).not.toContain('文档详情')
  })

  it('shows error when toggle indexed fails', async () => {
    mockUpdateDocumentIndexed.mockRejectedValue(new Error('操作失败'))

    const wrapper = mount(KnowledgeView)
    await flushPromises()

    const toggleButton = wrapper.findAll('button').find((b) => b.text() === '取消索引')!
    await toggleButton.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('操作失败')
  })
})
