<template>
  <div class="border rounded-lg bg-white">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between p-4 bg-emerald-50 rounded-t-lg border-b">
      <h3 class="text-lg font-semibold text-emerald-800">查询理解管道调试</h3>
      <Button
        v-if="result"
        variant="outline"
        size="sm"
        @click="clearResult"
        class="text-xs"
      >
        清空结果
      </Button>
    </div>

    <!-- 查询输入区域 -->
    <div class="p-4 border-b bg-gray-50">
      <div class="flex gap-3">
        <Input
          v-model="query"
          placeholder="输入查询文本进行测试..."
          class="flex-1"
          @keyup.enter="executeQuery"
        />
        <Button
          @click="executeQuery"
          :disabled="loading || !query.trim()"
          class="bg-emerald-600 hover:bg-emerald-700 min-w-24"
        >
          <span v-if="loading">处理中...</span>
          <span v-else>执行查询</span>
        </Button>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-md">
        <div class="flex items-start gap-2">
          <span class="text-red-600 font-semibold">错误:</span>
          <span class="text-red-700 text-sm">{{ error }}</span>
        </div>
      </div>
    </div>

    <!-- 结果展示区域 -->
    <div v-if="result" class="p-4">
      <!-- 总览信息 -->
      <div class="mb-6 grid grid-cols-3 gap-4">
        <div class="p-3 border rounded-lg bg-emerald-50">
          <div class="text-xs text-gray-600 mb-1">原始查询</div>
          <div class="font-medium text-gray-900">{{ result.originalQuery }}</div>
        </div>
        <div class="p-3 border rounded-lg bg-blue-50">
          <div class="text-xs text-gray-600 mb-1">最终查询</div>
          <div class="font-medium text-gray-900">{{ result.currentQuery }}</div>
        </div>
        <div class="p-3 border rounded-lg bg-purple-50">
          <div class="text-xs text-gray-600 mb-1">总处理时间</div>
          <div class="font-medium text-gray-900">{{ result.totalProcessingTime }} ms</div>
        </div>
      </div>

      <!-- 查询转换过程 -->
      <div class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          查询转换过程
        </h4>
        <div class="space-y-2">
          <QueryStage
            v-if="result.normalizedQuery && result.normalizedQuery !== result.originalQuery"
            label="规范化查询"
            :value="result.normalizedQuery"
            color="blue"
          />
          <QueryStage
            v-if="result.correctedQuery && result.correctedQuery !== (result.normalizedQuery || result.originalQuery)"
            label="纠错后查询"
            :value="result.correctedQuery"
            color="green"
          />
          <QueryStage
            v-if="result.expandedQuery && result.expandedQuery !== (result.correctedQuery || result.normalizedQuery || result.originalQuery)"
            label="扩展后查询"
            :value="result.expandedQuery"
            color="purple"
          />
          <QueryStage
            v-if="result.rewrittenQuery && result.rewrittenQuery !== (result.expandedQuery || result.correctedQuery || result.normalizedQuery || result.originalQuery)"
            label="重写后查询"
            :value="result.rewrittenQuery"
            color="orange"
          />
          <div v-if="!hasQueryTransformation" class="text-sm text-gray-500 italic">
            查询未经过转换处理
          </div>
        </div>
      </div>

      <!-- 意图识别 -->
      <div v-if="result.intent" class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          意图识别
        </h4>
        <div class="flex items-center gap-4 p-3 border rounded-lg bg-gray-50">
          <div class="flex-1">
            <div class="text-sm text-gray-600">识别意图</div>
            <div class="font-medium text-gray-900 mt-1">{{ result.intent }}</div>
          </div>
          <div class="flex-1">
            <div class="text-sm text-gray-600">置信度</div>
            <div class="font-medium text-gray-900 mt-1">
              {{ result.intentConfidence != null ? (result.intentConfidence * 100).toFixed(1) + '%' : '未知' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 实体识别 -->
      <div v-if="result.entities && result.entities.length > 0" class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          实体识别 ({{ result.entities.length }})
        </h4>
        <div class="space-y-2">
          <div
            v-for="(entity, index) in result.entities"
            :key="index"
            class="p-3 border rounded-lg bg-gray-50"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <span class="font-medium text-gray-900">{{ entity.text }}</span>
                <span class="px-2 py-1 bg-emerald-100 text-emerald-800 text-xs rounded-full">
                  {{ entity.type }}
                </span>
                <span class="text-sm text-gray-600">
                  置信度: {{ (entity.confidence * 100).toFixed(1) }}%
                </span>
              </div>
              <div class="text-xs text-gray-500">
                位置: {{ entity.startPosition }} - {{ entity.endPosition }}
              </div>
            </div>
            <div v-if="entity.normalizedForm !== entity.text" class="mt-2 text-sm text-gray-600">
              标准形式: {{ entity.normalizedForm }}
            </div>
          </div>
        </div>
      </div>

      <!-- 同义词 -->
      <div v-if="result.synonyms && result.synonyms.length > 0" class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          同义词扩展 ({{ result.synonyms.length }})
        </h4>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="(synonym, index) in result.synonyms"
            :key="index"
            class="px-3 py-1.5 bg-blue-100 text-blue-800 rounded-full text-sm"
          >
            {{ synonym }}
          </span>
        </div>
      </div>

      <!-- 语义相关词 -->
      <div v-if="result.relatedTerms && result.relatedTerms.length > 0" class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          语义相关词 ({{ result.relatedTerms.length }})
        </h4>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="(term, index) in result.relatedTerms"
            :key="index"
            class="px-3 py-1.5 bg-purple-100 text-purple-800 rounded-full text-sm"
          >
            {{ term }}
          </span>
        </div>
      </div>

      <!-- 检测到的短语 -->
      <div v-if="result.detectedPhrases && result.detectedPhrases.length > 0" class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          检测短语 ({{ result.detectedPhrases.length }})
        </h4>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="(phrase, index) in result.detectedPhrases"
            :key="index"
            class="px-3 py-1.5 bg-green-100 text-green-800 rounded-full text-sm"
          >
            {{ phrase }}
          </span>
        </div>
      </div>

      <!-- 热门话题 -->
      <div v-if="result.hotTopics && result.hotTopics.length > 0" class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          关联热门话题 ({{ result.hotTopics.length }})
        </h4>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="(topic, index) in result.hotTopics"
            :key="index"
            class="px-3 py-1.5 bg-orange-100 text-orange-800 rounded-full text-sm font-medium"
          >
            🔥 {{ topic }}
          </span>
        </div>
      </div>

      <!-- 处理器耗时 -->
      <div class="mb-6">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          处理器耗时统计
        </h4>
        <div class="space-y-2">
          <div
            v-for="(time, processor) in result.processorTimings"
            :key="processor"
            class="flex items-center gap-3 p-2 border rounded bg-gray-50"
          >
            <div class="flex-1 text-sm text-gray-700">{{ processor }}</div>
            <div class="text-sm font-medium text-gray-900">{{ time }} ms</div>
            <div class="w-48 bg-gray-200 rounded-full h-2">
              <div
                class="bg-emerald-600 h-2 rounded-full transition-all"
                :style="{ width: `${(time / result.totalProcessingTime) * 100}%` }"
              ></div>
            </div>
            <div class="text-xs text-gray-500 w-12 text-right">
              {{ ((time / result.totalProcessingTime) * 100).toFixed(1) }}%
            </div>
          </div>
        </div>
      </div>

      <!-- 元数据 -->
      <div v-if="result.metadata && Object.keys(result.metadata).length > 0">
        <h4 class="font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <span class="w-1 h-5 bg-emerald-600 rounded"></span>
          元数据
        </h4>
        <div class="p-3 border rounded-lg bg-gray-50 font-mono text-xs overflow-auto max-h-60">
          <pre>{{ JSON.stringify(result.metadata, null, 2) }}</pre>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!result && !loading" class="p-12 text-center text-gray-500">
      <div class="text-lg mb-2">输入查询文本开始测试</div>
      <div class="text-sm">查看查询在管道中的处理过程和最终结果</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { understandQuery } from '@/api/queryUnderstanding'
import type { QueryUnderstandingResponse } from '@/api/queryUnderstanding'
import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import QueryStage from './QueryStage.vue'

// 状态
const query = ref('')
const loading = ref(false)
const error = ref('')
const result = ref<QueryUnderstandingResponse | null>(null)

// 计算属性：判断是否有查询转换
const hasQueryTransformation = computed(() => {
  if (!result.value) return false
  return (
    (result.value.normalizedQuery && result.value.normalizedQuery !== result.value.originalQuery) ||
    (result.value.correctedQuery && result.value.correctedQuery !== (result.value.normalizedQuery || result.value.originalQuery)) ||
    (result.value.expandedQuery && result.value.expandedQuery !== (result.value.correctedQuery || result.value.normalizedQuery || result.value.originalQuery)) ||
    (result.value.rewrittenQuery && result.value.rewrittenQuery !== (result.value.expandedQuery || result.value.correctedQuery || result.value.normalizedQuery || result.value.originalQuery))
  )
})

// 执行查询
async function executeQuery() {
  if (!query.value.trim()) return

  loading.value = true
  error.value = ''
  result.value = null

  try {
    const response = await understandQuery({ query: query.value.trim(), includeDetails: true })
    console.log('查询理解响应:', response)

    // 确保 currentQuery 有值（兼容后端的 finalQuery 字段）
    if (!response.currentQuery && response.finalQuery) {
      response.currentQuery = response.finalQuery
    }

    result.value = response
  } catch (err: any) {
    error.value = err.message || '查询执行失败'
    console.error('查询理解执行失败:', err)
  } finally {
    loading.value = false
  }
}

// 清空结果
function clearResult() {
  result.value = null
  error.value = ''
}
</script>

<style scoped>
/* 过渡动画 */
.transition-all {
  transition: all 0.3s ease;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgb(167 243 208);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgb(110 231 183);
}
</style>
