<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="dialog-overlay" @click="handleClose">
        <div class="dialog-container" @click.stop>
          <div class="dialog-header">
            <h3 class="dialog-title">聚类参数配置</h3>
            <button class="close-btn" @click="handleClose">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>

          <div class="dialog-body">
            <!-- 时间范围 -->
            <div class="form-group">
              <label class="form-label">时间范围</label>
              <div class="time-range-buttons">
                <button
                  v-for="option in timeRangeOptions"
                  :key="option.value"
                  class="time-range-btn"
                  :class="{ active: form.timeRange === option.value }"
                  @click="form.timeRange = option.value"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>

            <!-- EPS 参数 -->
            <div class="form-group">
              <label class="form-label">
                DBSCAN eps 参数
                <span class="param-value">{{ form.eps }}</span>
              </label>
              <input
                v-model.number="form.eps"
                type="range"
                min="0.1"
                max="1.0"
                step="0.05"
                class="range-input"
              />
              <div class="param-hint">
                eps 值越小,聚类越严格,簇数量越多。推荐范围:0.3-0.5
              </div>
            </div>

            <!-- Min Samples 参数 -->
            <div class="form-group">
              <label class="form-label">
                DBSCAN min_samples 参数
                <span class="param-value">{{ form.minSamples }}</span>
              </label>
              <input
                v-model.number="form.minSamples"
                type="range"
                min="2"
                max="10"
                step="1"
                class="range-input"
              />
              <div class="param-hint">
                最小样本数越大,形成簇的难度越高。推荐范围:3-5
              </div>
            </div>

            <!-- 距离度量 -->
            <div class="form-group">
              <label class="form-label">距离度量方式</label>
              <div class="radio-group">
                <label class="radio-label">
                  <input
                    v-model="form.metric"
                    type="radio"
                    value="cosine"
                    class="radio-input"
                  />
                  <span class="radio-text">余弦距离 (推荐)</span>
                </label>
                <label class="radio-label">
                  <input
                    v-model="form.metric"
                    type="radio"
                    value="euclidean"
                    class="radio-input"
                  />
                  <span class="radio-text">欧氏距离</span>
                </label>
              </div>
              <div class="param-hint">
                余弦距离更适合文本相似度计算
              </div>
            </div>
          </div>

          <div class="dialog-footer">
            <button class="btn btn-cancel" @click="handleClose">取消</button>
            <button class="btn btn-confirm" @click="handleConfirm">确定</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

interface FormData {
  searchSpaceId: number
  timeRange: string
  eps: number
  minSamples: number
  metric: string
}

interface Props {
  visible: boolean
  initialData?: Partial<FormData>
}

const props = withDefaults(defineProps<Props>(), {
  visible: false
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', data: FormData): void
}>()

const timeRangeOptions = [
  { value: '7d', label: '最近 7 天' },
  { value: '30d', label: '最近 30 天' },
  { value: '90d', label: '最近 90 天' }
]

const form = reactive<FormData>({
  searchSpaceId: 7,
  timeRange: '30d',
  eps: 0.4,
  minSamples: 3,
  metric: 'cosine'
})

// 监听初始数据变化
watch(
  () => props.initialData,
  (newData) => {
    if (newData) {
      Object.assign(form, newData)
    }
  },
  { immediate: true, deep: true }
)

const handleClose = () => {
  emit('update:visible', false)
}

const handleConfirm = () => {
  emit('confirm', { ...form })
  handleClose()
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-container {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #333;
}

.close-btn svg {
  width: 20px;
  height: 20px;
}

.dialog-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.form-group {
  margin-bottom: 24px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.param-value {
  color: #52c41a;
  font-weight: 600;
}

.time-range-buttons {
  display: flex;
  gap: 8px;
}

.time-range-btn {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.time-range-btn:hover {
  border-color: #52c41a;
  color: #52c41a;
}

.time-range-btn.active {
  background: #f6ffed;
  border-color: #52c41a;
  color: #52c41a;
  font-weight: 500;
}

.range-input {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: #e8e8e8;
  outline: none;
  -webkit-appearance: none;
}

.range-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #52c41a;
  cursor: pointer;
  transition: all 0.2s;
}

.range-input::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  box-shadow: 0 0 0 4px rgba(82, 196, 26, 0.1);
}

.range-input::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #52c41a;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.range-input::-moz-range-thumb:hover {
  transform: scale(1.2);
  box-shadow: 0 0 0 4px rgba(82, 196, 26, 0.1);
}

.param-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.radio-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.radio-input {
  margin-right: 8px;
  cursor: pointer;
  accent-color: #52c41a;
}

.radio-text {
  font-size: 14px;
  color: #666;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e8e8e8;
}

.btn {
  padding: 8px 24px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-cancel {
  background: #f5f5f5;
  color: #666;
}

.btn-cancel:hover {
  background: #e8e8e8;
}

.btn-confirm {
  background: #52c41a;
  color: white;
}

.btn-confirm:hover {
  background: #73d13d;
}

/* 过渡动画 */
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.3s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

.dialog-fade-enter-active .dialog-container,
.dialog-fade-leave-active .dialog-container {
  transition: transform 0.3s ease;
}

.dialog-fade-enter-from .dialog-container,
.dialog-fade-leave-to .dialog-container {
  transform: scale(0.9);
}
</style>
