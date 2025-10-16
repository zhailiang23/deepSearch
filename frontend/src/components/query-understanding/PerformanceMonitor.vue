<template>
  <div class="border rounded-lg bg-white">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between p-4 bg-emerald-50 rounded-t-lg border-b">
      <h3 class="text-lg font-semibold text-emerald-800">性能监控</h3>
      <div class="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          @click="refreshMetrics"
          :disabled="loading"
          class="text-xs"
        >
          {{ loading ? '刷新中...' : '刷新数据' }}
        </Button>
        <Button
          variant="outline"
          size="sm"
          @click="showResetDialog = true"
          class="text-xs text-red-600 border-red-300 hover:bg-red-50"
        >
          重置指标
        </Button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="p-4 bg-red-50 border-b">
      <div class="flex items-start gap-2">
        <span class="text-red-600 font-semibold">错误:</span>
        <span class="text-red-700 text-sm">{{ error }}</span>
      </div>
    </div>

    <!-- 指标摘要 -->
    <div v-if="summary" class="p-4">
      <!-- 核心指标卡片 -->
      <div class="grid grid-cols-4 gap-4 mb-6">
        <MetricCard
          label="总查询数"
          :value="summary.totalQueries"
          icon="📊"
          color="blue"
        />
        <MetricCard
          label="缓存命中率"
          :value="`${(summary.cacheHitRate * 100).toFixed(1)}%`"
          icon="⚡"
          color="green"
          :subtitle="`${summary.cacheHits} / ${summary.cacheHits + summary.cacheMisses}`"
        />
        <MetricCard
          label="平均处理时间"
          :value="`${summary.averagePipelineTime.toFixed(2)} ms`"
          icon="⏱️"
          color="purple"
        />
        <MetricCard
          label="错误率"
          :value="`${summary.totalQueries > 0 ? ((summary.errorQueries / summary.totalQueries) * 100).toFixed(1) : 0}%`"
          icon="⚠️"
          color="red"
          :subtitle="`${summary.errorQueries} 个错误`"
        />
      </div>

      <!-- 查询类型分布 -->
      <div class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          查询类型分布
        </h4>
        <div class="grid grid-cols-3 gap-4">
          <div class="p-4 border rounded-lg bg-blue-50">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm text-gray-600">简单查询</span>
              <span class="text-lg font-semibold text-blue-700">{{ summary.simpleQueries }}</span>
            </div>
            <div class="w-full bg-blue-200 rounded-full h-2">
              <div
                class="bg-blue-600 h-2 rounded-full transition-all"
                :style="{ width: `${getPercentage(summary.simpleQueries, summary.totalQueries)}%` }"
              ></div>
            </div>
          </div>
          <div class="p-4 border rounded-lg bg-purple-50">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm text-gray-600">复杂查询</span>
              <span class="text-lg font-semibold text-purple-700">{{ summary.complexQueries }}</span>
            </div>
            <div class="w-full bg-purple-200 rounded-full h-2">
              <div
                class="bg-purple-600 h-2 rounded-full transition-all"
                :style="{ width: `${getPercentage(summary.complexQueries, summary.totalQueries)}%` }"
              ></div>
            </div>
          </div>
          <div class="p-4 border rounded-lg bg-orange-50">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm text-gray-600">超时查询</span>
              <span class="text-lg font-semibold text-orange-700">{{ summary.timeoutQueries }}</span>
            </div>
            <div class="w-full bg-orange-200 rounded-full h-2">
              <div
                class="bg-orange-600 h-2 rounded-full transition-all"
                :style="{ width: `${getPercentage(summary.timeoutQueries, summary.totalQueries)}%` }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 处理器性能 -->
      <div v-if="processors && Object.keys(processors).length > 0" class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          处理器性能指标
        </h4>
        <div class="space-y-2">
          <div
            v-for="(processor, name) in sortedProcessors"
            :key="name"
            class="p-3 border rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors"
          >
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-3">
                <span class="font-medium text-gray-900">{{ processor.name }}</span>
                <span
                  class="px-2 py-0.5 rounded-full text-xs"
                  :class="processor.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-600'"
                >
                  {{ processor.enabled ? '启用' : '禁用' }}
                </span>
                <span class="text-xs text-gray-500">
                  优先级: {{ processor.priority }}
                </span>
              </div>
              <div class="flex items-center gap-4 text-sm">
                <div class="text-gray-600">
                  平均耗时: <span class="font-medium text-gray-900">{{ processor.averageTime.toFixed(2) }} ms</span>
                </div>
                <div class="text-gray-600">
                  错误率: <span class="font-medium" :class="processor.errorRate > 0 ? 'text-red-600' : 'text-gray-900'">
                    {{ (processor.errorRate * 100).toFixed(2) }}%
                  </span>
                </div>
              </div>
            </div>
            <!-- 性能条 -->
            <div class="flex items-center gap-2">
              <div class="flex-1 bg-gray-200 rounded-full h-2">
                <div
                  class="h-2 rounded-full transition-all"
                  :class="getPerformanceBarColor(processor.averageTime)"
                  :style="{ width: `${getProcessorTimePercentage(processor.averageTime)}%` }"
                ></div>
              </div>
              <span class="text-xs text-gray-500 w-12 text-right">
                {{ getProcessorTimePercentage(processor.averageTime).toFixed(0) }}%
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 管道配置 -->
      <div v-if="pipelineConfig">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          管道配置
        </h4>
        <div class="p-4 border rounded-lg bg-gray-50">
          <div class="flex items-center gap-6 text-sm">
            <div>
              <span class="text-gray-600">处理器总数:</span>
              <span class="font-medium text-gray-900 ml-2">{{ pipelineConfig.processorCount }}</span>
            </div>
            <div>
              <span class="text-gray-600">启用处理器:</span>
              <span class="font-medium text-gray-900 ml-2">
                {{ pipelineConfig.processors.filter((p: any) => p.enabled).length }}
              </span>
            </div>
            <div>
              <span class="text-gray-600">最后查询时间:</span>
              <span class="font-medium text-gray-900 ml-2">{{ formatTime(summary.lastQueryTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && !summary" class="p-12 text-center text-gray-500">
      <div class="text-lg mb-2">加载中...</div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && !summary" class="p-12 text-center text-gray-500">
      <div class="text-lg mb-2">暂无性能数据</div>
      <div class="text-sm">执行查询后会显示性能指标</div>
    </div>

    <!-- 重置确认对话框 -->
    <div v-if="showResetDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-4 w-96 max-w-full mx-4">
        <h3 class="text-lg font-semibold mb-4">确认重置</h3>
        <p class="text-sm text-gray-700 mb-4">
          确定要重置所有性能指标吗？此操作不可撤销。
        </p>
        <div class="flex justify-end gap-2">
          <Button
            variant="outline"
            @click="showResetDialog = false"
            class="text-sm"
          >
            取消
          </Button>
          <Button
            @click="resetMetrics"
            class="text-sm bg-red-600 hover:bg-red-700"
          >
            重置
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMetricsSummary, getProcessorMetrics, getPipelineConfig, resetMetrics as resetMetricsApi } from '@/api/queryUnderstanding'
import type { MetricsSummary, ProcessorMetric } from '@/api/queryUnderstanding'
import Button from '@/components/ui/button/Button.vue'
import MetricCard from './MetricCard.vue'

// 状态
const loading = ref(false)
const error = ref('')
const summary = ref<MetricsSummary | null>(null)
const processors = ref<Record<string, ProcessorMetric> | null>(null)
const pipelineConfig = ref<any>(null)
const showResetDialog = ref(false)

// 排序后的处理器列表
const sortedProcessors = computed(() => {
  if (!processors.value) return []
  return Object.entries(processors.value)
    .map(([processorName, processor]) => ({ ...processor, name: processorName }))
    .sort((a, b) => b.priority - a.priority)
})

// 获取最大处理时间（用于计算百分比）
const maxProcessorTime = computed(() => {
  if (!processors.value) return 1
  const times = Object.values(processors.value).map(p => p.averageTime)
  return Math.max(...times, 1)
})

// 刷新指标
async function refreshMetrics() {
  loading.value = true
  error.value = ''

  try {
    const [summaryRes, processorsRes, configRes] = await Promise.all([
      getMetricsSummary(),
      getProcessorMetrics(),
      getPipelineConfig()
    ])

    summary.value = summaryRes
    processors.value = processorsRes
    pipelineConfig.value = configRes
  } catch (err: any) {
    error.value = err.message || '获取指标失败'
    console.error('获取性能指标失败:', err)
  } finally {
    loading.value = false
  }
}

// 重置指标
async function resetMetrics() {
  showResetDialog.value = false
  loading.value = true
  error.value = ''

  try {
    await resetMetricsApi()
    await refreshMetrics()
  } catch (err: any) {
    error.value = err.message || '重置指标失败'
    console.error('重置指标失败:', err)
  } finally {
    loading.value = false
  }
}

// 计算百分比
function getPercentage(value: number, total: number): number {
  return total > 0 ? (value / total) * 100 : 0
}

// 获取处理器时间百分比
function getProcessorTimePercentage(time: number): number {
  return (time / maxProcessorTime.value) * 100
}

// 获取性能条颜色
function getPerformanceBarColor(time: number): string {
  if (time < 10) return 'bg-green-600'
  if (time < 50) return 'bg-yellow-600'
  if (time < 100) return 'bg-orange-600'
  return 'bg-red-600'
}

// 格式化时间
function formatTime(timestamp: number): string {
  try {
    const date = new Date(timestamp)
    return date.toLocaleString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return '-'
  }
}

// 组件挂载时加载数据
onMounted(() => {
  refreshMetrics()
})
</script>

<style scoped>
/* 过渡动画 */
.transition-all {
  transition: all 0.3s ease;
}

.transition-colors {
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

/* 对话框动画 */
.fixed.inset-0 {
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.fixed.inset-0 > div {
  animation: slideIn 0.2s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>
