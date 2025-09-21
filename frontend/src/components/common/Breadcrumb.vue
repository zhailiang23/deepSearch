<template>
  <nav class="flex" aria-label="Breadcrumb">
    <ol class="inline-flex items-center space-x-1 md:space-x-3">
      <li class="inline-flex items-center">
        <router-link
          to="/"
          class="inline-flex items-center text-sm font-medium text-muted-foreground hover:text-foreground"
        >
          <Home class="w-4 h-4 mr-2" />
          首页
        </router-link>
      </li>
      <li v-for="(item, index) in breadcrumbs" :key="index">
        <div class="flex items-center">
          <ChevronRight class="w-4 h-4 text-muted-foreground" />
          <router-link
            v-if="item.to && index < breadcrumbs.length - 1"
            :to="item.to"
            class="ml-1 text-sm font-medium text-muted-foreground hover:text-foreground md:ml-2"
          >
            {{ item.label }}
          </router-link>
          <span
            v-else
            class="ml-1 text-sm font-medium text-foreground md:ml-2"
          >
            {{ item.label }}
          </span>
        </div>
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Home, ChevronRight } from 'lucide-vue-next'

const route = useRoute()

const breadcrumbs = computed(() => {
  const routeMap: Record<string, string> = {
    '/': '仪表板',
    '/users': '用户管理',
    '/settings': '系统设置'
  }

  const path = route.path
  if (path === '/') return []

  return [
    {
      label: routeMap[path] || '未知页面',
      to: path
    }
  ]
})
</script>