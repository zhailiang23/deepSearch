<template>
  <div class="search-log-filter">
    <div class="filter-section">

      <div class="filter-grid">
        

        <!-- 搜索空间筛选 -->
        <div class="filter-item">
          <label class="filter-label">搜索空间</label>
          <select
            v-model="localFilters.searchSpaceId"
            @change="handleFilter"
            class="w-full h-10 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500"
          >
            <option value="">选择搜索空间</option>
            <option
              v-for="space in searchSpaces"
              :key="space.id"
              :value="space.id"
            >
              {{ space.name }}
            </option>
          </select>
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

        <!-- 操作按钮 -->
        <div class="filter-item">
          <label class="filter-label">&nbsp;</label>
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
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import http from '@/utils/http'

// 简单的防抖函数实现
function debounce<T extends (...args: any[]) => any>(func: T, wait: number): T {
  let timeout: ReturnType<typeof setTimeout>
  return ((...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }) as T
}

// 保存防抖计时器的引用用于清理
let debouncedTimeout: ReturnType<typeof setTimeout> | null = null
import type { SearchLogQuery } from '@/types/searchLog'

// 组件导入
import Input from '@/components/ui/input/Input.vue'
import { Button } from '@/components/ui/button'

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

// 组件卸载标志
const isUnmounted = ref(false)

// 响应式数据
const searchSpaces = ref<SearchSpace[]>([])

// 本地筛选参数
const localFilters = reactive<SearchLogQuery>({
  ...props.modelValue
})

// 计算属性 - 移除了hasActiveFilters和activeFilterTags

// 防抖筛选，使用更安全的实现
const debouncedFilter = (() => {
  let timeout: ReturnType<typeof setTimeout> | null = null

  const fn = () => {
    if (!isUnmounted.value) {
      handleFilter()
    }
  }

  const debouncedFn = () => {
    if (timeout) {
      clearTimeout(timeout)
    }
    timeout = setTimeout(fn, 500)
  }

  // 添加清理方法
  debouncedFn.cancel = () => {
    if (timeout) {
      clearTimeout(timeout)
      timeout = null
    }
  }

  return debouncedFn
})()

// 方法
const handleFilter = () => {
  if (isUnmounted.value) return

  const filters = { ...localFilters }

  // 清理空值和"all"值
  Object.keys(filters).forEach(key => {
    const value = filters[key as keyof SearchLogQuery]
    if (value === '' || value === null || value === undefined || value === 'all') {
      delete filters[key as keyof SearchLogQuery]
    }
  })

  // 重置分页
  filters.page = 0

  emit('update:modelValue', filters)
  emit('filter', filters)
}

const handleReset = () => {
  if (isUnmounted.value) return

  // 保留分页设置，重置其他筛选条件
  const resetFilters: SearchLogQuery = {
    page: 0,
    size: localFilters.size || 20,
    sort: 'createdAt',
    direction: 'desc',
    searchSpaceId: 'all'
  }

  Object.assign(localFilters, resetFilters)
  emit('reset')
  emit('update:modelValue', resetFilters)
  emit('filter', resetFilters)
}


// 监听外部值变化
const stopWatcher = watch(() => props.modelValue, (newValue) => {
  if (!isUnmounted.value) {
    Object.assign(localFilters, newValue)
  }
}, { deep: true })

// 加载搜索空间列表
async function loadAvailableSearchSpaces() {
  try {
    console.log('开始加载搜索空间列表...')

    // 使用统一的http工具类，自动处理token认证
    const result = await http.get('/search-spaces', {
      params: {
        page: 0,
        size: 100
      }
    })

    console.log('搜索空间API响应:', result)
    console.log('响应数据类型:', typeof result)
    console.log('result.data:', result.data)

    // 响应拦截器已自动解包，直接访问result.data.content
    if (result && result.success && result.data && result.data.content && Array.isArray(result.data.content)) {
      searchSpaces.value = result.data.content.map((space: any) => ({
        id: space.id.toString(),
        name: space.name
      }))
      console.log('搜索空间列表加载成功:', searchSpaces.value)
    } else {
      console.warn('搜索空间API返回格式异常或内容为空:', result)
      searchSpaces.value = []
    }
  } catch (error) {
    console.error('加载搜索空间列表时发生错误:', error)
    searchSpaces.value = []
  }
}

onMounted(async () => {
  if (isUnmounted.value) return

  try {
    await loadAvailableSearchSpaces()
  } catch (error) {
    if (!isUnmounted.value) {
      console.error('组件初始化失败:', error)
    }
  }
})

// 组件卸载前的清理工作
onBeforeUnmount(() => {
  isUnmounted.value = true

  // 清理防抖计时器
  if (debouncedFilter && typeof debouncedFilter.cancel === 'function') {
    debouncedFilter.cancel()
  }

  // 停止监听器
  if (stopWatcher) {
    stopWatcher()
  }

  // 重置响应式数据
  searchSpaces.value = []
})
</script>

<style scoped>
.search-log-filter {
  @apply bg-white rounded-lg shadow;
}

.filter-section {
  @apply px-6 pt-6 pb-7;
}

.filter-title {
  @apply text-lg font-semibold text-gray-900 mb-6;
}

.filter-grid {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6;
}


.filter-item {
  @apply space-y-2;
}

.filter-label {
  @apply block text-sm font-medium text-gray-700;
}



.filter-actions {
  @apply flex items-center space-x-3;
}



/* 响应式设计 */
@media (max-width: 768px) {
  .filter-grid {
    @apply grid-cols-1;
  }

  .filter-actions {
    @apply flex-col items-stretch space-x-0 space-y-2;
  }


}

/* 绿色主题定制 */
.filter-title {
  @apply text-green-900;
}

</style>