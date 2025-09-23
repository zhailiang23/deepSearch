<template>
  <div
    :class="[
      'flex items-center justify-between',
      // 响应式高度和padding
      isMobile ? 'h-14 px-4' : 'h-16 px-6'
    ]"
  >
    <!-- 左侧：菜单按钮和面包屑 -->
    <div class="flex items-center space-x-4">
      <!-- 桌面端侧边栏切换按钮 -->
      <Button
        v-if="!isMobile"
        variant="ghost"
        size="sm"
        @click="$emit('toggle-sidebar')"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        class="hidden lg:flex"
      >
        <PanelLeftClose v-if="!sidebarCollapsed" class="h-4 w-4" />
        <PanelLeft v-else class="h-4 w-4" />
      </Button>

      <!-- 移动端菜单按钮 -->
      <Button
        v-if="isMobile"
        variant="ghost"
        size="sm"
        @click="$emit('toggle-mobile-sidebar')"
        :aria-label="'切换菜单'"
        :class="[
          // 触摸设备优化
          'min-h-[44px] min-w-[44px]'
        ]"
      >
        <Menu class="h-5 w-5" />
      </Button>

      <!-- 面包屑导航 -->
      <div v-if="!isMobile" class="hidden md:block">
        <Breadcrumb />
      </div>
    </div>

    <!-- 右侧：工具栏 -->
    <div
      :class="[
        'flex items-center',
        // 响应式间距
        isMobile ? 'space-x-2' : 'space-x-4'
      ]"
    >
      <!-- 搜索按钮 (移动端) -->
      <Button
        v-if="isMobile"
        variant="ghost"
        size="sm"
        :aria-label="'搜索'"
        class="min-h-[44px] min-w-[44px]"
      >
        <Search class="h-4 w-4" />
      </Button>

      <!-- 主题切换 -->
      <ThemeToggle />


      <!-- 通知按钮 (桌面端) -->
      <Button
        v-if="!isMobile"
        variant="ghost"
        size="sm"
        :aria-label="'通知'"
        class="relative"
      >
        <Bell class="h-4 w-4" />
        <!-- 通知徽章 -->
        <span
          class="absolute -top-1 -right-1 h-3 w-3 bg-destructive rounded-full"
          aria-hidden="true"
        ></span>
      </Button>

      <!-- 用户菜单 -->
      <UserMenu />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Menu, Search, Bell, PanelLeft, PanelLeftClose } from 'lucide-vue-next'

import { Button } from '@/components/ui/button'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import UserMenu from '@/components/common/UserMenu.vue'

interface Props {
  isMobile?: boolean
  sidebarCollapsed?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isMobile: false,
  sidebarCollapsed: false
})


defineEmits<{
  'toggle-sidebar': []
  'toggle-mobile-sidebar': []
}>()
</script>

<style scoped>
/* 确保通知徽章在正确位置 */
.relative .absolute {
  transform: translate(50%, -50%);
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .bg-destructive {
    @apply ring-2 ring-background;
  }
}
</style>