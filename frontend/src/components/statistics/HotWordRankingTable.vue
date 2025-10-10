<template>
  <div
    ref="containerRef"
    class="hot-word-ranking-table w-full h-full relative"
    :class="containerClasses"
  >
    <!-- 表格头部 -->
    <div
      class="table-header sticky top-0 z-10 bg-white border-b border-gray-200"
      :class="headerClasses"
    >
      <div class="flex items-center justify-between px-4 py-3">
        <!-- 标题和统计信息 -->
        <div class="flex items-center space-x-4">
          <h3 class="text-lg font-semibold text-gray-900">热词排行榜</h3>
          <div class="text-sm text-gray-500">
            <span>共 {{ totalCount }} 个热词</span>
            <span v-if="isVirtualized" class="ml-2">
              (显示 {{ virtualState.startIndex + 1 }}-{{ Math.min(virtualState.endIndex + 1, totalCount) }})
            </span>
          </div>
        </div>

        <!-- 工具栏 -->
        <div class="flex items-center space-x-2">
          <!-- 显示模式切换 -->
          <div class="flex rounded-md border border-gray-300 bg-white">
            <button
              @click="viewMode = 'compact'"
              :class="[
                'px-3 py-1 text-sm font-medium transition-colors',
                viewMode === 'compact'
                  ? 'bg-emerald-500 text-white'
                  : 'text-gray-600 hover:text-gray-900'
              ]"
            >
              紧凑
            </button>
            <button
              @click="viewMode = 'comfortable'"
              :class="[
                'px-3 py-1 text-sm font-medium transition-colors border-l border-gray-300',
                viewMode === 'comfortable'
                  ? 'bg-emerald-500 text-white'
                  : 'text-gray-600 hover:text-gray-900'
              ]"
            >
              舒适
            </button>
          </div>

          <!-- 排序选择 -->
          <select
            v-model="sortBy"
            class="px-3 py-1 text-sm border border-gray-300 rounded-md bg-white focus:ring-emerald-500 focus:border-emerald-500"
            @change="handleSortChange"
          >
            <option value="weight">按权重排序</option>
            <option value="text">按名称排序</option>
            <option value="trend">按趋势排序</option>
          </select>

          <!-- 刷新按钮 -->
          <button
            @click="$emit('refresh')"
            :disabled="loading"
            class="p-1.5 text-gray-500 hover:text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            title="刷新数据"
          >
            <svg
              class="w-4 h-4"
              :class="{ 'animate-spin': loading }"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 表头 -->
      <div class="grid grid-cols-12 gap-4 px-4 py-2 text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border-t border-gray-200">
        <div class="col-span-1 text-center">排名</div>
        <div class="col-span-4">热词</div>
        <div class="col-span-2 text-right">搜索次数</div>
        <div class="col-span-2 text-center" v-if="showTrend">趋势</div>
        <div class="col-span-2 text-center" v-if="showChange">变化</div>
        <div class="col-span-1 text-center">操作</div>
      </div>
    </div>

    <!-- 虚拟滚动容器 -->
    <div
      class="table-body flex-1 overflow-auto"
      :style="{ height: `${containerHeight}px` }"
    >
      <!-- 上方占位 -->
      <div
        v-if="isVirtualized && offsetTop > 0"
        :style="{ height: `${offsetTop}px` }"
        class="virtual-spacer-top"
      />

      <!-- 可见项目列表 -->
      <div class="virtual-items">
        <HotWordRankingRow
          v-for="item in visibleItems"
          :key="`${item.id}-${item.index}`"
          :item="item"
          :index="item.index"
          :view-mode="viewMode"
          :show-trend="showTrend"
          :show-change="showChange"
          :clickable="clickable"
          @click="handleItemClick"
          @hover="handleItemHover"
        />
      </div>

      <!-- 下方占位 -->
      <div
        v-if="isVirtualized && offsetBottom > 0"
        :style="{ height: `${offsetBottom}px` }"
        class="virtual-spacer-bottom"
      />

      <!-- 加载状态 -->
      <div
        v-if="loading && visibleItems.length === 0"
        class="flex items-center justify-center py-12"
      >
        <div class="flex flex-col items-center space-y-3">
          <div class="w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />
          <p class="text-sm text-gray-500">正在加载热词数据...</p>
        </div>
      </div>

      <!-- 空数据状态 -->
      <div
        v-if="!loading && visibleItems.length === 0"
        class="flex items-center justify-center py-12"
      >
        <div class="text-center">
          <div class="w-16 h-16 mx-auto mb-4 text-gray-400">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold text-gray-700 mb-2">暂无数据</h3>
          <p class="text-gray-500">当前没有可显示的热词数据</p>
        </div>
      </div>
    </div>

    <!-- 底部工具栏 -->
    <div
      v-if="showPagination && !isVirtualized"
      class="table-footer border-t border-gray-200 bg-white px-4 py-3"
    >
      <div class="flex items-center justify-between">
        <div class="text-sm text-gray-500">
          显示第 {{ (currentPage - 1) * pageSize + 1 }}-{{ Math.min(currentPage * pageSize, totalCount) }} 条，共 {{ totalCount }} 条
        </div>

        <div class="flex items-center space-x-2">
          <button
            @click="goToPage(currentPage - 1)"
            :disabled="currentPage <= 1"
            class="px-3 py-1 text-sm border border-gray-300 rounded-md bg-white text-gray-500 hover:text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            上一页
          </button>

          <div class="flex items-center space-x-1">
            <template v-for="page in visiblePages" :key="page">
              <button
                v-if="page !== '...'"
                @click="goToPage(page as number)"
                :class="[
                  'px-3 py-1 text-sm border rounded-md transition-colors',
                  page === currentPage
                    ? 'bg-emerald-500 text-white border-emerald-500'
                    : 'bg-white text-gray-500 border-gray-300 hover:text-gray-700'
                ]"
              >
                {{ page }}
              </button>
              <span v-else class="px-2 text-gray-400">...</span>
            </template>
          </div>

          <button
            @click="goToPage(currentPage + 1)"
            :disabled="currentPage >= totalPages"
            class="px-3 py-1 text-sm border border-gray-300 rounded-md bg-white text-gray-500 hover:text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 性能监控面板（开发模式） -->
    <div
      v-if="showDebugInfo"
      class="absolute bottom-4 right-4 bg-black/80 text-white text-xs p-3 rounded-lg font-mono"
    >
      <div>总数: {{ totalCount }}</div>
      <div>可见: {{ visibleItems.length }}</div>
      <div>滚动: {{ virtualState.scrollTop }}px</div>
      <div>渲染: {{ renderTime }}ms</div>
      <div v-if="isVirtualized">虚拟: {{ virtualState.startIndex }}-{{ virtualState.endIndex }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, type PropType } from 'vue'
import { useFixedVirtualScroll, createVirtualScrollItem, type VirtualScrollItem } from '@/composables/useVirtualScroll'
import { usePerformanceMonitor } from '@/composables/usePerformanceMonitor'
import type { RankingItem, RankingTableConfig } from '@/types/statistics'
import HotWordRankingRow from './ranking/HotWordRankingRow.vue'

// ============= Props =============

const props = defineProps({
  /** 排行榜数据 */
  items: {
    type: Array as PropType<RankingItem[]>,
    default: () => []
  },
  /** 排行榜配置 */
  config: {
    type: Object as PropType<RankingTableConfig>,
    default: () => ({
      pageSize: 50,
      showTrend: true,
      showChange: true,
      clickable: true
    })
  },
  /** 是否启用虚拟滚动 */
  enableVirtualScroll: {
    type: Boolean,
    default: true
  },
  /** 虚拟滚动阈值 */
  virtualScrollThreshold: {
    type: Number,
    default: 100
  },
  /** 项目高度 */
  itemHeight: {
    type: Number,
    default: 56
  },
  /** 容器高度 */
  height: {
    type: Number,
    default: 400
  },
  /** 加载状态 */
  loading: {
    type: Boolean,
    default: false
  },
  /** 错误信息 */
  error: {
    type: String,
    default: null
  },
  /** 是否显示分页 */
  showPagination: {
    type: Boolean,
    default: false
  },
  /** 是否显示调试信息 */
  showDebugInfo: {
    type: Boolean,
    default: false
  }
})

// ============= Emits =============

const emit = defineEmits<{
  /** 项目点击事件 */
  itemClick: [item: RankingItem, index: number]
  /** 项目悬停事件 */
  itemHover: [item: RankingItem, index: number]
  /** 排序变化事件 */
  sortChange: [sortBy: string, sortOrder: 'asc' | 'desc']
  /** 分页变化事件 */
  pageChange: [page: number, pageSize: number]
  /** 刷新事件 */
  refresh: []
}>()

// ============= 模板引用 =============

const containerRef = ref<HTMLElement | null>(null)

// ============= 响应式状态 =============

/** 视图模式 */
const viewMode = ref<'compact' | 'comfortable'>('comfortable')

/** 排序字段 */
const sortBy = ref('weight')

/** 排序顺序 */
const sortOrder = ref<'asc' | 'desc'>('desc')

/** 当前页码 */
const currentPage = ref(1)

/** 容器高度 */
const containerHeight = ref(props.height)

// ============= 性能监控 =============

const { startMeasure, endMeasure, getLastMeasurement } = usePerformanceMonitor()

const renderTime = computed(() => {
  const measurement = getLastMeasurement('table-render')
  return measurement ? Math.round(measurement.duration) : 0
})

// ============= 虚拟滚动设置 =============

/** 是否启用虚拟滚动 */
const isVirtualized = computed(() => {
  return props.enableVirtualScroll && props.items.length >= props.virtualScrollThreshold
})

/** 虚拟滚动项目 */
const virtualScrollItems = computed<VirtualScrollItem[]>(() => {
  return props.items.map((item, index) =>
    createVirtualScrollItem(
      `${item.text}-${item.rank}`,
      { ...item, originalIndex: index },
      getItemHeight()
    )
  )
})

/** 获取项目高度 */
const getItemHeight = (): number => {
  const baseHeight = props.itemHeight
  return viewMode.value === 'compact' ? baseHeight * 0.8 : baseHeight
}

// ============= 虚拟滚动逻辑 =============

const {
  virtualState,
  isScrolling,
  visibleItems: rawVisibleItems,
  offsetTop,
  offsetBottom,
  scrollToIndex,
  scrollToTop,
  debug: virtualDebug
} = useFixedVirtualScroll(
  containerRef,
  virtualScrollItems,
  {
    itemSize: getItemHeight(),
    bufferSize: 200,
    containerHeight: containerHeight.value,
    throttleDelay: 16
  }
)

// ============= 数据处理 =============

/** 可见项目（适配虚拟滚动和普通模式） */
const visibleItems = computed(() => {
  startMeasure('table-render')

  let items: any[]

  if (isVirtualized.value) {
    // 虚拟滚动模式
    items = rawVisibleItems.value.map(item => ({
      ...item.data,
      id: item.id,
      index: item.index,
      renderHeight: item.computedHeight
    }))
  } else {
    // 普通分页模式
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    items = props.items.slice(start, end).map((item, index) => ({
      ...item,
      id: `${item.text}-${item.rank}`,
      index: start + index,
      renderHeight: getItemHeight()
    }))
  }

  endMeasure('table-render')
  return items
})

/** 总数量 */
const totalCount = computed(() => props.items.length)

/** 页面大小 */
const pageSize = computed(() => props.config.pageSize)

/** 总页数 */
const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value))

/** 是否显示趋势 */
const showTrend = computed(() => props.config.showTrend)

/** 是否显示变化 */
const showChange = computed(() => props.config.showChange)

/** 是否可点击 */
const clickable = computed(() => props.config.clickable)

// ============= 分页逻辑 =============

/** 可见页码 */
const visiblePages = computed(() => {
  const pages: (number | string)[] = []
  const current = currentPage.value
  const total = totalPages.value

  if (total <= 7) {
    // 总页数较少，显示全部
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 总页数较多，显示部分
    pages.push(1)

    if (current > 4) {
      pages.push('...')
    }

    const start = Math.max(2, current - 1)
    const end = Math.min(total - 1, current + 1)

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }

    if (current < total - 3) {
      pages.push('...')
    }

    if (total > 1) {
      pages.push(total)
    }
  }

  return pages
})

// ============= 样式类 =============

const containerClasses = computed(() => [
  'border border-gray-200 rounded-lg bg-white shadow-sm',
  {
    'overflow-hidden': true,
    'opacity-75': props.loading
  }
])

const headerClasses = computed(() => [
  {
    'shadow-sm': isScrolling.value
  }
])

// ============= 事件处理 =============

/** 处理项目点击 */
const handleItemClick = (item: RankingItem, index: number) => {
  if (clickable.value) {
    emit('itemClick', item, index)
  }
}

/** 处理项目悬停 */
const handleItemHover = (item: RankingItem, index: number) => {
  emit('itemHover', item, index)
}

/** 处理排序变化 */
const handleSortChange = () => {
  emit('sortChange', sortBy.value, sortOrder.value)
}

/** 跳转到指定页 */
const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    emit('pageChange', page, pageSize.value)
  }
}

// ============= 公共方法 =============

/** 滚动到顶部 */
const scrollToTopMethod = () => {
  if (isVirtualized.value) {
    scrollToTop()
  } else if (containerRef.value) {
    containerRef.value.scrollTop = 0
  }
}

/** 滚动到指定排名 */
const scrollToRank = (rank: number) => {
  const index = props.items.findIndex(item => item.rank === rank)
  if (index >= 0) {
    if (isVirtualized.value) {
      scrollToIndex(index)
    } else {
      const page = Math.ceil((index + 1) / pageSize.value)
      goToPage(page)
    }
  }
}

// ============= 监听器 =============

// 监听视图模式变化，更新项目高度
watch(viewMode, () => {
  nextTick(() => {
    // 触发虚拟滚动重新计算
    if (isVirtualized.value && containerRef.value) {
      const event = new Event('resize')
      window.dispatchEvent(event)
    }
  })
})

// 监听容器高度变化
watch(
  () => props.height,
  (newHeight) => {
    containerHeight.value = newHeight
  }
)

// ============= 生命周期 =============

onMounted(async () => {
  await nextTick()

  // 更新容器高度
  if (containerRef.value) {
    const rect = containerRef.value.getBoundingClientRect()
    if (rect.height > 0) {
      containerHeight.value = rect.height
    }
  }
})

// ============= 公开方法 =============

defineExpose({
  /** 滚动到顶部 */
  scrollToTop: scrollToTopMethod,
  /** 滚动到指定排名 */
  scrollToRank,
  /** 跳转到指定页 */
  goToPage,
  /** 获取虚拟滚动状态 */
  getVirtualState: () => virtualState,
  /** 获取调试信息 */
  getDebugInfo: () => ({
    ...virtualDebug.value,
    isVirtualized: isVirtualized.value,
    renderTime: renderTime.value
  })
})
</script>

<style scoped>
.hot-word-ranking-table {
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.table-header {
  flex-shrink: 0;
}

.table-body {
  flex: 1;
  min-height: 0;
}

.table-footer {
  flex-shrink: 0;
}

.virtual-spacer-top,
.virtual-spacer-bottom {
  flex-shrink: 0;
}

.virtual-items {
  position: relative;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hot-word-ranking-table {
    min-height: 150px;
  }

  .grid-cols-12 {
    grid-template-columns: 1fr 3fr 2fr 1fr;
  }

  .col-span-1 {
    grid-column: span 1;
  }

  .col-span-4 {
    grid-column: span 1;
  }

  .col-span-2 {
    grid-column: span 1;
  }
}

@media (max-width: 480px) {
  .hot-word-ranking-table {
    min-height: 120px;
  }

  .grid-cols-12 {
    grid-template-columns: 1fr 2fr 1fr;
  }
}

/* 滚动条样式 */
.table-body::-webkit-scrollbar {
  width: 6px;
}

.table-body::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.table-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.table-body::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 平滑过渡 */
.virtual-items {
  transition: transform 0.1s ease-out;
}

/* 加载状态 */
.loading-overlay {
  backdrop-filter: blur(2px);
}
</style>