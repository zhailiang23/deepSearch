<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="modal-overlay"
      @click="handleOverlayClick"
    >
      <div
        class="modal-content"
        @click.stop
      >
        <!-- 模态框头部 -->
        <div class="modal-header">
          <div class="header-content">
            <h2 class="modal-title">
              搜索日志详情
              <span v-if="logDetail" class="log-id">#{{ logDetail.id }}</span>
            </h2>
            <p class="modal-description">查看完整的搜索日志信息和点击记录</p>
          </div>
          <button
            @click="handleClose"
            class="close-btn"
            aria-label="关闭"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- 模态框内容 -->
        <div class="modal-body">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>

          <div v-else-if="logDetail" class="detail-content">
            <!-- 基本信息 -->
            <section class="detail-section">
              <h3 class="section-title">基本信息</h3>
              <div class="info-grid">
                <div class="info-item">
                  <label>搜索ID:</label>
                  <span class="font-mono">{{ logDetail.id }}</span>
                </div>
                <div class="info-item">
                  <label>用户ID:</label>
                  <span>{{ logDetail.userId }}</span>
                </div>
                <div class="info-item">
                  <label>用户IP:</label>
                  <span class="font-mono">{{ logDetail.userIp }}</span>
                </div>
                <div class="info-item">
                  <label>搜索空间:</label>
                  <span>{{ logDetail.searchSpaceId }}</span>
                </div>
                <div class="info-item">
                  <label>查询关键词:</label>
                  <span class="font-semibold text-green-800">{{ logDetail.query }}</span>
                </div>
                <div class="info-item">
                  <label>状态:</label>
                  <StatusBadge :status="logDetail.status" />
                </div>
                <div class="info-item">
                  <label>结果数量:</label>
                  <span class="font-semibold">{{ logDetail.resultCount }}</span>
                </div>
                <div class="info-item">
                  <label>响应时间:</label>
                  <span
                    :class="getResponseTimeClass(logDetail.responseTime)"
                    class="font-mono font-semibold"
                  >
                    {{ logDetail.responseTime }}ms
                  </span>
                </div>
                <div class="info-item">
                  <label>创建时间:</label>
                  <span>{{ formatDateTime(logDetail.createdAt) }}</span>
                </div>
                <div class="info-item">
                  <label>点击次数:</label>
                  <span class="font-semibold">{{ logDetail.clickCount }}</span>
                </div>
              </div>
            </section>

            <!-- 请求参数 -->
            <section class="detail-section">
              <h3 class="section-title">请求参数</h3>
              <CodeBlock
                :code="logDetail.requestParams"
                language="json"
                :collapsible="true"
                :max-height="'200px'"
                title="Search Request Parameters"
              />
            </section>

            <!-- 响应数据 -->
            <section v-if="logDetail.responseData" class="detail-section">
              <h3 class="section-title">响应数据</h3>
              <CodeBlock
                :code="logDetail.responseData"
                language="json"
                :collapsible="true"
                :max-height="'300px'"
                title="Search Response Data"
              />
            </section>

            <!-- 错误信息 -->
            <section v-if="logDetail.errorMessage" class="detail-section">
              <h3 class="section-title">错误信息</h3>
              <div class="error-container">
                <div class="error-icon">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div class="error-content">
                  <h4 class="error-title">执行错误</h4>
                  <p class="error-message">{{ logDetail.errorMessage }}</p>
                </div>
              </div>
            </section>

            <!-- 点击记录 -->
            <section v-if="logDetail.clickLogs?.length > 0" class="detail-section">
              <h3 class="section-title">
                点击记录
                <span class="click-count-badge">{{ logDetail.clickLogs.length }} 次点击</span>
              </h3>
              <div class="click-logs-container">
                <div
                  v-for="(click, index) in logDetail.clickLogs"
                  :key="click.id"
                  class="click-item"
                >
                  <div class="click-header">
                    <div class="click-info">
                      <span class="click-index">#{{ index + 1 }}</span>
                      <span class="click-position">位置: {{ click.clickPosition }}</span>
                      <span class="click-time">{{ formatDateTime(click.clickTime) }}</span>
                    </div>
                    <span class="click-type-badge">{{ click.clickType || 'click' }}</span>
                  </div>
                  <div class="click-document">
                    <h5 class="document-title">{{ click.documentTitle }}</h5>
                    <a
                      :href="click.documentUrl"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="document-url"
                    >
                      {{ click.documentUrl }}
                      <svg class="w-3 h-3 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                      </svg>
                    </a>
                  </div>
                  <div v-if="click.userAgent" class="click-meta">
                    <span class="user-agent">{{ click.userAgent }}</span>
                  </div>
                </div>
              </div>
            </section>

            <!-- 空状态 - 无点击记录 -->
            <section v-else-if="logDetail.clickCount === 0" class="detail-section">
              <h3 class="section-title">点击记录</h3>
              <div class="empty-clicks">
                <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122" />
                </svg>
                <p class="empty-text">该搜索暂无点击记录</p>
              </div>
            </section>
          </div>

          <div v-else class="error-container">
            <div class="error-icon">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <span>加载失败，请重试</span>
          </div>
        </div>

        <!-- 模态框底部 -->
        <div class="modal-footer">
          <Button @click="handleClose" variant="outline">
            关闭
          </Button>
          <Button v-if="logDetail" @click="exportDetail" variant="default">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            导出详情
          </Button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Teleport } from 'vue'
import { formatDateTime } from '@/utils/date'
import type { SearchLogDetail } from '@/types/searchLog'

// 组件导入
import { Button } from '@/components/ui/button'
import CodeBlock from '@/components/ui/CodeBlock.vue'

// 状态徽章组件（内联定义）
const StatusBadge = ({ status }: { status: 'SUCCESS' | 'ERROR' }) => {
  const config = {
    SUCCESS: { text: '成功', class: 'bg-green-100 text-green-800 border-green-200' },
    ERROR: { text: '失败', class: 'bg-red-100 text-red-800 border-red-200' }
  }[status]

  return `<span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border ${config.class}">${config.text}</span>`
}

interface Props {
  visible: boolean
  logId: number | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'close'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const loading = ref(false)
const logDetail = ref<SearchLogDetail | null>(null)

// 计算属性
const modalClasses = computed(() => ({
  'modal-open': props.visible
}))

// 方法
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

const handleOverlayClick = () => {
  handleClose()
}

const getResponseTimeClass = (responseTime: number) => {
  if (responseTime < 500) return 'text-green-600'
  if (responseTime < 1000) return 'text-yellow-600'
  return 'text-red-600'
}

const exportDetail = () => {
  if (!logDetail.value) return

  const exportData = {
    基本信息: {
      搜索ID: logDetail.value.id,
      用户ID: logDetail.value.userId,
      用户IP: logDetail.value.userIp,
      搜索空间: logDetail.value.searchSpaceId,
      查询关键词: logDetail.value.query,
      状态: logDetail.value.status,
      结果数量: logDetail.value.resultCount,
      响应时间: `${logDetail.value.responseTime}ms`,
      点击次数: logDetail.value.clickCount,
      创建时间: formatDateTime(logDetail.value.createdAt)
    },
    请求参数: JSON.parse(logDetail.value.requestParams || '{}'),
    响应数据: logDetail.value.responseData ? JSON.parse(logDetail.value.responseData) : null,
    错误信息: logDetail.value.errorMessage,
    点击记录: logDetail.value.clickLogs
  }

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr)

  const exportFileDefaultName = `search-log-${logDetail.value.id}-${new Date().toISOString().split('T')[0]}.json`

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportFileDefaultName)
  linkElement.click()
}

// 监听弹窗显示状态
watch(
  () => props.visible,
  (visible) => {
    if (visible && props.logId) {
      loadLogDetail()
    } else if (!visible) {
      // 清理数据
      logDetail.value = null
    }
  }
)

// 监听日志ID变化
watch(
  () => props.logId,
  (logId) => {
    if (logId && props.visible) {
      loadLogDetail()
    }
  }
)

// 模拟API调用加载日志详情
const loadLogDetail = async () => {
  if (!props.logId) return

  try {
    loading.value = true
    logDetail.value = null

    // 模拟API延迟
    await new Promise(resolve => setTimeout(resolve, 800))

    // 模拟数据，实际应该调用 searchLogApi.getSearchLogDetail(props.logId)
    const mockDetail: SearchLogDetail = {
      id: props.logId,
      userId: 'user123',
      userIp: '192.168.1.100',
      searchSpaceId: 'space1',
      query: 'Vue 3 Composition API',
      resultCount: 25,
      responseTime: 342,
      status: 'SUCCESS',
      createdAt: new Date().toISOString(),
      clickCount: 3,
      requestParams: JSON.stringify({
        query: 'Vue 3 Composition API',
        page: 0,
        size: 10,
        filters: {
          type: 'document',
          language: 'zh-CN'
        }
      }, null, 2),
      responseData: JSON.stringify({
        total: 25,
        results: [
          {
            id: 'doc1',
            title: 'Vue 3 Composition API 指南',
            url: 'https://example.com/vue3-guide',
            score: 0.95
          },
          {
            id: 'doc2',
            title: 'Vue 3 组件开发最佳实践',
            url: 'https://example.com/vue3-practices',
            score: 0.87
          }
        ]
      }, null, 2),
      errorMessage: undefined,
      clickLogs: [
        {
          id: 1,
          documentId: 'doc1',
          documentTitle: 'Vue 3 Composition API 指南',
          documentUrl: 'https://example.com/vue3-guide',
          clickPosition: 1,
          clickTime: new Date().toISOString(),
          userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
          clickType: 'click'
        },
        {
          id: 2,
          documentId: 'doc2',
          documentTitle: 'Vue 3 组件开发最佳实践',
          documentUrl: 'https://example.com/vue3-practices',
          clickPosition: 2,
          clickTime: new Date().toISOString(),
          userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
          clickType: 'click'
        },
        {
          id: 3,
          documentId: 'doc1',
          documentTitle: 'Vue 3 Composition API 指南',
          documentUrl: 'https://example.com/vue3-guide',
          clickPosition: 1,
          clickTime: new Date().toISOString(),
          userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15',
          clickType: 'click'
        }
      ]
    }

    logDetail.value = mockDetail
  } catch (error) {
    console.error('加载日志详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 防止页面滚动
watch(() => props.visible, (visible) => {
  if (visible) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})
</script>

<style scoped>
.modal-overlay {
  @apply fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  @apply bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-hidden flex flex-col;
  animation: slideIn 0.3s ease-out;
}

.modal-header {
  @apply flex items-start justify-between p-6 border-b border-gray-200 bg-gray-50;
}

.header-content {
  @apply flex-1;
}

.modal-title {
  @apply text-xl font-semibold text-gray-900 mb-1;
}

.log-id {
  @apply text-green-600 font-mono;
}

.modal-description {
  @apply text-sm text-gray-600;
}

.close-btn {
  @apply p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded transition-colors;
}

.modal-body {
  @apply flex-1 overflow-y-auto p-6;
}

.loading-container {
  @apply flex items-center justify-center py-12 space-x-3;
}

.loading-spinner {
  @apply w-6 h-6 border-2 border-green-500 border-t-transparent rounded-full animate-spin;
}

.detail-content {
  @apply space-y-6;
}

.detail-section {
  @apply bg-gray-50 rounded-lg p-5 border border-gray-200;
}

.section-title {
  @apply text-lg font-semibold text-gray-900 mb-4 flex items-center;
}

.click-count-badge {
  @apply ml-2 text-xs px-2 py-1 bg-green-100 text-green-700 rounded-full font-medium;
}

.info-grid {
  @apply grid grid-cols-1 md:grid-cols-2 gap-4;
}

.info-item {
  @apply flex flex-col space-y-1;
}

.info-item label {
  @apply text-sm font-medium text-gray-500;
}

.info-item span {
  @apply text-gray-900;
}

.error-container {
  @apply flex items-start space-x-3 p-4 bg-red-50 border border-red-200 rounded-lg;
}

.error-icon {
  @apply text-red-500 flex-shrink-0;
}

.error-content {
  @apply flex-1;
}

.error-title {
  @apply font-medium text-red-800 mb-1;
}

.error-message {
  @apply text-red-700 text-sm;
}

.click-logs-container {
  @apply space-y-4;
}

.click-item {
  @apply bg-white rounded-lg p-4 border border-gray-200 hover:border-green-300 transition-colors;
}

.click-header {
  @apply flex items-center justify-between mb-3;
}

.click-info {
  @apply flex items-center space-x-3 text-sm;
}

.click-index {
  @apply font-medium text-green-600;
}

.click-position {
  @apply text-gray-600;
}

.click-time {
  @apply text-gray-500 font-mono;
}

.click-type-badge {
  @apply px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded font-medium;
}

.click-document {
  @apply mb-2;
}

.document-title {
  @apply font-medium text-gray-900 mb-1;
}

.document-url {
  @apply text-sm text-blue-600 hover:text-blue-800 hover:underline inline-flex items-center transition-colors;
}

.click-meta {
  @apply pt-2 border-t border-gray-100;
}

.user-agent {
  @apply text-xs text-gray-500 font-mono;
}

.empty-clicks {
  @apply flex flex-col items-center justify-center py-8 text-gray-500;
}

.empty-icon {
  @apply w-12 h-12 mb-3;
}

.empty-text {
  @apply text-gray-600;
}

.modal-footer {
  @apply flex items-center justify-end space-x-3 p-6 border-t border-gray-200 bg-gray-50;
}

/* 动画效果 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-content {
    @apply mx-2 max-h-[95vh];
  }

  .info-grid {
    @apply grid-cols-1;
  }

  .click-header {
    @apply flex-col items-start space-y-2;
  }

  .click-info {
    @apply flex-wrap space-x-2;
  }

  .modal-footer {
    @apply flex-col-reverse space-x-0 space-y-reverse space-y-2;
  }
}

/* 滚动条样式 */
.modal-body::-webkit-scrollbar {
  @apply w-2;
}

.modal-body::-webkit-scrollbar-track {
  @apply bg-gray-100;
}

.modal-body::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}

/* 深层选择器样式 */
:deep(.status-badge) {
  @apply inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border;
}
</style>