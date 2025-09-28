<template>
  <div class="hot-word-limit-selector">
    <label class="label">显示数量</label>
    <div class="selector-content">
      <!-- 预设选项 -->
      <div class="preset-options">
        <button
          v-for="option in limitOptions"
          :key="option.value"
          @click="selectLimit(option.value)"
          :class="[
            'preset-btn',
            {
              'preset-btn-active': localLimit === option.value,
              'preset-btn-inactive': localLimit !== option.value
            }
          ]"
        >
          {{ option.label }}
        </button>
      </div>

      <!-- 自定义数量输入 -->
      <div class="custom-input-group">
        <label class="custom-label">自定义</label>
        <div class="input-with-controls">
          <input
            v-model.number="customLimit"
            type="number"
            :min="minLimit"
            :max="maxLimit"
            class="custom-input"
            @input="handleCustomInput"
            @blur="handleCustomBlur"
          >
          <div class="input-controls">
            <button
              @click="decreaseLimit"
              :disabled="customLimit <= minLimit"
              class="control-btn decrease-btn"
              title="减少"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
              </svg>
            </button>
            <button
              @click="increaseLimit"
              :disabled="customLimit >= maxLimit"
              class="control-btn increase-btn"
              title="增加"
            >
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 限制说明 -->
      <div class="limit-info">
        <span class="info-text">
          当前显示前 <strong class="text-green-600">{{ localLimit }}</strong> 个热词
          <span class="text-gray-400">(范围: {{ minLimit }}-{{ maxLimit }})</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface LimitOption {
  label: string
  value: number
}

interface Props {
  limit: number
  minLimit?: number
  maxLimit?: number
}

interface Emits {
  (e: 'update:limit', value: number): void
}

const props = withDefaults(defineProps<Props>(), {
  minLimit: 10,
  maxLimit: 1000
})

const emit = defineEmits<Emits>()

const localLimit = ref(props.limit)
const customLimit = ref(props.limit)

/**
 * 预设限制选项
 */
const limitOptions: LimitOption[] = [
  { label: '10', value: 10 },
  { label: '20', value: 20 },
  { label: '50', value: 50 },
  { label: '100', value: 100 },
  { label: '200', value: 200 }
]

/**
 * 检查是否为预设值
 */
const isPresetValue = computed(() => {
  return limitOptions.some(option => option.value === localLimit.value)
})

/**
 * 选择预设限制
 */
const selectLimit = (value: number) => {
  localLimit.value = value
  customLimit.value = value
  emit('update:limit', value)
}

/**
 * 处理自定义输入
 */
const handleCustomInput = () => {
  // 确保输入值在有效范围内
  if (customLimit.value < props.minLimit) {
    customLimit.value = props.minLimit
  } else if (customLimit.value > props.maxLimit) {
    customLimit.value = props.maxLimit
  }

  localLimit.value = customLimit.value
  emit('update:limit', customLimit.value)
}

/**
 * 处理输入框失焦
 */
const handleCustomBlur = () => {
  // 确保输入值是有效数字
  if (isNaN(customLimit.value) || customLimit.value < props.minLimit) {
    customLimit.value = props.minLimit
  } else if (customLimit.value > props.maxLimit) {
    customLimit.value = props.maxLimit
  }

  localLimit.value = customLimit.value
  emit('update:limit', customLimit.value)
}

/**
 * 减少限制数量
 */
const decreaseLimit = () => {
  if (customLimit.value > props.minLimit) {
    customLimit.value = Math.max(customLimit.value - 10, props.minLimit)
    localLimit.value = customLimit.value
    emit('update:limit', customLimit.value)
  }
}

/**
 * 增加限制数量
 */
const increaseLimit = () => {
  if (customLimit.value < props.maxLimit) {
    customLimit.value = Math.min(customLimit.value + 10, props.maxLimit)
    localLimit.value = customLimit.value
    emit('update:limit', customLimit.value)
  }
}

// 监听props变化
watch(() => props.limit, (newVal) => {
  localLimit.value = newVal
  customLimit.value = newVal
})
</script>

<style scoped>
.hot-word-limit-selector {
  @apply space-y-3;
}

.label {
  @apply block text-sm font-medium text-gray-700;
}

.selector-content {
  @apply space-y-4;
}

/* 预设选项样式 */
.preset-options {
  @apply flex flex-wrap gap-2;
}

.preset-btn {
  @apply
    px-3
    py-1.5
    text-sm
    font-medium
    rounded-md
    border
    transition-colors
    duration-200
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:ring-offset-2;
}

.preset-btn-active {
  @apply
    bg-green-100
    text-green-800
    border-green-300
    hover:bg-green-200;
}

.preset-btn-inactive {
  @apply
    bg-white
    text-gray-600
    border-gray-300
    hover:bg-gray-50
    hover:border-gray-400;
}

/* 自定义输入样式 */
.custom-input-group {
  @apply space-y-2;
}

.custom-label {
  @apply block text-xs font-medium text-gray-600;
}

.input-with-controls {
  @apply relative flex items-center;
}

.custom-input {
  @apply
    w-full
    pr-16
    pl-3
    py-2
    text-sm
    border
    border-gray-300
    rounded-md
    focus:outline-none
    focus:ring-2
    focus:ring-green-500
    focus:border-green-500
    transition-colors
    duration-200;
}

.input-controls {
  @apply absolute right-1 flex;
}

.control-btn {
  @apply
    p-1
    text-gray-400
    hover:text-gray-600
    disabled:text-gray-300
    disabled:cursor-not-allowed
    focus:outline-none
    focus:text-green-600
    transition-colors
    duration-200;
}

.decrease-btn:hover:not(:disabled) {
  @apply text-red-500;
}

.increase-btn:hover:not(:disabled) {
  @apply text-green-500;
}

/* 限制信息样式 */
.limit-info {
  @apply
    p-3
    bg-green-50
    border
    border-green-200
    rounded-md;
}

.info-text {
  @apply text-sm text-gray-700;
}

/* 响应式优化 */
@media (max-width: 640px) {
  .preset-options {
    @apply grid grid-cols-3 gap-2;
  }

  .preset-btn {
    @apply text-xs px-2 py-1;
  }

  .custom-input {
    @apply text-base; /* 防止iOS缩放 */
  }

  .control-btn {
    @apply p-1.5; /* 增加触摸目标大小 */
  }
}

/* 数字输入框优化 */
.custom-input::-webkit-outer-spin-button,
.custom-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.custom-input[type=number] {
  -moz-appearance: textfield;
}

/* 焦点状态优化 */
.custom-input:focus + .input-controls .control-btn {
  @apply text-green-500;
}

/* 验证状态样式 */
.custom-input:invalid {
  @apply border-red-300 focus:border-red-500 focus:ring-red-500;
}

/* 动画优化 */
.control-btn {
  transition: all 0.15s ease-in-out;
}

.control-btn:active:not(:disabled) {
  @apply scale-95;
}
</style>