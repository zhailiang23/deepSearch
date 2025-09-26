<template>
  <div class="min-h-screen">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      
      <!-- 主内容 -->
      <SearchSpaceList
      @create="handleCreate"
      @config="handleConfig"
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

    <!-- 配置对话框 -->
    <SearchSpaceConfigDialog
      :open="showConfigDialog"
      :search-space="currentSearchSpace"
      :loading="formLoading"
      :initial-tab="configDialogTab"
      @close="closeConfigDialog"
      @submit="handleConfigSubmit"
    />


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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CheckCircle, XCircle } from 'lucide-vue-next'
import { useSearchSpaceStore } from '@/stores/searchSpace'
import SearchSpaceList from '@/components/searchSpace/SearchSpaceList.vue'
import SearchSpaceForm from '@/components/searchSpace/SearchSpaceForm.vue'
import SearchSpaceConfigDialog from '@/components/searchSpace/SearchSpaceConfigDialog.vue'
import type { SearchSpace, CreateSearchSpaceRequest, UpdateSearchSpaceRequest } from '@/types/searchSpace'

const searchSpaceStore = useSearchSpaceStore()

// 对话框状态
const showFormDialog = ref(false)
const showConfigDialog = ref(false)
const configDialogTab = ref<'view' | 'edit'>('view')

// 当前操作的搜索空间
const currentSearchSpace = ref<SearchSpace | null>(null)

// 加载状态
const formLoading = ref(false)

// 消息提示
const message = ref<{ type: 'success' | 'error'; text: string } | null>(null)

// 显示消息
const showMessage = (type: 'success' | 'error', text: string) => {
  message.value = { type, text }
  setTimeout(() => {
    message.value = null
  }, 3000)
}


// 事件处理函数
const handleCreate = () => {
  currentSearchSpace.value = null
  showFormDialog.value = true
}

// 配置搜索空间（合并编辑和查看功能）
const handleConfig = async (space: SearchSpace) => {
  try {
    // 获取最新的详细信息
    const latestSpace = await searchSpaceStore.fetchSearchSpaceById(space.id)
    currentSearchSpace.value = latestSpace
    configDialogTab.value = 'view' // 默认打开查看tab
    showConfigDialog.value = true
  } catch (error) {
    showMessage('error', '获取搜索空间详情失败')
  }
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


// 关闭对话框
const closeFormDialog = () => {
  showFormDialog.value = false
  currentSearchSpace.value = null
  formLoading.value = false
}

// 配置对话框提交
const handleConfigSubmit = async (data: UpdateSearchSpaceRequest) => {
  try {
    formLoading.value = true

    if (currentSearchSpace.value) {
      await searchSpaceStore.updateSearchSpace(currentSearchSpace.value.id, data)
      showMessage('success', '搜索空间更新成功')

      // 更新当前搜索空间数据
      const updatedSpace = await searchSpaceStore.fetchSearchSpaceById(currentSearchSpace.value.id)
      currentSearchSpace.value = updatedSpace
    }
  } catch (error: any) {
    showMessage('error', error.message || '更新失败')
  } finally {
    formLoading.value = false
  }
}

const closeConfigDialog = () => {
  showConfigDialog.value = false
  currentSearchSpace.value = null
  formLoading.value = false
  configDialogTab.value = 'view'
}

</script>