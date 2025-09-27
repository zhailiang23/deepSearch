<template>
  <div class="space-y-4">
    <!-- 拼音搜索状态说明 -->
    <div class="p-3 bg-emerald-50 rounded-lg">
      <div class="flex items-center gap-3">
        <div class="flex-shrink-0">
          <svg class="w-5 h-5 text-emerald-600" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
          </svg>
        </div>
        <div class="space-y-1">
          <h3 class="text-sm font-medium text-emerald-800">拼音搜索已启用</h3>
          <p class="text-xs text-emerald-700">
            系统默认开启智能拼音搜索，支持中文拼音输入进行搜索匹配
          </p>
        </div>
      </div>
    </div>

    <!-- 当前配置显示 -->
    <div class="p-3 bg-gray-50 rounded-lg">
      <h4 class="text-sm font-medium text-gray-700 mb-3">当前拼音搜索配置</h4>
      <div class="space-y-2 text-xs">
        <div class="flex justify-between items-center">
          <span class="text-gray-600">搜索模式:</span>
          <span class="font-medium text-emerald-700">智能匹配</span>
        </div>
        <div class="flex justify-between items-center">
          <span class="text-gray-600">声调处理:</span>
          <span class="font-medium text-emerald-700">忽略声调</span>
        </div>
        <div class="flex justify-between items-center">
          <span class="text-gray-600">分段处理:</span>
          <span class="font-medium text-emerald-700">支持分段</span>
        </div>
      </div>
    </div>

    <!-- 搜索示例 -->
    <div class="p-3 bg-blue-50 rounded-lg">
      <h4 class="text-sm font-medium text-blue-800 mb-3">搜索示例</h4>
      <div class="space-y-3 text-xs">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <span class="text-blue-700 font-medium">模糊匹配:</span>
            <code class="px-2 py-1 bg-blue-100 rounded">zh</code>
          </div>
          <p class="text-blue-600 ml-2">可匹配: 中国、中华、中文、众多</p>
        </div>
        <div>
          <div class="flex items-center gap-2 mb-1">
            <span class="text-blue-700 font-medium">分段搜索:</span>
            <code class="px-2 py-1 bg-blue-100 rounded">zh guo</code>
          </div>
          <p class="text-blue-600 ml-2">可匹配: 中国、中华民国</p>
        </div>
        <div>
          <div class="flex items-center gap-2 mb-1">
            <span class="text-blue-700 font-medium">声调忽略:</span>
            <code class="px-2 py-1 bg-blue-100 rounded">ma</code>
          </div>
          <p class="text-blue-600 ml-2">可匹配: 妈、麻、马、骂</p>
        </div>
      </div>
    </div>

    <!-- 优势说明 -->
    <div class="p-3 bg-yellow-50 rounded-lg">
      <h4 class="text-sm font-medium text-yellow-800 mb-2">智能搜索优势</h4>
      <ul class="space-y-1 text-xs text-yellow-700">
        <li class="flex items-start gap-2">
          <span class="text-yellow-600">•</span>
          <span>结合模糊和精确匹配，提供最佳搜索结果</span>
        </li>
        <li class="flex items-start gap-2">
          <span class="text-yellow-600">•</span>
          <span>自动适应用户输入习惯，提升搜索体验</span>
        </li>
        <li class="flex items-start gap-2">
          <span class="text-yellow-600">•</span>
          <span>支持首字母缩写、分段输入等多种方式</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { useVModel } from '@vueuse/core'

import type { PinyinSearchConfigProps, PinyinSearchConfig } from '@/types/demo'

// Props & Emits
const props = withDefaults(defineProps<PinyinSearchConfigProps>(), {
  disabled: false
})

const emits = defineEmits<{
  'update:modelValue': [value: PinyinSearchConfig]
}>()

// Local state - 设置默认值为智能匹配模式
const localConfig = useVModel(props, 'modelValue', emits, {
  passive: true,
  defaultValue: {
    enabled: true,
    mode: 'both', // 智能匹配
    toneIgnore: true, // 忽略声调
    segmentMatch: true // 支持分段
  }
})

// 确保组件挂载时设置正确的默认值
onMounted(() => {
  if (!localConfig.value || !localConfig.value.enabled) {
    localConfig.value = {
      enabled: true,
      mode: 'both',
      toneIgnore: true,
      segmentMatch: true
    }
  }
})
</script>

<style scoped>
/* 自定义单选按钮样式 */
input[type="radio"] {
  accent-color: rgb(16 185 129);
}

input[type="radio"]:focus {
  box-shadow: 0 0 0 2px rgb(16 185 129 / 0.2);
}

input[type="radio"]:disabled {
  opacity: 0.5;
}

/* 配置区域样式 */
.border-l-2 {
  border-left-width: 2px;
}

.border-emerald-200 {
  border-color: rgb(167 243 208);
}

/* 开关样式 */
.data-\[state\=checked\]\:bg-emerald-600[data-state="checked"] {
  background-color: rgb(5 150 105);
}

/* 示例代码样式 */
code {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

/* 过渡效果 */
.space-y-4 > * {
  transition: all 0.2s ease;
}

/* 悬停效果 */
label:hover {
  color: rgb(4 120 87);
}

/* 禁用状态样式 */
:disabled {
  cursor: not-allowed;
}
</style>