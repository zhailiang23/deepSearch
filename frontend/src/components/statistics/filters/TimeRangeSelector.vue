<template>
  <div class="time-range-selector">
    <label class="label">时间范围</label>
    <div class="selector-content">
      <!-- 预设时间范围选择 -->
      <div class="preset-options">
        <button
          v-for="option in timeRangeOptions"
          :key="option.value"
          @click="selectPreset(option)"
          :class="[
            'preset-btn',
            {
              'preset-btn-active': selectedPreset === option.value,
              'preset-btn-inactive': selectedPreset !== option.value
            }
          ]"
        >
          {{ option.label }}
        </button>
      </div>

      <!-- 自定义时间范围 -->
      <div class="custom-range">
        <div class="date-input-group">
          <label class="date-label">开始时间</label>
          <input
            v-model="localStartTime"
            type="datetime-local"
            class="date-input"
            @change="handleCustomChange"
          >
        </div>
        <div class="date-input-group">
          <label class="date-label">结束时间</label>
          <input
            v-model="localEndTime"
            type="datetime-local"
            class="date-input"
            @change="handleCustomChange"
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { TimeRangeOption } from '@/types/statistics'

interface Props {
  startTime: string
  endTime: string
  preset?: string
}

interface Emits {
  (e: 'update:startTime', value: string): void
  (e: 'update:endTime', value: string): void
  (e: 'update:preset', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  preset: ''
})

const emit = defineEmits<Emits>()

const localStartTime = ref(props.startTime)
const localEndTime = ref(props.endTime)
const selectedPreset = ref(props.preset)

/**
 * 预设时间范围选项
 */
const timeRangeOptions: TimeRangeOption[] = [
  {
    label: '今天',
    value: 'today',
    startTime: '',
    endTime: ''
  },
  {
    label: '最近7天',
    value: 'last7days',
    startTime: '',
    endTime: ''
  },
  {
    label: '最近30天',
    value: 'last30days',
    startTime: '',
    endTime: ''
  },
  {
    label: '本月',
    value: 'thisMonth',
    startTime: '',
    endTime: ''
  },
  {
    label: '上月',
    value: 'lastMonth',
    startTime: '',
    endTime: ''
  }
]

/**
 * 获取预设时间范围的实际时间
 */
const getPresetTimes = (preset: string) => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

  switch (preset) {
    case 'today':
      return {
        startTime: today.toISOString().slice(0, 16),
        endTime: new Date(today.getTime() + 24 * 60 * 60 * 1000 - 1).toISOString().slice(0, 16)
      }
    case 'last7days':
      return {
        startTime: new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
        endTime: new Date(today.getTime() + 24 * 60 * 60 * 1000 - 1).toISOString().slice(0, 16)
      }
    case 'last30days':
      return {
        startTime: new Date(today.getTime() - 29 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16),
        endTime: new Date(today.getTime() + 24 * 60 * 60 * 1000 - 1).toISOString().slice(0, 16)
      }
    case 'thisMonth':
      const thisMonthStart = new Date(now.getFullYear(), now.getMonth(), 1)
      return {
        startTime: thisMonthStart.toISOString().slice(0, 16),
        endTime: new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59).toISOString().slice(0, 16)
      }
    case 'lastMonth':
      const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59)
      return {
        startTime: lastMonthStart.toISOString().slice(0, 16),
        endTime: lastMonthEnd.toISOString().slice(0, 16)
      }
    default:
      return {
        startTime: localStartTime.value,
        endTime: localEndTime.value
      }
  }
}

/**
 * 选择预设时间范围
 */
const selectPreset = (option: TimeRangeOption) => {
  selectedPreset.value = option.value
  const times = getPresetTimes(option.value)
  localStartTime.value = times.startTime
  localEndTime.value = times.endTime

  emit('update:preset', option.value)
  emit('update:startTime', times.startTime)
  emit('update:endTime', times.endTime)
}

/**
 * 处理自定义时间变化
 */
const handleCustomChange = () => {
  selectedPreset.value = ''
  emit('update:preset', '')
  emit('update:startTime', localStartTime.value)
  emit('update:endTime', localEndTime.value)
}

// 监听props变化
watch(() => props.startTime, (newVal) => {
  localStartTime.value = newVal
})

watch(() => props.endTime, (newVal) => {
  localEndTime.value = newVal
})

watch(() => props.preset, (newVal) => {
  selectedPreset.value = newVal
})
</script>

<style scoped>
.time-range-selector {
  @apply space-y-3;
}

.label {
  @apply block text-sm font-medium text-gray-700 mb-2;
}

.selector-content {
  @apply space-y-4;
}

/* 预设选项样式 */
.preset-options {
  @apply flex flex-wrap gap-2;
}

.preset-btn {
  @apply
    px-3
    py-1.5
    text-sm
    font-medium
    rounded-md
    border
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:ring-offset-2;
}

.preset-btn-active {
  @apply
    bg-green-100
    text-green-800
    border-green-300
    hover:bg-green-200;
}

.preset-btn-inactive {
  @apply
    bg-white
    text-gray-600
    border-gray-300
    hover:bg-gray-50
    hover:border-gray-400;
}

/* 自定义时间范围样式 */
.custom-range {
  @apply grid grid-cols-1 md:grid-cols-2 gap-4;
}

.date-input-group {
  @apply space-y-1;
}

.date-label {
  @apply block text-xs font-medium text-gray-600;
}

.date-input {
  @apply
    w-full
    px-3
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
    duration-200;
}

/* 响应式优化 */
@media (max-width: 640px) {
  .preset-options {
    @apply grid grid-cols-2 gap-2;
  }

  .preset-btn {
    @apply text-xs px-2 py-1;
  }

  .custom-range {
    @apply grid-cols-1;
  }
}
</style>