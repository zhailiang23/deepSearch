<template>
    

    <!-- 控制区域 -->
    <div class="control-section">
      <!-- 时间范围选择 -->
      <div class="time-range-selector">
        <label class="control-label">时间范围</label>
        <div class="time-range-buttons">
          <button
            v-for="option in timeRangeOptions"
            :key="option.value"
            class="time-range-btn"
            :class="{ active: params.timeRange === option.value }"
            @click="params.timeRange = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button class="btn-secondary" @click="showParamDialog = true">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 1v6m0 6v6m0-12c-1.66 0-3 1.34-3 3m0 0H1m8 0h14M4 12H1m18 0h1" />
          </svg>
          聚类参数设置
        </button>
        <button class="btn-primary" @click="handleStartAnalysis(params)" :disabled="loading">
          <svg v-if="!loading" class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span v-if="loading" class="loading-spinner-small"></span>
          {{ loading ? '分析中...' : '开始分析' }}
        </button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div v-if="!loading && !result" class="empty-state">
      <svg class="empty-icon" viewBox="0 0 64 64">
        <circle cx="32" cy="32" r="30" fill="#f0f0f0" />
        <path
          d="M32 16c-8.8 0-16 7.2-16 16s7.2 16 16 16 16-7.2 16-16-7.2-16-16-16zm0 28c-6.6 0-12-5.4-12-12s5.4-12 12-12 12 5.4 12 12-5.4 12-12 12z"
          fill="#52c41a"
        />
        <circle cx="32" cy="32" r="4" fill="#52c41a" />
      </svg>
      <h3 class="empty-title">开始分析</h3>
      <p class="empty-desc">点击"配置参数"按钮设置聚类参数,然后开始分析</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p class="loading-text">正在分析中,请稍候...</p>
      <p class="loading-hint">这可能需要几分钟时间</p>
    </div>

    <!-- 分析结果 -->
    <div v-if="!loading && result" class="result-container">

      <!-- 散点图和话题列表(左右布局) -->
      <div class="visualization-container">
        <!-- 左侧散点图 -->
        <div class="chart-section">
          <h3 class="section-title">聚类可视化</h3>
          <ClusterScatterChart :data="result.scatterData" height="600px" />
        </div>

        <!-- 右侧话题列表 -->
        <div class="topics-section">
          <div class="section-header">
            <h3 class="section-title">话题分析结果</h3>
            <button class="btn-add-topics" @click="handleBatchAddTopics" :disabled="batchAdding">
              <svg v-if="!batchAdding" class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
              </svg>
              <span v-if="batchAdding" class="loading-spinner-small"></span>
              {{ batchAdding ? '添加中...' : '批量增加热门话题' }}
            </button>
          </div>
          <ClusterTopicList :topics="result.clusters" />
        </div>
      </div>
    </div>

    <!-- 参数配置弹窗 -->
    <ClusterParamDialog
      v-model:visible="showParamDialog"
      :initial-data="params"
      @confirm="handleStartAnalysis"
    />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { analyzeCluster, type ClusterAnalysisRequest, type ClusterAnalysisResponse } from '@/api/clustering'
import { batchCreateHotTopics, type BatchCreateHotTopicRequest } from '@/api/hotTopic'
import ClusterScatterChart from '@/components/clustering/ClusterScatterChart.vue'
import ClusterTopicList from '@/components/clustering/ClusterTopicList.vue'
import ClusterParamDialog from '@/components/clustering/ClusterParamDialog.vue'

const loading = ref(false)
const batchAdding = ref(false)
const result = ref<ClusterAnalysisResponse | null>(null)
const showParamDialog = ref(false)

const params = ref<ClusterAnalysisRequest>({
  searchSpaceId: 7, // 默认搜索空间ID
  timeRange: '30d',
  eps: 0.4,
  minSamples: 3,
  metric: 'cosine'
})

const timeRangeOptions = [
  { value: '7d', label: '最近 7 天' },
  { value: '30d', label: '最近 30 天' },
  { value: '90d', label: '最近 90 天' }
]

// 开始分析
const handleStartAnalysis = async (newParams: ClusterAnalysisRequest) => {
  params.value = { ...newParams }
  loading.value = true
  result.value = null

  try {
    const response = await analyzeCluster(params.value)
    console.log('API完整响应:', response)

    // analyzeCluster 已返回解包后的数据(ClusterAnalysisResponse)
    if (response && response.clusters) {
      result.value = response
      console.log('聚类分析完成!', result.value)
    } else {
      console.error('响应格式错误:', response)
      alert('聚类分析失败: 响应数据格式错误')
    }
  } catch (error: any) {
    console.error('聚类分析失败:', error)
    alert('聚类分析失败: ' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 批量增加热门话题
const handleBatchAddTopics = async () => {
  if (!result.value || !result.value.clusters) {
    alert('没有聚类结果')
    return
  }

  batchAdding.value = true

  try {
    const request: BatchCreateHotTopicRequest = {
      topics: result.value.clusters.map(cluster => ({
        name: cluster.topic,
        popularity: cluster.size,
        visible: true
      })),
      skipExisting: true
    }

    const response = await batchCreateHotTopics(request)
    console.log('批量创建响应:', response)

    const data = response.data
    const message = `批量添加完成!\n成功: ${data.successCount} 个\n跳过(已存在): ${data.skippedCount} 个\n失败: ${data.failedCount} 个`

    if (data.failedCount > 0) {
      alert(message + '\n\n失败的话题:\n' + data.failedTopics.join('\n'))
    } else {
      alert(message)
    }
  } catch (error: any) {
    console.error('批量添加热门话题失败:', error)
    alert('批量添加失败: ' + (error.message || '网络错误'))
  } finally {
    batchAdding.value = false
  }
}
</script>

<style scoped>
.cluster-analysis-page {
  padding: 24px;
  min-height: 100vh;
  background: #fafafa;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.control-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.time-range-selector {
  flex: 1;
}

.control-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-bottom: 12px;
}

.time-range-buttons {
  display: flex;
  gap: 12px;
}

.time-range-btn {
  padding: 8px 24px;
  border: 2px solid #e8e8e8;
  background: white;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.time-range-btn:hover {
  border-color: #52c41a;
  color: #52c41a;
}

.time-range-btn.active {
  background: #f6ffed;
  border-color: #52c41a;
  color: #52c41a;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.btn-primary,
.btn-secondary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary {
  background: #52c41a;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #73d13d;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
}

.btn-primary:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
}

.btn-secondary:hover {
  background: #e8e8e8;
}

.btn-primary .icon,
.btn-secondary .icon {
  width: 18px;
  height: 18px;
}

.loading-spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-icon {
  width: 120px;
  height: 120px;
  margin-bottom: 24px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f0f0f0;
  border-top-color: #52c41a;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 24px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 8px 0;
}

.loading-hint {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.result-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-label {
  font-size: 13px;
  color: #888;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.stat-value.highlight {
  color: #52c41a;
}

.visualization-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: start;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.btn-add-topics {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #52c41a;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-add-topics:hover:not(:disabled) {
  background: #73d13d;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
}

.btn-add-topics:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
  transform: none;
}

.btn-add-topics .icon {
  width: 18px;
  height: 18px;
}

.chart-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.topics-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-height: 680px;
  overflow-y: auto;
}

@media (max-width: 1200px) {
  .visualization-container {
    grid-template-columns: 1fr;
  }
}
</style>
