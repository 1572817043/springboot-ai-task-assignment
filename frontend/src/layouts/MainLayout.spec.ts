import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import MainLayout from './MainLayout.vue'

describe('MainLayout', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('renders the core navigation and page title', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: { template: '<div>页面内容</div>' },
          meta: { title: '工作台' },
        },
      ],
    })

    router.push('/')
    await router.isReady()

    const wrapper = mount(MainLayout, {
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.text()).toContain('工作台')
    expect(wrapper.text()).toContain('项目管理')
    expect(wrapper.text()).toContain('任务列表')
    expect(wrapper.text()).toContain('AI 分配')
    expect(wrapper.text()).toContain('AI 知识库')
    expect(wrapper.text()).toContain('成员画像')
    expect(wrapper.text()).toContain('页面内容')
  })

  it('shows logout button when user menu is clicked', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: { template: '<div />' },
          meta: { title: '工作台' },
        },
      ],
    })

    router.push('/')
    await router.isReady()

    const wrapper = mount(MainLayout, {
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.find('[data-testid="logout-button"]').exists()).toBe(false)

    await wrapper.find('[data-testid="user-menu-button"]').trigger('click')

    expect(wrapper.find('[data-testid="logout-button"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="logout-button"]').text()).toContain('退出登录')
  })
})
