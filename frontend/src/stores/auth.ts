import { defineStore } from 'pinia'

import {
  getCurrentUser,
  login as loginApi,
  type CurrentUser,
  type LoginPayload,
} from '../api/auth'

function readStoredUser(): CurrentUser | null {
  const value = localStorage.getItem('user')
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as CurrentUser
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') ?? '',
    user: readStoredUser(),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    async login(payload: LoginPayload) {
      const result = await loginApi(payload)
      this.token = result.token
      this.user = result.user
      localStorage.setItem('token', result.token)
      localStorage.setItem('user', JSON.stringify(result.user))
    },
    async fetchCurrentUser() {
      const user = await getCurrentUser()
      this.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
  },
})
