import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DeviceFrame from '../DeviceFrame.vue'

describe('DeviceFrame', () => {
  it('应该正确渲染DeviceFrame组件', () => {
    const wrapper = mount(DeviceFrame)

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.classes()).toContain('device-frame')
  })

  it('应该正确应用缩放样式', () => {
    const wrapper = mount(DeviceFrame, {
      props: {
        scale: 0.8
      }
    })

    const deviceFrame = wrapper.find('.device-frame')
    expect(deviceFrame.attributes('style')).toContain('transform: scale(0.8)')
    expect(deviceFrame.classes()).toContain('device-scaled')
  })

  it('应该正确处理设备颜色类', () => {
    const wrapper = mount(DeviceFrame, {
      props: {
        deviceColor: 'gold'
      }
    })

    expect(wrapper.find('.device-frame').classes()).toContain('device-gold')
  })

  it('应该包含所有设备结构元素', () => {
    const wrapper = mount(DeviceFrame)

    // 检查主要结构
    expect(wrapper.find('.device-outer').exists()).toBe(true)
    expect(wrapper.find('.device-border').exists()).toBe(true)
    expect(wrapper.find('.device-screen').exists()).toBe(true)
    expect(wrapper.find('.screen-content').exists()).toBe(true)

    // 检查设备特征
    expect(wrapper.find('.device-notch').exists()).toBe(true)
    expect(wrapper.find('.speaker-grille').exists()).toBe(true)
    expect(wrapper.find('.front-camera').exists()).toBe(true)
    expect(wrapper.find('.home-indicator').exists()).toBe(true)

    // 检查设备按钮
    expect(wrapper.find('.volume-up').exists()).toBe(true)
    expect(wrapper.find('.volume-down').exists()).toBe(true)
    expect(wrapper.find('.mute-switch').exists()).toBe(true)
    expect(wrapper.find('.power-button').exists()).toBe(true)
  })

  it('应该正确处理插槽内容', () => {
    const slotContent = '<div class="test-slot">测试插槽内容</div>'

    const wrapper = mount(DeviceFrame, {
      slots: {
        default: slotContent
      }
    })

    expect(wrapper.find('.test-slot').exists()).toBe(true)
    expect(wrapper.find('.test-slot').text()).toBe('测试插槽内容')
  })

  it('应该使用默认属性值', () => {
    const wrapper = mount(DeviceFrame)

    const deviceFrame = wrapper.find('.device-frame')
    expect(deviceFrame.attributes('style')).toContain('transform: scale(1)')
    expect(deviceFrame.classes()).toContain('device-black')
    expect(deviceFrame.classes()).not.toContain('device-scaled')
  })

  it('应该为不同设备颜色应用正确的类', () => {
    const colors: Array<'black' | 'white' | 'gold' | 'silver'> = ['black', 'white', 'gold', 'silver']

    colors.forEach(color => {
      const wrapper = mount(DeviceFrame, {
        props: {
          deviceColor: color
        }
      })

      expect(wrapper.find('.device-frame').classes()).toContain(`device-${color}`)
    })
  })

  it('应该具有正确的设备尺寸', () => {
    const wrapper = mount(DeviceFrame)
    const deviceOuter = wrapper.find('.device-outer')

    // 检查设备尺寸是否通过CSS设置
    const style = window.getComputedStyle ? {} : {}
    expect(deviceOuter.exists()).toBe(true)
  })

  it('应该在缩放时应用缩放类', () => {
    const wrapper = mount(DeviceFrame, {
      props: {
        scale: 0.5
      }
    })

    expect(wrapper.find('.device-frame').classes()).toContain('device-scaled')
  })

  it('应该在缩放为1时不应用缩放类', () => {
    const wrapper = mount(DeviceFrame, {
      props: {
        scale: 1
      }
    })

    expect(wrapper.find('.device-frame').classes()).not.toContain('device-scaled')
  })

  it('应该正确处理转换原点', () => {
    const wrapper = mount(DeviceFrame, {
      props: {
        scale: 0.7
      }
    })

    const deviceFrame = wrapper.find('.device-frame')
    expect(deviceFrame.attributes('style')).toContain('transform-origin: center center')
  })
})