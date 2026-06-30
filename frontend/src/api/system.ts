import request from './request'

export interface HealthStatus {
  status: string
  service: string
}

export function healthCheck(): Promise<HealthStatus> {
  return request.get('/system/health') as unknown as Promise<HealthStatus>
}
