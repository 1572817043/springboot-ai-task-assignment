import request from './request'
import type { PageResult } from './types'

export interface KnowledgeDocument {
  id: number
  userId: number
  sourceType: string
  sourceId: number
  title: string
  content: string
  indexed: number
  createdAt: string
  updatedAt: string
}

export function syncMemberResume(userId: number): Promise<void> {
  return request.post(`/ai-knowledge/sync/member/${userId}/resume`) as unknown as Promise<void>
}

export function syncTaskResult(taskId: number): Promise<void> {
  return request.post(`/ai-knowledge/sync/tasks/${taskId}/result`) as unknown as Promise<void>
}

export function getKnowledgeDocuments(params?: {
  sourceType?: string
  keyword?: string
  indexed?: number
  page?: number
  size?: number
}): Promise<PageResult<KnowledgeDocument>> {
  return request.get('/ai-knowledge/documents', { params }) as unknown as Promise<PageResult<KnowledgeDocument>>
}

export function getKnowledgeDocument(id: number): Promise<KnowledgeDocument> {
  return request.get(`/ai-knowledge/documents/${id}`) as unknown as Promise<KnowledgeDocument>
}

export function updateDocumentIndexed(id: number, indexed: number): Promise<void> {
  return request.patch(`/ai-knowledge/documents/${id}/indexed`, { indexed }) as unknown as Promise<void>
}
