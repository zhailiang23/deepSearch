<template>
  <div class="min-h-screen bg-background">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      
      <!-- 主内容 -->
      <ChannelList
        @create="handleCreate"
        @edit="handleEdit"
        @view="handleView"
        @delete="handleDelete"
      />

      <!-- 创建/编辑对话框 -->
      <div
        v-if="showFormDialog"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
        @click.self="closeFormDialog"
      >
        <div class="bg-card rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          <div class="px-6 py-4 border-b border-border">
            <h3 class="text-lg font-medium text-card-foreground">
              {{ currentChannel ? '编辑渠道' : '创建渠道' }}
            </h3>
          </div>
          <div class="px-6 py-4">
            <ChannelForm
              :channel="currentChannel"
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
        <div class="bg-card rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          <div class="px-6 py-4 border-b border-border flex justify-between items-center">
            <h3 class="text-lg font-medium text-card-foreground">渠道详情</h3>
            <button
              @click="closeDetailDialog"
              class="text-muted-foreground hover:text-foreground"
            >
              <X class="h-6 w-6" />
            </button>
          </div>
          <div v-if="currentChannel" class="px-6 py-4 space-y-6">
            <!-- 基本信息 -->
            <div>
              <h4 class="text-sm font-medium text-card-foreground mb-3">基本信息</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">渠道ID</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.id }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">渠道名称</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.name }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">渠道代码</dt>
                  <dd class="mt-1 text-sm text-card-foreground font-mono">{{ currentChannel.code }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">创建时间</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ formatDate(currentChannel.createdAt) }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">更新时间</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ formatDate(currentChannel.updatedAt) }}</dd>
                </div>
                <div v-if="currentChannel.createdBy">
                  <dt class="text-sm font-medium text-muted-foreground">创建者</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.createdBy }}</dd>
                </div>
                <div v-if="currentChannel.updatedBy">
                  <dt class="text-sm font-medium text-muted-foreground">更新者</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.updatedBy }}</dd>
                </div>
              </div>
            </div>

            <!-- 描述 -->
            <div v-if="currentChannel.description" class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="md:col-span-2">
                <dt class="text-sm font-medium text-muted-foreground">描述</dt>
                <dd class="mt-1 text-sm text-card-foreground whitespace-pre-wrap break-words">{{ currentChannel.description }}</dd>
              </div>
            </div>
          </div>
          <div class="px-6 py-4 border-t border-border flex justify-end">
            <button
              @click="closeDetailDialog"
              class="px-4 py-2 text-sm font-medium text-foreground bg-background border border-border rounded-md hover:bg-accent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring"
            >
              关闭
            </button>
          </div>
        </div>
      </div>

      <!-- 删除确认对话框 -->
      <div
        v-if="showDeleteDialog"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
        @click.self="closeDeleteDialog"
      >
        <div class="bg-card rounded-lg shadow-xl max-w-md w-full">
          <div class="px-6 py-4">
            <div class="flex items-center">
              <div class="flex-shrink-0">
                <AlertTriangle class="h-6 w-6 text-destructive" />
              </div>
              <div class="ml-3">
                <h3 class="text-lg font-medium text-card-foreground">确认删除</h3>
                <div class="mt-2">
                  <p class="text-sm text-muted-foreground">
                    您确定要删除渠道 "<span class="font-medium">{{ deleteTarget?.name }}</span>" 吗？
                    此操作不可撤销。
                  </p>
                </div>
              </div>
            </div>
          </div>
          <div class="px-6 py-4 border-t border-border flex justify-end space-x-3">
            <button
              @click="closeDeleteDialog"
              type="button"
              class="px-4 py-2 text-sm font-medium text-foreground bg-background border border-border rounded-md hover:bg-accent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring"
            >
              取消
            </button>
            <button
              @click="confirmDelete"
              :disabled="deleteLoading"
              type="button"
              class="px-4 py-2 text-sm font-medium text-destructive-foreground bg-destructive border border-transparent rounded-md hover:bg-destructive/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-destructive disabled:opacity-50 disabled:cursor-not-allowed"
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
          ? 'bg-primary/10 text-primary border border-primary/20'
          : 'bg-destructive/10 text-destructive border border-destructive/20'
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
import { useChannelStore } from '@/stores/channel'
import ChannelList from '@/components/channel/ChannelList.vue'
import ChannelForm from '@/components/channel/ChannelForm.vue'
import type { Channel, CreateChannelRequest, UpdateChannelRequest } from '@/types/channel'

const channelStore = useChannelStore()

// 对话框状态
const showFormDialog = ref(false)
const showDetailDialog = ref(false)
const showDeleteDialog = ref(false)

// 当前操作的渠道
const currentChannel = ref<Channel | null>(null)
const deleteTarget = ref<Channel | null>(null)

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

// 工具函数
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

// 创建渠道
const handleCreate = () => {
  currentChannel.value = null
  showFormDialog.value = true
}

// 编辑渠道
const handleEdit = (channel: Channel) => {
  currentChannel.value = channel
  showFormDialog.value = true
}

// 查看渠道详情
const handleView = async (channel: Channel) => {
  try {
    // 获取最新的详细信息
    const latestChannel = await channelStore.fetchChannelById(channel.id)
    currentChannel.value = latestChannel
    showDetailDialog.value = true
  } catch (error) {
    showMessage('error', '获取渠道详情失败')
  }
}

// 删除渠道
const handleDelete = (channel: Channel) => {
  deleteTarget.value = channel
  showDeleteDialog.value = true
}

// 表单提交
const handleFormSubmit = async (data: CreateChannelRequest | UpdateChannelRequest) => {
  try {
    formLoading.value = true

    if (currentChannel.value) {
      // 更新
      const updateData = data as UpdateChannelRequest
      await channelStore.updateChannel(currentChannel.value.id, updateData)
      showMessage('success', '渠道更新成功')
    } else {
      // 创建
      const createData = data as CreateChannelRequest
      await channelStore.createChannel(createData)
      showMessage('success', '渠道创建成功')
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
    await channelStore.deleteChannel(deleteTarget.value.id)
    showMessage('success', `渠道 "${deleteTarget.value.name}" 已删除`)
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
  currentChannel.value = null
  formLoading.value = false
}

const closeDetailDialog = () => {
  showDetailDialog.value = false
  currentChannel.value = null
}

const closeDeleteDialog = () => {
  showDeleteDialog.value = false
  deleteTarget.value = null
  deleteLoading.value = false
}

// 初始化
onMounted(() => {
  // 清理状态
  channelStore.reset()
})
</script>