<template>
  <ResponsiveLayout
    :show-mobile-sidebar="true"
    :show-fab="false"
    class="dashboard-layout"
    @sidebar-toggle="handleSidebarToggle"
    @breakpoint-change="handleBreakpointChange"
    ref="responsiveLayoutRef"
  >
    <!-- 跳转到主要内容的无障碍链接 -->
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 z-50 bg-primary text-primary-foreground px-4 py-2 rounded-md"
    >
      跳转到主内容
    </a>

    <!-- 桌面端侧边栏 -->
    <aside
      v-if="!breakpoints.isMobile"
      :class="[
        'fixed inset-y-0 left-0 z-50 bg-card border-r transition-all duration-300',
        sidebarCollapsed ? 'w-16' : 'w-64'
      ]"
      :aria-hidden="breakpoints.isMobile"
    >
      <SidebarNav :collapsed="sidebarCollapsed" />
    </aside>

    <!-- 主内容区 -->
    <div
      :class="[
        'flex flex-col min-h-screen',
        // 桌面端左边距
        !breakpoints.isMobile && (sidebarCollapsed ? 'lg:ml-16' : 'lg:ml-64'),
        // 平滑过渡
        'transition-all duration-300'
      ]"
    >
      <!-- 顶部导航 -->
      <header
        :class="[
          'sticky top-0 z-40 border-b',
          'bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60',
          // 移动端适配
          breakpoints.isMobile ? 'h-14' : 'h-16'
        ]"
      >
        <TopNav
          :is-mobile="breakpoints.isMobile.value"
          :sidebar-collapsed="sidebarCollapsed"
          @toggle-sidebar="toggleSidebar"
          @toggle-mobile-sidebar="toggleMobileSidebar"
        />
      </header>

      <!-- 页面内容 -->
      <main
        id="main-content"
        :class="[
          'flex-1',
          // 响应式padding
          breakpoints.isMobile ? 'p-4' : 'p-6'
        ]"
        role="main"
        aria-label="主要内容"
      >
        <router-view />
      </main>
    </div>

    <!-- 移动端侧边栏内容 -->
    <template #sidebar-content>
      <SidebarNav :is-mobile="true" />
    </template>

    <!-- 移动端侧边栏头部 -->
    <template #sidebar-header>
      <div class="flex items-center space-x-2">
        <div class="w-8 h-8 bg-primary rounded-md flex items-center justify-center">
          <span class="text-primary-foreground text-sm font-bold">M</span>
        </div>
        <span class="font-semibold">管理系统</span>
      </div>
    </template>
  </ResponsiveLayout>
</template>

<script setup lang="ts">
import { ref, provide, onMounted } from 'vue'

import ResponsiveLayout from '@/components/layout/ResponsiveLayout.vue'
import SidebarNav from '@/components/layout/SidebarNav.vue'
import TopNav from '@/components/layout/TopNav.vue'
import { useBreakpoints } from '@/composables/useBreakpoints'

const breakpoints = useBreakpoints()

// 响应式布局引用
const responsiveLayoutRef = ref()

// 桌面端侧边栏状态
const sidebarCollapsed = ref(false)

// 向子组件提供断点信息
provide('breakpoints', breakpoints)

// 桌面端侧边栏切换
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value

  // 保存用户偏好
  localStorage.setItem('sidebar-collapsed', String(sidebarCollapsed.value))
}

// 移动端侧边栏切换
const toggleMobileSidebar = () => {
  if (responsiveLayoutRef.value) {
    responsiveLayoutRef.value.toggleSidebar()
  }
}

// 侧边栏状态变化处理
const handleSidebarToggle = (visible: boolean) => {
  console.log('Mobile sidebar toggled:', visible)
}

// 断点变化处理
const handleBreakpointChange = (breakpoint: string) => {
  console.log('Breakpoint changed to:', breakpoint)

  // 移动端到桌面端切换时，自动关闭移动端侧边栏
  if (!breakpoints.isMobile.value && responsiveLayoutRef.value?.isSidebarVisible) {
    responsiveLayoutRef.value.closeSidebar()
  }
}

// 初始化用户偏好
const initializePreferences = () => {
  const savedCollapsed = localStorage.getItem('sidebar-collapsed')
  if (savedCollapsed !== null) {
    sidebarCollapsed.value = savedCollapsed === 'true'
  }
}

// 生命周期
onMounted(() => {
  initializePreferences()
})
</script>

<style scoped>
.dashboard-layout {
  /* 确保布局占满全屏 */
  min-height: 100vh;
  min-height: 100dvh; /* 动态视口高度 */
}

/* 侧边栏动画优化 */
@media (prefers-reduced-motion: reduce) {
  aside,
  .dashboard-layout > div {
    transition: none !important;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  aside {
    @apply border-2;
  }

  header {
    @apply border-b-2;
  }
}
</style>