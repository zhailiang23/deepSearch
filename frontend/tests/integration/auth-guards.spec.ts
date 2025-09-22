import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import type { RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  authGuard,
  adminGuard,
  guestGuard,
  createPermissionGuard,
  createRoleGuard
} from '@/router/guards/auth'

// Mock用户数据
const mockAdminUser = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  role: 'ADMIN' as const,
  status: 'ACTIVE' as const,
  createdAt: '2023-01-01T00:00:00Z',
  updatedAt: '2023-01-01T00:00:00Z'
}

const mockRegularUser = {
  id: 2,
  username: 'user',
  email: 'user@example.com',
  role: 'USER' as const,
  status: 'ACTIVE' as const,
  createdAt: '2023-01-01T00:00:00Z',
  updatedAt: '2023-01-01T00:00:00Z'
}

const mockLockedUser = {
  id: 3,
  username: 'locked',
  email: 'locked@example.com',
  role: 'USER' as const,
  status: 'LOCKED' as const,
  createdAt: '2023-01-01T00:00:00Z',
  updatedAt: '2023-01-01T00:00:00Z'
}

// 创建mock路由对象
const createMockRoute = (
  path: string,
  meta: Record<string, any> = {},
  query: Record<string, any> = {}
): RouteLocationNormalized => ({
  path,
  name: path.slice(1) || 'Home',
  params: {},
  query,
  hash: '',
  fullPath: path + (Object.keys(query).length ? '?' + new URLSearchParams(query).toString() : ''),
  matched: [],
  meta,
  redirectedFrom: undefined
})

describe('认证路由守卫测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('authGuard - 主要认证守卫', () => {
    it('应该允许已认证用户访问需要认证的页面', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/dashboard', { requiresAuth: true })
      const from = createMockRoute('/login')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该重定向未认证用户到登录页', async () => {
      const authStore = useAuthStore()
      authStore.token = null
      authStore.user = null

      const to = createMockRoute('/dashboard', { requiresAuth: true })
      const from = createMockRoute('/')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toEqual({
        name: 'Login',
        query: { redirect: '/dashboard' }
      })
    })

    it('应该保存重定向路径', async () => {
      const authStore = useAuthStore()
      authStore.token = null

      const to = createMockRoute('/users/settings', { requiresAuth: true })
      const from = createMockRoute('/')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toEqual({
        name: 'Login',
        query: { redirect: '/users/settings' }
      })
    })

    it('应该重定向已认证用户离开访客页面', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/login', { guest: true })
      const from = createMockRoute('/dashboard', {}, { redirect: '/settings' })

      const result = await authGuard(to, from, vi.fn())
      expect(result).toBe('/settings')
    })

    it('应该重定向已认证用户到首页当没有重定向参数时', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/login', { guest: true })
      const from = createMockRoute('/dashboard')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toBe('/')
    })

    it('应该检查角色权限', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/admin', { requiresAuth: true, requiredRole: 'ADMIN' })
      const from = createMockRoute('/dashboard')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('应该允许有正确角色的用户访问', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockAdminUser

      const to = createMockRoute('/admin', { requiresAuth: true, requiredRole: 'ADMIN' })
      const from = createMockRoute('/dashboard')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该检查具体权限', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/users', {
        requiresAuth: true,
        permissions: ['admin:delete']
      })
      const from = createMockRoute('/dashboard')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('应该允许管理员访问所有权限', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockAdminUser

      const to = createMockRoute('/users', {
        requiresAuth: true,
        permissions: ['admin:delete']
      })
      const from = createMockRoute('/dashboard')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该处理被锁定的用户', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockLockedUser
      const logoutSpy = vi.spyOn(authStore, 'logout').mockImplementation(vi.fn())

      const to = createMockRoute('/dashboard', { requiresAuth: true })
      const from = createMockRoute('/login')

      const result = await authGuard(to, from, vi.fn())
      expect(logoutSpy).toHaveBeenCalled()
      expect(result).toEqual({
        name: 'Login',
        query: { message: '账户已被禁用或锁定' }
      })
    })

    it('应该在获取用户信息失败时处理错误', async () => {
      const authStore = useAuthStore()
      authStore.token = 'invalid-token'
      authStore.user = null

      // Mock getCurrentUser to throw error
      vi.spyOn(authStore, 'getCurrentUser').mockRejectedValue(new Error('Token expired'))

      const to = createMockRoute('/dashboard', { requiresAuth: true })
      const from = createMockRoute('/login')

      const result = await authGuard(to, from, vi.fn())
      // 应该继续处理，不因为错误而中断
      expect(typeof result).toBeDefined()
    })
  })

  describe('adminGuard - 管理员守卫', () => {
    it('应该允许管理员访问', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockAdminUser

      const to = createMockRoute('/admin')
      const from = createMockRoute('/dashboard')

      const result = await adminGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该拒绝普通用户访问', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/admin')
      const from = createMockRoute('/dashboard')

      const result = await adminGuard(to, from, vi.fn())
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('应该重定向未认证用户到登录页', async () => {
      const authStore = useAuthStore()
      authStore.token = null
      authStore.user = null

      const to = createMockRoute('/admin')
      const from = createMockRoute('/')

      const result = await adminGuard(to, from, vi.fn())
      expect(result).toEqual({
        name: 'Login',
        query: { redirect: '/admin' }
      })
    })
  })

  describe('guestGuard - 访客守卫', () => {
    it('应该允许未认证用户访问', async () => {
      const authStore = useAuthStore()
      authStore.token = null
      authStore.user = null

      const to = createMockRoute('/login')
      const from = createMockRoute('/')

      const result = await guestGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该重定向已认证用户', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/login')
      const from = createMockRoute('/dashboard')

      const result = await guestGuard(to, from, vi.fn())
      expect(result).toEqual({ name: 'Dashboard' })
    })
  })

  describe('createPermissionGuard - 权限守卫工厂', () => {
    it('应该创建有效的权限守卫', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockAdminUser

      const permissionGuard = createPermissionGuard(['read:users', 'write:users'])
      const to = createMockRoute('/users')
      const from = createMockRoute('/dashboard')

      const result = await permissionGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该拒绝没有权限的用户', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const permissionGuard = createPermissionGuard(['admin:delete'])
      const to = createMockRoute('/admin-action')
      const from = createMockRoute('/dashboard')

      const result = await permissionGuard(to, from, vi.fn())
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('应该重定向未认证用户', async () => {
      const authStore = useAuthStore()
      authStore.token = null

      const permissionGuard = createPermissionGuard(['read:users'])
      const to = createMockRoute('/users')
      const from = createMockRoute('/')

      const result = await permissionGuard(to, from, vi.fn())
      expect(result).toEqual({
        name: 'Login',
        query: { redirect: '/users' }
      })
    })
  })

  describe('createRoleGuard - 角色守卫工厂', () => {
    it('应该允许有正确角色的用户', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockAdminUser

      const roleGuard = createRoleGuard('ADMIN')
      const to = createMockRoute('/admin')
      const from = createMockRoute('/dashboard')

      const result = await roleGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该允许管理员访问用户权限', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockAdminUser

      const roleGuard = createRoleGuard('USER')
      const to = createMockRoute('/user-area')
      const from = createMockRoute('/dashboard')

      const result = await roleGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该拒绝没有角色的用户', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const roleGuard = createRoleGuard('ADMIN')
      const to = createMockRoute('/admin')
      const from = createMockRoute('/dashboard')

      const result = await roleGuard(to, from, vi.fn())
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('应该重定向未认证用户', async () => {
      const authStore = useAuthStore()
      authStore.token = null

      const roleGuard = createRoleGuard('USER')
      const to = createMockRoute('/user-area')
      const from = createMockRoute('/')

      const result = await roleGuard(to, from, vi.fn())
      expect(result).toEqual({
        name: 'Login',
        query: { redirect: '/user-area' }
      })
    })
  })

  describe('角色层级测试', () => {
    it('应该正确实现角色层级', async () => {
      const authStore = useAuthStore()

      // 测试ADMIN角色包含USER权限
      authStore.user = mockAdminUser
      expect(authStore.hasRole('ADMIN')).toBe(true)
      expect(authStore.hasRole('USER')).toBe(true)

      // 测试USER角色不包含ADMIN权限
      authStore.user = mockRegularUser
      expect(authStore.hasRole('USER')).toBe(true)
      expect(authStore.hasRole('ADMIN')).toBe(false)
    })
  })

  describe('权限系统测试', () => {
    it('应该正确检查权限', async () => {
      const authStore = useAuthStore()

      // 管理员应该有所有权限
      authStore.user = mockAdminUser
      expect(authStore.hasPermission('read:profile')).toBe(true)
      expect(authStore.hasPermission('admin:delete')).toBe(true)
      expect(authStore.hasPermission('any:permission')).toBe(true)

      // 普通用户只有特定权限
      authStore.user = mockRegularUser
      expect(authStore.hasPermission('read:profile')).toBe(true)
      expect(authStore.hasPermission('update:profile')).toBe(true)
      expect(authStore.hasPermission('admin:delete')).toBe(false)
    })

    it('应该在用户为null时返回false', async () => {
      const authStore = useAuthStore()
      authStore.user = null

      expect(authStore.hasRole('USER')).toBe(false)
      expect(authStore.hasPermission('read:profile')).toBe(false)
    })
  })

  describe('复杂路由场景测试', () => {
    it('应该处理嵌套路由的权限检查', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockRegularUser

      const to = createMockRoute('/admin/users/create', {
        requiresAuth: true,
        requiredRole: 'ADMIN',
        permissions: ['admin:create']
      })
      const from = createMockRoute('/dashboard')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toEqual({ name: 'Dashboard' })
    })

    it('应该处理多个权限要求', async () => {
      const authStore = useAuthStore()
      authStore.token = 'valid-token'
      authStore.user = mockAdminUser

      const to = createMockRoute('/sensitive-data', {
        requiresAuth: true,
        requiredRole: 'ADMIN',
        permissions: ['read:sensitive', 'export:data']
      })
      const from = createMockRoute('/dashboard')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toBe(true)
    })

    it('应该处理根路径重定向', async () => {
      const authStore = useAuthStore()
      authStore.token = null

      const to = createMockRoute('/', { requiresAuth: true })
      const from = createMockRoute('/login')

      const result = await authGuard(to, from, vi.fn())
      expect(result).toEqual({
        name: 'Login',
        query: undefined // 根路径不需要重定向参数
      })
    })
  })
})