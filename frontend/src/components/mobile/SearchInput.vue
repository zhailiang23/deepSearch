<template>
  <div class="search-input-container">
    <div class="search-input-wrapper">
      <!-- 搜索图标 -->
      <div class="search-icon">
        <svg class="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m21 21-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>

      <!-- 搜索输入框 -->
      <input
        ref="inputRef"
        v-model="inputValue"
        type="search"
        :placeholder="placeholder"
        :disabled="disabled"
        class="search-input"
        @input="handleInput"
        @keyup.enter="handleEnter"
        @focus="handleFocus"
        @blur="handleBlur"
      />

      <!-- 清除按钮 -->
      <button
        v-if="showClearButton && inputValue.length > 0"
        class="clear-button"
        type="button"
        @click="handleClear"
      >
        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>

      <!-- 搜索按钮 -->
      <button
        v-if="showSearchButton"
        class="search-button"
        type="button"
        :disabled="disabled || !inputValue.trim()"
        @click="handleSearch"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m21 21-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </button>
    </div>

    <!-- 搜索建议/历史 -->
    <div
      v-if="showSuggestions && (suggestions.length > 0 || searchHistory.length > 0)"
      class="suggestions-dropdown"
    >
      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0 && !inputValue.trim()" class="suggestions-section">
        <div class="suggestions-header">
          <svg class="w-4 h-4 text-gray-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span class="text-sm font-medium text-gray-600">最近搜索</span>
          <button
            class="ml-auto text-xs text-emerald-600 hover:text-emerald-700"
            @click="clearHistory"
          >
            清空
          </button>
        </div>
        <div class="suggestions-list">
          <button
            v-for="(item, index) in searchHistory.slice(0, 5)"
            :key="index"
            class="suggestion-item history-item"
            @click="handleHistorySelect(item)"
          >
            <svg class="w-4 h-4 text-gray-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="flex-1 text-left truncate">{{ item }}</span>
          </button>
        </div>
      </div>

      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="suggestions-section">
        <div class="suggestions-header">
          <svg class="w-4 h-4 text-gray-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <span class="text-sm font-medium text-gray-600">搜索建议</span>
        </div>
        <div class="suggestions-list">
          <button
            v-for="(item, index) in suggestions"
            :key="index"
            class="suggestion-item"
            @click="handleSuggestionSelect(item)"
          >
            <svg class="w-4 h-4 text-emerald-500 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m21 21-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <span class="flex-1 text-left truncate">{{ item }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useFocus } from '@vueuse/core'

// 组件属性定义
export interface SearchInputProps {
  modelValue: string
  placeholder?: string
  showSearchButton?: boolean
  showClearButton?: boolean
  disabled?: boolean
  enableSuggestions?: boolean
  enableHistory?: boolean
}

const props = withDefaults(defineProps<SearchInputProps>(), {
  placeholder: '请输入搜索关键词...',
  showSearchButton: false,
  showClearButton: true,
  disabled: false,
  enableSuggestions: true,
  enableHistory: true
})

// 组件事件定义
const emit = defineEmits<{
  'update:modelValue': [value: string]
  search: [query: string]
  clear: []
  focus: []
  blur: []
  input: [value: string]
}>()

// 输入框引用
const inputRef = ref<HTMLInputElement>()
const { focused } = useFocus(inputRef)

// 输入值
const inputValue = ref(props.modelValue)

// 搜索历史
const searchHistory = ref<string[]>([])

// 搜索建议
const suggestions = ref<string[]>([])

// 是否显示建议框
const showSuggestions = computed(() => {
  return focused.value && props.enableSuggestions
})

// 监听外部值变化
watch(
  () => props.modelValue,
  (newValue) => {
    inputValue.value = newValue
  }
)

// 监听内部值变化，同步到外部
watch(inputValue, (newValue) => {
  emit('update:modelValue', newValue)
})

// 处理输入事件
const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  inputValue.value = target.value
  emit('input', target.value)

  // 生成搜索建议
  generateSuggestions(target.value)
}

// 处理回车键
const handleEnter = () => {
  if (inputValue.value.trim()) {
    handleSearch()
  }
}

// 处理搜索
const handleSearch = () => {
  const query = inputValue.value.trim()
  if (query) {
    addToHistory(query)
    emit('search', query)
    hideSuggestions()
  }
}

// 处理清除
const handleClear = () => {
  inputValue.value = ''
  emit('clear')
  inputRef.value?.focus()
}

// 处理聚焦
const handleFocus = () => {
  emit('focus')
}

// 处理失焦
const handleBlur = () => {
  // 延迟隐藏建议，以便点击建议项
  setTimeout(() => {
    emit('blur')
  }, 150)
}

// 处理历史记录选择
const handleHistorySelect = (item: string) => {
  inputValue.value = item
  emit('search', item)
  hideSuggestions()
}

// 处理建议选择
const handleSuggestionSelect = (item: string) => {
  inputValue.value = item
  emit('search', item)
  addToHistory(item)
  hideSuggestions()
}

// 隐藏建议框
const hideSuggestions = () => {
  inputRef.value?.blur()
}

// 添加到历史记录
const addToHistory = (query: string) => {
  if (!props.enableHistory || !query.trim()) return

  const trimmedQuery = query.trim()
  const index = searchHistory.value.indexOf(trimmedQuery)

  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }

  searchHistory.value.unshift(trimmedQuery)

  // 限制历史记录数量
  if (searchHistory.value.length > 10) {
    searchHistory.value = searchHistory.value.slice(0, 10)
  }

  // 保存到本地存储
  saveHistory()
}

// 清空历史记录
const clearHistory = () => {
  searchHistory.value = []
  saveHistory()
}

// 保存历史记录
const saveHistory = () => {
  try {
    localStorage.setItem('mobile-search-history', JSON.stringify(searchHistory.value))
  } catch (err) {
    console.warn('无法保存搜索历史:', err)
  }
}

// 加载历史记录
const loadHistory = () => {
  if (!props.enableHistory) return

  try {
    const saved = localStorage.getItem('mobile-search-history')
    if (saved) {
      searchHistory.value = JSON.parse(saved)
    }
  } catch (err) {
    console.warn('无法加载搜索历史:', err)
  }
}

// 生成搜索建议
const generateSuggestions = (query: string) => {
  if (!props.enableSuggestions || !query.trim()) {
    suggestions.value = []
    return
  }

  // 这里可以根据需要实现搜索建议逻辑
  // 暂时使用简单的示例建议
  const commonSuggestions = [
    '用户管理',
    '数据分析',
    '系统配置',
    '权限管理',
    '日志查看'
  ]

  suggestions.value = commonSuggestions.filter(item =>
    item.toLowerCase().includes(query.toLowerCase())
  )
}

// 处理点击外部
const handleClickOutside = (event: Event) => {
  const target = event.target as Element
  const container = inputRef.value?.closest('.search-input-container')

  if (container && !container.contains(target)) {
    hideSuggestions()
  }
}

// 组件挂载
onMounted(() => {
  loadHistory()
  document.addEventListener('click', handleClickOutside)
})

// 组件卸载
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 暴露方法给父组件
defineExpose({
  focus: () => inputRef.value?.focus(),
  blur: () => inputRef.value?.blur(),
  clear: handleClear
})
</script>

<style scoped>
.search-input-container {
  @apply relative w-full;
}

.search-input-wrapper {
  @apply relative flex items-center;
  @apply bg-white rounded-xl border border-gray-200;
  @apply shadow-sm hover:shadow-md transition-shadow duration-200;
  @apply focus-within:border-emerald-500 focus-within:ring-2 focus-within:ring-emerald-100;
}

.search-icon {
  @apply absolute left-3 z-10;
  @apply pointer-events-none;
}

.search-input {
  @apply w-full pl-10 pr-16 py-3;
  @apply text-base text-gray-900 placeholder-gray-500;
  @apply bg-transparent rounded-xl;
  @apply border-0 outline-none ring-0;
  @apply appearance-none;
}

.search-input:disabled {
  @apply bg-gray-50 text-gray-400 cursor-not-allowed;
}

.clear-button {
  @apply absolute right-10 z-10;
  @apply p-1 rounded-full;
  @apply hover:bg-gray-100 transition-colors duration-150;
  @apply flex items-center justify-center;
}

.search-button {
  @apply absolute right-2 z-10;
  @apply p-2 rounded-lg;
  @apply bg-emerald-500 hover:bg-emerald-600;
  @apply text-white;
  @apply transition-colors duration-150;
  @apply disabled:bg-gray-300 disabled:cursor-not-allowed;
}

.suggestions-dropdown {
  @apply absolute top-full left-0 right-0 z-20 mt-2;
  @apply bg-white rounded-lg shadow-lg border border-gray-200;
  @apply max-h-64 overflow-y-auto;
  @apply opacity-100 duration-150;
}

.suggestions-section {
  @apply border-b border-gray-100 last:border-b-0;
}

.suggestions-header {
  @apply flex items-center px-4 py-3;
  @apply bg-gray-50 border-b border-gray-100;
}

.suggestions-list {
  @apply py-1;
}

.suggestion-item {
  @apply w-full flex items-center px-4 py-3;
  @apply text-left text-gray-700;
  @apply hover:bg-emerald-50 hover:text-emerald-700;
  @apply transition-colors duration-150;
  @apply border-b border-gray-50 last:border-b-0;
}

.history-item {
  @apply text-gray-600;
}

.suggestion-item:active {
  @apply bg-emerald-100;
}

/* 移动端触摸优化 */
@media (max-width: 640px) {
  .search-input {
    @apply text-base; /* 防止 iOS 缩放 */
  }

  .suggestion-item {
    @apply py-4 min-h-[44px]; /* iOS 触摸目标最小高度 */
  }
}

/* iOS 搜索框样式重置 */
.search-input[type="search"] {
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
}

.search-input[type="search"]::-webkit-search-cancel-button,
.search-input[type="search"]::-webkit-search-decoration {
  -webkit-appearance: none;
  appearance: none;
}

/* 滚动条样式 */
.suggestions-dropdown::-webkit-scrollbar {
  @apply w-1;
}

.suggestions-dropdown::-webkit-scrollbar-track {
  @apply bg-gray-100 rounded-full;
}

.suggestions-dropdown::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full hover:bg-gray-400;
}
</style>