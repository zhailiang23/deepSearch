import { createRouter, createWebHistory } from 'vue-router'
import { authGuard } from './guards/auth'

const routes = [
  {
    path: '/',
    component: () => import('@/pages/DashboardSimple.vue'),
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/pages/DashboardContent.vue'),
        meta: {
          title: '控制台'
        }
      },
      {
        path: 'search-spaces',
        name: 'SearchSpaces',
        component: () => import('@/pages/searchSpace/SearchSpaceContent.vue'),
        meta: {
          requiresAuth: true,
          title: '搜索空间管理'
        }
      },
      {
        path: 'search-data',
        name: 'SearchData',
        component: () => import('@/views/SearchDataManagePage.vue'),
        meta: {
          requiresAuth: true,
          title: '搜索数据管理'
        }
      },
      {
        path: 'channels',
        name: 'Channels',
        component: () => import('@/pages/channels/ChannelManage.vue'),
        meta: {
          requiresAuth: true,
          title: '渠道管理'
        }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/pages/Settings.vue'),
        meta: {
          requiresAuth: true,
          title: '设置'
        }
      }
    ]
  },
  {
    path: '/old',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true },
    children: [
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
      },
      {
        path: '/search-spaces',
        name: 'OldSearchSpaces',
        component: () => import('@/pages/searchSpace/SearchSpaceManage.vue'),
        meta: {
          requiresAuth: true,
          title: '搜索空间管理（旧版）'
        }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/LoginSimple.vue'),
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

// 注册全局路由守卫 - 暂时注释掉
// router.beforeEach(authGuard)

// 设置页面标题
router.afterEach((to) => {
  const defaultTitle = 'DeepSearch'
  document.title = to.meta.title ? `${to.meta.title} - ${defaultTitle}` : defaultTitle
})

export default router