# iPhone 设备模拟器组件

这个组件库提供了高保真的iPhone 12 Pro设备模拟器，用于展示移动端界面效果。

## 组件结构

### PhoneSimulator.vue
主要的iPhone设备模拟器组件，整合了设备外观和内容显示功能。

#### Props
```typescript
interface Props {
  scale?: number          // 缩放比例，默认1.0
  showStatusBar?: boolean // 是否显示状态栏，默认true
  deviceColor?: 'black' | 'white' | 'gold' | 'silver' // 设备颜色，默认'black'
}
```

#### 使用示例
```vue
<template>
  <!-- 基本使用 -->
  <PhoneSimulator>
    <div class="your-mobile-content">
      你的移动端内容
    </div>
  </PhoneSimulator>

  <!-- 自定义配置 -->
  <PhoneSimulator
    :scale="0.8"
    device-color="gold"
    :show-status-bar="true"
  >
    <YourMobileApp />
  </PhoneSimulator>
</template>
```

### DeviceFrame.vue
iPhone设备外观框架组件，提供真实的设备外观。

#### 特性
- 精确的iPhone 12 Pro尺寸比例
- 真实的设备边框和圆角效果
- 设备按钮（音量键、电源键、静音开关）
- 刘海设计和Home指示器
- 支持多种设备颜色

### StatusBar.vue
iOS风格的状态栏组件。

#### 特性
- 实时时间显示
- 信号强度指示器
- WiFi图标
- 电池电量显示
- iOS风格的背景模糊效果

## 技术规格

### 设备尺寸
- 屏幕内容区域：375 × 812px (iPhone 12 Pro标准)
- 设备总尺寸：390 × 844px (含边框)
- 状态栏高度：44px
- 设备边框半径：40px
- 屏幕边框半径：32px

### 样式特性
- 使用TailwindCSS实现
- CSS3渐变和阴影效果
- 支持响应式缩放
- 高性能CSS3动画
- 真实的设备立体感

## 文件结构

```
src/components/mobile/
├── PhoneSimulator.vue      # 主模拟器组件
├── DeviceFrame.vue         # 设备边框组件
├── StatusBar.vue           # 状态栏组件
├── index.ts                # 导出文件
├── README.md               # 本文档
└── __tests__/              # 测试文件
    ├── PhoneSimulator.spec.ts
    ├── DeviceFrame.spec.ts
    └── StatusBar.spec.ts
```

## 测试覆盖

所有组件都有完整的单元测试覆盖：
- 组件渲染测试
- Props属性测试
- 插槽内容测试
- 样式和布局测试
- 生命周期测试

运行测试：
```bash
npx vitest run src/components/mobile/__tests__
```

## 访问测试页面

开发服务器启动后，可以通过以下路径访问测试页面：
```
http://localhost:3002/phone-simulator-test
```

测试页面提供了：
- 设备缩放控制
- 设备颜色选择
- 状态栏开关
- 默认iOS主屏幕演示
- 移动搜索界面示例

## 性能优化

- 使用CSS3代替JavaScript动画
- 最小化DOM节点数量
- 合理的组件更新策略
- 优化的图片和图标资源

## 浏览器兼容性

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## 注意事项

1. 内容区域严格按照375x812px设计，确保移动端界面的准确显示
2. 设备外观高度还原真实iPhone，适合产品演示
3. 状态栏自动更新时间，展示真实的使用效果
4. 支持插槽内容，可以灵活显示各种移动端界面