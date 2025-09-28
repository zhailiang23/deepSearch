<template>
  <div class="statistics-page-layout">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-container">
        <div class="header-content">
          <!-- 标题区域 -->
          <div class="title-section">
            <div class="breadcrumb">
              <span class="breadcrumb-item">管理后台</span>
              <span class="breadcrumb-separator">/</span>
              <span class="breadcrumb-item breadcrumb-item--current">热词统计分析</span>
            </div>
            <h1 class="page-title">热词统计分析</h1>
            <p class="page-description">
              基于搜索日志的热词分析和可视化展示，提供实时数据洞察
            </p>
          </div>

          <!-- 操作区域 -->
          <div class="actions-section">
            <div class="action-buttons">
              <!-- 刷新按钮 -->
              <button
                @click="handleRefresh"
                :disabled="loading"
                class="action-button action-button--secondary"
                title="刷新数据"
              >
                <svg class="button-icon" :class="{ 'animate-spin': loading }" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
                </svg>
                <span class="button-text">刷新数据</span>
              </button>

              <!-- 导出按钮 -->
              <button
                @click="handleExport"
                :disabled="loading || !hasData"
                class="action-button action-button--primary"
                title="导出报告"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                <span class="button-text">导出报告</span>
              </button>

              <!-- 设置按钮 -->
              <button
                @click="toggleSettings"
                class="action-button action-button--ghost"
                title="页面设置"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                  <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                </svg>
              </button>
            </div>

            <!-- 状态指示器 -->
            <div class="status-indicators">
              <!-- 连接状态 -->
              <div class="status-indicator" :class="connectionStatus">
                <div class="status-dot"></div>
                <span class="status-text">{{ getConnectionText() }}</span>
              </div>

              <!-- 最后更新时间 -->
              <div class="last-update">
                <svg class="update-icon" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <span class="update-text">{{ lastUpdateText }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 全局错误提示 -->
    <div v-if="error" class="error-banner">
      <div class="error-container">
        <div class="error-content">
          <svg class="error-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
          </svg>
          <div class="error-text">
            <p class="error-message">{{ error }}</p>
            <p class="error-suggestion">请检查网络连接或稍后重试</p>
          </div>
        </div>
        <button @click="dismissError" class="error-dismiss">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 主内容区域 -->
    <main class="page-main">
      <div class="main-container">
        <!-- 筛选条件区域 -->
        <section v-if="$slots.filters" class="filters-section">
          <div class="section-wrapper">
            <slot name="filters" />
          </div>
        </section>

        <!-- 统计概览区域 -->
        <section v-if="$slots.overview" class="overview-section">
          <div class="section-wrapper">
            <slot name="overview" />
          </div>
        </section>

        <!-- 主内容区域 -->
        <section v-if="$slots['main-content']" class="main-content-section">
          <div class="section-wrapper">
            <slot name="main-content" />
          </div>
        </section>

        <!-- 数据表格区域 -->
        <section v-if="$slots['data-table']" class="data-table-section">
          <div class="section-wrapper">
            <slot name="data-table" />
          </div>
        </section>

        <!-- 自定义内容区域 -->
        <section v-if="$slots.default" class="custom-section">
          <div class="section-wrapper">
            <slot />
          </div>
        </section>
      </div>
    </main>

    <!-- 页面设置侧边栏 -->
    <Transition name="slide-over">
      <div v-if="showSettings" class="settings-overlay" @click="closeSettings">
        <div class="settings-panel" @click.stop>
          <div class="settings-header">
            <h3 class="settings-title">页面设置</h3>
            <button @click="closeSettings" class="settings-close">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <div class="settings-content">
            <!-- 布局设置 -->
            <div class="setting-group">
              <h4 class="setting-group-title">布局设置</h4>
              <div class="setting-options">
                <!-- 紧凑模式 */
                <label class="setting-option">
                  <input
                    v-model="layoutSettings.compact"
                    type="checkbox"
                    class="setting-checkbox"
                  />
                  <span class="setting-label">紧凑模式</span>
                  <span class="setting-description">减少元素间距，显示更多内容</span>
                </label>

                <!-- 固定侧边栏 -->
                <label class="setting-option">
                  <input
                    v-model="layoutSettings.fixedSidebar"
                    type="checkbox"
                    class="setting-checkbox"
                  />
                  <span class="setting-label">固定侧边栏</span>
                  <span class="setting-description">侧边栏保持可见状态</span>
                </label>

                <!-- 全屏模式 -->
                <label class="setting-option">
                  <input
                    v-model="layoutSettings.fullscreen"
                    type="checkbox"
                    class="setting-checkbox"
                  />
                  <span class="setting-label">全屏模式</span>
                  <span class="setting-description">隐藏页面头部和导航</span>
                </label>
              </div>
            </div>

            <!-- 主题设置 -->
            <div class="setting-group">
              <h4 class="setting-group-title">主题设置</h4>
              <div class="setting-options">
                <!-- 主题选择 -->
                <div class="setting-option">
                  <span class="setting-label">页面主题</span>
                  <select
                    v-model="layoutSettings.theme"
                    class="setting-select"
                  >
                    <option value="light">浅色主题</option>
                    <option value="dark">深色主题</option>
                    <option value="auto">跟随系统</option>
                  </select>
                </div>

                <!-- 强调色 -->
                <div class="setting-option">
                  <span class="setting-label">强调色</span>
                  <div class="color-palette">
                    <button
                      v-for="color in accentColors"
                      :key="color.name"
                      @click="layoutSettings.accentColor = color.value"
                      :class="[
                        'color-option',
                        { 'color-option--active': layoutSettings.accentColor === color.value }
                      ]"
                      :style="{ backgroundColor: color.value }"
                      :title="color.name"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- 显示设置 */
            <div class="setting-group">
              <h4 class="setting-group-title">显示设置</h4>
              <div class="setting-options">
                <!-- 动画效果 -->
                <label class="setting-option">
                  <input
                    v-model="layoutSettings.animations"
                    type="checkbox"
                    class="setting-checkbox"
                  />
                  <span class="setting-label">动画效果</span>
                  <span class="setting-description">启用页面过渡和动画</span>
                </label>

                <!-- 自动刷新 -->
                <label class="setting-option">
                  <input
                    v-model="layoutSettings.autoRefresh"
                    type="checkbox"
                    class="setting-checkbox"
                  />
                  <span class="setting-label">自动刷新</span>
                  <span class="setting-description">定期自动更新数据</span>
                </label>

                <!-- 刷新间隔 -->
                <div v-if="layoutSettings.autoRefresh" class="setting-option">
                  <span class="setting-label">刷新间隔</span>
                  <select
                    v-model="layoutSettings.refreshInterval"
                    class="setting-select"
                  >
                    <option :value="30">30秒</option>
                    <option :value="60">1分钟</option>
                    <option :value="300">5分钟</option>
                    <option :value="600">10分钟</option>
                  </select>
                </div>
              </div>
            </div>
          </div>

          <div class="settings-footer">
            <button @click="resetSettings" class="settings-button settings-button--secondary">
              重置设置
            </button>
            <button @click="saveSettings" class="settings-button settings-button--primary">
              保存设置
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 全局加载遮罩 -->
    <Transition name="fade">
      <div v-if="loading && showGlobalLoading" class="global-loading">
        <div class="loading-content">
          <div class="loading-spinner"></div>
          <p class="loading-text">{{ loadingText }}</p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'

// ============= 接口定义 =============

interface LayoutSettings {
  compact: boolean
  fixedSidebar: boolean
  fullscreen: boolean
  theme: 'light' | 'dark' | 'auto'
  accentColor: string
  animations: boolean
  autoRefresh: boolean
  refreshInterval: number
}

interface Props {
  loading?: boolean
  error?: string | null
  hasData?: boolean
  loadingText?: string
  showGlobalLoading?: boolean
  lastUpdate?: Date | null
}

interface Emits {
  (e: 'refresh'): void
  (e: 'export'): void
  (e: 'settings-change', settings: LayoutSettings): void
}

// ============= Props & Emits =============

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  error: null,
  hasData: true,
  loadingText: '加载中...',
  showGlobalLoading: false,
  lastUpdate: null
})

const emit = defineEmits<Emits>()

// ============= 响应式状态 =============

const showSettings = ref(false)
const connectionStatus = ref<'connected' | 'disconnected' | 'reconnecting'>('connected')
const autoRefreshTimer = ref<number | null>(null)

const layoutSettings = reactive<LayoutSettings>({
  compact: false,
  fixedSidebar: false,
  fullscreen: false,
  theme: 'light',
  accentColor: '#10b981',
  animations: true,
  autoRefresh: false,
  refreshInterval: 60
})

const accentColors = [
  { name: '翠绿色', value: '#10b981' },
  { name: '蓝色', value: '#3b82f6' },
  { name: '紫色', value: '#8b5cf6' },
  { name: '粉色', value: '#ec4899' },
  { name: '橙色', value: '#f97316' },
  { name: '红色', value: '#ef4444' },
  { name: '黄色', value: '#eab308' },
  { name: '青色', value: '#06b6d4' }
]

// ============= 计算属性 =============

const lastUpdateText = computed(() => {
  if (!props.lastUpdate) return '暂无数据'
  const now = new Date()
  const diff = now.getTime() - props.lastUpdate.getTime()

  if (diff < 60000) return '刚刚更新'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return props.lastUpdate.toLocaleString('zh-CN')
})

// ============= 方法 =============

const handleRefresh = () => {
  emit('refresh')
}

const handleExport = () => {
  emit('export')
}

const toggleSettings = () => {
  showSettings.value = !showSettings.value
}

const closeSettings = () => {
  showSettings.value = false
}

const dismissError = () => {
  // 发送事件给父组件处理错误清除
  emit('refresh')
}

const getConnectionText = (): string => {
  switch (connectionStatus.value) {
    case 'connected': return '已连接'
    case 'disconnected': return '连接断开'
    case 'reconnecting': return '重新连接中'
    default: return '未知状态'
  }
}

const resetSettings = () => {
  Object.assign(layoutSettings, {
    compact: false,
    fixedSidebar: false,
    fullscreen: false,
    theme: 'light',
    accentColor: '#10b981',
    animations: true,
    autoRefresh: false,
    refreshInterval: 60
  })
}

const saveSettings = () => {
  // 保存设置到本地存储
  localStorage.setItem('statistics-layout-settings', JSON.stringify(layoutSettings))
  emit('settings-change', { ...layoutSettings })
  showSettings.value = false
}

const loadSettings = () => {
  try {
    const saved = localStorage.getItem('statistics-layout-settings')
    if (saved) {
      Object.assign(layoutSettings, JSON.parse(saved))
    }
  } catch (error) {
    console.warn('Failed to load layout settings:', error)
  }
}

const setupAutoRefresh = () => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
  }

  if (layoutSettings.autoRefresh) {
    autoRefreshTimer.value = window.setInterval(() => {
      if (!props.loading && connectionStatus.value === 'connected') {
        emit('refresh')
      }
    }, layoutSettings.refreshInterval * 1000)
  }
}

const checkConnection = async () => {
  try {
    // 模拟连接检查
    await new Promise(resolve => setTimeout(resolve, 100))
    connectionStatus.value = 'connected'
  } catch {
    connectionStatus.value = 'disconnected'
  }
}

// ============= 监听器 =============

watch(
  () => layoutSettings.autoRefresh,
  () => setupAutoRefresh()
)

watch(
  () => layoutSettings.refreshInterval,
  () => setupAutoRefresh()
)

watch(
  layoutSettings,
  () => {
    emit('settings-change', { ...layoutSettings })
  },
  { deep: true }
)

// ============= 生命周期 =============

onMounted(() => {
  loadSettings()
  setupAutoRefresh()
  checkConnection()
})

onUnmounted(() => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
  }
})
</script>

<style scoped>
/* ============= 基础布局样式 ============= */
.statistics-page-layout {
  @apply min-h-screen bg-gray-50;
}

/* 页面头部样式 */
.page-header {
  @apply bg-white shadow-sm border-b border-gray-200 sticky top-0 z-40;
}

.header-container {
  @apply max-w-7xl mx-auto px-4 sm:px-6 lg:px-8;
}

.header-content {
  @apply py-6 flex items-center justify-between;
}

.title-section {
  @apply flex-1 min-w-0;
}

.breadcrumb {
  @apply flex items-center text-sm text-gray-500 mb-2;
}

.breadcrumb-item {
  @apply hover:text-gray-700 transition-colors;
}

.breadcrumb-item--current {
  @apply text-emerald-600 font-medium;
}

.breadcrumb-separator {
  @apply mx-2;
}

.page-title {
  @apply text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate;
}

.page-description {
  @apply mt-1 text-sm text-gray-500 max-w-2xl;
}

.actions-section {
  @apply flex items-center space-x-6 ml-6;
}

.action-buttons {
  @apply flex items-center space-x-3;
}

.action-button {
  @apply inline-flex items-center px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed;
}

.action-button--primary {
  @apply bg-emerald-600 text-white hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500;
}

.action-button--secondary {
  @apply bg-white text-gray-700 border border-gray-300 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500;
}

.action-button--ghost {
  @apply bg-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-100 p-2 rounded-md;
}

.button-icon {
  @apply w-4 h-4 mr-2;
}

.action-button--ghost .button-icon {
  @apply w-5 h-5 mr-0;
}

.button-text {
  @apply hidden sm:inline;
}

.status-indicators {
  @apply flex items-center space-x-4 text-sm;
}

.status-indicator {
  @apply flex items-center space-x-2;
}

.status-dot {
  @apply w-2 h-2 rounded-full;
}

.status-indicator.connected .status-dot {
  @apply bg-green-500;
}

.status-indicator.disconnected .status-dot {
  @apply bg-red-500;
}

.status-indicator.reconnecting .status-dot {
  @apply bg-yellow-500 animate-pulse;
}

.status-text {
  @apply text-gray-600;
}

.last-update {
  @apply flex items-center space-x-1 text-gray-500;
}

.update-icon {
  @apply w-4 h-4;
}

.update-text {
  @apply hidden sm:inline;
}

/* 错误横幅样式 */
.error-banner {
  @apply bg-red-50 border-l-4 border-red-400;
}

.error-container {
  @apply max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-start justify-between;
}

.error-content {
  @apply flex items-start space-x-3;
}

.error-icon {
  @apply w-5 h-5 text-red-400 mt-0.5 flex-shrink-0;
}

.error-text {
  @apply min-w-0 flex-1;
}

.error-message {
  @apply text-sm font-medium text-red-800;
}

.error-suggestion {
  @apply text-sm text-red-600 mt-1;
}

.error-dismiss {
  @apply p-1 text-red-400 hover:text-red-600 transition-colors;
}

.error-dismiss svg {
  @apply w-4 h-4;
}

/* 主内容区域样式 */
.page-main {
  @apply flex-1;
}

.main-container {
  @apply max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8;
}

.section-wrapper {
  @apply w-full;
}

/* 设置面板样式 */
.settings-overlay {
  @apply fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-end;
}

.settings-panel {
  @apply bg-white h-full w-80 shadow-xl flex flex-col;
}

.settings-header {
  @apply flex items-center justify-between p-6 border-b border-gray-200;
}

.settings-title {
  @apply text-lg font-semibold text-gray-900;
}

.settings-close {
  @apply p-2 text-gray-400 hover:text-gray-600 transition-colors;
}

.settings-close svg {
  @apply w-5 h-5;
}

.settings-content {
  @apply flex-1 overflow-y-auto p-6 space-y-6;
}

.setting-group {
  @apply space-y-4;
}

.setting-group-title {
  @apply text-sm font-semibold text-gray-900 uppercase tracking-wide;
}

.setting-options {
  @apply space-y-4;
}

.setting-option {
  @apply flex flex-col space-y-1 cursor-pointer;
}

.setting-checkbox {
  @apply w-4 h-4 text-emerald-600 border-gray-300 rounded focus:ring-emerald-500 mr-3;
}

.setting-label {
  @apply text-sm font-medium text-gray-700;
}

.setting-description {
  @apply text-xs text-gray-500;
}

.setting-select {
  @apply mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-emerald-500 focus:border-emerald-500 text-sm;
}

.color-palette {
  @apply flex space-x-2 mt-2;
}

.color-option {
  @apply w-6 h-6 rounded-full border-2 border-white shadow-sm hover:scale-110 transition-transform;
}

.color-option--active {
  @apply ring-2 ring-emerald-500 ring-offset-2;
}

.settings-footer {
  @apply p-6 border-t border-gray-200 flex space-x-3;
}

.settings-button {
  @apply flex-1 px-4 py-2 text-sm font-medium rounded-md transition-colors;
}

.settings-button--primary {
  @apply bg-emerald-600 text-white hover:bg-emerald-700;
}

.settings-button--secondary {
  @apply bg-white text-gray-700 border border-gray-300 hover:bg-gray-50;
}

/* 全局加载遮罩样式 */
.global-loading {
  @apply fixed inset-0 bg-white bg-opacity-90 flex items-center justify-center z-50;
}

.loading-content {
  @apply text-center;
}

.loading-spinner {
  @apply w-8 h-8 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto mb-4;
}

.loading-text {
  @apply text-sm text-gray-600;
}

/* 过渡动画 */
.slide-over-enter-active,
.slide-over-leave-active {
  transition: opacity 0.3s ease;
}

.slide-over-enter-from,
.slide-over-leave-to {
  opacity: 0;
}

.slide-over-enter-active .settings-panel,
.slide-over-leave-active .settings-panel {
  transition: transform 0.3s ease;
}

.slide-over-enter-from .settings-panel,
.slide-over-leave-to .settings-panel {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .header-content {
    @apply flex-col space-y-4 items-start;
  }

  .actions-section {
    @apply w-full justify-between ml-0 space-x-0;
  }

  .action-buttons {
    @apply flex-1;
  }

  .status-indicators {
    @apply flex-col items-start space-y-2 space-x-0;
  }
}

@media (max-width: 640px) {
  .main-container {
    @apply px-3 py-6 space-y-6;
  }

  .header-container {
    @apply px-3;
  }

  .header-content {
    @apply py-4;
  }

  .action-button .button-text {
    @apply hidden;
  }

  .action-button .button-icon {
    @apply mr-0;
  }

  .settings-panel {
    @apply w-full;
  }

  .status-indicator {
    @apply hidden;
  }

  .last-update .update-text {
    @apply hidden;
  }
}

/* 主题相关样式 */
.statistics-page-layout[data-theme="dark"] {
  @apply bg-gray-900;
}

.statistics-page-layout[data-theme="dark"] .page-header {
  @apply bg-gray-800 border-gray-700;
}

.statistics-page-layout[data-theme="dark"] .page-title {
  @apply text-white;
}

.statistics-page-layout[data-theme="dark"] .page-description {
  @apply text-gray-300;
}

/* 动画禁用样式 */
.statistics-page-layout[data-animations="false"] * {
  transition: none !important;
  animation: none !important;
}

/* 紧凑模式样式 */
.statistics-page-layout[data-compact="true"] .main-container {
  @apply space-y-4;
}

.statistics-page-layout[data-compact="true"] .header-content {
  @apply py-4;
}

/* 全屏模式样式 */
.statistics-page-layout[data-fullscreen="true"] .page-header {
  @apply hidden;
}

.statistics-page-layout[data-fullscreen="true"] .main-container {
  @apply max-w-none px-0 py-0;
}
</style>