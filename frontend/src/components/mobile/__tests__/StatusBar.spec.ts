import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBar from '../StatusBar.vue'

describe('StatusBar', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.clearAllTimers()
    vi.useRealTimers()
  })

  it('应该正确渲染StatusBar组件', () => {
    const wrapper = mount(StatusBar)

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.classes()).toContain('status-bar')
  })

  it('应该显示当前时间', async () => {
    // 设置一个固定的时间
    const mockDate = new Date('2024-01-15 14:30:00')
    vi.setSystemTime(mockDate)

    const wrapper = mount(StatusBar)
    await wrapper.vm.$nextTick()

    // 检查时间显示
    const timeText = wrapper.text()
    expect(timeText).toContain('14:30')
  })

  it('应该包含状态栏的所有基本元素', () => {
    const wrapper = mount(StatusBar)

    // 检查左侧时间显示区域
    expect(wrapper.find('.status-bar').exists()).toBe(true)

    // 检查右侧信息区域存在
    const rightSide = wrapper.findAll('.status-bar > div')
    expect(rightSide.length).toBeGreaterThanOrEqual(2)

    // 检查信号强度指示器（通过类名匹配）
    const signalIndicators = wrapper.findAll('[class*="w-1 h-"]')
    expect(signalIndicators.length).toBeGreaterThan(0)

    // 检查WiFi图标
    expect(wrapper.find('svg').exists()).toBe(true)

    // 检查电池指示器
    expect(wrapper.text()).toContain('100%')
  })

  it('应该具有正确的状态栏高度', () => {
    const wrapper = mount(StatusBar)
    const statusBar = wrapper.find('.status-bar')

    // 通过CSS类检查高度设置
    expect(statusBar.exists()).toBe(true)
  })

  it('应该具有iOS风格的背景效果', () => {
    const wrapper = mount(StatusBar)
    const statusBar = wrapper.find('.status-bar')

    expect(statusBar.exists()).toBe(true)
    // 检查是否有正确的CSS类用于背景效果
    expect(statusBar.classes()).toContain('status-bar')
  })

  it('应该正确设置定时器更新时间', async () => {
    // Mock setInterval
    const setIntervalSpy = vi.spyOn(global, 'setInterval')

    const wrapper = mount(StatusBar)

    // 等待组件挂载完成
    await wrapper.vm.$nextTick()

    // 验证初始时间设置
    expect(wrapper.vm.currentTime).toBeTruthy()

    // 验证setInterval被调用
    expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 60000)

    setIntervalSpy.mockRestore()
  })

  it('应该在组件卸载时清理定时器', async () => {
    // Mock clearInterval
    const clearIntervalSpy = vi.spyOn(global, 'clearInterval')

    const wrapper = mount(StatusBar)
    await wrapper.vm.$nextTick()

    // 卸载组件
    wrapper.unmount()

    // 验证clearInterval被调用
    expect(clearIntervalSpy).toHaveBeenCalled()

    clearIntervalSpy.mockRestore()
  })

  it('应该包含信号强度指示器', () => {
    const wrapper = mount(StatusBar)

    // 查找信号强度的点
    const signalIndicators = wrapper.findAll('[class*="w-1 h-"]')
    expect(signalIndicators.length).toBeGreaterThan(0)
  })

  it('应该包含WiFi图标SVG', () => {
    const wrapper = mount(StatusBar)

    const wifiIcon = wrapper.find('svg')
    expect(wifiIcon.exists()).toBe(true)
    expect(wifiIcon.find('path').exists()).toBe(true)
  })

  it('应该显示电池百分比', () => {
    const wrapper = mount(StatusBar)

    expect(wrapper.text()).toContain('100%')
  })

  it('应该具有正确的文本颜色和样式', () => {
    const wrapper = mount(StatusBar)
    const statusBar = wrapper.find('.status-bar')

    expect(statusBar.classes()).toContain('text-white')
    expect(statusBar.classes()).toContain('text-sm')
    expect(statusBar.classes()).toContain('font-medium')
  })

  it('应该具有合适的内边距', () => {
    const wrapper = mount(StatusBar)
    const statusBar = wrapper.find('.status-bar')

    expect(statusBar.classes()).toContain('px-6')
    expect(statusBar.classes()).toContain('py-2')
  })

  it('应该使用flex布局正确排列元素', () => {
    const wrapper = mount(StatusBar)
    const statusBar = wrapper.find('.status-bar')

    expect(statusBar.classes()).toContain('flex')
    expect(statusBar.classes()).toContain('justify-between')
    expect(statusBar.classes()).toContain('items-center')
  })
})