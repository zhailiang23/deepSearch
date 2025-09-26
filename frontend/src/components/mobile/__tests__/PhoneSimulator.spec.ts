import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import PhoneSimulator from '../PhoneSimulator.vue'
import DeviceFrame from '../DeviceFrame.vue'
import StatusBar from '../StatusBar.vue'

describe('PhoneSimulator', () => {
  beforeEach(() => {
    vi.clearAllTimers()
    vi.useFakeTimers()
  })

  it('应该正确渲染PhoneSimulator组件', () => {
    const wrapper = mount(PhoneSimulator)

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.classes()).toContain('phone-simulator')
  })

  it('应该包含DeviceFrame和StatusBar子组件', () => {
    const wrapper = mount(PhoneSimulator)

    expect(wrapper.findComponent(DeviceFrame).exists()).toBe(true)
    expect(wrapper.findComponent(StatusBar).exists()).toBe(true)
  })

  it('应该正确处理scale属性', () => {
    const wrapper = mount(PhoneSimulator, {
      props: {
        scale: 0.8
      }
    })

    const deviceFrame = wrapper.findComponent(DeviceFrame)
    expect(deviceFrame.props('scale')).toBe(0.8)
  })

  it('应该正确处理deviceColor属性', () => {
    const wrapper = mount(PhoneSimulator, {
      props: {
        deviceColor: 'gold'
      }
    })

    const deviceFrame = wrapper.findComponent(DeviceFrame)
    expect(deviceFrame.props('deviceColor')).toBe('gold')
  })

  it('应该根据showStatusBar属性显示或隐藏状态栏', () => {
    // 测试显示状态栏
    const wrapperWithStatusBar = mount(PhoneSimulator, {
      props: {
        showStatusBar: true
      }
    })
    expect(wrapperWithStatusBar.findComponent(StatusBar).exists()).toBe(true)

    // 测试隐藏状态栏
    const wrapperWithoutStatusBar = mount(PhoneSimulator, {
      props: {
        showStatusBar: false
      }
    })
    expect(wrapperWithoutStatusBar.find('.status-bar').exists()).toBe(false)
  })

  it('应该正确处理插槽内容', () => {
    const customContent = '<div class="custom-content">自定义内容</div>'

    const wrapper = mount(PhoneSimulator, {
      slots: {
        default: customContent
      }
    })

    expect(wrapper.find('.custom-content').exists()).toBe(true)
    expect(wrapper.find('.custom-content').text()).toBe('自定义内容')
  })

  it('应该在没有插槽内容时显示默认主屏幕', () => {
    const wrapper = mount(PhoneSimulator)

    expect(wrapper.find('.default-home-screen').exists()).toBe(true)
    expect(wrapper.find('.home-screen-bg').exists()).toBe(true)
    expect(wrapper.find('.app-grid').exists()).toBe(true)
    expect(wrapper.find('.dock').exists()).toBe(true)
  })

  it('应该包含正确的应用图标', () => {
    const wrapper = mount(PhoneSimulator)

    // 检查主屏幕应用图标
    expect(wrapper.find('.app-messages').exists()).toBe(true)
    expect(wrapper.find('.app-calendar').exists()).toBe(true)
    expect(wrapper.find('.app-photos').exists()).toBe(true)
    expect(wrapper.find('.app-camera').exists()).toBe(true)

    // 检查Dock图标
    expect(wrapper.findAll('.dock-icon')).toHaveLength(4)
  })

  it('应该正确设置main-content的类', () => {
    const wrapperWithStatusBar = mount(PhoneSimulator, {
      props: {
        showStatusBar: true
      }
    })
    expect(wrapperWithStatusBar.find('.main-content').classes()).toContain('with-status-bar')

    const wrapperWithoutStatusBar = mount(PhoneSimulator, {
      props: {
        showStatusBar: false
      }
    })
    expect(wrapperWithoutStatusBar.find('.main-content').classes()).not.toContain('with-status-bar')
  })

  it('应该具有正确的屏幕容器结构', () => {
    const wrapper = mount(PhoneSimulator)

    expect(wrapper.find('.screen-container').exists()).toBe(true)
    expect(wrapper.find('.main-content').exists()).toBe(true)
  })

  it('应该正确处理默认props值', () => {
    const wrapper = mount(PhoneSimulator)
    const deviceFrame = wrapper.findComponent(DeviceFrame)

    expect(deviceFrame.props('scale')).toBe(1)
    expect(deviceFrame.props('deviceColor')).toBe('black')
    expect(wrapper.findComponent(StatusBar).exists()).toBe(true)
  })
})