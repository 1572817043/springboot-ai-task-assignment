import request from './request'

export interface LoginPayload {
  username: string
  password: string
}

export interface CurrentUser {
  id: number
  username: string
  realName: string
  role: 'ADMIN' | 'MANAGER' | 'MEMBER'
}

export interface LoginResult {
  token: string
  user: CurrentUser
}

export function login(payload: LoginPayload): Promise<LoginResult> {
  return request.post('/auth/login', payload) as unknown as Promise<LoginResult>
}

export function getCurrentUser(): Promise<CurrentUser> {
  return request.get('/auth/me') as unknown as Promise<CurrentUser>
}
