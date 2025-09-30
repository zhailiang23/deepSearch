<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900">关键词不匹配度Top10</h1>
      <div class="flex items-center space-x-4">
        <div class="flex items-center space-x-2">
          <span class="text-sm text-gray-500">数据更新时间：</span>
          <span class="text-sm font-medium text-gray-900">{{ lastUpdateTime }}</span>
        </div>
        <button
          @click="refreshData"
          :disabled="loading"
          class="px-4 py-2 bg-emerald-600 text-white rounded-md hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
        >
          <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          {{ loading ? '加载中...' : '刷新数据' }}
        </button>
      </div>
    </div>

    <!-- 时间范围选择 -->
    <div class="bg-white rounded-lg shadow p-4">
      <div class="flex items-center space-x-4">
        <label class="text-sm font-medium text-gray-700">时间范围：</label>
        <div class="flex space-x-2">
          <button
            v-for="option in timeRangeOptions"
            :key="option.value"
            @click="selectTimeRange(option.value)"
            :class="[
              'px-4 py-2 rounded-md text-sm font-medium transition-colors',
              filters.timeRange === option.value
                ? 'bg-emerald-600 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 统计概览 -->
    <div v-if="statistics" class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
              <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14"></path>
              </svg>
            </div>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-500">总关键词数</p>
            <p class="text-2xl font-semibold text-gray-900">{{ statistics.totalKeywords }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-yellow-100 rounded-full flex items-center justify-center">
              <svg class="w-5 h-5 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </div>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-500">平均不匹配率</p>
            <p class="text-2xl font-semibold text-gray-900">{{ formatPercentage(statistics.avgMismatchRate) }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
              <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"></path>
              </svg>
            </div>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-500">最高不匹配率</p>
            <p class="text-2xl font-semibold text-gray-900">{{ formatPercentage(statistics.maxMismatchRate) }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center">
              <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"></path>
              </svg>
            </div>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-500">最低不匹配率</p>
            <p class="text-2xl font-semibold text-gray-900">{{ formatPercentage(statistics.minMismatchRate) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Top10 关键词不匹配度排行榜 -->
    <div class="bg-white rounded-lg shadow">
      
      <div class="p-6">
        <div v-if="loading" class="flex justify-center items-center py-12">
          <svg class="animate-spin h-8 w-8 text-emerald-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
        </div>

        <div v-else-if="rankingData.length === 0" class="text-center py-12">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
          </svg>
          <p class="mt-2 text-sm text-gray-500">暂无数据</p>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">排名</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">关键词</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">不匹配率</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">出现次数</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">不匹配次数</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">最后搜索时间</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="item in rankingData" :key="item.keyword" class="hover:bg-gray-50 transition-colors">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center">
                    <span class="inline-flex items-center justify-center w-8 h-8 rounded-full text-sm font-bold"
                          :class="getRankClass(item.rank)">
                      {{ item.rank }}
                    </span>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-gray-900">{{ item.keyword }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium"
                        :class="getMismatchRateClass(item.mismatchRate)">
                    {{ formatPercentage(item.mismatchRate) }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm text-gray-900">{{ item.searchCount }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm text-gray-900">{{ item.mismatchCount }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ item.lastSearchTime || '-' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import MismatchKeywordApi from '@/services/mismatchKeywordApi'
import type { MismatchKeywordRanking, StatisticsInfo } from '@/types/mismatchKeyword'

// 响应式数据
const loading = ref(false)
const lastUpdateTime = ref('')
const rankingData = ref<MismatchKeywordRanking[]>([])
const statistics = ref<StatisticsInfo | null>(null)

const filters = reactive({
  timeRange: '7d'
})

const timeRangeOptions = [
  { label: '最近7天', value: '7d' },
  { label: '最近30天', value: '30d' },
  { label: '最近90天', value: '90d' }
]

// 方法
const loadRankingData = async () => {
  loading.value = true

  try {
    const response = await MismatchKeywordApi.getRanking({
      timeRange: filters.timeRange,
      page: 1,
      size: 10  // 只获取Top 10
    })

    rankingData.value = response.ranking

    if (response.statistics) {
      statistics.value = response.statistics
    }

    if (response.queryTime) {
      lastUpdateTime.value = response.queryTime
    }
  } catch (error) {
    console.error('加载排行榜数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const stats = await MismatchKeywordApi.getStatistics(filters.timeRange)
    statistics.value = stats
    if (stats.lastUpdated) {
      lastUpdateTime.value = stats.lastUpdated
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const selectTimeRange = (timeRange: string) => {
  filters.timeRange = timeRange
  refreshData()
}

const refreshData = async () => {
  await Promise.all([
    loadRankingData(),
    loadStatistics()
  ])
}

const formatPercentage = (value: number): string => {
  return `${(value * 100).toFixed(1)}%`
}

const getRankClass = (rank: number): string => {
  if (rank === 1) return 'bg-yellow-400 text-white'
  if (rank === 2) return 'bg-gray-300 text-gray-800'
  if (rank === 3) return 'bg-orange-300 text-orange-900'
  return 'bg-blue-100 text-blue-800'
}

const getMismatchRateClass = (rate: number): string => {
  if (rate >= 0.8) return 'bg-red-100 text-red-800'
  if (rate >= 0.6) return 'bg-orange-100 text-orange-800'
  if (rate >= 0.4) return 'bg-yellow-100 text-yellow-800'
  return 'bg-green-100 text-green-800'
}

// 生命周期
onMounted(() => {
  refreshData()
})
</script>