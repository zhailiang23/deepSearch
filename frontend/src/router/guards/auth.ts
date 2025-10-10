import type { NavigationGuard, RouteLocationNormalized } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 认证守卫接口
 */
export interface AuthGuard {
  beforeEnter: NavigationGuard
}

/**
 * 路由元信息接口
 */
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guest?: boolean
    requiredRole?: string
    permissions?: string[]
  }
}

/**
 * 检查用户是否有指定角色
 */
function hasRole(userRole: string, requiredRole: string): boolean {
  const roleHierarchy = {
    ADMIN: ['ADMIN', 'USER'],
    USER: ['USER']
  }

  return roleHierarchy[userRole as keyof typeof roleHierarchy]?.includes(requiredRole) ?? false
}

/**
 * 检查用户是否有指定权限
 */
function hasPermissions(userRole: string, requiredPermissions: string[]): boolean {
  // 管理员拥有所有权限
  if (userRole === 'ADMIN') {
    return true
  }

  // 这里可以根据实际需求实现更复杂的权限检查逻辑
  const userPermissions = {
    USER: ['read:profile', 'update:profile'],
    ADMIN: ['*'] // 所有权限
  }

  const permissions = userPermissions[userRole as keyof typeof userPermissions] || []

  return requiredPermissions.every(permission =>
    permissions.includes('*') || permissions.includes(permission)
  )
}

/**
 * 主要认证守卫
 */
export const authGuard: NavigationGuard = async (to, from) => {
  const authStore = useAuthStore()

  try {
    // 如果用户已登录但没有用户信息，尝试获取用户信息
    if (authStore.isAuthenticated && !authStore.user) {
      await authStore.getCurrentUser()
    }
  } catch (error) {
    // 获取用户信息失败，可能token已过期
    console.warn('Failed to get current user:', error)
  }

  // 检查是否需要认证
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // 保存原目标页面，用于登录后重定向
    const redirect = to.fullPath !== '/' ? to.fullPath : undefined
    return {
      name: 'Login',
      query: redirect ? { redirect } : undefined
    }
  }

  // 检查是否为访客页面（如登录页）
  if (to.meta.guest && authStore.isAuthenticated) {
    // 如果有重定向参数，跳转到指定页面，否则跳转到首页
    const redirect = from.query.redirect as string
    return redirect && redirect !== '/login' ? redirect : '/'
  }

  // 检查角色权限
  if (to.meta.requiredRole && authStore.user) {
    const userRole = authStore.user.roleCode
    if (!hasRole(userRole, to.meta.requiredRole)) {
      return { name: 'Dashboard' } // 或者跳转到403页面
    }
  }

  // 检查具体权限
  if (to.meta.permissions && authStore.user) {
    const userRole = authStore.user.roleCode
    if (!hasPermissions(userRole, to.meta.permissions)) {
      return { name: 'Dashboard' } // 或者跳转到403页面
    }
  }

  // 检查用户状态
  if (authStore.user && authStore.user.status !== 'ACTIVE') {
    // 用户被禁用或锁定，强制登出
    authStore.logout()
    return {
      name: 'Login',
      query: { message: '账户已被禁用或锁定' }
    }
  }

  return true
}

/**
 * 管理员角色守卫
 */
export const adminGuard: NavigationGuard = (to, from) => {
  const authStore = useAuthStore()

  if (!authStore.isAuthenticated) {
    return {
      name: 'Login',
      query: { redirect: to.fullPath }
    }
  }

  const userRole = authStore.user?.roleCode
  if (!authStore.user || userRole !== 'ADMIN') {
    return { name: 'Dashboard' }
  }

  return true
}

/**
 * 访客守卫（仅未登录用户可访问）
 */
export const guestGuard: NavigationGuard = (to, from) => {
  const authStore = useAuthStore()

  if (authStore.isAuthenticated) {
    return { name: 'Dashboard' }
  }

  return true
}

/**
 * 创建权限守卫
 */
export function createPermissionGuard(permissions: string[]): NavigationGuard {
  return (to, from) => {
    const authStore = useAuthStore()

    if (!authStore.isAuthenticated) {
      return {
        name: 'Login',
        query: { redirect: to.fullPath }
      }
    }

    const userRole = authStore.user?.roleCode || ''
    if (!authStore.user || !hasPermissions(userRole, permissions)) {
      return { name: 'Dashboard' }
    }

    return true
  }
}

/**
 * 创建角色守卫
 */
export function createRoleGuard(requiredRole: string): NavigationGuard {
  return (to, from) => {
    const authStore = useAuthStore()

    if (!authStore.isAuthenticated) {
      return {
        name: 'Login',
        query: { redirect: to.fullPath }
      }
    }

    const userRole = authStore.user?.roleCode || ''
    if (!authStore.user || !hasRole(userRole, requiredRole)) {
      return { name: 'Dashboard' }
    }

    return true
  }
}