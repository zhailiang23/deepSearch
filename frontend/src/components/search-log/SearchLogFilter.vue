<template>
  <div class="search-log-filter">
    <div class="filter-section">
      <h3 class="filter-title">筛选条件</h3>

      <div class="filter-grid">
        <!-- 用户ID筛选 -->
        <div class="filter-item">
          <label class="filter-label">用户ID</label>
          <Input
            v-model="localFilters.userId"
            placeholder="输入用户ID"
            class="w-full"
            @input="debouncedFilter"
          />
        </div>

        <!-- 搜索空间筛选 -->
        <div class="filter-item">
          <label class="filter-label">搜索空间</label>
          <Select v-model="localFilters.searchSpaceId" @update:model-value="handleFilter">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="选择搜索空间" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">全部</SelectItem>
              <SelectItem
                v-for="space in searchSpaces"
                :key="space.id"
                :value="space.id"
              >
                {{ space.name }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- 查询关键词 -->
        <div class="filter-item">
          <label class="filter-label">查询关键词</label>
          <Input
            v-model="localFilters.query"
            placeholder="输入关键词"
            class="w-full"
            @input="debouncedFilter"
          />
        </div>

        <!-- 状态筛选 -->
        <div class="filter-item">
          <label class="filter-label">状态</label>
          <Select v-model="localFilters.status" @update:model-value="handleFilter">
            <SelectTrigger class="w-full">
              <SelectValue placeholder="选择状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">全部</SelectItem>
              <SelectItem value="SUCCESS">成功</SelectItem>
              <SelectItem value="ERROR">失败</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- 时间范围 -->
        <div class="filter-item">
          <label class="filter-label">创建时间</label>
          <DateTimePicker
            v-model:start="localFilters.startTime"
            v-model:end="localFilters.endTime"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            @change="handleFilter"
          />
        </div>

        <!-- 响应时间范围 -->
        <div class="filter-item">
          <label class="filter-label">响应时间(ms)</label>
          <div class="range-inputs">
            <Input
              v-model.number="localFilters.minResponseTime"
              placeholder="最小值"
              type="number"
              class="flex-1"
              @input="debouncedFilter"
            />
            <span class="range-separator">至</span>
            <Input
              v-model.number="localFilters.maxResponseTime"
              placeholder="最大值"
              type="number"
              class="flex-1"
              @input="debouncedFilter"
            />
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="filter-actions">
        <Button @click="handleFilter" variant="default">
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          筛选
        </Button>
        <Button @click="handleReset" variant="outline">
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          重置
        </Button>
        <Button
          @click="toggleAdvanced"
          variant="ghost"
          size="sm"
        >
          {{ showAdvanced ? '简单筛选' : '高级筛选' }}
        </Button>
      </div>

      <!-- 高级筛选 -->
      <div v-if="showAdvanced" class="advanced-filters">
        <div class="divider my-4"></div>

        <div class="advanced-grid">
          <!-- 点击次数范围 -->
          <div class="filter-item">
            <label class="filter-label">点击次数</label>
            <div class="range-inputs">
              <Input
                v-model.number="localFilters.minClicks"
                placeholder="最小"
                type="number"
                class="flex-1"
                @input="debouncedFilter"
              />
              <span class="range-separator">至</span>
              <Input
                v-model.number="localFilters.maxClicks"
                placeholder="最大"
                type="number"
                class="flex-1"
                @input="debouncedFilter"
              />
            </div>
          </div>

          <!-- 结果数量范围 -->
          <div class="filter-item">
            <label class="filter-label">结果数量</label>
            <div class="range-inputs">
              <Input
                v-model.number="localFilters.minResults"
                placeholder="最小"
                type="number"
                class="flex-1"
                @input="debouncedFilter"
              />
              <span class="range-separator">至</span>
              <Input
                v-model.number="localFilters.maxResults"
                placeholder="最大"
                type="number"
                class="flex-1"
                @input="debouncedFilter"
              />
            </div>
          </div>

          <!-- 用户IP -->
          <div class="filter-item">
            <label class="filter-label">用户IP</label>
            <Input
              v-model="localFilters.userIp"
              placeholder="输入IP地址"
              class="w-full"
              @input="debouncedFilter"
            />
          </div>

          <!-- 排序方式 -->
          <div class="filter-item">
            <label class="filter-label">排序方式</label>
            <div class="sort-controls">
              <Select v-model="localFilters.sort" @update:model-value="handleFilter">
                <SelectTrigger class="flex-1">
                  <SelectValue placeholder="排序字段" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="createdAt">创建时间</SelectItem>
                  <SelectItem value="responseTime">响应时间</SelectItem>
                  <SelectItem value="resultCount">结果数量</SelectItem>
                  <SelectItem value="clickCount">点击次数</SelectItem>
                  <SelectItem value="id">ID</SelectItem>
                </SelectContent>
              </Select>
              <Select v-model="localFilters.direction" @update:model-value="handleFilter">
                <SelectTrigger class="w-24">
                  <SelectValue placeholder="排序" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="desc">降序</SelectItem>
                  <SelectItem value="asc">升序</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>
      </div>

      <!-- 当前筛选条件摘要 -->
      <div v-if="hasActiveFilters" class="filter-summary">
        <div class="summary-header">
          <span class="summary-title">当前筛选条件</span>
          <Button @click="handleReset" variant="ghost" size="sm">
            清除全部
          </Button>
        </div>
        <div class="summary-tags">
          <span
            v-for="tag in activeFilterTags"
            :key="tag.key"
            class="filter-tag"
          >
            {{ tag.label }}: {{ tag.value }}
            <button
              @click="removeFilter(tag.key)"
              class="tag-remove"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
// 简单的防抖函数实现
function debounce<T extends (...args: any[]) => any>(func: T, wait: number): T {
  let timeout: ReturnType<typeof setTimeout>
  return ((...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }) as T
}
import type { SearchLogQuery } from '@/types/searchLog'

// 组件导入
import Input from '@/components/ui/input/Input.vue'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from '@/components/ui/select'
import DateTimePicker from '@/components/ui/DateTimePicker.vue'

interface SearchSpace {
  id: string
  name: string
}

interface Props {
  modelValue: SearchLogQuery
}

interface Emits {
  (e: 'update:modelValue', value: SearchLogQuery): void
  (e: 'filter', filters: SearchLogQuery): void
  (e: 'reset'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const showAdvanced = ref(false)
const searchSpaces = ref<SearchSpace[]>([])

// 扩展的本地筛选参数（包含高级筛选字段）
interface ExtendedSearchLogQuery extends SearchLogQuery {
  minClicks?: number
  maxClicks?: number
  minResults?: number
  maxResults?: number
  userIp?: string
}

// 本地筛选参数
const localFilters = reactive<ExtendedSearchLogQuery>({
  ...props.modelValue
})

// 计算属性
const hasActiveFilters = computed(() => {
  const filters = localFilters
  return !!(
    filters.userId ||
    filters.searchSpaceId ||
    filters.query ||
    filters.status ||
    filters.startTime ||
    filters.endTime ||
    filters.minResponseTime ||
    filters.maxResponseTime ||
    filters.minClicks ||
    filters.maxClicks ||
    filters.minResults ||
    filters.maxResults ||
    filters.userIp
  )
})

const activeFilterTags = computed(() => {
  const tags: Array<{ key: string; label: string; value: string }> = []

  if (localFilters.userId) {
    tags.push({ key: 'userId', label: '用户ID', value: localFilters.userId })
  }
  if (localFilters.searchSpaceId) {
    const space = searchSpaces.value.find(s => s.id === localFilters.searchSpaceId)
    tags.push({ key: 'searchSpaceId', label: '搜索空间', value: space?.name || localFilters.searchSpaceId })
  }
  if (localFilters.query) {
    tags.push({ key: 'query', label: '关键词', value: localFilters.query })
  }
  if (localFilters.status) {
    tags.push({ key: 'status', label: '状态', value: localFilters.status === 'SUCCESS' ? '成功' : '失败' })
  }
  if (localFilters.startTime || localFilters.endTime) {
    const timeRange = `${localFilters.startTime || ''} ~ ${localFilters.endTime || ''}`
    tags.push({ key: 'timeRange', label: '时间范围', value: timeRange })
  }
  if (localFilters.minResponseTime || localFilters.maxResponseTime) {
    const responseRange = `${localFilters.minResponseTime || 0} - ${localFilters.maxResponseTime || '∞'}ms`
    tags.push({ key: 'responseTimeRange', label: '响应时间', value: responseRange })
  }
  if (localFilters.userIp) {
    tags.push({ key: 'userIp', label: '用户IP', value: localFilters.userIp })
  }

  return tags
})

// 防抖筛选
const debouncedFilter = debounce(() => {
  handleFilter()
}, 500)

// 方法
const handleFilter = () => {
  const filters = { ...localFilters }

  // 清理空值
  Object.keys(filters).forEach(key => {
    const value = filters[key as keyof ExtendedSearchLogQuery]
    if (value === '' || value === null || value === undefined) {
      delete filters[key as keyof ExtendedSearchLogQuery]
    }
  })

  // 重置分页
  filters.page = 0

  emit('update:modelValue', filters)
  emit('filter', filters)
}

const handleReset = () => {
  // 保留分页设置，重置其他筛选条件
  const resetFilters: ExtendedSearchLogQuery = {
    page: 0,
    size: localFilters.size || 20,
    sort: 'createdAt',
    direction: 'desc'
  }

  Object.assign(localFilters, resetFilters)
  emit('reset')
  emit('update:modelValue', resetFilters)
  emit('filter', resetFilters)
}

const toggleAdvanced = () => {
  showAdvanced.value = !showAdvanced.value
}

const removeFilter = (filterKey: string) => {
  switch (filterKey) {
    case 'userId':
      localFilters.userId = undefined
      break
    case 'searchSpaceId':
      localFilters.searchSpaceId = undefined
      break
    case 'query':
      localFilters.query = undefined
      break
    case 'status':
      localFilters.status = undefined
      break
    case 'timeRange':
      localFilters.startTime = undefined
      localFilters.endTime = undefined
      break
    case 'responseTimeRange':
      localFilters.minResponseTime = undefined
      localFilters.maxResponseTime = undefined
      break
    case 'userIp':
      localFilters.userIp = undefined
      break
  }
  handleFilter()
}

// 监听外部值变化
watch(() => props.modelValue, (newValue) => {
  Object.assign(localFilters, newValue)
}, { deep: true })

// 加载搜索空间列表
onMounted(async () => {
  try {
    // 这里应该调用 searchSpaceApi.getSearchSpaces()
    // 为了简化，先使用模拟数据
    searchSpaces.value = [
      { id: 'space1', name: '文档搜索空间' },
      { id: 'space2', name: '产品搜索空间' },
      { id: 'space3', name: '用户搜索空间' }
    ]
  } catch (error) {
    console.error('加载搜索空间失败:', error)
  }
})
</script>

<style scoped>
.search-log-filter {
  @apply bg-white rounded-lg shadow;
}

.filter-section {
  @apply p-6;
}

.filter-title {
  @apply text-lg font-semibold text-gray-900 mb-6;
}

.filter-grid {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6;
}

.advanced-grid {
  @apply grid grid-cols-1 md:grid-cols-2 gap-6;
}

.filter-item {
  @apply space-y-2;
}

.filter-label {
  @apply block text-sm font-medium text-gray-700;
}

.range-inputs {
  @apply flex items-center space-x-2;
}

.range-separator {
  @apply text-gray-500 text-sm font-medium;
}

.sort-controls {
  @apply flex space-x-2;
}

.filter-actions {
  @apply flex items-center space-x-3 pb-4;
}

.advanced-filters {
  @apply pt-4;
}

.divider {
  @apply border-t border-gray-200;
}

.filter-summary {
  @apply mt-4 pt-4 border-t border-gray-200;
}

.summary-header {
  @apply flex items-center justify-between mb-3;
}

.summary-title {
  @apply text-sm font-medium text-gray-700;
}

.summary-tags {
  @apply flex flex-wrap gap-2;
}

.filter-tag {
  @apply inline-flex items-center px-3 py-1 rounded-full text-xs bg-green-50 text-green-700 border border-green-200;
}

.tag-remove {
  @apply ml-2 text-green-500 hover:text-green-700 transition-colors;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-grid {
    @apply grid-cols-1;
  }

  .filter-actions {
    @apply flex-col items-stretch space-x-0 space-y-2;
  }

  .range-inputs {
    @apply flex-col space-x-0 space-y-2;
  }

  .sort-controls {
    @apply flex-col space-x-0 space-y-2;
  }
}

/* 绿色主题定制 */
.filter-title {
  @apply text-green-900;
}

.summary-title {
  @apply text-green-800;
}
</style>