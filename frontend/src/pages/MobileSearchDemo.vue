<template>
  <div class="mobile-search-demo-container">
    
    <!-- 主要内容区域 - 双栏布局 -->
    <div class="main-content bg-white rounded-lg shadow-sm border border-gray-200 p-6 w-full">
      <!-- 使用 Flexbox 横向布局，让手机占用固定宽度，参数区域占用剩余空间 -->
      <div class="flex flex-col md:flex-row gap-6">
        <!-- 手机模拟器区域 - 固定宽度 -->
        <div class="flex-shrink-0">
          <MobileSearchInterface
            :config="searchConfig"
            :real-time-sync="true"
          />
        </div>

        <!-- 参数控制区域 - 占用剩余空间，固定高度 -->
        <div class="flex-1 h-[814px] overflow-hidden">
          <ParameterPanel
            v-model="searchConfig"
            :show-presets="true"
            :allow-reset="true"
            :sync-status="syncStatus"
            @parameter-change="handleParameterChange"
            @sync-status="handleSyncStatusChange"
          />
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, watch } from 'vue'
import ParameterPanel from '@/components/demo/ParameterPanel.vue'
import MobileSearchInterface from '@/components/mobile/MobileSearchInterface.vue'
import { useBreakpoints } from '@/composables/useBreakpoints'
import { useMobileSearchDemoStore } from '@/stores/mobileSearchDemo'
import { useParameterSync } from '@/composables/useParameterSync'
import type { ParameterChangeEvent, SyncStatus } from '@/types/demo'

// 页面元信息
defineOptions({
  name: 'MobileSearchDemo'
})

// 使用Store
const store = useMobileSearchDemoStore()

// 使用参数同步
const paramSync = useParameterSync({
  enableUrlSync: true,
  enableRealtimeSync: true,
  syncDebounceMs: 300,
  defaultConflictResolution: 'usePanel',
  enableChangeHistory: true
})

// 响应式断点检测
const breakpoints = useBreakpoints()
const isMobile = ref(breakpoints.isMobile)

// 从store获取状态
const searchConfig = computed(() => store.config)
const syncStatus = computed(() => paramSync.syncStatus.value)

// 监听配置变化并同步到移动端
watch(searchConfig, (newConfig, oldConfig) => {
  if (oldConfig && JSON.stringify(newConfig) !== JSON.stringify(oldConfig)) {
    console.log('配置变更，同步到移动端:', newConfig)
    console.log('拼音配置状态:', newConfig.pinyinSearch)
    console.log('高级设置状态:', newConfig.searchBehavior, newConfig.resultDisplay)
    paramSync.syncToMobile(newConfig)
  }
}, { deep: true })

// 事件处理
const handleParameterChange = (event: ParameterChangeEvent) => {
  console.log('参数变更:', event)
  console.log('当前 store.config:', store.config)

  // 记录变更到同步系统 (变更历史记录)
  // paramSync 系统会自动记录变更历史

  // 实时应用变更
  if (event.type === 'searchSpace') {
    console.log('更新搜索空间:', event.value)
    store.selectSearchSpaces(event.value)
  } else if (event.type === 'channel') {
    console.log('更新渠道:', event.value)
    store.updateConfig({ channel: event.value })
  } else if (event.type === 'pinyin') {
    console.log('更新拼音配置:', event.value)
    store.updateConfig({ pinyinSearch: event.value })
  } else if (event.type === 'semantic') {
    console.log('更新语义搜索配置:', event.value)
    store.updateConfig({ semanticSearch: event.value })
  } else if (event.type === 'rerank') {
    console.log('更新语义重排序配置:', event.value)
    store.updateConfig({ rerank: event.value })
  } else if (event.type === 'pagination') {
    console.log('更新分页配置:', event.value)
    store.updateConfig({ pagination: event.value })
  } else if (event.type === 'behavior') {
    console.log('更新行为配置:', event.value)
    store.updateConfig({ searchBehavior: event.value })
  } else if (event.type === 'display') {
    console.log('更新显示配置:', event.value)
    store.updateConfig({ resultDisplay: event.value })
  }

  console.log('更新后 store.config:', store.config)
}

const handleSyncStatusChange = (status: SyncStatus) => {
  console.log('同步状态变更:', status)
  // 可以在这里添加同步状态的UI反馈
}

// 页面初始化
onMounted(async () => {
  console.log('MobileSearchDemo 页面已加载')
  console.log('当前设备类型:', isMobile.value ? '移动端' : '桌面端')
  console.log('窗口尺寸:', window.innerWidth, 'x', window.innerHeight)
  console.log('断点状态:', breakpoints)
  console.log('初始 searchConfig:', searchConfig.value)

  // 搜索空间数据现在由 SearchSpaceSelector 组件直接从后端加载
  // 不再需要在这里设置模拟数据

  // 尝试从URL恢复配置
  try {
    const urlConfig = paramSync.syncFromUrl()
    if (urlConfig) {
      console.log('从URL恢复配置:', urlConfig)
      store.updateConfig(urlConfig)
    }
    // 搜索空间的初始选择现在由 SearchSpaceSelector 组件处理
    // 在用户通过组件选择搜索空间后，会自动同步到 store
  } catch (error) {
    console.warn('URL配置恢复失败:', error)
    // 使用默认配置，搜索空间选择由组件处理
  }

  console.log('初始化完成，当前配置:', store.config)
})
</script>

<style scoped>
.mobile-search-demo-container {
  min-height: 100vh;
  padding: 1.5rem;
}

.main-content {
  min-height: 500px;
}
</style>