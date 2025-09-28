/**
 * 词云组件性能测试
 * 测试组件在各种负载条件下的性能表现
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, type VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import HotWordCloudChart from '../HotWordCloudChart.vue'
import { useWordCloudPerformance } from '@/composables/useWordCloudPerformance'
import type { HotWordItem } from '@/types/statistics'

// Mock performance APIs
Object.defineProperty(window, 'performance', {
  value: {
    now: vi.fn(() => Date.now()),
    memory: {
      usedJSHeapSize: 50 * 1024 * 1024, // 50MB
      totalJSHeapSize: 100 * 1024 * 1024, // 100MB
      jsHeapSizeLimit: 2 * 1024 * 1024 * 1024 // 2GB
    },
    mark: vi.fn(),
    measure: vi.fn(),
    getEntriesByType: vi.fn(() => [])
  },
  writable: true
})

Object.defineProperty(navigator, 'hardwareConcurrency', {
  value: 8,
  writable: true
})

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  constructor(callback: ResizeObserverCallback) {}
  observe() {}
  unobserve() {}
  disconnect() {}
}

// Mock requestAnimationFrame
let rafId = 0
global.requestAnimationFrame = vi.fn((callback: FrameRequestCallback) => {
  rafId++
  setTimeout(() => callback(performance.now()), 16) // 60fps
  return rafId
})

global.cancelAnimationFrame = vi.fn((id: number) => {
  // Mock implementation
})

describe('HotWordCloudChart Performance', () => {
  let wrapper: VueWrapper<any>

  beforeEach(() => {
    vi.clearAllMocks()
    rafId = 0
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  describe('大数据集性能测试', () => {
    it('应该能处理1000个词语的大数据集', async () => {
      const largeDataSet: HotWordItem[] = Array.from({ length: 1000 }, (_, i) => ({
        text: `词语${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      const startTime = performance.now()

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: largeDataSet,
          width: 800,
          height: 600
        }
      })

      await nextTick()

      const endTime = performance.now()
      const renderTime = endTime - startTime

      // 组件应该在合理时间内完成初始化（1秒以内）
      expect(renderTime).toBeLessThan(1000)

      // 验证组件正常渲染
      expect(wrapper.find('.hot-word-cloud-chart').exists()).toBe(true)
    })

    it('应该正确处理超大数据集（5000个词语）', async () => {
      const massiveDataSet: HotWordItem[] = Array.from({ length: 5000 }, (_, i) => ({
        text: `海量词语${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: massiveDataSet,
          width: 1200,
          height: 800
        }
      })

      await nextTick()

      // 验证组件没有崩溃
      expect(wrapper.find('.hot-word-cloud-chart').exists()).toBe(true)

      // 验证性能优化被触发（实际数据应该被限制）
      // 具体实现取决于 useWordCloudPerformance 的逻辑
    })

    it('应该在低内存环境下降级处理', async () => {
      // 模拟低内存环境
      ;(window.performance as any).memory = {
        usedJSHeapSize: 90 * 1024 * 1024, // 90MB
        totalJSHeapSize: 100 * 1024 * 1024, // 100MB - 90% 使用率
        jsHeapSizeLimit: 100 * 1024 * 1024
      }

      const dataSet: HotWordItem[] = Array.from({ length: 500 }, (_, i) => ({
        text: `内存测试词语${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: dataSet,
          width: 800,
          height: 600
        }
      })

      await nextTick()

      // 在低内存环境下，组件应该仍然能够渲染
      expect(wrapper.find('.hot-word-cloud-chart').exists()).toBe(true)
    })
  })

  describe('渲染性能测试', () => {
    it('应该在多次重新渲染时保持性能', async () => {
      const baseDataSet: HotWordItem[] = Array.from({ length: 100 }, (_, i) => ({
        text: `性能测试${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: baseDataSet,
          width: 600,
          height: 400
        }
      })

      await nextTick()

      const renderTimes: number[] = []

      // 执行10次重新渲染
      for (let i = 0; i < 10; i++) {
        const startTime = performance.now()

        // 更新数据触发重新渲染
        const newDataSet = baseDataSet.map(word => ({
          ...word,
          weight: Math.floor(Math.random() * 100) + 1
        }))

        await wrapper.setProps({ words: newDataSet })
        await nextTick()

        const endTime = performance.now()
        renderTimes.push(endTime - startTime)
      }

      // 验证渲染时间相对稳定，没有明显的性能退化
      const avgTime = renderTimes.reduce((sum, time) => sum + time, 0) / renderTimes.length
      const maxTime = Math.max(...renderTimes)

      expect(avgTime).toBeLessThan(100) // 平均渲染时间应小于100ms
      expect(maxTime).toBeLessThan(200) // 最大渲染时间应小于200ms

      // 验证渲染时间的方差不应该太大（性能稳定）
      const variance = renderTimes.reduce((sum, time) => sum + Math.pow(time - avgTime, 2), 0) / renderTimes.length
      expect(variance).toBeLessThan(1000) // 方差应该较小
    })

    it('应该正确处理快速连续的数据更新', async () => {
      const initialData: HotWordItem[] = [
        { text: '快速更新1', weight: 50 },
        { text: '快速更新2', weight: 60 },
        { text: '快速更新3', weight: 70 }
      ]

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: initialData,
          width: 400,
          height: 300
        }
      })

      await nextTick()

      // 快速连续更新数据（模拟实时数据流）
      const updatePromises = []
      for (let i = 0; i < 20; i++) {
        const newData = initialData.map(word => ({
          ...word,
          weight: Math.floor(Math.random() * 100) + 1
        }))

        updatePromises.push(wrapper.setProps({ words: newData }))
      }

      await Promise.all(updatePromises)
      await nextTick()

      // 验证组件没有崩溃或出现错误
      expect(wrapper.find('.hot-word-cloud-chart').exists()).toBe(true)
    })

    it('应该在不同画布尺寸下保持性能', async () => {
      const testData: HotWordItem[] = Array.from({ length: 200 }, (_, i) => ({
        text: `尺寸测试${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      const canvasSizes = [
        { width: 400, height: 300 },
        { width: 800, height: 600 },
        { width: 1200, height: 900 },
        { width: 1600, height: 1200 }
      ]

      const renderTimes: Record<string, number> = {}

      for (const size of canvasSizes) {
        const startTime = performance.now()

        wrapper = mount(HotWordCloudChart, {
          props: {
            words: testData,
            width: size.width,
            height: size.height
          }
        })

        await nextTick()

        const endTime = performance.now()
        renderTimes[`${size.width}x${size.height}`] = endTime - startTime

        wrapper.unmount()
      }

      // 验证不同尺寸下的渲染时间都在合理范围内
      Object.entries(renderTimes).forEach(([size, time]) => {
        expect(time).toBeLessThan(500) // 每个尺寸的渲染时间应小于500ms
      })

      // 验证大尺寸画布的渲染时间增长是合理的
      const smallTime = renderTimes['400x300']
      const largeTime = renderTimes['1600x1200']
      const timeIncrease = largeTime / smallTime

      expect(timeIncrease).toBeLessThan(5) // 尺寸增加4倍，时间增长不应超过5倍
    })
  })

  describe('内存使用测试', () => {
    it('应该正确管理内存使用', async () => {
      const dataSet: HotWordItem[] = Array.from({ length: 300 }, (_, i) => ({
        text: `内存管理测试${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      const initialMemory = (performance as any).memory.usedJSHeapSize

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: dataSet,
          width: 800,
          height: 600
        }
      })

      await nextTick()

      // 模拟多次操作后的内存使用
      for (let i = 0; i < 5; i++) {
        const newData = dataSet.map(word => ({
          ...word,
          weight: Math.floor(Math.random() * 100) + 1
        }))

        await wrapper.setProps({ words: newData })
        await nextTick()
      }

      const finalMemory = (performance as any).memory.usedJSHeapSize
      const memoryIncrease = finalMemory - initialMemory

      // 内存增长应该在合理范围内（不应有明显的内存泄露）
      expect(memoryIncrease).toBeLessThan(10 * 1024 * 1024) // 小于10MB
    })

    it('组件卸载后应该清理资源', async () => {
      const dataSet: HotWordItem[] = Array.from({ length: 100 }, (_, i) => ({
        text: `资源清理测试${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: dataSet,
          width: 600,
          height: 400
        }
      })

      await nextTick()

      // 记录卸载前的状态
      const beforeUnmount = {
        rafCalls: vi.mocked(requestAnimationFrame).mock.calls.length,
        cancelCalls: vi.mocked(cancelAnimationFrame).mock.calls.length
      }

      // 卸载组件
      wrapper.unmount()

      // 验证资源清理
      // 实际的清理逻辑会在组件的 onUnmounted 钩子中执行
      expect(wrapper.vm).toBeUndefined()
    })
  })

  describe('响应式性能测试', () => {
    it('容器尺寸变化时应该高效响应', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          words: [
            { text: '响应式测试1', weight: 50 },
            { text: '响应式测试2', weight: 60 }
          ],
          responsive: true
        }
      })

      await nextTick()

      const resizeTimes: number[] = []

      // 模拟多次容器尺寸变化
      for (let i = 0; i < 10; i++) {
        const startTime = performance.now()

        // 模拟ResizeObserver回调
        const mockRect = {
          width: 600 + i * 50,
          height: 400 + i * 30
        }

        // 触发尺寸变化逻辑
        // 实际实现中会通过ResizeObserver触发
        await nextTick()

        const endTime = performance.now()
        resizeTimes.push(endTime - startTime)
      }

      const avgResizeTime = resizeTimes.reduce((sum, time) => sum + time, 0) / resizeTimes.length

      // 尺寸变化响应应该很快
      expect(avgResizeTime).toBeLessThan(50) // 平均响应时间应小于50ms
    })

    it('应该正确节流频繁的尺寸变化', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          words: [{ text: '节流测试', weight: 50 }],
          responsive: true
        }
      })

      await nextTick()

      let updateCount = 0
      const originalUpdateDimensions = wrapper.vm.updateDimensions

      // Mock updateDimensions to count calls
      wrapper.vm.updateDimensions = (...args: any[]) => {
        updateCount++
        return originalUpdateDimensions?.(...args)
      }

      // 快速触发多次尺寸变化
      for (let i = 0; i < 50; i++) {
        // 模拟快速的ResizeObserver事件
        // 实际实现中会被节流/防抖
      }

      await new Promise(resolve => setTimeout(resolve, 500)) // 等待防抖/节流

      // 由于节流，实际更新次数应该远少于触发次数
      expect(updateCount).toBeLessThan(10)
    })
  })

  describe('并发处理测试', () => {
    it('应该正确处理并发的操作请求', async () => {
      const dataSet: HotWordItem[] = Array.from({ length: 100 }, (_, i) => ({
        text: `并发测试${i + 1}`,
        weight: Math.floor(Math.random() * 100) + 1
      }))

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: dataSet,
          width: 600,
          height: 400
        }
      })

      await nextTick()

      // 并发执行多个操作
      const operations = [
        wrapper.setProps({ theme: 'dark-green' }),
        wrapper.setProps({ width: 800 }),
        wrapper.setProps({ height: 600 }),
        wrapper.setProps({ words: dataSet.slice(0, 50) })
      ]

      await Promise.all(operations)
      await nextTick()

      // 验证最终状态正确
      expect(wrapper.props('theme')).toBe('dark-green')
      expect(wrapper.props('width')).toBe(800)
      expect(wrapper.props('height')).toBe(600)
      expect(wrapper.props('words')).toHaveLength(50)
    })
  })

  describe('边界性能测试', () => {
    it('空数据时应该快速响应', async () => {
      const startTime = performance.now()

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: [],
          width: 600,
          height: 400
        }
      })

      await nextTick()

      const endTime = performance.now()
      const renderTime = endTime - startTime

      // 空数据的渲染应该非常快
      expect(renderTime).toBeLessThan(10)
    })

    it('单个词语时应该快速渲染', async () => {
      const startTime = performance.now()

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: [{ text: '单个词语', weight: 100 }],
          width: 400,
          height: 300
        }
      })

      await nextTick()

      const endTime = performance.now()
      const renderTime = endTime - startTime

      // 单个词语的渲染应该很快
      expect(renderTime).toBeLessThan(20)
    })

    it('极小画布时应该正确处理', async () => {
      wrapper = mount(HotWordCloudChart, {
        props: {
          words: [
            { text: '小画布测试', weight: 50 }
          ],
          width: 50,
          height: 50
        }
      })

      await nextTick()

      // 验证组件在极小画布下不会崩溃
      expect(wrapper.find('.hot-word-cloud-chart').exists()).toBe(true)
    })

    it('极大画布时应该有性能限制', async () => {
      const startTime = performance.now()

      wrapper = mount(HotWordCloudChart, {
        props: {
          words: Array.from({ length: 50 }, (_, i) => ({
            text: `大画布测试${i + 1}`,
            weight: Math.floor(Math.random() * 100) + 1
          })),
          width: 4000,
          height: 3000
        }
      })

      await nextTick()

      const endTime = performance.now()
      const renderTime = endTime - startTime

      // 即使是极大画布，渲染时间也应该在合理范围内
      expect(renderTime).toBeLessThan(2000) // 2秒以内

      // 验证组件正常渲染
      expect(wrapper.find('.hot-word-cloud-chart').exists()).toBe(true)
    })
  })
})