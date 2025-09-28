<template>
  <div
    ref="containerRef"
    class="hot-word-cloud-chart w-full h-full relative"
    :class="containerClasses"
  >
    <!-- 画布容器 -->
    <div
      class="canvas-wrapper relative w-full h-full overflow-hidden rounded-lg"
      :style="containerStyle"
    >
      <!-- 主画布 -->
      <canvas
        ref="canvasRef"
        class="word-cloud-canvas w-full h-full"
        :class="canvasClasses"
        @click="handleCanvasClick"
        @mousemove="handleCanvasMouseMove"
        @mouseleave="handleCanvasMouseLeave"
      />

      <!-- 加载状态遮罩 -->
      <div
        v-if="renderState.isRendering || loading"
        class="absolute inset-0 bg-white/80 flex items-center justify-center z-10"
      >
        <div class="flex flex-col items-center space-y-3">
          <!-- 加载动画 -->
          <div class="w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />

          <!-- 进度条 -->
          <div class="w-32 h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              class="h-full bg-emerald-500 transition-all duration-300 ease-out"
              :style="{ width: `${renderState.progress}%` }"
            />
          </div>

          <!-- 进度文本 -->
          <p class="text-sm text-gray-600">
            正在生成词云图... {{ Math.round(renderState.progress) }}%
          </p>
        </div>
      </div>

      <!-- 错误状态 -->
      <div
        v-if="renderState.error || error"
        class="absolute inset-0 bg-red-50 flex items-center justify-center z-10"
      >
        <div class="text-center p-6">
          <div class="w-16 h-16 mx-auto mb-4 text-red-500">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-red-800 mb-2">渲染失败</h3>
          <p class="text-red-600 mb-4">{{ renderState.error || error }}</p>
          <button
            @click="handleRetry"
            class="px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors"
          >
            重试
          </button>
        </div>
      </div>

      <!-- 空数据状态 -->
      <div
        v-if="!loading && !renderState.isRendering && !renderState.error && !error && (!words || words.length === 0)"
        class="absolute inset-0 bg-gray-50 flex items-center justify-center z-10"
      >
        <div class="text-center p-6">
          <div class="w-16 h-16 mx-auto mb-4 text-gray-400">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4zm2.5 2.25l1.77-1.77L12 9.88l-3.5 3.5L4 9.88l1.77 1.77L9 15.88l3-3 6.5 6.5z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-gray-700 mb-2">暂无数据</h3>
          <p class="text-gray-500">请配置词云数据后查看效果</p>
        </div>
      </div>
    </div>

    <!-- 工具栏（可选） -->
    <div
      v-if="showToolbar"
      class="absolute top-2 right-2 flex space-x-2 z-20"
    >
      <!-- 重新渲染按钮 -->
      <button
        @click="handleRefresh"
        :disabled="renderState.isRendering"
        class="p-2 bg-white/90 hover:bg-white rounded-md shadow-sm border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        title="重新渲染"
      >
        <svg class="w-4 h-4 text-gray-600" viewBox="0 0 24 24" fill="currentColor">
          <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
        </svg>
      </button>

      <!-- 下载按钮 -->
      <button
        @click="handleDownload"
        :disabled="renderState.isRendering"
        class="p-2 bg-white/90 hover:bg-white rounded-md shadow-sm border border-gray-200 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        title="下载图片"
      >
        <svg class="w-4 h-4 text-gray-600" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 12v7H5v-7H3v7c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-7h-2zm-6 .67l2.59-2.58L17 11.5l-5 5-5-5 1.41-1.41L11 12.67V3h2v9.67z"/>
        </svg>
      </button>
    </div>

    <!-- 悬停提示 -->
    <div
      v-if="hoveredWord"
      ref="tooltipRef"
      class="absolute pointer-events-none z-30 bg-gray-900 text-white px-3 py-2 rounded-md shadow-lg transform -translate-x-1/2 -translate-y-full"
      :style="tooltipStyle"
    >
      <div class="text-sm font-medium">{{ hoveredWord.text }}</div>
      <div class="text-xs text-gray-300">权重: {{ hoveredWord.weight }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, type PropType } from 'vue'
import { useWordCloud, useWordCloudTheme } from '@/composables/useWordCloud'
import type {
  HotWordItem,
  WordCloudOptions,
  WordCloudTheme,
  WordCloudEvents
} from '@/types/statistics'

// ============= Props =============

const props = defineProps({
  /** 词云数据 */
  words: {
    type: Array as PropType<HotWordItem[]>,
    default: () => []
  },
  /** 画布宽度 */
  width: {
    type: Number,
    default: undefined
  },
  /** 画布高度 */
  height: {
    type: Number,
    default: undefined
  },
  /** 词云配置选项 */
  options: {
    type: Object as PropType<Partial<WordCloudOptions>>,
    default: () => ({})
  },
  /** 主题名称 */
  theme: {
    type: String,
    default: 'light-green'
  },
  /** 是否响应式 */
  responsive: {
    type: Boolean,
    default: true
  },
  /** 外部加载状态 */
  loading: {
    type: Boolean,
    default: false
  },
  /** 外部错误状态 */
  error: {
    type: String,
    default: null
  },
  /** 是否显示工具栏 */
  showToolbar: {
    type: Boolean,
    default: true
  },
  /** 自定义CSS类 */
  containerClass: {
    type: String,
    default: ''
  }
})

// ============= Emits =============

const emit = defineEmits<{
  /** 词语点击事件 */
  wordClick: [word: HotWordItem, event: Event]
  /** 词语悬停事件 */
  wordHover: [word: HotWordItem, event: Event]
  /** 渲染开始事件 */
  renderStart: []
  /** 渲染完成事件 */
  renderComplete: []
  /** 渲染错误事件 */
  renderError: [error: string]
  /** 下载事件 */
  download: [canvas: HTMLCanvasElement]
}>()

// ============= 模板引用 =============

const containerRef = ref<HTMLDivElement>()
const canvasRef = ref<HTMLCanvasElement | null>(null)
const tooltipRef = ref<HTMLDivElement>()

// ============= 状态管理 =============

/** 悬停的词语 */
const hoveredWord = ref<HotWordItem | null>(null)

/** 鼠标位置 */
const mousePosition = ref({ x: 0, y: 0 })

// ============= 主题管理 =============

const { currentTheme, setTheme, getColorFunction } = useWordCloudTheme()

// ============= 事件处理 =============

const events: WordCloudEvents = {
  onWordClick: (word: HotWordItem, event: Event) => {
    emit('wordClick', word, event)
  },
  onWordHover: (word: HotWordItem, event: Event) => {
    hoveredWord.value = word
    emit('wordHover', word, event)
  },
  onRenderStart: () => {
    emit('renderStart')
  },
  onRenderComplete: () => {
    emit('renderComplete')
  },
  onRenderError: (error: string) => {
    emit('renderError', error)
  }
}

// ============= 词云逻辑 =============

const {
  renderState,
  dimensions,
  canRender,
  renderWordCloud,
  stopRendering,
  clearCanvas,
  updateWords,
  updateDimensions,
  checkWordCloudAvailability
} = useWordCloud(canvasRef, {
  ...props.options,
  color: computed(() => {
    const themeColorFunction = currentTheme.value ? getColorFunction(currentTheme.value) : undefined
    return themeColorFunction
  }).value,
  fontFamily: computed(() => currentTheme.value?.fonts.family).value,
  fontWeight: computed(() => currentTheme.value?.fonts.weight).value,
  backgroundColor: computed(() => currentTheme.value?.colors.background).value
}, events)

// ============= 计算属性 =============

const containerClasses = computed(() => [
  props.containerClass,
  {
    'cursor-wait': renderState.isRendering,
    'cursor-default': !renderState.isRendering
  }
])

const canvasClasses = computed(() => ({
  'opacity-0': renderState.isRendering && !props.loading,
  'opacity-100': !renderState.isRendering || props.loading
}))

const containerStyle = computed(() => {
  const style: Record<string, string> = {}

  if (props.width) {
    style.width = `${props.width}px`
  }

  if (props.height) {
    style.height = `${props.height}px`
  }

  return style
})

const tooltipStyle = computed(() => ({
  left: `${mousePosition.value.x}px`,
  top: `${mousePosition.value.y}px`
}))

// ============= 方法 =============

/** 处理画布点击 */
const handleCanvasClick = (event: MouseEvent) => {
  // wordcloud2.js 会自动处理点击事件
  // 这里主要是为了兼容性和额外的处理逻辑
}

/** 处理画布鼠标移动 */
const handleCanvasMouseMove = (event: MouseEvent) => {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    mousePosition.value = {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top
    }
  }
}

/** 处理画布鼠标离开 */
const handleCanvasMouseLeave = () => {
  hoveredWord.value = null
}

/** 处理重新渲染 */
const handleRefresh = async () => {
  if (canRender.value) {
    await renderWordCloud()
  }
}

/** 处理重试 */
const handleRetry = async () => {
  clearCanvas()
  await handleRefresh()
}

/** 处理下载 */
const handleDownload = () => {
  if (canvasRef.value) {
    emit('download', canvasRef.value)

    // 默认下载逻辑
    const link = document.createElement('a')
    link.download = `wordcloud-${Date.now()}.png`
    link.href = canvasRef.value.toDataURL()
    link.click()
  }
}

/** 更新容器尺寸 */
const updateContainerDimensions = () => {
  if (!props.responsive || !containerRef.value) return

  const rect = containerRef.value.getBoundingClientRect()
  if (rect.width > 0 && rect.height > 0) {
    updateDimensions(rect.width, rect.height)
  }
}

// ============= 监听器 =============

// 监听词云数据变化
watch(
  () => props.words,
  async (newWords) => {
    if (newWords && newWords.length > 0) {
      await updateWords(newWords)
    } else {
      clearCanvas()
    }
  },
  { deep: true, immediate: true }
)

// 监听主题变化
watch(
  () => props.theme,
  (newTheme) => {
    setTheme(newTheme)
  },
  { immediate: true }
)

// 监听尺寸变化
watch(
  [() => props.width, () => props.height],
  ([newWidth, newHeight]) => {
    if (newWidth && newHeight) {
      updateDimensions(newWidth, newHeight)
    }
  }
)

// ============= 生命周期 =============

onMounted(async () => {
  // 检查WordCloud库
  const isAvailable = await checkWordCloudAvailability()
  if (!isAvailable) {
    console.error('WordCloud library is not available')
    return
  }

  // 初始化主题
  setTheme(props.theme)

  // 等待下一个tick确保DOM已渲染
  await nextTick()

  // 更新容器尺寸
  updateContainerDimensions()

  // 初始渲染
  if (props.words && props.words.length > 0) {
    await updateWords(props.words)
  }
})

onUnmounted(() => {
  stopRendering()
})

// ============= 公开方法 =============

defineExpose({
  /** 重新渲染 */
  refresh: handleRefresh,
  /** 下载图片 */
  download: handleDownload,
  /** 清空画布 */
  clear: clearCanvas,
  /** 停止渲染 */
  stop: stopRendering,
  /** 获取画布元素 */
  getCanvas: () => canvasRef.value,
  /** 获取渲染状态 */
  getRenderState: () => renderState
})
</script>

<style scoped>
.hot-word-cloud-chart {
  min-height: 200px;
}

.canvas-wrapper {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
}

.word-cloud-canvas {
  transition: opacity 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hot-word-cloud-chart {
    min-height: 150px;
  }
}

@media (max-width: 480px) {
  .hot-word-cloud-chart {
    min-height: 120px;
  }
}
</style>