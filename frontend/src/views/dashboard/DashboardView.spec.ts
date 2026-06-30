import { mount, flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import DashboardView from './DashboardView.vue'

vi.mock('../../api/system', () => ({
  healthCheck: vi.fn(),
}))

import { healthCheck } from '../../api/system'

describe('DashboardView', () => {
  it('renders project task summary and recent recommendation sections', async () => {
    vi.mocked(healthCheck).mockResolvedValue({ status: 'UP', service: 'task-ai-backend' })

    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('项目总数')
    expect(wrapper.text()).toContain('任务总数')
    expect(wrapper.text()).toContain('待分配任务')
    expect(wrapper.text()).toContain('逾期任务')
    expect(wrapper.text()).toContain('最近任务')
    expect(wrapper.text()).toContain('AI 推荐记录')
  })

  it('displays backend health status on success', async () => {
    vi.mocked(healthCheck).mockResolvedValue({ status: 'UP', service: 'task-ai-backend' })

    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('后端服务 UP')
  })

  it('displays disconnected state when health check fails', async () => {
    vi.mocked(healthCheck).mockRejectedValue(new Error('Network Error'))

    const wrapper = mount(DashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('后端未连接')
    expect(wrapper.text()).not.toContain('后端服务')
  })
})
