<template>
  <div class="h-screen flex bg-gray-50">
    <!-- 左侧内容区域 -->
    <div class="flex-1 p-6">
      <div class="bg-white rounded-lg shadow-sm p-6 h-full">
        <h2 class="text-2xl font-bold text-gray-900 mb-4">移动端搜索演示</h2>

        <!-- 搜索配置显示 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <div class="p-4 bg-emerald-50 rounded-lg">
            <h3 class="text-sm font-medium text-emerald-800 mb-2">当前搜索空间</h3>
            <div class="space-y-1">
              <div v-if="config.searchSpaces.selected.length === 0" class="text-sm text-gray-500">
                未选择搜索空间
              </div>
              <div v-else class="flex flex-wrap gap-1">
                <span
                  v-for="spaceId in config.searchSpaces.selected"
                  :key="spaceId"
                  class="inline-flex items-center px-2 py-1 rounded-full text-xs bg-emerald-200 text-emerald-800"
                >
                  {{ getSpaceName(spaceId) }}
                </span>
              </div>
            </div>
          </div>

          <div class="p-4 bg-blue-50 rounded-lg">
            <h3 class="text-sm font-medium text-blue-800 mb-2">拼音搜索</h3>
            <div class="text-sm">
              <div class="text-gray-700">
                状态: <span class="font-medium">{{ config.pinyinSearch.enabled ? '启用' : '禁用' }}</span>
              </div>
              <div v-if="config.pinyinSearch.enabled" class="text-gray-700 mt-1">
                模式: <span class="font-medium">{{ getPinyinModeText(config.pinyinSearch.mode) }}</span>
              </div>
            </div>
          </div>

          <div class="p-4 bg-purple-50 rounded-lg">
            <h3 class="text-sm font-medium text-purple-800 mb-2">分页配置</h3>
            <div class="text-sm space-y-1">
              <div class="text-gray-700">
                页面大小: <span class="font-medium">{{ config.pagination.pageSize }}</span>
              </div>
              <div class="text-gray-700">
                初始加载: <span class="font-medium">{{ config.pagination.initialLoad }} 条</span>
              </div>
            </div>
          </div>

          <div class="p-4 bg-gray-50 rounded-lg">
            <h3 class="text-sm font-medium text-gray-800 mb-2">搜索行为</h3>
            <div class="text-sm space-y-1">
              <div class="text-gray-700">
                防抖延迟: <span class="font-medium">{{ config.searchBehavior.debounceMs }}ms</span>
              </div>
              <div class="text-gray-700">
                自动搜索: <span class="font-medium">{{ config.searchBehavior.autoSearch ? '启用' : '禁用' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 模拟搜索界面 -->
        <div class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
          <h3 class="text-lg font-medium text-gray-900 mb-2">移动端搜索界面</h3>
          <p class="text-sm text-gray-600 mb-4">
            这里将显示移动端搜索界面组件
          </p>
          <div class="max-w-md mx-auto">
            <div class="flex gap-2">
              <input
                v-model="demoQuery"
                type="text"
                placeholder="输入搜索关键词..."
                class="flex-1 px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500"
                @input="handleSearchInput"
              />
              <button
                @click="performSearch"
                :disabled="!demoQuery.trim()"
                class="px-4 py-2 bg-emerald-600 text-white text-sm font-medium rounded-md hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                搜索
              </button>
            </div>
          </div>

          <!-- 搜索结果模拟 -->
          <div v-if="searchResults.length > 0" class="mt-6 space-y-3 max-w-md mx-auto">
            <div
              v-for="(result, index) in searchResults"
              :key="index"
              class="p-3 bg-white border border-gray-200 rounded-lg text-left"
            >
              <h4 class="text-sm font-medium text-gray-900">{{ result.title }}</h4>
              <p class="text-xs text-gray-600 mt-1">{{ result.summary }}</p>
            </div>

            <div class="text-xs text-gray-500 mt-4">
              共找到 {{ searchResults.length }} 条结果 (模拟数据)
            </div>
          </div>
        </div>

        <!-- 参数变更日志 -->
        <div v-if="parameterChanges.length > 0" class="mt-6">
          <h3 class="text-sm font-medium text-gray-700 mb-3">参数变更记录</h3>
          <div class="max-h-32 overflow-y-auto space-y-1">
            <div
              v-for="change in parameterChanges.slice(-10)"
              :key="change.timestamp"
              class="text-xs p-2 bg-gray-50 rounded flex justify-between"
            >
              <span>{{ change.type }}.{{ change.key }} 已更新</span>
              <span class="text-gray-500">{{ formatTime(change.timestamp) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧参数面板 -->
    <ParameterPanel
      v-model="config"
      :collapsed="false"
      show-presets
      allow-reset
      @parameter-change="handleParameterChange"
      @sync-status="handleSyncStatus"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ParameterPanel from './ParameterPanel.vue'
import { DEFAULT_SEARCH_DEMO_CONFIG } from '@/types/demo'
import type { SearchDemoConfig, ParameterChangeEvent, SyncStatus, SearchSpaceOption } from '@/types/demo'

// 响应式数据
const config = ref<SearchDemoConfig>({
  ...DEFAULT_SEARCH_DEMO_CONFIG,
  searchSpaces: {
    ...DEFAULT_SEARCH_DEMO_CONFIG.searchSpaces,
    available: [
      {
        id: 'user-info',
        name: '用户信息',
        description: '用户基础信息和档案数据',
        fields: ['name', 'email', 'phone', 'address'],
        enabled: true,
        indexStatus: 'healthy',
        docCount: 10250
      },
      {
        id: 'article-content',
        name: '文章内容',
        description: '新闻文章、博客和内容数据',
        fields: ['title', 'content', 'author', 'tags'],
        enabled: true,
        indexStatus: 'healthy',
        docCount: 45680
      },
      {
        id: 'system-logs',
        name: '系统日志',
        description: '应用程序日志和审计记录',
        fields: ['level', 'message', 'timestamp', 'source'],
        enabled: true,
        indexStatus: 'warning',
        docCount: 125430
      }
    ]
  }
})

const demoQuery = ref('')
const searchResults = ref<Array<{ title: string; summary: string }>>([])
const parameterChanges = ref<ParameterChangeEvent[]>([])

// 防抖搜索
let searchDebounceTimer: number | null = null

// 计算属性
const availableSpaces = computed(() => config.value.searchSpaces.available)

// 方法
function getSpaceName(spaceId: string): string {
  const space = availableSpaces.value.find(s => s.id === spaceId)
  return space?.name || spaceId
}

function getPinyinModeText(mode: string): string {
  const modeTexts = {
    fuzzy: '模糊匹配',
    exact: '精确匹配',
    both: '智能匹配'
  }
  return modeTexts[mode as keyof typeof modeTexts] || '未知模式'
}

function getLoadModeText(mode: string): string {
  const modeTexts = {
    pagination: '分页模式',
    infinite: '无限滚动',
    loadMore: '点击加载'
  }
  return modeTexts[mode as keyof typeof modeTexts] || '未知模式'
}

function handleParameterChange(event: ParameterChangeEvent) {
  parameterChanges.value.push(event)
  console.log('参数变更:', event)
}

function handleSyncStatus(status: SyncStatus) {
  console.log('同步状态:', status)
}

function handleSearchInput() {
  if (config.value.searchBehavior.autoSearch) {
    if (searchDebounceTimer) {
      clearTimeout(searchDebounceTimer)
    }

    searchDebounceTimer = setTimeout(() => {
      if (demoQuery.value.length >= config.value.searchBehavior.minQueryLength) {
        performSearch()
      }
    }, config.value.searchBehavior.debounceMs)
  }
}

function performSearch() {
  if (!demoQuery.value.trim()) return

  // 模拟搜索结果
  const mockResults = [
    {
      title: `关于"${demoQuery.value}"的搜索结果 1`,
      summary: '这是一个模拟的搜索结果摘要，显示匹配的内容片段...'
    },
    {
      title: `关于"${demoQuery.value}"的搜索结果 2`,
      summary: '另一个模拟结果，展示搜索功能的配置效果...'
    },
    {
      title: `关于"${demoQuery.value}"的搜索结果 3`,
      summary: '第三个搜索结果，用于测试分页和加载配置...'
    }
  ]

  // 根据页面大小限制结果
  searchResults.value = mockResults.slice(0, config.value.pagination.pageSize)
}

function formatTime(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 监听配置变化以清除搜索结果
watch(() => config.value.searchSpaces.selected, () => {
  searchResults.value = []
}, { deep: true })
</script>

<style scoped>
/* 确保滚动条样式 */
::-webkit-scrollbar {
  width: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgb(167 243 208);
  border-radius: 2px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgb(110 231 183);
}

/* 过渡动画 */
.transition-colors {
  transition: background-color 0.2s ease, border-color 0.2s ease;
}
</style>