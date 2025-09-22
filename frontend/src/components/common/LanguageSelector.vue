<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button
        variant="ghost"
        size="sm"
        :class="[
          'gap-2',
          // 响应式尺寸
          breakpoints?.isMobile ? 'h-9 px-2' : 'h-9 px-3'
        ]"
        :aria-label="$t('common.selectLanguage')"
      >
        <!-- 当前语言标志 -->
        <span
          v-if="currentLocaleInfo"
          class="text-base leading-none"
          :aria-hidden="true"
        >
          {{ currentLocaleInfo.flag }}
        </span>

        <!-- 语言图标 -->
        <Languages
          :class="[
            breakpoints?.isMobile ? 'h-3 w-3' : 'h-4 w-4'
          ]"
        />

        <!-- 桌面端显示语言名称 -->
        <span
          v-if="!breakpoints?.isMobile && currentLocaleInfo"
          class="hidden sm:inline-block text-sm font-medium"
        >
          {{ currentLocaleInfo.name }}
        </span>

        <!-- 屏幕阅读器文本 -->
        <span class="sr-only">{{ $t('common.selectLanguage') }}</span>
      </Button>
    </DropdownMenuTrigger>

    <DropdownMenuContent
      align="end"
      :class="[
        // 响应式宽度
        breakpoints?.isMobile ? 'w-40' : 'w-48'
      ]"
    >
      <DropdownMenuItem
        v-for="lang in availableLocales"
        :key="lang.code"
        @click="changeLanguage(lang.code)"
        :class="[
          'flex items-center gap-3 cursor-pointer',
          currentLocale === lang.code && 'bg-accent text-accent-foreground'
        ]"
        :aria-current="currentLocale === lang.code ? 'true' : 'false'"
      >
        <!-- 国旗图标 -->
        <span
          class="text-base leading-none flex-shrink-0"
          :aria-hidden="true"
        >
          {{ lang.flag }}
        </span>

        <!-- 语言信息 -->
        <div class="flex flex-col min-w-0 flex-1">
          <span class="text-sm font-medium truncate">
            {{ lang.name }}
          </span>
          <span
            v-if="lang.nativeName && String(lang.nativeName) !== String(lang.name)"
            class="text-xs text-muted-foreground truncate"
          >
            {{ lang.nativeName }}
          </span>
        </div>

        <!-- 当前选中标识 -->
        <Check
          v-if="currentLocale === lang.code"
          class="h-4 w-4 text-primary flex-shrink-0"
          :aria-hidden="true"
        />
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { Languages, Check } from 'lucide-vue-next'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

import {
  setLocale,
  getCurrentLocaleInfo,
  getAvailableLocales,
  type SupportedLocale
} from '@/locales'

interface Props {
  size?: 'sm' | 'md' | 'lg'
  showText?: boolean
  showFlag?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'sm',
  showText: true,
  showFlag: true
})

// 可选的断点注入（如果在响应式布局中使用）
const breakpoints = inject<any>('breakpoints', null)

const { locale, t } = useI18n()

// 计算属性
const currentLocale = computed(() => locale.value as SupportedLocale)
const currentLocaleInfo = computed(() => getCurrentLocaleInfo())
const availableLocales = computed(() => getAvailableLocales())

// 语言切换处理
const changeLanguage = (langCode: SupportedLocale) => {
  try {
    setLocale(langCode)

    // 触发自定义事件（如果需要）
    emit('language-changed', {
      from: currentLocale.value,
      to: langCode,
      localeInfo: getCurrentLocaleInfo()
    })

    // 可选：显示切换成功提示
    console.log(`Language changed to: ${langCode}`)
  } catch (error) {
    console.error('Failed to change language:', error)
  }
}

// 事件定义
const emit = defineEmits<{
  'language-changed': [details: {
    from: SupportedLocale
    to: SupportedLocale
    localeInfo: typeof currentLocaleInfo.value
  }]
}>()

// 键盘导航支持
const handleKeyDown = (event: KeyboardEvent, langCode: SupportedLocale) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    changeLanguage(langCode)
  }
}
</script>

<style scoped>
/* 确保图标和文本垂直居中 */
.dropdown-menu-item {
  align-items: center;
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .dropdown-menu-content {
    @apply border-2;
  }
}

/* 减少动画运动 */
@media (prefers-reduced-motion: reduce) {
  .dropdown-menu-content {
    @apply transition-none;
  }
}
</style>