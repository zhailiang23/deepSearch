<template>
  <div class="space-y-4">
    <!-- 页面大小配置 -->
    <div class="space-y-2">
      <div class="flex items-center justify-between">
        <Label class="text-sm font-medium text-emerald-700">页面大小</Label>
        <span class="text-xs text-gray-500">{{ localConfig.pageSize }} 条/页</span>
      </div>

      <div class="space-y-2">
        <!-- 滑块控件 -->
        <div class="px-3">
          <input
            v-model.number="localConfig.pageSize"
            type="range"
            :min="minPageSize"
            :max="maxPageSize"
            step="5"
            class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer slider-emerald"
          />
          <div class="flex justify-between text-xs text-gray-500 mt-1">
            <span>{{ minPageSize }}</span>
            <span>{{ maxPageSize }}</span>
          </div>
        </div>

        <!-- 数值输入框 -->
        <div class="flex items-center gap-2">
          <Input
            v-model.number="localConfig.pageSize"
            type="number"
            :min="minPageSize"
            :max="maxPageSize"
            step="5"
            class="w-20 h-8 text-sm text-center"
          />
          <span class="text-sm text-gray-600">条记录</span>
        </div>
      </div>
    </div>


    <!-- 高级设置 -->
    <div class="space-y-3">
      <Label class="text-sm font-medium text-emerald-700">高级设置</Label>

      <!-- 初始加载数量 -->
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <Label class="text-sm">初始加载数量</Label>
          <span class="text-xs text-gray-500">{{ localConfig.initialLoad }} 条</span>
        </div>

        <div class="flex items-center gap-2">
          <Input
            v-model.number="localConfig.initialLoad"
            type="number"
            :min="minPageSize"
            :max="maxPageSize"
            step="5"
            class="w-20 h-8 text-sm text-center"
          />
          <span class="text-sm text-gray-600">条记录</span>
        </div>

        <div class="text-xs text-gray-500">
          首次搜索时加载的记录数，建议设置为页面大小的 1-2 倍
        </div>
      </div>

      <!-- 预加载下一页 -->
      <div class="flex items-center justify-between">
        <div class="space-y-1">
          <Label class="text-sm">预加载下一页</Label>
          <p class="text-xs text-gray-600">
            在用户浏览当前页时预加载下一页数据
          </p>
        </div>
        <Switch
          v-model:checked="localConfig.prefetchNext"
          class="data-[state=checked]:bg-emerald-600"
        />
      </div>
    </div>

    <!-- 性能提示 -->
    <div class="p-3 rounded-lg"
         :class="{
           'bg-green-50 border border-green-200': getPerformanceLevel() === 'good',
           'bg-yellow-50 border border-yellow-200': getPerformanceLevel() === 'warning',
           'bg-red-50 border border-red-200': getPerformanceLevel() === 'poor'
         }">
      <div class="flex items-start gap-2">
        <div class="flex-shrink-0 mt-0.5">
          <svg
            v-if="getPerformanceLevel() === 'good'"
            class="w-4 h-4 text-green-600"
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
          <svg
            v-else-if="getPerformanceLevel() === 'warning'"
            class="w-4 h-4 text-yellow-600"
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
          <svg
            v-else
            class="w-4 h-4 text-red-600"
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
        </div>
        <div class="flex-1">
          <h4 class="text-sm font-medium"
              :class="{
                'text-green-800': getPerformanceLevel() === 'good',
                'text-yellow-800': getPerformanceLevel() === 'warning',
                'text-red-800': getPerformanceLevel() === 'poor'
              }">
            {{ getPerformanceTitle() }}
          </h4>
          <p class="text-xs mt-1"
             :class="{
               'text-green-700': getPerformanceLevel() === 'good',
               'text-yellow-700': getPerformanceLevel() === 'warning',
               'text-red-700': getPerformanceLevel() === 'poor'
             }">
            {{ getPerformanceMessage() }}
          </p>
        </div>
      </div>
    </div>

    <!-- 配置摘要 -->
    <div class="p-3 bg-gray-50 rounded-lg">
      <h4 class="text-sm font-medium text-gray-700 mb-2">当前配置</h4>
      <div class="grid grid-cols-2 gap-2 text-xs">
        <div>
          <span class="text-gray-600">页面大小:</span>
          <span class="ml-1 font-medium">{{ localConfig.pageSize }}</span>
        </div>
        <div>
          <span class="text-gray-600">初始加载:</span>
          <span class="ml-1 font-medium">{{ localConfig.initialLoad }}</span>
        </div>
        <div>
          <span class="text-gray-600">预加载:</span>
          <span class="ml-1 font-medium">{{ localConfig.prefetchNext ? '启用' : '禁用' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useVModel } from '@vueuse/core'

import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import { Switch } from '@/components/ui/switch'

import type { PagingConfigProps, PaginationConfig } from '@/types/demo'

// Props & Emits
const props = withDefaults(defineProps<PagingConfigProps>(), {
  maxPageSize: 100,
  minPageSize: 5
})

const emits = defineEmits<{
  'update:modelValue': [value: PaginationConfig]
}>()

// Local state
const localConfig = useVModel(props, 'modelValue', emits, { passive: true })

// Watchers
watch(
  () => localConfig.value.pageSize,
  (newSize) => {
    // 确保初始加载数量不小于页面大小
    if (localConfig.value.initialLoad < newSize) {
      localConfig.value.initialLoad = newSize
    }
  }
)


// Computed
const performanceLevel = computed(() => {
  const pageSize = localConfig.value.pageSize
  const initialLoad = localConfig.value.initialLoad

  // 性能评估逻辑
  if (pageSize <= 20) {
    return 'good'
  } else if (pageSize <= 50) {
    return 'warning'
  } else if (pageSize > 50 || initialLoad > 100) {
    return 'poor'
  } else {
    return 'good'
  }
})

// Methods

function getPerformanceLevel(): 'good' | 'warning' | 'poor' {
  return performanceLevel.value
}

function getPerformanceTitle(): string {
  const titles = {
    good: '性能良好',
    warning: '性能一般',
    poor: '性能较差'
  }
  return titles[performanceLevel.value]
}

function getPerformanceMessage(): string {
  const pageSize = localConfig.value.pageSize

  if (performanceLevel.value === 'good') {
    return '当前配置能提供流畅的用户体验，加载速度和内存使用都在合理范围内。'
  } else if (performanceLevel.value === 'warning') {
    return '页面大小适中，在大多数情况下能提供良好的用户体验。'
  } else {
    if (pageSize > 50) {
      return `页面大小过大 (${pageSize})，建议不超过50条记录以确保良好的用户体验。`
    } else if (localConfig.value.initialLoad > 100) {
      return '初始加载数量过多，可能导致首次加载时间过长。'
    } else {
      return '当前配置可能导致性能问题，建议调整参数以提升用户体验。'
    }
  }
}
</script>

<style scoped>
/* 自定义滑块样式 */
.slider-emerald::-webkit-slider-thumb {
  appearance: none;
  height: 20px;
  width: 20px;
  border-radius: 50%;
  background: rgb(16 185 129);
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.slider-emerald::-webkit-slider-track {
  height: 8px;
  cursor: pointer;
  background: rgb(167 243 208);
  border-radius: 4px;
}

.slider-emerald::-moz-range-thumb {
  height: 20px;
  width: 20px;
  border-radius: 50%;
  background: rgb(16 185 129);
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.slider-emerald::-moz-range-track {
  height: 8px;
  cursor: pointer;
  background: rgb(167 243 208);
  border-radius: 4px;
}

/* 自定义单选按钮样式 */
input[type="radio"] {
  accent-color: rgb(16 185 129);
}

input[type="radio"]:focus {
  box-shadow: 0 0 0 2px rgb(16 185 129 / 0.2);
}

/* 过渡动画 */
.transition-colors {
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

/* 开关样式 */
.data-\[state\=checked\]\:bg-emerald-600[data-state="checked"] {
  background-color: rgb(5 150 105);
}

/* 输入框样式调整 */
input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
}

/* 悬停效果 */
label:hover {
  color: rgb(4 120 87);
}
</style>