<template>
  <div class="simulator-test-page">
    <div class="container mx-auto px-4 py-8">
      <h1 class="text-3xl font-bold text-center mb-8 text-primary">
        iPhone 设备模拟器测试
      </h1>

      <!-- 控制面板 -->
      <div class="controls bg-card border border-border rounded-lg p-6 mb-8">
        <h2 class="text-xl font-semibold mb-4">控制选项</h2>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- 缩放控制 -->
          <div>
            <label class="block text-sm font-medium mb-2">缩放比例: {{ scale }}</label>
            <input
              v-model.number="scale"
              type="range"
              min="0.3"
              max="1.5"
              step="0.1"
              class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            />
            <div class="flex justify-between text-xs text-muted-foreground mt-1">
              <span>0.3x</span>
              <span>1.5x</span>
            </div>
          </div>

          <!-- 设备颜色 -->
          <div>
            <label class="block text-sm font-medium mb-2">设备颜色</label>
            <select
              v-model="deviceColor"
              class="w-full px-3 py-2 border border-input rounded-md bg-background"
            >
              <option value="black">深空灰</option>
              <option value="white">银色</option>
              <option value="gold">金色</option>
              <option value="silver">石墨色</option>
            </select>
          </div>

          <!-- 状态栏开关 -->
          <div class="flex items-center space-x-2">
            <input
              id="statusbar-toggle"
              v-model="showStatusBar"
              type="checkbox"
              class="rounded border-gray-300 text-primary focus:ring-primary"
            />
            <label for="statusbar-toggle" class="text-sm font-medium">
              显示状态栏
            </label>
          </div>
        </div>
      </div>

      <!-- 示例展示区域 -->
      <div class="examples-section">
        <h2 class="text-xl font-semibold mb-6">示例展示</h2>

        <div class="grid grid-cols-1 xl:grid-cols-2 gap-12">
          <!-- 默认主屏幕 -->
          <div class="example-item">
            <h3 class="text-lg font-medium mb-4 text-center">默认 iOS 主屏幕</h3>
            <div class="flex justify-center">
              <PhoneSimulator
                :scale="scale"
                :device-color="deviceColor"
                :show-status-bar="showStatusBar"
              />
            </div>
          </div>

          <!-- 真实移动搜索界面 -->
          <div class="example-item">
            <h3 class="text-lg font-medium mb-4 text-center">真实移动搜索界面</h3>
            <div class="search-config-panel bg-white p-4 rounded-lg border mb-4">
              <h4 class="font-medium mb-3">搜索配置</h4>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium mb-1">搜索空间ID</label>
                  <input
                    v-model="searchSpaceId"
                    type="text"
                    class="w-full px-2 py-1 border rounded text-sm"
                    placeholder="输入搜索空间ID"
                  />
                </div>
                <div>
                  <label class="block text-sm font-medium mb-1">初始查询</label>
                  <input
                    v-model="initialQuery"
                    type="text"
                    class="w-full px-2 py-1 border rounded text-sm"
                    placeholder="可选的初始搜索词"
                  />
                </div>
              </div>
            </div>
            <div class="flex justify-center">
              <PhoneSimulator
                :scale="scale"
                :device-color="deviceColor"
                :show-status-bar="showStatusBar"
              >
                <MobileSearchApp
                  :search-space-id="searchSpaceId"
                  :initial-query="initialQuery"
                  :enable-history="true"
                  :page-size="10"
                  @item-click="handleSearchItemClick"
                  @search="handleSearchEvent"
                />
              </PhoneSimulator>
            </div>
          </div>
        </div>
      </div>

      <!-- 技术说明 -->
      <div class="tech-specs mt-12 bg-muted/50 rounded-lg p-6">
        <h2 class="text-xl font-semibold mb-4">技术规格</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 class="font-medium mb-2">设备规格</h3>
            <ul class="text-sm text-muted-foreground space-y-1">
              <li>• 屏幕尺寸：375 × 812px (iPhone 12 Pro)</li>
              <li>• 设备外框：390 × 844px</li>
              <li>• 状态栏高度：44px</li>
              <li>• 设备边框半径：40px</li>
              <li>• 屏幕边框半径：32px</li>
            </ul>
          </div>
          <div>
            <h3 class="font-medium mb-2">组件特性</h3>
            <ul class="text-sm text-muted-foreground space-y-1">
              <li>• 支持自定义内容插槽</li>
              <li>• 可调节设备颜色和缩放</li>
              <li>• 真实的设备外观和阴影效果</li>
              <li>• 响应式设计，支持不同屏幕</li>
              <li>• 高性能的CSS3渲染</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { PhoneSimulator } from '@/components/mobile'
import MobileSearchApp from '@/components/mobile/MobileSearchApp.vue'
import type { SearchResult } from '@/components/mobile/MobileSearchApp.vue'

// 响应式状态
const scale = ref(0.8)
const deviceColor = ref<'black' | 'white' | 'gold' | 'silver'>('black')
const showStatusBar = ref(true)

// 搜索配置
const searchSpaceId = ref('1') // 默认搜索空间ID
const initialQuery = ref('')

// 事件处理
const handleSearchItemClick = (result: SearchResult) => {
  console.log('搜索项被点击:', result)
  // 可以在这里处理结果项点击事件，比如打开详情页
}

const handleSearchEvent = (query: string, results: SearchResult[]) => {
  console.log('搜索完成:', { query, resultCount: results.length })
  // 可以在这里处理搜索事件，比如记录用户行为
}
</script>

<style scoped>
.simulator-test-page {
  min-height: 100vh;
  background: var(--background);
}

/* 搜索配置面板样式 */
.search-config-panel {
  font-size: 14px;
}

.search-config-panel input {
  border-color: #d1d5db;
}

.search-config-panel input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.1);
}
</style>