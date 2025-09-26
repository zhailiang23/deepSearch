/**
 * 移动端搜索演示 Composable 集成测试
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { useMobileSearchDemo } from '../useMobileSearchDemo'
import type { SearchApiAdapter, SearchDemoConfig } from '@/types/demo'

// 模拟API适配器
const createMockApiAdapter = (): SearchApiAdapter => ({
  search: vi.fn().mockResolvedValue({
    results: [
      {
        id: '1',
        title: '测试结果1',
        summary: '这是一个测试搜索结果',
        source: { content: 'test content 1' },
        index: 'test-index',
        score: 0.95
      },
      {
        id: '2',
        title: '测试结果2',
        summary: '这是另一个测试搜索结果',
        source: { content: 'test content 2' },
        index: 'test-index',
        score: 0.85
      }
    ],
    total: 2,
    duration: 125,
    hasMore: false,
    page: 0,
    size: 20
  }),
  suggest: vi.fn().mockResolvedValue(['建议1', '建议2', '建议3']),
  getSpaces: vi.fn().mockResolvedValue([
    {
      id: 'space1',
      name: '测试空间1',
      description: '这是第一个测试搜索空间',
      fields: ['title', 'content'],
      enabled: true,
      indexCount: 1,
      indexStatus: 'healthy' as const,
      docCount: 100
    },
    {
      id: 'space2',
      name: '测试空间2',
      description: '这是第二个测试搜索空间',
      fields: ['title', 'description'],
      enabled: true,
      indexCount: 1,
      indexStatus: 'healthy' as const,
      docCount: 50
    }
  ]),
  validateConfig: vi.fn().mockResolvedValue({ valid: true, errors: [], warnings: [] }),
  cancelSearch: vi.fn().mockResolvedValue(undefined)
})

describe('useMobileSearchDemo', () => {
  let mockApiAdapter: SearchApiAdapter

  beforeEach(() => {
    // 设置 Pinia
    setActivePinia(createPinia())

    // 创建模拟API适配器
    mockApiAdapter = createMockApiAdapter()

    // 模拟localStorage
    const localStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    }
    global.localStorage = localStorageMock as any

    // 模拟sessionStorage
    const sessionStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
      length: 0,
      key: vi.fn()
    }
    global.sessionStorage = sessionStorageMock as any

    // 模拟 performance API
    global.performance = {
      now: vi.fn(() => Date.now())
    } as any
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('应该正确初始化状态', () => {
    const { config, searchState, results, hasResults, isSearching } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    // 检查初始状态
    expect(config.value.searchSpaces.selected).toEqual([])
    expect(searchState.value.loading).toBe(false)
    expect(searchState.value.error).toBeNull()
    expect(results.value).toEqual([])
    expect(hasResults.value).toBe(false)
    expect(isSearching.value).toBe(false)
  })

  it('应该能够更新配置', () => {
    const { config, updateConfig } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    const updates: Partial<SearchDemoConfig> = {
      pagination: {
        pageSize: 50,
        loadMode: 'infinite',
        initialLoad: 20,
        prefetchNext: true
      }
    }

    updateConfig(updates)

    expect(config.value.pagination.pageSize).toBe(50)
    expect(config.value.pagination.loadMode).toBe('infinite')
  })

  it('应该能够执行搜索并处理结果', async () => {
    const {
      search,
      results,
      hasResults,
      searchState,
      performance
    } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter,
      enableCache: true,
      enableHistory: true
    })

    // 执行搜索
    await search('测试查询')

    // 验证API调用
    expect(mockApiAdapter.search).toHaveBeenCalledWith(
      '测试查询',
      expect.any(Object),
      expect.objectContaining({
        page: 0,
        signal: expect.any(AbortSignal)
      })
    )

    // 验证搜索结果
    expect(results.value).toHaveLength(2)
    expect(results.value[0].title).toBe('测试结果1')
    expect(hasResults.value).toBe(true)
    expect(searchState.value.loading).toBe(false)
    expect(searchState.value.total).toBe(2)

    // 验证性能指标更新
    expect(performance.value.searchCount).toBeGreaterThan(0)
  })

  it('应该能够处理搜索错误', async () => {
    const errorApiAdapter = {
      ...mockApiAdapter,
      search: vi.fn().mockRejectedValue(new Error('搜索失败'))
    }

    const { search, searchState, hasError } = useMobileSearchDemo({
      apiAdapter: errorApiAdapter
    })

    await search('测试查询')

    expect(searchState.value.error).toContain('搜索失败')
    expect(hasError.value).toBe(true)
    expect(searchState.value.loading).toBe(false)
  })

  it('应该能够取消搜索', async () => {
    const { search, cancelSearch, searchState } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    // 开始搜索
    const searchPromise = search('测试查询')

    // 立即取消
    cancelSearch()

    await searchPromise

    expect(searchState.value.loading).toBe(false)
  })

  it('应该正确管理搜索历史', async () => {
    const {
      search,
      searchHistory,
      recentQueries,
      addToHistory,
      clearHistory
    } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter,
      enableHistory: true
    })

    // 执行几次搜索
    await search('查询1')
    await search('查询2')
    await search('查询3')

    // 验证历史记录
    expect(searchHistory.value.length).toBeGreaterThan(0)
    expect(recentQueries.value).toContain('查询1')
    expect(recentQueries.value).toContain('查询2')
    expect(recentQueries.value).toContain('查询3')

    // 手动添加历史记录
    addToHistory('手动查询', 5, 200)
    expect(searchHistory.value.some(item => item.query === '手动查询')).toBe(true)

    // 清空历史
    clearHistory()
    expect(searchHistory.value).toEqual([])
  })

  it('应该正确管理缓存', async () => {
    const {
      search,
      cacheStats,
      clearCache
    } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter,
      enableCache: true
    })

    // 第一次搜索
    await search('缓存测试')

    // 第二次搜索相同查询（应该使用缓存）
    await search('缓存测试')

    // 验证缓存统计
    expect(cacheStats.value.size).toBeGreaterThan(0)

    // 清空缓存
    clearCache()
    expect(cacheStats.value.size).toBe(0)
  })

  it('应该能够生成搜索建议', () => {
    const { generateSuggestions } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    // 模拟一些历史查询
    const suggestions = generateSuggestions('测试')

    expect(Array.isArray(suggestions)).toBe(true)
  })

  it('应该能够高亮搜索词', () => {
    const { highlightQuery } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    const text = '这是一个测试文本'
    const highlighted = highlightQuery(text, '测试')

    expect(highlighted).toContain('<mark>')
    expect(highlighted).toContain('测试')
  })

  it('应该能够格式化搜索时长', () => {
    const { formatDuration } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    expect(formatDuration(500)).toBe('500ms')
    expect(formatDuration(1500)).toBe('1.5s')
  })

  it('应该能够导出和导入搜索历史', () => {
    const { exportHistory, importHistory, addToHistory } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter,
      enableHistory: true
    })

    // 添加一些历史记录
    addToHistory('导出测试1', 10, 100)
    addToHistory('导出测试2', 20, 200)

    // 导出历史
    const exported = exportHistory()
    expect(typeof exported).toBe('string')

    // 清空并导入
    const success = importHistory(exported)
    expect(success).toBe(true)
  })

  it('应该能够初始化并加载搜索空间', async () => {
    const { initialize, availableSpaces } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    await initialize()

    expect(mockApiAdapter.getSpaces).toHaveBeenCalled()
    expect(availableSpaces.value).toHaveLength(2)
    expect(availableSpaces.value[0].name).toBe('测试空间1')
  })

  it('应该能够重置所有状态', () => {
    const {
      resetAll,
      config,
      results,
      searchState,
      searchHistory,
      cacheStats
    } = useMobileSearchDemo({
      apiAdapter: mockApiAdapter
    })

    resetAll()

    expect(results.value).toEqual([])
    expect(searchState.value.loading).toBe(false)
    expect(searchState.value.error).toBeNull()
    expect(searchHistory.value).toEqual([])
    expect(cacheStats.value.size).toBe(0)
  })

  it('应该能够处理加载更多', async () => {
    const multiPageApiAdapter = {
      ...mockApiAdapter,
      search: vi.fn().mockImplementation((query, config, options) => {
        const page = options?.page || 0
        return Promise.resolve({
          results: [
            {
              id: `${page}-1`,
              title: `第${page + 1}页结果1`,
              summary: '测试结果',
              source: { content: 'test' },
              index: 'test-index',
              score: 0.9
            }
          ],
          total: 10,
          duration: 100,
          hasMore: page < 2,
          page,
          size: 5
        })
      })
    }

    const { search, loadMore, results, canLoadMore } = useMobileSearchDemo({
      apiAdapter: multiPageApiAdapter
    })

    // 首次搜索
    await search('分页测试')
    expect(results.value).toHaveLength(1)
    expect(canLoadMore.value).toBe(true)

    // 加载更多
    await loadMore()
    expect(results.value).toHaveLength(2) // 应该追加结果
    expect(results.value[1].title).toBe('第2页结果1')
  })
})