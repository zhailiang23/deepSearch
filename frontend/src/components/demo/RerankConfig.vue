<template>
  <div class="space-y-4">
    <!-- 启用语义重排序 -->
    <div class="flex items-center justify-between">
      <Label class="text-xs">启用语义重排序</Label>
      <Switch v-model:checked="localConfig.enabled" />
    </div>

    <!-- 重排序配置 - 只有在启用时显示 -->
    <div v-if="localConfig.enabled" class="space-y-3 pt-2 border-t border-gray-200">
      <!-- Top-N配置 -->
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <Label class="text-xs">返回前N条结果</Label>
          <span class="text-xs text-muted-foreground">{{ localConfig.topN }} 条</span>
        </div>

        <!-- 滑块容器 -->
        <div class="relative">
          <!-- 滑块轨道 -->
          <div class="w-full h-2 bg-gray-200 rounded-full relative">
            <!-- 进度条 -->
            <div
              class="h-2 bg-green-500 rounded-full transition-all duration-200"
              :style="{ width: `${((localConfig.topN - 10) / 90) * 100}%` }"
            ></div>

            <!-- 滑块按钮 -->
            <div
              class="absolute top-1/2 w-4 h-4 bg-white border-2 border-green-500 rounded-full transform -translate-y-1/2 cursor-pointer shadow-sm hover:shadow-md transition-all duration-200"
              :style="{ left: `calc(${((localConfig.topN - 10) / 90) * 100}% - 8px)` }"
              @mousedown="startDrag"
            ></div>
          </div>

          <!-- 隐藏的input用于实际控制 -->
          <input
            ref="sliderInput"
            v-model.number="localConfig.topN"
            type="range"
            min="10"
            max="100"
            step="10"
            class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            @input="updateTopN"
          />
        </div>

        <!-- Top-N刻度 -->
        <div class="flex justify-between text-xs text-muted-foreground">
          <span>10</span>
          <span>50</span>
          <span>100</span>
        </div>

        <!-- 说明 -->
        <p class="text-xs text-muted-foreground">
          基于BGE-Reranker-v2-m3模型,对搜索结果按语义相关度重新排序
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useVModel } from '@vueuse/core'

import Label from '@/components/ui/label/Label.vue'
import { Switch } from '@/components/ui/switch'

// 重排序配置接口
export interface RerankConfig {
  enabled: boolean
  topN: number
}

// Props & Emits
interface Props {
  modelValue: RerankConfig
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emits = defineEmits<{
  'update:modelValue': [value: RerankConfig]
}>()

// Local state
const localConfig = useVModel(props, 'modelValue', emits, {
  passive: true,
  deep: true
})

const sliderInput = ref<HTMLInputElement>()
const isDragging = ref(false)

// Methods
const updateTopN = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.topN = parseInt(target.value)
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
  const value = Math.round((10 + percent * 90) / 10) * 10 // 10的倍数
  localConfig.value.topN = Math.max(10, Math.min(100, value))
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}
</script>

<style scoped>
/* 淡绿色主题 */
.bg-green-500 {
  background-color: rgb(16 185 129);
}

.border-green-500 {
  border-color: rgb(16 185 129);
}

.hover\:shadow-md:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
</style>
