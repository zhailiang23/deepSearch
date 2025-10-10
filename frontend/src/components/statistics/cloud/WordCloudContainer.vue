<template>
  <div
    ref="containerRef"
    class="word-cloud-container relative overflow-hidden"
    :class="containerClasses"
    :style="containerStyle"
  >
    <!-- 响应式词云组件 -->
    <HotWordCloudChart
      ref="wordCloudRef"
      :words="optimizedWords"
      :width="dimensions.width"
      :height="dimensions.height"
      :options="optimizedOptions"
      :theme="theme"
      :responsive="true"
      :loading="performanceState.isProcessing || loading"
      :error="performanceState.error || error"
      :show-toolbar="showToolbar"
      :container-class="wordCloudClass"
      @word-click="handleWordClick"
      @word-hover="handleWordHover"
      @render-start="handleRenderStart"
      @render-complete="handleRenderComplete"
      @render-error="handleRenderError"
      @download="handleDownload"
    />

    <!-- 性能监控面板 (开发模式) -->
    <div
      v-if="showPerformancePanel && performanceMetrics"
      class="absolute top-2 left-2 bg-black/80 text-white rounded-lg p-3 text-xs z-50"
      :class="{ 'opacity-50 hover:opacity-100': !showPerformancePanel }"
    >
      <div class="font-semibold mb-2">性能监控</div>
      <div class="space-y-1">
        <div>渲染时间: {{ performanceMetrics.renderTime }}ms</div>
        <div>FPS: {{ performanceMetrics.fps }}</div>
        <div>内存: {{ formatMemory(performanceMetrics.memoryUsage) }}</div>
        <div>词语数: {{ optimizedWords.length }}</div>
        <div :class="getPerformanceLevelClass(performanceLevel)">
          性能等级: {{ performanceLevel }}
        </div>
      </div>
    </div>

    <!-- 自动优化提示 -->
    <Transition
      name="slide-down"
      enter-active-class="transition-all duration-300 ease-out"
      leave-active-class="transition-all duration-200 ease-in"
      enter-from-class="transform -translate-y-full opacity-0"
      enter-to-class="transform translate-y-0 opacity-100"
      leave-from-class="transform translate-y-0 opacity-100"
      leave-to-class="transform -translate-y-full opacity-0"
    >
      <div
        v-if="showOptimizationTip && optimizationResult?.suggestions.length"
        class="absolute top-0 left-0 right-0 bg-blue-50 border-b border-blue-200 px-4 py-2 z-40"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-2">
            <svg class="w-4 h-4 text-blue-500" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
            </svg>
            <span class="text-sm text-blue-700">
              {{ optimizationResult.suggestions[0] }}
            </span>
          </div>
          <button
            @click="showOptimizationTip = false"
            class="text-blue-500 hover:text-blue-700 p-1"
          >
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </div>
    </Transition>

    <!-- 调整尺寸手柄 -->
    <div
      v-if="resizable"
      class="absolute bottom-0 right-0 w-4 h-4 cursor-se-resize opacity-30 hover:opacity-60 transition-opacity"
      @mousedown="startResize"
    >
      <svg class="w-full h-full text-gray-400" fill="currentColor" viewBox="0 0 20 20">
        <path d="M5 5h2v2H5V5zm4 4h2v2H9V9zm4 4h2v2h-2v-2z"/>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick, type PropType } from 'vue'
import { useResizeObserver } from '@vueuse/core'
import HotWordCloudChart from '../HotWordCloudChart.vue'
import { useWordCloudPerformance } from '@/composables/useWordCloudPerformance'
import {
  generateOptimizedConfig,
  detectDevicePerformance,
  getBreakpoint,
  adjustConfigForBreakpoint,
  debounce,
  type OptimizationContext,
  type RenderOptimizationResult
} from '@/utils/wordCloudOptimizations'
import type {
  HotWordItem,
  WordCloudOptions,
  WordCloudPerformanceConfig
} from '@/types/statistics'

// ============= Props =============

const props = defineProps({
  /** 词云数据 */
  words: {
    type: Array as PropType<HotWordItem[]>,
    default: () => []
  },
  /** 初始宽度 */
  initialWidth: {
    type: Number,
    default: undefined
  },
  /** 初始高度 */
  initialHeight: {
    type: Number,
    default: undefined
  },
  /** 最小宽度 */
  minWidth: {
    type: Number,
    default: 300
  },
  /** 最小高度 */
  minHeight: {
    type: Number,
    default: 200
  },
  /** 最大宽度 */
  maxWidth: {
    type: Number,
    default: undefined
  },
  /** 最大高度 */
  maxHeight: {
    type: Number,
    default: undefined
  },
  /** 词云基础配置 */
  options: {
    type: Object as PropType<Partial<WordCloudOptions>>,
    default: () => ({})
  },
  /** 性能配置 */
  performanceConfig: {
    type: Object as PropType<Partial<WordCloudPerformanceConfig>>,
    default: () => ({})
  },
  /** 主题 */
  theme: {
    type: String,
    default: 'light-green'
  },
  /** 是否自动优化 */
  autoOptimize: {
    type: Boolean,
    default: true
  },
  /** 是否显示性能面板 */
  showPerformancePanel: {
    type: Boolean,
    default: false
  },
  /** 是否显示工具栏 */
  showToolbar: {
    type: Boolean,
    default: true
  },
  /** 是否可调整尺寸 */
  resizable: {
    type: Boolean,
    default: false
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
  /** 容器CSS类 */
  containerClass: {
    type: String,
    default: ''
  },
  /** 词云CSS类 */
  wordCloudClass: {
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
  /** 尺寸变化事件 */
  resize: [width: number, height: number]
  /** 性能警告事件 */
  performanceWarning: [level: string, metrics: any]
}>()

// ============= 模板引用 =============

const containerRef = ref<HTMLDivElement>()
const wordCloudRef = ref<InstanceType<typeof HotWordCloudChart>>()

// ============= 状态管理 =============

/** 容器尺寸 */
const dimensions = reactive({
  width: props.initialWidth || 800,
  height: props.initialHeight || 600
})

/** 性能状态 */
const performanceState = reactive({
  isProcessing: false,
  error: null as string | null,
  lastOptimization: Date.now()
})

/** 优化结果 */
const optimizationResult = ref<RenderOptimizationResult | null>(null)

/** 是否显示优化提示 */
const showOptimizationTip = ref(false)

/** 调整尺寸状态 */
const resizeState = reactive({
  isResizing: false,
  startX: 0,
  startY: 0,
  startWidth: 0,
  startHeight: 0
})

// ============= 性能管理 =============

const {
  metrics: performanceMetrics,
  processedWords,
  config: performanceConfigRef,
  performanceLevel,
  preprocessWordData,
  smartResponsiveRender,
  virtualizeWordRendering,
  adjustRenderQuality,
  measureRenderPerformance
} = useWordCloudPerformance(props.performanceConfig)

// ============= 计算属性 =============

/** 优化后的词语数据 */
const optimizedWords = computed(() => {
  if (!props.autoOptimize) {
    return [...props.words]
  }

  if (processedWords.value.length > 0) {
    return virtualizeWordRendering(
      [...processedWords.value],
      dimensions.width,
      dimensions.height
    )
  }

  return [...props.words]
})

/** 优化后的配置选项 */
const optimizedOptions = computed(() => {
  let baseOptions = { ...props.options }

  if (props.autoOptimize && optimizationResult.value) {
    baseOptions = {
      ...baseOptions,
      ...optimizationResult.value.optimizedOptions
    }
  }

  // 根据响应式断点调整
  const breakpoint = getBreakpoint(dimensions.width)
  baseOptions = adjustConfigForBreakpoint(baseOptions, breakpoint)

  // 根据实时性能调整
  if (performanceMetrics && props.autoOptimize) {
    const qualitySettings = adjustRenderQuality()
    if (qualitySettings) {
      baseOptions.gridSize = qualitySettings.gridSize
      baseOptions.rotationSteps = qualitySettings.rotationSteps
    }
  }

  return baseOptions
})

/** 容器样式 */
const containerStyle = computed(() => {
  const style: Record<string, string> = {}

  if (props.initialWidth) {
    style.width = `${dimensions.width}px`
  }

  if (props.initialHeight) {
    style.height = `${dimensions.height}px`
  }

  if (props.minWidth) {
    style.minWidth = `${props.minWidth}px`
  }

  if (props.minHeight) {
    style.minHeight = `${props.minHeight}px`
  }

  if (props.maxWidth) {
    style.maxWidth = `${props.maxWidth}px`
  }

  if (props.maxHeight) {
    style.maxHeight = `${props.maxHeight}px`
  }

  return style
})

/** 容器CSS类 */
const containerClasses = computed(() => [
  props.containerClass,
  {
    'border-2 border-blue-300': resizeState.isResizing,
    'transition-all duration-200': !resizeState.isResizing
  }
])

// ============= 方法 =============

/** 生成优化配置 */
const generateOptimization = async () => {
  if (!props.autoOptimize) return

  performanceState.isProcessing = true
  performanceState.error = null

  try {
    const context: OptimizationContext = {
      containerWidth: dimensions.width,
      containerHeight: dimensions.height,
      devicePixelRatio: window.devicePixelRatio || 1,
      availableMemory: performanceMetrics?.memoryUsage || 0,
      currentFPS: performanceMetrics?.fps || 60
    }

    const result = generateOptimizedConfig(context, props.options)
    optimizationResult.value = result

    // 显示优化提示
    if (result.suggestions.length > 0) {
      showOptimizationTip.value = true
      setTimeout(() => {
        showOptimizationTip.value = false
      }, 5000)
    }

    // 发出性能警告
    if (result.performanceLevel === 'low') {
      emit('performanceWarning', result.performanceLevel, performanceMetrics)
    }
  } catch (error) {
    performanceState.error = error instanceof Error ? error.message : '优化失败'
    console.error('词云优化失败:', error)
  } finally {
    performanceState.isProcessing = false
  }
}

/** 更新容器尺寸 */
const updateDimensions = (width: number, height: number) => {
  // 应用尺寸限制
  const newWidth = Math.max(
    props.minWidth,
    props.maxWidth ? Math.min(width, props.maxWidth) : width
  )
  const newHeight = Math.max(
    props.minHeight,
    props.maxHeight ? Math.min(height, props.maxHeight) : height
  )

  if (dimensions.width !== newWidth || dimensions.height !== newHeight) {
    dimensions.width = newWidth
    dimensions.height = newHeight
    emit('resize', newWidth, newHeight)
  }
}

/** 防抖的尺寸更新 */
const debouncedUpdateDimensions = debounce(updateDimensions, 150)

/** 防抖的优化生成 */
const debouncedGenerateOptimization = debounce(generateOptimization, 300)

/** 处理容器尺寸变化 */
const handleContainerResize = () => {
  if (!containerRef.value) return

  const rect = containerRef.value.getBoundingClientRect()
  debouncedUpdateDimensions(rect.width, rect.height)
}

/** 开始调整尺寸 */
const startResize = (event: MouseEvent) => {
  if (!props.resizable) return

  event.preventDefault()

  resizeState.isResizing = true
  resizeState.startX = event.clientX
  resizeState.startY = event.clientY
  resizeState.startWidth = dimensions.width
  resizeState.startHeight = dimensions.height

  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
}

/** 处理调整尺寸 */
const handleResize = (event: MouseEvent) => {
  if (!resizeState.isResizing) return

  const deltaX = event.clientX - resizeState.startX
  const deltaY = event.clientY - resizeState.startY

  const newWidth = resizeState.startWidth + deltaX
  const newHeight = resizeState.startHeight + deltaY

  updateDimensions(newWidth, newHeight)
}

/** 停止调整尺寸 */
const stopResize = () => {
  resizeState.isResizing = false
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
}

/** 格式化内存显示 */
const formatMemory = (bytes: number): string => {
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(1)}MB`
}

/** 获取性能等级样式 */
const getPerformanceLevelClass = (level: string) => ({
  'text-green-400': level === 'high',
  'text-yellow-400': level === 'medium',
  'text-red-400': level === 'low'
})

// ============= 事件处理 =============

const handleWordClick = (word: HotWordItem, event: Event) => {
  emit('wordClick', word, event)
}

const handleWordHover = (word: HotWordItem, event: Event) => {
  emit('wordHover', word, event)
}

const handleRenderStart = () => {
  emit('renderStart')
}

const handleRenderComplete = () => {
  emit('renderComplete')
}

const handleRenderError = (error: string) => {
  performanceState.error = error
  emit('renderError', error)
}

const handleDownload = (canvas: HTMLCanvasElement) => {
  emit('download', canvas)
}

// ============= 监听器 =============

// 监听词语数据变化
watch(
  () => props.words,
  async (newWords) => {
    if (newWords && newWords.length > 0 && props.autoOptimize) {
      await preprocessWordData(newWords)
      await debouncedGenerateOptimization()
    }
  },
  { deep: true, immediate: true }
)

// 监听尺寸变化
watch(
  [() => dimensions.width, () => dimensions.height],
  () => {
    if (props.autoOptimize) {
      debouncedGenerateOptimization()
    }
  }
)

// 监听性能配置变化
watch(
  () => props.performanceConfig,
  (newConfig) => {
    Object.assign(performanceConfigRef, newConfig)
  },
  { deep: true }
)

// ============= 生命周期 =============

onMounted(async () => {
  await nextTick()

  // 设置ResizeObserver监听容器尺寸变化
  if (containerRef.value) {
    useResizeObserver(containerRef.value, handleContainerResize)

    // 初始化尺寸
    const rect = containerRef.value.getBoundingClientRect()
    if (rect.width > 0 && rect.height > 0) {
      updateDimensions(rect.width, rect.height)
    }
  }

  // 初始优化
  if (props.autoOptimize) {
    await generateOptimization()
  }
})

onUnmounted(() => {
  // 清理调整尺寸事件监听器
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
})

// ============= 公开方法 =============

defineExpose({
  /** 刷新词云 */
  refresh: () => wordCloudRef.value?.refresh(),
  /** 下载图片 */
  download: () => wordCloudRef.value?.download(),
  /** 清空画布 */
  clear: () => wordCloudRef.value?.clear(),
  /** 停止渲染 */
  stop: () => wordCloudRef.value?.stop(),
  /** 获取画布元素 */
  getCanvas: () => wordCloudRef.value?.getCanvas(),
  /** 获取渲染状态 */
  getRenderState: () => wordCloudRef.value?.getRenderState(),
  /** 获取性能指标 */
  getPerformanceMetrics: () => performanceMetrics,
  /** 获取优化结果 */
  getOptimizationResult: () => optimizationResult.value,
  /** 手动触发优化 */
  optimize: generateOptimization,
  /** 更新尺寸 */
  updateSize: updateDimensions
})
</script>

<style scoped>
.word-cloud-container {
  background: linear-gradient(135deg, #f8fffe 0%, #f0fdf4 100%);
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.word-cloud-container:hover {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .word-cloud-container {
    border-radius: 8px;
  }
}

@media (max-width: 480px) {
  .word-cloud-container {
    border-radius: 6px;
  }
}

/* 过渡动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>