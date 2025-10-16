<template>
  <div class="space-y-4">
    
    <!-- Tab 导航 -->
    <div class="border-b border-gray-200">
      <nav class="-mb-px flex space-x-8">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          @click="activeTab = tab.id"
          class="whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors"
          :class="activeTab === tab.id
            ? 'border-emerald-500 text-emerald-600'
            : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'"
        >
          <div class="flex items-center gap-2">
            <component :is="tab.icon" class="w-5 h-5" />
            <span>{{ tab.label }}</span>
          </div>
        </button>
      </nav>
    </div>

    <!-- Tab 内容 -->
    <div class="mt-6">
      <PipelineDebugger v-if="activeTab === 'debugger'" />
      <PerformanceMonitor v-if="activeTab === 'performance'" />
      <ProcessorConfig v-if="activeTab === 'config'" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PipelineDebugger from '@/components/query-understanding/PipelineDebugger.vue'
import PerformanceMonitor from '@/components/query-understanding/PerformanceMonitor.vue'
import ProcessorConfig from '@/components/query-understanding/ProcessorConfig.vue'

// Tab 定义
const tabs = [
  {
    id: 'debugger',
    label: '查询测试',
    icon: 'svg'
  },
  {
    id: 'performance',
    label: '性能监控',
    icon: 'svg'
  },
  {
    id: 'config',
    label: '处理器配置',
    icon: 'svg'
  }
]

// 当前激活的 Tab
const activeTab = ref('debugger')
</script>

<style scoped>
/* Tab 过渡动画 */
.tab-content {
  animation: fadeIn 0.2s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
