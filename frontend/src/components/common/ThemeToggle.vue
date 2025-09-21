<template>
  <Button
    variant="ghost"
    size="sm"
    @click="toggleTheme"
    class="w-9 h-9 p-0"
  >
    <Sun class="h-4 w-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
    <Moon class="absolute h-4 w-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
    <span class="sr-only">切换主题</span>
  </Button>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Sun, Moon } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'

const toggleTheme = () => {
  const htmlElement = document.documentElement
  if (htmlElement.classList.contains('dark')) {
    htmlElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  } else {
    htmlElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark')
  }
})
</script>