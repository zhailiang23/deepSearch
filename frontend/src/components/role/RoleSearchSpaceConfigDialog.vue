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
            <div class="p-2 bg-green-100 rounded-lg mr-3">
              <Settings class="h-6 w-6 text-green-600" />
            </div>
            <h3 class="text-xl font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
              配置搜索空间权限
            </h3>
          </div>
          <button
            class="p-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 rounded-md"
            @click="closeDialog"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <!-- 角色信息 -->
        <div v-if="role" class="bg-green-50 border border-green-200 rounded-lg p-4">
          <div class="flex items-start">
            <Info class="h-5 w-5 text-green-500 mt-0.5 mr-3" />
            <div class="flex-1">
              <p class="text-sm text-green-700">
                为角色 <span class="font-semibold">{{ role.name }}</span> ({{ role.code }}) 配置可访问的搜索空间。选中的搜索空间将允许该角色的用户进行访问。
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 对话框内容 -->
      <div class="flex-1 overflow-hidden px-6">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex items-center justify-center py-8">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600"></div>
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

        <!-- 搜索空间列表 -->
        <div v-else-if="searchSpaces.length > 0" class="space-y-2 max-h-96 overflow-y-auto">
          <label
            v-for="space in searchSpaces"
            :key="space.id"
            class="flex items-center gap-3 p-3 rounded-lg border border-gray-200 hover:bg-gray-50 cursor-pointer transition-colors"
            :class="{ 'bg-gray-50': selectedSpaceIds.includes(space.id) }"
          >
            <input
              type="checkbox"
              :value="space.id"
              v-model="selectedSpaceIds"
              class="w-4 h-4 text-green-600 rounded border-gray-300 focus:ring-green-500"
            />
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <span class="font-medium text-gray-900">{{ space.name }}</span>
                <span
                  class="px-2 py-0.5 text-xs rounded-full"
                  :class="{
                    'bg-green-100 text-green-700': space.status === 'ACTIVE',
                    'bg-gray-100 text-gray-700': space.status === 'INACTIVE',
                    'bg-yellow-100 text-yellow-700': space.status === 'MAINTENANCE'
                  }"
                >
                  {{ getStatusText(space.status) }}
                </span>
              </div>
              <div class="text-sm text-gray-500">{{ space.code }}</div>
              <div v-if="space.description" class="text-xs text-gray-400 mt-1">
                {{ space.description }}
              </div>
            </div>
          </label>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-8 text-gray-500">
          <div class="text-4xl mb-2">📁</div>
          <div class="text-sm">暂无可用搜索空间</div>
        </div>

        <!-- 已选择提示 -->
        <div v-if="!loading && !error && searchSpaces.length > 0" class="mt-4 text-sm text-gray-600 bg-gray-50 rounded p-3">
          已选择 <span class="font-medium text-green-600">{{ selectedSpaceIds.length }}</span> 个搜索空间
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
          class="px-6 py-2 bg-green-600 hover:bg-green-700 text-white rounded transition-colors disabled:opacity-50 flex items-center"
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
  Settings,
  Info,
  AlertCircle,
  X,
  Check
} from 'lucide-vue-next'
import { useRoleStore } from '@/stores/role'
import type { Role } from '@/types/role'
import type { SearchSpace } from '@/types/searchSpace'

interface Props {
  open: boolean
  role: Role | null
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const roleStore = useRoleStore()

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const searchSpaces = ref<SearchSpace[]>([])
const selectedSpaceIds = ref<number[]>([])

// 状态文本映射
const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'ACTIVE': '活跃',
    'INACTIVE': '未激活',
    'MAINTENANCE': '维护中',
    'DELETED': '已删除'
  }
  return statusMap[status] || status
}

// 监听对话框打开,加载数据
watch(() => props.open, async (isOpen) => {
  if (isOpen && props.role) {
    await loadData()
  }
})

/**
 * 加载搜索空间列表和当前角色的配置
 */
const loadData = async () => {
  if (!props.role) return

  loading.value = true
  error.value = null

  try {
    // 并行加载所有可用搜索空间和当前角色已配置的搜索空间
    const [availableSpaces, configuredSpaces] = await Promise.all([
      roleStore.getAvailableSearchSpaces(props.role.id),
      roleStore.getRoleSearchSpaces(props.role.id)
    ])

    searchSpaces.value = availableSpaces
    selectedSpaceIds.value = configuredSpaces.map(space => space.id)
  } catch (err: any) {
    error.value = err.message || '加载搜索空间列表失败'
    console.error('加载搜索空间数据失败:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 保存搜索空间配置
 */
const handleSave = async () => {
  if (!props.role) return

  saving.value = true
  error.value = null

  try {
    // 调用API更新角色的搜索空间配置
    await roleStore.configureSearchSpaces(props.role.id, selectedSpaceIds.value)

    // 关闭对话框并通知父组件刷新
    emit('update:open', false)
    emit('success')
  } catch (err: any) {
    error.value = err.message || '保存失败,请重试'
    console.error('保存搜索空间配置失败:', err)
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
