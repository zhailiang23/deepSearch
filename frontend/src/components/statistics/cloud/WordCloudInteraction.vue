<template>
  <div class="word-cloud-interaction" :class="{ 'interactive': isInteractive }">
    <!-- 交互控制面板 -->
    <div v-if="showControls" class="interaction-controls">
      <div class="controls-header">
        <h4 class="text-sm font-medium text-foreground">交互控制</h4>
        <Button
          variant="ghost"
          size="sm"
          @click="toggleControls"
          class="h-6 w-6 p-0"
        >
          <ChevronUp v-if="showControls" class="h-4 w-4" />
          <ChevronDown v-else class="h-4 w-4" />
        </Button>
      </div>

      <div class="controls-content space-y-4">
        <!-- 基础交互设置 -->
        <div class="control-group">
          <Label class="text-xs font-medium text-muted-foreground">基础交互</Label>
          <div class="space-y-2">
            <label class="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                v-model="localConfig.enableHover"
                @change="updateConfig"
                class="checkbox"
              />
              启用悬停效果
            </label>
            <label class="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                v-model="localConfig.enableClick"
                @change="updateConfig"
                class="checkbox"
              />
              启用点击交互
            </label>
          </div>
        </div>

        <!-- 悬停效果设置 -->
        <div v-if="localConfig.enableHover" class="control-group">
          <Label class="text-xs font-medium text-muted-foreground">悬停效果</Label>
          <Select :value="localConfig.hoverEffect" @update:value="updateHoverEffect">
            <SelectTrigger class="h-8">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="highlight">高亮</SelectItem>
              <SelectItem value="shadow">阴影</SelectItem>
              <SelectItem value="scale">缩放</SelectItem>
              <SelectItem value="glow">发光</SelectItem>
              <SelectItem value="none">无效果</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- 点击动作设置 -->
        <div v-if="localConfig.enableClick" class="control-group">
          <Label class="text-xs font-medium text-muted-foreground">点击动作</Label>
          <Select :value="localConfig.clickAction" @update:value="updateClickAction">
            <SelectTrigger class="h-8">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="search">搜索词语</SelectItem>
              <SelectItem value="filter">筛选数据</SelectItem>
              <SelectItem value="custom">自定义</SelectItem>
              <SelectItem value="none">无动作</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- 缩放控制 -->
        <div class="control-group">
          <Label class="text-xs font-medium text-muted-foreground">缩放控制</Label>
          <div class="space-y-2">
            <label class="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                v-model="zoomConfig.enabled"
                @change="updateZoomConfig"
                class="checkbox"
              />
              启用滚轮缩放
            </label>
            <div v-if="zoomConfig.enabled" class="grid grid-cols-2 gap-2">
              <div class="flex items-center gap-1">
                <Label class="text-xs text-muted-foreground">最小</Label>
                <input
                  type="number"
                  v-model.number="zoomConfig.minScale"
                  @change="updateZoomConfig"
                  min="0.1"
                  max="1"
                  step="0.1"
                  class="zoom-input"
                />
              </div>
              <div class="flex items-center gap-1">
                <Label class="text-xs text-muted-foreground">最大</Label>
                <input
                  type="number"
                  v-model.number="zoomConfig.maxScale"
                  @change="updateZoomConfig"
                  min="1"
                  max="10"
                  step="0.5"
                  class="zoom-input"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 拖拽控制 -->
        <div class="control-group">
          <Label class="text-xs font-medium text-muted-foreground">拖拽控制</Label>
          <div class="space-y-2">
            <label class="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                v-model="dragConfig.enabled"
                @change="updateDragConfig"
                class="checkbox"
              />
              启用拖拽移动
            </label>
            <label v-if="dragConfig.enabled" class="flex items-center gap-2 text-xs">
              <input
                type="checkbox"
                v-model="dragConfig.inertia"
                @change="updateDragConfig"
                class="checkbox"
              />
              惯性滑动
            </label>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="control-actions">
          <Button
            variant="outline"
            size="sm"
            @click="resetInteraction"
            class="text-xs"
          >
            重置交互
          </Button>
          <Button
            variant="outline"
            size="sm"
            @click="resetViewport"
            class="text-xs"
          >
            重置视图
          </Button>
        </div>
      </div>
    </div>

    <!-- 交互状态显示 -->
    <div v-if="showStatus" class="interaction-status">
      <div class="status-item">
        <span class="status-label">当前缩放:</span>
        <span class="status-value">{{ Math.round(interactionState.currentScale * 100) }}%</span>
      </div>
      <div v-if="interactionState.hoveredWord" class="status-item">
        <span class="status-label">悬停词语:</span>
        <span class="status-value">{{ interactionState.hoveredWord }}</span>
      </div>
      <div v-if="interactionState.isDragging" class="status-item">
        <span class="status-label">拖拽中</span>
        <div class="drag-indicator"></div>
      </div>
    </div>

    <!-- 词语详情气泡 -->
    <Teleport to="body">
      <div
        v-if="showTooltip && hoveredWordInfo"
        ref="tooltipRef"
        class="word-tooltip"
        :style="tooltipStyle"
      >
        <div class="tooltip-content">
          <div class="tooltip-word">{{ hoveredWordInfo.word }}</div>
          <div class="tooltip-info">
            <div class="tooltip-weight">权重: {{ hoveredWordInfo.weight }}</div>
            <div v-if="hoveredWordInfo.frequency" class="tooltip-frequency">
              频次: {{ hoveredWordInfo.frequency }}
            </div>
            <div v-if="hoveredWordInfo.rank" class="tooltip-rank">
              排名: #{{ hoveredWordInfo.rank }}
            </div>
          </div>
          <div v-if="hoveredWordInfo.extraData" class="tooltip-extra">
            {{ hoveredWordInfo.extraData }}
          </div>
        </div>
        <div class="tooltip-arrow"></div>
      </div>
    </Teleport>

    <!-- 点击波纹效果 -->
    <div
      v-for="ripple in ripples"
      :key="ripple.id"
      class="click-ripple"
      :style="{
        left: ripple.x + 'px',
        top: ripple.y + 'px',
        animationDuration: ripple.duration + 'ms'
      }"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ChevronUp, ChevronDown } from 'lucide-vue-next'

import type {
  WordCloudItem,
  WordCloudDimension,
  WordCloudInteractionConfig
} from '@/types/wordCloudTypes'

import {
  useWordCloudEvents,
  createDefaultInteractionConfig,
  createDefaultZoomConfig,
  createDefaultDragConfig,
  type WordCloudEventHandlers,
  type ZoomConfig,
  type DragConfig
} from '@/composables/useWordCloudEvents'

import {
  WordCloudDataManager,
  InteractionAnimationManager,
  createInteractionEffect,
  throttle,
  debounce
} from '@/utils/wordCloudInteractions'

// Props接口
interface Props {
  canvasRef: HTMLCanvasElement | null
  config?: WordCloudInteractionConfig
  showControls?: boolean
  showStatus?: boolean
  showTooltip?: boolean
  onWordClick?: (word: string, item: WordCloudItem) => void
  onWordHover?: (word: string | null, item: WordCloudItem | null) => void
  onZoom?: (scale: number) => void
  onDrag?: (deltaX: number, deltaY: number) => void
}

const props = withDefaults(defineProps<Props>(), {
  config: () => createDefaultInteractionConfig(),
  showControls: true,
  showStatus: true,
  showTooltip: true
})

// Emits
const emit = defineEmits<{
  configChange: [config: WordCloudInteractionConfig]
  wordClick: [word: string, item: WordCloudItem, event: MouseEvent]
  wordHover: [word: string | null, item: WordCloudItem | null, event: MouseEvent | null]
  zoom: [scale: number, event: WheelEvent]
  drag: [deltaX: number, deltaY: number, event: MouseEvent]
  interactionStateChange: [state: any]
}>()

// 响应式数据
const canvasRef = ref<HTMLCanvasElement | null>(props.canvasRef)
const localConfig = reactive<WordCloudInteractionConfig>({ ...props.config })
const tooltipRef = ref<HTMLElement | null>(null)

// 控制状态
const showControlsPanel = ref(props.showControls)

// 悬停词语信息
const hoveredWordInfo = ref<{
  word: string
  weight: number
  frequency?: number
  rank?: number
  extraData?: any
} | null>(null)

// 提示框位置
const tooltipPosition = ref({ x: 0, y: 0 })

// 点击波纹效果
const ripples = ref<Array<{
  id: number
  x: number
  y: number
  duration: number
}>>([])

// 数据管理器
let dataManager: WordCloudDataManager | null = null
let animationManager: InteractionAnimationManager | null = null

// 初始化管理器
const initializeManagers = () => {
  if (canvasRef.value) {
    dataManager = new WordCloudDataManager(canvasRef.value)
    animationManager = new InteractionAnimationManager()
  }
}

// 事件处理器配置
const eventHandlers: WordCloudEventHandlers = {
  onWordClick: (word, item, dimension, event) => {
    // 创建点击波纹效果
    createClickRipple(event.clientX, event.clientY)

    // 处理点击动作
    handleWordClick(word, item, event)

    // 触发事件
    emit('wordClick', word, item, event)
    props.onWordClick?.(word, item)
  },

  onWordHover: (word, item, dimension, event) => {
    // 更新悬停信息
    updateHoveredWordInfo(word, item)

    // 更新提示框位置
    if (event && word) {
      updateTooltipPosition(event.clientX, event.clientY)
    }

    // 触发事件
    emit('wordHover', word, item, event)
    props.onWordHover?.(word, item)
  },

  onZoom: (scale, event) => {
    emit('zoom', scale, event)
    props.onZoom?.(scale)
  },

  onDrag: (deltaX, deltaY, event) => {
    emit('drag', deltaX, deltaY, event)
    props.onDrag?.(deltaX, deltaY)
  }
}

// 使用事件管理可组合函数
const {
  interactionState,
  zoomConfig,
  dragConfig,
  isInteractive,
  registerEventListeners,
  unregisterEventListeners,
  resetInteractionState,
  updateZoomConfig: updateZoomConfigComposable,
  updateDragConfig: updateDragConfigComposable
} = useWordCloudEvents(canvasRef, ref(localConfig), eventHandlers)

// 计算属性
const tooltipStyle = computed(() => ({
  left: tooltipPosition.value.x + 'px',
  top: tooltipPosition.value.y + 'px',
  opacity: hoveredWordInfo.value ? 1 : 0,
  pointerEvents: 'none' as 'none'
}))

// 方法
const toggleControls = () => {
  showControlsPanel.value = !showControlsPanel.value
}

const updateConfig = () => {
  emit('configChange', localConfig)
}

const updateHoverEffect = (effect: string) => {
  localConfig.hoverEffect = effect as any
  updateConfig()
}

const updateClickAction = (action: string) => {
  localConfig.clickAction = action as any
  updateConfig()
}

const updateZoomConfig = () => {
  updateZoomConfigComposable(zoomConfig.value)
}

const updateDragConfig = () => {
  updateDragConfigComposable(dragConfig.value)
}

const resetInteraction = () => {
  resetInteractionState()

  // 重置配置
  Object.assign(localConfig, createDefaultInteractionConfig())
  Object.assign(zoomConfig.value, createDefaultZoomConfig())
  Object.assign(dragConfig.value, createDefaultDragConfig())

  updateConfig()
}

const resetViewport = () => {
  // 使用resetInteractionState来重置状态，因为interactionState是readonly
  resetInteractionState()

  // 重置画布变换
  if (canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) {
      ctx.setTransform(1, 0, 0, 1, 0, 0)
    }
  }
}

// 处理词语点击
const handleWordClick = (word: string, item: WordCloudItem, event: MouseEvent) => {
  switch (localConfig.clickAction) {
    case 'search':
      // 触发搜索操作
      console.log(`搜索词语: ${word}`)
      break
    case 'filter':
      // 触发筛选操作
      console.log(`筛选词语: ${word}`)
      break
    case 'custom':
      // 自定义操作
      console.log(`自定义操作: ${word}`)
      break
  }
}

// 更新悬停词语信息
const updateHoveredWordInfo = (word: string | null, item: WordCloudItem | null) => {
  if (word && item && Array.isArray(item)) {
    hoveredWordInfo.value = {
      word,
      weight: item[1] || 0,
      frequency: item[2] || undefined,
      rank: item[3] || undefined,
      extraData: item.length > 4 ? item.slice(4) : undefined
    }
  } else {
    hoveredWordInfo.value = null
  }
}

// 更新提示框位置
const updateTooltipPosition = throttle((clientX: number, clientY: number) => {
  tooltipPosition.value = {
    x: clientX + 10,
    y: clientY - 10
  }
}, 16) // 60fps

// 创建点击波纹效果
const createClickRipple = (x: number, y: number) => {
  const ripple = {
    id: Date.now(),
    x: x - 25, // 波纹半径的一半
    y: y - 25,
    duration: 600
  }

  ripples.value.push(ripple)

  // 动画结束后移除波纹
  setTimeout(() => {
    const index = ripples.value.findIndex(r => r.id === ripple.id)
    if (index > -1) {
      ripples.value.splice(index, 1)
    }
  }, ripple.duration)
}

// 监听配置变化
watch(
  () => props.config,
  (newConfig) => {
    if (newConfig) {
      Object.assign(localConfig, newConfig)
    }
  },
  { deep: true }
)

watch(
  () => props.canvasRef,
  (newCanvas) => {
    canvasRef.value = newCanvas
    if (newCanvas) {
      nextTick(() => {
        initializeManagers()
        registerEventListeners()
      })
    }
  }
)

// 监听交互状态变化
watch(
  interactionState,
  (newState) => {
    emit('interactionStateChange', newState)
  },
  { deep: true }
)

// 生命周期
onMounted(() => {
  initializeManagers()
})

onUnmounted(() => {
  unregisterEventListeners()
  animationManager?.stopAllAnimations()
})

// 暴露方法供父组件使用
defineExpose({
  resetInteraction,
  resetViewport,
  createClickRipple,
  dataManager: () => dataManager,
  animationManager: () => animationManager
})
</script>

<style scoped>
.word-cloud-interaction {
  @apply relative;
}

.word-cloud-interaction.interactive {
  @apply cursor-pointer;
}

/* 控制面板样式 */
.interaction-controls {
  @apply absolute top-2 right-2 bg-card border border-border rounded-lg shadow-md p-3 z-10;
  @apply transition-all duration-200;
  min-width: 200px;
}

.controls-header {
  @apply flex items-center justify-between mb-3;
}

.controls-content {
  @apply space-y-3;
}

.control-group {
  @apply space-y-2;
}

.control-actions {
  @apply flex gap-2 pt-2 border-t border-border;
}

/* 状态显示样式 */
.interaction-status {
  @apply absolute bottom-2 left-2 bg-card/90 border border-border rounded-md p-2 space-y-1 z-10;
  @apply text-xs text-muted-foreground;
}

.status-item {
  @apply flex items-center gap-2;
}

.status-label {
  @apply font-medium;
}

.status-value {
  @apply text-foreground;
}

.drag-indicator {
  @apply w-2 h-2 bg-primary rounded-full animate-pulse;
}

/* 输入控件样式 */
.checkbox {
  @apply w-3 h-3 accent-primary;
}

.zoom-input {
  @apply flex-1 px-1 py-0 text-xs border border-border rounded bg-background text-foreground;
  @apply focus:outline-none focus:ring-1 focus:ring-primary;
}

/* 提示框样式 */
.word-tooltip {
  @apply fixed z-50 bg-popover border border-border rounded-md shadow-lg p-2;
  @apply transition-opacity duration-200;
  @apply pointer-events-none;
  max-width: 200px;
}

.tooltip-content {
  @apply space-y-1;
}

.tooltip-word {
  @apply font-medium text-foreground text-sm;
}

.tooltip-info {
  @apply text-xs text-muted-foreground space-y-0.5;
}

.tooltip-extra {
  @apply text-xs text-muted-foreground border-t border-border pt-1;
}

.tooltip-arrow {
  @apply absolute -bottom-1 left-4 w-2 h-2 bg-popover border-r border-b border-border;
  @apply transform rotate-45;
}

/* 点击波纹效果 */
.click-ripple {
  @apply fixed w-12 h-12 border-2 border-primary rounded-full pointer-events-none z-40;
  @apply animate-ping;
  animation-fill-mode: forwards;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .interaction-controls {
    @apply right-1 top-1 p-2;
    min-width: 180px;
  }

  .word-tooltip {
    @apply p-1.5;
    max-width: 150px;
  }

  .tooltip-word {
    @apply text-xs;
  }

  .tooltip-info {
    @apply text-xs;
  }
}

/* 动画 */
@keyframes ripple {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

.click-ripple {
  animation: ripple var(--duration, 600ms) ease-out forwards;
}

/* 悬停效果 */
.interaction-controls:hover {
  @apply shadow-lg;
}

.control-group:hover {
  @apply bg-accent/50 rounded p-1 -m-1;
}

/* 禁用状态 */
.interaction-controls [disabled] {
  @apply opacity-50 cursor-not-allowed;
}

/* 深色模式适配 */
.dark .word-tooltip {
  @apply bg-popover-foreground text-popover;
}

.dark .tooltip-arrow {
  @apply bg-popover-foreground border-border;
}

.dark .click-ripple {
  @apply border-primary/80;
}
</style>