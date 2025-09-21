<template>
  <div class="flex h-full flex-col">
    <!-- Logo区域 -->
    <div class="flex h-16 items-center px-6 border-b">
      <router-link to="/" class="flex items-center space-x-2">
        <div class="h-8 w-8 rounded bg-primary"></div>
        <span class="font-semibold text-lg">管理系统</span>
      </router-link>
    </div>

    <!-- 导航菜单 -->
    <nav class="flex-1 overflow-y-auto p-4">
      <ul class="space-y-2">
        <li v-for="item in navigation" :key="item.name">
          <router-link
            :to="item.to"
            :class="[
              'flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-colors',
              'hover:bg-accent hover:text-accent-foreground',
              $route.path === item.to ? 'bg-accent text-accent-foreground' : 'text-muted-foreground'
            ]"
          >
            <component :is="item.icon" class="h-5 w-5" />
            <span>{{ $t(item.label) }}</span>
          </router-link>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { LayoutDashboard, Users, Settings } from 'lucide-vue-next'

const $route = useRoute()

const navigation = computed(() => [
  { name: 'dashboard', label: 'nav.dashboard', to: '/', icon: LayoutDashboard },
  { name: 'users', label: 'nav.users', to: '/users', icon: Users },
  { name: 'settings', label: 'nav.settings', to: '/settings', icon: Settings },
])
</script>