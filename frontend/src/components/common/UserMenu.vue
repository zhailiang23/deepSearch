<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button
        variant="ghost"
        class="relative h-8 w-8 rounded-full"
        data-testid="user-menu"
      >
        <div class="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
          <User class="h-4 w-4" />
        </div>
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent class="w-56" align="end" force-mount>
      <DropdownMenuLabel class="font-normal">
        <div class="flex flex-col space-y-1">
          <p class="text-sm font-medium leading-none">{{ user?.fullName || user?.username }}</p>
          <p class="text-xs leading-none text-muted-foreground">
            {{ user?.email }}
          </p>
        </div>
      </DropdownMenuLabel>
      <DropdownMenuSeparator />
      <DropdownMenuItem @click="viewProfile">
        <UserIcon class="mr-2 h-4 w-4" />
        <span>个人资料</span>
      </DropdownMenuItem>
      <DropdownMenuItem @click="goToSettings">
        <SettingsIcon class="mr-2 h-4 w-4" />
        <span>设置</span>
      </DropdownMenuItem>
      <DropdownMenuSeparator />
      <DropdownMenuItem @click="logout" data-testid="logout-button">
        <LogOut class="mr-2 h-4 w-4" />
        <span>退出登录</span>
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Settings as SettingsIcon, LogOut, User as UserIcon } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const user = computed(() => authStore.user)

const viewProfile = () => {
  router.push('/profile')
}

const goToSettings = () => {
  router.push('/settings')
}

const logout = async () => {
  try {
    await authStore.logout()
    // 登出后重定向到登录页面
    await router.push('/login')
  } catch (error) {
    console.error('登出失败:', error)
    // 即使登出失败也要重定向到登录页面
    await router.push('/login')
  }
}
</script>