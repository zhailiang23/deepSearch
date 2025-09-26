<template>
  <div class="status-bar flex justify-between items-center px-6 py-2 text-white text-sm font-medium">
    <!-- 左侧：时间 -->
    <div class="flex items-center">
      <span>{{ currentTime }}</span>
    </div>

    <!-- 右侧：信号、WiFi、电池 -->
    <div class="flex items-center space-x-1">
      <!-- 信号强度 -->
      <div class="flex items-center space-x-0.5">
        <div class="w-1 h-2 bg-white rounded-full opacity-60"></div>
        <div class="w-1 h-2.5 bg-white rounded-full opacity-80"></div>
        <div class="w-1 h-3 bg-white rounded-full"></div>
        <div class="w-1 h-3.5 bg-white rounded-full"></div>
      </div>

      <!-- WiFi图标 -->
      <div class="ml-1">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" class="text-white">
          <path d="M12 20h.01M2 12.82a9 9 0 0 1 20 0M5 17a5 5 0 0 1 10 0M8.5 19.5a2 2 0 0 1 3 0" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>

      <!-- 电池图标 -->
      <div class="ml-1 flex items-center">
        <div class="relative">
          <div class="w-6 h-3 border border-white rounded-sm bg-white">
            <div class="w-4 h-1.5 bg-primary rounded-sm m-0.5"></div>
          </div>
          <div class="absolute -right-0.5 top-1 w-0.5 h-1 bg-white rounded-r"></div>
        </div>
        <span class="ml-1 text-xs">100%</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

// 当前时间状态
const currentTime = ref('')

// 更新时间的函数
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

// 定时器
let timeInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  updateTime()
  // 每分钟更新一次时间
  timeInterval = setInterval(updateTime, 60000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.status-bar {
  height: 44px; /* iPhone状态栏标准高度 */
  background: linear-gradient(135deg, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0.6) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}
</style>