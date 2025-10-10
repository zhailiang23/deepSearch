<template>
    

    <!-- 过滤器主体 -->
    <div v-show="!isCollapsed" class="hot-word-filter__body">
      <!-- 基础过滤条件 -->
      <div class="filter-section">
        <!-- 时间范围选择 -->
        <div class="filter-group">
          <div class="time-range-buttons">
            <label class="time-range-button" :class="{ active: filterData.timeRange === 'last7days' }">
              <input
                type="radio"
                value="last7days"
                v-model="filterData.timeRange"
                @change="handleTimeRangeChange"
                :disabled="loading"
                class="sr-only"
              />
              <span>最近7天</span>
            </label>
            <label class="time-range-button" :class="{ active: filterData.timeRange === 'last30days' }">
              <input
                type="radio"
                value="last30days"
                v-model="filterData.timeRange"
                @change="handleTimeRangeChange"
                :disabled="loading"
                class="sr-only"
              />
              <span>最近30天</span>
            </label>
            <label class="time-range-button" :class="{ active: filterData.timeRange === 'last90days' }">
              <input
                type="radio"
                value="last90days"
                v-model="filterData.timeRange"
                @change="handleTimeRangeChange"
                :disabled="loading"
                class="sr-only"
              />
              <span>最近90天</span>
            </label>
          </div>
        </div>

        <!-- 自定义时间范围 -->
        <div v-if="filterData.timeRange === 'custom'" class="filter-group">
          <label class="filter-label">自定义时间</label>
          <div class="custom-time-range">
            <input
              v-model="filterData.customStartDate"
              type="date"
              class="date-input"
              :disabled="loading"
              @change="handleCustomDateChange"
            />
            <span class="date-separator">至</span>
            <input
              v-model="filterData.customEndDate"
              type="date"
              class="date-input"
              :disabled="loading"
              @change="handleCustomDateChange"
            />
          </div>
        </div>

      </div>

    </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'

// 导出的类型定义
export interface HotWordFilterData {
  timeRange?: {
    start: Date
    end: Date
    label?: string
  }
  searchCondition?: {
    keywords?: string[]
    userTypes?: string[]
    sortOrder?: string
  }
  limitConfig?: {
    limit?: number
    displayMode?: string
  }
}

// 组件属性
interface Props {
  loading?: boolean
  estimatedDataSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  estimatedDataSize: 0
})

// 组件事件
interface Emits {
  filter: [filterData: any]
  reset: []
}

const emit = defineEmits<Emits>()

// 响应式数据
const isCollapsed = ref(false)

const filterData = reactive({
  timeRange: 'last7days',
  customStartDate: '',
  customEndDate: '',
  limit: 5000
})



// 方法

const handleTimeRangeChange = () => {
  if (filterData.timeRange !== 'custom') {
    filterData.customStartDate = ''
    filterData.customEndDate = ''
  }
  // 自动触发过滤
  emit('filter', { ...filterData })
}

const handleCustomDateChange = () => {
  // 验证日期范围
  if (filterData.customStartDate && filterData.customEndDate) {
    // 自动触发过滤
    emit('filter', { ...filterData })
  }
}


// 初始化，自动触发默认过滤
const initializeFilter = () => {
  // 组件初始化时自动触发过滤，获取默认7天数据
  emit('filter', { ...filterData })
}

// 监听器
watch(() => props.loading, (newVal) => {
  if (newVal) {
    // 加载开始
  } else {
    // 加载结束
  }
})

// 暴露初始化方法
defineExpose({
  initializeFilter
})
</script>

<style scoped>
.hot-word-filter {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.hot-word-filter__header {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
  padding: 16px;
  color: white;
}

.filter-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.collapse-button,
.reset-button {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.collapse-button:hover,
.reset-button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
}

.reset-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hot-word-filter__body {
  padding: 20px;
}

.filter-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
}

.time-range-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.time-range-button {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 80px;
}

.time-range-button:hover {
  border-color: #10b981;
  background: #f0fdf4;
}

.time-range-button.active {
  background: #10b981;
  border-color: #10b981;
  color: white;
}

.time-range-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.date-input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.date-input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.custom-time-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  color: #6b7280;
  font-size: 14px;
}



/* 响应式设计 */
@media (max-width: 768px) {
  .filter-section {
    grid-template-columns: 1fr;
  }
}

/* 动画效果 */
.hot-word-filter {
  animation: slideIn 0.4s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>