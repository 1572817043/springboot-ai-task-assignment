import request from './request'
import type { PageResult } from './types'

export interface UserListItem {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  status: string
  roleCode: string
  roleName: string
  createdAt: string
}

export interface UserDetail {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  avatarUrl: string
  status: string
  roleId: number
  roleCode: string
  roleName: string
  createdAt: string
  updatedAt: string
}

export interface UserCreatePayload {
  username: string
  password: string
  realName: string
  email?: string
  phone?: string
  roleId: number
}

export interface UserUpdatePayload {
  realName: string
  email?: string
  phone?: string
  roleId: number
}

export function getUserList(params?: {
  keyword?: string
  status?: string
  page?: number
  size?: number
}): Promise<PageResult<UserListItem>> {
  return request.get('/users', { params }) as unknown as Promise<PageResult<UserListItem>>
}

export function getUserDetail(id: number): Promise<UserDetail> {
  return request.get(`/users/${id}`) as unknown as Promise<UserDetail>
}

export function createUser(payload: UserCreatePayload): Promise<void> {
  return request.post('/users', payload) as unknown as Promise<void>
}

export function updateUser(id: number, payload: UserUpdatePayload): Promise<void> {
  return request.put(`/users/${id}`, payload) as unknown as Promise<void>
}

export function updateUserStatus(id: number, status: string): Promise<void> {
  return request.patch(`/users/${id}/status`, { status }) as unknown as Promise<void>
}

export function updatePassword(id: number, password: string): Promise<void> {
  return request.patch(`/users/${id}/password`, { password }) as unknown as Promise<void>
}
