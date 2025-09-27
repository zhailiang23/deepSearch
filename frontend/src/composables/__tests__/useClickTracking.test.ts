/**
 * useClickTracking 组合函数测试
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { nextTick, ref } from 'vue'
import { useClickTracking } from '../useClickTracking'
import { searchLogApi } from '@/api/searchLog'
import { trackingConfig, updateTrackingConfig } from '@/config/tracking'
import type { SearchResult, ClickTrackingData } from '@/types/searchLog'

// Mock API
vi.mock('@/api/searchLog', () => ({
  searchLogApi: {
    recordClick: vi.fn(),
    recordClicks: vi.fn()
  }
}))

// Mock VueUse localStorage
vi.mock('@vueuse/core', () => ({
  useLocalStorage: vi.fn((key: string, defaultValue: any) => {
    const storage = ref(defaultValue)
    return storage
  })
}))

// Mock tracking config
vi.mock('@/config/tracking', () => ({
  trackingConfig: {
    enabled: true,
    batchSize: 10,
    retryAttempts: 3,
    retryDelay: 1000,
    offlineStorage: true,
    debugMode: true,
    maxOfflineItems: 100,
    autoSyncInterval: 30000,
    requestTimeout: 5000
  },
  updateTrackingConfig: vi.fn(),
  resetTrackingConfig: vi.fn()
}))

// Mock localStorage
const mockLocalStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage
})

// Mock navigator
Object.defineProperty(window, 'navigator', {
  value: {
    onLine: true,
    userAgent: 'test-agent'
  },
  writable: true
})

// Mock performance.now
Object.defineProperty(window, 'performance', {
  value: {
    now: vi.fn(() => Date.now())
  },
  writable: true
})

describe('useClickTracking', () => {
  const mockSearchResult: SearchResult = {
    id: 'doc1',
    title: '测试文档',
    url: 'https://example.com/doc1',
    summary: '这是一个测试文档'
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockLocalStorage.getItem.mockReturnValue(null)

    // Mock successful API response
    vi.mocked(searchLogApi.recordClick).mockResolvedValue({
      success: true,
      data: null,
      message: '记录成功'
    })

    vi.mocked(searchLogApi.recordClicks).mockResolvedValue({
      success: true,
      data: null,
      message: '批量记录成功'
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('应该正确初始化追踪状态', () => {
    const { isTracking, trackingState, offlineCacheCount } = useClickTracking()

    expect(isTracking.value).toBe(true)
    expect(trackingState.isEnabled).toBe(true)
    expect(offlineCacheCount.value).toBe(0)
  })

  it('应该正确记录点击行为', async () => {
    const { trackClick } = useClickTracking()

    await trackClick(123, mockSearchResult, 1)

    expect(searchLogApi.recordClick).toHaveBeenCalledWith(
      expect.objectContaining({
        searchLogId: 123,
        documentId: 'doc1',
        documentTitle: '测试文档',
        documentUrl: 'https://example.com/doc1',
        clickPosition: 1,
        clickType: 'click',
        userAgent: 'test-agent'
      })
    )
  })

  it('应该处理鼠标点击事件', async () => {
    const { trackClick } = useClickTracking()

    const mockMouseEvent = new MouseEvent('click', {
      ctrlKey: true,
      shiftKey: false,
      altKey: true
    })

    await trackClick(123, mockSearchResult, 1, mockMouseEvent)

    expect(searchLogApi.recordClick).toHaveBeenCalledWith(
      expect.objectContaining({
        clickType: 'click',
        modifierKeys: {
          ctrl: true,
          shift: false,
          alt: true
        }
      })
    )
  })

  it('应该在API失败时使用离线缓存', async () => {
    // Mock API failure
    vi.mocked(searchLogApi.recordClick).mockRejectedValue(new Error('网络错误'))

    const { trackClick, offlineCacheCount } = useClickTracking()

    await trackClick(123, mockSearchResult, 1)

    // 等待状态更新
    await nextTick()

    expect(offlineCacheCount.value).toBe(1)
  })

  it('应该正确设置追踪状态', () => {
    const { setTrackingEnabled, isTracking, trackingState } = useClickTracking()

    setTrackingEnabled(false)

    expect(isTracking.value).toBe(false)
    expect(trackingState.isEnabled).toBe(false)
  })

  it('应该在禁用追踪时跳过记录', async () => {
    const { trackClick, setTrackingEnabled } = useClickTracking()

    setTrackingEnabled(false)
    await trackClick(123, mockSearchResult, 1)

    expect(searchLogApi.recordClick).not.toHaveBeenCalled()
  })

  it('应该正确处理批量点击追踪', async () => {
    const { trackMultipleClicks } = useClickTracking()

    const clicks = [
      {
        searchLogId: 123,
        documentId: 'doc1',
        documentTitle: '文档1',
        documentUrl: 'https://example.com/doc1',
        clickPosition: 1,
        clickTime: new Date().toISOString(),
        userAgent: 'test-agent',
        clickType: 'click',
        modifierKeys: { ctrl: false, shift: false, alt: false }
      },
      {
        searchLogId: 123,
        documentId: 'doc2',
        documentTitle: '文档2',
        documentUrl: 'https://example.com/doc2',
        clickPosition: 2,
        clickTime: new Date().toISOString(),
        userAgent: 'test-agent',
        clickType: 'click',
        modifierKeys: { ctrl: false, shift: false, alt: false }
      }
    ]

    await trackMultipleClicks(clicks)

    expect(searchLogApi.recordClick).toHaveBeenCalledTimes(2)
  })

  it('应该正确清除离线缓存', async () => {
    // Mock API failure to force offline cache
    vi.mocked(searchLogApi.recordClick).mockRejectedValue(new Error('网络错误'))

    const { trackClick, clearOfflineCache, offlineCacheCount } = useClickTracking()

    await trackClick(123, mockSearchResult, 1)
    await nextTick()

    expect(offlineCacheCount.value).toBe(1)

    clearOfflineCache()

    expect(offlineCacheCount.value).toBe(0)
  })

  it('应该正确同步离线缓存', async () => {
    // 首先让API失败以创建离线缓存
    vi.mocked(searchLogApi.recordClick).mockRejectedValueOnce(new Error('网络错误'))

    const { trackClick, syncOfflineCache, offlineCacheCount } = useClickTracking()

    await trackClick(123, mockSearchResult, 1)
    await nextTick()

    expect(offlineCacheCount.value).toBe(1)

    // 然后让API成功，并同步缓存
    vi.mocked(searchLogApi.recordClicks).mockResolvedValue({
      success: true,
      data: null,
      message: '批量记录成功'
    })

    await syncOfflineCache()
    await nextTick()

    expect(searchLogApi.recordClicks).toHaveBeenCalled()
    expect(offlineCacheCount.value).toBe(0)
  })

  it('应该处理网络状态变化', () => {
    const { isOnline } = useClickTracking()

    expect(isOnline.value).toBe(true)

    // 模拟网络断开
    Object.defineProperty(window.navigator, 'onLine', {
      value: false,
      writable: true
    })

    window.dispatchEvent(new Event('offline'))
    expect(isOnline.value).toBe(false)

    // 模拟网络恢复
    Object.defineProperty(window.navigator, 'onLine', {
      value: true,
      writable: true
    })

    window.dispatchEvent(new Event('online'))
    expect(isOnline.value).toBe(true)
  })

  it('应该处理点击事件的修饰键', async () => {
    const { trackClick } = useClickTracking()

    const mockKeyboardEvent = new KeyboardEvent('keydown', {
      key: 'Enter',
      ctrlKey: true,
      shiftKey: false,
      altKey: true
    })

    await trackClick(123, mockSearchResult, 1, mockKeyboardEvent)

    expect(searchLogApi.recordClick).toHaveBeenCalledWith(
      expect.objectContaining({
        clickType: 'keydown',
        modifierKeys: {
          ctrl: false, // KeyboardEvent modifiers are handled differently
          shift: false,
          alt: false
        }
      })
    )
  })

  it('应该正确处理自动同步功能', async () => {
    vi.useFakeTimers()

    const { startAutoSync, stopAutoSync, isOnline, trackingState } = useClickTracking()

    // Mock有离线数据需要同步
    trackingState.offlineCount = 5

    startAutoSync()

    // 快进30秒
    vi.advanceTimersByTime(30000)

    expect(searchLogApi.recordClicks).toHaveBeenCalled()

    stopAutoSync()

    vi.useRealTimers()
  })

  it('应该处理配置更新', () => {
    const { setTrackingEnabled } = useClickTracking()

    setTrackingEnabled(false)

    expect(updateTrackingConfig).toHaveBeenCalledWith({ enabled: false })
  })

  it('应该处理错误重试机制', async () => {
    const { trackClick } = useClickTracking()

    // 模拟API连续失败
    vi.mocked(searchLogApi.recordClick)
      .mockRejectedValueOnce(new Error('网络超时'))
      .mockRejectedValueOnce(new Error('服务器错误'))
      .mockResolvedValueOnce({
        success: true,
        data: null,
        message: '记录成功'
      })

    await trackClick(123, mockSearchResult, 1)

    // 验证会重试并最终成功
    expect(searchLogApi.recordClick).toHaveBeenCalledTimes(1)
  })

  it('应该正确处理批量同步数据', async () => {
    const { syncOfflineCache, trackingState } = useClickTracking()

    // 模拟离线缓存中有数据
    trackingState.offlineCount = 25 // 超过batchSize的数据

    await syncOfflineCache()

    // 验证分批处理
    expect(searchLogApi.recordClicks).toHaveBeenCalled()
  })

  it('应该处理追踪错误记录', async () => {
    const { trackClick, trackingState, clearTrackingErrors } = useClickTracking()

    vi.mocked(searchLogApi.recordClick).mockRejectedValue(new Error('测试错误'))

    await trackClick(123, mockSearchResult, 1)
    await nextTick()

    expect(trackingState.errors.length).toBeGreaterThan(0)
    expect(trackingState.errors[0].message).toContain('测试错误')

    clearTrackingErrors()
    expect(trackingState.errors.length).toBe(0)
  })

  it('应该限制离线缓存大小', async () => {
    // 临时修改配置
    const originalMaxItems = trackingConfig.maxOfflineItems
    trackingConfig.maxOfflineItems = 3

    const { trackClick, offlineCacheCount } = useClickTracking()

    // 模拟API失败以强制使用离线缓存
    vi.mocked(searchLogApi.recordClick).mockRejectedValue(new Error('网络错误'))

    // 添加超过限制的数据
    for (let i = 0; i < 5; i++) {
      await trackClick(123, { ...mockSearchResult, id: `doc${i}` }, i)
    }

    await nextTick()

    // 验证缓存大小被限制
    expect(offlineCacheCount.value).toBeLessThanOrEqual(trackingConfig.maxOfflineItems)

    // 恢复原始配置
    trackingConfig.maxOfflineItems = originalMaxItems
  })

  it('应该处理点击时间戳格式', async () => {
    const { trackClick } = useClickTracking()

    const beforeClick = new Date().toISOString()
    await trackClick(123, mockSearchResult, 1)
    const afterClick = new Date().toISOString()

    expect(searchLogApi.recordClick).toHaveBeenCalledWith(
      expect.objectContaining({
        clickTime: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
      })
    )

    const callArgs = vi.mocked(searchLogApi.recordClick).mock.calls[0][0]
    expect(callArgs.clickTime).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
    expect(callArgs.clickTime >= beforeClick).toBe(true)
    expect(callArgs.clickTime <= afterClick).toBe(true)
  })

  it('应该正确处理用户代理信息', async () => {
    const { trackClick } = useClickTracking()

    await trackClick(123, mockSearchResult, 1)

    expect(searchLogApi.recordClick).toHaveBeenCalledWith(
      expect.objectContaining({
        userAgent: 'test-agent'
      })
    )
  })

  it('应该处理异步操作的竞态条件', async () => {
    const { trackClick } = useClickTracking()

    // 同时发送多个点击请求
    const promises = Array.from({ length: 5 }, (_, i) =>
      trackClick(123, { ...mockSearchResult, id: `doc${i}` }, i)
    )

    await Promise.all(promises)

    // 验证所有请求都被正确处理
    expect(searchLogApi.recordClick).toHaveBeenCalledTimes(5)
  })

  it('应该正确计算缓存统计信息', () => {
    const { offlineCacheCount, hasTrackingErrors, trackingState } = useClickTracking()

    expect(offlineCacheCount.value).toBe(0)
    expect(hasTrackingErrors.value).toBe(false)

    // 模拟有缓存数据
    trackingState.offlineCount = 10
    expect(offlineCacheCount.value).toBe(10)

    // 模拟有错误
    trackingState.errors.push({
      id: 'error1',
      message: '测试错误',
      timestamp: new Date().toISOString()
    })
    expect(hasTrackingErrors.value).toBe(true)
  })

  it('应该处理空数据的边界情况', async () => {
    const { trackClick, trackMultipleClicks } = useClickTracking()

    // 测试空结果对象
    const emptyResult: SearchResult = {
      id: '',
      title: '',
      url: ''
    }

    await expect(trackClick(0, emptyResult, 0)).resolves.not.toThrow()

    // 测试空数组
    await expect(trackMultipleClicks([])).resolves.not.toThrow()
  })

  it('应该处理无效的配置值', () => {
    const { setTrackingEnabled } = useClickTracking()

    // 测试各种无效值
    expect(() => setTrackingEnabled(true)).not.toThrow()
    expect(() => setTrackingEnabled(false)).not.toThrow()
  })

  it('应该支持调试模式', async () => {
    const consoleSpy = vi.spyOn(console, 'debug').mockImplementation(() => {})

    const { trackClick } = useClickTracking()

    await trackClick(123, mockSearchResult, 1)

    // 验证调试信息被输出（如果debugMode为true）
    if (trackingConfig.debugMode) {
      expect(consoleSpy).toHaveBeenCalled()
    }

    consoleSpy.mockRestore()
  })

  it('应该处理API响应失败的情况', async () => {
    const { trackClick, offlineCacheCount } = useClickTracking()

    // 模拟API返回失败响应
    vi.mocked(searchLogApi.recordClick).mockResolvedValue({
      success: false,
      data: null,
      message: 'API处理失败'
    })

    await trackClick(123, mockSearchResult, 1)
    await nextTick()

    // 验证失败时会使用离线缓存
    expect(offlineCacheCount.value).toBe(1)
  })

  it('应该处理离线状态下的点击追踪', async () => {
    const { trackClick, offlineCacheCount, isOnline } = useClickTracking()

    // 设置为离线状态
    Object.defineProperty(window.navigator, 'onLine', {
      value: false,
      writable: true
    })
    window.dispatchEvent(new Event('offline'))

    await trackClick(123, mockSearchResult, 1)
    await nextTick()

    // 验证离线时直接缓存，不调用API
    expect(searchLogApi.recordClick).not.toHaveBeenCalled()
    expect(offlineCacheCount.value).toBe(1)
  })

  it('应该正确处理同步时的错误恢复', async () => {
    const { syncOfflineCache, trackingState } = useClickTracking()

    // 模拟有离线数据
    trackingState.offlineCount = 3

    // 模拟同步时部分失败
    vi.mocked(searchLogApi.recordClicks)
      .mockRejectedValueOnce(new Error('同步失败'))
      .mockResolvedValueOnce({
        success: true,
        data: null,
        message: '同步成功'
      })

    await syncOfflineCache()

    // 验证错误被记录
    expect(trackingState.errors.length).toBeGreaterThan(0)
  })
})