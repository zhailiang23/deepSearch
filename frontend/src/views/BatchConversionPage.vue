<template>
  <div class="batch-conversion-page">
    <!-- 顶部操作按钮 -->
    <div class="page-header">
      <div class="header-actions">
        <button @click="handleSubmit" :disabled="isLoading" class="btn btn-primary">
          <span v-if="isLoading" class="btn-spinner"></span>
          {{ isLoading ? '识别中...' : '开始识别' }}
        </button>
        <button @click="showConfigDialog = true" class="btn btn-secondary">
          批量识别配置
        </button>
        <button @click="showPromptConfigDialog = true" class="btn btn-secondary">
          识别提示词配置
        </button>
      </div>
      <div v-if="results.length > 0" class="results-stats">
        <span class="stat-item">总数: {{ totalCount }}</span>
        <span class="stat-item success">成功: {{ successCount }}</span>
        <span class="stat-item failure">失败: {{ failureCount }}</span>
        <span v-if="insertedCount > 0" class="stat-item inserted">已导入: {{ insertedCount }}</span>
        <span v-if="skippedCount > 0" class="stat-item skipped">跳过: {{ skippedCount }}</span>
      </div>
    </div>

    <!-- 主内容区域：结果列表 -->
    <div class="main-content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-section">
        <div class="loading-spinner"></div>
        <p>正在批量识别图片，请稍候...</p>
      </div>

      <!-- 结果展示 -->
      <div v-else-if="results.length > 0" class="results-section">
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
            <div class="result-actions">
              <button
                v-if="result.success"
                @click="handleEditResult(result)"
                class="btn btn-edit"
                title="编辑活动信息"
              >
                编辑
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-section">
        <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
        </svg>
        <p class="empty-text">请点击"批量识别配置"按钮配置数据库连接，然后点击"开始识别"</p>
      </div>
    </div>

    <!-- 配置对话框 -->
    <el-dialog
      v-model="showConfigDialog"
      title="批量识别配置"
      width="600px"
      :close-on-click-modal="false"
    >
      <form @submit.prevent="saveConfig" class="config-form">
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
          <label for="searchSpaceId">目标搜索空间 *</label>
          <select
            id="searchSpaceId"
            v-model="formData.searchSpaceId"
            required
            class="form-input"
          >
            <option value="">请选择搜索空间</option>
            <option v-for="space in searchSpaces" :key="space.id" :value="space.id">
              {{ space.name }}
            </option>
          </select>
        </div>
      </form>

      <template #footer>
        <div class="dialog-footer">
          <button @click="showConfigDialog = false" class="btn btn-cancel">
            取消
          </button>
          <button @click="saveConfig" class="btn btn-primary">
            保存配置
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <ActivityEditDialog
      v-model:open="showEditDialog"
      :result="editingResult"
      @save="handleSaveEdit"
    />

    <!-- 提示词配置对话框 -->
    <PromptConfigDialog
      v-model:visible="showPromptConfigDialog"
      @saved="handlePromptConfigSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { batchConvertImages, type ImageRecognitionResult } from '@/api/batchConversion'
import { ElMessage, ElDialog } from 'element-plus'
import http from '@/utils/http'
import ActivityEditDialog from '@/components/batchConversion/ActivityEditDialog.vue'
import PromptConfigDialog from '@/components/batchConversion/PromptConfigDialog.vue'

// 搜索空间列表
const searchSpaces = ref<Array<{id: string; name: string}>>([])

// 对话框显示状态
const showConfigDialog = ref(false)
const showEditDialog = ref(false)
const showPromptConfigDialog = ref(false)
const editingResult = ref<ImageRecognitionResult | null>(null)

// 表单数据接口
interface BatchConversionFormData {
  dbHost: string
  dbPort: number
  dbName: string
  dbUsername: string
  dbPassword: string
  tableName: string
  imagePathColumn: string
  imageUrlColumn?: string
  primaryKeyColumn?: string
  searchSpaceId: string
}

// 表单数据
const formData = ref<BatchConversionFormData>({
  dbHost: 'localhost',
  dbPort: 3306,
  dbName: 'mgmt_db',
  dbUsername: 'mgmt_user',
  dbPassword: 'mgmt_password',
  tableName: 'pictures',
  imagePathColumn: 'path',
  imageUrlColumn: 'link',
  primaryKeyColumn: 'id',
  searchSpaceId: ''
})

// 状态
const isLoading = ref(false)
const results = ref<ImageRecognitionResult[]>([])
const totalCount = ref(0)
const successCount = ref(0)
const failureCount = ref(0)
const insertedCount = ref(0)
const skippedCount = ref(0)

// 保存配置
const saveConfig = () => {
  if (!formData.value.searchSpaceId) {
    ElMessage.error('请选择目标搜索空间')
    return
  }
  showConfigDialog.value = false
  ElMessage.success('配置已保存')
}


// 提交表单
const handleSubmit = async () => {
  if (!formData.value.searchSpaceId) {
    ElMessage.error('请先配置目标搜索空间')
    showConfigDialog.value = true
    return
  }

  isLoading.value = true
  results.value = []

  try {
    // 步骤1：调用批量识别API获取识别结果（不保存到索引）
    const batchRequest = {
      dbHost: formData.value.dbHost,
      dbPort: formData.value.dbPort,
      dbName: formData.value.dbName,
      dbUsername: formData.value.dbUsername,
      dbPassword: formData.value.dbPassword,
      tableName: formData.value.tableName,
      imagePathColumn: formData.value.imagePathColumn,
      imageUrlColumn: formData.value.imageUrlColumn,
      primaryKeyColumn: formData.value.primaryKeyColumn
    }

    const response = await batchConvertImages(batchRequest)

    if (!response.success) {
      ElMessage.error(response.message || '批量识别失败')
      return
    }

    // 显示识别结果
    results.value = response.results
    totalCount.value = response.totalCount
    successCount.value = response.successCount
    failureCount.value = response.failureCount

    // 步骤2：将识别成功的结果转换为JSON数组
    const activitiesToImport = response.results
      .filter((result: ImageRecognitionResult) => result.success && result.recognizedText)
      .map((result: ImageRecognitionResult) => {
        return {
          id: result.id,
          name: result.name || '',
          descript: result.descript || '',
          link: result.link || '',
          startDate: result.startDate || '',
          endDate: result.endDate || '',
          status: result.status || '进行中',
          imagePath: result.imagePath,
          all: result.recognizedText
        }
      })

    if (activitiesToImport.length === 0) {
      ElMessage.warning('没有可导入的活动数据')
      return
    }

    // 步骤3：调用import-json-content API保存到Elasticsearch
    const importResponse = await http.post(
      `/search-spaces/${formData.value.searchSpaceId}/import-json-content`,
      activitiesToImport
    )

    // http.post返回的是整个ApiResponse对象
    if (importResponse && importResponse.success) {
      insertedCount.value = activitiesToImport.length
      skippedCount.value = 0

      ElMessage.success(
        `批量识别并导入完成！识别成功: ${response.successCount}, 失败: ${response.failureCount}, 已导入: ${activitiesToImport.length}`
      )
    } else {
      ElMessage.error(importResponse?.message || '导入失败')
    }
  } catch (error: any) {
    console.error('批量识别错误:', error)
    ElMessage.error(error.response?.data?.message || error.message || '批量识别失败，请检查配置')
  } finally {
    isLoading.value = false
  }
}

// 加载搜索空间列表
const loadSearchSpaces = async () => {
  try {
    const result = await http.get('/search-spaces', {
      params: {
        page: 0,
        size: 100
      }
    })

    // http.get返回的是整个ApiResponse对象,需要访问data字段
    if (result && result.data && result.data.content && Array.isArray(result.data.content)) {
      searchSpaces.value = result.data.content.map((space: any) => ({
        id: space.id.toString(),
        name: space.name
      }))

      // 自动选中名称为"活动"的搜索空间作为默认值
      const activitySpace = searchSpaces.value.find(space => space.name === '活动')
      if (activitySpace) {
        formData.value.searchSpaceId = activitySpace.id
      }
    } else {
      searchSpaces.value = []
    }
  } catch (error) {
    console.error('加载搜索空间列表失败:', error)
    searchSpaces.value = []
  }
}

// 编辑结果
const handleEditResult = (result: ImageRecognitionResult) => {
  console.log('handleEditResult 被调用, result:', result)
  editingResult.value = result
  console.log('editingResult.value =', editingResult.value)
  showEditDialog.value = true
  console.log('showEditDialog.value =', showEditDialog.value)
}

// 保存编辑
const handleSaveEdit = (updatedResult: ImageRecognitionResult) => {
  // 在results数组中找到对应的结果并更新
  const index = results.value.findIndex(r => r.id === updatedResult.id)
  if (index !== -1) {
    results.value[index] = updatedResult
  }
}

// 提示词配置保存成功处理
const handlePromptConfigSaved = () => {
  ElMessage.success('提示词配置已更新,下次识别将使用新的提示词')
}

// 组件挂载时加载搜索空间列表
onMounted(() => {
  loadSearchSpaces()
})
</script>

<style scoped>
.batch-conversion-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow: hidden;
}

/* 页面头部 */
.page-header {
  flex-shrink: 0;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.results-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
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

/* 按钮样式 */
.btn {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
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

.btn-secondary {
  background: white;
  color: #495057;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f8f9fa;
  border-color: #81c784;
  color: #66bb6a;
}

.btn-cancel {
  background: white;
  color: #6c757d;
  border: 1px solid #d1d5db;
}

.btn-cancel:hover {
  background: #f8f9fa;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #ffffff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
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
  text-align: center;
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
  height: 182px; /* 150px (缩略图) + 32px (padding) */
  overflow: hidden;
  position: relative;
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
  overflow: hidden;
}

.result-path {
  font-size: 12px;
  color: #6c757d;
  margin-bottom: 8px;
  word-break: break-all;
}

.result-text {
  flex: 1;
  font-size: 14px;
  color: #2c3e50;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义内容区域滚动条 */
.result-text::-webkit-scrollbar {
  width: 4px;
}

.result-text::-webkit-scrollbar-track {
  background: #f8f9fa;
  border-radius: 2px;
}

.result-text::-webkit-scrollbar-thumb {
  background: #c1c7cd;
  border-radius: 2px;
}

.result-text::-webkit-scrollbar-thumb:hover {
  background: #adb5bd;
}

.result-error {
  font-size: 14px;
  color: #dc3545;
  font-weight: 500;
}

.result-actions {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  padding-top: 4px;
}

.btn-edit {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #81c784;
  background: white;
  color: #66bb6a;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-edit:hover {
  background: #81c784;
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(129, 199, 132, 0.3);
}

.btn-edit:active {
  transform: translateY(0);
}

/* 配置表单 */
.config-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
}

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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
