import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { login as loginApi } from '../../api/auth'
import { createAppRouter } from '../../router'
import LoginView from './LoginView.vue'

vi.mock('../../api/auth', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(),
}))

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('renders a concise login form for the task platform', () => {
    const wrapper = mount(LoginView)

    expect(wrapper.text()).toContain('AI 任务分配平台')
    expect(wrapper.find('input[name="username"]').exists()).toBe(true)
    expect(wrapper.find('input[name="password"]').exists()).toBe(true)
    expect(wrapper.find('button[type="submit"]').text()).toBe('登录')
  })

  it('submits credentials and redirects to dashboard', async () => {
    vi.mocked(loginApi).mockResolvedValue({
      token: 'token-manager',
      user: {
        username: 'manager',
        realName: '项目经理',
        role: 'MANAGER',
      },
    })
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter()
    await router.push('/login')
    await router.isReady()

    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia, router],
      },
    })

    await wrapper.find('input[name="username"]').setValue('manager')
    await wrapper.find('input[name="password"]').setValue('manager123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    await flushPromises()

    expect(loginApi).toHaveBeenCalledWith({
      username: 'manager',
      password: 'manager123',
    })
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })
})
