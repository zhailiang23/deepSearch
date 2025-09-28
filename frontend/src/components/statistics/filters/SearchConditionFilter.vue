<template>
  <div class="search-condition-filter">
    <label class="label">搜索条件</label>
    <div class="search-input-group">
      <div class="relative">
        <input
          v-model="localSearchKeyword"
          type="text"
          placeholder="输入关键词进行过滤..."
          class="search-input"
          @input="handleSearchChange"
          @keyup.enter="handleSearchSubmit"
        >
        <div class="search-icon">
          <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
        <button
          v-if="localSearchKeyword"
          @click="clearSearch"
          class="clear-btn"
          title="清除搜索"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 搜索建议（如果有历史搜索记录） -->
      <div v-if="showSuggestions && suggestions.length > 0" class="suggestions-dropdown">
        <ul class="suggestions-list">
          <li
            v-for="suggestion in suggestions"
            :key="suggestion"
            @click="selectSuggestion(suggestion)"
            class="suggestion-item"
          >
            <svg class="w-3 h-3 text-gray-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ suggestion }}
          </li>
        </ul>
      </div>
    </div>

    <!-- 搜索提示 -->
    <div class="search-hint">
      <span class="hint-text">
        支持模糊匹配，按 Enter 键立即搜索
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  searchKeyword: string
  suggestions?: string[]
  debounceMs?: number
}

interface Emits {
  (e: 'update:searchKeyword', value: string): void
  (e: 'search', keyword: string): void
}

const props = withDefaults(defineProps<Props>(), {
  suggestions: () => [],
  debounceMs: 300
})

const emit = defineEmits<Emits>()

const localSearchKeyword = ref(props.searchKeyword)
const showSuggestions = ref(false)
const debounceTimer = ref<number | null>(null)

/**
 * 过滤后的建议列表
 */
const suggestions = computed(() => {
  if (!localSearchKeyword.value || !props.suggestions.length) {
    return []
  }
  return props.suggestions
    .filter(suggestion =>
      suggestion.toLowerCase().includes(localSearchKeyword.value.toLowerCase()) &&
      suggestion !== localSearchKeyword.value
    )
    .slice(0, 5) // 最多显示5个建议
})

/**
 * 处理搜索输入变化（防抖）
 */
const handleSearchChange = () => {
  if (debounceTimer.value) {
    clearTimeout(debounceTimer.value)
  }

  debounceTimer.value = setTimeout(() => {
    emit('update:searchKeyword', localSearchKeyword.value)
    showSuggestions.value = localSearchKeyword.value.length > 0
  }, props.debounceMs)
}

/**
 * 处理搜索提交（回车键）
 */
const handleSearchSubmit = () => {
  if (debounceTimer.value) {
    clearTimeout(debounceTimer.value)
  }
  emit('update:searchKeyword', localSearchKeyword.value)
  emit('search', localSearchKeyword.value)
  showSuggestions.value = false
}

/**
 * 清除搜索
 */
const clearSearch = () => {
  localSearchKeyword.value = ''
  emit('update:searchKeyword', '')
  emit('search', '')
  showSuggestions.value = false
}

/**
 * 选择建议项
 */
const selectSuggestion = (suggestion: string) => {
  localSearchKeyword.value = suggestion
  emit('update:searchKeyword', suggestion)
  emit('search', suggestion)
  showSuggestions.value = false
}

/**
 * 点击外部关闭建议列表
 */
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as Element
  if (!target.closest('.search-condition-filter')) {
    showSuggestions.value = false
  }
}

// 监听props变化
watch(() => props.searchKeyword, (newVal) => {
  localSearchKeyword.value = newVal
})

// 生命周期
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (debounceTimer.value) {
    clearTimeout(debounceTimer.value)
  }
})
</script>

<style scoped>
.search-condition-filter {
  @apply space-y-2;
}

.label {
  @apply block text-sm font-medium text-gray-700;
}

.search-input-group {
  @apply relative;
}

.search-input {
  @apply
    w-full
    pl-10
    pr-10
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
    duration-200
    placeholder-gray-400;
}

.search-icon {
  @apply
    absolute
    left-3
    top-1/2
    transform
    -translate-y-1/2
    pointer-events-none;
}

.clear-btn {
  @apply
    absolute
    right-3
    top-1/2
    transform
    -translate-y-1/2
    text-gray-400
    hover:text-gray-600
    focus:outline-none
    focus:text-gray-600
    transition-colors
    duration-200;
}

/* 建议下拉框样式 */
.suggestions-dropdown {
  @apply
    absolute
    top-full
    left-0
    right-0
    mt-1
    bg-white
    border
    border-gray-200
    rounded-md
    shadow-lg
    z-50
    max-h-40
    overflow-y-auto;
}

.suggestions-list {
  @apply py-1;
}

.suggestion-item {
  @apply
    flex
    items-center
    px-3
    py-2
    text-sm
    text-gray-700
    hover:bg-green-50
    hover:text-green-800
    cursor-pointer
    transition-colors
    duration-150;
}

.suggestion-item:hover {
  @apply bg-green-50 text-green-800;
}

/* 搜索提示样式 */
.search-hint {
  @apply mt-1;
}

.hint-text {
  @apply text-xs text-gray-500;
}

/* 响应式优化 */
@media (max-width: 640px) {
  .search-input {
    @apply text-base; /* 防止iOS缩放 */
  }

  .suggestions-dropdown {
    @apply max-h-32;
  }

  .suggestion-item {
    @apply py-3; /* 增加触摸目标大小 */
  }
}

/* 无障碍优化 */
.search-input:focus + .search-icon {
  @apply text-green-500;
}

/* 加载状态样式 */
.search-input:disabled {
  @apply bg-gray-50 cursor-not-allowed;
}

/* 动画优化 */
.suggestions-dropdown {
  animation: slideDown 0.2s ease-out;
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
</style>