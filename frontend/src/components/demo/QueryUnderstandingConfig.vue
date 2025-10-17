<template>
  <div class="space-y-4">
    <!-- 启用查询理解管道 -->
    <div class="flex items-center justify-between">
      <Label class="text-xs">启用查询理解管道</Label>
      <Switch v-model:checked="localConfig.enabled" />
    </div>

    <!-- 查询理解管道配置 - 只有在启用时显示 -->
    <div v-if="localConfig.enabled" class="space-y-3 pt-2 border-t border-gray-200">
      <!-- 使用增强查询进行搜索 -->
      <div class="flex items-center justify-between">
        <div class="flex-1">
          <Label class="text-xs">使用增强查询执行搜索</Label>
          <p class="text-xs text-muted-foreground mt-0.5">
            使用增强改写后的查询进行实际搜索和语义排序
          </p>
        </div>
        <Switch v-model:checked="localConfig.useEnhancedQuery" />
      </div>

      <!-- 保留原始查询用于高亮 -->
      <div class="flex items-center justify-between">
        <div class="flex-1">
          <Label class="text-xs">保留原始查询用于高亮</Label>
          <p class="text-xs text-muted-foreground mt-0.5">
            高亮显示时使用原始查询字符串而非增强后的查询
          </p>
        </div>
        <Switch v-model:checked="localConfig.keepOriginalForHighlight" />
      </div>

      <!-- 说明 -->
      <div class="mt-3 p-3 bg-emerald-50 border border-emerald-200 rounded-md">
        <p class="text-xs text-emerald-700">
          <strong>查询理解管道说明：</strong>
        </p>
        <ul class="text-xs text-emerald-600 mt-2 space-y-1 list-disc list-inside">
          <li>通过LLM增强并重写用户查询</li>
          <li>支持意图识别、实体提取、同义词扩展等</li>
          <li>增强查询用于实际搜索，提高召回率和相关性</li>
          <li>原始查询用于高亮显示，保持用户输入的可见性</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'

import Label from '@/components/ui/label/Label.vue'
import { Switch } from '@/components/ui/switch'
import type { QueryUnderstandingConfig } from '@/types/demo'

// Props & Emits
interface Props {
  modelValue: QueryUnderstandingConfig
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
})

const emits = defineEmits<{
  'update:modelValue': [value: QueryUnderstandingConfig]
}>()

// Local state
const localConfig = useVModel(props, 'modelValue', emits, {
  passive: true,
  deep: true
})
</script>

<style scoped>
/* 淡绿色主题 */
.bg-emerald-50 {
  background-color: rgb(236 253 245);
}

.border-emerald-200 {
  border-color: rgb(167 243 208);
}

.text-emerald-700 {
  color: rgb(4 120 87);
}

.text-emerald-600 {
  color: rgb(5 150 105);
}
</style>
