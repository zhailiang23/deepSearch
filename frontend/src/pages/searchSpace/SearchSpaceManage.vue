<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 页面标题 -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">搜索空间管理</h1>
        <p class="mt-2 text-gray-600 dark:text-gray-400">
          管理您的搜索空间，包括创建、编辑、启用和禁用操作
        </p>
      </div>

      <!-- 主内容 -->
      <SearchSpaceList
        @create="handleCreate"
        @edit="handleEdit"
        @view="handleView"
        @delete="handleDelete"
        @enable="handleEnable"
        @disable="handleDisable"
      />

      <!-- 创建/编辑对话框 -->
      <div
        v-if="showFormDialog"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
        @click.self="closeFormDialog"
      >
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
          <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h3 class="text-lg font-medium text-gray-900 dark:text-white">
              {{ currentSearchSpace ? '编辑搜索空间' : '创建搜索空间' }}
            </h3>
          </div>
          <div class="px-6 py-4">
            <SearchSpaceForm
              :search-space="currentSearchSpace"
              :loading="formLoading"
              @submit="handleFormSubmit"
              @cancel="closeFormDialog"
            />
          </div>
        </div>
      </div>

      <!-- 详情对话框 -->
      <div
        v-if="showDetailDialog"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
        @click.self="closeDetailDialog"
      >
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
            <h3 class="text-lg font-medium text-gray-900 dark:text-white">搜索空间详情</h3>
            <button
              @click="closeDetailDialog"
              class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            >
              <X class="h-6 w-6" />
            </button>
          </div>
          <div v-if="currentSearchSpace" class="px-6 py-4 space-y-6">
            <!-- 基本信息 -->
            <div>
              <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">基本信息</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">名称</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ currentSearchSpace.name }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">代码</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white font-mono">{{ currentSearchSpace.code }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">状态</dt>
                  <dd class="mt-1">
                    <SearchSpaceStatusBadge :status="currentSearchSpace.status" />
                  </dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">版本</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ currentSearchSpace.version }}</dd>
                </div>
              </div>
            </div>

            <!-- 描述 -->
            <div v-if="currentSearchSpace.description">
              <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">描述</h4>
              <p class="text-sm text-gray-600 dark:text-gray-400">{{ currentSearchSpace.description }}</p>
            </div>

            <!-- 索引状态 -->
            <div v-if="currentSearchSpace.indexStatus">
              <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">索引状态</h4>
              <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">索引名称</dt>
                    <dd class="mt-1 text-sm text-gray-900 dark:text-white font-mono">{{ currentSearchSpace.indexStatus.name }}</dd>
                  </div>
                  <div>
                    <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">是否存在</dt>
                    <dd class="mt-1">
                      <span
                        :class="[
                          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
                          currentSearchSpace.indexStatus.exists
                            ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                            : 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
                        ]"
                      >
                        {{ currentSearchSpace.indexStatus.exists ? '存在' : '不存在' }}
                      </span>
                    </dd>
                  </div>
                  <div>
                    <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">健康状态</dt>
                    <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ currentSearchSpace.indexStatus.health }}</dd>
                  </div>
                  <div v-if="currentSearchSpace.indexStatus.error">
                    <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">错误信息</dt>
                    <dd class="mt-1 text-sm text-red-600 dark:text-red-400">{{ currentSearchSpace.indexStatus.error }}</dd>
                  </div>
                </div>
              </div>
            </div>

            <!-- 时间信息 -->
            <div>
              <h4 class="text-sm font-medium text-gray-900 dark:text-white mb-3">时间信息</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">创建时间</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ formatDate(currentSearchSpace.createdAt) }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-gray-500 dark:text-gray-400">更新时间</dt>
                  <dd class="mt-1 text-sm text-gray-900 dark:text-white">{{ formatDate(currentSearchSpace.updatedAt) }}</dd>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 删除确认对话框 -->
      <div
        v-if="showDeleteDialog"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
        @click.self="closeDeleteDialog"
      >
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-md w-full">
          <div class="px-6 py-4">
            <div class="flex items-center">
              <div class="flex-shrink-0">
                <AlertTriangle class="h-6 w-6 text-red-600" />
              </div>
              <div class="ml-3">
                <h3 class="text-lg font-medium text-gray-900 dark:text-white">确认删除</h3>
                <div class="mt-2">
                  <p class="text-sm text-gray-500 dark:text-gray-400">
                    您确定要删除搜索空间 "<span class="font-medium">{{ deleteTarget?.name }}</span>" 吗？
                    此操作不可撤销。
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div class="px-6 py-4 bg-gray-50 dark:bg-gray-700 flex justify-end space-x-3">
            <button
              @click="closeDeleteDialog"
              type="button"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 dark:bg-gray-800 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-700"
            >
              取消
            </button>
            <button
              @click="confirmDelete"
              :disabled="deleteLoading"
              type="button"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ deleteLoading ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息提示 -->
    <div
      v-if="message"
      :class="[
        'fixed top-4 right-4 px-4 py-3 rounded-md shadow-lg z-50 transition-all duration-300',
        message.type === 'success'
          ? 'bg-green-100 text-green-800 border border-green-200'
          : 'bg-red-100 text-red-800 border border-red-200'
      ]"
    >
      <div class="flex items-center">
        <CheckCircle v-if="message.type === 'success'" class="h-5 w-5 mr-2" />
        <XCircle v-else class="h-5 w-5 mr-2" />
        {{ message.text }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { X, AlertTriangle, CheckCircle, XCircle } from 'lucide-vue-next'
import { useSearchSpaceStore } from '@/stores/searchSpace'
import SearchSpaceList from '@/components/searchSpace/SearchSpaceList.vue'
import SearchSpaceForm from '@/components/searchSpace/SearchSpaceForm.vue'
import SearchSpaceStatusBadge from '@/components/searchSpace/SearchSpaceStatusBadge.vue'
import type { SearchSpace, CreateSearchSpaceRequest, UpdateSearchSpaceRequest } from '@/types/searchSpace'

const searchSpaceStore = useSearchSpaceStore()

// 对话框状态
const showFormDialog = ref(false)
const showDetailDialog = ref(false)
const showDeleteDialog = ref(false)

// 当前操作的搜索空间
const currentSearchSpace = ref<SearchSpace | null>(null)
const deleteTarget = ref<SearchSpace | null>(null)

// 加载状态
const formLoading = ref(false)
const deleteLoading = ref(false)

// 消息提示
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

// 显示消息
const showMessage = (type: 'success' | 'error', text: string) => {
  message.value = { type, text }
  setTimeout(() => {
    message.value = null
  }, 3000)
}

// 格式化日期
const formatDate = (dateString: string) => {
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

// 创建搜索空间
const handleCreate = () => {
  currentSearchSpace.value = null
  showFormDialog.value = true
}

// 编辑搜索空间
const handleEdit = (space: SearchSpace) => {
  currentSearchSpace.value = space
  showFormDialog.value = true
}

// 查看搜索空间详情
const handleView = async (space: SearchSpace) => {
  try {
    // 获取最新的详细信息
    const latestSpace = await searchSpaceStore.fetchSearchSpaceById(space.id)
    currentSearchSpace.value = latestSpace
    showDetailDialog.value = true
  } catch (error) {
    showMessage('error', '获取搜索空间详情失败')
  }
}

// 删除搜索空间
const handleDelete = (space: SearchSpace) => {
  deleteTarget.value = space
  showDeleteDialog.value = true
}

// 启用搜索空间
const handleEnable = async (space: SearchSpace) => {
  try {
    await searchSpaceStore.enableSearchSpace(space.id)
    showMessage('success', `搜索空间 "${space.name}" 已启用`)
  } catch (error) {
    showMessage('error', '启用搜索空间失败')
  }
}

// 禁用搜索空间
const handleDisable = async (space: SearchSpace) => {
  try {
    await searchSpaceStore.disableSearchSpace(space.id)
    showMessage('success', `搜索空间 "${space.name}" 已禁用`)
  } catch (error) {
    showMessage('error', '禁用搜索空间失败')
  }
}

// 表单提交
const handleFormSubmit = async (data: CreateSearchSpaceRequest | UpdateSearchSpaceRequest) => {
  try {
    formLoading.value = true

    if (currentSearchSpace.value) {
      // 更新
      const updateData = data as UpdateSearchSpaceRequest
      await searchSpaceStore.updateSearchSpace(currentSearchSpace.value.id, updateData)
      showMessage('success', '搜索空间更新成功')
    } else {
      // 创建
      const createData = data as CreateSearchSpaceRequest
      await searchSpaceStore.createSearchSpace(createData)
      showMessage('success', '搜索空间创建成功')
    }

    closeFormDialog()
  } catch (error: any) {
    showMessage('error', error.message || '操作失败')
  } finally {
    formLoading.value = false
  }
}

// 确认删除
const confirmDelete = async () => {
  if (!deleteTarget.value) return

  try {
    deleteLoading.value = true
    await searchSpaceStore.deleteSearchSpace(deleteTarget.value.id)
    showMessage('success', `搜索空间 "${deleteTarget.value.name}" 已删除`)
    closeDeleteDialog()
  } catch (error: any) {
    showMessage('error', error.message || '删除失败')
  } finally {
    deleteLoading.value = false
  }
}

// 关闭对话框
const closeFormDialog = () => {
  showFormDialog.value = false
  currentSearchSpace.value = null
  formLoading.value = false
}

const closeDetailDialog = () => {
  showDetailDialog.value = false
  currentSearchSpace.value = null
}

const closeDeleteDialog = () => {
  showDeleteDialog.value = false
  deleteTarget.value = null
  deleteLoading.value = false
}

// 初始化
onMounted(() => {
  // 清理状态
  searchSpaceStore.reset()
})
</script>