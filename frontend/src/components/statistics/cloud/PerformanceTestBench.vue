<template>
  <div class="performance-test-bench bg-white rounded-lg shadow-lg p-6">
    <div class="mb-6">
      <h3 class="text-xl font-bold text-gray-800 mb-2">词云性能测试台</h3>
      <p class="text-gray-600">测试不同配置下的词云渲染性能和内存使用</p>
    </div>

    <!-- 测试配置 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
      <!-- 数据量配置 -->
      <div class="bg-gray-50 p-4 rounded-lg">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          数据量: {{ testConfig.wordCount }}
        </label>
        <input
          v-model.number="testConfig.wordCount"
          type="range"
          min="50"
          max="2000"
          step="50"
          class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
        />
      </div>

      <!-- 容器尺寸 -->
      <div class="bg-gray-50 p-4 rounded-lg">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          容器尺寸
        </label>
        <select v-model="testConfig.containerSize" class="w-full p-2 border rounded">
          <option value="small">小 (400x300)</option>
          <option value="medium">中 (800x600)</option>
          <option value="large">大 (1200x800)</option>
          <option value="xlarge">超大 (1600x1000)</option>
        </select>
      </div>

      <!-- 性能模式 -->
      <div class="bg-gray-50 p-4 rounded-lg">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          性能模式
        </label>
        <select v-model="testConfig.performanceMode" class="w-full p-2 border rounded">
          <option value="performance">性能优先</option>
          <option value="balanced">平衡模式</option>
          <option value="quality">质量优先</option>
        </select>
      </div>
    </div>

    <!-- 测试按钮 -->
    <div class="flex flex-wrap gap-3 mb-6">
      <button
        @click="runSingleTest"
        :disabled="isRunning"
        class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50"
      >
        {{ isRunning ? '测试中...' : '单次测试' }}
      </button>

      <button
        @click="runBatchTest"
        :disabled="isRunning"
        class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 disabled:opacity-50"
      >
        批量测试
      </button>

      <button
        @click="runStressTest"
        :disabled="isRunning"
        class="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 disabled:opacity-50"
      >
        压力测试
      </button>

      <button
        @click="clearResults"
        class="px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600"
      >
        清除结果
      </button>
    </div>

    <!-- 测试进度 -->
    <div v-if="isRunning" class="mb-6">
      <div class="bg-gray-200 rounded-full h-3 mb-2">
        <div
          class="bg-blue-500 h-3 rounded-full transition-all duration-300"
          :style="{ width: `${testProgress}%` }"
        />
      </div>
      <p class="text-sm text-gray-600 text-center">{{ currentTestDescription }}</p>
    </div>

    <!-- 实时性能指标 -->
    <div v-if="currentMetrics" class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-blue-50 p-3 rounded-lg text-center">
        <div class="text-2xl font-bold text-blue-600">{{ currentMetrics.renderTime }}ms</div>
        <div class="text-sm text-blue-500">渲染时间</div>
      </div>
      <div class="bg-green-50 p-3 rounded-lg text-center">
        <div class="text-2xl font-bold text-green-600">{{ currentMetrics.fps }}</div>
        <div class="text-sm text-green-500">FPS</div>
      </div>
      <div class="bg-yellow-50 p-3 rounded-lg text-center">
        <div class="text-2xl font-bold text-yellow-600">{{ formatMemory(currentMetrics.memoryUsage) }}</div>
        <div class="text-sm text-yellow-500">内存使用</div>
      </div>
      <div class="bg-purple-50 p-3 rounded-lg text-center">
        <div class="text-2xl font-bold text-purple-600">{{ currentMetrics.wordsRendered }}</div>
        <div class="text-sm text-purple-500">词语数量</div>
      </div>
    </div>

    <!-- 词云测试容器 -->
    <div class="mb-6">
      <WordCloudContainer
        ref="wordCloudContainerRef"
        :words="testWords"
        :initial-width="containerDimensions.width"
        :initial-height="containerDimensions.height"
        :auto-optimize="true"
        :show-performance-panel="true"
        :performance-config="currentPerformanceConfig"
        @render-complete="handleRenderComplete"
        @render-error="handleRenderError"
        @performance-warning="handlePerformanceWarning"
        class="border-2 border-gray-300 rounded-lg"
      />
    </div>

    <!-- 测试结果 -->
    <div v-if="testResults.length > 0" class="space-y-4">
      <h4 class="text-lg font-semibold text-gray-800">测试结果</h4>

      <!-- 结果统计 -->
      <div class="bg-gray-50 p-4 rounded-lg">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <div class="text-sm text-gray-500">平均渲染时间</div>
            <div class="text-lg font-semibold">{{ averageMetrics.renderTime }}ms</div>
          </div>
          <div>
            <div class="text-sm text-gray-500">平均FPS</div>
            <div class="text-lg font-semibold">{{ averageMetrics.fps }}</div>
          </div>
          <div>
            <div class="text-sm text-gray-500">平均内存</div>
            <div class="text-lg font-semibold">{{ formatMemory(averageMetrics.memoryUsage) }}</div>
          </div>
          <div>
            <div class="text-sm text-gray-500">成功率</div>
            <div class="text-lg font-semibold">{{ successRate }}%</div>
          </div>
        </div>
      </div>

      <!-- 详细结果表格 -->
      <div class="overflow-x-auto">
        <table class="min-w-full bg-white border border-gray-200 rounded-lg">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">测试</th>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">词语数</th>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">渲染时间</th>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">FPS</th>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">内存</th>
              <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="(result, index) in testResults" :key="index" class="hover:bg-gray-50">
              <td class="px-4 py-2 text-sm">{{ result.testName }}</td>
              <td class="px-4 py-2 text-sm">{{ result.wordCount }}</td>
              <td class="px-4 py-2 text-sm">{{ result.renderTime }}ms</td>
              <td class="px-4 py-2 text-sm">{{ result.fps }}</td>
              <td class="px-4 py-2 text-sm">{{ formatMemory(result.memoryUsage) }}</td>
              <td class="px-4 py-2 text-sm">
                <span
                  :class="{
                    'text-green-600': result.success,
                    'text-red-600': !result.success
                  }"
                >
                  {{ result.success ? '成功' : '失败' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 性能建议 -->
      <div v-if="performanceRecommendations.length > 0" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <h5 class="font-medium text-yellow-800 mb-2">性能优化建议</h5>
        <ul class="list-disc list-inside space-y-1">
          <li v-for="(recommendation, index) in performanceRecommendations" :key="index" class="text-yellow-700">
            {{ recommendation }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import WordCloudContainer from './WordCloudContainer.vue'
import { generateTestData } from '@/utils/testDataGenerator'
import type { HotWordItem, WordCloudPerformanceConfig } from '@/types/statistics'

// ============= 类型定义 =============

interface TestConfig {
  wordCount: number
  containerSize: 'small' | 'medium' | 'large' | 'xlarge'
  performanceMode: 'performance' | 'balanced' | 'quality'
}

interface TestResult {
  testName: string
  wordCount: number
  renderTime: number
  fps: number
  memoryUsage: number
  success: boolean
  timestamp: number
}

interface CurrentMetrics {
  renderTime: number
  fps: number
  memoryUsage: number
  wordsRendered: number
}

// ============= 状态管理 =============

const wordCloudContainerRef = ref<InstanceType<typeof WordCloudContainer>>()

/** 测试配置 */
const testConfig = reactive<TestConfig>({
  wordCount: 100,
  containerSize: 'medium',
  performanceMode: 'balanced'
})

/** 测试状态 */
const isRunning = ref(false)
const testProgress = ref(0)
const currentTestDescription = ref('')

/** 测试数据 */
const testWords = ref<HotWordItem[]>([])

/** 当前性能指标 */
const currentMetrics = ref<CurrentMetrics | null>(null)

/** 测试结果 */
const testResults = ref<TestResult[]>([])

/** 性能建议 */
const performanceRecommendations = ref<string[]>([])

// ============= 计算属性 =============

/** 容器尺寸 */
const containerDimensions = computed(() => {
  switch (testConfig.containerSize) {
    case 'small': return { width: 400, height: 300 }
    case 'medium': return { width: 800, height: 600 }
    case 'large': return { width: 1200, height: 800 }
    case 'xlarge': return { width: 1600, height: 1000 }
    default: return { width: 800, height: 600 }
  }
})

/** 性能配置 */
const currentPerformanceConfig = computed((): Partial<WordCloudPerformanceConfig> => {
  switch (testConfig.performanceMode) {
    case 'performance':
      return {
        batchSize: 30,
        renderDelay: 5,
        maxRenderTime: 3000,
        debounceDelay: 200
      }
    case 'quality':
      return {
        batchSize: 100,
        renderDelay: 20,
        maxRenderTime: 15000,
        debounceDelay: 500
      }
    case 'balanced':
    default:
      return {
        batchSize: 50,
        renderDelay: 10,
        maxRenderTime: 8000,
        debounceDelay: 300
      }
  }
})

/** 平均性能指标 */
const averageMetrics = computed(() => {
  if (testResults.value.length === 0) {
    return { renderTime: 0, fps: 0, memoryUsage: 0 }
  }

  const successfulResults = testResults.value.filter(r => r.success)
  if (successfulResults.length === 0) {
    return { renderTime: 0, fps: 0, memoryUsage: 0 }
  }

  const total = successfulResults.reduce(
    (acc, result) => ({
      renderTime: acc.renderTime + result.renderTime,
      fps: acc.fps + result.fps,
      memoryUsage: acc.memoryUsage + result.memoryUsage
    }),
    { renderTime: 0, fps: 0, memoryUsage: 0 }
  )

  return {
    renderTime: Math.round(total.renderTime / successfulResults.length),
    fps: Math.round(total.fps / successfulResults.length),
    memoryUsage: Math.round(total.memoryUsage / successfulResults.length)
  }
})

/** 成功率 */
const successRate = computed(() => {
  if (testResults.value.length === 0) return 0
  const successCount = testResults.value.filter(r => r.success).length
  return Math.round((successCount / testResults.value.length) * 100)
})

// ============= 方法 =============

/** 生成测试数据 */
const generateTestWords = (count: number): HotWordItem[] => {
  return generateTestData(count)
}

/** 格式化内存显示 */
const formatMemory = (bytes: number): string => {
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(1)}MB`
}

/** 单次测试 */
const runSingleTest = async () => {
  if (isRunning.value) return

  isRunning.value = true
  testProgress.value = 0
  currentTestDescription.value = '准备测试数据...'

  try {
    // 生成测试数据
    testWords.value = generateTestWords(testConfig.wordCount)
    testProgress.value = 20

    currentTestDescription.value = '开始渲染测试...'

    // 获取性能指标
    const startTime = performance.now()
    const startMemory = getMemoryUsage()

    await new Promise(resolve => setTimeout(resolve, 100))
    testProgress.value = 50

    // 模拟渲染完成
    const endTime = performance.now()
    const endMemory = getMemoryUsage()

    const renderTime = Math.round(endTime - startTime)
    const memoryUsage = endMemory - startMemory

    currentMetrics.value = {
      renderTime,
      fps: renderTime > 0 ? Math.round(1000 / renderTime * 60) : 60,
      memoryUsage,
      wordsRendered: testWords.value.length
    }

    testProgress.value = 100
    currentTestDescription.value = '测试完成'

    // 添加到结果
    testResults.value.push({
      testName: `单次测试 #${testResults.value.length + 1}`,
      wordCount: testConfig.wordCount,
      renderTime,
      fps: currentMetrics.value.fps,
      memoryUsage,
      success: renderTime < 10000,
      timestamp: Date.now()
    })

    // 生成建议
    generateRecommendations()

  } catch (error) {
    console.error('测试失败:', error)
  } finally {
    isRunning.value = false
    testProgress.value = 0
  }
}

/** 批量测试 */
const runBatchTest = async () => {
  if (isRunning.value) return

  isRunning.value = true
  const testConfigs = [
    { wordCount: 50, size: 'small' },
    { wordCount: 100, size: 'medium' },
    { wordCount: 200, size: 'medium' },
    { wordCount: 500, size: 'large' },
    { wordCount: 1000, size: 'large' }
  ]

  for (let i = 0; i < testConfigs.length; i++) {
    const config = testConfigs[i]
    testProgress.value = (i / testConfigs.length) * 100
    currentTestDescription.value = `批量测试 ${i + 1}/${testConfigs.length}: ${config.wordCount} 词语`

    // 设置测试配置
    testConfig.wordCount = config.wordCount
    testConfig.containerSize = config.size as any

    // 运行测试
    await runTestIteration(`批量测试 #${i + 1}`, config.wordCount)

    // 等待间隔
    await new Promise(resolve => setTimeout(resolve, 200))
  }

  isRunning.value = false
  testProgress.value = 0
  generateRecommendations()
}

/** 压力测试 */
const runStressTest = async () => {
  if (isRunning.value) return

  isRunning.value = true
  const stressConfigs = [
    { wordCount: 1000, iterations: 3 },
    { wordCount: 1500, iterations: 2 },
    { wordCount: 2000, iterations: 1 }
  ]

  let totalIterations = 0
  let currentIteration = 0

  stressConfigs.forEach(config => {
    totalIterations += config.iterations
  })

  for (const config of stressConfigs) {
    testConfig.wordCount = config.wordCount
    testConfig.containerSize = 'xlarge'

    for (let i = 0; i < config.iterations; i++) {
      currentIteration++
      testProgress.value = (currentIteration / totalIterations) * 100
      currentTestDescription.value = `压力测试: ${config.wordCount} 词语 (${i + 1}/${config.iterations})`

      await runTestIteration(`压力测试 ${config.wordCount}词 #${i + 1}`, config.wordCount)

      // 强制垃圾回收
      if ((window as any).gc) {
        (window as any).gc()
      }

      await new Promise(resolve => setTimeout(resolve, 500))
    }
  }

  isRunning.value = false
  testProgress.value = 0
  generateRecommendations()
}

/** 运行单次测试迭代 */
const runTestIteration = async (testName: string, wordCount: number) => {
  testWords.value = generateTestWords(wordCount)

  const startTime = performance.now()
  const startMemory = getMemoryUsage()

  // 模拟渲染时间
  await new Promise(resolve => setTimeout(resolve, Math.random() * 200 + 50))

  const endTime = performance.now()
  const endMemory = getMemoryUsage()

  const renderTime = Math.round(endTime - startTime)
  const memoryUsage = Math.max(0, endMemory - startMemory)

  testResults.value.push({
    testName,
    wordCount,
    renderTime,
    fps: renderTime > 0 ? Math.round(1000 / renderTime * 60) : 60,
    memoryUsage,
    success: renderTime < 10000 && memoryUsage < 100 * 1024 * 1024,
    timestamp: Date.now()
  })
}

/** 获取内存使用量 */
const getMemoryUsage = (): number => {
  const memory = (performance as any).memory
  return memory ? memory.usedJSHeapSize : 0
}

/** 生成性能建议 */
const generateRecommendations = () => {
  const recommendations: string[] = []
  const avgMetrics = averageMetrics.value

  if (avgMetrics.renderTime > 1000) {
    recommendations.push('渲染时间较长，建议减少词语数量或启用性能模式')
  }

  if (avgMetrics.fps < 30) {
    recommendations.push('帧率较低，建议优化渲染配置或使用更小的容器')
  }

  if (avgMetrics.memoryUsage > 50 * 1024 * 1024) {
    recommendations.push('内存使用较高，建议启用内存优化或增加清理频率')
  }

  if (successRate.value < 90) {
    recommendations.push('成功率较低，建议检查渲染配置或降低数据复杂度')
  }

  const largeDataResults = testResults.value.filter(r => r.wordCount > 500)
  if (largeDataResults.length > 0) {
    const avgLargeDataTime = largeDataResults.reduce((sum, r) => sum + r.renderTime, 0) / largeDataResults.length
    if (avgLargeDataTime > 2000) {
      recommendations.push('大数据量渲染较慢，建议启用虚拟化或分批渲染')
    }
  }

  performanceRecommendations.value = recommendations
}

/** 清除结果 */
const clearResults = () => {
  testResults.value = []
  performanceRecommendations.value = []
  currentMetrics.value = null
}

// ============= 事件处理 =============

const handleRenderComplete = () => {
  console.log('Render completed')
}

const handleRenderError = (error: string) => {
  console.error('Render error:', error)
}

const handlePerformanceWarning = (level: string, metrics: any) => {
  console.warn('Performance warning:', level, metrics)
}

// ============= 生命周期 =============

onMounted(() => {
  // 初始化测试数据
  testWords.value = generateTestWords(testConfig.wordCount)
})
</script>

<style scoped>
.performance-test-bench {
  min-height: 600px;
}

/* 响应式表格 */
@media (max-width: 768px) {
  .overflow-x-auto table {
    font-size: 0.875rem;
  }

  .overflow-x-auto th,
  .overflow-x-auto td {
    padding: 0.5rem 0.25rem;
  }
}
</style>