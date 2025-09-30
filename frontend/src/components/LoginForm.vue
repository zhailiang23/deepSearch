<template>
  <form @submit.prevent="handleSubmit" class="flex flex-col gap-6">
    <!-- 标题区域 -->
    <div class="flex flex-col items-center gap-2 text-center">
      <h1 class="text-2xl font-bold">登录您的账户</h1>
      <p class="text-muted-foreground text-sm text-balance">
        请输入您的用户名和密码进行登录
      </p>
    </div>

    <!-- 表单字段 -->
    <div class="grid gap-6">
      <!-- 用户名输入 -->
      <div class="grid gap-3">
        <Label for="username">用户名</Label>
        <Input
          id="username"
          v-model="form.username"
          type="text"
          placeholder="请输入用户名"
          required
          :disabled="loading"
          autocomplete="username"
          tabindex="1"
        />
      </div>

      <!-- 密码输入 -->
      <div class="grid gap-3">
        <div class="flex items-center">
          <Label for="password">密码</Label>
          <a
            href="#"
            class="ml-auto text-sm underline-offset-4 hover:underline"
            tabindex="5"
            @click.prevent="$emit('forgot-password')"
          >
            忘记密码？
          </a>
        </div>
        <Input
          id="password"
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          required
          :disabled="loading"
          autocomplete="current-password"
          tabindex="2"
        />
      </div>

      <!-- 错误提示 -->
      <div
        v-if="error"
        class="flex items-center space-x-2 p-3 bg-destructive/10 border border-destructive/20 rounded-md"
        role="alert"
        aria-live="polite"
      >
        <AlertCircle class="h-4 w-4 text-destructive flex-shrink-0" />
        <span class="text-sm text-destructive">{{ error }}</span>
      </div>

      <!-- 登录按钮 -->
      <Button type="submit" class="w-full" :disabled="loading || !isFormValid" tabindex="3">
        <Loader2 v-if="loading" class="mr-2 h-4 w-4 animate-spin" />
        {{ loading ? '登录中...' : '登录' }}
      </Button>
    </div>

    <!-- 注册链接 -->
    <div class="text-center text-sm">
      还没有账户？
      <a
        href="#"
        class="underline underline-offset-4"
        @click.prevent="$emit('sign-up')"
        tabindex="6"
      >
        立即注册
      </a>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { AlertCircle, Loader2 } from 'lucide-vue-next'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

// Props
interface Props {
  loading?: boolean
  error?: string
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  error: ''
})

// Emits
interface Emits {
  (e: 'submit', formData: { username: string; password: string }): void
  (e: 'forgot-password'): void
  (e: 'sign-up'): void
}

const emit = defineEmits<Emits>()

// 响应式数据
const form = ref({
  username: '',
  password: ''
})

// 计算属性
const isFormValid = computed(() => {
  return form.value.username.trim() !== '' && form.value.password.trim() !== ''
})

// 方法
const handleSubmit = () => {
  if (!isFormValid.value || props.loading) return

  emit('submit', {
    username: form.value.username.trim(),
    password: form.value.password
  })
}

// 生命周期
onMounted(() => {
  // 聚焦到用户名输入框
  const usernameInput = document.getElementById('username')
  if (usernameInput) {
    usernameInput.focus()
  }
})
</script>