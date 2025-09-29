<template>
  <div class="hot-word-filter">
    

    <!-- 过滤器主体 -->
    <div v-show="!isCollapsed" class="hot-word-filter__body">
      <!-- 基础过滤条件 -->
      <div class="filter-section">
        <!-- 时间范围选择 -->
        <div class="filter-group">
          <label class="filter-label">时间范围</label>
          <div class="time-range-selector">
            <select
              v-model="filterData.timeRange"
              @change="handleTimeRangeChange"
              class="time-range-select"
              :disabled="loading"
            >
              <option value="last7days">最近7天</option>
              <option value="last30days">最近30天</option>
              <option value="last90days">最近90天</option>
            </select>
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

      <!-- 操作按钮 -->
      <div class="filter-actions-footer">
        <button
          @click="applyFilter"
          class="apply-button"
          type="button"
          :disabled="loading"
        >
          {{ loading ? '查询中...' : '应用过滤器' }}
        </button>
        <button
          @click="resetFilter"
          class="reset-button-footer"
          type="button"
          :disabled="loading"
        >
          重置
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'

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
  timeRange: '',
  customStartDate: '',
  customEndDate: '',
  limit: 5000
})

// 计算属性
const hasActiveFilters = computed(() => {
  return !!(
    filterData.timeRange ||
    filterData.limit !== 5000
  )
})


// 方法
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleTimeRangeChange = () => {
  if (filterData.timeRange !== 'custom') {
    filterData.customStartDate = ''
    filterData.customEndDate = ''
  }
}

const handleCustomDateChange = () => {
  // 验证日期范围
}

const applyFilter = () => {
  emit('filter', { ...filterData })
}

const resetFilter = () => {
  Object.assign(filterData, {
    timeRange: '',
    customStartDate: '',
    customEndDate: '',
    limit: 5000
  })
  emit('reset')
}

// 监听器
watch(() => props.loading, (newVal) => {
  if (newVal) {
    // 加载开始
  } else {
    // 加载结束
  }
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

.time-range-select,
.date-input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.time-range-select:focus,
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


.filter-actions-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

.apply-button {
  background: #10b981;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.apply-button:hover:not(:disabled) {
  background: #059669;
}

.apply-button:disabled {
  background: #9ca3af;
  cursor: not-allowed;
}

.reset-button-footer {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reset-button-footer:hover:not(:disabled) {
  background: #e5e7eb;
}

.reset-button-footer:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-section {
    grid-template-columns: 1fr;
  }

  .filter-actions-footer {
    flex-direction: column;
  }

  .apply-button,
  .reset-button-footer {
    width: 100%;
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