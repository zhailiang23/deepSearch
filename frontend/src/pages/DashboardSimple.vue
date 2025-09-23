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
            <SidebarGroupLabel>平台</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <Collapsible v-model:open="platformOpen">
                  <CollapsibleTrigger as-child>
                    <SidebarMenuButton class="w-full justify-between">
                      <div class="flex items-center gap-2">
                        <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
                        </svg>
                        <span>平台</span>
                      </div>
                      <ChevronRight
                        class="size-4 transition-transform duration-200"
                        :class="{ 'rotate-90': platformOpen }"
                      />
                    </SidebarMenuButton>
                  </CollapsibleTrigger>
                  <CollapsibleContent>
                    <SidebarMenu class="ml-4">
                      <SidebarMenuItem>
                        <SidebarMenuButton as-child>
                          <a href="#" class="flex items-center gap-2">
                            <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h8m-9 3h10a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"></path>
                            </svg>
                            <span>试验场</span>
                          </a>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                    </SidebarMenu>
                  </CollapsibleContent>
                </Collapsible>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>

          <SidebarGroup>
            <SidebarGroupLabel>模型</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <a href="#" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
                    </svg>
                    <span>模型</span>
                  </a>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>

          <SidebarGroup>
            <SidebarGroupLabel>文档</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <Collapsible v-model:open="documentationOpen">
                  <CollapsibleTrigger as-child>
                    <SidebarMenuButton class="w-full justify-between">
                      <div class="flex items-center gap-2">
                        <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
                        </svg>
                        <span>文档</span>
                      </div>
                      <ChevronRight
                        class="size-4 transition-transform duration-200"
                        :class="{ 'rotate-90': documentationOpen }"
                      />
                    </SidebarMenuButton>
                  </CollapsibleTrigger>
                  <CollapsibleContent>
                    <SidebarMenu class="ml-4">
                      <SidebarMenuItem>
                        <SidebarMenuButton as-child>
                          <a href="#" class="flex items-center gap-2">
                            <svg class="size-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                            <span>入门指南</span>
                          </a>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                      <SidebarMenuItem>
                        <SidebarMenuButton as-child>
                          <a href="#" class="flex items-center gap-2">
                            <svg class="size-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                            </svg>
                            <span>API 参考</span>
                          </a>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                      <SidebarMenuItem>
                        <SidebarMenuButton as-child>
                          <a href="#" class="flex items-center gap-2">
                            <svg class="size-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
                            </svg>
                            <span>示例</span>
                          </a>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                      <SidebarMenuItem>
                        <SidebarMenuButton as-child>
                          <a href="#" class="flex items-center gap-2">
                            <svg class="size-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
                            </svg>
                            <span>教程</span>
                          </a>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                    </SidebarMenu>
                  </CollapsibleContent>
                </Collapsible>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>

          <SidebarGroup>
            <SidebarGroupLabel>设置</SidebarGroupLabel>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton as-child>
                  <a href="#" class="flex items-center gap-2">
                    <svg class="size-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    </svg>
                    <span>设置</span>
                  </a>
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
                  <span class="text-foreground font-medium">概览</span>
                </li>
              </ol>
            </nav>
          </div>
        </header>

        <div class="flex flex-1 flex-col gap-4 p-4 pt-0">
          <div class="grid auto-rows-min gap-4 md:grid-cols-3">
            <div class="aspect-video rounded-xl bg-muted/50" />
            <div class="aspect-video rounded-xl bg-muted/50" />
            <div class="aspect-video rounded-xl bg-muted/50" />
          </div>

          <!-- Statistics Cards -->
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <Card>
              <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle class="text-sm font-medium">总收入</CardTitle>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  class="h-4 w-4 text-muted-foreground"
                >
                  <path d="M12 2v20m0 0l7-7m-7 7l-7-7" />
                </svg>
              </CardHeader>
              <CardContent>
                <div class="text-2xl font-bold">¥45,231.89</div>
                <p class="text-xs text-muted-foreground">
                  +20.1% 较上月
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle class="text-sm font-medium">新客户</CardTitle>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  class="h-4 w-4 text-muted-foreground"
                >
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
              </CardHeader>
              <CardContent>
                <div class="text-2xl font-bold">+2350</div>
                <p class="text-xs text-muted-foreground">
                  +180.1% 较上月
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle class="text-sm font-medium">活跃账户</CardTitle>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  class="h-4 w-4 text-muted-foreground"
                >
                  <rect width="20" height="14" x="2" y="5" rx="2" />
                  <path d="M2 10h20" />
                </svg>
              </CardHeader>
              <CardContent>
                <div class="text-2xl font-bold">+12,234</div>
                <p class="text-xs text-muted-foreground">
                  +19% 较上月
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader class="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle class="text-sm font-medium">增长率</CardTitle>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  class="h-4 w-4 text-muted-foreground"
                >
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
                </svg>
              </CardHeader>
              <CardContent>
                <div class="text-2xl font-bold">+573</div>
                <p class="text-xs text-muted-foreground">
                  +201 较上月
                </p>
              </CardContent>
            </Card>
          </div>

          <!-- Chart and Table -->
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
            <Card class="col-span-4">
              <CardHeader>
                <CardTitle>概览</CardTitle>
              </CardHeader>
              <CardContent class="pl-2">
                <div class="h-[200px] rounded-md border border-dashed flex items-center justify-center">
                  <div class="text-center">
                    <div class="text-sm text-muted-foreground">图表区域</div>
                    <div class="text-xs text-muted-foreground mt-1">在这里显示数据可视化</div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card class="col-span-3">
              <CardHeader>
                <CardTitle>最近项目</CardTitle>
                <CardDescription>
                  您最近查看的项目概览
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div class="space-y-8">
                  <div class="flex items-center">
                    <div class="ml-4 space-y-1">
                      <p class="text-sm font-medium leading-none">项目 Alpha</p>
                      <p class="text-sm text-muted-foreground">
                        进行中
                      </p>
                    </div>
                    <div class="ml-auto font-medium">+¥1,999.00</div>
                  </div>
                  <div class="flex items-center">
                    <div class="ml-4 space-y-1">
                      <p class="text-sm font-medium leading-none">项目 Beta</p>
                      <p class="text-sm text-muted-foreground">
                        已完成
                      </p>
                    </div>
                    <div class="ml-auto font-medium">+¥39.00</div>
                  </div>
                  <div class="flex items-center">
                    <div class="ml-4 space-y-1">
                      <p class="text-sm font-medium leading-none">项目 Gamma</p>
                      <p class="text-sm text-muted-foreground">
                        计划中
                      </p>
                    </div>
                    <div class="ml-auto font-medium">+¥299.00</div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <!-- Data Table -->
          <Card>
            <CardHeader>
              <CardTitle>项目详情</CardTitle>
              <CardDescription>所有项目的详细信息</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>项目名称</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>创建时间</TableHead>
                    <TableHead class="text-right">预算</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow>
                    <TableCell class="font-medium">项目 Alpha</TableCell>
                    <TableCell>进行中</TableCell>
                    <TableCell>2024-01-15</TableCell>
                    <TableCell class="text-right">¥25,000</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell class="font-medium">项目 Beta</TableCell>
                    <TableCell>已完成</TableCell>
                    <TableCell>2024-01-10</TableCell>
                    <TableCell class="text-right">¥15,000</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell class="font-medium">项目 Gamma</TableCell>
                    <TableCell>计划中</TableCell>
                    <TableCell>2024-01-20</TableCell>
                    <TableCell class="text-right">¥30,000</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </SidebarInset>
    </SidebarProvider>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
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
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Separator } from '@/components/ui/separator'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'

// Store
const userStore = useAuthStore()

// Collapsible states
const platformOpen = ref(false)
const documentationOpen = ref(false)

// Computed properties
const userInitials = computed(() => {
  const user = userStore.user
  if (user?.username) {
    return user.username.charAt(0).toUpperCase()
  }
  return 'U'
})
</script>