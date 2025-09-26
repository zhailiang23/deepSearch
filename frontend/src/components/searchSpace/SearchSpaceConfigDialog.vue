<template>
  <div
    v-if="open"
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
    @click.self="$emit('close')"
  >
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-hidden">
      <!-- 标题栏 -->
      <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
        <h3 class="text-lg font-medium text-gray-900 dark:text-white">
          搜索空间配置 - {{ searchSpace?.name }}
        </h3>
        <button
          @click="$emit('close')"
          class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
        >
          <X class="h-6 w-6" />
        </button>
      </div>

      <!-- Tab 导航 -->
      <div class="border-b border-gray-200 dark:border-gray-700">
        <nav class="flex space-x-8 px-6">
          <button
            @click="activeTab = 'view'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors',
              activeTab === 'view'
                ? 'border-green-500 text-green-600 dark:text-green-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
            ]"
          >
            <Eye class="h-4 w-4 mr-2 inline-block" />
            查看详情
          </button>
          <button
            @click="activeTab = 'edit'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors',
              activeTab === 'edit'
                ? 'border-green-500 text-green-600 dark:text-green-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
            ]"
          >
            <Edit class="h-4 w-4 mr-2 inline-block" />
            编辑配置
          </button>
          <button
            @click="activeTab = 'mapping'"
            :class="[
              'py-4 px-1 border-b-2 font-medium text-sm transition-colors',
              activeTab === 'mapping'
                ? 'border-green-500 text-green-600 dark:text-green-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
            ]"
          >
            <Settings class="h-4 w-4 mr-2 inline-block" />
            Mapping 配置
          </button>
        </nav>
      </div>

      <!-- Tab 内容 -->
      <div :class="[
        activeTab === 'mapping' ? 'h-[calc(90vh-140px)]' : 'px-6 py-4 overflow-y-auto max-h-[calc(90vh-140px)]'
      ]">
        <!-- 查看详情 Tab -->
        <div v-if="activeTab === 'view'" class="space-y-6">
          <!-- 基本信息 -->
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">基本信息</h4>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">名称</dt>
                <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ searchSpace?.name }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">代码</dt>
                <dd class="mt-1 text-sm text-gray-900 dark:text-white font-mono">{{ searchSpace?.code }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">状态</dt>
                <dd class="mt-1">
                  <SearchSpaceStatusBadge v-if="searchSpace?.status" :status="searchSpace.status" />
                </dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">版本</dt>
                <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ searchSpace?.version }}</dd>
              </div>
            </div>
          </div>

          <!-- 描述 -->
          <div v-if="searchSpace?.description">
            <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">描述</h4>
            <p class="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap break-words">{{ searchSpace.description }}</p>
          </div>

          <!-- 索引状态 -->
          <div v-if="searchSpace?.indexStatus">
            <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">索引状态</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">索引名称</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white font-mono">{{ searchSpace.indexStatus.name }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">是否存在</dt>
                  <dd class="mt-1">
                    <span
                      :class="[
                        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                        searchSpace.indexStatus.exists
                          ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                          : 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
                      ]"
                    >
                      {{ searchSpace.indexStatus.exists ? '存在' : '不存在' }}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">健康状态</dt>
                  <dd class="mt-1">
                    <span
                      :class="[
                        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                        searchSpace.indexStatus.health === 'green'
                          ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                          : searchSpace.indexStatus.health === 'yellow'
                          ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400'
                          : 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
                      ]"
                    >
                      {{ searchSpace.indexStatus.health }}
                    </span>
                  </dd>
                </div>
                <div v-if="searchSpace.indexStatus.exists && searchSpace.indexStatus.docsCount !== undefined">
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">文档数量</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ searchSpace.indexStatus.docsCount.toLocaleString() }}</dd>
                </div>
                <div v-if="searchSpace.indexStatus.primaryShards">
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">主分片数</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ searchSpace.indexStatus.primaryShards }}</dd>
                </div>
                <div v-if="searchSpace.indexStatus.replicas !== undefined">
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">副本数</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ searchSpace.indexStatus.replicas }}</dd>
                </div>
                <div v-if="searchSpace.indexStatus.error" class="md:col-span-2">
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">错误信息</dt>
                  <dd class="mt-1 text-sm text-red-600 dark:text-red-400 break-words">{{ searchSpace.indexStatus.error }}</dd>
                </div>
              </div>
          </div>

          <!-- 时间信息 -->
          <div>
            <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">时间信息</h4>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">创建时间</dt>
                <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ formatDate(searchSpace?.createdAt) }}</dd>
              </div>
              <div>
                <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">更新时间</dt>
                <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ formatDate(searchSpace?.updatedAt) }}</dd>
              </div>
            </div>
          </div>
        </div>

        <!-- 编辑配置 Tab -->
        <div v-else-if="activeTab === 'edit'">
          <SearchSpaceForm
            :search-space="searchSpace"
            :loading="loading"
            @submit="handleFormSubmit"
            @cancel="() => activeTab = 'view'"
          />
        </div>

        <!-- Mapping 配置 Tab -->
        <div v-else-if="activeTab === 'mapping'" class="h-full">
          <MappingConfigTab
            v-if="searchSpace?.id"
            v-model="mappingConfig"
            :space-id="searchSpace.id"
            @save="handleMappingSave"
            @validate="handleMappingValidate"
            @load-error="handleMappingLoadError"
            @save-error="handleMappingSaveError"
            @save-success="handleMappingSaveSuccess"
          />
          <div v-else class="flex items-center justify-center h-full text-gray-500 dark:text-gray-400">
            无法加载映射配置：搜索空间ID不可用
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Eye, Edit, Settings } from 'lucide-vue-next'
import SearchSpaceForm from './SearchSpaceForm.vue'
import SearchSpaceStatusBadge from './SearchSpaceStatusBadge.vue'
import MappingConfigTab from './MappingConfigTab.vue'
import type { SearchSpace, UpdateSearchSpaceRequest } from '@/types/searchSpace'

interface Props {
  open: boolean
  searchSpace: SearchSpace | null
  loading?: boolean
  initialTab?: 'view' | 'edit' | 'mapping'
}

interface Emits {
  (e: 'close'): void
  (e: 'submit', data: UpdateSearchSpaceRequest): void
  (e: 'saveMappingConfig', data: { searchSpaceId: number, mappingConfig: string }): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  initialTab: 'view'
})

const emit = defineEmits<Emits>()

// 当前激活的 Tab
const activeTab = ref<'view' | 'edit' | 'mapping'>(props.initialTab)

// Mapping 配置相关状态
const mappingConfig = ref('')
const mappingSaving = ref(false)
const mappingValid = ref(true)

// 监听 props 变化
watch(() => props.open, (newOpen) => {
  if (newOpen) {
    activeTab.value = props.initialTab
    // 当弹窗打开时初始化 mapping 配置
    initializeMappingConfig()
  }
})

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 处理表单提交
const handleFormSubmit = (data: UpdateSearchSpaceRequest) => {
  emit('submit', data)
  // 提交成功后切换到查看 tab
  activeTab.value = 'view'
}

// 处理 Mapping 配置保存
const handleMappingSave = async (config: string) => {
  if (!props.searchSpace?.id) return

  try {
    mappingSaving.value = true
    emit('saveMappingConfig', {
      searchSpaceId: props.searchSpace.id,
      mappingConfig: config
    })
  } finally {
    mappingSaving.value = false
  }
}

// 处理 Mapping 配置验证
const handleMappingValidate = (isValid: boolean) => {
  mappingValid.value = isValid
}

// 处理 Mapping 加载错误
const handleMappingLoadError = (error: string) => {
  console.error('Mapping 加载错误:', error)
  // 这里可以添加通知或者其他处理逻辑
}

// 处理 Mapping 保存错误
const handleMappingSaveError = (error: string) => {
  console.error('Mapping 保存错误:', error)
  mappingSaving.value = false
  // 这里可以添加通知或者其他处理逻辑
}

// 处理 Mapping 保存成功
const handleMappingSaveSuccess = () => {
  console.log('Mapping 保存成功')
  mappingSaving.value = false
  // 这里可以添加成功通知
}

// 初始化默认的 mapping 配置
const initializeMappingConfig = () => {
  if (!mappingConfig.value) {
    mappingConfig.value = JSON.stringify({
      "mappings": {
        "properties": {
          "title": {
            "type": "text",
            "analyzer": "standard"
          },
          "content": {
            "type": "text",
            "analyzer": "standard"
          },
          "created_at": {
            "type": "date"
          },
          "updated_at": {
            "type": "date"
          }
        }
      },
      "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
        "analysis": {
          "analyzer": {
            "default": {
              "type": "standard"
            }
          }
        }
      }
    }, null, 2)
  }
}
</script>