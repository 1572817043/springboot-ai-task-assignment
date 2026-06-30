import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import UserSettingsView from './UserSettingsView.vue'

const mockGetUserList = vi.fn()
const mockCreateUser = vi.fn()
const mockUpdateUser = vi.fn()
const mockUpdateUserStatus = vi.fn()
const mockUpdatePassword = vi.fn()
const mockGetRoleOptions = vi.fn()

vi.mock('../../api/users', () => ({
  getUserList: (...args: unknown[]) => mockGetUserList(...args),
  createUser: (...args: unknown[]) => mockCreateUser(...args),
  updateUser: (...args: unknown[]) => mockUpdateUser(...args),
  updateUserStatus: (...args: unknown[]) => mockUpdateUserStatus(...args),
  updatePassword: (...args: unknown[]) => mockUpdatePassword(...args),
}))

vi.mock('../../api/roles', () => ({
  getRoleOptions: (...args: unknown[]) => mockGetRoleOptions(...args),
}))

function sampleUsers() {
  return {
    records: [
      {
        id: 1,
        username: 'admin',
        realName: '管理员',
        email: 'admin@test.com',
        phone: '13800000001',
        status: 'ENABLED',
        roleCode: 'ADMIN',
        roleName: '管理员',
        createdAt: '2026-06-01T10:00:00',
      },
      {
        id: 2,
        username: 'manager',
        realName: '项目经理',
        email: '',
        phone: '',
        status: 'ENABLED',
        roleCode: 'MANAGER',
        roleName: '项目经理',
        createdAt: '2026-06-02T10:00:00',
      },
    ],
    total: 2,
    size: 10,
    current: 1,
    pages: 1,
  }
}

function sampleRoles() {
  return [
    { id: 1, roleCode: 'ADMIN', roleName: '管理员' },
    { id: 2, roleCode: 'MANAGER', roleName: '项目经理' },
    { id: 3, roleCode: 'MEMBER', roleName: '成员' },
  ]
}

describe('UserSettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetUserList.mockResolvedValue(sampleUsers())
    mockGetRoleOptions.mockResolvedValue(sampleRoles())
    mockCreateUser.mockResolvedValue(undefined)
    mockUpdateUser.mockResolvedValue(undefined)
    mockUpdateUserStatus.mockResolvedValue(undefined)
    mockUpdatePassword.mockResolvedValue(undefined)
  })

  it('loads user list and role options on mount', async () => {
    const wrapper = mount(UserSettingsView)
    await flushPromises()

    expect(mockGetUserList).toHaveBeenCalled()
    expect(mockGetRoleOptions).toHaveBeenCalled()
    expect(wrapper.text()).toContain('admin')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('项目经理')
    expect(wrapper.text()).toContain('启用')
  })

  it('searches with keyword and reloads list', async () => {
    const wrapper = mount(UserSettingsView)
    await flushPromises()

    const keywordInput = wrapper.find('input[placeholder="搜索用户名/姓名"]')
    await keywordInput.setValue('admin')

    const searchButton = wrapper.findAll('button').find((b) => b.text() === '搜索')!
    await searchButton.trigger('click')
    await flushPromises()

    expect(mockGetUserList).toHaveBeenLastCalledWith({
      keyword: 'admin',
      status: undefined,
      page: 1,
      size: 10,
    })
  })

  it('creates a user and refreshes list', async () => {
    const wrapper = mount(UserSettingsView)
    await flushPromises()

    // 打开新增弹层
    const addButton = wrapper.findAll('button').find((b) => b.text() === '新增用户')!
    await addButton.trigger('click')

    // 填写表单
    const inputs = wrapper.findAll('.fixed input')
    await inputs[0].setValue('newuser') // username
    await inputs[1].setValue('pass123') // password
    await inputs[2].setValue('新用户') // realName

    // 选择角色
    const select = wrapper.findAll('.fixed select')[0]
    await select.setValue(3) // MEMBER

    // 提交
    const saveButton = wrapper.findAll('.fixed button').find((b) => b.text() === '保存')!
    await saveButton.trigger('submit')
    await flushPromises()

    expect(mockCreateUser).toHaveBeenCalledWith({
      username: 'newuser',
      password: 'pass123',
      realName: '新用户',
      email: undefined,
      phone: undefined,
      roleId: 3,
    })
    expect(mockGetUserList).toHaveBeenCalledTimes(2)
  })

  it('toggles user status', async () => {
    const wrapper = mount(UserSettingsView)
    await flushPromises()

    // 点击第一个用户的"禁用"按钮
    const disableButton = wrapper.findAll('button').find((b) => b.text() === '禁用')!
    await disableButton.trigger('click')
    await flushPromises()

    expect(mockUpdateUserStatus).toHaveBeenCalledWith(1, 'DISABLED')
    expect(mockGetUserList).toHaveBeenCalledTimes(2)
  })

  it('resets user password', async () => {
    const wrapper = mount(UserSettingsView)
    await flushPromises()

    // 点击"重置密码"
    const resetButton = wrapper.findAll('button').find((b) => b.text() === '重置密码')!
    await resetButton.trigger('click')
    await wrapper.vm.$nextTick()

    // 弹层出现
    expect(wrapper.text()).toContain('重置密码')
    expect(wrapper.text()).toContain('管理员')

    // 输入新密码
    const passwordInput = wrapper.find('.fixed input[type="password"]')
    await passwordInput.setValue('newpass456')

    // 确认
    const confirmButton = wrapper.findAll('.fixed button').find((b) => b.text() === '确认重置')!
    await confirmButton.trigger('click')
    await flushPromises()

    expect(mockUpdatePassword).toHaveBeenCalledWith(1, 'newpass456')
    // 弹层关闭
    expect(wrapper.findAll('.fixed').length).toBe(0)
  })

  it('shows empty state when no users', async () => {
    mockGetUserList.mockResolvedValue({
      records: [],
      total: 0,
      size: 10,
      current: 1,
      pages: 0,
    })

    const wrapper = mount(UserSettingsView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无用户数据')
  })

  it('shows error message when loading fails', async () => {
    mockGetUserList.mockRejectedValue(new Error('网络错误'))

    const wrapper = mount(UserSettingsView)
    await flushPromises()

    expect(wrapper.text()).toContain('网络错误')
  })
})
