import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useAuthStore } from '../stores/auth'
import { createAppRouter } from './index'

describe('router auth guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('redirects protected pages to login when token is missing', async () => {
    const router = createAppRouter()

    await router.push('/dashboard')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('allows protected pages when token exists', async () => {
    localStorage.setItem('token', 'token-manager')
    const router = createAppRouter()

    await router.push('/dashboard')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('redirects to login after logout when accessing protected page', async () => {
    localStorage.setItem('token', 'token-manager')
    const router = createAppRouter()

    await router.push('/dashboard')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/dashboard')

    const store = useAuthStore()
    store.logout()

    await router.push('/projects')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('redirects to dashboard when logged-in user visits login page', async () => {
    localStorage.setItem('token', 'token-manager')
    const router = createAppRouter()

    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('has /knowledge route and allows access when authenticated', async () => {
    localStorage.setItem('token', 'token-manager')
    const router = createAppRouter()

    await router.push('/knowledge')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/knowledge')
    expect(router.currentRoute.value.name).toBe('knowledge')
  })
})
