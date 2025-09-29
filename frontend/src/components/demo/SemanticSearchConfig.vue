<template>
  <div class="space-y-4">
    <!-- 启用语义搜索 -->
    <div class="flex items-center justify-between">
      <Label class="text-xs">启用语义搜索</Label>
      <Switch v-model:checked="localConfig.enabled" />
    </div>

    <!-- 语义搜索配置 - 只有在启用时显示 -->
    <div v-if="localConfig.enabled" class="space-y-3 pt-2 border-t border-gray-200">
      <!-- 语义权重滑块 -->
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <Label class="text-xs">语义搜索权重</Label>
          <span class="text-xs text-muted-foreground">{{ (localConfig.weight * 100).toFixed(0) }}%</span>
        </div>

        <!-- 滑块容器 -->
        <div class="relative">
          <!-- 滑块轨道 -->
          <div class="w-full h-2 bg-gray-200 rounded-full relative">
            <!-- 进度条 -->
            <div
              class="h-2 bg-emerald-500 rounded-full transition-all duration-200"
              :style="{ width: `${localConfig.weight * 100}%` }"
            ></div>

            <!-- 滑块按钮 -->
            <div
              class="absolute top-1/2 w-4 h-4 bg-white border-2 border-emerald-500 rounded-full transform -translate-y-1/2 cursor-pointer shadow-sm hover:shadow-md transition-all duration-200"
              :style="{ left: `calc(${localConfig.weight * 100}% - 8px)` }"
              @mousedown="startDrag"
            ></div>
          </div>

          <!-- 隐藏的input用于实际控制 -->
          <input
            ref="sliderInput"
            v-model.number="localConfig.weight"
            type="range"
            min="0"
            max="1"
            step="0.1"
            class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            @input="updateWeight"
          />
        </div>

        <!-- 权重提示 -->
        <div class="flex justify-between text-xs text-muted-foreground">
          <span>关键词</span>
          <span>语义</span>
        </div>

        <!-- 权重说明 -->
        <p class="text-xs text-muted-foreground">
          权重越高，越依赖语义理解；权重越低，越依赖关键词匹配
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useVModel } from '@vueuse/core'

import Label from '@/components/ui/label/Label.vue'
import { Switch } from '@/components/ui/switch'

import type { SemanticSearchConfig } from '@/types/demo'

// Props & Emits
interface Props {
  modelValue: SemanticSearchConfig
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emits = defineEmits<{
  'update:modelValue': [value: SemanticSearchConfig]
}>()

// Local state
const localConfig = useVModel(props, 'modelValue', emits, {
  passive: true,
  deep: true
})

const sliderInput = ref<HTMLInputElement>()
const isDragging = ref(false)

// Methods
const updateWeight = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.weight = parseFloat(target.value)
}

const startDrag = (event: MouseEvent) => {
  isDragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  event.preventDefault()
}

const onDrag = (event: MouseEvent) => {
  if (!isDragging.value || !sliderInput.value) return

  const rect = sliderInput.value.getBoundingClientRect()
  const percent = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width))
  localConfig.value.weight = Math.round(percent * 10) / 10 // 保留一位小数
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

// 计算属性用于显示权重百分比
const weightPercentage = computed(() => {
  return Math.round(localConfig.value.weight * 100)
})

// 监听语义搜索启用状态，自动设置为AUTO模式
watch(() => localConfig.value.enabled, (enabled) => {
  if (enabled) {
    localConfig.value.mode = 'AUTO'
  }
}, { immediate: true })
</script>

<style scoped>
/* 自定义滑块样式 */
.range-slider {
  -webkit-appearance: none;
  appearance: none;
  background: transparent;
  cursor: pointer;
}

.range-slider::-webkit-slider-track {
  background: #e5e7eb;
  height: 8px;
  border-radius: 4px;
}

.range-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  background: #10b981;
  height: 16px;
  width: 16px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.range-slider::-moz-range-track {
  background: #e5e7eb;
  height: 8px;
  border-radius: 4px;
  border: none;
}

.range-slider::-moz-range-thumb {
  background: #10b981;
  height: 16px;
  width: 16px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 淡绿色主题 */
.bg-emerald-500 {
  background-color: rgb(16 185 129);
}

.border-emerald-500 {
  border-color: rgb(16 185 129);
}

.text-emerald-600 {
  color: rgb(5 150 105);
}

.hover\:shadow-md:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>