<template>
  <div
    :class="[
      'dual-pane-layout',
      'h-full w-full',
      layoutClasses,
      containerClass
    ]"
    :style="customProperties"
  >
    <!-- 左栏 - 手机模拟器区域 -->
    <div
      :class="[
        'left-pane',
        'h-full',
        leftPaneClasses
      ]"
    >
      <div class="pane-content h-full">
        <slot name="left-pane">
          <div class="flex items-center justify-center h-full text-gray-400 bg-gray-50 rounded-lg">
            <div class="text-center">
              <div class="mb-3">
                <svg class="w-12 h-12 mx-auto text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                </svg>
              </div>
              <p class="text-sm">手机模拟器区域</p>
            </div>
          </div>
        </slot>
      </div>
    </div>

    <!-- 分隔线 (可选) -->
    <div
      v-if="showDivider"
      :class="[
        'divider',
        'shrink-0',
        dividerClasses
      ]"
    >
      <div class="h-full bg-gray-200 w-px"></div>
    </div>

    <!-- 右栏 - 参数控制区域 -->
    <div
      :class="[
        'right-pane',
        'h-full',
        rightPaneClasses
      ]"
    >
      <div class="pane-content h-full">
        <slot name="right-pane">
          <div class="flex items-center justify-center h-full text-gray-400 bg-gray-50 rounded-lg">
            <div class="text-center">
              <div class="mb-3">
                <svg class="w-12 h-12 mx-auto text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4"></path>
                </svg>
              </div>
              <p class="text-sm">参数控制区域</p>
            </div>
          </div>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useBreakpoints } from '@/composables/useBreakpoints'

interface Props {
  // 布局模式
  layoutMode?: 'grid' | 'flex'

  // 响应式配置
  stackOnMobile?: boolean
  mobileBreakpoint?: number

  // 分栏比例 (仅在 flex 模式下生效)
  leftPaneRatio?: number
  rightPaneRatio?: number

  // 间距配置
  gap?: string
  padding?: string

  // 分隔线
  showDivider?: boolean

  // 最小高度
  minHeight?: string

  // 自定义类名
  containerClass?: string
  leftPaneClass?: string
  rightPaneClass?: string
}

const props = withDefaults(defineProps<Props>(), {
  layoutMode: 'grid',
  stackOnMobile: true,
  mobileBreakpoint: 768,
  leftPaneRatio: 1,
  rightPaneRatio: 1,
  gap: '1rem',
  padding: '0',
  showDivider: false,
  minHeight: 'calc(100vh - 200px)',
  containerClass: '',
  leftPaneClass: '',
  rightPaneClass: ''
})

const breakpoints = useBreakpoints({
  md: props.mobileBreakpoint
})

// 布局类名计算
const layoutClasses = computed(() => {
  const baseClasses = []

  if (props.layoutMode === 'grid') {
    // Grid 布局模式
    baseClasses.push('grid')

    if (props.stackOnMobile && breakpoints.isMobile.value) {
      // 移动端堆叠布局
      baseClasses.push('grid-cols-1 gap-4')
    } else {
      // 桌面端双栏布局
      baseClasses.push('grid-cols-2')
      baseClasses.push(props.gap === '1rem' ? 'gap-4' : `gap-[${props.gap}]`)
    }
  } else {
    // Flex 布局模式
    baseClasses.push('flex')

    if (props.stackOnMobile && breakpoints.isMobile.value) {
      // 移动端堆叠布局
      baseClasses.push('flex-col')
      baseClasses.push(props.gap === '1rem' ? 'space-y-4' : `space-y-[${props.gap}]`)
    } else {
      // 桌面端双栏布局
      baseClasses.push('flex-row')
      baseClasses.push(props.gap === '1rem' ? 'space-x-4' : `space-x-[${props.gap}]`)
    }
  }

  return baseClasses
})

// 左栏类名
const leftPaneClasses = computed(() => {
  const classes = [props.leftPaneClass]

  if (props.layoutMode === 'flex' && !breakpoints.isMobile.value) {
    // Flex 模式下的比例控制
    const ratio = props.leftPaneRatio / (props.leftPaneRatio + props.rightPaneRatio)
    classes.push(`flex-[${ratio}]`)
  }

  return classes.filter(Boolean)
})

// 右栏类名
const rightPaneClasses = computed(() => {
  const classes = [props.rightPaneClass]

  if (props.layoutMode === 'flex' && !breakpoints.isMobile.value) {
    // Flex 模式下的比例控制
    const ratio = props.rightPaneRatio / (props.leftPaneRatio + props.rightPaneRatio)
    classes.push(`flex-[${ratio}]`)
  }

  return classes.filter(Boolean)
})

// 分隔线类名
const dividerClasses = computed(() => [
  breakpoints.isMobile.value && props.stackOnMobile ? 'hidden' : 'block'
])

// CSS 自定义属性
const customProperties = computed(() => ({
  '--min-height': props.minHeight,
  '--gap': props.gap,
  '--padding': props.padding,
  '--mobile-breakpoint': `${props.mobileBreakpoint}px`
}))

// 暴露给父组件的数据
defineExpose({
  breakpoints,
  isMobile: breakpoints.isMobile
})
</script>

<style scoped>
.dual-pane-layout {
  min-height: var(--min-height);
  padding: var(--padding);
}

.pane-content {
  /* 确保内容区域能正确滚动 */
  overflow: auto;
  position: relative;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .dual-pane-layout {
    /* 移动端优化 */
    min-height: auto;
  }

  .pane-content {
    /* 移动端减小最小高度 */
    min-height: 300px;
  }
}

/* 高对比度模式支持 */
@media (prefers-contrast: high) {
  .divider {
    @apply bg-gray-800;
  }
}

/* 减少动画运动偏好 */
@media (prefers-reduced-motion: reduce) {
  .pane-content {
    @apply transition-none;
  }
}

/* 打印样式优化 */
@media print {
  .dual-pane-layout {
    @apply grid-cols-1 gap-0;
  }

  .divider {
    @apply hidden;
  }
}
</style>