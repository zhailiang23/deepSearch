<template>
  <div class="hot-word-filter">
    <!-- 过滤器头部 -->
    <div class="filter-header">
      <h3 class="filter-title">搜索过滤器</h3>
      <div class="filter-actions">
        <button
          @click="resetFilters"
          :disabled="!hasActiveFilters"
          class="reset-btn"
          title="重置所有过滤条件"
        >
          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          重置
        </button>
        <button
          @click="applyFilters"
          class="apply-btn"
          title="应用过滤条件"
        >
          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          应用
        </button>
      </div>
    </div>

    <!-- 过滤器内容 -->
    <div class="filter-content">
      <!-- 时间范围选择器 -->
      <div class="filter-section">
        <TimeRangeSelector
          v-model:start-time="localFilterState.timeRange.startTime"
          v-model:end-time="localFilterState.timeRange.endTime"
          v-model:preset="localFilterState.timeRange.preset"
        />
      </div>

      <!-- 搜索条件过滤器 -->
      <div class="filter-section">
        <SearchConditionFilter
          v-model:search-keyword="localFilterState.searchKeyword"
          :suggestions="searchSuggestions"
          @search="handleSearch"
        />
      </div>

      <!-- 热词数量限制 -->
      <div class="filter-section">
        <HotWordLimitSelector
          v-model:limit="localFilterState.limit"
          :min-limit="10"
          :max-limit="1000"
        />
      </div>

      <!-- 高级选项（可折叠） -->
      <div class="filter-section">
        <button
          @click="toggleAdvancedOptions"
          class="advanced-toggle"
        >
          <span class="advanced-label">高级选项</span>
          <svg
            :class="[
              'w-4 h-4 transition-transform duration-200',
              { 'transform rotate-180': showAdvancedOptions }
            ]"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        <div v-if="showAdvancedOptions" class="advanced-options">
          <!-- 排序选项 -->
          <div class="advanced-option">
            <label class="option-label">排序方式</label>
            <select
              v-model="localFilterState.sortBy"
              class="option-select"
            >
              <option value="count">按搜索次数</option>
              <option value="keyword">按关键词</option>
              <option value="rank">按排名</option>
            </select>
          </div>

          <div class="advanced-option">
            <label class="option-label">排序顺序</label>
            <select
              v-model="localFilterState.sortOrder"
              class="option-select"
            >
              <option value="desc">降序</option>
              <option value="asc">升序</option>
            </select>
          </div>

          <!-- 最小搜索次数过滤 -->
          <div class="advanced-option">
            <label class="option-label">最小搜索次数</label>
            <input
              v-model.number="localFilterState.minCount"
              type="number"
              min="1"
              class="option-input"
              placeholder="如: 10"
            >
          </div>
        </div>
      </div>

      <!-- 过滤状态指示器 -->
      <div v-if="hasActiveFilters" class="filter-status">
        <div class="status-header">
          <span class="status-label">当前过滤条件:</span>
          <button @click="resetFilters" class="clear-all-btn">
            清除全部
          </button>
        </div>
        <div class="status-tags">
          <span v-if="localFilterState.timeRange.preset" class="status-tag">
            时间: {{ getPresetLabel(localFilterState.timeRange.preset) }}
            <button @click="clearTimeFilter" class="tag-clear">×</button>
          </span>
          <span v-if="localFilterState.searchKeyword" class="status-tag">
            关键词: {{ localFilterState.searchKeyword }}
            <button @click="clearSearchFilter" class="tag-clear">×</button>
          </span>
          <span v-if="localFilterState.limit !== defaultFilterState.limit" class="status-tag">
            数量: {{ localFilterState.limit }}
            <button @click="clearLimitFilter" class="tag-clear">×</button>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import TimeRangeSelector from './filters/TimeRangeSelector.vue'
import SearchConditionFilter from './filters/SearchConditionFilter.vue'
import HotWordLimitSelector from './filters/HotWordLimitSelector.vue'
import type { FilterState, HotWordQueryParams } from '@/types/statistics'

interface Props {
  filterState: FilterState
  searchSuggestions?: string[]
  loading?: boolean
}

interface Emits {
  (e: 'update:filterState', value: FilterState): void
  (e: 'filter-change', params: HotWordQueryParams): void
  (e: 'search', keyword: string): void
  (e: 'reset'): void
}

const props = withDefaults(defineProps<Props>(), {
  searchSuggestions: () => [],
  loading: false
})

const emit = defineEmits<Emits>()

const showAdvancedOptions = ref(false)

// 默认过滤状态
const defaultFilterState: FilterState = {
  timeRange: {
    startTime: '',
    endTime: '',
    preset: 'last7days'
  },
  searchKeyword: '',
  limit: 50,
  sortBy: 'count',
  sortOrder: 'desc',
  minCount: undefined
}

// 本地过滤状态
const localFilterState = reactive<FilterState & { sortBy: 'count' | 'keyword' | 'rank', sortOrder: 'asc' | 'desc', minCount?: number }>({
  ...defaultFilterState,
  ...props.filterState,
  sortBy: 'count',
  sortOrder: 'desc'
})

/**
 * 检查是否有活跃的过滤条件
 */
const hasActiveFilters = computed(() => {
  return (
    localFilterState.timeRange.preset !== defaultFilterState.timeRange.preset ||
    localFilterState.searchKeyword !== defaultFilterState.searchKeyword ||
    localFilterState.limit !== defaultFilterState.limit ||
    localFilterState.minCount !== undefined
  )
})

/**
 * 获取预设时间范围的标签
 */
const getPresetLabel = (preset: string) => {
  const presetLabels: Record<string, string> = {
    today: '今天',
    last7days: '最近7天',
    last30days: '最近30天',
    thisMonth: '本月',
    lastMonth: '上月'
  }
  return presetLabels[preset] || preset
}

/**
 * 切换高级选项显示
 */
const toggleAdvancedOptions = () => {
  showAdvancedOptions.value = !showAdvancedOptions.value
}

/**
 * 应用过滤条件
 */
const applyFilters = () => {
  const queryParams: HotWordQueryParams = {
    startTime: localFilterState.timeRange.startTime,
    endTime: localFilterState.timeRange.endTime,
    searchKeyword: localFilterState.searchKeyword,
    limit: localFilterState.limit,
    sortBy: localFilterState.sortBy,
    sortOrder: localFilterState.sortOrder
  }

  // 添加最小搜索次数过滤
  if (localFilterState.minCount && localFilterState.minCount > 0) {
    queryParams.minCount = localFilterState.minCount
  }

  emit('update:filterState', { ...localFilterState })
  emit('filter-change', queryParams)
}

/**
 * 重置所有过滤条件
 */
const resetFilters = () => {
  Object.assign(localFilterState, defaultFilterState)
  emit('update:filterState', { ...localFilterState })
  emit('reset')
}

/**
 * 处理搜索事件
 */
const handleSearch = (keyword: string) => {
  localFilterState.searchKeyword = keyword
  emit('search', keyword)
  applyFilters()
}

/**
 * 清除时间过滤条件
 */
const clearTimeFilter = () => {
  localFilterState.timeRange = { ...defaultFilterState.timeRange }
  applyFilters()
}

/**
 * 清除搜索过滤条件
 */
const clearSearchFilter = () => {
  localFilterState.searchKeyword = defaultFilterState.searchKeyword
  applyFilters()
}

/**
 * 清除数量限制过滤条件
 */
const clearLimitFilter = () => {
  localFilterState.limit = defaultFilterState.limit
  applyFilters()
}

// 监听props变化
watch(() => props.filterState, (newState) => {
  Object.assign(localFilterState, newState)
}, { deep: true })

// 监听本地状态变化（自动应用）
watch(localFilterState, () => {
  if (hasActiveFilters.value) {
    // 可以添加自动应用逻辑，当前需要手动点击应用按钮
  }
}, { deep: true })
</script>

<style scoped>
.hot-word-filter {
  @apply bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden;
}

/* 过滤器头部 */
.filter-header {
  @apply
    flex
    items-center
    justify-between
    p-4
    bg-green-50
    border-b
    border-green-200;
}

.filter-title {
  @apply text-lg font-semibold text-gray-900;
}

.filter-actions {
  @apply flex items-center gap-2;
}

.reset-btn {
  @apply
    inline-flex
    items-center
    px-3
    py-1.5
    text-sm
    font-medium
    text-gray-600
    bg-white
    border
    border-gray-300
    rounded-md
    hover:bg-gray-50
    hover:border-gray-400
    disabled:opacity-50
    disabled:cursor-not-allowed
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-gray-500
    focus:ring-offset-2;
}

.apply-btn {
  @apply
    inline-flex
    items-center
    px-3
    py-1.5
    text-sm
    font-medium
    text-white
    bg-green-600
    border
    border-green-600
    rounded-md
    hover:bg-green-700
    hover:border-green-700
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:ring-offset-2;
}

/* 过滤器内容 */
.filter-content {
  @apply p-4 space-y-6;
}

.filter-section {
  @apply space-y-3;
}

/* 高级选项 */
.advanced-toggle {
  @apply
    flex
    items-center
    justify-between
    w-full
    text-left
    text-sm
    font-medium
    text-gray-700
    hover:text-gray-900
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:ring-offset-2
    rounded-md
    p-2
    hover:bg-gray-50
    transition-colors
    duration-200;
}

.advanced-label {
  @apply flex items-center gap-2;
}

.advanced-options {
  @apply mt-3 space-y-4 p-4 bg-gray-50 rounded-md border border-gray-200;
}

.advanced-option {
  @apply space-y-1;
}

.option-label {
  @apply block text-xs font-medium text-gray-600;
}

.option-select,
.option-input {
  @apply
    w-full
    px-3
    py-2
    text-sm
    border
    border-gray-300
    rounded-md
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:border-green-500
    transition-colors
    duration-200;
}

/* 过滤状态指示器 */
.filter-status {
  @apply
    p-4
    bg-blue-50
    border
    border-blue-200
    rounded-md
    space-y-3;
}

.status-header {
  @apply flex items-center justify-between;
}

.status-label {
  @apply text-sm font-medium text-blue-800;
}

.clear-all-btn {
  @apply
    text-xs
    text-blue-600
    hover:text-blue-800
    focus:outline-none
    focus:underline
    transition-colors
    duration-200;
}

.status-tags {
  @apply flex flex-wrap gap-2;
}

.status-tag {
  @apply
    inline-flex
    items-center
    gap-1
    px-2
    py-1
    text-xs
    font-medium
    text-blue-800
    bg-blue-100
    border
    border-blue-200
    rounded-md;
}

.tag-clear {
  @apply
    ml-1
    text-blue-600
    hover:text-blue-800
    focus:outline-none
    font-bold
    transition-colors
    duration-200;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .filter-header {
    @apply flex-col items-start gap-3 p-3;
  }

  .filter-actions {
    @apply self-end;
  }

  .filter-title {
    @apply text-base;
  }

  .filter-content {
    @apply p-3 space-y-4;
  }

  .reset-btn,
  .apply-btn {
    @apply text-xs px-2 py-1;
  }

  .status-tags {
    @apply flex-col gap-1;
  }

  .status-tag {
    @apply justify-between;
  }
}

/* 加载状态优化 */
.hot-word-filter[data-loading="true"] {
  @apply opacity-75 pointer-events-none;
}

/* 动画优化 */
.advanced-options {
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 焦点状态优化 */
.filter-section:focus-within {
  @apply ring-2 ring-green-500 ring-opacity-50 rounded-md;
}
</style>