import { createRouter, createWebHistory } from 'vue-router'
import { authGuard } from './guards/auth'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/pages/Dashboard.vue'),
        meta: {
          requiresAuth: true,
          title: '控制台'
        }
      },
      {
        path: '/users',
        name: 'Users',
        component: () => import('@/pages/users/UserList.vue'),
        meta: {
          requiresAuth: true,
          requiredRole: 'ADMIN',
          permissions: ['read:users'],
          title: '用户管理'
        }
      },
      {
        path: '/settings',
        name: 'Settings',
        component: () => import('@/pages/Settings.vue'),
        meta: {
          requiresAuth: true,
          title: '系统设置'
        }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/Login.vue'),
    meta: {
      guest: true,
      title: '登录'
    }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/pages/error/403.vue'),
    meta: {
      title: '访问被拒绝'
    }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/pages/error/404.vue'),
    meta: {
      title: '页面未找到'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 注册全局路由守卫
router.beforeEach(authGuard)

// 设置页面标题
router.afterEach((to) => {
  const defaultTitle = 'DeepSearch'
  document.title = to.meta.title ? `${to.meta.title} - ${defaultTitle}` : defaultTitle
})

export default router