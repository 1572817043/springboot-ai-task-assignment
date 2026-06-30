import request from './request'
import type { PageResult } from './types'

export interface SkillListItem {
  id: number
  skillName: string
  category: string
  description: string
  createdAt: string
  updatedAt: string
}

export interface SkillDetail {
  id: number
  skillName: string
  category: string
  description: string
  createdAt: string
  updatedAt: string
}

export interface SkillOption {
  id: number
  skillName: string
  category: string
}

export interface SkillCreatePayload {
  skillName: string
  category: string
  description?: string
}

export interface SkillUpdatePayload {
  skillName: string
  category: string
  description?: string
}

export function getSkillList(params?: {
  keyword?: string
  category?: string
  page?: number
  size?: number
}): Promise<PageResult<SkillListItem>> {
  return request.get('/skills', { params }) as unknown as Promise<PageResult<SkillListItem>>
}

export function getSkillDetail(id: number): Promise<SkillDetail> {
  return request.get(`/skills/${id}`) as unknown as Promise<SkillDetail>
}

export function getSkillOptions(): Promise<SkillOption[]> {
  return request.get('/skills/options') as unknown as Promise<SkillOption[]>
}

export function createSkill(payload: SkillCreatePayload): Promise<void> {
  return request.post('/skills', payload) as unknown as Promise<void>
}

export function updateSkill(id: number, payload: SkillUpdatePayload): Promise<void> {
  return request.put(`/skills/${id}`, payload) as unknown as Promise<void>
}

export function deleteSkill(id: number): Promise<void> {
  return request.delete(`/skills/${id}`) as unknown as Promise<void>
}
