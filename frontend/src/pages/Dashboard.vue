<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-3xl font-bold tracking-tight">仪表板</h1>
      <p class="text-muted-foreground">
        欢迎回来！这里是您的管理仪表板。
      </p>
    </div>

    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <div class="rounded-lg border bg-card text-card-foreground shadow-sm p-6">
        <div class="flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">总用户数</h3>
          <Users class="h-4 w-4 text-muted-foreground" />
        </div>
        <div class="text-2xl font-bold">1,234</div>
        <p class="text-xs text-muted-foreground">
          +10.1% 较上月
        </p>
      </div>

      <div class="rounded-lg border bg-card text-card-foreground shadow-sm p-6">
        <div class="flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">活跃用户</h3>
          <UserCheck class="h-4 w-4 text-muted-foreground" />
        </div>
        <div class="text-2xl font-bold">892</div>
        <p class="text-xs text-muted-foreground">
          +5.2% 较上月
        </p>
      </div>

      <div class="rounded-lg border bg-card text-card-foreground shadow-sm p-6">
        <div class="flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">今日登录</h3>
          <Activity class="h-4 w-4 text-muted-foreground" />
        </div>
        <div class="text-2xl font-bold">156</div>
        <p class="text-xs text-muted-foreground">
          +8.1% 较昨日
        </p>
      </div>

      <div class="rounded-lg border bg-card text-card-foreground shadow-sm p-6">
        <div class="flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">系统状态</h3>
          <Shield class="h-4 w-4 text-muted-foreground" />
        </div>
        <div class="text-2xl font-bold text-green-600">正常</div>
        <p class="text-xs text-muted-foreground">
          所有服务运行正常
        </p>
      </div>
    </div>

    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
      <div class="col-span-4 rounded-lg border bg-card text-card-foreground shadow-sm p-6">
        <h3 class="text-lg font-semibold">最近活动</h3>
        <div class="mt-4 space-y-3">
          <div class="flex items-center">
            <div class="h-2 w-2 rounded-full bg-blue-500"></div>
            <p class="ml-3 text-sm">新用户 "张三" 注册</p>
            <p class="ml-auto text-xs text-muted-foreground">2分钟前</p>
          </div>
          <div class="flex items-center">
            <div class="h-2 w-2 rounded-full bg-green-500"></div>
            <p class="ml-3 text-sm">用户 "李四" 登录系统</p>
            <p class="ml-auto text-xs text-muted-foreground">5分钟前</p>
          </div>
          <div class="flex items-center">
            <div class="h-2 w-2 rounded-full bg-yellow-500"></div>
            <p class="ml-3 text-sm">系统配置已更新</p>
            <p class="ml-auto text-xs text-muted-foreground">10分钟前</p>
          </div>
        </div>
      </div>

      <div class="col-span-3 rounded-lg border bg-card text-card-foreground shadow-sm p-6">
        <h3 class="text-lg font-semibold">快速操作</h3>
        <div class="mt-4 space-y-2">
          <Button class="w-full justify-start" variant="ghost">
            <UserPlus class="mr-2 h-4 w-4" />
            添加新用户
          </Button>
          <Button class="w-full justify-start" variant="ghost">
            <Settings class="mr-2 h-4 w-4" />
            系统设置
          </Button>
          <Button class="w-full justify-start" variant="ghost">
            <FileText class="mr-2 h-4 w-4" />
            查看报告
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Users, UserCheck, Activity, Shield, UserPlus, Settings, FileText } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
</script>