<template>
  <div class="space-y-2">
    <label class="text-sm font-medium text-gray-700">搜索空间选择</label>

    <!-- 多选搜索空间 -->
    <div class="multi-select-wrapper">
      <div
        class="multi-select-control"
        :class="{ 'disabled': loading || !displayOptions.length, 'focused': isFocused }"
        @click="toggleDropdown"
      >
        <!-- 选中的标签 -->
        <div class="selected-tags" v-if="selectedOptions.length > 0">
          <span
            v-for="option in selectedOptions"
            :key="option.id"
            class="selected-tag"
          >
            {{ option.name }}
            <button
              type="button"
              class="remove-tag"
              @click.stop="removeOption(option.id)"
            >
              ×
            </button>
          </span>
        </div>
        <!-- 占位文本 -->
        <span v-else class="placeholder">
          {{ loading ? '加载中...' : '请选择搜索空间' }}
        </span>

        <!-- 下拉箭头 -->
        <svg
          class="dropdown-arrow"
          :class="{ 'open': isOpen }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
        </svg>
      </div>

      <!-- 下拉选项列表 -->
      <div v-if="isOpen && !loading" class="dropdown-menu">
        <div
          v-for="option in displayOptions"
          :key="option.id"
          class="dropdown-item"
          :class="{
            'selected': isSelected(option.id),
            'disabled': option.enabled === false
          }"
          @click="toggleOption(option)"
        >
          <input
            type="checkbox"
            :checked="isSelected(option.id)"
            :disabled="option.enabled === false"
            class="checkbox"
          />
          <span class="option-label">{{ option.name }}</span>
        </div>
        <div v-if="displayOptions.length === 0" class="dropdown-empty">
          暂无可用的搜索空间
        </div>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="text-xs text-red-600">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
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
  disabled?: boolean
}

// Props & Emits
const props = withDefaults(defineProps<Props>(), {
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
const selectedValues = ref<string[]>([...props.modelValue])
const availableOptions = ref<SearchSpaceOption[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const isOpen = ref(false)
const isFocused = ref(false)

// Computed - 使用本地加载的选项或传入的选项
const displayOptions = computed(() => {
  return props.options?.length ? props.options : availableOptions.value
})

// 选中的选项对象列表
const selectedOptions = computed(() => {
  return displayOptions.value.filter(opt => selectedValues.value.includes(opt.id))
})

// Watch for prop changes
watch(() => props.modelValue, (newValue) => {
  selectedValues.value = [...newValue]
}, { deep: true })

// Watch for local changes and emit
watch(selectedValues, (newValue) => {
  emits('update:modelValue', newValue)

  // 同步到 store - 使用 selectSearchSpaces 方法
  store.selectSearchSpaces(newValue)
}, { deep: true })

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

    console.log('MultiSearchSpaceSelector: 开始加载搜索空间列表...')

    // 使用与 SearchDataManagePage 相同的API调用
    // 注意:http拦截器已经返回了response.data,所以result就是ApiResponse对象
    const result = await http.get('/search-spaces', {
      params: {
        page: 0,
        size: 100
      }
    }) as any

    console.log('MultiSearchSpaceSelector: 搜索空间API响应:', result)

    // 处理后端返回的ApiResponse格式
    if (result.success && result.data && result.data.content) {
      const spaces = result.data.content.map((space: BackendSearchSpace) => ({
        id: space.id.toString(),
        name: space.name,
        description: space.description,
        fields: [],
        enabled: true,
        indexStatus: 'healthy' as const,
        docCount: 0
      }))

      availableOptions.value = spaces

      // 重要：将搜索空间列表同步到store中
      store.setSearchSpaces(spaces)

      console.log('MultiSearchSpaceSelector: 搜索空间列表加载成功:', availableOptions.value)
    } else {
      console.warn('MultiSearchSpaceSelector: 搜索空间API返回格式异常:', result)
      throw new Error(result.message || '获取搜索空间列表失败')
    }
  } catch (err: any) {
    console.error('MultiSearchSpaceSelector: 加载搜索空间列表时发生错误:', err)

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

function toggleDropdown() {
  if (loading.value || !displayOptions.value.length || props.disabled) {
    return
  }
  isOpen.value = !isOpen.value
  isFocused.value = isOpen.value
}

function toggleOption(option: SearchSpaceOption) {
  if (option.enabled === false || props.disabled) {
    return
  }

  const index = selectedValues.value.indexOf(option.id)
  if (index > -1) {
    selectedValues.value.splice(index, 1)
  } else {
    selectedValues.value.push(option.id)
  }
}

function removeOption(optionId: string) {
  const index = selectedValues.value.indexOf(optionId)
  if (index > -1) {
    selectedValues.value.splice(index, 1)
  }
}

function isSelected(optionId: string): boolean {
  return selectedValues.value.includes(optionId)
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.multi-select-wrapper')) {
    isOpen.value = false
    isFocused.value = false
  }
}

// 组件挂载时加载搜索空间
onMounted(() => {
  loadAvailableSearchSpaces()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.multi-select-wrapper {
  @apply relative;
  /* 确保下拉菜单不被父容器裁剪 */
  z-index: 1;
}

.multi-select-control {
  @apply w-full min-h-[40px] px-3 py-2 border border-gray-300 rounded-md cursor-pointer transition-all;
  @apply flex items-center gap-2 flex-wrap;
  background: white;
}

.multi-select-control.disabled {
  @apply opacity-50 cursor-not-allowed bg-gray-50;
}

.multi-select-control.focused {
  @apply ring-2 ring-emerald-500 border-emerald-500;
}

.multi-select-control:hover:not(.disabled) {
  @apply border-emerald-400;
}

.selected-tags {
  @apply flex items-center gap-2 flex-wrap flex-1;
}

.selected-tag {
  @apply inline-flex items-center gap-1 px-2 py-1 bg-emerald-100 text-emerald-800 rounded text-sm;
}

.remove-tag {
  @apply text-emerald-600 hover:text-emerald-800 font-bold text-lg leading-none;
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.placeholder {
  @apply text-gray-400 text-sm flex-1;
}

.dropdown-arrow {
  @apply w-5 h-5 text-gray-400 transition-transform flex-shrink-0;
}

.dropdown-arrow.open {
  @apply transform rotate-180;
}

.dropdown-menu {
  @apply absolute top-full left-0 right-0 mt-1 bg-white border border-gray-300 rounded-md shadow-lg;
  @apply max-h-60 overflow-y-auto;
  z-index: 9999;
}

.dropdown-item {
  @apply flex items-center gap-2 px-3 py-2 cursor-pointer transition-colors;
}

.dropdown-item:hover:not(.disabled) {
  @apply bg-emerald-50;
}

.dropdown-item.selected {
  @apply bg-emerald-100;
}

.dropdown-item.disabled {
  @apply opacity-50 cursor-not-allowed;
}

.checkbox {
  @apply w-4 h-4 text-emerald-600 rounded border-gray-300 focus:ring-emerald-500;
}

.option-label {
  @apply text-sm text-gray-700 flex-1;
}

.dropdown-empty {
  @apply px-3 py-2 text-sm text-gray-500 text-center;
}

/* 滚动条样式 */
.dropdown-menu::-webkit-scrollbar {
  width: 6px;
}

.dropdown-menu::-webkit-scrollbar-track {
  @apply bg-gray-100;
}

.dropdown-menu::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
}

.dropdown-menu::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}
</style>