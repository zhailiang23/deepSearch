<template>
  <div class="error-state">
    <div class="error-container" :class="containerClass">
      <!-- 错误图标 -->
      <div class="error-icon">
        <div v-if="errorType === 'network'" class="icon-wrapper network-error">
          <svg class="w-16 h-16 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>

        <div v-else-if="errorType === 'server'" class="icon-wrapper server-error">
          <svg class="w-16 h-16 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
          </svg>
          <div class="error-overlay">
            <svg class="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.464 0L4.35 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          </div>
        </div>

        <div v-else-if="errorType === 'timeout'" class="icon-wrapper timeout-error">
          <svg class="w-16 h-16 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div class="error-overlay">
            <svg class="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
        </div>

        <div v-else-if="errorType === 'permission'" class="icon-wrapper permission-error">
          <svg class="w-16 h-16 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
          <div class="error-overlay">
            <svg class="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
        </div>

        <div v-else class="icon-wrapper general-error">
          <svg class="w-16 h-16 text-red-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.464 0L4.35 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
        </div>
      </div>

      <!-- 错误内容 -->
      <div class="error-content">
        <h3 class="error-title">
          {{ title || getDefaultTitle() }}
        </h3>

        <p class="error-description">
          {{ description || getDefaultDescription() }}
        </p>

        <!-- 错误详情 -->
        <div v-if="showDetails && message" class="error-details">
          <details class="details-disclosure">
            <summary class="details-summary">
              查看详细错误信息
              <svg class="w-4 h-4 ml-1 transform transition-transform duration-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </summary>
            <div class="details-content">
              <pre class="error-message">{{ message }}</pre>
            </div>
          </details>
        </div>

        <!-- 错误码显示 -->
        <div v-if="errorCode" class="error-code">
          <span class="code-label">错误码：</span>
          <span class="code-value">{{ errorCode }}</span>
        </div>
      </div>

      <!-- 错误操作 -->
      <div class="error-actions">
        <!-- 主要操作按钮 -->
        <div class="primary-actions">
          <button
            class="action-button primary"
            @click="handleRetry"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            重试
          </button>

          <button
            v-if="showReloadButton"
            class="action-button secondary"
            @click="handleReload"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            刷新页面
          </button>
        </div>

        <!-- 次要操作按钮 -->
        <div class="secondary-actions">
          <button
            v-if="showReportButton"
            class="action-button ghost"
            @click="handleReport"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.464 0L4.35 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            报告问题
          </button>

          <button
            v-if="showBackButton"
            class="action-button ghost"
            @click="handleGoBack"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            返回
          </button>

          <slot name="actions" />
        </div>
      </div>

      <!-- 帮助信息 -->
      <div v-if="showHelp" class="help-section">
        <div class="help-title">
          <svg class="w-4 h-4 text-blue-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>可能的解决方案：</span>
        </div>
        <div class="help-content">
          <ul class="help-list">
            <li v-for="(tip, index) in getHelpTips()" :key="index" class="help-item">
              {{ tip }}
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

// 组件属性定义
export interface ErrorStateProps {
  errorType?: 'network' | 'server' | 'timeout' | 'permission' | 'general'
  title?: string
  description?: string
  message?: string
  errorCode?: string | number
  showDetails?: boolean
  showReloadButton?: boolean
  showReportButton?: boolean
  showBackButton?: boolean
  showHelp?: boolean
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<ErrorStateProps>(), {
  errorType: 'general',
  title: '',
  description: '',
  message: '',
  errorCode: '',
  showDetails: true,
  showReloadButton: false,
  showReportButton: true,
  showBackButton: false,
  showHelp: true,
  size: 'medium'
})

// 组件事件定义
const emit = defineEmits<{
  retry: []
  reload: []
  report: [error: { type: string; message: string; code?: string | number }]
  back: []
}>()

// 获取默认标题
const getDefaultTitle = () => {
  switch (props.errorType) {
    case 'network':
      return '网络连接失败'
    case 'server':
      return '服务器错误'
    case 'timeout':
      return '请求超时'
    case 'permission':
      return '权限不足'
    default:
      return '出现错误'
  }
}

// 获取默认描述
const getDefaultDescription = () => {
  switch (props.errorType) {
    case 'network':
      return '无法连接到服务器，请检查网络连接。'
    case 'server':
      return '服务器暂时无法处理您的请求。'
    case 'timeout':
      return '请求处理时间过长，连接已超时。'
    case 'permission':
      return '您没有执行此操作的权限。'
    default:
      return '发生了未知错误，请稍后重试。'
  }
}

// 获取帮助提示
const getHelpTips = () => {
  switch (props.errorType) {
    case 'network':
      return [
        '检查网络连接是否正常',
        '尝试切换到其他网络',
        '关闭防火墙或代理设置'
      ]
    case 'server':
      return [
        '等待几分钟后重试',
        '清除浏览器缓存',
        '联系系统管理员'
      ]
    case 'timeout':
      return [
        '检查网络连接速度',
        '尝试减少请求数据量',
        '稍后重试操作'
      ]
    case 'permission':
      return [
        '联系管理员获取权限',
        '确认登录状态',
        '尝试重新登录'
      ]
    default:
      return [
        '刷新页面重试',
        '清除浏览器缓存',
        '联系技术支持'
      ]
  }
}

// 计算样式类
const containerClass = computed(() => {
  const sizeClasses = {
    small: 'error-small',
    medium: 'error-medium',
    large: 'error-large'
  }
  return [sizeClasses[props.size]]
})

// 处理重试
const handleRetry = () => {
  emit('retry')
}

// 处理重载
const handleReload = () => {
  emit('reload')
}

// 处理报告
const handleReport = () => {
  const errorInfo = {
    type: props.errorType,
    message: props.message,
    code: props.errorCode
  }
  emit('report', errorInfo)
}

// 处理返回
const handleGoBack = () => {
  emit('back')
}
</script>

<style scoped>
.error-state {
  @apply flex items-center justify-center;
  @apply h-full min-h-96 p-6;
  @apply bg-red-50;
}

.error-container {
  @apply flex flex-col items-center;
  @apply max-w-md w-full text-center;
}

/* 图标区域 */
.error-icon {
  @apply relative mb-6;
}

.icon-wrapper {
  @apply flex items-center justify-center;
}

.error-overlay {
  @apply absolute -bottom-1 -right-1;
  @apply bg-white rounded-full p-1;
  @apply border-2 border-red-100;
}

/* 内容区域 */
.error-content {
  @apply mb-6 w-full;
}

.error-title {
  @apply text-xl font-semibold text-red-700 mb-2;
}

.error-description {
  @apply text-base text-red-600 leading-relaxed mb-4;
}

/* 错误详情 */
.error-details {
  @apply mb-4;
}

.details-disclosure {
  @apply text-left;
}

.details-summary {
  @apply flex items-center justify-center cursor-pointer;
  @apply text-sm font-medium text-gray-600;
  @apply hover:text-gray-800;
  @apply transition-colors duration-150;
}

.details-summary::-webkit-details-marker {
  @apply hidden;
}

.details-disclosure[open] .details-summary svg {
  transform: rotate(180deg);
}

.details-content {
  @apply mt-3 p-3;
  @apply bg-gray-100 rounded-lg;
  @apply border border-gray-200;
}

.error-message {
  @apply text-xs text-gray-700;
  @apply font-mono whitespace-pre-wrap;
  @apply max-h-32 overflow-y-auto;
}

/* 错误码 */
.error-code {
  @apply text-sm text-gray-600 mb-4;
}

.code-label {
  @apply font-medium;
}

.code-value {
  @apply font-mono bg-gray-100 px-2 py-1 rounded ml-2;
}

/* 操作区域 */
.error-actions {
  @apply w-full space-y-4;
}

.primary-actions {
  @apply flex flex-col sm:flex-row gap-3 justify-center;
}

.secondary-actions {
  @apply flex flex-col sm:flex-row gap-2 justify-center;
}

.action-button {
  @apply flex items-center justify-center px-6 py-3;
  @apply font-medium rounded-lg;
  @apply transition-colors duration-150;
  @apply min-h-[44px]; /* iOS 触摸目标最小高度 */
}

.action-button.primary {
  @apply bg-red-500 text-white;
  @apply hover:bg-red-600;
  @apply active:bg-red-700;
}

.action-button.secondary {
  @apply bg-white text-red-700 border border-red-300;
  @apply hover:bg-red-50 hover:border-red-400;
  @apply active:bg-red-100;
}

.action-button.ghost {
  @apply text-gray-600;
  @apply hover:text-gray-800 hover:bg-gray-100;
  @apply active:bg-gray-200;
}

/* 帮助区域 */
.help-section {
  @apply w-full mt-6 pt-6 border-t border-red-200;
}

.help-title {
  @apply flex items-center justify-center text-sm font-medium text-blue-700 mb-3;
}

.help-content {
  @apply text-left;
}

.help-list {
  @apply list-disc list-inside space-y-1;
}

.help-item {
  @apply text-sm text-gray-600;
}

/* 尺寸变体 */
.error-small .error-icon svg {
  @apply w-12 h-12;
}

.error-small .error-title {
  @apply text-lg;
}

.error-small .error-description {
  @apply text-sm;
}

.error-large .error-icon svg {
  @apply w-20 h-20;
}

.error-large .error-title {
  @apply text-2xl;
}

.error-large .error-description {
  @apply text-lg;
}

/* 移动端优化 */
@media (max-width: 640px) {
  .error-state {
    @apply p-4;
  }

  .error-container {
    @apply max-w-none;
  }

  .primary-actions,
  .secondary-actions {
    @apply flex-col;
  }

  .action-button {
    @apply w-full;
  }

  .error-message {
    @apply text-xs;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .error-state {
    @apply bg-red-900;
  }

  .error-title {
    @apply text-red-300;
  }

  .error-description {
    @apply text-red-400;
  }

  .error-code {
    @apply text-gray-400;
  }

  .code-value {
    @apply bg-gray-800;
  }

  .details-summary {
    @apply text-gray-400 hover:text-gray-200;
  }

  .details-content {
    @apply bg-gray-800 border-gray-700;
  }

  .error-message {
    @apply text-gray-300;
  }

  .action-button.primary {
    @apply bg-red-600 hover:bg-red-700;
  }

  .action-button.secondary {
    @apply bg-gray-800 text-red-400 border-red-700;
    @apply hover:bg-gray-700 hover:border-red-600;
  }

  .action-button.ghost {
    @apply text-gray-400 hover:text-gray-200 hover:bg-gray-800;
  }

  .help-section {
    @apply border-red-800;
  }

  .help-title {
    @apply text-blue-400;
  }

  .help-item {
    @apply text-gray-400;
  }

  .error-overlay {
    @apply bg-gray-800 border-red-800;
  }
}

/* 高对比度模式适配 */
@media (prefers-contrast: high) {
  .error-title {
    @apply text-red-900;
  }

  .error-description {
    @apply text-red-800;
  }

  .action-button.primary {
    @apply bg-red-700 hover:bg-red-800;
  }
}

/* 动画效果 */
.error-container {
  @apply opacity-100 duration-500;
}

.action-button:active {
  transform: scale(0.98);
}

/* 减少动画模式适配 */
@media (prefers-reduced-motion: reduce) {
  .error-container {
    @apply animate-none;
  }

  .action-button:active {
    transform: none;
  }

  .details-summary svg {
    @apply transition-none;
  }
}

/* 滚动条样式 */
.error-message::-webkit-scrollbar {
  @apply w-1;
}

.error-message::-webkit-scrollbar-track {
  @apply bg-gray-200 rounded-full;
}

.error-message::-webkit-scrollbar-thumb {
  @apply bg-gray-400 rounded-full hover:bg-gray-500;
}
</style>