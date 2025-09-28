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
        path: 'search-logs',
        name: 'SearchLogs',
        component: () => import('@/views/admin/SearchLogManagePage.vue'),
        meta: {
          requiresAuth: true,
          title: '搜索日志管理'
        }
      },
      {
        path: 'hot-word-statistics',
        name: 'HotWordStatistics',
        component: () => import('@/views/statistics/HotWordStatisticsPage.vue'),
        meta: {
          requiresAuth: true,
          title: '热词统计分析'
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
        path: 'roles',
        name: 'Roles',
        component: () => import('@/pages/roles/RoleManage.vue'),
        meta: {
          requiresAuth: true,
          title: '角色管理'
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
      },
      {
        path: 'codemirror-test',
        name: 'CodeMirrorTest',
        component: () => import('@/components/CodeMirrorTest.vue'),
        meta: {
          title: 'CodeMirror 测试'
        }
      },
      {
        path: 'mobile-search-demo',
        name: 'MobileSearchDemo',
        component: () => import('@/pages/MobileSearchDemo.vue'),
        meta: {
          requiresAuth: false,
          title: '移动搜索演示'
        }
      },
      {
        path: 'phone-simulator-test',
        name: 'PhoneSimulatorTest',
        component: () => import('@/views/PhoneSimulatorTest.vue'),
        meta: {
          requiresAuth: false,
          title: 'iPhone设备模拟器测试'
        }
      },
      {
        path: 'wordcloud-test',
        name: 'WordCloudTest',
        component: () => import('@/pages/WordCloudTest.vue'),
        meta: {
          requiresAuth: false,
          title: '词云图组件测试'
        }
      },
      {
        path: 'hot-word-statistics',
        name: 'HotWordStatistics',
        component: () => import('@/views/statistics/HotWordStatisticsPage.vue'),
        meta: {
          requiresAuth: true,
          title: '热词统计分析'
        }
      },
      {
        path: 'admin/hot-word-statistics',
        name: 'AdminHotWordStatistics',
        component: () => import('@/views/admin/HotWordStatisticsPage.vue'),
        meta: {
          requiresAuth: true,
          title: '热词统计分析 - 管理后台'
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