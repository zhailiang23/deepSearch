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
            <div class="p-2 bg-emerald-100 rounded-lg mr-3">
              <Settings class="h-6 w-6 text-emerald-600" />
            </div>
            <h3 class="text-xl font-semibold leading-none tracking-tight text-gray-900 dark:text-white">
              文档配置
            </h3>
          </div>
          <button
            class="p-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 rounded-md"
            @click="closeDialog"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <!-- Tab 标签页 -->
        <div class="flex border-b border-gray-200">
          <button
            @click="activeTab = 'channel'"
            :class="[
              'px-4 py-2 font-medium text-sm transition-colors',
              activeTab === 'channel'
                ? 'text-emerald-600 border-b-2 border-emerald-600'
                : 'text-gray-500 hover:text-gray-700'
            ]"
          >
            渠道配置
          </button>
          <button
            @click="activeTab = 'role'"
            :class="[
              'px-4 py-2 font-medium text-sm transition-colors',
              activeTab === 'role'
                ? 'text-purple-600 border-b-2 border-purple-600'
                : 'text-gray-500 hover:text-gray-700'
            ]"
          >
            角色配置
          </button>
        </div>
      </div>

      <!-- 对话框内容 -->
      <div class="flex-1 overflow-hidden">
        <!-- 渠道配置 Tab -->
        <div v-show="activeTab === 'channel'" class="px-6">
          <!-- 说明信息 -->
          <div class="bg-emerald-50 border border-emerald-200 rounded-lg p-4 mb-4">
            <div class="flex items-start">
              <Info class="h-5 w-5 text-emerald-500 mt-0.5 mr-3" />
              <div class="flex-1">
                <p class="text-sm text-emerald-700">
                  为文档配置可访问的渠道白名单,选中的渠道用户可以搜索到此文档。未选择任何渠道表示该文档对所有渠道开放。
                </p>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="channelLoading" class="flex items-center justify-center py-8">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
            <span class="ml-3 text-gray-600">加载中...</span>
          </div>

          <!-- 错误提示 -->
          <div v-else-if="channelError" class="bg-red-50 border border-red-200 rounded-lg p-4">
            <div class="flex items-start">
              <AlertCircle class="h-5 w-5 text-red-500 mt-0.5 mr-3" />
              <div class="flex-1">
                <p class="text-sm text-red-700">{{ channelError }}</p>
              </div>
            </div>
          </div>

          <!-- 渠道列表 -->
          <div v-else-if="channels.length > 0" class="space-y-2 max-h-96 overflow-y-auto">
            <label
              v-for="channel in channels"
              :key="channel.id"
              class="flex items-center gap-3 p-3 rounded-lg border border-gray-200 hover:bg-gray-50 cursor-pointer transition-colors"
            >
              <input
                type="checkbox"
                :value="channel.code"
                v-model="selectedChannels"
                class="w-4 h-4 text-emerald-600 rounded border-gray-300 focus:ring-emerald-500"
              />
              <div class="flex-1">
                <div class="font-medium text-gray-900">{{ channel.name }}</div>
                <div class="text-sm text-gray-500">{{ channel.code }}</div>
                <div v-if="channel.description" class="text-xs text-gray-400 mt-1">
                  {{ channel.description }}
                </div>
              </div>
            </label>
          </div>

          <!-- 空状态 -->
          <div v-else class="text-center py-8 text-gray-500">
            <div class="text-4xl mb-2">📋</div>
            <div class="text-sm">暂无可用渠道</div>
          </div>

          <!-- 已选择提示 -->
          <div v-if="!channelLoading && !channelError && channels.length > 0" class="mt-4 text-sm text-gray-600 bg-gray-50 rounded p-3">
            已选择 <span class="font-medium text-emerald-600">{{ selectedChannels.length }}</span> 个渠道
            <span v-if="selectedChannels.length === 0" class="text-orange-600">
              (未选择表示对所有渠道开放)
            </span>
          </div>
        </div>

        <!-- 角色配置 Tab -->
        <div v-show="activeTab === 'role'" class="px-6">
          <!-- 说明信息 -->
          <div class="bg-purple-50 border border-purple-200 rounded-lg p-4 mb-4">
            <div class="flex items-start">
              <Info class="h-5 w-5 text-purple-500 mt-0.5 mr-3" />
              <div class="flex-1">
                <p class="text-sm text-purple-700">
                  为文档配置可访问的角色白名单,选中的角色用户可以搜索到此文档。未选择任何角色表示该文档对所有角色开放。
                </p>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="roleLoading" class="flex items-center justify-center py-8">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600"></div>
            <span class="ml-3 text-gray-600">加载中...</span>
          </div>

          <!-- 错误提示 -->
          <div v-else-if="roleError" class="bg-red-50 border border-red-200 rounded-lg p-4">
            <div class="flex items-start">
              <AlertCircle class="h-5 w-5 text-red-500 mt-0.5 mr-3" />
              <div class="flex-1">
                <p class="text-sm text-red-700">{{ roleError }}</p>
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
          <div v-if="!roleLoading && !roleError && roles.length > 0" class="mt-4 text-sm text-gray-600 bg-gray-50 rounded p-3">
            已选择 <span class="font-medium text-purple-600">{{ selectedRoles.length }}</span> 个角色
            <span v-if="selectedRoles.length === 0" class="text-orange-600">
              (未选择表示对所有角色开放)
            </span>
          </div>
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
          :disabled="isLoading || saving"
          :class="[
            'px-6 py-2 text-white rounded transition-colors disabled:opacity-50 flex items-center',
            activeTab === 'channel' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-purple-600 hover:bg-purple-700'
          ]"
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
import { ref, watch, computed } from 'vue'
import {
  Settings,
  Info,
  AlertCircle,
  X,
  Check
} from 'lucide-vue-next'
import { channelApi } from '@/services/channelApi'
import { roleApi } from '@/services/roleApi'
import { searchDataService } from '@/services/searchDataService'
import type { Channel } from '@/types/channel'
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

// Tab 状态
const activeTab = ref<'channel' | 'role'>('channel')

// 渠道相关状态
const channelLoading = ref(false)
const channelError = ref<string | null>(null)
const channels = ref<Channel[]>([])
const selectedChannels = ref<string[]>([])

// 角色相关状态
const roleLoading = ref(false)
const roleError = ref<string | null>(null)
const roles = ref<Role[]>([])
const selectedRoles = ref<string[]>([])

// 保存状态
const saving = ref(false)

// 计算是否正在加载
const isLoading = computed(() => channelLoading.value || roleLoading.value)

// 监听对话框打开,加载数据
watch(() => props.open, async (isOpen) => {
  if (isOpen && props.document) {
    await loadAllData()
  }
})

/**
 * 加载所有数据
 */
const loadAllData = async () => {
  await Promise.all([
    loadChannelData(),
    loadRoleData()
  ])
}

/**
 * 加载渠道列表和当前文档的渠道配置
 */
const loadChannelData = async () => {
  channelLoading.value = true
  channelError.value = null

  try {
    // 加载所有可用渠道
    const response = await channelApi.list({ page: 0, size: 1000 })
    channels.value = response.data.content

    // 加载当前文档的渠道白名单
    if (props.document?._source?.request_channel_white_list) {
      selectedChannels.value = [...props.document._source.request_channel_white_list]
    } else {
      selectedChannels.value = []
    }
  } catch (err: any) {
    channelError.value = err.message || '加载渠道列表失败'
    console.error('加载渠道数据失败:', err)
  } finally {
    channelLoading.value = false
  }
}

/**
 * 加载角色列表和当前文档的角色配置
 */
const loadRoleData = async () => {
  roleLoading.value = true
  roleError.value = null

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
    roleError.value = err.message || '加载角色列表失败'
    console.error('加载角色数据失败:', err)
  } finally {
    roleLoading.value = false
  }
}

/**
 * 保存配置
 */
const handleSave = async () => {
  if (!props.document) return

  saving.value = true

  try {
    if (activeTab.value === 'channel') {
      // 保存渠道配置
      channelError.value = null
      await searchDataService.updateDocumentChannels(
        props.document._id,
        props.document._index,
        selectedChannels.value
      )
    } else {
      // 保存角色配置
      roleError.value = null
      await searchDataService.updateDocumentRoles(
        props.document._id,
        props.document._index,
        selectedRoles.value
      )
    }

    // 关闭对话框并通知父组件刷新
    emit('update:open', false)
    emit('success')
  } catch (err: any) {
    const errorMessage = err.message || '保存失败,请重试'
    if (activeTab.value === 'channel') {
      channelError.value = errorMessage
    } else {
      roleError.value = errorMessage
    }
    console.error('保存配置失败:', err)
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
    // 重置状态
    activeTab.value = 'channel'
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
