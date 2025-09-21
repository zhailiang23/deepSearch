<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-3xl font-bold tracking-tight">用户管理</h1>
      <p class="text-muted-foreground">
        管理系统用户账户和权限。
      </p>
    </div>

    <div class="rounded-lg border bg-card text-card-foreground shadow-sm p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-semibold">用户列表</h2>
        <Button>
          <UserPlus class="mr-2 h-4 w-4" />
          添加用户
        </Button>
      </div>

      <div class="text-center py-8">
        <Users class="mx-auto h-12 w-12 text-muted-foreground" />
        <h3 class="mt-2 text-sm font-semibold">暂无用户数据</h3>
        <p class="mt-1 text-sm text-muted-foreground">
          用户管理功能将在后续开发中完善
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Users, UserPlus } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
</script>