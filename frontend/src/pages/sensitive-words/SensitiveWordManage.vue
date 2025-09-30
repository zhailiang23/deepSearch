<template>
  <div class="container mx-auto px-4 py-8">
    <!-- 敏感词表格 -->
    <SensitiveWordTable
      :statistics="statistics"
      @create="handleCreate"
      @edit="handleEdit"
      @view="handleView"
      @delete="handleDelete"
    />

    <!-- 创建/编辑表单模态框 -->
    <SensitiveWordForm
      v-if="showForm"
      :sensitive-word="currentSensitiveWord"
      @close="handleCloseForm"
      @success="handleFormSuccess"
    />

    <!-- 删除确认对话框 -->
    <div
      v-if="showDeleteConfirm"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
    >
      <div class="bg-background rounded-lg shadow-xl w-full max-w-md">
        <div class="p-6">
          <h3 class="text-lg font-medium text-foreground mb-4">删除敏感词</h3>
          <p class="text-muted-foreground mb-6">
            确定要删除敏感词「{{ currentSensitiveWord?.name }}」吗？此操作不可撤销。
          </p>
          <div class="flex items-center justify-end space-x-3">
            <button
              @click="handleCancelDelete"
              class="px-4 py-2 text-sm font-medium text-muted-foreground bg-background border border-border rounded-md hover:bg-accent"
            >
              取消
            </button>
            <button
              @click="handleConfirmDelete"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md hover:bg-red-700"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 简单通知 -->
    <div
      v-if="notification.show"
      class="fixed top-4 right-4 bg-background border border-border rounded-lg shadow-lg p-4 z-50"
      :class="{
        'border-green-500': notification.type === 'success',
        'border-red-500': notification.type === 'error'
      }"
    >
      <div class="flex items-start">
        <div class="flex-1">
          <p class="text-sm font-medium text-foreground">{{ notification.title }}</p>
          <p class="text-sm text-muted-foreground mt-1">{{ notification.message }}</p>
        </div>
        <button
          @click="hideNotification"
          class="ml-2 text-muted-foreground hover:text-foreground"
        >
          ×
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import SensitiveWordTable from '@/components/sensitiveWord/SensitiveWordTable.vue'
import SensitiveWordForm from '@/components/sensitiveWord/SensitiveWordForm.vue'
import { useSensitiveWordStore } from '@/stores/sensitiveWord'
import type { SensitiveWord, SensitiveWordStatistics } from '@/types/sensitiveWord'

const store = useSensitiveWordStore()

// 状态管理
const showForm = ref(false)
const showDeleteConfirm = ref(false)
const currentSensitiveWord = ref<SensitiveWord | null>(null)
const statistics = ref<SensitiveWordStatistics | null>(null)

// 通知状态
const notification = ref({
  show: false,
  type: 'success' as 'success' | 'error' | 'warning' | 'info',
  title: '',
  message: ''
})

// 显示通知
const showNotification = (type: typeof notification.value.type, title: string, message: string) => {
  notification.value = {
    show: true,
    type,
    title,
    message
  }

  // 3秒后自动隐藏
  setTimeout(() => {
    hideNotification()
  }, 3000)
}

// 隐藏通知
const hideNotification = () => {
  notification.value.show = false
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    statistics.value = await store.getStatistics()
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 事件处理函数
const handleCreate = () => {
  currentSensitiveWord.value = null
  showForm.value = true
}

const handleEdit = (word: SensitiveWord) => {
  currentSensitiveWord.value = word
  showForm.value = true
}

const handleView = (word: SensitiveWord) => {
  // 这里可以添加查看详情的逻辑
  console.log('查看敏感词:', word.name)
}

const handleDelete = (word: SensitiveWord) => {
  currentSensitiveWord.value = word
  showDeleteConfirm.value = true
}

const handleCloseForm = () => {
  showForm.value = false
  currentSensitiveWord.value = null
}

const handleFormSuccess = async (word: SensitiveWord) => {
  const isEdit = currentSensitiveWord.value !== null
  showNotification(
    'success',
    isEdit ? '更新成功' : '创建成功',
    `敏感词「${word.name}」已${isEdit ? '更新' : '创建'}成功`
  )

  // 重新加载统计信息
  await loadStatistics()
}

const handleConfirmDelete = async () => {
  if (!currentSensitiveWord.value) return

  try {
    const wordName = currentSensitiveWord.value.name
    await store.deleteSensitiveWord(currentSensitiveWord.value.id)

    showNotification(
      'success',
      '删除成功',
      `敏感词「${wordName}」已删除成功`
    )

    // 重新加载统计信息
    await loadStatistics()
  } catch (error) {
    showNotification(
      'error',
      '删除失败',
      error instanceof Error ? error.message : '删除敏感词失败'
    )
  } finally {
    showDeleteConfirm.value = false
    currentSensitiveWord.value = null
  }
}

const handleCancelDelete = () => {
  showDeleteConfirm.value = false
  currentSensitiveWord.value = null
}

// 初始化
onMounted(async () => {
  await loadStatistics()
})
</script>

<style scoped>
.container {
  max-width: 1200px;
}
</style>