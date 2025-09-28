<template>
  <div class="hot-word-filter">
    <!-- 过滤器标题栏 -->
    <div class="hot-word-filter__header">
      <div class="filter-header-content">
        <h3 class="filter-title">热词过滤器</h3>
        <div class="filter-actions">
          <button
            @click="toggleCollapse"
            class="collapse-button"
            type="button"
            :title="isCollapsed ? '展开过滤器' : '收起过滤器'"
          >
            {{ isCollapsed ? '展开' : '收起' }}
          </button>
          <button
            @click="resetFilter"
            class="reset-button"
            type="button"
            :disabled="loading || !hasActiveFilters"
          >
            重置
          </button>
        </div>
      </div>
    </div>

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
              <option value="">全部时间</option>
              <option value="today">今天</option>
              <option value="yesterday">昨天</option>
              <option value="last7days">最近7天</option>
              <option value="last30days">最近30天</option>
              <option value="custom">自定义</option>
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

        <!-- 关键词搜索 -->
        <div class="filter-group">
          <label class="filter-label">关键词</label>
          <div class="keyword-input-wrapper">
            <input
              v-model="filterData.keyword"
              type="text"
              placeholder="输入关键词搜索..."
              class="keyword-input"
              :disabled="loading"
              @input="debounceKeywordChange"
            />
          </div>
        </div>

        <!-- 数据量限制 -->
        <div class="filter-group">
          <label class="filter-label">数据量限制</label>
          <div class="limit-selector">
            <select
              v-model="filterData.limit"
              @change="handleLimitChange"
              class="limit-select"
              :disabled="loading"
            >
              <option :value="50">50条</option>
              <option :value="100">100条</option>
              <option :value="200">200条</option>
              <option :value="500">500条</option>
            </select>
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
  keyword: '',
  limit: 100
})

// 计算属性
const hasActiveFilters = computed(() => {
  return !!(
    filterData.timeRange ||
    filterData.keyword ||
    filterData.limit !== 100
  )
})

// 防抖关键词输入
let keywordTimeout: number | null = null
const debounceKeywordChange = () => {
  if (keywordTimeout) {
    clearTimeout(keywordTimeout)
  }
  keywordTimeout = setTimeout(() => {
    // 自动应用过滤器
  }, 500) as unknown as number
}

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

const handleLimitChange = () => {
  // 处理数据量限制变化
}

const applyFilter = () => {
  emit('filter', { ...filterData })
}

const resetFilter = () => {
  Object.assign(filterData, {
    timeRange: '',
    customStartDate: '',
    customEndDate: '',
    keyword: '',
    limit: 100
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
.limit-select,
.date-input,
.keyword-input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.time-range-select:focus,
.limit-select:focus,
.date-input:focus,
.keyword-input:focus {
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

.keyword-input-wrapper {
  position: relative;
}

.keyword-input {
  width: 100%;
}

.limit-selector {
  display: flex;
  align-items: center;
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