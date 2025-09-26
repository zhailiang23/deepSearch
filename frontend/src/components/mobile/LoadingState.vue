<template>
  <div class="loading-state">
    <div class="loading-container">
      <!-- 加载动画 -->
      <div class="loading-animation">
        <div v-if="animationType === 'spinner'" class="spinner-animation">
          <div class="spinner"></div>
        </div>

        <div v-else-if="animationType === 'dots'" class="dots-animation">
          <div class="dot dot-1"></div>
          <div class="dot dot-2"></div>
          <div class="dot dot-3"></div>
        </div>

        <div v-else-if="animationType === 'pulse'" class="pulse-animation">
          <div class="pulse-circle pulse-1"></div>
          <div class="pulse-circle pulse-2"></div>
          <div class="pulse-circle pulse-3"></div>
        </div>

        <div v-else class="skeleton-animation">
          <div class="skeleton-item">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-content">
              <div class="skeleton-line skeleton-title"></div>
              <div class="skeleton-line skeleton-text"></div>
              <div class="skeleton-line skeleton-text short"></div>
            </div>
          </div>
          <div class="skeleton-item">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-content">
              <div class="skeleton-line skeleton-title"></div>
              <div class="skeleton-line skeleton-text"></div>
            </div>
          </div>
          <div class="skeleton-item">
            <div class="skeleton-avatar"></div>
            <div class="skeleton-content">
              <div class="skeleton-line skeleton-title"></div>
              <div class="skeleton-line skeleton-text"></div>
              <div class="skeleton-line skeleton-text short"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载文本 -->
      <div class="loading-text">
        <p class="loading-message">{{ message }}</p>
        <p v-if="showTips && currentTip" class="loading-tip">
          {{ currentTip }}
        </p>
      </div>

      <!-- 进度指示器 -->
      <div v-if="showProgress && progress !== null" class="progress-container">
        <div class="progress-bar">
          <div
            class="progress-fill"
            :style="{ width: `${progress}%` }"
          ></div>
        </div>
        <span class="progress-text">{{ progress }}%</span>
      </div>
    </div>

    <!-- 取消按钮 -->
    <button
      v-if="showCancelButton"
      class="cancel-button"
      @click="handleCancel"
    >
      {{ cancelText }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 组件属性定义
export interface LoadingStateProps {
  message?: string
  animationType?: 'spinner' | 'dots' | 'pulse' | 'skeleton'
  showProgress?: boolean
  progress?: number | null
  showTips?: boolean
  tips?: string[]
  showCancelButton?: boolean
  cancelText?: string
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<LoadingStateProps>(), {
  message: '正在搜索...',
  animationType: 'skeleton',
  showProgress: false,
  progress: null,
  showTips: true,
  tips: () => [
    '正在连接 Elasticsearch 服务器...',
    '正在分析搜索查询...',
    '正在检索相关结果...',
    '正在优化搜索结果...',
    '正在格式化数据...'
  ],
  showCancelButton: false,
  cancelText: '取消',
  size: 'medium'
})

// 组件事件定义
const emit = defineEmits<{
  cancel: []
}>()

// 响应式数据
const currentTip = ref('')
const tipIndex = ref(0)
let tipTimer: number | null = null

// 计算样式类
const containerClass = computed(() => {
  const sizeClasses = {
    small: 'loading-small',
    medium: 'loading-medium',
    large: 'loading-large'
  }
  return [sizeClasses[props.size]]
})

// 处理取消
const handleCancel = () => {
  emit('cancel')
}

// 开始提示轮播
const startTipRotation = () => {
  if (!props.showTips || props.tips.length === 0) return

  currentTip.value = props.tips[0]

  if (props.tips.length > 1) {
    tipTimer = setInterval(() => {
      tipIndex.value = (tipIndex.value + 1) % props.tips.length
      currentTip.value = props.tips[tipIndex.value]
    }, 2000)
  }
}

// 停止提示轮播
const stopTipRotation = () => {
  if (tipTimer) {
    clearInterval(tipTimer)
    tipTimer = null
  }
}

// 组件挂载
onMounted(() => {
  startTipRotation()
})

// 组件卸载
onUnmounted(() => {
  stopTipRotation()
})
</script>

<style scoped>
.loading-state {
  @apply flex flex-col items-center justify-center;
  @apply h-full min-h-64 p-8;
  @apply bg-gray-50;
}

.loading-container {
  @apply flex flex-col items-center;
  @apply max-w-sm w-full;
}

.loading-animation {
  @apply mb-6;
}

/* 旋转器动画 */
.spinner-animation {
  @apply flex justify-center;
}

.spinner {
  @apply w-8 h-8 border-4 border-emerald-500 border-t-transparent;
  @apply rounded-full animate-spin;
}

/* 点点动画 */
.dots-animation {
  @apply flex justify-center space-x-2;
}

.dot {
  @apply w-3 h-3 bg-emerald-500 rounded-full;
  animation: bounce 1.4s ease-in-out infinite both;
}

.dot-1 { animation-delay: -0.32s; }
.dot-2 { animation-delay: -0.16s; }
.dot-3 { animation-delay: 0s; }

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 脉冲动画 */
.pulse-animation {
  @apply relative flex justify-center items-center;
  @apply w-16 h-16;
}

.pulse-circle {
  @apply absolute w-4 h-4 bg-emerald-500 rounded-full;
  animation: pulse 1.5s ease-in-out infinite;
}

.pulse-1 { animation-delay: 0s; }
.pulse-2 { animation-delay: 0.5s; }
.pulse-3 { animation-delay: 1s; }

@keyframes pulse {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

/* 骨架屏动画 */
.skeleton-animation {
  @apply space-y-4 w-full max-w-xs;
}

.skeleton-item {
  @apply flex items-start space-x-3;
  @apply p-3 bg-white rounded-lg border border-gray-200;
}

.skeleton-avatar {
  @apply w-10 h-10 bg-gray-200 rounded-full;
  @apply flex-shrink-0;
  animation: skeleton-loading 1.5s ease-in-out infinite alternate;
}

.skeleton-content {
  @apply flex-1 space-y-2;
}

.skeleton-line {
  @apply h-3 bg-gray-200 rounded;
  animation: skeleton-loading 1.5s ease-in-out infinite alternate;
}

.skeleton-title {
  @apply w-3/4 h-4;
}

.skeleton-text {
  @apply w-full;
}

.skeleton-text.short {
  @apply w-2/3;
}

@keyframes skeleton-loading {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0.4;
  }
}

/* 加载文本 */
.loading-text {
  @apply text-center mb-6;
}

.loading-message {
  @apply text-lg font-medium text-gray-700 mb-2;
}

.loading-tip {
  @apply text-sm text-gray-500;
  @apply opacity-100 duration-300;
}

/* 进度条 */
.progress-container {
  @apply w-full flex items-center space-x-3 mb-6;
}

.progress-bar {
  @apply flex-1 h-2 bg-gray-200 rounded-full overflow-hidden;
}

.progress-fill {
  @apply h-full bg-emerald-500 rounded-full;
  @apply transition-all duration-300 ease-out;
}

.progress-text {
  @apply text-sm font-medium text-gray-600 w-12 text-right;
}

/* 取消按钮 */
.cancel-button {
  @apply px-6 py-2;
  @apply text-sm font-medium text-gray-600;
  @apply bg-white border border-gray-300 rounded-lg;
  @apply hover:bg-gray-50 hover:border-gray-400;
  @apply active:bg-gray-100;
  @apply transition-colors duration-150;
  @apply min-h-[44px]; /* iOS 触摸目标最小高度 */
}

/* 尺寸变体 */
.loading-small .spinner {
  @apply w-6 h-6;
}

.loading-small .loading-message {
  @apply text-base;
}

.loading-small .loading-animation {
  @apply mb-4;
}

.loading-large .spinner {
  @apply w-12 h-12;
}

.loading-large .loading-message {
  @apply text-xl;
}

.loading-large .loading-animation {
  @apply mb-8;
}

/* 移动端优化 */
@media (max-width: 640px) {
  .loading-state {
    @apply p-6;
  }

  .loading-container {
    @apply max-w-none;
  }

  .skeleton-animation {
    @apply max-w-full;
  }

  .skeleton-item {
    @apply p-2;
  }

  .skeleton-avatar {
    @apply w-8 h-8;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .loading-state {
    @apply bg-gray-900;
  }

  .loading-message {
    @apply text-gray-200;
  }

  .loading-tip {
    @apply text-gray-400;
  }

  .skeleton-item {
    @apply bg-gray-800 border-gray-700;
  }

  .skeleton-avatar,
  .skeleton-line {
    @apply bg-gray-700;
  }

  .progress-bar {
    @apply bg-gray-700;
  }

  .cancel-button {
    @apply bg-gray-800 border-gray-600 text-gray-300;
    @apply hover:bg-gray-700 hover:border-gray-500;
  }
}

/* 减少动画模式适配 */
@media (prefers-reduced-motion: reduce) {
  .spinner,
  .dot,
  .pulse-circle,
  .skeleton-avatar,
  .skeleton-line,
  .loading-tip {
    @apply animate-none;
  }

  .progress-fill {
    @apply transition-none;
  }
}

/* 高对比度模式适配 */
@media (prefers-contrast: high) {
  .loading-message {
    @apply text-black;
  }

  .loading-tip {
    @apply text-gray-700;
  }

  .spinner {
    @apply border-black border-t-transparent;
  }

  .dot,
  .pulse-circle {
    @apply bg-black;
  }

  .progress-fill {
    @apply bg-black;
  }
}
</style>