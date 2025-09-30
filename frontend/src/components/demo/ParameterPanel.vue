<template>
  <div class="w-80 h-full border border-border bg-background p-4 overflow-y-auto">
    <!-- 面板标题 -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-foreground">参数设置</h3>
    </div>

    <!-- 配置内容 -->
    <div class="space-y-6">
      <!-- 搜索空间选择 -->
      <Collapsible v-model:open="sections.searchSpaces" class="border rounded-lg">
        <CollapsibleTrigger class="flex w-full items-center justify-between p-3 font-medium hover:bg-muted/50">
          <span class="text-emerald-700">搜索空间选择</span>
          <ChevronDown class="h-4 w-4 transition-transform duration-200"
                      :class="{ 'rotate-180': sections.searchSpaces }" />
        </CollapsibleTrigger>
        <CollapsibleContent class="px-3 pb-3">
          <SearchSpaceSelector
            v-model="localConfig.searchSpaces.selected"
            :multiple="localConfig.searchSpaces.allowMultiple"
            show-details
            @loading="handleSearchSpaceLoading"
            @error="handleSearchSpaceError"
          />
        </CollapsibleContent>
      </Collapsible>

      <!-- 渠道选择 -->
      <Collapsible v-model:open="sections.channel" class="border rounded-lg">
        <CollapsibleTrigger class="flex w-full items-center justify-between p-3 font-medium hover:bg-muted/50">
          <span class="text-emerald-700">渠道选择</span>
          <ChevronDown class="h-4 w-4 transition-transform duration-200"
                      :class="{ 'rotate-180': sections.channel }" />
        </CollapsibleTrigger>
        <CollapsibleContent class="px-3 pb-3">
          <div class="space-y-2">
            <Label class="text-sm">选择渠道</Label>
            <select
              v-model="localConfig.channel"
              class="w-full px-3 py-2 border border-input rounded-md bg-background text-sm"
              :disabled="loadingChannels"
            >
              <option value="">全部渠道</option>
              <option
                v-for="channel in channels"
                :key="channel.id"
                :value="channel.code"
              >
                {{ channel.name }}
              </option>
            </select>
            <p class="text-xs text-muted-foreground">
              选择特定渠道将只显示该渠道可访问的文档
            </p>
          </div>
        </CollapsibleContent>
      </Collapsible>

      <!-- 拼音搜索配置 -->
      <Collapsible v-model:open="sections.pinyinSearch" class="border rounded-lg">
        <CollapsibleTrigger class="flex w-full items-center justify-between p-3 font-medium hover:bg-muted/50">
          <span class="text-emerald-700">拼音搜索配置</span>
          <ChevronDown class="h-4 w-4 transition-transform duration-200"
                      :class="{ 'rotate-180': sections.pinyinSearch }" />
        </CollapsibleTrigger>
        <CollapsibleContent class="px-3 pb-3">
          <PinyinSearchConfig
            v-model="localConfig.pinyinSearch"
          />
        </CollapsibleContent>
      </Collapsible>

      <!-- 语义搜索配置 -->
      <Collapsible v-model:open="sections.semanticSearch" class="border rounded-lg">
        <CollapsibleTrigger class="flex w-full items-center justify-between p-3 font-medium hover:bg-muted/50">
          <span class="text-emerald-700">语义搜索配置</span>
          <ChevronDown class="h-4 w-4 transition-transform duration-200"
                      :class="{ 'rotate-180': sections.semanticSearch }" />
        </CollapsibleTrigger>
        <CollapsibleContent class="px-3 pb-3">
          <SemanticSearchConfig
            v-model="localConfig.semanticSearch"
          />
        </CollapsibleContent>
      </Collapsible>

      <!-- 分页配置 -->
      <Collapsible v-model:open="sections.pagination" class="border rounded-lg">
        <CollapsibleTrigger class="flex w-full items-center justify-between p-3 font-medium hover:bg-muted/50">
          <span class="text-emerald-700">分页设置</span>
          <ChevronDown class="h-4 w-4 transition-transform duration-200"
                      :class="{ 'rotate-180': sections.pagination }" />
        </CollapsibleTrigger>
        <CollapsibleContent class="px-3 pb-3">
          <PagingConfig
            v-model="localConfig.pagination"
            :max-page-size="100"
            :min-page-size="5"
          />
        </CollapsibleContent>
      </Collapsible>

      <!-- 高级设置 -->
      <Collapsible v-model:open="sections.advanced" class="border rounded-lg">
        <CollapsibleTrigger class="flex w-full items-center justify-between p-3 font-medium hover:bg-muted/50">
          <span class="text-emerald-700">高级设置</span>
          <ChevronDown class="h-4 w-4 transition-transform duration-200"
                      :class="{ 'rotate-180': sections.advanced }" />
        </CollapsibleTrigger>
        <CollapsibleContent class="px-3 pb-3 space-y-4">
          <!-- 搜索行为配置 -->
          <div class="space-y-3">
            <h4 class="text-sm font-medium text-muted-foreground">搜索行为</h4>

            <div class="grid grid-cols-2 gap-3">
              <div class="space-y-1">
                <Label class="text-xs">防抖延迟(ms)</Label>
                <Input
                  v-model.number="localConfig.searchBehavior.debounceMs"
                  type="number"
                  min="0"
                  max="2000"
                  step="50"
                  class="h-8 text-xs"
                />
              </div>
              <div class="space-y-1">
                <Label class="text-xs">最小查询长度</Label>
                <Input
                  v-model.number="localConfig.searchBehavior.minQueryLength"
                  type="number"
                  min="1"
                  max="10"
                  class="h-8 text-xs"
                />
              </div>
            </div>

            <div class="flex items-center justify-between">
              <Label class="text-xs">高亮匹配</Label>
              <Switch v-model:checked="localConfig.searchBehavior.highlightMatch" />
            </div>
          </div>

          <!-- 结果显示配置 -->
          <div class="space-y-3">
            <h4 class="text-sm font-medium text-muted-foreground">结果显示</h4>

            <div class="space-y-1">
              <Label class="text-xs">内容最大长度</Label>
              <Input
                v-model.number="localConfig.resultDisplay.maxContentLength"
                type="number"
                min="50"
                max="1000"
                step="50"
                class="h-8 text-xs"
              />
            </div>

            <div class="flex items-center justify-between">
              <Label class="text-xs">显示相关性分数</Label>
              <Switch v-model:checked="localConfig.resultDisplay.showScore" />
            </div>

            <div class="flex items-center justify-between">
              <Label class="text-xs">按类型分组</Label>
              <Switch v-model:checked="localConfig.resultDisplay.groupByType" />
            </div>
          </div>
        </CollapsibleContent>
      </Collapsible>

    </div>

    <!-- 同步状态指示 -->
    <div v-if="syncStatus.pending" class="mt-4 p-3 bg-muted rounded-lg">
      <div class="flex items-center gap-2 text-sm text-muted-foreground">
        <div class="animate-spin w-4 h-4 border-2 border-emerald-500 border-t-transparent rounded-full"></div>
        <span>同步参数中...</span>
      </div>
    </div>

    <div v-if="syncStatus.error" class="mt-4 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
      <div class="text-sm text-destructive">
        同步失败: {{ syncStatus.error }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import { useVModel } from '@vueuse/core'

import Input from '@/components/ui/input/Input.vue'
import Label from '@/components/ui/label/Label.vue'
import { Switch } from '@/components/ui/switch'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'

import SearchSpaceSelector from './SearchSpaceSelector.vue'
import PinyinSearchConfig from './PinyinSearchConfig.vue'
import SemanticSearchConfig from './SemanticSearchConfig.vue'
import PagingConfig from './PagingConfig.vue'

import {
  DEFAULT_SEARCH_DEMO_CONFIG
} from '@/types/demo'

import type {
  ParameterPanelProps,
  SearchDemoConfig,
  SyncStatus,
  ParameterChangeEvent
} from '@/types/demo'

import { channelApi } from '@/services/channelApi'
import type { Channel } from '@/types/channel'

// Props & Emits
const props = withDefaults(defineProps<ParameterPanelProps>(), {
  collapsed: false,
  showPresets: true,
  allowReset: true
})

const emits = defineEmits<{
  'update:modelValue': [value: SearchDemoConfig]
  'parameter-change': [event: ParameterChangeEvent]
  'sync-status': [status: SyncStatus]
}>()

// Local state
const localConfig = useVModel(props, 'modelValue', emits, {
  passive: true,
  deep: true
})

const collapsed = ref(props.collapsed)

const sections = ref({
  searchSpaces: true,
  channel: false,
  pinyinSearch: false,
  semanticSearch: true,
  pagination: false,
  advanced: false
})

const syncStatus = ref<SyncStatus>({
  pending: false,
  lastSync: Date.now()
})

// 渠道列表
const channels = ref<Channel[]>([])
const loadingChannels = ref(false)

// 加载渠道列表
onMounted(async () => {
  loadingChannels.value = true
  try {
    // Spring Data JPA分页从0开始
    const response = await channelApi.list({ page: 0, size: 100 })
    channels.value = response.data.content
  } catch (error) {
    console.error('加载渠道列表失败:', error)
  } finally {
    loadingChannels.value = false
  }
})

// Computed
const canReset = computed(() => {
  return JSON.stringify(localConfig.value) !== JSON.stringify(DEFAULT_SEARCH_DEMO_CONFIG)
})

// Watchers for parameter changes
watch(
  () => localConfig.value,
  (newValue, oldValue) => {
    if (!oldValue) return

    // 检测具体变更并发出事件
    const changes = detectChanges(oldValue, newValue)
    changes.forEach(change => {
      emits('parameter-change', change)
    })

    // 模拟同步状态
    handleSync()
  },
  { deep: true }
)

// Methods
function detectChanges(oldConfig: SearchDemoConfig, newConfig: SearchDemoConfig): ParameterChangeEvent[] {
  const changes: ParameterChangeEvent[] = []
  const timestamp = Date.now()

  // 检查搜索空间变更
  if (JSON.stringify(oldConfig.searchSpaces) !== JSON.stringify(newConfig.searchSpaces)) {
    changes.push({
      type: 'searchSpace',
      key: 'selected',
      value: newConfig.searchSpaces.selected,
      previous: oldConfig.searchSpaces.selected,
      timestamp
    })
  }

  // 检查拼音搜索配置变更
  if (JSON.stringify(oldConfig.pinyinSearch) !== JSON.stringify(newConfig.pinyinSearch)) {
    changes.push({
      type: 'pinyin',
      key: 'config',
      value: newConfig.pinyinSearch,
      previous: oldConfig.pinyinSearch,
      timestamp
    })
  }

  // 检查语义搜索配置变更
  if (JSON.stringify(oldConfig.semanticSearch) !== JSON.stringify(newConfig.semanticSearch)) {
    changes.push({
      type: 'semantic',
      key: 'config',
      value: newConfig.semanticSearch,
      previous: oldConfig.semanticSearch,
      timestamp
    })
  }

  // 检查分页配置变更
  if (JSON.stringify(oldConfig.pagination) !== JSON.stringify(newConfig.pagination)) {
    changes.push({
      type: 'pagination',
      key: 'config',
      value: newConfig.pagination,
      previous: oldConfig.pagination,
      timestamp
    })
  }

  // 检查渠道变更
  if (oldConfig.channel !== newConfig.channel) {
    changes.push({
      type: 'channel',
      key: 'channel',
      value: newConfig.channel,
      previous: oldConfig.channel,
      timestamp
    })
  }

  return changes
}

async function handleSync() {
  syncStatus.value = { pending: true, lastSync: syncStatus.value.lastSync }
  emits('sync-status', syncStatus.value)

  try {
    // 模拟同步延迟
    await new Promise(resolve => setTimeout(resolve, 500))

    syncStatus.value = {
      pending: false,
      lastSync: Date.now()
    }
  } catch (error) {
    syncStatus.value = {
      pending: false,
      error: error instanceof Error ? error.message : '同步失败',
      lastSync: syncStatus.value.lastSync
    }
  }

  emits('sync-status', syncStatus.value)
}

function handleReset() {
  localConfig.value = { ...DEFAULT_SEARCH_DEMO_CONFIG }
}


// 搜索空间相关处理方法
function handleSearchSpaceLoading(loading: boolean) {
  console.log('SearchSpaceSelector 加载状态:', loading)
  // 可以在这里更新全局加载状态
}

function handleSearchSpaceError(error: string | null) {
  if (error) {
    console.error('SearchSpaceSelector 错误:', error)
    // 可以在这里显示错误提示
  }
}


</script>

<style scoped>
/* 确保面板固定宽度和淡绿色主题 */
.border-emerald-200 {
  border-color: rgb(167 243 208);
}

.text-emerald-700 {
  color: rgb(4 120 87);
}

.bg-emerald-50 {
  background-color: rgb(236 253 245);
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgb(167 243 208);
  border-radius: 2px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgb(110 231 183);
}
</style>