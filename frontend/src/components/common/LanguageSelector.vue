<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button variant="ghost" size="sm" class="w-9 h-9 p-0">
        <Languages class="h-4 w-4" />
        <span class="sr-only">选择语言</span>
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end">
      <DropdownMenuItem
        v-for="lang in languages"
        :key="lang.code"
        @click="changeLanguage(lang.code)"
        :class="{ 'bg-accent': currentLocale === lang.code }"
      >
        {{ lang.name }}
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Languages } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

const { locale } = useI18n()

const languages = [
  { code: 'zh-CN', name: '中文' },
  { code: 'en-US', name: 'English' }
]

const currentLocale = computed(() => locale.value)

const changeLanguage = (langCode: string) => {
  locale.value = langCode
  localStorage.setItem('language', langCode)
}
</script>