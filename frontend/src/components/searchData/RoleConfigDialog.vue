<template>
  <!-- 只有当open为true时才渲染 -->
  <div
    v-if="open"
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
    @click.self="closeDialog"
  >
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full flex flex-col border">
      <!-- 对话框头部 -->
      <div class="flex flex-col space-y-3 flex-shrink-0 p-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <div class="p-2 bg-purple-100 rounded-lg mr-3">
              <Shield class="h-6 w-6 text-purple-600" />
            </div>
            <h3 class="text-xl font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
              角色配置
            </h3>
          </div>
          <button
            class="p-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 rounded-md"
            @click="closeDialog"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <!-- 说明信息 -->
        <div class="bg-purple-50 border border-purple-200 rounded-lg p-4">
          <div class="flex items-start">
            <Info class="h-5 w-5 text-purple-500 mt-0.5 mr-3" />
            <div class="flex-1">
              <p class="text-sm text-purple-700">
                为文档配置可访问的角色白名单，选中的角色用户可以搜索到此文档。未选择任何角色表示该文档对所有角色开放。
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 对话框内容 -->
      <div class="flex-1 overflow-hidden px-6">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex items-center justify-center py-8">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600"></div>
          <span class="ml-3 text-gray-600">加载中...</span>
        </div>

        <!-- 错误提示 -->
        <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4">
          <div class="flex items-start">
            <AlertCircle class="h-5 w-5 text-red-500 mt-0.5 mr-3" />
            <div class="flex-1">
              <p class="text-sm text-red-700">{{ error }}</p>
            </div>
          </div>
        </div>

        <!-- 角色列表 -->
        <div v-else-if="roles.length > 0" class="space-y-2 max-h-96 overflow-y-auto">
          <label
            v-for="role in roles"
            :key="role.id"
            class="flex items-center gap-3 p-3 rounded-lg border border-gray-200 hover:bg-gray-50 cursor-pointer transition-colors"
          >
            <input
              type="checkbox"
              :value="role.code"
              v-model="selectedRoles"
              class="w-4 h-4 text-purple-600 rounded border-gray-300 focus:ring-purple-500"
            />
            <div class="flex-1">
              <div class="font-medium text-gray-900">{{ role.name }}</div>
              <div class="text-sm text-gray-500">{{ role.code }}</div>
              <div v-if="role.description" class="text-xs text-gray-400 mt-1">
                {{ role.description }}
              </div>
            </div>
          </label>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-8 text-gray-500">
          <div class="text-4xl mb-2">👥</div>
          <div class="text-sm">暂无可用角色</div>
        </div>

        <!-- 已选择提示 -->
        <div v-if="!loading && !error && roles.length > 0" class="mt-4 text-sm text-gray-600 bg-gray-50 rounded p-3">
          已选择 <span class="font-medium text-purple-600">{{ selectedRoles.length }}</span> 个角色
          <span v-if="selectedRoles.length === 0" class="text-orange-600">
            （未选择表示对所有角色开放）
          </span>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="flex justify-end space-x-3 p-6 border-t">
        <button
          @click="closeDialog"
          :disabled="saving"
          class="px-6 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors disabled:opacity-50"
        >
          取消
        </button>
        <button
          @click="handleSave"
          :disabled="loading || saving"
          class="px-6 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded transition-colors disabled:opacity-50 flex items-center"
        >
          <Check v-if="!saving" class="w-4 h-4 mr-2" />
          <div v-else class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-white border-t-transparent"></div>
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  Shield,
  Info,
  AlertCircle,
  X,
  Check
} from 'lucide-vue-next'
import { roleApi } from '@/services/roleApi'
import { searchDataService } from '@/services/searchDataService'
import type { Role } from '@/types/role'
import type { TableRow } from '@/types/tableData'

interface Props {
  open: boolean
  document: TableRow | null
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const roles = ref<Role[]>([])
const selectedRoles = ref<string[]>([])

// 监听对话框打开，加载数据
watch(() => props.open, async (isOpen) => {
  if (isOpen && props.document) {
    await loadData()
  }
})

/**
 * 加载角色列表和当前文档的角色配置
 */
const loadData = async () => {
  loading.value = true
  error.value = null

  try {
    // 加载所有可用角色
    const response = await roleApi.getAll()
    roles.value = response.data

    // 加载当前文档的角色白名单
    if (props.document?._source?.request_role_white_list) {
      selectedRoles.value = [...props.document._source.request_role_white_list]
    } else {
      selectedRoles.value = []
    }
  } catch (err: any) {
    error.value = err.message || '加载角色列表失败'
    console.error('加载角色数据失败:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 保存角色配置
 */
const handleSave = async () => {
  if (!props.document) return

  saving.value = true
  error.value = null

  try {
    // 调用API更新文档角色配置
    await searchDataService.updateDocumentRoles(
      props.document._id,
      props.document._index,
      selectedRoles.value
    )

    // 关闭对话框并通知父组件刷新
    emit('update:open', false)
    emit('success')
  } catch (err: any) {
    error.value = err.message || '保存失败，请重试'
    console.error('保存角色配置失败:', err)
  } finally {
    saving.value = false
  }
}

/**
 * 关闭对话框
 */
const closeDialog = () => {
  if (!saving.value) {
    emit('update:open', false)
  }
}
</script>

<style scoped>
/* 自定义滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>