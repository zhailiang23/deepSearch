<template>
  <div
    class="device-frame relative"
    :class="[
      `device-${deviceColor}`,
      { 'device-scaled': scale !== 1 }
    ]"
    :style="{
      transform: `scale(${scale})`,
      transformOrigin: 'center center'
    }"
  >
    <!-- 设备外壳 -->
    <div class="device-outer">
      <!-- 设备边框 -->
      <div class="device-border">
        <!-- 屏幕区域 -->
        <div class="device-screen">
          <!-- 刘海区域 -->
          <div class="device-notch">
            <!-- 扬声器 -->
            <div class="speaker-grille"></div>
            <!-- 前置摄像头 -->
            <div class="front-camera"></div>
          </div>

          <!-- 屏幕内容区域 -->
          <div class="screen-content">
            <slot></slot>
          </div>

          <!-- Home指示器 -->
          <div class="home-indicator"></div>
        </div>
      </div>

      <!-- 设备按钮 -->
      <!-- 音量按钮 -->
      <div class="volume-up"></div>
      <div class="volume-down"></div>

      <!-- 静音开关 -->
      <div class="mute-switch"></div>

      <!-- 电源按钮 -->
      <div class="power-button"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  scale?: number
  deviceColor?: 'black' | 'white' | 'gold' | 'silver'
}

const props = withDefaults(defineProps<Props>(), {
  scale: 1,
  deviceColor: 'black'
})
</script>

<style scoped>
/* iPhone 12 Pro 设备规格：
   - 屏幕尺寸：375x812px (内容区域)
   - 设备总尺寸：约 390x844px (含边框)
   - 设备厚度：7.4mm
   - 边框半径：约40px
*/

.device-frame {
  display: inline-block;
  position: relative;
  user-select: none;
}

.device-outer {
  position: relative;
  width: 390px;
  height: 844px;
}

/* 设备边框 */
.device-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 40px;
  padding: 8px;
  background: linear-gradient(145deg, #2a2a2a 0%, #1a1a1a 50%, #0f0f0f 100%);
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.3),
    0 10px 20px rgba(0, 0, 0, 0.2),
    inset 0 2px 4px rgba(255, 255, 255, 0.1),
    inset 0 -2px 4px rgba(0, 0, 0, 0.3);
}

.device-black .device-border {
  background: linear-gradient(145deg, #2a2a2a 0%, #1a1a1a 50%, #0f0f0f 100%);
}

.device-white .device-border {
  background: linear-gradient(145deg, #f5f5f5 0%, #e0e0e0 50%, #d0d0d0 100%);
}

.device-gold .device-border {
  background: linear-gradient(145deg, #f4e4bc 0%, #e6d5a8 50%, #d4c294 100%);
}

.device-silver .device-border {
  background: linear-gradient(145deg, #e8e8e8 0%, #d4d4d4 50%, #c0c0c0 100%);
}

/* 屏幕区域 */
.device-screen {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 32px;
  background: #000;
  overflow: hidden;
  box-shadow:
    inset 0 2px 8px rgba(0, 0, 0, 0.3),
    inset 0 0 2px rgba(0, 0, 0, 0.5);
}

/* 刘海区域 */
.device-notch {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 150px;
  height: 30px;
  background: #000;
  border-radius: 0 0 20px 20px;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
}

/* 扬声器格栅 */
.speaker-grille {
  width: 50px;
  height: 4px;
  background: #333;
  border-radius: 2px;
}

/* 前置摄像头 */
.front-camera {
  width: 8px;
  height: 8px;
  background: radial-gradient(circle, #1a1a1a 0%, #000 70%);
  border-radius: 50%;
  border: 1px solid #333;
}

/* 屏幕内容区域 */
.screen-content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 375px;
  height: 812px;
  margin: 8px auto;
  background: #fff;
  border-radius: 30px;
  overflow: hidden;
}

/* Home指示器 */
.home-indicator {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 134px;
  height: 5px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2.5px;
  z-index: 10;
}

/* 设备按钮 */
.volume-up,
.volume-down,
.mute-switch,
.power-button {
  position: absolute;
  background: linear-gradient(90deg, #333 0%, #222 50%, #111 100%);
  border-radius: 2px;
}

.volume-up {
  left: -3px;
  top: 120px;
  width: 6px;
  height: 30px;
  border-radius: 0 2px 2px 0;
}

.volume-down {
  left: -3px;
  top: 170px;
  width: 6px;
  height: 30px;
  border-radius: 0 2px 2px 0;
}

.mute-switch {
  left: -3px;
  top: 80px;
  width: 6px;
  height: 20px;
  border-radius: 0 2px 2px 0;
}

.power-button {
  right: -3px;
  top: 150px;
  width: 6px;
  height: 80px;
  border-radius: 2px 0 0 2px;
}

/* 缩放时保持居中 */
.device-scaled {
  margin: 20px;
}

/* 设备阴影效果 */
.device-frame::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 20px;
  background: radial-gradient(ellipse, rgba(0, 0, 0, 0.3) 0%, transparent 70%);
  border-radius: 100px;
  z-index: -1;
}
</style>