<template>
  <div class="min-h-screen bg-background">
    <!-- 侧边栏 -->
    <aside
      :class="[
        'fixed inset-y-0 left-0 z-50 w-64 bg-card border-r transition-transform duration-300',
        sidebarCollapsed ? '-translate-x-full lg:translate-x-0 lg:w-20' : 'translate-x-0'
      ]"
    >
      <SidebarNav />
    </aside>

    <!-- 主内容区 -->
    <div :class="['lg:ml-64', sidebarCollapsed && 'lg:ml-20']">
      <!-- 顶部导航 -->
      <header class="sticky top-0 z-40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b">
        <TopNav @toggle-sidebar="toggleSidebar" />
      </header>

      <!-- 页面内容 -->
      <main class="p-6">
        <router-view />
      </main>
    </div>

    <!-- 移动端遮罩 -->
    <div
      v-if="!sidebarCollapsed"
      class="fixed inset-0 z-40 bg-black/50 lg:hidden"
      @click="toggleSidebar"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import SidebarNav from '@/components/layout/SidebarNav.vue'
import TopNav from '@/components/layout/TopNav.vue'

const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>