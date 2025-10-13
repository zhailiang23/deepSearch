<template>
    <!-- 主内容区域：左右分栏 -->
    <div class="main-content">
      <!-- 左侧：配置区域 (1/3) -->
      <div class="left-panel">
        <div class="config-section">
          <h2 class="section-title">数据库配置</h2>
          <form @submit.prevent="handleSubmit" class="config-form">
            <div class="form-group">
              <label for="dbHost">数据库 IP 地址 *</label>
              <input
                id="dbHost"
                v-model="formData.dbHost"
                type="text"
                placeholder="例如: localhost"
                required
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="dbPort">数据库端口 *</label>
              <input
                id="dbPort"
                v-model.number="formData.dbPort"
                type="number"
                placeholder="例如: 3306"
                required
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="dbName">数据库名称 *</label>
              <input
                id="dbName"
                v-model="formData.dbName"
                type="text"
                placeholder="例如: my_database"
                required
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="dbUsername">用户名 *</label>
              <input
                id="dbUsername"
                v-model="formData.dbUsername"
                type="text"
                placeholder="数据库用户名"
                required
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="dbPassword">密码 *</label>
              <input
                id="dbPassword"
                v-model="formData.dbPassword"
                type="password"
                placeholder="数据库密码"
                required
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="tableName">数据表名称 *</label>
              <input
                id="tableName"
                v-model="formData.tableName"
                type="text"
                placeholder="例如: images"
                required
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="imagePathColumn">图片地址列名 *</label>
              <input
                id="imagePathColumn"
                v-model="formData.imagePathColumn"
                type="text"
                placeholder="例如: image_path"
                required
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="imageUrlColumn">图片链接列名（可选）</label>
              <input
                id="imageUrlColumn"
                v-model="formData.imageUrlColumn"
                type="text"
                placeholder="例如: image_url"
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="primaryKeyColumn">主键列名</label>
              <input
                id="primaryKeyColumn"
                v-model="formData.primaryKeyColumn"
                type="text"
                placeholder="默认: id"
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="indexName">目标 Elasticsearch 索引名称</label>
              <input
                id="indexName"
                v-model="formData.indexName"
                type="text"
                placeholder="例如: activity"
                class="form-input"
              />
            </div>

            <div class="form-actions">
              <button type="submit" :disabled="isLoading" class="btn btn-primary">
                <span v-if="isLoading" class="btn-spinner"></span>
                {{ isLoading ? '识别中...' : '开始识别' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- 右侧：结果展示区域 (2/3) -->
      <div class="right-panel">
        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-section">
          <div class="loading-spinner"></div>
          <p>正在批量识别图片，请稍候...</p>
        </div>

        <!-- 结果展示 -->
        <div v-else-if="results.length > 0" class="results-section">
          <div class="results-header">
            <h2 class="section-title">识别结果</h2>
            <div class="results-stats">
              <span class="stat-item">总数: {{ totalCount }}</span>
              <span class="stat-item success">成功: {{ successCount }}</span>
              <span class="stat-item failure">失败: {{ failureCount }}</span>
              <span v-if="formData.indexName && (insertedCount > 0 || skippedCount > 0)" class="stat-item inserted">插入: {{ insertedCount }}</span>
              <span v-if="formData.indexName && skippedCount > 0" class="stat-item skipped">跳过: {{ skippedCount }}</span>
            </div>
          </div>

          <div class="results-list">
            <div
              v-for="result in results"
              :key="result.id"
              class="result-item"
              :class="{ failed: !result.success }"
            >
              <div class="result-thumbnail">
                <img
                  v-if="result.success && result.thumbnailBase64"
                  :src="result.thumbnailBase64"
                  :alt="result.imagePath"
                  class="thumbnail-image"
                />
                <div v-else class="thumbnail-placeholder">
                  <span>无缩略图</span>
                </div>
              </div>
              <div class="result-content">
                <div class="result-path">{{ result.imagePath }}</div>
                <div v-if="result.success" class="result-text">
                  {{ result.recognizedText }}
                </div>
                <div v-else class="result-error">
                  错误: {{ result.errorMessage }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-section">
          <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
          </svg>
          <p class="empty-text">请配置数据库连接并开始识别</p>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { batchConvertImages, type BatchConversionRequest, type ImageRecognitionResult } from '@/api/batchConversion'
import { ElMessage } from 'element-plus'

// 表单数据
const formData = ref<BatchConversionRequest>({
  dbHost: 'localhost',
  dbPort: 3306,
  dbName: '',
  dbUsername: '',
  dbPassword: '',
  tableName: '',
  imagePathColumn: '',
  imageUrlColumn: '',
  primaryKeyColumn: 'id',
  indexName: ''
})

// 状态
const isLoading = ref(false)
const results = ref<ImageRecognitionResult[]>([])
const totalCount = ref(0)
const successCount = ref(0)
const failureCount = ref(0)
const insertedCount = ref(0)
const skippedCount = ref(0)

// 提交表单
const handleSubmit = async () => {
  isLoading.value = true
  results.value = []

  try {
    const response = await batchConvertImages(formData.value)

    if (response.success) {
      results.value = response.results
      totalCount.value = response.totalCount
      successCount.value = response.successCount
      failureCount.value = response.failureCount
      insertedCount.value = response.insertedCount
      skippedCount.value = response.skippedCount

      let message = `批量识别完成！成功: ${response.successCount}, 失败: ${response.failureCount}`
      if (formData.value.indexName && (response.insertedCount > 0 || response.skippedCount > 0)) {
        message += `, 插入: ${response.insertedCount}, 跳过: ${response.skippedCount}`
      }
      ElMessage.success(message)
    } else {
      ElMessage.error(response.message || '批量识别失败')
    }
  } catch (error: any) {
    console.error('批量识别错误:', error)
    ElMessage.error(error.response?.data?.message || error.message || '批量识别失败，请检查配置')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.batch-conversion-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow: hidden;
}

.page-header {
  flex-shrink: 0;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.page-description {
  font-size: 14px;
  color: #6c757d;
}

/* 主内容区域：左右分栏 */
.main-content {
  flex: 1;
  display: flex;
  gap: 24px;
  min-height: 0;
  overflow: hidden;
}

/* 左侧面板 (1/3) */
.left-panel {
  flex: 0 0 33.333%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.config-section {
  flex: 1;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.config-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义滚动条 */
.config-form::-webkit-scrollbar {
  width: 6px;
}

.config-form::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.config-form::-webkit-scrollbar-thumb {
  background: #81c784;
  border-radius: 3px;
}

.config-form::-webkit-scrollbar-thumb:hover {
  background: #66bb6a;
}

.form-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #495057;
  margin-bottom: 8px;
}

.form-input {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #81c784;
  box-shadow: 0 0 0 3px rgba(129, 199, 132, 0.1);
}

.form-actions {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #e9ecef;
  flex-shrink: 0;
}

.btn {
  width: 100%;
  padding: 12px 32px;
  font-size: 16px;
  font-weight: 500;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #81c784 0%, #66bb6a 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #66bb6a 0%, #4caf50 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #ffffff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 右侧面板 (2/3) */
.right-panel {
  flex: 0 0 66.667%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 加载状态 */
.loading-section {
  flex: 1;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 48px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f4f6;
  border-top-color: #81c784;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 空状态 */
.empty-section {
  flex: 1;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 48px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  color: #d1d5db;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #6c757d;
}

/* 结果展示区域 */
.results-section {
  flex: 1;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 16px;
}

.results-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  font-size: 13px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 4px;
  background: #f8f9fa;
  white-space: nowrap;
}

.stat-item.success {
  color: #28a745;
  background: #d4edda;
}

.stat-item.failure {
  color: #dc3545;
  background: #f8d7da;
}

.stat-item.inserted {
  color: #007bff;
  background: #cfe2ff;
}

.stat-item.skipped {
  color: #6c757d;
  background: #e9ecef;
}

.results-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 自定义滚动条 */
.results-list::-webkit-scrollbar {
  width: 6px;
}

.results-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.results-list::-webkit-scrollbar-thumb {
  background: #81c784;
  border-radius: 3px;
}

.results-list::-webkit-scrollbar-thumb:hover {
  background: #66bb6a;
}

.result-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.result-item:hover {
  border-color: #81c784;
  box-shadow: 0 2px 8px rgba(129, 199, 132, 0.2);
}

.result-item.failed {
  border-color: #f8d7da;
  background: #fff5f5;
}

.result-thumbnail {
  flex-shrink: 0;
  width: 150px;
  height: 150px;
  border-radius: 4px;
  overflow: hidden;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.thumbnail-placeholder {
  color: #adb5bd;
  font-size: 13px;
}

.result-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.result-path {
  font-size: 12px;
  color: #6c757d;
  margin-bottom: 8px;
  word-break: break-all;
}

.result-text {
  font-size: 14px;
  color: #2c3e50;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-error {
  font-size: 14px;
  color: #dc3545;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .left-panel,
  .right-panel {
    flex: 1 1 auto;
    min-height: 400px;
  }
}
</style>
