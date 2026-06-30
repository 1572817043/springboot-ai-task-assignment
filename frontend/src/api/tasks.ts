import request from './request'
import type { PageResult, TaskPriority, TaskStatus, ReviewStatus } from './types'

export interface TaskRequiredSkill {
  skillId: number
  skillName: string
  category: string
  weight: number
}

export interface TaskStatusLog {
  oldStatus: string
  newStatus: string
  operatorName: string
  remark: string
  createdAt: string
}

export interface TaskResult {
  resultSummary: string
  resultUrl: string
  reviewStatus: ReviewStatus
  reviewComment: string
  submittedAt: string
  reviewedAt: string
}

export interface TaskListItem {
  id: number
  projectId: number
  projectName: string
  title: string
  priority: TaskPriority
  status: TaskStatus
  creatorId: number
  creatorName: string
  assigneeId: number
  assigneeName: string
  deadline: string
  estimatedHours: number
  createdAt: string
  updatedAt: string
}

export interface TaskDetail {
  id: number
  projectId: number
  projectName: string
  title: string
  description: string
  priority: TaskPriority
  status: TaskStatus
  creatorId: number
  creatorName: string
  assigneeId: number
  assigneeName: string
  deadline: string
  estimatedHours: number
  createdAt: string
  updatedAt: string
  requiredSkills: TaskRequiredSkill[]
  statusLogs: TaskStatusLog[]
  latestResult: TaskResult | null
}

export interface TaskCreatePayload {
  projectId: number
  title: string
  description?: string
  priority: TaskPriority
  deadline?: string
  estimatedHours?: number
  requiredSkills?: { skillId: number; weight: number }[]
}

export interface TaskUpdatePayload {
  title: string
  description?: string
  priority: TaskPriority
  deadline?: string
  estimatedHours?: number
  requiredSkills?: { skillId: number; weight: number }[]
}

export function getTaskList(params?: {
  projectId?: number
  status?: TaskStatus
  priority?: TaskPriority
  assigneeId?: number
  keyword?: string
  page?: number
  size?: number
}): Promise<PageResult<TaskListItem>> {
  return request.get('/tasks', { params }) as unknown as Promise<PageResult<TaskListItem>>
}

export function getTaskDetail(id: number): Promise<TaskDetail> {
  return request.get(`/tasks/${id}`) as unknown as Promise<TaskDetail>
}

export function createTask(payload: TaskCreatePayload): Promise<void> {
  return request.post('/tasks', payload) as unknown as Promise<void>
}

export function updateTask(id: number, payload: TaskUpdatePayload): Promise<void> {
  return request.put(`/tasks/${id}`, payload) as unknown as Promise<void>
}

export function updateTaskStatus(id: number, status: TaskStatus, remark?: string): Promise<void> {
  return request.patch(`/tasks/${id}/status`, { status, remark }) as unknown as Promise<void>
}

export function assignTask(id: number, assigneeId: number, reason?: string): Promise<void> {
  return request.patch(`/tasks/${id}/assignee`, { assigneeId, reason }) as unknown as Promise<void>
}

export function submitTaskResult(id: number, payload: { resultSummary: string; resultUrl?: string }): Promise<void> {
  return request.post(`/tasks/${id}/result`, payload) as unknown as Promise<void>
}

export function reviewTaskResult(id: number, payload: { reviewStatus: ReviewStatus; reviewComment?: string }): Promise<void> {
  return request.patch(`/tasks/${id}/result/review`, payload) as unknown as Promise<void>
}
