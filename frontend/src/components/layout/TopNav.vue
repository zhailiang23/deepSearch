<template>
  <div class="flex h-16 items-center justify-between px-6">
    <!-- 左侧：菜单按钮和面包屑 -->
    <div class="flex items-center space-x-4">
      <Button
        variant="ghost"
        size="sm"
        @click="$emit('toggle-sidebar')"
        class="lg:hidden"
      >
        <Menu class="h-4 w-4" />
      </Button>

      <Breadcrumb />
    </div>

    <!-- 右侧：用户菜单和设置 -->
    <div class="flex items-center space-x-4">
      <ThemeToggle />
      <LanguageSelector />
      <UserMenu />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Menu } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import Breadcrumb from '@/components/common/Breadcrumb.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import LanguageSelector from '@/components/common/LanguageSelector.vue'
import UserMenu from '@/components/common/UserMenu.vue'

defineEmits(['toggle-sidebar'])
</script>