import request from './request'

export interface RoleOption {
  id: number
  roleCode: string
  roleName: string
}

export function getRoleOptions(): Promise<RoleOption[]> {
  return request.get('/roles/options') as unknown as Promise<RoleOption[]>
}
