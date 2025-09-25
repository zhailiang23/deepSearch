<template>
  <div class="pagination-control bg-white border border-gray-200 rounded-lg p-4">
    <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
      <!-- 左侧信息 -->
      <div class="flex items-center gap-4 text-sm text-gray-600">
        <!-- 每页数量选择 -->
        <div class="flex items-center gap-2">
          <span class="whitespace-nowrap">每页显示</span>
          <select
            :value="pageSize"
            @change="handlePageSizeChange"
            :disabled="loading"
            class="border border-gray-300 rounded-md px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <option 
              v-for="size in pageSizeOptions"
              :key="size"
              :value="size"
            >
              {{ size }} 条
            </option>
          </select>
        </div>
        
        <!-- 数据统计 -->
        <div class="text-gray-600 whitespace-nowrap">
          <template v-if="total > 0">
            共 {{ total }} 条，第 {{ startRecord }}-{{ endRecord }} 条
          </template>
          <template v-else>
            暂无数据
          </template>
        </div>
      </div>
      
      <!-- 右侧分页控制 -->
      <div class="flex items-center gap-2" v-if="totalPages > 1">
        <!-- 第一页 -->
        <Button
          variant="outline"
          size="sm"
          :disabled="currentPage <= 1 || loading"
          @click="goToPage(1)"
          class="text-gray-600 border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronsLeft class="w-4 h-4" />
        </Button>
        
        <!-- 上一页 -->
        <Button
          variant="outline"
          size="sm"
          :disabled="currentPage <= 1 || loading"
          @click="goToPage(currentPage - 1)"
          class="text-gray-600 border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronLeft class="w-4 h-4" />
          <span class="hidden sm:inline ml-1">上一页</span>
        </Button>
        
        <!-- 页码按钮 -->
        <div class="flex items-center gap-1">
          <template v-for="page in visiblePages" :key="page">
            <Button
              v-if="typeof page === 'number'"
              :variant="page === currentPage ? 'default' : 'outline'"
              size="sm"
              :disabled="loading"
              @click="goToPage(page)"
              :class="[
                page === currentPage
                  ? 'bg-emerald-600 text-white border-emerald-600 hover:bg-emerald-700'
                  : 'text-gray-600 border-gray-300 hover:bg-gray-50',
                'min-w-8 disabled:opacity-50 disabled:cursor-not-allowed'
              ]"
            >
              {{ page }}
            </Button>
            <span
              v-else
              class="px-2 py-1 text-gray-400 text-sm"
            >
              ...
            </span>
          </template>
        </div>
        
        <!-- 下一页 -->
        <Button
          variant="outline"
          size="sm"
          :disabled="currentPage >= totalPages || loading"
          @click="goToPage(currentPage + 1)"
          class="text-gray-600 border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <span class="hidden sm:inline mr-1">下一页</span>
          <ChevronRight class="w-4 h-4" />
        </Button>
        
        <!-- 最后一页 -->
        <Button
          variant="outline"
          size="sm"
          :disabled="currentPage >= totalPages || loading"
          @click="goToPage(totalPages)"
          class="text-gray-600 border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <ChevronsRight class="w-4 h-4" />
        </Button>
        
        <!-- 页码跳转 -->
        <div class="hidden md:flex items-center gap-2 ml-4 border-l border-gray-200 pl-4">
          <span class="text-sm text-gray-600 whitespace-nowrap">跳至</span>
          <input
            v-model.number="jumpPage"
            type="number"
            :min="1"
            :max="totalPages"
            :disabled="loading"
            class="w-16 px-2 py-1 border border-gray-300 rounded-md text-sm text-center focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed"
            @keyup.enter="handleJumpPage"
          >
          <span class="text-sm text-gray-600">页</span>
          <Button
            variant="outline"
            size="sm"
            :disabled="loading || !jumpPage || jumpPage < 1 || jumpPage > totalPages"
            @click="handleJumpPage"
            class="text-gray-600 border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            跳转
          </Button>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="flex items-center gap-2 text-emerald-600 text-sm">
        <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-emerald-600"></div>
        <span>加载中...</span>
      </div>
    </div>
    
    <!-- 移动端页码跳转 -->
    <div v-if="totalPages > 1" class="md:hidden mt-4 pt-4 border-t border-gray-200">
      <div class="flex items-center justify-center gap-2">
        <span class="text-sm text-gray-600">跳至</span>
        <input
          v-model.number="jumpPage"
          type="number"
          :min="1"
          :max="totalPages"
          :disabled="loading"
          class="w-16 px-2 py-1 border border-gray-300 rounded-md text-sm text-center focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed"
          @keyup.enter="handleJumpPage"
        >
        <span class="text-sm text-gray-600">页 (共{{ totalPages }}页)</span>
        <Button
          variant="outline"
          size="sm"
          :disabled="loading || !jumpPage || jumpPage < 1 || jumpPage > totalPages"
          @click="handleJumpPage"
          class="text-gray-600 border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          跳转
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { 
  ChevronLeft, 
  ChevronRight, 
  ChevronsLeft, 
  ChevronsRight 
} from 'lucide-vue-next'

interface Props {
  currentPage: number
  pageSize: number
  total: number
  loading?: boolean
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  showTotal?: boolean
  pageSizeOptions?: number[]
  maxVisiblePages?: number
}

interface Emits {
  (e: 'update:currentPage', page: number): void
  (e: 'update:pageSize', size: number): void
  (e: 'change', page: number, size: number): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: true,
  pageSizeOptions: () => [10, 20, 50, 100],
  maxVisiblePages: 7
})

const emit = defineEmits<Emits>()

// 跳转页码
const jumpPage = ref<number>()

// 计算属性
const totalPages = computed(() => Math.ceil(props.total / props.pageSize))

const startRecord = computed(() => {
  return props.total > 0 ? (props.currentPage - 1) * props.pageSize + 1 : 0
})

const endRecord = computed(() => {
  const end = props.currentPage * props.pageSize
  return Math.min(end, props.total)
})

// 可见页码计算
const visiblePages = computed(() => {
  const pages: (number | string)[] = []
  const { maxVisiblePages } = props
  const current = props.currentPage
  const total = totalPages.value

  if (total <= maxVisiblePages) {
    // 总页数小于等于最大显示页数，显示所有页码
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 总页数大于最大显示页数，需要智能显示
    const half = Math.floor(maxVisiblePages / 2)
    
    if (current <= half + 1) {
      // 当前页在前半部分
      for (let i = 1; i <= maxVisiblePages - 2; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    } else if (current >= total - half) {
      // 当前页在后半部分
      pages.push(1)
      pages.push('...')
      for (let i = total - maxVisiblePages + 3; i <= total; i++) {
        pages.push(i)
      }
    } else {
      // 当前页在中间
      pages.push(1)
      pages.push('...')
      for (let i = current - half + 1; i <= current + half - 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    }
  }

  return pages
})

// 页码跳转
function goToPage(page: number) {
  if (page < 1 || page > totalPages.value || page === props.currentPage) {
    return
  }
  
  emit('update:currentPage', page)
  emit('change', page, props.pageSize)
}

// 每页数量变更
function handlePageSizeChange(event: Event) {
  const target = event.target as HTMLSelectElement
  const newSize = parseInt(target.value)
  
  if (newSize === props.pageSize) return
  
  // 计算新的页码，保持当前记录位置尽可能不变
  const currentFirstRecord = (props.currentPage - 1) * props.pageSize + 1
  const newCurrentPage = Math.ceil(currentFirstRecord / newSize)
  
  emit('update:pageSize', newSize)
  emit('update:currentPage', newCurrentPage)
  emit('change', newCurrentPage, newSize)
}

// 处理跳转
function handleJumpPage() {
  if (jumpPage.value && jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    goToPage(jumpPage.value)
    jumpPage.value = undefined
  }
}

// 监听当前页变化，重置跳转输入
watch(() => props.currentPage, () => {
  jumpPage.value = undefined
})
</script>

<style scoped>
/* 页码按钮动画 */
.pagination-control button {
  transition: all 0.2s ease-in-out;
}

/* 输入框聚焦效果 */
input[type="number"]:focus {
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

/* 移除数字输入框的箭头 */
input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
}

/* 响应式优化 */
@media (max-width: 640px) {
  .pagination-control {
    padding: 1rem;
  }
  
  .pagination-control button {
    min-width: 32px;
    height: 32px;
  }
  
  .pagination-control input {
    width: 60px;
  }
}

/* 加载状态动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* 禁用状态优化 */
button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

input:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background-color: #f9fafb;
}

/* 省略号样式 */
.pagination-control span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
}
</style>