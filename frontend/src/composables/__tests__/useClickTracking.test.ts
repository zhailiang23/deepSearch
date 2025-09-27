/**
 * useClickTracking 组合函数测试
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { nextTick } from 'vue'
import { useClickTracking } from '../useClickTracking'
import { searchLogApi } from '@/api/searchLog'
import type { SearchResult } from '@/types/searchLog'

// Mock API
vi.mock('@/api/searchLog', () => ({
  searchLogApi: {
    recordClick: vi.fn(),
    recordClicks: vi.fn()
  }
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
})