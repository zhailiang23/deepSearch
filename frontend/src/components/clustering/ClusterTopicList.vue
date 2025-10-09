<template>
  <div class="cluster-topic-list">
    <div class="topic-header">
      <h3 class="text-lg font-semibold text-gray-800">聚类话题分析</h3>
      <div class="text-sm text-gray-600">
        共 {{ topics.length }} 个话题 ({{ totalSize }} 条查询)
      </div>
    </div>

    <div class="topic-items">
      <div
        v-for="(topic, index) in topics"
        :key="topic.clusterId"
        class="topic-item"
        :class="{ expanded: expandedIds.has(topic.clusterId) }"
      >
        <div class="topic-summary" @click="toggleExpand(topic.clusterId)">
          <div class="flex items-center gap-3 flex-1">
            <div class="cluster-badge" :style="{ backgroundColor: getClusterColor(index) }">
              {{ topic.clusterId }}
            </div>
            <div class="flex-1">
              <div class="topic-name">{{ topic.topic }}</div>
              <div class="topic-meta">
                <span class="meta-item">{{ topic.size }} 条</span>
              </div>
            </div>
          </div>
          <svg
            class="expand-icon"
            :class="{ rotated: expandedIds.has(topic.clusterId) }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </div>

        <div v-if="expandedIds.has(topic.clusterId)" class="topic-details">
          <!-- 标签 -->
          <div v-if="topic.tags && topic.tags.length > 0" class="detail-section">
            <div class="section-title">业务标签</div>
            <div class="tag-list">
              <span
                v-for="tag in topic.tags"
                :key="tag"
                class="tag"
                :style="{ borderColor: getClusterColor(index), color: getClusterColor(index) }"
              >
                {{ tag }}
              </span>
            </div>
          </div>

          <!-- 代表性问题 -->
          <div v-if="topic.examples && topic.examples.length > 0" class="detail-section">
            <div class="section-title">代表性问题</div>
            <ul class="example-list">
              <li v-for="(example, idx) in topic.examples" :key="idx" class="example-item">
                {{ example }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ClusterTopic } from '@/api/clustering'

interface Props {
  topics: ClusterTopic[]
}

const props = defineProps<Props>()

const expandedIds = ref<Set<number>>(new Set())

// 计算总查询数
const totalSize = computed(() => {
  return props.topics.reduce((sum, topic) => sum + topic.size, 0)
})

// 切换展开状态
const toggleExpand = (clusterId: number) => {
  if (expandedIds.value.has(clusterId)) {
    expandedIds.value.delete(clusterId)
  } else {
    expandedIds.value.add(clusterId)
  }
}

// 获取簇颜色
const getClusterColor = (index: number): string => {
  const colors = [
    '#52c41a',
    '#73d13d',
    '#95de64',
    '#bae637',
    '#13c2c2',
    '#36cfc9',
    '#5cdbd3',
    '#87e8de',
    '#389e0d',
    '#237804'
  ]
  return colors[index % colors.length]
}
</script>

<style scoped>
.cluster-topic-list {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.topic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.topic-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-item {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.topic-item:hover {
  border-color: #95de64;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.1);
}

.topic-item.expanded {
  border-color: #52c41a;
}

.topic-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  cursor: pointer;
  user-select: none;
}

.cluster-badge {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.topic-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.topic-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #888;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.expand-icon {
  width: 20px;
  height: 20px;
  color: #999;
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

.topic-details {
  padding: 0 16px 16px;
  border-top: 1px solid #f0f0f0;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.detail-section {
  margin-top: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin-bottom: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 12px;
  border-radius: 12px;
  border: 1px solid;
  font-size: 12px;
  background: white;
  font-weight: 500;
}

.example-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.example-item {
  padding: 8px 12px;
  background: #f9f9f9;
  border-radius: 4px;
  font-size: 13px;
  color: #555;
  border-left: 3px solid #52c41a;
}
</style>
