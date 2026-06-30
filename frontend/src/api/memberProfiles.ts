import request from './request'
import type { PageResult } from './types'

export interface MemberSkill {
  skillId: number
  skillName: string
  category: string
  level: number
  years: number
  description: string
}

export interface MemberProfileListItem {
  userId: number
  username: string
  realName: string
  email: string
  currentWorkload: number
  completedTaskCount: number
  overdueTaskCount: number
  taskCompletionRate: number
  overdueRate: number
  skills: string[]
}

export interface MemberProfileDetail {
  userId: number
  username: string
  realName: string
  email: string
  phone: string
  resumeText: string
  experienceSummary: string
  currentWorkload: number
  completedTaskCount: number
  overdueTaskCount: number
  taskCompletionRate: number
  overdueRate: number
  skills: MemberSkill[]
}

export interface MemberProfileUpdatePayload {
  phone?: string
  resumeText?: string
  experienceSummary?: string
}

export interface MemberSkillItem {
  skillId: number
  level: number
  years?: number
  description?: string
}

export function getMemberProfileList(params?: {
  keyword?: string
  skillId?: number
  page?: number
  size?: number
}): Promise<PageResult<MemberProfileListItem>> {
  return request.get('/member-profiles', { params }) as unknown as Promise<PageResult<MemberProfileListItem>>
}

export function getMemberProfileDetail(userId: number): Promise<MemberProfileDetail> {
  return request.get(`/member-profiles/${userId}`) as unknown as Promise<MemberProfileDetail>
}

export function updateMemberProfile(userId: number, payload: MemberProfileUpdatePayload): Promise<void> {
  return request.put(`/member-profiles/${userId}`, payload) as unknown as Promise<void>
}

export function saveMemberSkills(userId: number, skills: MemberSkillItem[]): Promise<void> {
  return request.put(`/member-profiles/${userId}/skills`, { skills }) as unknown as Promise<void>
}
