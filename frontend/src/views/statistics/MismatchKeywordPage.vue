<template>
  <div class="p-6 space-y-6">
    <!-- 页面标题 -->
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900">搜索效果分析</h1>
      <div class="flex items-center space-x-2">
        <span class="text-sm text-gray-500">数据更新时间：</span>
        <span class="text-sm font-medium text-gray-900">{{ lastUpdateTime }}</span>
      </div>
    </div>

    <!-- 筛选器 -->
    <div class="bg-white rounded-lg shadow p-4">
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <!-- 时间范围 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">时间范围</label>
          <select
            v-model="filters.timeRange"
            class="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            @change="handleFilterChange"
          >
            <option value="last7days">最近7天</option>
            <option value="last30days">最近30天</option>
            <option value="last90days">最近90天</option>
          </select>
        </div>

        <!-- 搜索空间 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">搜索空间</label>
          <select
            v-model="filters.searchSpace"
            class="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            @change="handleFilterChange"
          >
            <option value="">全部</option>
            <option value="products">产品库</option>
            <option value="documents">文档库</option>
            <option value="news">新闻库</option>
          </select>
        </div>

        <!-- 结果数量限制 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">显示条数</label>
          <select
            v-model="filters.limit"
            class="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
            @change="handleFilterChange"
          >
            <option value="50">50条</option>
            <option value="100">100条</option>
            <option value="200">200条</option>
          </select>
        </div>

        <!-- 刷新按钮 -->
        <div class="flex items-end">
          <button
            @click="refreshData"
            :disabled="loading"
            class="w-full px-4 py-2 bg-emerald-600 text-white rounded-md hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
          >
            <svg v-if="loading" class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ loading ? '加载中...' : '刷新数据' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 统计概览 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
              <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
            </div>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-500">无结果搜索</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.noResultsCount }}</p>
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
            <p class="text-sm font-medium text-gray-500">低匹配度搜索</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.lowMatchCount }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
              <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
              </svg>
            </div>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-500">总搜索次数</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.totalSearchCount }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="flex-shrink-0">
            <div class="w-8 h-8 bg-emerald-100 rounded-full flex items-center justify-center">
              <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </div>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-500">搜索效果评分</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.effectivenessScore }}%</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 问题关键词排行榜 -->
    <div class="bg-white rounded-lg shadow">
      <div class="px-6 py-4 border-b border-gray-200">
        <h2 class="text-lg font-medium text-gray-900">问题关键词排行榜</h2>
        <p class="text-sm text-gray-500 mt-1">搜索效果不佳的关键词统计</p>
      </div>

      <div class="p-6">
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">排名</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">关键词</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">搜索次数</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">无结果次数</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">效果评分</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">建议</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr v-for="(item, index) in mismatchKeywords" :key="item.keyword" class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex items-center">
                    <span class="inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-medium"
                          :class="getRankClass(index + 1)">
                      {{ index + 1 }}
                    </span>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-gray-900">{{ item.keyword }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm text-gray-900">{{ item.searchCount }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm text-gray-900">{{ item.noResultsCount }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                        :class="getScoreClass(item.effectivenessScore)">
                    {{ item.effectivenessScore }}%
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ item.suggestion }}
                </td>
              </tr>
              <tr v-if="mismatchKeywords.length === 0 && !loading">
                <td colspan="6" class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-center">
                  暂无数据
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
import { ref, reactive, onMounted, computed } from 'vue'

// 响应式数据
const loading = ref(false)
const lastUpdateTime = ref('2024-01-20 14:30:15')

const filters = reactive({
  timeRange: 'last7days',
  searchSpace: '',
  limit: '50'
})

const stats = reactive({
  noResultsCount: 1234,
  lowMatchCount: 567,
  totalSearchCount: 8901,
  effectivenessScore: 85.6
})

// 模拟数据
const mismatchKeywords = ref([
  {
    keyword: '投资理财产品',
    searchCount: 156,
    noResultsCount: 89,
    effectivenessScore: 42.9,
    suggestion: '优化产品索引'
  },
  {
    keyword: '基金收益率',
    searchCount: 134,
    noResultsCount: 67,
    effectivenessScore: 50.0,
    suggestion: '扩展同义词库'
  },
  {
    keyword: '股票分析',
    searchCount: 98,
    noResultsCount: 45,
    effectivenessScore: 54.1,
    suggestion: '增加相关内容'
  },
  {
    keyword: '债券投资',
    searchCount: 87,
    noResultsCount: 38,
    effectivenessScore: 56.3,
    suggestion: '调整搜索算法'
  },
  {
    keyword: '保险理财',
    searchCount: 76,
    noResultsCount: 32,
    effectivenessScore: 57.9,
    suggestion: '完善分类标签'
  }
])

// 方法
const handleFilterChange = () => {
  console.log('Filter changed:', filters)
  // 这里应该调用API重新获取数据
  refreshData()
}

const refreshData = async () => {
  loading.value = true

  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 更新时间戳
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')

    console.log('Data refreshed with filters:', filters)
  } catch (error) {
    console.error('Error refreshing data:', error)
  } finally {
    loading.value = false
  }
}

const getRankClass = (rank: number) => {
  if (rank === 1) return 'bg-yellow-100 text-yellow-800'
  if (rank === 2) return 'bg-gray-100 text-gray-800'
  if (rank === 3) return 'bg-orange-100 text-orange-800'
  return 'bg-blue-100 text-blue-800'
}

const getScoreClass = (score: number) => {
  if (score >= 80) return 'bg-green-100 text-green-800'
  if (score >= 60) return 'bg-yellow-100 text-yellow-800'
  if (score >= 40) return 'bg-orange-100 text-orange-800'
  return 'bg-red-100 text-red-800'
}

// 生命周期
onMounted(() => {
  refreshData()
})
</script>