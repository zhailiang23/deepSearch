<template>
  <div class="border rounded-lg bg-white">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between p-4 bg-emerald-50 rounded-t-lg border-b">
      <h3 class="text-lg font-semibold text-emerald-800">处理器配置管理</h3>
      <div class="flex items-center gap-2">
        <Button
          variant="outline"
          size="sm"
          @click="refreshConfig"
          :disabled="loading"
          class="text-xs"
        >
          {{ loading ? '刷新中...' : '刷新配置' }}
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

    <!-- 配置信息 -->
    <div v-if="config" class="p-4">
      <!-- 管道统计 -->
      <div class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          管道统计
        </h4>
        <div class="grid grid-cols-3 gap-4">
          <div class="p-4 border rounded-lg bg-blue-50">
            <div class="text-sm text-gray-600 mb-1">处理器总数</div>
            <div class="text-2xl font-bold text-blue-700">{{ config.processorCount }}</div>
          </div>
          <div class="p-4 border rounded-lg bg-green-50">
            <div class="text-sm text-gray-600 mb-1">启用处理器</div>
            <div class="text-2xl font-bold text-green-700">
              {{ config.processors.filter((p: any) => p.enabled).length }}
            </div>
          </div>
          <div class="p-4 border rounded-lg bg-orange-50">
            <div class="text-sm text-gray-600 mb-1">禁用处理器</div>
            <div class="text-2xl font-bold text-orange-700">
              {{ config.processors.filter((p: any) => !p.enabled).length }}
            </div>
          </div>
        </div>
      </div>

      <!-- 处理器列表 -->
      <div>
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          处理器列表 ({{ config.processors.length }})
        </h4>

        <!-- 过滤器 -->
        <div class="mb-4 flex items-center gap-3">
          <div class="flex items-center gap-2">
            <label class="text-sm text-gray-600">状态筛选:</label>
            <select
              v-model="statusFilter"
              class="px-3 py-1.5 border rounded-md text-sm"
            >
              <option value="all">全部</option>
              <option value="enabled">已启用</option>
              <option value="disabled">已禁用</option>
            </select>
          </div>

          <div class="flex items-center gap-2">
            <label class="text-sm text-gray-600">排序:</label>
            <select
              v-model="sortBy"
              class="px-3 py-1.5 border rounded-md text-sm"
            >
              <option value="priority">按优先级</option>
              <option value="name">按名称</option>
            </select>
          </div>
        </div>

        <!-- 处理器卡片 -->
        <div class="space-y-3">
          <div
            v-for="processor in filteredProcessors"
            :key="processor.name"
            class="p-4 border rounded-lg hover:bg-gray-50 transition-colors"
            :class="processor.enabled ? 'bg-white' : 'bg-gray-50 opacity-75'"
          >
            <div class="flex items-start justify-between">
              <!-- 处理器信息 -->
              <div class="flex-1">
                <div class="flex items-center gap-3 mb-2">
                  <h5 class="font-semibold text-gray-900">{{ processor.name }}</h5>
                  <span
                    class="px-2 py-0.5 rounded-full text-xs font-medium"
                    :class="processor.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-600'"
                  >
                    {{ processor.enabled ? '已启用' : '已禁用' }}
                  </span>
                  <span class="px-2 py-0.5 bg-blue-100 text-blue-800 rounded-full text-xs font-medium">
                    优先级: {{ processor.priority }}
                  </span>
                </div>

                <!-- 处理器描述 -->
                <div v-if="getProcessorDescription(processor.name)" class="text-sm text-gray-600 mb-3">
                  {{ getProcessorDescription(processor.name) }}
                </div>

                <!-- 性能指标（如果有） -->
                <div v-if="processorMetrics && processorMetrics[processor.name]" class="flex items-center gap-4 text-sm">
                  <div class="text-gray-600">
                    平均耗时: <span class="font-medium text-gray-900">
                      {{ processorMetrics[processor.name].averageTime.toFixed(2) }} ms
                    </span>
                  </div>
                  <div class="text-gray-600">
                    错误率: <span
                      class="font-medium"
                      :class="processorMetrics[processor.name].errorRate > 0 ? 'text-red-600' : 'text-gray-900'"
                    >
                      {{ (processorMetrics[processor.name].errorRate * 100).toFixed(2) }}%
                    </span>
                  </div>
                </div>
              </div>

              <!-- 操作按钮区域（暂时禁用，因为后端没有对应API） -->
              <div class="flex items-center gap-2 opacity-50 pointer-events-none" title="配置功能开发中">
                <Button
                  variant="outline"
                  size="sm"
                  class="text-xs"
                  disabled
                >
                  {{ processor.enabled ? '禁用' : '启用' }}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  class="text-xs"
                  disabled
                >
                  配置
                </Button>
              </div>
            </div>

            <!-- 处理器优先级可视化 -->
            <div class="mt-3">
              <div class="flex items-center gap-2">
                <div class="flex-1 bg-gray-200 rounded-full h-1.5">
                  <div
                    class="h-1.5 rounded-full transition-all"
                    :class="getPriorityColor(processor.priority)"
                    :style="{ width: `${(processor.priority / 100) * 100}%` }"
                  ></div>
                </div>
                <span class="text-xs text-gray-500">{{ processor.priority }}/100</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredProcessors.length === 0" class="py-8 text-center text-gray-500">
          <div class="text-sm">没有找到符合条件的处理器</div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && !config" class="p-12 text-center text-gray-500">
      <div class="text-lg mb-2">加载中...</div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && !config" class="p-12 text-center text-gray-500">
      <div class="text-lg mb-2">暂无配置数据</div>
      <div class="text-sm">请刷新页面重试</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getPipelineConfig, getProcessorMetrics } from '@/api/queryUnderstanding'
import type { ProcessorMetric } from '@/api/queryUnderstanding'
import Button from '@/components/ui/button/Button.vue'

// 状态
const loading = ref(false)
const error = ref('')
const config = ref<any>(null)
const processorMetrics = ref<Record<string, ProcessorMetric> | null>(null)
const statusFilter = ref<'all' | 'enabled' | 'disabled'>('all')
const sortBy = ref<'priority' | 'name'>('priority')

// 处理器描述映射
const processorDescriptions: Record<string, string> = {
  'NormalizationProcessor': '对查询文本进行标准化处理，包括去除多余空格、统一大小写等',
  'SpellCorrectionProcessor': '自动纠正查询中的拼写错误',
  'NamedEntityRecognitionProcessor': '识别查询中的命名实体（人名、地名、组织等）',
  'IntentRecognitionProcessor': '识别用户的查询意图（信息查询、命令、问题等）',
  'PinyinProcessor': '处理拼音查询，支持拼音搜索',
  'PhraseDetectionProcessor': '检测查询中的短语和关键词组',
  'SynonymExpansionProcessor': '扩展查询的同义词以提高召回率',
  'QueryExpansionProcessor': '根据用户历史和搜索热词扩展查询',
  'SemanticExpansionProcessor': '基于语义相似度扩展相关词汇',
  'HotTopicIntegrationProcessor': '关联当前热门话题，提升搜索相关性',
  'DisambiguationProcessor': '消除查询中的歧义词',
  'QueryRewriteProcessor': '重写查询以提高搜索质量',
  'QueryBuilderProcessor': '构建优化的Elasticsearch查询语句'
}

// 过滤后的处理器列表
const filteredProcessors = computed(() => {
  if (!config.value) return []

  let processors = [...config.value.processors]

  // 状态筛选
  if (statusFilter.value !== 'all') {
    processors = processors.filter((p: any) =>
      statusFilter.value === 'enabled' ? p.enabled : !p.enabled
    )
  }

  // 排序
  processors.sort((a: any, b: any) => {
    if (sortBy.value === 'priority') {
      return b.priority - a.priority
    } else {
      return a.name.localeCompare(b.name)
    }
  })

  return processors
})

// 刷新配置
async function refreshConfig() {
  loading.value = true
  error.value = ''

  try {
    const [configRes, metricsRes] = await Promise.all([
      getPipelineConfig(),
      getProcessorMetrics().catch(() => null)
    ])

    config.value = configRes
    processorMetrics.value = metricsRes
  } catch (err: any) {
    error.value = err.message || '获取配置失败'
    console.error('获取处理器配置失败:', err)
  } finally {
    loading.value = false
  }
}

// 获取处理器描述
function getProcessorDescription(name: string): string {
  return processorDescriptions[name] || ''
}

// 获取优先级颜色
function getPriorityColor(priority: number): string {
  if (priority >= 90) return 'bg-red-600'
  if (priority >= 70) return 'bg-orange-600'
  if (priority >= 50) return 'bg-yellow-600'
  if (priority >= 30) return 'bg-green-600'
  return 'bg-blue-600'
}

// 组件挂载时加载数据
onMounted(() => {
  refreshConfig()
})
</script>

<style scoped>
/* 过渡动画 */
.transition-colors {
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.transition-all {
  transition: all 0.3s ease;
}

/* 选择器样式 */
select {
  transition: border-color 0.2s ease;
}

select:focus {
  outline: none;
  border-color: rgb(16 185 129);
  box-shadow: 0 0 0 1px rgb(16 185 129 / 0.2);
}

/* 禁用状态 */
.pointer-events-none {
  cursor: not-allowed;
}
</style>
