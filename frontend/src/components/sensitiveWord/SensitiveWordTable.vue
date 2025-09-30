<template>
  <div class="space-y-4">
    <!-- 统计卡片 -->
    <div v-if="statistics" class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
      <div class="bg-card rounded-lg border border-border p-4">
        <div class="text-sm text-muted-foreground">总计</div>
        <div class="text-2xl font-bold text-foreground">{{ statistics.totalWords }}</div>
      </div>
      <div class="bg-card rounded-lg border border-border p-4">
        <div class="text-sm text-muted-foreground">已启用</div>
        <div class="text-2xl font-bold text-green-600">{{ statistics.enabledWords }}</div>
      </div>
      <div class="bg-card rounded-lg border border-border p-4">
        <div class="text-sm text-muted-foreground">已禁用</div>
        <div class="text-2xl font-bold text-orange-600">{{ statistics.disabledWords }}</div>
      </div>
      <div class="bg-card rounded-lg border border-border p-4">
        <div class="text-sm text-muted-foreground">高危等级(4-5级)</div>
        <div class="text-2xl font-bold text-red-600">
          {{ (statistics.harmLevelDistribution[4] || 0) + (statistics.harmLevelDistribution[5] || 0) }}
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
      <div class="flex flex-col sm:flex-row gap-2 flex-1">
        <!-- 搜索框 -->
        <div class="relative flex-1 max-w-md">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索敏感词名称..."
            class="w-full px-4 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            @keyup.enter="handleSearch"
          />
        </div>

        <!-- 筛选器 -->
        <select
          v-model="filterEnabled"
          class="px-4 py-2 border border-border rounded-md bg-background text-foreground"
          @change="handleFilter"
        >
          <option :value="undefined">全部状态</option>
          <option :value="true">已启用</option>
          <option :value="false">已禁用</option>
        </select>

        <select
          v-model="filterHarmLevel"
          class="px-4 py-2 border border-border rounded-md bg-background text-foreground"
          @change="handleFilter"
        >
          <option :value="undefined">全部等级</option>
          <option :value="1">1级(低危)</option>
          <option :value="2">2级</option>
          <option :value="3">3级(中危)</option>
          <option :value="4">4级</option>
          <option :value="5">5级(高危)</option>
        </select>
      </div>

      <!-- 创建按钮 -->
      <button
        @click="handleCreate"
        class="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
      >
        <span class="flex items-center gap-2">
          <span>+</span>
          <span>创建敏感词</span>
        </span>
      </button>
    </div>

    <!-- 表格 -->
    <div class="bg-card rounded-lg border border-border overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-muted">
            <tr>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">ID</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">敏感词名称</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">危害等级</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">状态</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">创建时间</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">更新时间</th>
              <th class="px-4 py-3 text-right text-sm font-medium text-muted-foreground">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border">
            <tr
              v-for="word in sensitiveWords"
              :key="word.id"
              class="hover:bg-muted/50 transition-colors"
            >
              <td class="px-4 py-3 text-sm text-foreground">{{ word.id }}</td>
              <td class="px-4 py-3 text-sm font-medium text-foreground">{{ word.name }}</td>
              <td class="px-4 py-3 text-sm">
                <span
                  :class="getHarmLevelColor(word.harmLevel)"
                  class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium"
                >
                  {{ word.harmLevel }}级
                </span>
              </td>
              <td class="px-4 py-3 text-sm">
                <button
                  @click="handleToggleStatus(word)"
                  :class="word.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
                  class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium hover:opacity-80 transition-opacity"
                >
                  {{ word.enabled ? '已启用' : '已禁用' }}
                </button>
              </td>
              <td class="px-4 py-3 text-sm text-muted-foreground">
                {{ formatDate(word.createdAt) }}
              </td>
              <td class="px-4 py-3 text-sm text-muted-foreground">
                {{ formatDate(word.updatedAt) }}
              </td>
              <td class="px-4 py-3 text-sm text-right">
                <div class="flex items-center justify-end gap-2">
                  <button
                    @click="handleEdit(word)"
                    class="px-3 py-1 text-xs text-primary hover:text-primary/80 transition-colors"
                  >
                    编辑
                  </button>
                  <button
                    @click="handleDelete(word)"
                    class="px-3 py-1 text-xs text-red-600 hover:text-red-700 transition-colors"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="sensitiveWords.length === 0">
              <td colspan="7" class="px-4 py-8 text-center text-muted-foreground">
                暂无数据
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.totalPages > 1" class="px-4 py-3 border-t border-border">
        <div class="flex items-center justify-between">
          <div class="text-sm text-muted-foreground">
            共 {{ pagination.totalElements }} 条记录，第 {{ pagination.page + 1 }} / {{ pagination.totalPages }} 页
          </div>
          <div class="flex gap-2">
            <button
              @click="handlePageChange(pagination.page - 1)"
              :disabled="pagination.first"
              class="px-3 py-1 text-sm border border-border rounded hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
            >
              上一页
            </button>
            <button
              @click="handlePageChange(pagination.page + 1)"
              :disabled="pagination.last"
              class="px-3 py-1 text-sm border border-border rounded hover:bg-muted disabled:opacity-50 disabled:cursor-not-allowed"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useSensitiveWordStore } from '@/stores/sensitiveWord'
import type { SensitiveWord, SensitiveWordStatistics } from '@/types/sensitiveWord'

const props = defineProps<{
  statistics: SensitiveWordStatistics | null
}>()

const emit = defineEmits<{
  create: []
  edit: [word: SensitiveWord]
  delete: [word: SensitiveWord]
  view: [word: SensitiveWord]
}>()

const store = useSensitiveWordStore()

const searchKeyword = ref('')
const filterEnabled = ref<boolean | undefined>(undefined)
const filterHarmLevel = ref<number | undefined>(undefined)

const sensitiveWords = computed(() => store.sensitiveWords)
const pagination = computed(() => store.pagination)

const getHarmLevelColor = (level: number): string => {
  const colors: Record<number, string> = {
    1: 'bg-blue-100 text-blue-800',
    2: 'bg-green-100 text-green-800',
    3: 'bg-yellow-100 text-yellow-800',
    4: 'bg-orange-100 text-orange-800',
    5: 'bg-red-100 text-red-800'
  }
  return colors[level] || 'bg-gray-100 text-gray-800'
}

const formatDate = (date: string): string => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadData = async () => {
  await store.fetchSensitiveWords({
    page: store.queryParams.page,
    size: store.queryParams.size,
    keyword: searchKeyword.value || undefined,
    enabled: filterEnabled.value,
    harmLevel: filterHarmLevel.value
  })
}

const handleSearch = () => {
  // 重置到第一页
  store.updateQueryParams({ page: 0 })

  if (searchKeyword.value.trim()) {
    store.searchSensitiveWords(searchKeyword.value.trim(), {
      page: 0,
      size: store.queryParams.size,
      enabled: filterEnabled.value,
      harmLevel: filterHarmLevel.value
    })
  } else {
    loadData()
  }
}

const handleFilter = () => {
  // 重置到第一页
  store.updateQueryParams({ page: 0 })
  loadData()
}

const handlePageChange = (page: number) => {
  store.updateQueryParams({ page })
  loadData()
}

const handleCreate = () => {
  emit('create')
}

const handleEdit = (word: SensitiveWord) => {
  emit('edit', word)
}

const handleDelete = (word: SensitiveWord) => {
  emit('delete', word)
}

const handleToggleStatus = async (word: SensitiveWord) => {
  try {
    await store.toggleStatus(word.id)
  } catch (error) {
    console.error('切换状态失败:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>