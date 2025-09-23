<template>
  <div class="grid min-h-svh lg:grid-cols-2">
    <!-- 左侧登录区域 -->
    <div class="flex flex-col gap-4 p-6 md:p-10">
      <!-- 品牌Logo -->
      <div class="flex justify-center gap-2 md:justify-start">
        <a href="#" class="flex items-center gap-2 font-medium">
          <div class="bg-primary text-primary-foreground flex size-6 items-center justify-center rounded-md">
            <span class="text-sm font-bold">DS</span>
          </div>
          深度搜索
        </a>
      </div>

      <!-- 登录表单容器 -->
      <div class="flex flex-1 items-center justify-center">
        <div class="w-full max-w-xs">
          <LoginForm @submit="handleSubmit" :loading="loading" :error="error" />
        </div>
      </div>
    </div>

    <!-- 右侧装饰区域 -->
    <div class="bg-muted relative hidden lg:block">
      <div class="absolute inset-0 bg-gradient-to-br from-primary/20 via-background/10 to-secondary/20" />
      <div class="absolute inset-0 flex items-center justify-center">
        <div class="text-center space-y-6 p-8">
          <div class="w-32 h-32 mx-auto rounded-full bg-primary/10 flex items-center justify-center">
            <Shield class="w-16 h-16 text-primary" />
          </div>
          <div class="space-y-2">
            <h2 class="text-2xl font-bold text-foreground">欢迎使用深度搜索</h2>
            <p class="text-muted-foreground">专业的管理系统平台</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Shield } from 'lucide-vue-next'
import axios from 'axios'

import LoginForm from '@/components/LoginForm.vue'

// 组合式API
const router = useRouter()

// 响应式数据
const loading = ref(false)
const error = ref('')

// 登录处理函数
const handleSubmit = async (formData: { username: string; password: string }) => {
  loading.value = true
  error.value = ''

  try {
    console.log('开始登录:', formData.username)

    // 调用登录API
    const response = await axios.post('http://localhost:8080/api/auth/login', {
      username: formData.username.trim(),
      password: formData.password,
      rememberMe: false
    }, {
      headers: {
        'Content-Type': 'application/json'
      },
      timeout: 10000
    })

    console.log('登录响应:', response.data)

    if (response.data && response.data.token) {
      // 存储token和用户信息
      localStorage.setItem('token', response.data.token)
      localStorage.setItem('user', JSON.stringify(response.data.user))
      localStorage.setItem('loginSuccess', 'true')

      console.log('登录成功，准备跳转到Dashboard')

      // 跳转到Dashboard
      window.location.href = '/'
    } else {
      error.value = '登录响应格式错误'
    }
  } catch (err: any) {
    console.error('登录错误:', err)

    if (err.response) {
      error.value = err.response.data?.message || `登录失败: ${err.response.status}`
    } else if (err.request) {
      error.value = '无法连接到服务器，请检查网络连接'
    } else {
      error.value = '请求配置错误: ' + err.message
    }
  } finally {
    loading.value = false
  }
}
</script>