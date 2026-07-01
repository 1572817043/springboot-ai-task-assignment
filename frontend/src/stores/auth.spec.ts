import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { login as loginApi } from '../api/auth'
import { useAuthStore } from './auth'

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(),
}))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('logs in and persists token with current user', async () => {
    vi.mocked(loginApi).mockResolvedValue({
      token: 'token-manager',
      user: {
        id: 2,
        username: 'manager',
        realName: '项目经理',
        role: 'MANAGER',
      },
    })

    const store = useAuthStore()
    await store.login({
      username: 'manager',
      password: 'manager123',
    })

    expect(store.token).toBe('token-manager')
    expect(store.user?.role).toBe('MANAGER')
    expect(localStorage.getItem('token')).toBe('token-manager')
    expect(localStorage.getItem('user')).toContain('项目经理')
  })

  it('logout clears token and user from store and localStorage', () => {
    localStorage.setItem('token', 'token-manager')
    localStorage.setItem('user', JSON.stringify({ id: 2, username: 'manager', realName: '项目经理', role: 'MANAGER' }))

    const store = useAuthStore()
    store.token = 'token-manager'
    store.user = { id: 2, username: 'manager', realName: '项目经理', role: 'MANAGER' }

    store.logout()

    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })
})
