<template>
  <div class="min-h-screen flex items-center justify-center bg-background">
    <div class="w-full max-w-md space-y-6">
      <div class="text-center">
        <h1 class="text-3xl font-bold">管理系统</h1>
        <p class="text-muted-foreground mt-2">请登录您的账户</p>
      </div>

      <div class="rounded-lg border bg-card text-card-foreground shadow-sm p-6">
        <div class="space-y-4">
          <div>
            <label class="text-sm font-medium">用户名</label>
            <input
              type="text"
              class="w-full mt-1 px-3 py-2 border border-input bg-background rounded-md"
              placeholder="请输入用户名"
            />
          </div>
          <div>
            <label class="text-sm font-medium">密码</label>
            <input
              type="password"
              class="w-full mt-1 px-3 py-2 border border-input bg-background rounded-md"
              placeholder="请输入密码"
            />
          </div>
          <Button class="w-full">
            登录
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>