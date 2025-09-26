<template>
  <div class="mobile-search-demo-container">
    <!-- 页面头部 -->
    <div class="mb-6">
      <h1 class="text-3xl font-bold text-gray-800 mb-2">移动搜索界面演示</h1>
      <p class="text-gray-600">基于 Elasticsearch 的移动端搜索界面演示工具</p>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content bg-white rounded-lg shadow-sm border border-gray-200 p-8">
      <div class="flex items-center justify-center h-96 text-gray-500">
        <div class="text-center">
          <div class="mb-4">
            <svg class="w-16 h-16 mx-auto text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
            </svg>
          </div>
          <h3 class="text-lg font-medium mb-2">演示界面准备中</h3>
          <p class="text-sm text-gray-400">双栏布局和移动端模拟器即将在这里显示</p>
        </div>
      </div>
    </div>

    <!-- 页面信息 -->
    <div class="mt-6 text-sm text-gray-500">
      <p>技术栈：Vue 3 + TypeScript + TailwindCSS + Elasticsearch</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

// 页面元信息
defineOptions({
  name: 'MobileSearchDemo'
})

// 页面初始化
onMounted(() => {
  console.log('MobileSearchDemo 页面已加载')
})
</script>

<style scoped>
.mobile-search-demo-container {
  @apply min-h-full p-6;
}

.main-content {
  min-height: 500px;
}
</style>