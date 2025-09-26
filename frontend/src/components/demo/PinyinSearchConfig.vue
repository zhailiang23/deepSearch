<template>
  <div class="space-y-4">
    <!-- 拼音搜索开关 -->
    <div class="flex items-center justify-between p-3 bg-emerald-50 rounded-lg">
      <div class="space-y-1">
        <Label class="text-sm font-medium text-emerald-800">启用拼音搜索</Label>
        <p class="text-xs text-emerald-700">
          支持中文拼音输入进行搜索匹配
        </p>
      </div>
      <Switch
        v-model:checked="localConfig.enabled"
        :disabled="disabled"
        class="data-[state=checked]:bg-emerald-600"
      />
    </div>

    <!-- 拼音搜索配置选项 -->
    <div v-if="localConfig.enabled" class="space-y-4 pl-4 border-l-2 border-emerald-200">
      <!-- 匹配模式选择 -->
      <div class="space-y-2">
        <Label class="text-sm font-medium text-emerald-700">匹配模式</Label>

        <div class="space-y-2">
          <div class="flex items-center gap-2">
            <input
              id="mode-fuzzy"
              type="radio"
              value="fuzzy"
              v-model="localConfig.mode"
              :disabled="disabled"
              class="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-300"
            />
            <div class="flex-1">
              <label for="mode-fuzzy" class="text-sm cursor-pointer">模糊匹配</label>
              <p class="text-xs text-gray-600">支持部分拼音匹配，如 "zh" 匹配 "中国"</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <input
              id="mode-exact"
              type="radio"
              value="exact"
              v-model="localConfig.mode"
              :disabled="disabled"
              class="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-300"
            />
            <div class="flex-1">
              <label for="mode-exact" class="text-sm cursor-pointer">精确匹配</label>
              <p class="text-xs text-gray-600">需要完整拼音匹配，如 "zhongguo" 匹配 "中国"</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <input
              id="mode-both"
              type="radio"
              value="both"
              v-model="localConfig.mode"
              :disabled="disabled"
              class="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-300"
            />
            <div class="flex-1">
              <label for="mode-both" class="text-sm cursor-pointer">智能匹配</label>
              <p class="text-xs text-gray-600">结合模糊和精确匹配，提供最佳搜索结果</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 高级选项 -->
      <div class="space-y-3">
        <h4 class="text-sm font-medium text-emerald-700">高级选项</h4>

        <!-- 忽略声调 -->
        <div class="flex items-center justify-between">
          <div class="space-y-1">
            <Label class="text-sm">忽略声调</Label>
            <p class="text-xs text-gray-600">
              不区分拼音声调，如 "ma" 可匹配 "妈", "麻", "马", "骂"
            </p>
          </div>
          <Switch
            v-model:checked="localConfig.toneIgnore"
            :disabled="disabled"
            class="data-[state=checked]:bg-emerald-600"
          />
        </div>

        <!-- 分段匹配 -->
        <div class="flex items-center justify-between">
          <div class="space-y-1">
            <Label class="text-sm">分段匹配</Label>
            <p class="text-xs text-gray-600">
              支持拼音分段搜索，如 "zh guo" 匹配 "中国"
            </p>
          </div>
          <Switch
            v-model:checked="localConfig.segmentMatch"
            :disabled="disabled"
            class="data-[state=checked]:bg-emerald-600"
          />
        </div>
      </div>

      <!-- 配置预览 -->
      <div class="mt-4 p-3 bg-gray-50 rounded-lg">
        <h4 class="text-sm font-medium text-gray-700 mb-2">配置预览</h4>
        <div class="space-y-2 text-xs">
          <div class="flex justify-between">
            <span class="text-gray-600">匹配模式:</span>
            <span class="font-medium">{{ getModeText(localConfig.mode) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">声调处理:</span>
            <span class="font-medium">{{ localConfig.toneIgnore ? '忽略声调' : '严格声调' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">分段处理:</span>
            <span class="font-medium">{{ localConfig.segmentMatch ? '支持分段' : '整体匹配' }}</span>
          </div>
        </div>
      </div>

      <!-- 使用示例 -->
      <div class="mt-4 p-3 bg-blue-50 rounded-lg">
        <h4 class="text-sm font-medium text-blue-800 mb-2">搜索示例</h4>
        <div class="space-y-2 text-xs">
          <div>
            <span class="text-blue-700 font-medium">输入:</span>
            <code class="ml-2 px-2 py-1 bg-blue-100 rounded">{{ getExampleInput() }}</code>
          </div>
          <div>
            <span class="text-blue-700 font-medium">匹配:</span>
            <span class="ml-2 text-blue-800">{{ getExampleMatches() }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 禁用状态提示 -->
    <div v-if="disabled" class="p-3 bg-gray-100 rounded-lg">
      <div class="flex items-center gap-2 text-sm text-gray-600">
        <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
        </svg>
        <span>拼音搜索配置当前不可编辑</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useVModel } from '@vueuse/core'

import Label from '@/components/ui/label/Label.vue'
import { Switch } from '@/components/ui/switch'

import type { PinyinSearchConfigProps, PinyinSearchConfig } from '@/types/demo'

// Props & Emits
const props = withDefaults(defineProps<PinyinSearchConfigProps>(), {
  disabled: false
})

const emits = defineEmits<{
  'update:modelValue': [value: PinyinSearchConfig]
}>()

// Local state
const localConfig = useVModel(props, 'modelValue', emits, { passive: true })

// Computed
const exampleQueries = computed(() => {
  const base = {
    fuzzy: { input: 'zh', matches: '中国、中华、中文、众多' },
    exact: { input: 'zhongguo', matches: '中国' },
    both: { input: 'zh guo', matches: '中国、中华民国' }
  }

  return base[localConfig.value.mode] || base.fuzzy
})

// Methods
function getModeText(mode: string): string {
  const modeTexts = {
    fuzzy: '模糊匹配',
    exact: '精确匹配',
    both: '智能匹配'
  }
  return modeTexts[mode as keyof typeof modeTexts] || '未知模式'
}

function getExampleInput(): string {
  const examples = {
    fuzzy: localConfig.value.segmentMatch ? 'zh zh' : 'zh',
    exact: localConfig.value.segmentMatch ? 'zhong guo' : 'zhongguo',
    both: localConfig.value.segmentMatch ? 'zh guo' : 'zhguo'
  }
  return examples[localConfig.value.mode as keyof typeof examples] || 'zh'
}

function getExampleMatches(): string {
  const baseMatches = {
    fuzzy: ['中国', '中华', '中文', '众多'],
    exact: ['中国'],
    both: ['中国', '中华民国', '中央政府']
  }

  let matches = baseMatches[localConfig.value.mode as keyof typeof baseMatches] || baseMatches.fuzzy

  // 如果忽略声调，添加更多匹配示例
  if (localConfig.value.toneIgnore && localConfig.value.mode === 'fuzzy') {
    matches = [...matches, '着', '之', '执', '制']
  }

  // 如果支持分段匹配，调整示例
  if (localConfig.value.segmentMatch) {
    matches = matches.slice(0, 2) // 减少示例数量以适应分段匹配
  }

  return matches.slice(0, 4).join('、')
}
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