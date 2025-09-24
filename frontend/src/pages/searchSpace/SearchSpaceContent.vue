<template>
  <div>
    <!-- 主内容 -->
    <SearchSpaceList
      @create="handleCreate"
      @edit="handleEdit"
      @view="handleView"
      @delete="handleDelete"
      @enable="handleEnable"
      @disable="handleDisable"
      @import="handleImport"
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
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <h3 class="text-lg font-medium text-gray-900 dark:text-white">搜索空间详情</h3>
          <button
            @click="closeDetailDialog"
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
          >
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="px-6 py-4 space-y-4" v-if="currentSearchSpace">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">名称</label>
              <p class="text-gray-900 dark:text-gray-100">{{ currentSearchSpace.name }}</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">代码</label>
              <p class="text-gray-900 dark:text-gray-100 font-mono">{{ currentSearchSpace.code }}</p>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">描述</label>
            <p class="text-gray-900 dark:text-gray-100">{{ currentSearchSpace.description || '无' }}</p>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">状态</label>
              <SearchSpaceStatusBadge :status="currentSearchSpace.status" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">创建时间</label>
              <p class="text-gray-900 dark:text-gray-100">{{ formatDate(currentSearchSpace.createdAt) }}</p>
            </div>
          </div>
          <div v-if="'configuration' in currentSearchSpace">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">配置</label>
            <pre class="bg-gray-100 dark:bg-gray-700 p-3 rounded text-sm overflow-x-auto">{{ JSON.stringify((currentSearchSpace as any).configuration, null, 2) }}</pre>
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
        <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
          <h3 class="text-lg font-medium text-gray-900 dark:text-white flex items-center">
            <AlertTriangle class="h-5 w-5 text-red-500 mr-2" />
            确认删除
          </h3>
        </div>
        <div class="px-6 py-4">
          <p class="text-gray-700 dark:text-gray-300 mb-4">
            确定要删除搜索空间 "{{ deleteTarget?.name }}" 吗？此操作不可撤销。
          </p>
          <div class="flex justify-end space-x-3">
            <button
              @click="closeDeleteDialog"
              class="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
              :disabled="deleteLoading"
            >
              取消
            </button>
            <button
              @click="confirmDelete"
              class="px-4 py-2 bg-red-600 text-white hover:bg-red-700 rounded transition-colors disabled:opacity-50"
              :disabled="deleteLoading"
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
      class="fixed top-4 right-4 z-50 max-w-sm bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-4"
      :class="{
        'border-green-200 dark:border-green-800': message.type === 'success',
        'border-red-200 dark:border-red-800': message.type === 'error'
      }"
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
import { ref } from 'vue'
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

// 事件处理函数
const handleCreate = () => {
  currentSearchSpace.value = null
  showFormDialog.value = true
}

const handleEdit = (searchSpace: SearchSpace) => {
  currentSearchSpace.value = { ...searchSpace }
  showFormDialog.value = true
}

const handleView = (searchSpace: SearchSpace) => {
  currentSearchSpace.value = searchSpace
  showDetailDialog.value = true
}

const handleDelete = (searchSpace: SearchSpace) => {
  deleteTarget.value = searchSpace
  showDeleteDialog.value = true
}

const handleEnable = async (searchSpace: SearchSpace) => {
  try {
    await searchSpaceStore.updateSearchSpace(searchSpace.id, { status: 'ACTIVE' })
    showMessage('success', '搜索空间已启用')
  } catch (error) {
    showMessage('error', '启用失败：' + (error as Error).message)
  }
}

const handleDisable = async (searchSpace: SearchSpace) => {
  try {
    await searchSpaceStore.updateSearchSpace(searchSpace.id, { status: 'INACTIVE' })
    showMessage('success', '搜索空间已禁用')
  } catch (error) {
    showMessage('error', '禁用失败：' + (error as Error).message)
  }
}

const handleImport = async (searchSpace: SearchSpace) => {
  try {
    // 这里暂时只显示一个消息，实际的文件处理逻辑将在后续实现
    showMessage('success', `准备导入数据到搜索空间: ${searchSpace.name}`)
    console.log('导入功能触发，搜索空间:', searchSpace)
  } catch (error) {
    showMessage('error', '导入失败：' + (error as Error).message)
  }
}

// 表单提交
const handleFormSubmit = async (data: CreateSearchSpaceRequest | UpdateSearchSpaceRequest) => {
  formLoading.value = true
  try {
    if (currentSearchSpace.value) {
      // 编辑
      await searchSpaceStore.updateSearchSpace(currentSearchSpace.value.id, data as UpdateSearchSpaceRequest)
      showMessage('success', '搜索空间更新成功')
    } else {
      // 创建
      await searchSpaceStore.createSearchSpace(data as CreateSearchSpaceRequest)
      showMessage('success', '搜索空间创建成功')
    }
    closeFormDialog()
  } catch (error) {
    showMessage('error', currentSearchSpace.value ? '更新失败：' + (error as Error).message : '创建失败：' + (error as Error).message)
  } finally {
    formLoading.value = false
  }
}

// 确认删除
const confirmDelete = async () => {
  if (!deleteTarget.value) return

  deleteLoading.value = true
  try {
    await searchSpaceStore.deleteSearchSpace(deleteTarget.value.id)
    showMessage('success', '搜索空间删除成功')
    closeDeleteDialog()
  } catch (error) {
    showMessage('error', '删除失败：' + (error as Error).message)
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
</script>