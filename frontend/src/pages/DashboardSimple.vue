<template>
  <div class="min-h-screen bg-background">
    <SidebarProvider>
      <Sidebar variant="inset">
        <SidebarHeader>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton size="lg" as-child>
                <div>
                  <div class="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                    <span class="size-4 font-bold">DS</span>
                  </div>
                  <div class="grid flex-1 text-left text-sm leading-tight">
                    <span class="truncate font-semibold">深度搜索</span>
                    <span class="truncate text-xs">管理系统</span>
                  </div>
                </div>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarHeader>

        <SidebarContent>

          <SidebarGroup>
            <SidebarGroupLabel>管理</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/search-spaces" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                    <span>搜索空间管理</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/search-data" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"></path>
                    </svg>
                    <span>搜索数据管理</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/search-logs" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    <span>搜索日志管理</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/hot-topics" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.879 16.121A3 3 0 1012.015 11L11 14l4-2-3 3.053z"></path>
                    </svg>
                    <span>热门话题管理</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/channels" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path>
                    </svg>
                    <span>渠道管理</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/roles" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"></path>
                    </svg>
                    <span>角色管理</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>

          <SidebarGroup>
            <SidebarGroupLabel>分析</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/hot-word-statistics" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
                    </svg>
                    <span>热词统计分析</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/mismatch-keyword" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <span>搜索效果分析</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/activity-recognition" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                    </svg>
                    <span>智能活动识别</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>

          <SidebarGroup>
            <SidebarGroupLabel>演示</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/mobile-search-demo" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                    </svg>
                    <span>移动搜索演示</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>

          <SidebarGroup>
            <SidebarGroupLabel>设置</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <router-link to="/settings" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    </svg>
                    <span>设置</span>
                  </router-link>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>
        </SidebarContent>

        <SidebarFooter>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton as-child>
                <div class="flex items-center gap-2 p-2">
                  <div class="size-8 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-sm font-medium">
                    {{ userInitials }}
                  </div>
                  <div class="grid flex-1 text-left text-sm leading-tight">
                    <span class="truncate font-semibold">{{ userStore.user?.username || 'User' }}</span>
                    <span class="truncate text-xs">{{ userStore.user?.email || 'user@example.com' }}</span>
                  </div>
                </div>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
      </Sidebar>

      <SidebarInset>
        <header class="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-[[data-collapsible=icon]]/sidebar-wrapper:h-12">
          <div class="flex items-center gap-2 px-4">
            <SidebarTrigger class="-ml-1" />
            <Separator orientation="vertical" class="mr-2 h-4" />
            <nav class="flex" aria-label="Breadcrumb">
              <ol class="flex items-center space-x-1 md:space-x-3">
                <li class="flex items-center">
                  <a class="text-muted-foreground hover:text-foreground transition-colors">管理系统</a>
                </li>
                <li class="text-muted-foreground">/</li>
                <li class="flex items-center">
                  <span class="text-foreground font-medium">{{ currentPageTitle }}</span>
                </li>
              </ol>
            </nav>
          </div>
        </header>

        <div class="flex flex-1 flex-col gap-4 p-4 pt-0 min-w-0 overflow-hidden">
          <router-view />
        </div>
      </SidebarInset>
    </SidebarProvider>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ChevronRight } from 'lucide-vue-next'

// shadcn-vue components
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import { Separator } from '@/components/ui/separator'

// 响应式状态 - 已移除不需要的状态

// 用户状态
const userStore = useAuthStore()
const route = useRoute()

// 计算属性
const userInitials = computed(() => {
  const name = userStore.user?.username || 'User'
  return name.charAt(0).toUpperCase()
})

const currentPageTitle = computed(() => {
  return route.meta?.title || '概览'
})
</script>