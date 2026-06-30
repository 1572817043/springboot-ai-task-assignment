import request from './request'
import type { PageResult, ProjectStatus } from './types'

export interface ProjectMember {
  userId: number
  username: string
  realName: string
  projectRole: string
  joinedAt: string
}

export interface ProjectListItem {
  id: number
  projectName: string
  description: string
  managerId: number
  managerName: string
  status: ProjectStatus
  startDate: string
  endDate: string
  memberCount: number
  createdAt: string
  updatedAt: string
}

export interface ProjectDetail {
  id: number
  projectName: string
  description: string
  managerId: number
  managerName: string
  status: ProjectStatus
  startDate: string
  endDate: string
  createdAt: string
  updatedAt: string
  members: ProjectMember[]
}

export interface ProjectCreatePayload {
  projectName: string
  description?: string
  managerId: number
  status?: ProjectStatus
  startDate?: string
  endDate?: string
}

export interface ProjectUpdatePayload {
  projectName: string
  description?: string
  managerId: number
  status?: ProjectStatus
  startDate?: string
  endDate?: string
}

export function getProjectList(params?: {
  keyword?: string
  status?: ProjectStatus
  page?: number
  size?: number
}): Promise<PageResult<ProjectListItem>> {
  return request.get('/projects', { params }) as unknown as Promise<PageResult<ProjectListItem>>
}

export function getProjectDetail(id: number): Promise<ProjectDetail> {
  return request.get(`/projects/${id}`) as unknown as Promise<ProjectDetail>
}

export function createProject(payload: ProjectCreatePayload): Promise<void> {
  return request.post('/projects', payload) as unknown as Promise<void>
}

export function updateProject(id: number, payload: ProjectUpdatePayload): Promise<void> {
  return request.put(`/projects/${id}`, payload) as unknown as Promise<void>
}

export function updateProjectStatus(id: number, status: ProjectStatus): Promise<void> {
  return request.patch(`/projects/${id}/status`, { status }) as unknown as Promise<void>
}

export function getProjectMembers(id: number): Promise<ProjectMember[]> {
  return request.get(`/projects/${id}/members`) as unknown as Promise<ProjectMember[]>
}

export function addProjectMember(projectId: number, userId: number, projectRole: string): Promise<void> {
  return request.post(`/projects/${projectId}/members`, { userId, projectRole }) as unknown as Promise<void>
}

export function removeProjectMember(projectId: number, userId: number): Promise<void> {
  return request.delete(`/projects/${projectId}/members/${userId}`) as unknown as Promise<void>
}
