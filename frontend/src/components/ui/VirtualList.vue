<template>
  <div 
    ref="containerRef" 
    class="virtual-list-container overflow-auto"
    :style="{ height: containerHeight + 'px' }"
    @scroll="handleScroll"
  >
    <!-- 占位高度，用于模拟总内容高度 -->
    <div 
      :style="{ 
        height: totalHeight + 'px',
        paddingTop: offsetY + 'px'
      }"
    >
      <!-- 可见区域渲染的项目 -->
      <div
        v-for="(item, index) in visibleItems"
        :key="getItemKey(item, startIndex + index)"
        :style="{ height: itemHeight + 'px' }"
        class="virtual-list-item flex items-center"
      >
        <slot 
          name="item" 
          :item="item" 
          :index="startIndex + index"
          :isVisible="true"
        />
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="virtual-list-loading flex items-center justify-center py-4">
      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-emerald-600"></div>
      <span class="ml-2 text-emerald-600">加载中...</span>
    </div>
    
    <!-- 空状态 -->
    <div 
      v-if="!loading && items.length === 0" 
      class="virtual-list-empty flex items-center justify-center py-8 text-gray-500"
    >
      <slot name="empty">
        <div class="text-center">
          <div class="text-4xl mb-2">📋</div>
          <div>暂无数据</div>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

interface Props {
  items: any[]
  itemHeight: number
  containerHeight?: number
  visibleCount?: number
  bufferSize?: number
  loading?: boolean
  keyField?: string
}

interface Emits {
  (e: 'scroll', payload: { scrollTop: number; direction: 'up' | 'down' }): void
  (e: 'reachBottom'): void
  (e: 'reachTop'): void
}

const props = withDefaults(defineProps<Props>(), {
  containerHeight: 400,
  visibleCount: 10,
  bufferSize: 5,
  loading: false,
  keyField: '_id'
})

const emit = defineEmits<Emits>()

// 响应式引用
const containerRef = ref<HTMLElement>()
const scrollTop = ref(0)
const lastScrollTop = ref(0)

// 计算属性
const totalHeight = computed(() => props.items.length * props.itemHeight)

const actualVisibleCount = computed(() => {
  return Math.min(
    props.visibleCount + props.bufferSize * 2,
    props.items.length
  )
})

const startIndex = computed(() => {
  const index = Math.floor(scrollTop.value / props.itemHeight) - props.bufferSize
  return Math.max(0, index)
})

const endIndex = computed(() => {
  const index = startIndex.value + actualVisibleCount.value
  return Math.min(props.items.length, index)
})

const visibleItems = computed(() => {
  return props.items.slice(startIndex.value, endIndex.value)
})

const offsetY = computed(() => {
  return startIndex.value * props.itemHeight
})

// 防抖处理
let scrollTimer: number | null = null
const scrollDebounceTime = 16 // 约60fps

const handleScroll = () => {
  if (scrollTimer) {
    cancelAnimationFrame(scrollTimer)
  }
  
  scrollTimer = requestAnimationFrame(() => {
    if (!containerRef.value) return
    
    const newScrollTop = containerRef.value.scrollTop
    const direction = newScrollTop > lastScrollTop.value ? 'down' : 'up'
    
    scrollTop.value = newScrollTop
    lastScrollTop.value = newScrollTop
    
    emit('scroll', { scrollTop: newScrollTop, direction })
    
    // 检查是否到达底部或顶部
    const { scrollHeight, clientHeight } = containerRef.value
    const reachBottomThreshold = 50
    const reachTopThreshold = 50
    
    if (scrollHeight - newScrollTop - clientHeight < reachBottomThreshold) {
      emit('reachBottom')
    }
    
    if (newScrollTop < reachTopThreshold) {
      emit('reachTop')
    }
  })
}

// 获取项目的唯一键
const getItemKey = (item: any, index: number): string => {
  if (props.keyField && item[props.keyField]) {
    return String(item[props.keyField])
  }
  return `virtual-item-${index}`
}

// 滚动到指定位置
const scrollTo = (offset: number) => {
  if (containerRef.value) {
    containerRef.value.scrollTop = offset
  }
}

// 滚动到指定项目
const scrollToItem = (index: number) => {
  const offset = index * props.itemHeight
  scrollTo(offset)
}

// 获取当前可见区域信息
const getVisibleRange = () => {
  return {
    start: startIndex.value,
    end: endIndex.value,
    visibleCount: visibleItems.value.length
  }
}

// 监听项目变化，保持滚动位置
watch(() => props.items.length, (newLength, oldLength) => {
  // 如果是数据刷新（长度减少），滚动到顶部
  if (newLength < oldLength) {
    nextTick(() => {
      scrollTo(0)
    })
  }
})

// 内存清理
onUnmounted(() => {
  if (scrollTimer) {
    cancelAnimationFrame(scrollTimer)
  }
})

// 暴露方法给父组件
defineExpose({
  scrollTo,
  scrollToItem,
  getVisibleRange
})
</script>

<style scoped>
.virtual-list-container {
  position: relative;
  width: 100%;
}

.virtual-list-item {
  width: 100%;
  box-sizing: border-box;
}

.virtual-list-loading {
  position: sticky;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

.virtual-list-empty {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

/* 滚动条样式优化 */
.virtual-list-container::-webkit-scrollbar {
  width: 6px;
}

.virtual-list-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.virtual-list-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
  transition: background 0.2s;
}

.virtual-list-container::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}
</style>