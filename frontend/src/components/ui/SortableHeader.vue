<template>
  <button
    type="button"
    class="sortable-header"
    :class="{
      'is-active': isActive,
      'is-ascending': isActive && currentSort.direction === 'asc',
      'is-descending': isActive && currentSort.direction === 'desc'
    }"
    @click="handleSort"
  >
    <span class="header-text">
      <slot />
    </span>
    <div class="sort-icons">
      <svg
        class="sort-icon sort-asc"
        :class="{ 'active': isActive && currentSort.direction === 'asc' }"
        viewBox="0 0 24 24"
        width="12"
        height="12"
      >
        <path fill="currentColor" d="M7 14l5-5 5 5z"/>
      </svg>
      <svg
        class="sort-icon sort-desc"
        :class="{ 'active': isActive && currentSort.direction === 'desc' }"
        viewBox="0 0 24 24"
        width="12"
        height="12"
      >
        <path fill="currentColor" d="M7 10l5 5 5-5z"/>
      </svg>
    </div>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  field: string
  currentSort: {
    field: string
    direction: 'asc' | 'desc'
  }
}

interface Emits {
  (e: 'sort', field: string, direction: 'asc' | 'desc'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

/**
 * 是否为当前排序字段
 */
const isActive = computed(() => props.currentSort.field === props.field)

/**
 * 处理排序点击
 */
const handleSort = () => {
  let direction: 'asc' | 'desc' = 'asc'

  if (isActive.value) {
    // 如果当前字段已激活，切换排序方向
    direction = props.currentSort.direction === 'asc' ? 'desc' : 'asc'
  }

  emit('sort', props.field, direction)
}
</script>

<style scoped>
.sortable-header {
  @apply
    inline-flex
    items-center
    space-x-1
    text-left
    hover:bg-green-50
    rounded
    px-2
    py-1
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:ring-opacity-50;
}

.sortable-header:hover {
  @apply text-green-700;
}

.header-text {
  @apply font-medium text-sm;
}

.sort-icons {
  @apply flex flex-col justify-center items-center ml-1;
}

.sort-icon {
  @apply text-gray-400 transition-colors duration-200;
  line-height: 1;
}

.sort-icon.active {
  @apply text-green-600;
}

.sortable-header:hover .sort-icon {
  @apply text-green-500;
}

.is-active {
  @apply text-green-600;
}

.is-active .header-text {
  @apply font-semibold;
}

/* 确保图标垂直对齐 */
.sort-asc {
  margin-bottom: -1px;
}

.sort-desc {
  margin-top: -1px;
}
</style>