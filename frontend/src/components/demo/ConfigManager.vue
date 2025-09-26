<template>
  <div class="border rounded-lg">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between p-3 bg-emerald-50 rounded-t-lg border-b">
      <h3 class="text-sm font-medium text-emerald-800">配置管理</h3>
      <div class="flex items-center gap-2">
        <Button
          v-if="allowSave"
          variant="outline"
          size="sm"
          @click="showSaveDialog = true"
          class="text-xs text-emerald-700 border-emerald-300 hover:bg-emerald-100"
        >
          保存预设
        </Button>
        <Button
          variant="outline"
          size="sm"
          @click="showImportDialog = true"
          class="text-xs"
        >
          导入
        </Button>
        <Button
          variant="outline"
          size="sm"
          @click="exportConfig"
          class="text-xs"
        >
          导出
        </Button>
      </div>
    </div>

    <!-- 预设列表 -->
    <div class="p-3">
      <div class="space-y-2 max-h-60 overflow-y-auto">
        <div
          v-for="preset in presets"
          :key="preset.id"
          class="flex items-center gap-3 p-2 border rounded-lg hover:bg-gray-50 transition-colors"
          :class="{ 'border-emerald-200 bg-emerald-50/50': isCurrentConfig(preset) }"
        >
          <!-- 预设信息 -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h4 class="text-sm font-medium text-gray-900 truncate">
                {{ preset.name }}
              </h4>
              <span
                v-if="preset.isBuiltIn"
                class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs bg-blue-100 text-blue-800"
              >
                内置
              </span>
              <span
                v-if="isCurrentConfig(preset)"
                class="inline-flex items-center px-1.5 py-0.5 rounded-full text-xs bg-emerald-100 text-emerald-800"
              >
                当前
              </span>
            </div>
            <p v-if="preset.description" class="text-xs text-gray-600 truncate mt-1">
              {{ preset.description }}
            </p>
            <div class="flex items-center gap-3 text-xs text-gray-500 mt-1">
              <span v-if="preset.createdAt">
                创建: {{ formatDate(preset.createdAt) }}
              </span>
              <span v-if="preset.updatedAt && preset.updatedAt !== preset.createdAt">
                更新: {{ formatDate(preset.updatedAt) }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              @click="applyPreset(preset)"
              :disabled="isCurrentConfig(preset)"
              class="text-xs px-2 py-1 h-auto"
            >
              应用
            </Button>
            <Button
              v-if="allowDelete && !preset.isBuiltIn"
              variant="ghost"
              size="sm"
              @click="confirmDelete(preset)"
              class="text-xs px-2 py-1 h-auto text-red-600 hover:bg-red-50 hover:text-red-700"
            >
              删除
            </Button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="presets.length === 0" class="text-center py-8 text-gray-500">
        <div class="text-sm">暂无保存的配置预设</div>
        <div class="text-xs mt-1">保存当前配置作为预设以便快速应用</div>
      </div>
    </div>

    <!-- 保存预设对话框 -->
    <div v-if="showSaveDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-4 w-96 max-w-full mx-4">
        <h3 class="text-lg font-semibold mb-4">保存配置预设</h3>

        <div class="space-y-3">
          <div>
            <Label class="text-sm font-medium">预设名称</Label>
            <Input
              v-model="saveForm.name"
              placeholder="输入预设名称..."
              class="mt-1"
            />
          </div>

          <div>
            <Label class="text-sm font-medium">描述 (可选)</Label>
            <textarea
              v-model="saveForm.description"
              placeholder="输入预设描述..."
              class="mt-1 w-full px-3 py-2 border border-input rounded-md text-sm resize-none"
              rows="3"
            ></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-2 mt-4">
          <Button
            variant="outline"
            @click="cancelSave"
            class="text-sm"
          >
            取消
          </Button>
          <Button
            @click="savePreset"
            :disabled="!saveForm.name.trim()"
            class="text-sm bg-emerald-600 hover:bg-emerald-700"
          >
            保存
          </Button>
        </div>
      </div>
    </div>

    <!-- 导入对话框 -->
    <div v-if="showImportDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-4 w-96 max-w-full mx-4">
        <h3 class="text-lg font-semibold mb-4">导入配置</h3>

        <div class="space-y-3">
          <div>
            <Label class="text-sm font-medium">配置数据</Label>
            <textarea
              v-model="importData"
              placeholder="粘贴配置 JSON 数据..."
              class="mt-1 w-full px-3 py-2 border border-input rounded-md text-sm resize-none font-mono"
              rows="8"
            ></textarea>
          </div>

          <div v-if="importError" class="p-2 bg-red-50 border border-red-200 rounded-md">
            <div class="text-sm text-red-700">{{ importError }}</div>
          </div>
        </div>

        <div class="flex justify-end gap-2 mt-4">
          <Button
            variant="outline"
            @click="cancelImport"
            class="text-sm"
          >
            取消
          </Button>
          <Button
            @click="importConfig"
            :disabled="!importData.trim()"
            class="text-sm bg-emerald-600 hover:bg-emerald-700"
          >
            导入
          </Button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="deletePreset" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-4 w-96 max-w-full mx-4">
        <h3 class="text-lg font-semibold mb-4">确认删除</h3>

        <p class="text-sm text-gray-700 mb-4">
          确定要删除预设 "{{ deletePreset.name }}" 吗？此操作不可撤销。
        </p>

        <div class="flex justify-end gap-2">
          <Button
            variant="outline"
            @click="deletePreset = null"
            class="text-sm"
          >
            取消
          </Button>
          <Button
            @click="doDeletePreset"
            class="text-sm bg-red-600 hover:bg-red-700"
          >
            删除
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

import Button from '@/components/ui/button/Button.vue'
import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'

import type { ConfigManagerProps, ConfigPreset, SearchDemoConfig } from '@/types/demo'

// Props & Emits
const props = withDefaults(defineProps<ConfigManagerProps>(), {
  allowSave: true,
  allowDelete: true
})

const emits = defineEmits<{
  'apply-preset': [preset: ConfigPreset]
  'save-preset': [data: { name: string; description?: string }]
  'delete-preset': [preset: ConfigPreset]
}>()

// Local state
const showSaveDialog = ref(false)
const showImportDialog = ref(false)
const importData = ref('')
const importError = ref('')
const deletePreset = ref<ConfigPreset | null>(null)

const saveForm = ref({
  name: '',
  description: ''
})

// Computed
const sortedPresets = computed(() => {
  return [...props.presets].sort((a, b) => {
    // 内置预设排在前面
    if (a.isBuiltIn && !b.isBuiltIn) return -1
    if (!a.isBuiltIn && b.isBuiltIn) return 1

    // 按名称排序
    return a.name.localeCompare(b.name)
  })
})

// Methods
function isCurrentConfig(preset: ConfigPreset): boolean {
  return JSON.stringify(preset.config) === JSON.stringify(props.currentConfig)
}

function applyPreset(preset: ConfigPreset) {
  emits('apply-preset', preset)
}

function confirmDelete(preset: ConfigPreset) {
  deletePreset.value = preset
}

function doDeletePreset() {
  if (deletePreset.value) {
    emits('delete-preset', deletePreset.value)
    deletePreset.value = null
  }
}

function savePreset() {
  if (saveForm.value.name.trim()) {
    emits('save-preset', {
      name: saveForm.value.name.trim(),
      description: saveForm.value.description.trim() || undefined
    })
    cancelSave()
  }
}

function cancelSave() {
  showSaveDialog.value = false
  saveForm.value = { name: '', description: '' }
}

function importConfig() {
  importError.value = ''

  try {
    const data = JSON.parse(importData.value)

    // 验证数据格式
    if (!data || typeof data !== 'object') {
      throw new Error('无效的配置格式')
    }

    // 检查是否是单个配置还是预设
    if (data.searchSpaces && data.pinyinSearch) {
      // 单个配置，直接应用
      emits('apply-preset', {
        id: `imported_${Date.now()}`,
        name: '导入的配置',
        description: '通过导入功能应用的配置',
        config: data as SearchDemoConfig,
        isBuiltIn: false,
        createdAt: new Date().toISOString()
      })
    } else if (Array.isArray(data)) {
      // 预设列表
      data.forEach((preset: any, index: number) => {
        if (preset.config && preset.name) {
          emits('save-preset', {
            name: preset.name,
            description: preset.description
          })
        }
      })
    } else {
      throw new Error('不支持的数据格式')
    }

    cancelImport()
  } catch (error) {
    importError.value = error instanceof Error ? error.message : '导入失败'
  }
}

function cancelImport() {
  showImportDialog.value = false
  importData.value = ''
  importError.value = ''
}

function exportConfig() {
  const exportData = {
    currentConfig: props.currentConfig,
    presets: props.presets.filter(p => !p.isBuiltIn),
    exportTime: new Date().toISOString(),
    version: '1.0.0'
  }

  const dataStr = JSON.stringify(exportData, null, 2)
  const blob = new Blob([dataStr], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `search-config-${new Date().toISOString().slice(0, 10)}.json`
  a.click()

  URL.revokeObjectURL(url)
}

function formatDate(dateStr: string): string {
  try {
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}
</script>

<style scoped>
/* 对话框遮罩动画 */
.fixed.inset-0 {
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.fixed.inset-0 > div {
  animation: slideIn 0.2s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgb(167 243 208);
  border-radius: 2px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgb(110 231 183);
}

/* 过渡动画 */
.transition-colors {
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

/* 文本域样式 */
textarea {
  transition: border-color 0.2s ease;
}

textarea:focus {
  outline: none;
  border-color: rgb(16 185 129);
  box-shadow: 0 0 0 1px rgb(16 185 129 / 0.2);
}

/* 等宽字体 */
.font-mono {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

/* 按钮悬停效果 */
.hover\:bg-red-50:hover {
  background-color: rgb(254 242 242);
}

.hover\:text-red-700:hover {
  color: rgb(185 28 28);
}
</style>