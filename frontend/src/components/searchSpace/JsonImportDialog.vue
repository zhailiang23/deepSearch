<template>
  <DialogRoot v-model:open="isOpen">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/20 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
      <DialogContent class="fixed left-[50%] top-[50%] z-50 w-full max-w-4xl translate-x-[-50%] translate-y-[-50%] border bg-white p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] rounded-lg dark:bg-gray-800 dark:border-gray-700 max-h-[90vh] flex flex-col">

        <!-- 对话框头部 -->
        <div class="flex flex-col space-y-2 flex-shrink-0">
          <div class="flex items-center justify-between">
            <DialogTitle class="text-xl font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
              导入JSON数据到 "{{ searchSpace?.name }}"
            </DialogTitle>
            <DialogClose as-child>
              <button
                class="p-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 rounded-md"
                @click="closeDialog"
              >
                <X class="h-4 w-4" />
              </button>
            </DialogClose>
          </div>
          <DialogDescription class="text-sm text-gray-500 dark:text-gray-400">
            通过简单的步骤将JSON数据导入到您的搜索空间中
          </DialogDescription>

          <!-- 步骤指示器 -->
          <div class="flex items-center space-x-2 pt-4">
            <div
              v-for="(step, index) in steps"
              :key="index"
              class="flex items-center"
            >
              <div
                :class="[
                  'flex items-center justify-center w-8 h-8 rounded-full text-sm font-medium',
                  index < currentStep
                    ? 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400'
                    : index === currentStep
                    ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-400'
                    : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                ]"
              >
                <CheckCircle2 v-if="index < currentStep" class="h-4 w-4" />
                <span v-else>{{ index + 1 }}</span>
              </div>
              <span
                :class="[
                  'ml-2 text-sm font-medium',
                  index <= currentStep
                    ? 'text-gray-900 dark:text-white'
                    : 'text-gray-500 dark:text-gray-400'
                ]"
              >
                {{ step.title }}
              </span>
              <ChevronRight
                v-if="index < steps.length - 1"
                class="h-4 w-4 mx-3 text-gray-400"
              />
            </div>
          </div>
        </div>

        <!-- 步骤内容 -->
        <div class="flex-1 min-h-[400px] overflow-hidden">
          <!-- 步骤1: 文件选择 -->
          <div v-if="currentStep === 0" class="h-full overflow-y-auto space-y-6">
            <div class="text-center mt-4">
              <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">
                选择JSON文件
              </h3>
              <p class="text-sm text-gray-500 dark:text-gray-400 mb-6">
                支持拖放文件或点击选择，最大支持20MB
              </p>
            </div>

            <!-- 文件拖放区域 -->
            <div
              ref="dropZone"
              :class="[
                'border-2 border-dashed rounded-lg p-8 text-center transition-colors',
                isDragOver
                  ? 'border-green-400 bg-green-50 dark:bg-green-900/10'
                  : 'border-gray-300 dark:border-gray-600 hover:border-gray-400'
              ]"
              @dragover.prevent="handleDragOver"
              @dragleave.prevent="handleDragLeave"
              @drop.prevent="handleDrop"
            >
              <input
                ref="fileInput"
                type="file"
                accept=".json,application/json"
                class="hidden"
                @change="handleFileSelect"
              />

              <div class="space-y-4">
                <div class="flex justify-center">
                  <Upload class="h-12 w-12 text-gray-400" />
                </div>
                <div>
                  <p class="text-lg font-medium text-gray-900 dark:text-white">
                    拖放文件到此处
                  </p>
                  <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                    或者
                    <button
                      @click="triggerFileSelect"
                      class="text-green-600 hover:text-green-700 dark:text-green-400 font-medium"
                    >
                      点击选择文件
                    </button>
                  </p>
                </div>
                <div class="text-xs text-gray-400">
                  支持 JSON 格式，文件大小限制 20MB
                </div>
              </div>
            </div>

            <!-- 选中文件信息 -->
            <div v-if="selectedFile" class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
              <div class="flex items-start justify-between">
                <div class="flex items-center space-x-3">
                  <FileText class="h-8 w-8 text-blue-500" />
                  <div>
                    <p class="text-sm font-medium text-gray-900 dark:text-white">
                      {{ selectedFile.name }}
                    </p>
                    <p class="text-xs text-gray-500 dark:text-gray-400">
                      {{ formatFileSize(selectedFile.size) }} • {{ formatDate(new Date(selectedFile.lastModified).toISOString()) }}
                    </p>
                  </div>
                </div>
                <button
                  @click="clearFile"
                  class="p-1 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300"
                  title="移除文件"
                >
                  <X class="h-4 w-4" />
                </button>
              </div>
            </div>

            <!-- 错误信息 -->
            <div v-if="error" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
              <div class="flex items-center">
                <AlertCircle class="h-5 w-5 text-red-400 mr-3" />
                <p class="text-sm text-red-800 dark:text-red-400">{{ error }}</p>
              </div>
            </div>
          </div>

          <!-- 步骤2: JSON预览 -->
          <div v-if="currentStep === 1" class="h-full flex flex-col space-y-6 pb-6">
            <div class="text-center flex-shrink-0 mt-4">
              <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">
                JSON数据预览
              </h3>
              <p class="text-sm text-gray-500 dark:text-gray-400">
                验证数据格式并查看数据统计信息
              </p>
            </div>

            <!-- 数据统计 -->
            <div v-if="jsonAnalysis" class="grid grid-cols-1 md:grid-cols-3 gap-4 flex-shrink-0">
              <div class="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
                <div class="text-2xl font-bold text-blue-600 dark:text-blue-400">
                  {{ jsonAnalysis.totalRecords }}
                </div>
                <div class="text-sm text-gray-600 dark:text-gray-400">总记录数</div>
              </div>
              <div class="bg-green-50 dark:bg-green-900/20 p-4 rounded-lg">
                <div class="text-2xl font-bold text-green-600 dark:text-green-400">
                  {{ jsonAnalysis.fieldCount }}
                </div>
                <div class="text-sm text-gray-600 dark:text-gray-400">字段数量</div>
              </div>
              <div class="bg-yellow-50 dark:bg-yellow-900/20 p-4 rounded-lg">
                <div class="text-2xl font-bold text-yellow-600 dark:text-yellow-400">
                  {{ formatFileSize(selectedFile?.size || 0) }}
                </div>
                <div class="text-sm text-gray-600 dark:text-gray-400">文件大小</div>
              </div>
            </div>

            <!-- 可滚动的内容区域 -->
            <div class="flex-1 overflow-y-auto space-y-4" style="max-height: calc(90vh - 300px);">
              <!-- 字段类型分析 -->
              <div v-if="jsonAnalysis" class="space-y-4">
                <h4 class="text-md font-medium text-gray-900 dark:text-white">
                  检测到的字段
                </h4>
                <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4 max-h-40 overflow-y-auto">
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                    <div
                      v-for="field in jsonAnalysis.fields"
                      :key="field.name"
                      class="flex items-center justify-between py-1"
                    >
                      <span class="text-sm font-medium text-gray-900 dark:text-white">
                        {{ field.name }}
                      </span>
                      <span
                        :class="[
                          'px-2 py-1 text-xs rounded',
                          getFieldTypeColor(field.type)
                        ]"
                      >
                        {{ getFieldTypeLabel(field.type) }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 数据预览 -->
              <div v-if="jsonPreview && jsonPreview.length > 0" class="space-y-4">
                <h4 class="text-md font-medium text-gray-900 dark:text-white">
                  数据预览 (前{{ Math.min(jsonPreview.length, 5) }}条记录)
                </h4>
                <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                  <pre class="text-xs text-gray-700 dark:text-gray-300 overflow-auto whitespace-pre-wrap max-h-60 mb-4">{{ JSON.stringify(jsonPreview.slice(0, 5), null, 2) }}</pre>
                </div>
              </div>
            </div>
          </div>

          <!-- 步骤3: 导入配置 -->
          <div v-if="currentStep === 2" class="h-full overflow-y-auto space-y-6">
            <div class="text-center mt-4">
              <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">
                导入配置
              </h3>
              <p class="text-sm text-gray-500 dark:text-gray-400">
                配置数据导入选项
              </p>
            </div>

            <div class="space-y-6">
              <!-- 导入模式 -->
              <div class="space-y-3">
                <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                  导入模式
                </label>
                <div class="space-y-2">
                  <label class="flex items-center">
                    <input
                      v-model="importConfig.mode"
                      type="radio"
                      value="append"
                      class="form-radio h-4 w-4 text-green-600"
                    />
                    <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">
                      追加 - 将数据添加到现有数据中
                    </span>
                  </label>
                  <label class="flex items-center">
                    <input
                      v-model="importConfig.mode"
                      type="radio"
                      value="replace"
                      class="form-radio h-4 w-4 text-green-600"
                    />
                    <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">
                      替换 - 清空现有数据后导入新数据
                    </span>
                  </label>
                </div>
              </div>

              <!-- 批处理大小 -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                  批处理大小
                </label>
                <select
                  v-model="importConfig.batchSize"
                  class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
                >
                  <option value="100">100</option>
                  <option value="500">500</option>
                  <option value="1000">1000</option>
                  <option value="2000">2000</option>
                </select>
                <p class="text-xs text-gray-500 dark:text-gray-400">
                  每批处理的记录数，较大的批次可能提高性能但占用更多内存
                </p>
              </div>

              <!-- 错误处理 -->
              <div class="space-y-2">
                <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                  错误处理
                </label>
                <div class="space-y-2">
                  <label class="flex items-center">
                    <input
                      v-model="importConfig.errorHandling"
                      type="radio"
                      value="stop"
                      class="form-radio h-4 w-4 text-green-600"
                    />
                    <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">
                      遇到错误时停止导入
                    </span>
                  </label>
                  <label class="flex items-center">
                    <input
                      v-model="importConfig.errorHandling"
                      type="radio"
                      value="skip"
                      class="form-radio h-4 w-4 text-green-600"
                    />
                    <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">
                      跳过错误记录继续导入
                    </span>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- 步骤4: 导入结果 -->
          <div v-if="currentStep === 3" class="h-full overflow-y-auto space-y-6">
            <div class="text-center mt-4">
              <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-2">
                导入结果
              </h3>
              <p class="text-sm text-gray-500 dark:text-gray-400">
                查看数据导入的详细结果
              </p>
            </div>

            <!-- 进度条 -->
            <div class="space-y-4">
              <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
                <div
                  class="bg-green-600 h-3 rounded-full transition-all duration-300"
                  :style="{ width: `${importProgress.percentage}%` }"
                ></div>
              </div>
              <div class="flex justify-between text-sm text-gray-600 dark:text-gray-400">
                <span>{{ importProgress.current }} / {{ importProgress.total }}</span>
                <span>{{ importProgress.percentage }}%</span>
              </div>
            </div>

            <!-- 状态信息 -->
            <div class="space-y-3">
              <!-- 成功状态 -->
              <div v-if="importProgress.status === 'completed'" class="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
                <div class="flex items-center">
                  <CheckCircle2 class="h-5 w-5 text-green-500 mr-3" />
                  <div>
                    <p class="text-sm font-medium text-green-800 dark:text-green-400">
                      导入完成
                    </p>
                    <p class="text-xs text-green-700 dark:text-green-300 mt-1">
                      成功导入 {{ importProgress.successCount }} 条记录
                      <span v-if="importProgress.errorCount > 0">
                        ，跳过 {{ importProgress.errorCount }} 条错误记录
                      </span>
                    </p>
                  </div>
                </div>
              </div>

              <!-- 错误状态 -->
              <div v-if="importProgress.status === 'failed'" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4">
                <div class="flex items-center">
                  <AlertCircle class="h-5 w-5 text-red-500 mr-3" />
                  <div>
                    <p class="text-sm font-medium text-red-800 dark:text-red-400">
                      导入失败
                    </p>
                    <p class="text-xs text-red-700 dark:text-red-300 mt-1">
                      {{ importProgress.error }}
                    </p>
                  </div>
                </div>
              </div>

              <!-- 处理中状态 -->
              <div v-if="importProgress.status === 'processing'" class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
                <div class="flex items-center">
                  <RefreshCw class="h-5 w-5 text-blue-500 mr-3 animate-spin" />
                  <p class="text-sm text-blue-800 dark:text-blue-400">
                    正在处理数据导入...
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="flex flex-col-reverse sm:flex-row sm:justify-between pt-6 border-t border-gray-200 dark:border-gray-600 flex-shrink-0">
          <div class="flex space-x-2">
            <!-- 上一步按钮 -->
            <button
              v-if="currentStep > 0 && currentStep < 3"
              @click="previousStep"
              class="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 dark:bg-gray-700 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-600"
            >
              <ChevronLeft class="h-4 w-4 mr-2" />
              上一步
            </button>
          </div>

          <div class="flex space-x-2 mb-3 sm:mb-0">
            <!-- 下一步按钮 -->
            <button
              v-if="currentStep < 2"
              :disabled="!canProceed"
              @click="nextStep"
              class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              下一步
              <ChevronRight class="h-4 w-4 ml-2" />
            </button>

            <!-- 开始导入按钮 -->
            <button
              v-if="currentStep === 2"
              :disabled="!canProceed || importProgress.status === 'processing'"
              @click.prevent="handleImportClick"
              class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="importProgress.status === 'processing'" class="flex items-center">
                <RefreshCw class="h-4 w-4 mr-2 animate-spin" />
                导入中...
              </span>
              <span v-else class="flex items-center">
                <Upload class="h-4 w-4 mr-2" />
                开始导入
              </span>
            </button>

            <!-- 关闭按钮（移到右边，在导入结果页面显示） -->
            <DialogClose as-child>
              <button
                v-if="currentStep === 3 || importProgress.status === 'processing'"
                :disabled="importProgress.status === 'processing'"
                class="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 disabled:opacity-50 disabled:cursor-not-allowed dark:bg-gray-700 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-600"
                @click="closeDialog"
              >
                {{ importProgress.status === 'completed' ? '关闭' : '取消' }}
              </button>
            </DialogClose>

            <!-- 其他步骤的关闭按钮 -->
            <DialogClose as-child>
              <button
                v-if="currentStep < 3 && importProgress.status !== 'processing'"
                class="inline-flex items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 dark:bg-gray-700 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-600"
                @click="closeDialog"
              >
                取消
              </button>
            </DialogClose>
          </div>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import {
  X,
  Upload,
  FileText,
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  ChevronLeft,
  RefreshCw,
  Eye
} from 'lucide-vue-next'
import {
  DialogRoot,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogClose
} from 'reka-ui'
import type { SearchSpace } from '@/types/searchSpace'
import { ImportService, type ImportTaskStatus, type ImportExecuteRequest, type ImportSyncResponse } from '@/services/importService'
import { useSearchSpaceStore } from '@/stores/searchSpace'

// === 🚨 调试日志：检测组件是否重新加载 ===
console.log('🔥🔥🔥 JsonImportDialog组件正在重新加载！时间戳:', new Date().toISOString())
console.log('🚀 JsonImportDialog组件已加载！版本：v2.0')

const searchSpaceStore = useSearchSpaceStore()
const { searchSpaces } = searchSpaceStore

// 组件props和emits
interface Props {
  open: boolean
  searchSpace: SearchSpace | null
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'import-complete', result: ImportResult): void
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  searchSpace: null
})

const emit = defineEmits<Emits>()

// 步骤定义
const steps = [
  { title: '选择文件', description: '上传JSON文件' },
  { title: 'JSON预览', description: '验证数据格式' },
  { title: '配置选项', description: '设置导入参数' },
  { title: '导入结果', description: '查看导入结果' }
]

// 组件状态
const currentStep = ref(0)
const selectedFile = ref<File | null>(null)
const isDragOver = ref(false)
const error = ref('')
const jsonPreview = ref<any[]>([])
const jsonAnalysis = ref<JsonAnalysis | null>(null)

// 组件引用
const fileInput = ref<HTMLInputElement | null>(null)
const dropZone = ref<HTMLDivElement | null>(null)

// 导入配置
const importConfig = ref({
  mode: 'append' as 'append' | 'replace',
  batchSize: 1000,
  errorHandling: 'skip' as 'stop' | 'skip'
})

// 导入进度
const importProgress = ref<ImportProgress>({
  status: 'idle',
  percentage: 0,
  current: 0,
  total: 0,
  currentBatch: 0,
  totalBatches: 0,
  successCount: 0,
  errorCount: 0,
  error: ''
})

// 真实的导入任务状态
const importTaskStatus = ref<ImportTaskStatus | null>(null)

// 轮询取消函数
let pollCancel: (() => void) | null = null

// 类型定义
interface JsonAnalysis {
  totalRecords: number
  fieldCount: number
  fields: Array<{
    name: string
    type: string
    sampleValues: any[]
  }>
  isValidJson: boolean
  estimatedSize: number
}

interface ImportProgress {
  status: 'idle' | 'processing' | 'completed' | 'failed'
  percentage: number
  current: number
  total: number
  currentBatch: number
  totalBatches: number
  successCount: number
  errorCount: number
  error: string
}

interface ImportResult {
  success: boolean
  importedCount: number
  errorCount: number
  duration: number
  errors?: string[]
}

// 计算属性
const isOpen = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value)
})

const canProceed = computed(() => {
  switch (currentStep.value) {
    case 0:
      return selectedFile.value !== null && !error.value
    case 1:
      return jsonAnalysis.value?.isValidJson === true
    case 2:
      return true
    default:
      return false
  }
})

// 文件处理方法
const triggerFileSelect = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    processFile(file)
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = true
}

const handleDragLeave = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = false
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  isDragOver.value = false

  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    processFile(files[0])
  }
}

const processFile = async (file: File) => {
  // 重置状态
  error.value = ''
  jsonPreview.value = []
  jsonAnalysis.value = null

  // 验证文件类型
  if (!file.type.includes('json') && !file.name.toLowerCase().endsWith('.json')) {
    error.value = '请选择JSON格式的文件'
    return
  }

  // 验证文件大小 (20MB)
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    error.value = '文件大小不能超过20MB'
    return
  }

  selectedFile.value = file

  try {
    // 读取文件内容
    const content = await readFileAsText(file)

    // 解析JSON
    let jsonData: any
    try {
      jsonData = JSON.parse(content)
    } catch (parseError) {
      error.value = '文件内容不是有效的JSON格式'
      return
    }

    // 分析JSON结构
    analyzeJsonData(jsonData)

  } catch (err) {
    error.value = '读取文件失败，请重试'
    console.error('File processing error:', err)
  }
}

const analyzeJsonData = (data: any) => {
  try {
    let records: any[] = []

    // 处理不同的JSON结构
    if (Array.isArray(data)) {
      records = data
    } else if (data && typeof data === 'object') {
      // 尝试找到数组字段
      const arrayFields = Object.keys(data).filter(key => Array.isArray(data[key]))
      if (arrayFields.length > 0) {
        records = data[arrayFields[0]]
      } else {
        records = [data]
      }
    } else {
      error.value = 'JSON数据应该是对象数组或包含数组的对象'
      return
    }

    if (records.length === 0) {
      error.value = 'JSON文件中没有找到可导入的数据记录'
      return
    }

    // 分析字段结构
    const fieldsMap = new Map<string, Set<string>>()

    records.slice(0, 100).forEach(record => { // 只分析前100条记录来提高性能
      if (record && typeof record === 'object') {
        Object.keys(record).forEach(key => {
          if (!fieldsMap.has(key)) {
            fieldsMap.set(key, new Set())
          }
          const value = record[key]
          const type = detectFieldType(value)
          fieldsMap.get(key)?.add(type)
        })
      }
    })

    const fields = Array.from(fieldsMap.entries()).map(([name, types]) => {
      const typeArray = Array.from(types)
      const primaryType = typeArray.length === 1 ? typeArray[0] : 'mixed'

      // 获取样本值
      const sampleValues = records
        .slice(0, 5)
        .map(r => r[name])
        .filter(v => v !== undefined && v !== null)
        .slice(0, 3)

      return {
        name,
        type: primaryType,
        sampleValues
      }
    })

    jsonAnalysis.value = {
      totalRecords: records.length,
      fieldCount: fields.length,
      fields,
      isValidJson: true,
      estimatedSize: selectedFile.value?.size || 0
    }

    jsonPreview.value = records

  } catch (err) {
    error.value = '分析JSON数据结构时出错'
    console.error('JSON analysis error:', err)
  }
}

const detectFieldType = (value: any): string => {
  if (value === null) return 'null'
  if (typeof value === 'string') return 'string'
  if (typeof value === 'number') return 'number'
  if (typeof value === 'boolean') return 'boolean'
  if (Array.isArray(value)) return 'array'
  if (typeof value === 'object') return 'object'
  return 'unknown'
}

const clearFile = () => {
  selectedFile.value = null
  error.value = ''
  jsonPreview.value = []
  jsonAnalysis.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 工具方法
const readFileAsText = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const result = e.target?.result
      if (typeof result === 'string') {
        resolve(result)
      } else {
        reject(new Error('Failed to read file as text'))
      }
    }
    reader.onerror = () => reject(new Error('Failed to read file'))
    reader.readAsText(file)
  })
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getFieldTypeColor = (type: string): string => {
  const colors: Record<string, string> = {
    string: 'bg-blue-100 text-blue-800 dark:bg-blue-900/20 dark:text-blue-400',
    number: 'bg-green-100 text-green-800 dark:bg-green-900/20 dark:text-green-400',
    boolean: 'bg-purple-100 text-purple-800 dark:bg-purple-900/20 dark:text-purple-400',
    array: 'bg-orange-100 text-orange-800 dark:bg-orange-900/20 dark:text-orange-400',
    object: 'bg-red-100 text-red-800 dark:bg-red-900/20 dark:text-red-400',
    null: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-400',
    mixed: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/20 dark:text-yellow-400'
  }
  return colors[type] || colors.mixed
}

const getFieldTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    string: '字符串',
    number: '数字',
    boolean: '布尔值',
    array: '数组',
    object: '对象',
    null: '空值',
    mixed: '混合类型'
  }
  return labels[type] || '未知'
}

// 步骤控制
const nextStep = () => {
  if (currentStep.value < steps.length - 1 && canProceed.value) {
    currentStep.value++
  }
}

const previousStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 处理导入按钮点击
const handleImportClick = (event: Event) => {
  console.log('🟢 开始导入按钮被点击了！')
  event.preventDefault()
  event.stopPropagation()
  startImport()
}

// 导入执行
const startImport = async () => {
  if (!selectedFile.value || !jsonAnalysis.value || !props.searchSpace) {
    return
  }

  console.log('🎯 开始导入 - 当前步骤:', currentStep.value)

  // 在切换步骤之前保存关键数据的引用，避免DOM变化导致响应式数据丢失
  const fileToUpload = selectedFile.value
  const analysisData = jsonAnalysis.value
  const searchSpaceRef = props.searchSpace

  // 切换到导入进度页面
  currentStep.value = 3
  console.log('🎯 已切换到第3步 (导入结果页面)')

  // 初始化导入进度
  importProgress.value = {
    status: 'processing',
    percentage: 0,
    current: 0,
    total: analysisData.totalRecords,
    currentBatch: 0,
    totalBatches: Math.ceil(analysisData.totalRecords / importConfig.value.batchSize),
    successCount: 0,
    errorCount: 0,
    error: ''
  }

  try {
    console.log('🚀 开始同步导入文件:', fileToUpload.name)

    // 调用新的同步导入接口
    const result = await ImportService.importFileSync(
      searchSpaceRef.id,
      fileToUpload,
      importConfig.value.mode === 'append' ? 'APPEND' : 'REPLACE',
      importConfig.value.batchSize,
      importConfig.value.errorHandling === 'stop' ? 'STOP_ON_ERROR' : 'SKIP_ERROR'
    )

    console.log('✅ 导入完成:', result)
    console.log('🎯 导入完成后当前步骤:', currentStep.value)

    // 更新进度显示
    importProgress.value = {
      status: result.success ? 'completed' : 'failed',
      percentage: 100,
      current: result.successRecords,
      total: result.totalRecords,
      currentBatch: 0,
      totalBatches: 0,
      successCount: result.successRecords,
      errorCount: result.failedRecords + result.skippedRecords,
      error: result.success ? '' : result.message
    }

    console.log('🎯 导入流程完成，停留在第4步，不发送emit事件')
    
    // 移除emit事件调用，避免触发父组件的状态变化导致页面刷新
    // 注释掉原来的emit调用：
    // emit('import-complete', importResult)

  } catch (err) {
    console.error('导入失败:', err)
    importProgress.value.status = 'failed'
    importProgress.value.error = err instanceof Error ? err.message : '导入失败'
  }
}

const viewResults = () => {
  closeDialog()
  // 这里可以添加跳转到结果查看页面的逻辑
}

const closeDialog = () => {
  isOpen.value = false
}

// 重置状态
const resetState = () => {
  console.log('🔄 resetState被调用了！当前步骤:', currentStep.value)
  console.trace('resetState调用堆栈')

  // 取消轮询
  if (pollCancel) {
    pollCancel()
    pollCancel = null
  }

  currentStep.value = 0
  selectedFile.value = null
  error.value = ''
  jsonPreview.value = []
  jsonAnalysis.value = null
  importTaskStatus.value = null
  importProgress.value = {
    status: 'idle',
    percentage: 0,
    current: 0,
    total: 0,
    currentBatch: 0,
    totalBatches: 0,
    successCount: 0,
    errorCount: 0,
    error: ''
  }

  console.log('🔄 resetState执行完毕，步骤已重置为:', currentStep.value)
}

// 监听对话框开关状态
watch(isOpen, (newVal, oldVal) => {
  console.log('🔍 isOpen状态变化:', { 旧值: oldVal, 新值: newVal, 当前步骤: currentStep.value })
  
  if (!newVal && oldVal === true) {
    // 对话框关闭时重置状态（延迟执行以避免动画问题）
    console.log('🔄 对话框关闭，将在300ms后重置状态')
    setTimeout(() => {
      console.log('🔄 执行延迟重置状态，当前步骤:', currentStep.value)
      resetState()
    }, 300)
  } else if (newVal && oldVal === false) {
    console.log('🔍 对话框打开')
  }
})

// 组件卸载时清理资源
onUnmounted(() => {
  if (pollCancel) {
    pollCancel()
  }
})
</script>

<style scoped>
/* 自定义样式 */
.form-radio {
  @apply focus:ring-green-500 focus:ring-offset-0;
}
</style>