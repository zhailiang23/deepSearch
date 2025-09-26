<template>
  <div class="phone-simulator">
    <DeviceFrame :scale="scale" :device-color="deviceColor">
      <!-- 设备屏幕内容 -->
      <div class="screen-container">
        <!-- 状态栏 -->
        <StatusBar v-if="showStatusBar" />

        <!-- 主要内容区域 -->
        <div
          class="main-content"
          :class="{ 'with-status-bar': showStatusBar }"
        >
          <slot>
            <!-- 默认内容：展示一个简单的iOS主屏幕效果 -->
            <div class="default-home-screen">
              <!-- 背景渐变 -->
              <div class="home-screen-bg">
                <!-- 应用图标网格 -->
                <div class="app-grid">
                  <!-- 第一行 -->
                  <div class="app-row">
                    <div class="app-icon app-messages"></div>
                    <div class="app-icon app-calendar"></div>
                    <div class="app-icon app-photos"></div>
                    <div class="app-icon app-camera"></div>
                  </div>

                  <!-- 第二行 -->
                  <div class="app-row">
                    <div class="app-icon app-weather"></div>
                    <div class="app-icon app-clock"></div>
                    <div class="app-icon app-maps"></div>
                    <div class="app-icon app-notes"></div>
                  </div>

                  <!-- 第三行 -->
                  <div class="app-row">
                    <div class="app-icon app-settings"></div>
                    <div class="app-icon app-phone"></div>
                    <div class="app-icon app-safari"></div>
                    <div class="app-icon app-music"></div>
                  </div>
                </div>

                <!-- 底部Dock -->
                <div class="dock">
                  <div class="dock-bg">
                    <div class="app-icon app-phone dock-icon"></div>
                    <div class="app-icon app-safari dock-icon"></div>
                    <div class="app-icon app-messages dock-icon"></div>
                    <div class="app-icon app-music dock-icon"></div>
                  </div>
                </div>
              </div>
            </div>
          </slot>
        </div>
      </div>
    </DeviceFrame>
  </div>
</template>

<script setup lang="ts">
import DeviceFrame from './DeviceFrame.vue'
import StatusBar from './StatusBar.vue'

interface Props {
  /** 缩放比例，默认1.0 */
  scale?: number
  /** 是否显示状态栏，默认true */
  showStatusBar?: boolean
  /** 设备颜色，默认'black' */
  deviceColor?: 'black' | 'white' | 'gold' | 'silver'
}

const props = withDefaults(defineProps<Props>(), {
  scale: 1,
  showStatusBar: true,
  deviceColor: 'black'
})
</script>

<style scoped>
.phone-simulator {
  display: inline-block;
  position: relative;
}

.screen-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.main-content {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.main-content.with-status-bar {
  height: calc(100% - 44px);
  margin-top: 44px;
}

/* 默认主屏幕样式 */
.default-home-screen {
  width: 100%;
  height: 100%;
  position: relative;
}

.home-screen-bg {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg,
    #667eea 0%,
    #764ba2 25%,
    #f093fb 50%,
    #f5576c 75%,
    #4facfe 100%);
  position: relative;
  padding: 60px 20px 120px;
}

.app-grid {
  display: flex;
  flex-direction: column;
  gap: 40px;
  align-items: center;
}

.app-row {
  display: flex;
  justify-content: center;
  gap: 30px;
}

.app-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  position: relative;
  box-shadow:
    0 4px 8px rgba(0, 0, 0, 0.2),
    0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.app-icon:hover {
  transform: scale(1.1);
}

/* 应用图标颜色 */
.app-messages { background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%); }
.app-calendar { background: linear-gradient(135deg, #ff4757 0%, #c44569 100%); }
.app-photos { background: linear-gradient(135deg, #ffb347 0%, #ff9500 100%); }
.app-camera { background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%); }
.app-weather { background: linear-gradient(135deg, #a29bfe 0%, #6c5ce7 100%); }
.app-clock { background: linear-gradient(135deg, #fd79a8 0%, #e84393 100%); }
.app-maps { background: linear-gradient(135deg, #00b894 0%, #00a085 100%); }
.app-notes { background: linear-gradient(135deg, #fdcb6e 0%, #f39c12 100%); }
.app-settings { background: linear-gradient(135deg, #636e72 0%, #2d3436 100%); }
.app-phone { background: linear-gradient(135deg, #00cec9 0%, #00b894 100%); }
.app-safari { background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%); }
.app-music { background: linear-gradient(135deg, #ff7675 0%, #d63031 100%); }

/* 底部Dock */
.dock {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  width: 280px;
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dock-bg {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 25px;
  padding: 15px 20px;
  display: flex;
  gap: 20px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.dock-icon {
  width: 60px;
  height: 60px;
}

/* 页面指示器点 */
.home-screen-bg::after {
  content: '';
  position: absolute;
  bottom: 140px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 8px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 4px;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
}

/* 添加图标内的符号 */
.app-icon::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-weight: bold;
  font-size: 24px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.app-messages::after { content: '💬'; }
.app-calendar::after { content: '📅'; }
.app-photos::after { content: '🖼️'; }
.app-camera::after { content: '📷'; }
.app-weather::after { content: '☀️'; }
.app-clock::after { content: '⏰'; }
.app-maps::after { content: '🗺️'; }
.app-notes::after { content: '📝'; }
.app-settings::after { content: '⚙️'; }
.app-phone::after { content: '📞'; }
.app-safari::after { content: '🧭'; }
.app-music::after { content: '🎵'; }
</style>