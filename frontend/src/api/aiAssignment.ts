import request from './request'

export interface AiCandidate {
  candidateId: number
  candidateUserId: number
  candidateName: string
  rankNo: number
  totalScore: number
  skillScore: number
  historyScore: number
  workloadScore: number
  completionScore: number
  deadlineRiskScore: number
  reason: string
  accepted: number
}

export interface AiRecommendation {
  batchId: number
  taskId: number
  candidates: AiCandidate[]
}

export function recommend(taskId: number): Promise<AiRecommendation> {
  return request.post(`/ai-assignment/tasks/${taskId}/recommend`) as unknown as Promise<AiRecommendation>
}

export function getLatestRecommendations(taskId: number): Promise<AiRecommendation> {
  return request.get(`/ai-assignment/tasks/${taskId}/recommendations/latest`) as unknown as Promise<AiRecommendation>
}

export function acceptCandidate(candidateId: number): Promise<void> {
  return request.patch(`/ai-assignment/candidates/${candidateId}/accept`) as unknown as Promise<void>
}
