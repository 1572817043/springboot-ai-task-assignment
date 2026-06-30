import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('axios', () => {
  const instance = {
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
    get: vi.fn(),
    post: vi.fn(),
  }
  return {
    default: {
      create: vi.fn(() => instance),
    },
  }
})

describe('request interceptor', () => {
  let responseSuccess: (response: unknown) => unknown

  beforeEach(() => {
    vi.resetModules()
    const mockAxios = vi.mocked(axios)
    mockAxios.create.mockReturnValue({
      interceptors: {
        request: { use: vi.fn() },
        response: {
          use: vi.fn((success) => {
            responseSuccess = success
          }),
        },
      },
    } as unknown as ReturnType<typeof axios.create>)
  })

  it('unwraps data when code is 200', async () => {
    await import('./request')
    const result = responseSuccess({ data: { code: 200, message: 'ok', data: { id: 1 } } })
    expect(result).toEqual({ id: 1 })
  })

  it('rejects when code is not 200', async () => {
    await import('./request')
    const result = responseSuccess({ data: { code: 400, message: '参数错误', data: null } })
    await expect(result).rejects.toThrow('参数错误')
  })

  it('rejects with default message when message is empty', async () => {
    await import('./request')
    const result = responseSuccess({ data: { code: 500, message: '', data: null } })
    await expect(result).rejects.toThrow('请求失败')
  })
})
