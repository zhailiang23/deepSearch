<template>
  <div class="min-h-screen bg-background">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- 页面标题 -->
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-foreground">渠道管理</h1>
        <p class="mt-2 text-muted-foreground">
          管理您的销售渠道，包括创建、编辑、激活和停用操作
        </p>
      </div>

      <!-- 主内容 -->
      <ChannelList
        @create="handleCreate"
        @edit="handleEdit"
        @view="handleView"
        @delete="handleDelete"
        @activate="handleActivate"
        @deactivate="handleDeactivate"
        @batchUpdate="handleBatchUpdate"
      />

      <!-- 创建/编辑对话框 -->
      <div
        v-if="showFormDialog"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
        @click.self="closeFormDialog"
      >
        <div class="bg-card rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
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
        <div class="bg-card rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
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
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">渠道名称</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.name }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">渠道代码</dt>
                  <dd class="mt-1 text-sm text-card-foreground font-mono">{{ currentChannel.code }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">渠道类型</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ getChannelTypeLabel(currentChannel.type) }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">状态</dt>
                  <dd class="mt-1">
                    <ChannelStatusBadge :status="currentChannel.status" />
                  </dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">版本</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.version }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">排序顺序</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.sortOrder }}</dd>
                </div>
              </div>
            </div>

            <!-- 描述 -->
            <div v-if="currentChannel.description">
              <h4 class="text-sm font-medium text-card-foreground mb-3">描述</h4>
              <p class="text-sm text-muted-foreground whitespace-pre-wrap break-words">{{ currentChannel.description }}</p>
            </div>

            <!-- 联系信息 -->
            <div v-if="hasContactInfo(currentChannel)">
              <h4 class="text-sm font-medium text-card-foreground mb-3">联系信息</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div v-if="currentChannel.contactName">
                  <dt class="text-sm font-medium text-muted-foreground">联系人</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.contactName }}</dd>
                </div>
                <div v-if="currentChannel.contactPhone">
                  <dt class="text-sm font-medium text-muted-foreground">联系电话</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.contactPhone }}</dd>
                </div>
                <div v-if="currentChannel.contactEmail">
                  <dt class="text-sm font-medium text-muted-foreground">联系邮箱</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.contactEmail }}</dd>
                </div>
                <div v-if="currentChannel.address">
                  <dt class="text-sm font-medium text-muted-foreground">地址</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.address }}</dd>
                </div>
                <div v-if="currentChannel.website" class="md:col-span-2">
                  <dt class="text-sm font-medium text-muted-foreground">网站</dt>
                  <dd class="mt-1 text-sm text-card-foreground">
                    <a :href="currentChannel.website" target="_blank" rel="noopener noreferrer" class="text-primary hover:text-primary/80 underline">
                      {{ currentChannel.website }}
                    </a>
                  </dd>
                </div>
              </div>
            </div>

            <!-- 业务信息 -->
            <div>
              <h4 class="text-sm font-medium text-card-foreground mb-3">业务信息</h4>
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">佣金率</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ currentChannel.commissionRate }}%</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">月度目标</dt>
                  <dd class="mt-1 text-sm text-card-foreground">¥{{ formatCurrency(currentChannel.monthlyTarget) }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">当月销售</dt>
                  <dd class="mt-1 text-sm text-primary">¥{{ formatCurrency(currentChannel.currentMonthSales || 0) }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">总销售额</dt>
                  <dd class="mt-1 text-sm text-accent-foreground">¥{{ formatCurrency(currentChannel.totalSales || 0) }}</dd>
                </div>
              </div>

              <!-- 业绩指标 -->
              <div v-if="currentChannel.performanceRatio !== undefined" class="mt-4 p-4 bg-muted rounded-lg">
                <div class="flex items-center justify-between mb-2">
                  <span class="text-sm font-medium text-muted-foreground">目标完成率</span>
                  <span class="text-sm font-semibold" :class="getPerformanceTextColor(currentChannel.performanceRatio)">
                    {{ (currentChannel.performanceRatio * 100).toFixed(1) }}%
                  </span>
                </div>
                <div class="w-full bg-secondary rounded-full h-3">
                  <div
                    :class="getPerformanceBarColor(currentChannel.performanceRatio)"
                    :style="{ width: Math.min(currentChannel.performanceRatio * 100, 100) + '%' }"
                    class="h-3 rounded-full transition-all duration-300"
                  ></div>
                </div>
                <div class="mt-2 text-xs text-muted-foreground">
                  {{ currentChannel.targetAchieved ? '已达成目标' : '未达成目标' }}
                </div>
              </div>
            </div>

            <!-- 时间信息 -->
            <div>
              <h4 class="text-sm font-medium text-card-foreground mb-3">时间信息</h4>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">创建时间</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ formatDate(currentChannel.createdAt) }}</dd>
                </div>
                <div>
                  <dt class="text-sm font-medium text-muted-foreground">更新时间</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ formatDate(currentChannel.updatedAt) }}</dd>
                </div>
                <div v-if="currentChannel.lastActivityAt">
                  <dt class="text-sm font-medium text-muted-foreground">最后活动</dt>
                  <dd class="mt-1 text-sm text-card-foreground">{{ formatDate(currentChannel.lastActivityAt) }}</dd>
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
          <div class="px-6 py-4 bg-muted flex justify-end space-x-3">
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
import ChannelStatusBadge from '@/components/channel/ChannelStatusBadge.vue'
import type { Channel, CreateChannelRequest, UpdateChannelRequest } from '@/types/channel'
import { CHANNEL_TYPE_LABELS } from '@/types/channel'

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

const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  }).format(amount)
}

const getChannelTypeLabel = (type: string) => {
  return CHANNEL_TYPE_LABELS[type as keyof typeof CHANNEL_TYPE_LABELS] || type
}

const hasContactInfo = (channel: Channel) => {
  return channel.contactName || channel.contactPhone || channel.contactEmail ||
         channel.address || channel.website
}

const getPerformanceTextColor = (ratio: number) => {
  if (ratio >= 1) return 'text-green-600'
  if (ratio >= 0.8) return 'text-yellow-600'
  return 'text-red-600'
}

const getPerformanceBarColor = (ratio: number) => {
  if (ratio >= 1) return 'bg-green-500'
  if (ratio >= 0.8) return 'bg-yellow-500'
  return 'bg-red-500'
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

// 激活渠道
const handleActivate = async (channel: Channel) => {
  try {
    await channelStore.activateChannel(channel.id)
    showMessage('success', `渠道 "${channel.name}" 已激活`)
  } catch (error) {
    showMessage('error', '激活渠道失败')
  }
}

// 停用渠道
const handleDeactivate = async (channel: Channel) => {
  try {
    await channelStore.deactivateChannel(channel.id)
    showMessage('success', `渠道 "${channel.name}" 已停用`)
  } catch (error) {
    showMessage('error', '停用渠道失败')
  }
}

// 批量更新
const handleBatchUpdate = (channelIds: number[], action: 'activate' | 'deactivate') => {
  const actionText = action === 'activate' ? '激活' : '停用'
  showMessage('success', `批量${actionText} ${channelIds.length} 个渠道成功`)
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