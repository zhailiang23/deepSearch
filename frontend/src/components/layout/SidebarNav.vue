<template>
  <div class="flex h-full flex-col">
    <!-- Logo区域 -->
    <div
      v-if="!isMobile"
      :class="[
        'flex items-center border-b transition-all duration-300',
        collapsed ? 'h-16 px-4 justify-center' : 'h-16 px-6',
      ]"
    >
      <router-link
        to="/"
        :class="[
          'flex items-center transition-all duration-300',
          collapsed ? 'space-x-0' : 'space-x-2'
        ]"
        :aria-label="collapsed ? '管理系统' : undefined"
      >
        <div
          :class="[
            'rounded bg-primary flex items-center justify-center',
            collapsed ? 'h-8 w-8' : 'h-8 w-8'
          ]"
        >
          <span class="text-primary-foreground text-sm font-bold">M</span>
        </div>
        <span
          v-if="!collapsed"
          class="font-semibold text-lg transition-opacity duration-300"
        >
          管理系统
        </span>
      </router-link>
    </div>

    <!-- 导航菜单 -->
    <nav
      :class="[
        'flex-1 overflow-y-auto',
        isMobile ? 'p-3' : (collapsed ? 'p-2' : 'p-4')
      ]"
    >
      <!-- 主要导航 -->
      <ul class="space-y-1">
        <li v-for="item in navigation" :key="item.name">
          <router-link
            :to="item.to"
            :class="[
              'flex items-center rounded-md text-sm font-medium transition-all duration-200',
              'hover:bg-accent hover:text-accent-foreground',
              'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
              // 响应式样式
              getNavItemClasses(item),
              // 激活状态
              isActiveRoute(item.to) ? 'bg-accent text-accent-foreground' : 'text-muted-foreground'
            ]"
            :aria-label="collapsed && !isMobile ? item.label : undefined"
            :title="collapsed && !isMobile ? item.label : undefined"
          >
            <component
              :is="item.icon"
              :class="[
                'flex-shrink-0 transition-all duration-200',
                isMobile ? 'h-5 w-5' : 'h-5 w-5'
              ]"
            />
            <span
              v-if="!collapsed || isMobile"
              :class="[
                'transition-all duration-300',
                collapsed && !isMobile ? 'opacity-0 w-0' : 'opacity-100'
              ]"
            >
              {{ item.label }}
            </span>
          </router-link>
        </li>
      </ul>

      <!-- 分隔线 -->
      <div
        v-if="(!collapsed || isMobile) && auxiliaryNavigation.length > 0"
        class="my-4 border-t border-border"
      />

      <!-- 辅助导航 -->
      <ul v-if="(!collapsed || isMobile) && auxiliaryNavigation.length > 0" class="space-y-1">
        <li v-for="item in auxiliaryNavigation" :key="item.name">
          <router-link
            :to="item.to"
            :class="[
              'flex items-center rounded-md text-sm font-medium transition-all duration-200',
              'hover:bg-accent hover:text-accent-foreground',
              'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
              getNavItemClasses(item),
              isActiveRoute(item.to) ? 'bg-accent text-accent-foreground' : 'text-muted-foreground'
            ]"
          >
            <component :is="item.icon" class="h-5 w-5 flex-shrink-0" />
            <span class="transition-all duration-300">
              {{ item.label }}
            </span>
          </router-link>
        </li>
      </ul>
    </nav>

    <!-- 底部区域 (移动端) -->
    <div
      v-if="isMobile"
      class="border-t p-4"
    >
      <div class="flex items-center space-x-3 px-3 py-2">
        <div class="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
          <User class="h-4 w-4 text-primary" />
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-medium truncate">管理员</p>
          <p class="text-xs text-muted-foreground truncate">admin@example.com</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Settings, User, Database, Store } from 'lucide-vue-next'

interface Props {
  collapsed?: boolean
  isMobile?: boolean
}

interface NavigationItem {
  name: string
  label: string
  to: string
  icon: any
}

const props = withDefaults(defineProps<Props>(), {
  collapsed: false,
  isMobile: false
})

const $route = useRoute()

// 主要导航菜单
const navigation = computed((): NavigationItem[] => {
  const nav = [
    { name: 'search-spaces', label: '搜索空间管理', to: '/search-spaces', icon: Search },
    { name: 'search-data', label: '搜索数据管理', to: '/search-data', icon: Database },
    { name: 'channels', label: '渠道管理', to: '/channels', icon: Store },
    { name: 'settings', label: '设置', to: '/settings', icon: Settings },
  ]
  console.log('Navigation items:', nav)
  return nav
})

// 辅助导航菜单 - 暂时移除
const auxiliaryNavigation = computed((): NavigationItem[] => [])

// 检查路由是否激活
const isActiveRoute = (path: string) => {
  if (path === '/') {
    return $route.path === '/'
  }
  return $route.path.startsWith(path)
}

// 获取导航项样式类
const getNavItemClasses = (item: any) => {
  const baseClasses = []

  if (props.isMobile) {
    // 移动端样式
    baseClasses.push('px-3 py-3 space-x-3 min-h-[44px]')
  } else if (props.collapsed) {
    // 桌面端折叠样式
    baseClasses.push('px-3 py-3 justify-center')
  } else {
    // 桌面端展开样式
    baseClasses.push('px-3 py-2 space-x-3')
  }

  return baseClasses
}
</script>

<style scoped>
/* 确保图标在折叠状态下居中 */
.router-link-active {
  @apply bg-accent text-accent-foreground;
}

/* 平滑过渡效果 */
.transition-all {
  transition-property: all;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .bg-accent {
    @apply ring-2 ring-primary;
  }
}

/* 减少动画 */
@media (prefers-reduced-motion: reduce) {
  .transition-all {
    transition: none !important;
  }
}

/* 触摸设备优化 */
@media (pointer: coarse) {
  a {
    @apply min-h-[44px];
  }
}
</style>