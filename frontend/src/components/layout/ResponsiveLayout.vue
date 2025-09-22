<template>
  <div
    :class="[
      'responsive-layout',
      layoutClasses,
      orientationClasses,
      deviceClasses
    ]"
    :style="customProperties"
  >
    <!-- 主要内容区域 -->
    <main
      :class="[
        'main-content',
        mainContentClasses
      ]"
      role="main"
      :aria-label="$t('common.mainContent')"
    >
      <slot />
    </main>

    <!-- 移动端侧边栏覆盖层 -->
    <aside
      v-if="showMobileSidebar"
      :class="[
        'mobile-sidebar-overlay',
        'fixed inset-0 z-50 bg-background/80 backdrop-blur-sm',
        'md:hidden',
        mobileSidebarVisible ? 'animate-in fade-in-0' : 'animate-out fade-out-0'
      ]"
      @click="closeMobileSidebar"
    >
      <div
        :class="[
          'mobile-sidebar',
          'fixed left-0 top-0 h-full w-72 bg-card border-r shadow-lg',
          'transform transition-transform duration-300 ease-in-out',
          mobileSidebarVisible ? 'translate-x-0' : '-translate-x-full'
        ]"
        @click.stop
      >
        <!-- 侧边栏头部 -->
        <div class="flex items-center justify-between p-4 border-b">
          <slot name="sidebar-header">
            <h2 class="text-lg font-semibold">{{ $t('nav.menu') }}</h2>
          </slot>
          <button
            @click="closeMobileSidebar"
            class="p-2 rounded-md hover:bg-accent"
            :aria-label="$t('common.close')"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <!-- 侧边栏内容 -->
        <div class="p-4">
          <slot name="sidebar-content" />
        </div>
      </div>
    </aside>

    <!-- 浮动操作按钮 (FAB) - 移动端显示 -->
    <div
      v-if="showFab && breakpoints.isMobile"
      :class="[
        'fab-container',
        'fixed bottom-4 right-4 z-40'
      ]"
    >
      <slot name="fab">
        <button
          @click="toggleMobileSidebar"
          :class="[
            'fab-button',
            'bg-primary text-primary-foreground',
            'w-14 h-14 rounded-full shadow-lg',
            'flex items-center justify-center',
            'hover:bg-primary/90 active:scale-95',
            'transition-all duration-200',
            'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2'
          ]"
          :aria-label="$t('nav.toggleMenu')"
        >
          <Menu class="h-6 w-6" />
        </button>
      </slot>
    </div>

    <!-- 键盘导航提示 -->
    <div
      v-if="showKeyboardHints && !breakpoints.isTouchDevice"
      class="sr-only"
      aria-live="polite"
    >
      {{ keyboardHint }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Menu, X } from 'lucide-vue-next'
import { useBreakpoints } from '@/composables/useBreakpoints'

interface Props {
  // 布局配置
  container?: boolean
  padding?: boolean | string
  maxWidth?: string

  // 侧边栏配置
  showMobileSidebar?: boolean
  showFab?: boolean
  sidebarWidth?: string

  // 响应式配置
  mobileBreakpoint?: number
  tabletBreakpoint?: number

  // 键盘导航
  showKeyboardHints?: boolean

  // 自定义类名
  containerClass?: string
  mainClass?: string

  // 无障碍
  skipToMainId?: string
}

const props = withDefaults(defineProps<Props>(), {
  container: true,
  padding: true,
  maxWidth: '1200px',
  showMobileSidebar: false,
  showFab: true,
  sidebarWidth: '288px', // 18rem
  mobileBreakpoint: 768,
  tabletBreakpoint: 1024,
  showKeyboardHints: true,
  containerClass: '',
  mainClass: '',
  skipToMainId: 'main-content'
})

const emit = defineEmits<{
  sidebarToggle: [visible: boolean]
  breakpointChange: [breakpoint: string]
  orientationChange: [orientation: 'portrait' | 'landscape']
}>()

const { t } = useI18n()
const breakpoints = useBreakpoints({
  md: props.mobileBreakpoint,
  lg: props.tabletBreakpoint
})

// 移动端侧边栏状态
const mobileSidebarVisible = ref(false)
const keyboardHint = ref('')

// 布局类名计算
const layoutClasses = computed(() => [
  // 基础布局
  'min-h-screen',
  'relative',

  // 容器配置
  props.container && 'container mx-auto',

  // 响应式padding
  typeof props.padding === 'string' ? props.padding :
    props.padding ? 'px-4 md:px-6 lg:px-8' : '',

  // 自定义类名
  props.containerClass
])

const mainContentClasses = computed(() => [
  // 基础样式
  'flex-1',
  'min-h-0', // 防止flex子元素溢出

  // 最大宽度
  props.maxWidth && `max-w-[${props.maxWidth}]`,

  // 自定义类名
  props.mainClass,

  // 移动端适配
  breakpoints.isMobile.value && 'pb-20' // 为FAB留出空间
])

const orientationClasses = computed(() => [
  breakpoints.isLandscape.value ? 'layout-landscape' : 'layout-portrait'
])

const deviceClasses = computed(() => [
  breakpoints.isMobile.value && 'layout-mobile',
  breakpoints.isTablet.value && 'layout-tablet',
  breakpoints.isDesktop.value && 'layout-desktop',
  breakpoints.isTouchDevice.value && 'layout-touch'
])

// CSS自定义属性
const customProperties = computed(() => ({
  '--sidebar-width': props.sidebarWidth,
  '--mobile-breakpoint': `${props.mobileBreakpoint}px`,
  '--tablet-breakpoint': `${props.tabletBreakpoint}px`,
  '--viewport-width': `${breakpoints.width.value}px`,
  '--viewport-height': `${breakpoints.height.value}px`
}))

// 移动端侧边栏控制
const toggleMobileSidebar = () => {
  mobileSidebarVisible.value = !mobileSidebarVisible.value
  emit('sidebarToggle', mobileSidebarVisible.value)
}

const closeMobileSidebar = () => {
  mobileSidebarVisible.value = false
  emit('sidebarToggle', false)
}

// 键盘事件处理
const handleKeyDown = (event: KeyboardEvent) => {
  // ESC 关闭移动端侧边栏
  if (event.key === 'Escape' && mobileSidebarVisible.value) {
    closeMobileSidebar()
    keyboardHint.value = t('nav.sidebarClosed')
    return
  }

  // Ctrl/Cmd + M 切换移动端侧边栏
  if ((event.ctrlKey || event.metaKey) && event.key === 'm' && breakpoints.isMobile.value) {
    event.preventDefault()
    toggleMobileSidebar()
    keyboardHint.value = mobileSidebarVisible.value ?
      t('nav.sidebarOpened') : t('nav.sidebarClosed')
    return
  }

  // Tab 键导航提示
  if (event.key === 'Tab') {
    keyboardHint.value = t('nav.tabNavigation')
  }
}

// 触摸手势处理 (简单的左滑关闭)
let touchStartX = 0
let touchStartY = 0

const handleTouchStart = (event: TouchEvent) => {
  touchStartX = event.touches[0].clientX
  touchStartY = event.touches[0].clientY
}

const handleTouchEnd = (event: TouchEvent) => {
  if (!mobileSidebarVisible.value) return

  const touchEndX = event.changedTouches[0].clientX
  const touchEndY = event.changedTouches[0].clientY

  const deltaX = touchEndX - touchStartX
  const deltaY = touchEndY - touchStartY

  // 水平滑动距离大于垂直滑动距离，且向左滑动超过50px
  if (Math.abs(deltaX) > Math.abs(deltaY) && deltaX < -50) {
    closeMobileSidebar()
  }
}

// 监听断点变化
watch(() => breakpoints.current.value, (newBreakpoint, oldBreakpoint) => {
  if (newBreakpoint !== oldBreakpoint) {
    emit('breakpointChange', newBreakpoint)

    // 从移动端切换到桌面端时关闭移动端侧边栏
    if (breakpoints.isDesktop.value && mobileSidebarVisible.value) {
      closeMobileSidebar()
    }
  }
})

// 监听屏幕方向变化
watch(() => breakpoints.isLandscape.value, (isLandscape) => {
  emit('orientationChange', isLandscape ? 'landscape' : 'portrait')
})

// 生命周期
onMounted(() => {
  document.addEventListener('keydown', handleKeyDown)

  if (breakpoints.isTouchDevice.value) {
    document.addEventListener('touchstart', handleTouchStart, { passive: true })
    document.addEventListener('touchend', handleTouchEnd, { passive: true })
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)

  if (breakpoints.isTouchDevice.value) {
    document.removeEventListener('touchstart', handleTouchStart)
    document.removeEventListener('touchend', handleTouchEnd)
  }
})

// 暴露给父组件的方法
defineExpose({
  toggleSidebar: toggleMobileSidebar,
  closeSidebar: closeMobileSidebar,
  breakpoints,
  isSidebarVisible: mobileSidebarVisible
})
</script>

<style scoped>
.responsive-layout {
  /* CSS变量定义在computed中 */
}

/* 动画效果 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeOut {
  from { opacity: 1; }
  to { opacity: 0; }
}

.animate-in {
  animation: fadeIn 0.2s ease-out;
}

.animate-out {
  animation: fadeOut 0.2s ease-out;
}

/* 触摸优化 */
.layout-touch .fab-button {
  @apply min-h-[44px] min-w-[44px]; /* Apple推荐的最小触摸目标 */
}

.layout-touch button,
.layout-touch a {
  @apply min-h-[44px]; /* 确保所有可点击元素都有足够的触摸面积 */
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .mobile-sidebar {
    @apply border-2;
  }

  .fab-button {
    @apply border-2 border-foreground;
  }
}

/* 减少动画运动 */
@media (prefers-reduced-motion: reduce) {
  .mobile-sidebar {
    @apply transition-none;
  }

  .fab-button {
    @apply transition-none;
  }

  .animate-in,
  .animate-out {
    animation: none;
  }
}

/* 打印样式 */
@media print {
  .mobile-sidebar-overlay,
  .fab-container {
    @apply hidden;
  }
}
</style>