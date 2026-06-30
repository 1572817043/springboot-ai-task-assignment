export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export type Role = 'ADMIN' | 'MANAGER' | 'MEMBER'

export type TaskStatus =
  | 'UNASSIGNED'
  | 'TODO'
  | 'IN_PROGRESS'
  | 'WAIT_REVIEW'
  | 'COMPLETED'

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type ProjectStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED'

export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type UserStatus = 'ENABLED' | 'DISABLED'
