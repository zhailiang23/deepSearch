<template>
  <div class="simple-wordcloud-container">
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p class="loading-text">加载词云数据中...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <p class="error-text">{{ error }}</p>
      <button @click="retry" class="retry-button">重试</button>
    </div>

    <div v-else-if="!hasValidData" class="empty-state">
      <div class="empty-icon">📊</div>
      <p class="empty-text">暂无词云数据</p>
    </div>

    <div v-else class="wordcloud-wrapper">
      <canvas
        ref="canvasRef"
        class="wordcloud-canvas"
        :style="{ width: '100%', height: '100%' }"
      ></canvas>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

// ============= 接口定义 =============

interface WordItem {
  text: string
  weight: number
}

interface Props {
  words: WordItem[]
  loading?: boolean
  error?: string
  theme?: 'light' | 'dark'
  maxWords?: number
}

// ============= Props =============

const props = withDefaults(defineProps<Props>(), {
  words: () => [],
  loading: false,
  error: '',
  theme: 'light',
  maxWords: 50
})

// ============= Emits =============

const emit = defineEmits<{
  wordClick: [word: WordItem, event: Event]
  renderComplete: []
  renderError: [error: string]
}>()

// ============= 响应式状态 =============

const canvasRef = ref<HTMLCanvasElement>()
const isRendering = ref(false)
const renderError = ref('')

// ============= 计算属性 =============

const hasValidData = computed(() =>
  Array.isArray(props.words) && props.words.length > 0
)

const processedWords = computed(() => {
  if (!hasValidData.value) return []

  return props.words
    .slice(0, props.maxWords)
    .map(word => [word.text, word.weight])
    .filter(([text, weight]) => text && typeof weight === 'number' && weight > 0)
})

const themeConfig = computed(() => {
  if (props.theme === 'dark') {
    return {
      backgroundColor: '#1f2937',
      colors: ['#10b981', '#34d399', '#6ee7b7', '#a7f3d0', '#d1fae5']
    }
  }

  return {
    backgroundColor: '#ffffff',
    colors: ['#10b981', '#059669', '#047857', '#065f46', '#064e3b']
  }
})

// ============= 方法 =============

let wordcloudInstance: any = null

const loadWordCloudLibrary = async (): Promise<any> => {
  if (typeof window !== 'undefined' && (window as any).WordCloud) {
    return (window as any).WordCloud
  }

  try {
    const wordcloud = await import('wordcloud')
    const WordCloud = wordcloud.default || wordcloud
    ;(window as any).WordCloud = WordCloud
    return WordCloud
  } catch (error) {
    console.error('Failed to load wordcloud library:', error)
    throw new Error('词云库加载失败')
  }
}

const renderWordCloud = async () => {
  if (!canvasRef.value || !hasValidData.value || isRendering.value) {
    return
  }

  try {
    isRendering.value = true
    renderError.value = ''

    // 加载词云库
    const WordCloud = await loadWordCloudLibrary()

    if (!WordCloud) {
      throw new Error('词云库未正确加载')
    }

    // 等待DOM更新
    await nextTick()

    const canvas = canvasRef.value
    const container = canvas.parentElement

    if (!container) {
      throw new Error('画布容器未找到')
    }

    // 设置画布尺寸
    const rect = container.getBoundingClientRect()
    const width = Math.max(400, rect.width)
    const height = Math.max(300, rect.height)

    const dpr = window.devicePixelRatio || 1
    canvas.width = width * dpr
    canvas.height = height * dpr
    canvas.style.width = `${width}px`
    canvas.style.height = `${height}px`

    // 获取画布上下文并缩放
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      throw new Error('无法获取画布上下文')
    }

    ctx.scale(dpr, dpr)

    // 词云配置
    const options = {
      list: processedWords.value,
      gridSize: Math.round(8 * Math.min(width / 800, 1)), // 减小网格尺寸，让词语排列更紧密
      weightFactor: (size: number) => {
        const baseFactor = Math.min(width / 800, 1)
        return Math.pow(size, 0.7) * 2.5 * baseFactor // 增加权重因子，让词语更大更分散
      },
      fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      fontWeight: '600',
      color: () => {
        const colors = themeConfig.value.colors
        return colors[Math.floor(Math.random() * colors.length)]
      },
      backgroundColor: themeConfig.value.backgroundColor,
      rotateRatio: 0.5, // 增加旋转比例，增加布局多样性
      rotationSteps: 4, // 增加旋转步数
      minRotation: -Math.PI / 2, // 扩大旋转角度范围
      maxRotation: Math.PI / 2,
      shuffle: true, // 启用随机排列
      clearCanvas: true,
      shrinkToFit: false, // 禁用自动收缩，让词云保持原始大小
      origin: [width / 2, height / 2],

      // 事件处理
      click: (item: any, dimension: any, event: Event) => {
        if (item && item[0] && item[1]) {
          const word: WordItem = { text: item[0], weight: item[1] }
          emit('wordClick', word, event)
        }
      }
    }

    // 渲染词云
    WordCloud(canvas, options)

    // 渲染完成
    setTimeout(() => {
      isRendering.value = false
      emit('renderComplete')
    }, 100)

  } catch (error) {
    isRendering.value = false
    const errorMessage = error instanceof Error ? error.message : '词云渲染失败'
    renderError.value = errorMessage
    emit('renderError', errorMessage)
    console.error('WordCloud rendering error:', error)
  }
}

const retry = () => {
  renderError.value = ''
  renderWordCloud()
}

const handleResize = () => {
  if (!isRendering.value) {
    renderWordCloud()
  }
}

// ============= 生命周期和监听器 =============

// 监听数据变化
watch(
  () => [props.words, props.theme],
  () => {
    if (!props.loading && hasValidData.value) {
      nextTick(() => {
        renderWordCloud()
      })
    }
  },
  { deep: true }
)

// 监听窗口大小变化
let resizeTimeout: number | null = null
const debouncedResize = () => {
  if (resizeTimeout) clearTimeout(resizeTimeout)
  resizeTimeout = window.setTimeout(handleResize, 300)
}

onMounted(async () => {
  window.addEventListener('resize', debouncedResize)

  // 等待组件完全挂载后渲染
  await nextTick()
  if (!props.loading && hasValidData.value) {
    renderWordCloud()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', debouncedResize)
  if (resizeTimeout) {
    clearTimeout(resizeTimeout)
  }
})
</script>

<style scoped>
.simple-wordcloud-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
  border-radius: 8px;
  position: relative;
}

.wordcloud-wrapper {
  width: 100%;
  height: 100%;
  min-height: 300px;
  position: relative;
}

.wordcloud-canvas {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #6b7280;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top: 3px solid #10b981;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  font-size: 14px;
  margin: 0;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #ef4444;
}

.error-icon {
  font-size: 32px;
}

.error-text {
  font-size: 14px;
  margin: 0;
  text-align: center;
}

.retry-button {
  padding: 8px 16px;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.retry-button:hover {
  background: #059669;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 32px;
}

.empty-text {
  font-size: 14px;
  margin: 0;
}

/* 暗色主题 */
.simple-wordcloud-container[data-theme="dark"] {
  background: #1f2937;
}
</style>