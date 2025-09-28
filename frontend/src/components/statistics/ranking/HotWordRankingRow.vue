<template>
  <div
    class="hot-word-ranking-row border-b border-gray-100 transition-all duration-200"
    :class="rowClasses"
    :style="rowStyle"
    @click="handleClick"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <div class="grid grid-cols-12 gap-4 px-4 py-3 items-center">
      <!-- 排名 -->
      <div class="col-span-1 text-center">
        <div class="flex items-center justify-center">
          <!-- 排名徽章 -->
          <div
            class="rank-badge"
            :class="rankBadgeClasses"
          >
            {{ item.rank }}
          </div>

          <!-- 排名变化指示器 -->
          <div
            v-if="showRankChange && item.rankChange"
            class="ml-1 flex items-center"
          >
            <svg
              class="w-3 h-3"
              :class="rankChangeClasses"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path
                v-if="item.rankChange > 0"
                d="M7 14l5-5 5 5z"
              />
              <path
                v-else
                d="M7 10l5 5 5-5z"
              />
            </svg>
            <span class="text-xs font-medium ml-0.5">
              {{ Math.abs(item.rankChange) }}
            </span>
          </div>
        </div>
      </div>

      <!-- 热词 -->
      <div class="col-span-4">
        <div class="flex items-center space-x-3">
          <!-- 热词文本 */
          <div class="flex-1 min-w-0">
            <div
              class="font-medium text-gray-900 truncate"
              :class="{ 'text-sm': viewMode === 'compact' }"
              :title="item.text"
            >
              {{ item.text }}
            </div>

            <!-- 额外信息（舒适模式） -->
            <div
              v-if="viewMode === 'comfortable' && (item.category || item.tags)"
              class="flex items-center mt-1 space-x-2"
            >
              <span
                v-if="item.category"
                class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800"
              >
                {{ item.category }}
              </span>

              <div
                v-if="item.tags && item.tags.length > 0"
                class="flex items-center space-x-1"
              >
                <span
                  v-for="tag in item.tags.slice(0, 2)"
                  :key="tag"
                  class="inline-flex items-center px-1.5 py-0.5 rounded text-xs bg-emerald-100 text-emerald-800"
                >
                  {{ tag }}
                </span>
                <span
                  v-if="item.tags.length > 2"
                  class="text-xs text-gray-500"
                >
                  +{{ item.tags.length - 2 }}
                </span>
              </div>
            </div>
          </div>

          <!-- 热度指示器 */
          <div class="flex-shrink-0">
            <div
              class="w-2 h-8 rounded-full bg-gradient-to-t from-emerald-200 to-emerald-500"
              :style="{
                background: `linear-gradient(to top, #a7f3d0, #10b981 ${heatLevel}%)`
              }"
              :title="`热度: ${heatLevel}%`"
            />
          </div>
        </div>
      </div>

      <!-- 搜索次数 -->
      <div class="col-span-2 text-right">
        <div
          class="font-semibold text-gray-900"
          :class="{ 'text-sm': viewMode === 'compact' }"
        >
          {{ formatNumber(item.weight) }}
        </div>

        <!-- 增长率（舒适模式） -->
        <div
          v-if="viewMode === 'comfortable' && item.growthRate !== undefined"
          class="text-xs mt-1"
          :class="growthRateClasses"
        >
          <span class="inline-flex items-center">
            <svg
              v-if="item.growthRate !== 0"
              class="w-3 h-3 mr-0.5"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path
                v-if="item.growthRate > 0"
                d="M7 14l5-5 5 5z"
              />
              <path
                v-else
                d="M7 10l5 5 5-5z"
              />
            </svg>
            {{ formatGrowthRate(item.growthRate) }}
          </span>
        </div>
      </div>

      <!-- 趋势 -->
      <div
        v-if="showTrend"
        class="col-span-2 text-center"
      >
        <div class="flex items-center justify-center space-x-1">
          <!-- 趋势图标 -->
          <div
            class="p-1.5 rounded-full"
            :class="trendBadgeClasses"
          >
            <svg
              class="w-4 h-4"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <!-- 上升趋势 -->
              <path
                v-if="item.trend === 'up' || item.trend === 'rising'"
                d="M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z"
              />
              <!-- 下降趋势 -->
              <path
                v-else-if="item.trend === 'down' || item.trend === 'falling'"
                d="M16 18l2.29-2.29-4.88-4.88-4 4L2 7.41 3.41 6l6 6 4-4 6.3 6.29L22 12v6z"
              />
              <!-- 新热词 -->
              <path
                v-else-if="item.trend === 'new'"
                d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
              />
              <!-- 稳定 -->
              <path
                v-else
                d="M3 12h18v2H3z"
              />
            </svg>
          </div>

          <!-- 趋势文本 -->
          <span
            class="text-xs font-medium"
            :class="trendTextClasses"
          >
            {{ trendText }}
          </span>
        </div>
      </div>

      <!-- 变化 -->
      <div
        v-if="showChange"
        class="col-span-2 text-center"
      >
        <div
          v-if="item.change !== undefined"
          class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium"
          :class="changeBadgeClasses"
        >
          <svg
            v-if="item.change !== 0"
            class="w-3 h-3 mr-1"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path
              v-if="item.change > 0"
              d="M7 14l5-5 5 5z"
            />
            <path
              v-else
              d="M7 10l5 5 5-5z"
            />
          </svg>
          {{ formatChange(item.change) }}
        </div>

        <span
          v-else
          class="text-xs text-gray-400"
        >
          -
        </span>
      </div>

      <!-- 操作 -->
      <div class="col-span-1 text-center">
        <div class="flex items-center justify-center space-x-1">
          <!-- 详情按钮 -->
          <button
            v-if="clickable"
            @click.stop="handleDetailClick"
            class="p-1 text-gray-400 hover:text-emerald-600 transition-colors"
            title="查看详情"
          >
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
            </svg>
          </button>

          <!-- 更多操作 -->
          <div class="relative" v-if="showMoreActions">
            <button
              @click.stop="toggleMoreActions"
              class="p-1 text-gray-400 hover:text-gray-600 transition-colors"
              title="更多操作"
            >
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
              </svg>
            </button>

            <!-- 下拉菜单 -->
            <div
              v-if="showActionsMenu"
              class="absolute right-0 top-full mt-1 w-32 bg-white border border-gray-200 rounded-md shadow-lg z-10"
              @click.stop
            >
              <button
                @click="handleCompareClick"
                class="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                对比分析
              </button>
              <button
                @click="handleExportClick"
                class="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 transition-colors"
              >
                导出数据
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 悬停效果线条 -->
    <div
      v-if="isHovered"
      class="absolute left-0 top-0 w-1 h-full bg-emerald-500 transition-all duration-200"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, type PropType } from 'vue'
import type { RankingItem } from '@/types/statistics'

// ============= Props =============

const props = defineProps({
  /** 排行项目数据 */
  item: {
    type: Object as PropType<RankingItem>,
    required: true
  },
  /** 项目索引 */
  index: {
    type: Number,
    required: true
  },
  /** 视图模式 */
  viewMode: {
    type: String as PropType<'compact' | 'comfortable'>,
    default: 'comfortable'
  },
  /** 是否显示趋势 */
  showTrend: {
    type: Boolean,
    default: true
  },
  /** 是否显示变化 */
  showChange: {
    type: Boolean,
    default: true
  },
  /** 是否可点击 */
  clickable: {
    type: Boolean,
    default: true
  },
  /** 是否显示更多操作 */
  showMoreActions: {
    type: Boolean,
    default: true
  }
})

// ============= Emits =============

const emit = defineEmits<{
  /** 点击事件 */
  click: [item: RankingItem, index: number]
  /** 悬停事件 */
  hover: [item: RankingItem, index: number]
  /** 详情点击事件 */
  detail: [item: RankingItem, index: number]
  /** 对比点击事件 */
  compare: [item: RankingItem, index: number]
  /** 导出点击事件 */
  export: [item: RankingItem, index: number]
}>()

// ============= 响应式状态 =============

const isHovered = ref(false)
const showActionsMenu = ref(false)

// ============= 计算属性 =============

/** 行样式类 */
const rowClasses = computed(() => [
  {
    'hover:bg-gray-50': props.clickable,
    'cursor-pointer': props.clickable,
    'relative': true,
    'bg-emerald-50': isTopRank.value,
    'ring-1 ring-emerald-200': isTopRank.value && isHovered.value
  }
])

/** 行内联样式 */
const rowStyle = computed(() => ({
  height: props.viewMode === 'compact' ? '48px' : '64px'
}))

/** 是否为前三名 */
const isTopRank = computed(() => props.item.rank <= 3)

/** 排名徽章样式类 */
const rankBadgeClasses = computed(() => {
  const base = 'flex items-center justify-center w-8 h-8 rounded-full text-sm font-bold'

  if (props.item.rank === 1) {
    return `${base} bg-yellow-100 text-yellow-800 border-2 border-yellow-300`
  } else if (props.item.rank === 2) {
    return `${base} bg-gray-100 text-gray-800 border-2 border-gray-300`
  } else if (props.item.rank === 3) {
    return `${base} bg-orange-100 text-orange-800 border-2 border-orange-300`
  } else {
    return `${base} bg-emerald-100 text-emerald-800`
  }
})

/** 是否显示排名变化 */
const showRankChange = computed(() => {
  return props.item.rankChange !== undefined && props.item.rankChange !== 0
})

/** 排名变化样式类 */
const rankChangeClasses = computed(() => {
  if (!props.item.rankChange) return ''

  return props.item.rankChange > 0
    ? 'text-red-500' // 排名上升（数字变小）显示红色
    : 'text-green-500' // 排名下降（数字变大）显示绿色
})

/** 热度级别（百分比） */
const heatLevel = computed(() => {
  // 假设权重在某个范围内，计算相对热度
  const maxWeight = 10000 // 可以从父组件传入最大权重
  return Math.min(100, (props.item.weight / maxWeight) * 100)
})

/** 增长率样式类 */
const growthRateClasses = computed(() => {
  if (props.item.growthRate === undefined) return 'text-gray-500'

  if (props.item.growthRate > 0) {
    return 'text-green-600'
  } else if (props.item.growthRate < 0) {
    return 'text-red-600'
  } else {
    return 'text-gray-500'
  }
})

/** 趋势徽章样式类 */
const trendBadgeClasses = computed(() => {
  const trend = props.item.trend

  if (trend === 'up' || trend === 'rising') {
    return 'bg-green-100 text-green-600'
  } else if (trend === 'down' || trend === 'falling') {
    return 'bg-red-100 text-red-600'
  } else if (trend === 'new') {
    return 'bg-blue-100 text-blue-600'
  } else {
    return 'bg-gray-100 text-gray-600'
  }
})

/** 趋势文本样式类 */
const trendTextClasses = computed(() => {
  const trend = props.item.trend

  if (trend === 'up' || trend === 'rising') {
    return 'text-green-600'
  } else if (trend === 'down' || trend === 'falling') {
    return 'text-red-600'
  } else if (trend === 'new') {
    return 'text-blue-600'
  } else {
    return 'text-gray-600'
  }
})

/** 趋势文本 */
const trendText = computed(() => {
  const trend = props.item.trend

  switch (trend) {
    case 'up':
    case 'rising':
      return '上升'
    case 'down':
    case 'falling':
      return '下降'
    case 'new':
      return '新'
    case 'stable':
    default:
      return '稳定'
  }
})

/** 变化徽章样式类 */
const changeBadgeClasses = computed(() => {
  if (props.item.change === undefined || props.item.change === 0) {
    return 'bg-gray-100 text-gray-600'
  }

  return props.item.change > 0
    ? 'bg-green-100 text-green-700'
    : 'bg-red-100 text-red-700'
})

// ============= 格式化函数 =============

/** 格式化数字 */
const formatNumber = (num: number): string => {
  if (num >= 1000000) {
    return `${(num / 1000000).toFixed(1)}M`
  } else if (num >= 1000) {
    return `${(num / 1000).toFixed(1)}K`
  } else {
    return num.toString()
  }
}

/** 格式化增长率 */
const formatGrowthRate = (rate: number): string => {
  const sign = rate > 0 ? '+' : ''
  return `${sign}${rate.toFixed(1)}%`
}

/** 格式化变化值 */
const formatChange = (change: number): string => {
  const sign = change > 0 ? '+' : ''
  return `${sign}${change}`
}

// ============= 事件处理 =============

/** 处理点击事件 */
const handleClick = () => {
  if (props.clickable) {
    emit('click', props.item, props.index)
  }
}

/** 处理鼠标进入 */
const handleMouseEnter = () => {
  isHovered.value = true
  emit('hover', props.item, props.index)
}

/** 处理鼠标离开 */
const handleMouseLeave = () => {
  isHovered.value = false
  showActionsMenu.value = false
}

/** 处理详情点击 */
const handleDetailClick = () => {
  emit('detail', props.item, props.index)
}

/** 处理对比点击 */
const handleCompareClick = () => {
  showActionsMenu.value = false
  emit('compare', props.item, props.index)
}

/** 处理导出点击 */
const handleExportClick = () => {
  showActionsMenu.value = false
  emit('export', props.item, props.index)
}

/** 切换更多操作菜单 */
const toggleMoreActions = () => {
  showActionsMenu.value = !showActionsMenu.value
}
</script>

<style scoped>
.hot-word-ranking-row {
  position: relative;
  transition: all 0.2s ease;
}

.rank-badge {
  transition: all 0.2s ease;
}

.hot-word-ranking-row:hover .rank-badge {
  transform: scale(1.05);
}

/* 移动端适配 */
@media (max-width: 768px) {
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
  .grid-cols-12 {
    grid-template-columns: 1fr 2fr 1fr;
  }
}

/* 动画效果 */
@keyframes rankHighlight {
  0% { background-color: transparent; }
  50% { background-color: rgba(16, 185, 129, 0.1); }
  100% { background-color: transparent; }
}

.hot-word-ranking-row.rank-updated {
  animation: rankHighlight 1s ease-in-out;
}

/* 热度指示器动画 */
.heat-indicator {
  position: relative;
  overflow: hidden;
}

.heat-indicator::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, transparent 30%, rgba(255, 255, 255, 0.3) 50%, transparent 70%);
  transform: translateX(-100%);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  to {
    transform: translateX(100%);
  }
}
</style>