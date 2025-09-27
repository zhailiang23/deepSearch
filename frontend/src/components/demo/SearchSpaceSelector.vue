<template>
  <div class="space-y-2">
    <label class="text-sm font-medium text-gray-700">搜索空间选择</label>
    <select
      v-model="selectedValue"
      @change="handleSelectionChange"
      :disabled="loading || !displayOptions.length"
      class="w-full h-10 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50"
    >
      <option value="">{{ loading ? '加载中...' : '请选择搜索空间' }}</option>
      <option
        v-for="option in displayOptions"
        :key="option.id"
        :value="option.id"
        :disabled="option.enabled === false"
      >
        {{ option.name }}
      </option>
    </select>

    <!-- 错误提示 -->
    <div v-if="error" class="text-xs text-red-600">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import http from '@/utils/http'
import { useMobileSearchDemoStore } from '@/stores/mobileSearchDemo'

// Types
interface SearchSpaceOption {
  id: string
  name: string
  description?: string
  fields?: string[]
  enabled?: boolean
  indexStatus?: 'healthy' | 'warning' | 'error' | 'unknown'
  docCount?: number
}

interface BackendSearchSpace {
  id: number
  name: string
  description?: string
}

interface Props {
  modelValue: string[]
  options?: SearchSpaceOption[]
  multiple?: boolean
  showDetails?: boolean
  disabled?: boolean
}

// Props & Emits
const props = withDefaults(defineProps<Props>(), {
  multiple: true,
  showDetails: false,
  disabled: false,
  options: () => []
})

const emits = defineEmits<{
  'update:modelValue': [value: string[]]
  'loading': [loading: boolean]
  'error': [error: string | null]
}>()

// Store
const store = useMobileSearchDemoStore()

// Local state
const selectedValue = ref<string>(props.modelValue[0] || '')
const availableOptions = ref<SearchSpaceOption[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

// Computed - 使用本地加载的选项或传入的选项
const displayOptions = computed(() => {
  return props.options?.length ? props.options : availableOptions.value
})

// Watch for prop changes
watch(() => props.modelValue, (newValue) => {
  selectedValue.value = newValue[0] || ''
}, { deep: true })

// Watch for local changes and emit
watch(selectedValue, (newValue) => {
  emits('update:modelValue', newValue ? [newValue] : [])
})

// Watch loading state and emit
watch(loading, (newValue) => {
  emits('loading', newValue)
})

// Watch error state and emit
watch(error, (newValue) => {
  emits('error', newValue)
})

// Methods
async function loadAvailableSearchSpaces() {
  // 如果已经有 props.options，不需要加载
  if (props.options?.length) {
    return
  }

  try {
    loading.value = true
    error.value = null

    console.log('SearchSpaceSelector: 开始加载搜索空间列表...')

    // 使用与 SearchDataManagePage 相同的API调用
    const result = await http.get('/search-spaces', {
      params: {
        page: 0,
        size: 100
      }
    })

    console.log('SearchSpaceSelector: 搜索空间API响应:', result)

    // 处理后端返回的ApiResponse格式
    if (result.success && result.data && result.data.content) {
      const spaces = result.data.content.map((space: BackendSearchSpace) => ({
        id: space.id.toString(),
        name: space.name,
        description: space.description,
        fields: [], // 后端可能没有提供字段信息
        enabled: true,
        indexStatus: 'healthy' as const,
        docCount: 0
      }))

      availableOptions.value = spaces

      // 重要：将搜索空间同步到store中
      store.setSearchSpaces(spaces)

      console.log('SearchSpaceSelector: 搜索空间列表加载成功:', availableOptions.value)
    } else {
      console.warn('SearchSpaceSelector: 搜索空间API返回格式异常:', result)
      throw new Error(result.message || '获取搜索空间列表失败')
    }
  } catch (err: any) {
    console.error('SearchSpaceSelector: 加载搜索空间列表时发生错误:', err)

    const errorMessage = err.response?.data?.message || err.message || '加载搜索空间失败'
    error.value = errorMessage

    // 使用备用数据
    const fallbackSpaces = [
      {
        id: '1',
        name: '示例搜索空间1',
        description: '这是第一个示例搜索空间',
        fields: [],
        enabled: true,
        indexStatus: 'healthy' as const,
        docCount: 0
      },
      {
        id: '2',
        name: '示例搜索空间2',
        description: '这是第二个示例搜索空间',
        fields: [],
        enabled: true,
        indexStatus: 'healthy' as const,
        docCount: 0
      }
    ]

    availableOptions.value = fallbackSpaces

    // 同时更新store
    store.setSearchSpaces(fallbackSpaces)
  } finally {
    loading.value = false
  }
}

function handleSelectionChange() {
  // v-model will handle the update automatically
}

// 组件挂载时加载搜索空间
onMounted(() => {
  loadAvailableSearchSpaces()
})
</script>