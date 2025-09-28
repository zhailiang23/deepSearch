<template>
  <div class="hot-word-cloud-chart" :class="{ 'loading': isLoading, 'error': hasError }">
    <!-- 工具栏 -->
    <div v-if="showToolbar" class="cloud-toolbar">
      <div class="toolbar-left">
        <h3 class="chart-title">{{ title || '热词云图' }}</h3>
        <span v-if="wordCount > 0" class="word-count">{{ wordCount }} 个词语</span>
      </div>
      <div class="toolbar-right">
        <Button
          variant="outline"
          size="sm"
          @click="refreshChart"
          :disabled="isLoading"
          class="refresh-btn"
        >
          <RotateCcw class="h-4 w-4 mr-1" />
          刷新
        </Button>
        <Button
          variant="outline"
          size="sm"
          @click="downloadChart"
          :disabled="isLoading || hasError"
          class="download-btn"
        >
          <Download class="h-4 w-4 mr-1" />
          下载
        </Button>
        <Button
          variant="outline"
          size="sm"
          @click="toggleSettings"
          class="settings-btn"
        >
          <Settings class="h-4 w-4" />
        </Button>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="chart-container" :style="containerStyle">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-overlay">
        <div class="loading-content">
          <div class="loading-spinner"></div>
          <div class="loading-text">{{ loadingText }}</div>
          <div v-if="renderProgress > 0" class="loading-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: renderProgress + '%' }"></div>
            </div>
            <span class="progress-text">{{ Math.round(renderProgress) }}%</span>
          </div>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-if="hasError && !isLoading" class="error-overlay">
        <div class="error-content">
          <AlertCircle class="error-icon" />
          <div class="error-message">{{ errorMessage }}</div>
          <Button variant="outline" size="sm" @click="retryRender" class="retry-btn">
            重试
          </Button>
        </div>
      </div>

      <!-- 空数据状态 -->
      <div v-if="isEmpty && !isLoading && !hasError" class="empty-overlay">
        <div class="empty-content">
          <FileText class="empty-icon" />
          <div class="empty-message">暂无词云数据</div>
          <div class="empty-description">请选择时间范围或调整筛选条件</div>
        </div>
      </div>

      <!-- 词云Canvas -->
      <canvas
        ref="canvasRef"
        class="word-cloud-canvas"
        :width="canvasWidth"
        :height="canvasHeight"
        :style="canvasStyle"
      ></canvas>

      <!-- 交互组件 -->
      <WordCloudInteraction
        v-if="interactive && !isEmpty"
        :canvas-ref="canvasRef"
        :config="interactionConfig"
        :show-controls="showInteractionControls"
        :show-status="showInteractionStatus"
        :show-tooltip="showTooltip"
        @word-click="handleWordClick"
        @word-hover="handleWordHover"
        @zoom="handleZoom"
        @drag="handleDrag"
        @config-change="handleInteractionConfigChange"
      />

      <!-- 配置面板 -->
      <div v-if="showConfigPanel" class="config-panel-overlay">
        <div class="config-panel">
          <div class="panel-header">
            <h4 class="panel-title">词云配置</h4>
            <Button
              variant="ghost"
              size="sm"
              @click="toggleSettings"
              class="h-6 w-6 p-0"
            >
              <X class="h-4 w-4" />
            </Button>
          </div>
          <div class="panel-content">
            <WordCloudConfig
              :config="themeConfig"
              :themes="availableThemes"
              @config-change="handleThemeConfigChange"
              @theme-change="handleThemeChange"
              @reset="handleConfigReset"
              @export="handleConfigExport"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 调试信息 -->
    <div v-if="showDebugInfo && debugInfo" class="debug-info">
      <details class="debug-details">
        <summary class="debug-summary">调试信息</summary>
        <div class="debug-content">
          <div class="debug-item">
            <span class="debug-label">渲染时间:</span>
            <span class="debug-value">{{ debugInfo.renderTime }}ms</span>
          </div>
          <div class="debug-item">
            <span class="debug-label">词语数量:</span>
            <span class="debug-value">{{ debugInfo.totalWords }}</span>
          </div>
          <div class="debug-item">
            <span class="debug-label">成功渲染:</span>
            <span class="debug-value">{{ debugInfo.renderedWords }}</span>
          </div>
          <div class="debug-item">
            <span class="debug-label">最大权重:</span>
            <span class="debug-value">{{ debugInfo.maxWeight }}</span>
          </div>
          <div class="debug-item">
            <span class="debug-label">画布尺寸:</span>
            <span class="debug-value">{{ canvasWidth }} × {{ canvasHeight }}</span>
          </div>
        </div>
      </details>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { Button } from '@/components/ui/button'
import { RotateCcw, Download, Settings, X, AlertCircle, FileText } from 'lucide-vue-next'

import WordCloudInteraction from './cloud/WordCloudInteraction.vue'
import WordCloudConfig from './cloud/WordCloudConfig.vue'

import type {
  WordCloudItem,
  WordCloudConfig as WordCloudConfigType,
  WordCloudDimension,
  WordCloudStats
} from '@/types/statistics'

import type {
  WordCloudThemeConfig,
  WordCloudInteractionConfig
} from '@/types/wordCloudTypes'

import {
  PRESET_THEMES,
  LIGHT_THEME_CONFIG,
  DARK_THEME_CONFIG,
  DEFAULT_WORDCLOUD_CONFIG
} from '@/constants/wordCloudDefaults'

import {
  getCurrentThemeMode,
  getThemeByName,
  applyThemeToConfig,
  createResponsiveConfig,
  optimizeConfigForDataset
} from '@/utils/wordCloudThemes'

import { createDefaultInteractionConfig } from '@/composables/useWordCloudEvents'
import { useWordCloud } from '@/composables/useWordCloud'

// Props接口
interface Props {
  words: WordCloudItem[]
  width?: number
  height?: number
  theme?: string
  options?: Partial<WordCloudConfigType>
  interactive?: boolean
  showToolbar?: boolean
  showInteractionControls?: boolean
  showInteractionStatus?: boolean
  showTooltip?: boolean
  showDebugInfo?: boolean
  title?: string
  loading?: boolean
  error?: string | null
  loadingText?: string
  responsive?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  width: 800,
  height: 600,
  theme: 'auto',
  interactive: true,
  showToolbar: true,
  showInteractionControls: true,
  showInteractionStatus: true,
  showTooltip: true,
  showDebugInfo: false,
  loadingText: '正在生成词云...',
  responsive: true
})

// Emits
const emit = defineEmits<{
  wordClick: [word: string, item: WordCloudItem, event: MouseEvent]
  wordHover: [word: string | null, item: WordCloudItem | null]
  renderStart: []
  renderComplete: [stats: WordCloudStats]
  renderError: [error: Error]
  download: [canvas: HTMLCanvasElement]
  configChange: [config: WordCloudConfigType]
  themeChange: [theme: string]
}>()

// 响应式数据
const canvasRef = ref<HTMLCanvasElement | null>(null)
const showConfigPanel = ref(false)

// 配置管理
const themeConfig = reactive<WordCloudThemeConfig>({ ...LIGHT_THEME_CONFIG })
const interactionConfig = reactive<WordCloudInteractionConfig>(createDefaultInteractionConfig())
const availableThemes = ref(PRESET_THEMES)

// 使用词云可组合函数
const {
  words: wordCloudWords,
  renderState,
  dimensions,
  performanceConfig,
  canRender,
  mergedOptions,
  renderWordCloud,
  stopRendering,
  clearCanvas,
  updateWords,
  updateDimensions,
  debouncedRender,
  setupResizeObserver,
  getRenderStats,
  cleanup
} = useWordCloud(canvasRef, mergedWordCloudConfig)

// 状态管理（从useWordCloud获取）
const isLoading = computed(() => props.loading || renderState.isRendering)
const hasError = computed(() => !!props.error || renderState.hasError)
const errorMessage = computed(() => props.error || renderState.errorMessage || '')
const renderProgress = computed(() => renderState.renderProgress)

// 调试信息
const debugInfo = computed(() => getRenderStats())

// 计算属性
const wordCount = computed(() => props.words?.length || 0)
const isEmpty = computed(() => wordCount.value === 0)

const containerStyle = computed(() => ({
  width: props.responsive ? '100%' : `${props.width}px`,
  height: props.responsive ? 'auto' : `${props.height}px`,
  aspectRatio: props.responsive ? `${props.width} / ${props.height}` : undefined
}))

const canvasWidth = computed(() => {
  if (props.responsive && canvasRef.value) {
    const container = canvasRef.value.parentElement
    return container ? container.clientWidth : props.width
  }
  return props.width
})

const canvasHeight = computed(() => {
  if (props.responsive && canvasRef.value) {
    const container = canvasRef.value.parentElement
    const aspectRatio = props.width / props.height
    return Math.round(canvasWidth.value / aspectRatio)
  }
  return props.height
})

const canvasStyle = computed(() => ({
  maxWidth: '100%',
  height: 'auto',
  display: isEmpty.value ? 'none' : 'block'
}))

// WordCloud配置
const mergedWordCloudConfig = computed(() => {
  const baseConfig = applyThemeToConfig(props.options || {}, themeConfig)
  const responsiveConfig = createResponsiveConfig(baseConfig, canvasWidth.value, canvasHeight.value)
  const optimizedConfig = optimizeConfigForDataset(responsiveConfig, props.words)

  // 添加交互回调
  optimizedConfig.hover = handleWordCloudHover
  optimizedConfig.click = handleWordCloudClick

  return optimizedConfig
})

// 方法
const initializeWordCloud = async () => {
  if (isEmpty.value) return

  try {
    emit('renderStart')
    await updateWords(props.words)
  } catch (error) {
    console.error('WordCloud渲染错误:', error)
    emit('renderError', error instanceof Error ? error : new Error('Unknown error'))
  }
}

const handleWordCloudClick = (item: WordCloudItem, dimension: any, event: MouseEvent) => {
  const word = Array.isArray(item) ? item[0] : String(item)
  emit('wordClick', word, item, event)
}

const handleWordCloudHover = (item?: WordCloudItem, dimension?: any, event?: MouseEvent) => {
  if (item) {
    const word = Array.isArray(item) ? item[0] : String(item)
    emit('wordHover', word, item)
  } else {
    emit('wordHover', null, null)
  }
}

const refreshChart = async () => {
  await renderWordCloud()
}

const retryRender = async () => {
  await refreshChart()
}

const downloadChart = () => {
  if (!canvasRef.value) return

  try {
    // 创建下载链接
    const link = document.createElement('a')
    link.download = `词云图-${new Date().getTime()}.png`
    link.href = canvasRef.value.toDataURL('image/png')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    emit('download', canvasRef.value)
  } catch (error) {
    console.error('下载失败:', error)
  }
}

const toggleSettings = () => {
  showConfigPanel.value = !showConfigPanel.value
}

// 事件处理
const handleWordClick = (word: string, item: WordCloudItem, event: MouseEvent) => {
  console.log(`词语点击: ${word}`)
  emit('wordClick', word, item, event)
}

const handleWordHover = (word: string | null, item: WordCloudItem | null) => {
  emit('wordHover', word, item)
}

const handleZoom = (scale: number, event: WheelEvent) => {
  console.log(`缩放: ${scale}`)
}

const handleDrag = (deltaX: number, deltaY: number, event: MouseEvent) => {
  console.log(`拖拽: ${deltaX}, ${deltaY}`)
}

const handleInteractionConfigChange = (config: WordCloudInteractionConfig) => {
  Object.assign(interactionConfig, config)
}

const handleThemeConfigChange = (config: Partial<WordCloudThemeConfig>) => {
  Object.assign(themeConfig, config)
  nextTick(() => {
    refreshChart()
  })
}

const handleThemeChange = (themeName: string) => {
  const theme = getThemeByName(themeName)
  Object.assign(themeConfig, theme)
  emit('themeChange', themeName)
  nextTick(() => {
    refreshChart()
  })
}

const handleConfigReset = () => {
  const currentMode = getCurrentThemeMode()
  const defaultTheme = currentMode === 'dark' ? DARK_THEME_CONFIG : LIGHT_THEME_CONFIG
  Object.assign(themeConfig, defaultTheme)
  Object.assign(interactionConfig, createDefaultInteractionConfig())
  nextTick(() => {
    refreshChart()
  })
}

const handleConfigExport = () => {
  console.log('配置导出')
}

// Canvas事件监听器
const setupCanvasEventListeners = () => {
  if (!canvasRef.value) return

  const canvas = canvasRef.value

  // WordCloud生命周期事件
  canvas.addEventListener('wordcloudstart', () => {
    renderStartTime = Date.now()
    renderProgress.value = 0
  })

  canvas.addEventListener('wordclouddrawn', () => {
    const renderTime = Date.now() - renderStartTime
    isLoading.value = false

    // 更新调试信息
    debugInfo.value = {
      totalWords: wordCount.value,
      renderedWords: wordCount.value, // 简化处理
      maxWeight: Math.max(...props.words.map(item => Array.isArray(item) ? item[1] : 0)),
      minWeight: Math.min(...props.words.map(item => Array.isArray(item) ? item[1] : 0)),
      averageWeight: props.words.reduce((sum, item) => sum + (Array.isArray(item) ? item[1] : 0), 0) / wordCount.value,
      renderTime
    }

    emit('renderComplete', debugInfo.value)
  })

  canvas.addEventListener('wordcloudabort', () => {
    isLoading.value = false
    hasError.value = true
    errorMessage.value = '词云渲染被中断'
  })
}

// 主题监听
const watchThemeChanges = () => {
  // 监听系统主题变化
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const handleThemeChange = (e: MediaQueryListEvent) => {
    if (props.theme === 'auto') {
      const newTheme = e.matches ? DARK_THEME_CONFIG : LIGHT_THEME_CONFIG
      Object.assign(themeConfig, newTheme)
      nextTick(() => {
        refreshChart()
      })
    }
  }

  mediaQuery.addEventListener('change', handleThemeChange)

  // 返回清理函数
  return () => {
    mediaQuery.removeEventListener('change', handleThemeChange)
  }
}

// 初始化主题
const initializeTheme = () => {
  if (props.theme === 'auto') {
    const currentMode = getCurrentThemeMode()
    const defaultTheme = currentMode === 'dark' ? DARK_THEME_CONFIG : LIGHT_THEME_CONFIG
    Object.assign(themeConfig, defaultTheme)
  } else {
    const theme = getThemeByName(props.theme)
    Object.assign(themeConfig, theme)
  }
}

// 监听Props变化
watch(
  () => props.words,
  () => {
    nextTick(() => {
      if (!isEmpty.value) {
        refreshChart()
      }
    })
  },
  { deep: true }
)

watch(
  () => props.loading,
  (newLoading) => {
    isLoading.value = newLoading
  }
)

watch(
  () => props.error,
  (newError) => {
    hasError.value = !!newError
    errorMessage.value = newError || ''
  }
)

watch(
  [() => canvasWidth.value, () => canvasHeight.value],
  () => {
    nextTick(() => {
      if (!isEmpty.value && !isLoading.value) {
        refreshChart()
      }
    })
  }
)

// 生命周期
let themeCleanup: (() => void) | null = null

onMounted(() => {
  initializeTheme()
  themeCleanup = watchThemeChanges()

  nextTick(() => {
    setupCanvasEventListeners()
    setupResizeObserver()
    if (!isEmpty.value) {
      initializeWordCloud()
    }
  })
})

onUnmounted(() => {
  themeCleanup?.()
  cleanup()
})

// 暴露方法
defineExpose({
  refresh: refreshChart,
  download: downloadChart,
  clear: clearCanvas,
  stop: stopRendering,
  getCanvas: () => canvasRef.value,
  getDebugInfo: () => debugInfo.value,
  getRenderState: () => renderState
})
</script>

<style scoped>
.hot-word-cloud-chart {
  @apply relative bg-card border border-border rounded-lg overflow-hidden;
}

/* 工具栏 */
.cloud-toolbar {
  @apply flex items-center justify-between p-3 bg-card border-b border-border;
}

.toolbar-left {
  @apply flex items-center gap-3;
}

.chart-title {
  @apply text-lg font-semibold text-foreground;
}

.word-count {
  @apply text-sm text-muted-foreground;
}

.toolbar-right {
  @apply flex items-center gap-2;
}

/* 图表容器 */
.chart-container {
  @apply relative min-h-[400px] bg-background;
}

.word-cloud-canvas {
  @apply w-full h-auto;
}

/* 加载状态 */
.loading-overlay {
  @apply absolute inset-0 bg-background/80 backdrop-blur-sm z-20;
  @apply flex items-center justify-center;
}

.loading-content {
  @apply text-center space-y-4;
}

.loading-spinner {
  @apply w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto;
}

.loading-text {
  @apply text-sm text-muted-foreground;
}

.loading-progress {
  @apply space-y-2;
}

.progress-bar {
  @apply w-32 h-2 bg-secondary rounded-full overflow-hidden;
}

.progress-fill {
  @apply h-full bg-primary transition-all duration-300;
}

.progress-text {
  @apply text-xs text-muted-foreground;
}

/* 错误状态 */
.error-overlay {
  @apply absolute inset-0 bg-background z-20;
  @apply flex items-center justify-center;
}

.error-content {
  @apply text-center space-y-4;
}

.error-icon {
  @apply w-12 h-12 text-destructive mx-auto;
}

.error-message {
  @apply text-sm text-destructive font-medium;
}

/* 空状态 */
.empty-overlay {
  @apply absolute inset-0 bg-background z-10;
  @apply flex items-center justify-center;
}

.empty-content {
  @apply text-center space-y-3;
}

.empty-icon {
  @apply w-12 h-12 text-muted-foreground mx-auto;
}

.empty-message {
  @apply text-base font-medium text-muted-foreground;
}

.empty-description {
  @apply text-sm text-muted-foreground;
}

/* 配置面板 */
.config-panel-overlay {
  @apply absolute inset-0 bg-black/20 backdrop-blur-sm z-30;
  @apply flex items-center justify-center p-4;
}

.config-panel {
  @apply bg-card border border-border rounded-lg shadow-lg;
  @apply w-full max-w-md max-h-[80vh] overflow-hidden;
}

.panel-header {
  @apply flex items-center justify-between p-4 border-b border-border;
}

.panel-title {
  @apply text-lg font-semibold text-foreground;
}

.panel-content {
  @apply p-4 overflow-y-auto;
}

/* 调试信息 */
.debug-info {
  @apply mt-4 border-t border-border;
}

.debug-details {
  @apply p-3;
}

.debug-summary {
  @apply text-sm font-medium text-muted-foreground cursor-pointer;
}

.debug-content {
  @apply mt-2 space-y-1;
}

.debug-item {
  @apply flex justify-between text-xs;
}

.debug-label {
  @apply text-muted-foreground;
}

.debug-value {
  @apply text-foreground font-mono;
}

/* 状态指示器 */
.hot-word-cloud-chart.loading {
  @apply opacity-75;
}

.hot-word-cloud-chart.error {
  @apply border-destructive;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .cloud-toolbar {
    @apply flex-col gap-2 items-start;
  }

  .toolbar-left,
  .toolbar-right {
    @apply w-full justify-between;
  }

  .chart-title {
    @apply text-base;
  }

  .config-panel {
    @apply m-2;
  }
}

/* 动画 */
.word-cloud-canvas {
  @apply transition-opacity duration-300;
}

.hot-word-cloud-chart.loading .word-cloud-canvas {
  @apply opacity-50;
}

/* 按钮状态 */
.refresh-btn:disabled,
.download-btn:disabled {
  @apply opacity-50 cursor-not-allowed;
}

/* 深色模式适配 */
.dark .loading-overlay {
  @apply bg-background/90;
}

.dark .config-panel-overlay {
  @apply bg-black/40;
}
</style>