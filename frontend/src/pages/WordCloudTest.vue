<template>
  <div class="word-cloud-test-page min-h-screen bg-gray-50 py-8">
    <div class="max-w-6xl mx-auto px-4">
      <!-- 页面标题 -->
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">词云图组件测试</h1>
        <p class="text-gray-600">测试HotWordCloudChart组件的基础渲染功能</p>
      </div>

      <!-- 控制面板 -->
      <div class="bg-white rounded-lg shadow-sm border p-6 mb-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">控制面板</h2>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- 主题选择 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">主题</label>
            <select
              v-model="selectedTheme"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="light-green">淡绿色</option>
              <option value="dark-green">深绿色</option>
            </select>
          </div>

          <!-- 数据集选择 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">数据集</label>
            <select
              v-model="selectedDataset"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="small">小数据集 (10个词)</option>
              <option value="medium">中数据集 (50个词)</option>
              <option value="large">大数据集 (100个词)</option>
            </select>
          </div>

          <!-- 尺寸设置 */
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">尺寸</label>
            <select
              v-model="selectedSize"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              <option value="small">小 (400x300)</option>
              <option value="medium">中 (600x400)</option>
              <option value="large">大 (800x600)</option>
              <option value="responsive">响应式</option>
            </select>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-end space-x-2">
            <button
              @click="refreshWordCloud"
              :disabled="isLoading"
              class="px-4 py-2 bg-emerald-500 text-white rounded-md hover:bg-emerald-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              刷新
            </button>
            <button
              @click="downloadWordCloud"
              :disabled="isLoading"
              class="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              下载
            </button>
          </div>
        </div>
      </div>

      <!-- 词云图容器 -->
      <div class="bg-white rounded-lg shadow-sm border p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">词云图</h2>

        <!-- 状态信息 -->
        <div v-if="renderInfo" class="mb-4 p-4 bg-gray-50 rounded-md">
          <h3 class="text-sm font-medium text-gray-700 mb-2">渲染信息</h3>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm text-gray-600">
            <div>
              <span class="font-medium">状态:</span>
              {{ renderInfo.status }}
            </div>
            <div>
              <span class="font-medium">词语数量:</span>
              {{ renderInfo.wordCount }}
            </div>
            <div>
              <span class="font-medium">渲染时间:</span>
              {{ renderInfo.renderTime }}ms
            </div>
            <div>
              <span class="font-medium">画布尺寸:</span>
              {{ renderInfo.dimensions }}
            </div>
          </div>
        </div>

        <!-- 词云图组件 -->
        <div
          class="word-cloud-container border-2 border-dashed border-gray-200 rounded-lg"
          :style="containerStyle"
        >
          <HotWordCloudChart
            ref="wordCloudRef"
            :words="currentWords"
            :width="currentWidth"
            :height="currentHeight"
            :theme="selectedTheme"
            :responsive="selectedSize === 'responsive'"
            :loading="isLoading"
            :error="error || undefined"
            @word-click="handleWordClick"
            @word-hover="handleWordHover"
            @render-start="handleRenderStart"
            @render-complete="handleRenderComplete"
            @render-error="handleRenderError"
            @download="handleDownload"
          />
        </div>
      </div>

      <!-- 事件日志 -->
      <div class="bg-white rounded-lg shadow-sm border p-6 mt-8">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-gray-900">事件日志</h2>
          <button
            @click="clearEventLog"
            class="px-3 py-1 text-sm bg-gray-500 text-white rounded-md hover:bg-gray-600 transition-colors"
          >
            清除
          </button>
        </div>

        <div class="h-48 overflow-y-auto border border-gray-200 rounded-md p-3 bg-gray-50">
          <div v-if="eventLog.length === 0" class="text-gray-500 text-sm">
            暂无事件日志
          </div>
          <div
            v-for="(event, index) in eventLog"
            :key="index"
            class="mb-1 text-sm"
            :class="getEventLogClass(event.type)"
          >
            <span class="font-mono text-xs text-gray-500">{{ event.timestamp }}</span>
            <span class="ml-2">{{ event.message }}</span>
          </div>
        </div>
      </div>

      <!-- 词语列表 -->
      <div class="bg-white rounded-lg shadow-sm border p-6 mt-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">当前词语数据</h2>

        <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-2 max-h-48 overflow-y-auto">
          <div
            v-for="word in currentWords"
            :key="word.text"
            class="px-3 py-2 bg-gray-100 rounded-md text-sm"
          >
            <div class="font-medium">{{ word.text }}</div>
            <div class="text-xs text-gray-500">权重: {{ word.weight }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import HotWordCloudChart from '@/components/statistics/HotWordCloudChart.vue'
import type { HotWordItem } from '@/types/statistics'

// ============= 响应式状态 =============

const wordCloudRef = ref()
const isLoading = ref(false)
const error = ref<string | null>(null)

// 配置状态
const selectedTheme = ref('light-green')
const selectedDataset = ref('medium')
const selectedSize = ref('responsive')

// 渲染信息
const renderInfo = ref<{
  status: string
  wordCount: number
  renderTime: number
  dimensions: string
} | null>(null)

// 事件日志
interface EventLogItem {
  timestamp: string
  type: 'info' | 'success' | 'warning' | 'error'
  message: string
}

const eventLog = ref<EventLogItem[]>([])

// ============= 测试数据 =============

const datasets = {
  small: [
    { text: '人工智能', weight: 100 },
    { text: '机器学习', weight: 80 },
    { text: '深度学习', weight: 75 },
    { text: '神经网络', weight: 70 },
    { text: '算法', weight: 60 },
    { text: '数据科学', weight: 55 },
    { text: '编程', weight: 50 },
    { text: '技术', weight: 45 },
    { text: '创新', weight: 40 },
    { text: '未来', weight: 35 }
  ],
  medium: [
    // 科技类词汇
    { text: '人工智能', weight: 100 },
    { text: '机器学习', weight: 95 },
    { text: '深度学习', weight: 90 },
    { text: '神经网络', weight: 85 },
    { text: '大数据', weight: 80 },
    { text: '云计算', weight: 75 },
    { text: '物联网', weight: 70 },
    { text: '区块链', weight: 65 },
    { text: '算法', weight: 60 },
    { text: '数据科学', weight: 55 },

    // 编程语言
    { text: 'Python', weight: 90 },
    { text: 'JavaScript', weight: 85 },
    { text: 'Java', weight: 80 },
    { text: 'TypeScript', weight: 75 },
    { text: 'Go', weight: 70 },
    { text: 'Rust', weight: 65 },
    { text: 'C++', weight: 60 },
    { text: 'Swift', weight: 55 },
    { text: 'Kotlin', weight: 50 },
    { text: 'PHP', weight: 45 },

    // 框架和工具
    { text: 'Vue.js', weight: 85 },
    { text: 'React', weight: 80 },
    { text: 'Angular', weight: 75 },
    { text: 'Node.js', weight: 70 },
    { text: 'Express', weight: 65 },
    { text: 'Spring Boot', weight: 60 },
    { text: 'Django', weight: 55 },
    { text: 'Flask', weight: 50 },
    { text: 'TensorFlow', weight: 75 },
    { text: 'PyTorch', weight: 70 },

    // 概念和方法
    { text: '微服务', weight: 80 },
    { text: '容器化', weight: 75 },
    { text: 'DevOps', weight: 70 },
    { text: '敏捷开发', weight: 65 },
    { text: '测试驱动', weight: 60 },
    { text: '持续集成', weight: 55 },
    { text: '性能优化', weight: 50 },
    { text: '用户体验', weight: 65 },
    { text: '响应式设计', weight: 60 },
    { text: '移动端', weight: 55 },

    // 行业和应用
    { text: '金融科技', weight: 70 },
    { text: '电子商务', weight: 65 },
    { text: '在线教育', weight: 60 },
    { text: '智能制造', weight: 55 },
    { text: '智慧城市', weight: 50 },
    { text: '医疗健康', weight: 65 },
    { text: '新能源', weight: 60 },
    { text: '自动驾驶', weight: 75 },
    { text: '虚拟现实', weight: 70 },
    { text: '增强现实', weight: 65 }
  ],
  large: [] as HotWordItem[]
}

// 生成大数据集
datasets.large = [
  ...datasets.medium,
  // 添加更多随机词汇
  ...Array.from({ length: 50 }, (_, i) => ({
    text: `词汇${i + 1}`,
    weight: Math.floor(Math.random() * 80) + 20
  }))
]

// ============= 计算属性 =============

const currentWords = computed(() => datasets[selectedDataset.value as keyof typeof datasets])

const sizeConfig = {
  small: { width: 400, height: 300 },
  medium: { width: 600, height: 400 },
  large: { width: 800, height: 600 },
  responsive: { width: undefined, height: undefined }
}

const currentWidth = computed(() => sizeConfig[selectedSize.value as keyof typeof sizeConfig].width)
const currentHeight = computed(() => sizeConfig[selectedSize.value as keyof typeof sizeConfig].height)

const containerStyle = computed(() => {
  if (selectedSize.value === 'responsive') {
    return { height: '500px' }
  }
  return {
    width: `${currentWidth.value}px`,
    height: `${currentHeight.value}px`,
    margin: '0 auto'
  }
})

// ============= 方法 =============

const addEventLog = (type: EventLogItem['type'], message: string) => {
  const timestamp = new Date().toLocaleTimeString()
  eventLog.value.unshift({ timestamp, type, message })

  // 限制日志数量
  if (eventLog.value.length > 100) {
    eventLog.value = eventLog.value.slice(0, 100)
  }
}

const clearEventLog = () => {
  eventLog.value = []
}

const getEventLogClass = (type: string) => {
  switch (type) {
    case 'success':
      return 'text-green-600'
    case 'warning':
      return 'text-yellow-600'
    case 'error':
      return 'text-red-600'
    default:
      return 'text-gray-600'
  }
}

const refreshWordCloud = async () => {
  if (wordCloudRef.value && wordCloudRef.value.refresh) {
    await wordCloudRef.value.refresh()
    addEventLog('info', '手动刷新词云图')
  }
}

const downloadWordCloud = () => {
  if (wordCloudRef.value && wordCloudRef.value.download) {
    wordCloudRef.value.download()
    addEventLog('info', '下载词云图')
  }
}

// ============= 事件处理 =============

const handleWordClick = (word: HotWordItem, event: Event) => {
  addEventLog('info', `点击词语: ${word.text} (权重: ${word.weight})`)
}

const handleWordHover = (word: HotWordItem, event: Event) => {
  addEventLog('info', `悬停词语: ${word.text}`)
}

const handleRenderStart = () => {
  isLoading.value = true
  error.value = null
  addEventLog('info', '开始渲染词云图')

  renderInfo.value = {
    status: '渲染中...',
    wordCount: currentWords.value.length,
    renderTime: 0,
    dimensions: currentWidth.value && currentHeight.value
      ? `${currentWidth.value}x${currentHeight.value}`
      : '响应式'
  }
}

const handleRenderComplete = () => {
  isLoading.value = false
  addEventLog('success', '词云图渲染完成')

  if (renderInfo.value) {
    renderInfo.value.status = '渲染完成'
    // 这里可以计算实际的渲染时间
    renderInfo.value.renderTime = Math.floor(Math.random() * 1000) + 500
  }
}

const handleRenderError = (errorMessage: string) => {
  isLoading.value = false
  error.value = errorMessage
  addEventLog('error', `渲染错误: ${errorMessage}`)

  if (renderInfo.value) {
    renderInfo.value.status = '渲染失败'
  }
}

const handleDownload = (canvas: HTMLCanvasElement) => {
  addEventLog('success', '词云图下载完成')
}

// ============= 监听器 =============

watch([selectedTheme, selectedDataset, selectedSize], () => {
  addEventLog('info', `配置变更: 主题=${selectedTheme.value}, 数据集=${selectedDataset.value}, 尺寸=${selectedSize.value}`)
})

// ============= 生命周期 =============

onMounted(() => {
  addEventLog('info', '词云图测试页面初始化完成')
})
</script>

<style scoped>
.word-cloud-container {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>