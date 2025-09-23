<template>
  <ResponsiveLayout
    :container="false"
    :padding="false"
    class="login-page"
  >
    <!-- 跳转到主要内容的无障碍链接 -->
    <a
      href="#login-form"
      class="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 z-50 bg-primary text-primary-foreground px-4 py-2 rounded-md"
    >
      跳转到主要内容
    </a>


    <!-- 主要登录区域 -->
    <div class="min-h-screen flex items-center justify-center bg-background relative">
      <!-- 背景装饰 -->
      <div
        class="absolute inset-0 bg-gradient-to-br from-primary/5 via-background to-secondary/5"
        aria-hidden="true"
      />

      <!-- 登录容器 -->
      <div
        :class="[
          'w-full max-w-md space-y-6 relative z-10',
          // 响应式padding
          'px-4 sm:px-6 lg:px-8',
          // 移动端适配
          breakpoints.isMobile ? 'mx-4' : 'mx-auto'
        ]"
      >
        <!-- 标题区域 -->
        <div class="text-center space-y-2">
          <div
            :class="[
              'inline-flex items-center justify-center',
              'w-16 h-16 rounded-full bg-primary/10 mb-4',
              // 移动端尺寸调整
              breakpoints.isMobile && 'w-12 h-12'
            ]"
          >
            <Shield
              :class="[
                'text-primary',
                breakpoints.isMobile ? 'h-6 w-6' : 'h-8 w-8'
              ]"
            />
          </div>

          <h1
            :class="[
              'font-bold text-foreground',
              // 响应式字体大小
              breakpoints.isMobile ? 'text-2xl' : 'text-3xl lg:text-4xl'
            ]"
          >
            欢迎使用管理系统
          </h1>

          <p
            :class="[
              'text-muted-foreground',
              breakpoints.isMobile ? 'text-sm' : 'text-base'
            ]"
          >
            登录您的账户
          </p>
        </div>

        <!-- 登录表单 -->
        <div
          id="login-form"
          :class="[
            'rounded-lg border bg-card text-card-foreground shadow-sm',
            // 响应式padding
            breakpoints.isMobile ? 'p-4' : 'p-6'
          ]"
          role="main"
          aria-label="登录"
        >
          <form @submit.prevent="handleLogin" class="space-y-4">
            <!-- 用户名输入 -->
            <div class="space-y-2">
              <label
                for="username"
                class="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
              >
                用户名
              </label>
              <input
                id="username"
                v-model="loginForm.username"
                type="text"
                autocomplete="username"
                required
                placeholder="请输入用户名"
                :class="[
                  'w-full px-3 py-2 border border-input bg-background rounded-md',
                  'text-sm ring-offset-background',
                  'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
                  'disabled:cursor-not-allowed disabled:opacity-50',
                  // 触摸设备优化
                  breakpoints.isTouchDevice && 'min-h-[44px]',
                  // 错误状态
                  loginError && 'border-destructive focus:ring-destructive'
                ]"
                :disabled="isLoading"
              />
            </div>

            <!-- 密码输入 -->
            <div class="space-y-2">
              <label
                for="password"
                class="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
              >
                密码
              </label>
              <div class="relative">
                <input
                  id="password"
                  v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  required
                  placeholder="请输入密码"
                  :class="[
                    'w-full px-3 py-2 pr-10 border border-input bg-background rounded-md',
                    'text-sm ring-offset-background',
                    'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
                    'disabled:cursor-not-allowed disabled:opacity-50',
                    // 触摸设备优化
                    breakpoints.isTouchDevice && 'min-h-[44px]',
                    // 错误状态
                    loginError && 'border-destructive focus:ring-destructive'
                  ]"
                  :disabled="isLoading"
                />
                <button
                  type="button"
                  @click="togglePasswordVisibility"
                  :class="[
                    'absolute right-3 top-1/2 -translate-y-1/2',
                    'text-muted-foreground hover:text-foreground',
                    'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-sm',
                    // 触摸设备优化
                    breakpoints.isTouchDevice && 'p-1'
                  ]"
                  :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                  :disabled="isLoading"
                >
                  <Eye v-if="showPassword" class="h-4 w-4" />
                  <EyeOff v-else class="h-4 w-4" />
                </button>
              </div>
            </div>

            <!-- 记住我选项 -->
            <div class="flex items-center space-x-2">
              <input
                id="remember-me"
                v-model="loginForm.rememberMe"
                type="checkbox"
                :class="[
                  'h-4 w-4 rounded border border-input',
                  'text-primary focus:ring-2 focus:ring-ring focus:ring-offset-2',
                  // 触摸设备优化
                  breakpoints.isTouchDevice && 'h-6 w-6'
                ]"
                :disabled="isLoading"
              />
              <label
                for="remember-me"
                :class="[
                  'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
                  // 触摸设备优化
                  breakpoints.isTouchDevice && 'text-base'
                ]"
              >
                记住我
              </label>
            </div>

            <!-- 错误提示 -->
            <div
              v-if="loginError"
              class="flex items-center space-x-2 p-3 bg-destructive/10 border border-destructive/20 rounded-md"
              role="alert"
              aria-live="polite"
            >
              <AlertCircle class="h-4 w-4 text-destructive flex-shrink-0" />
              <span class="text-sm text-destructive">{{ loginError }}</span>
            </div>

            <!-- 登录按钮 -->
            <Button
              type="submit"
              :class="[
                'w-full',
                // 触摸设备优化
                breakpoints.isTouchDevice && 'min-h-[44px]'
              ]"
              :disabled="isLoading || !isFormValid"
            >
              <Loader2 v-if="isLoading" class="mr-2 h-4 w-4 animate-spin" />
              {{ isLoading ? '登录中...' : '登录' }}
            </Button>

            <!-- 忘记密码链接 -->
            <div class="text-center">
              <button
                type="button"
                class="text-sm text-primary hover:text-primary/80 underline-offset-4 hover:underline focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-sm"
                @click="handleForgotPassword"
                :disabled="isLoading"
              >
                忘记密码？
              </button>
            </div>
          </form>
        </div>

        <!-- 注册链接 -->
        <div class="text-center">
          <p class="text-sm text-muted-foreground">
            还没有账户？
            <button
              type="button"
              class="text-primary hover:text-primary/80 underline-offset-4 hover:underline focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded-sm"
              @click="handleRegister"
              :disabled="isLoading"
            >
              立即注册
            </button>
          </p>
        </div>

        <!-- 调试信息 (开发环境) -->
        <div
          v-if="isDev && breakpoints"
          class="mt-8 p-4 bg-muted rounded-lg text-xs text-muted-foreground"
        >
          <p><strong>当前断点:</strong> {{ breakpoints.current }}</p>
          <p><strong>屏幕尺寸:</strong> {{ breakpoints.width }}x{{ breakpoints.height }}</p>
          <p><strong>触摸设备:</strong> {{ breakpoints.isTouchDevice ? '是' : '否' }}</p>
        </div>
      </div>
    </div>
  </ResponsiveLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Shield, Eye, EyeOff, AlertCircle, Loader2 } from 'lucide-vue-next'

import { Button } from '@/components/ui/button'
import ResponsiveLayout from '@/components/layout/ResponsiveLayout.vue'
import { useBreakpoints } from '@/composables/useBreakpoints'
import { useAuthStore } from '@/stores/auth'

// 组合式API
const router = useRouter()
const authStore = useAuthStore()
const breakpoints = useBreakpoints()

// 响应式数据
const isLoading = ref(false)
const showPassword = ref(false)
const loginError = ref('')

const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false
})

// 计算属性
const isFormValid = computed(() => {
  return loginForm.value.username.trim() !== '' && loginForm.value.password.trim() !== ''
})

const isDev = computed(() => {
  return import.meta.env.DEV
})

// 方法
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const handleLogin = async () => {
  if (!isFormValid.value || isLoading.value) return

  isLoading.value = true
  loginError.value = ''

  try {
    await authStore.login({
      username: loginForm.value.username.trim(),
      password: loginForm.value.password,
      rememberMe: loginForm.value.rememberMe
    })

    // 登录成功，跳转到仪表板
    await router.push('/')
  } catch (error: any) {
    loginError.value = error.message || '登录失败'
  } finally {
    isLoading.value = false
  }
}

const handleForgotPassword = () => {
  // TODO: 实现忘记密码功能
  console.log('Forgot password clicked')
}

const handleRegister = () => {
  // TODO: 实现注册功能或跳转到注册页面
  console.log('Register clicked')
}

// 键盘导航优化
const handleKeyDown = (event: KeyboardEvent) => {
  // Enter键提交表单
  if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey && !event.metaKey) {
    const activeElement = document.activeElement as HTMLElement
    if (activeElement?.tagName === 'INPUT') {
      handleLogin()
    }
  }
}

// 生命周期
onMounted(() => {
  // 清除之前的错误
  loginError.value = ''

  // 如果已经登录，重定向到仪表板
  if (authStore.isAuthenticated) {
    router.push('/')
    return
  }

  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeyDown)

  // 聚焦到用户名输入框
  const usernameInput = document.getElementById('username')
  if (usernameInput) {
    usernameInput.focus()
  }
})

// 清理
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.login-page {
  /* 移动端视口高度优化 */
  min-height: 100vh;
  min-height: 100dvh; /* 动态视口高度 */
}

/* 表单动画 */
.login-page form {
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 移动端优化 */
@media (max-width: 767px) {
  .login-page {
    padding-bottom: env(safe-area-inset-bottom, 0);
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .login-page .bg-card {
    @apply border-2;
  }
}

/* 减少动画 */
@media (prefers-reduced-motion: reduce) {
  .login-page form {
    animation: none;
  }
}
</style>