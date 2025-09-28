<template>
  <div class="word-cloud-config">
    <!-- 配置面板标题 -->
    <div class="config-header">
      <h3 class="text-lg font-semibold text-foreground">词云配置</h3>
      <div class="flex gap-2">
        <Button
          variant="outline"
          size="sm"
          @click="resetToDefaults"
          class="text-sm"
        >
          重置
        </Button>
        <Button
          variant="outline"
          size="sm"
          @click="exportConfig"
          class="text-sm"
        >
          导出
        </Button>
      </div>
    </div>

    <!-- 主题选择 -->
    <div class="config-section">
      <Label class="text-sm font-medium">主题</Label>
      <Select :value="selectedThemeName" @update:value="handleThemeChange">
        <SelectTrigger class="w-full">
          <SelectValue placeholder="选择主题" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem
            v-for="theme in availableThemes"
            :key="theme.name"
            :value="theme.name"
          >
            <div class="flex items-center gap-3">
              <div class="theme-preview">
                <div
                  v-for="(color, index) in getThemePreviewColors(theme)"
                  :key="index"
                  class="preview-color"
                  :style="{ backgroundColor: color }"
                />
              </div>
              <span>{{ getThemeDisplayName(theme.name) }}</span>
            </div>
          </SelectItem>
        </SelectContent>
      </Select>
    </div>

    <!-- 颜色配置 -->
    <div class="config-section">
      <Label class="text-sm font-medium">颜色设置</Label>
      <div class="space-y-3">
        <div class="flex items-center gap-3">
          <Label class="text-xs text-muted-foreground w-20">类型</Label>
          <Select
            :value="localConfig.colors.type"
            @update:value="updateColorType"
          >
            <SelectTrigger class="flex-1">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="gradient">渐变</SelectItem>
              <SelectItem value="custom">自定义</SelectItem>
              <SelectItem value="random-dark">随机深色</SelectItem>
              <SelectItem value="random-light">随机浅色</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!-- 渐变颜色配置 -->
        <div v-if="localConfig.colors.type === 'gradient'" class="space-y-2">
          <div class="flex items-center gap-3">
            <Label class="text-xs text-muted-foreground w-20">起始色</Label>
            <input
              type="color"
              :value="hslToHex(localConfig.colors.startColor || THEME_COLORS.LIGHT.PRIMARY)"
              @change="updateStartColor"
              class="color-input"
            />
            <span class="text-xs text-muted-foreground flex-1">
              {{ localConfig.colors.startColor }}
            </span>
          </div>
          <div class="flex items-center gap-3">
            <Label class="text-xs text-muted-foreground w-20">结束色</Label>
            <input
              type="color"
              :value="hslToHex(localConfig.colors.endColor || THEME_COLORS.LIGHT.PRIMARY_LIGHT)"
              @change="updateEndColor"
              class="color-input"
            />
            <span class="text-xs text-muted-foreground flex-1">
              {{ localConfig.colors.endColor }}
            </span>
          </div>
        </div>

        <!-- 自定义颜色配置 -->
        <div v-if="localConfig.colors.type === 'custom'" class="space-y-2">
          <div class="flex items-center justify-between">
            <Label class="text-xs text-muted-foreground">色彩列表</Label>
            <Button
              variant="outline"
              size="sm"
              @click="addCustomColor"
              class="h-6 text-xs"
            >
              添加颜色
            </Button>
          </div>
          <div class="custom-colors-list space-y-1">
            <div
              v-for="(color, index) in localConfig.colors.colors || []"
              :key="index"
              class="flex items-center gap-2"
            >
              <input
                type="color"
                :value="hslToHex(color)"
                @change="(e) => updateCustomColor(index, e)"
                class="color-input"
              />
              <span class="text-xs text-muted-foreground flex-1">{{ color }}</span>
              <Button
                variant="ghost"
                size="sm"
                @click="removeCustomColor(index)"
                class="h-6 w-6 p-0 text-destructive hover:text-destructive-foreground hover:bg-destructive"
              >
                ×
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 字体配置 -->
    <div class="config-section">
      <Label class="text-sm font-medium">字体设置</Label>
      <div class="space-y-3">
        <div class="flex items-center gap-3">
          <Label class="text-xs text-muted-foreground w-20">字体族</Label>
          <Select
            :value="localConfig.font.family"
            @update:value="updateFontFamily"
          >
            <SelectTrigger class="flex-1">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Inter, sans-serif">Inter</SelectItem>
              <SelectItem value="'Helvetica Neue', Helvetica, Arial, sans-serif">Helvetica</SelectItem>
              <SelectItem value="'Microsoft YaHei', sans-serif">微软雅黑</SelectItem>
              <SelectItem value="'PingFang SC', sans-serif">苹方</SelectItem>
              <SelectItem value="Georgia, serif">Georgia</SelectItem>
              <SelectItem value="'Times New Roman', serif">Times</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="flex items-center gap-3">
          <Label class="text-xs text-muted-foreground w-20">字重</Label>
          <Select
            :value="String(localConfig.font.weight)"
            @update:value="updateFontWeight"
          >
            <SelectTrigger class="flex-1">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="normal">正常 (400)</SelectItem>
              <SelectItem value="500">中等 (500)</SelectItem>
              <SelectItem value="600">半粗 (600)</SelectItem>
              <SelectItem value="bold">粗体 (700)</SelectItem>
              <SelectItem value="800">特粗 (800)</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div class="flex items-center gap-2">
            <Label class="text-xs text-muted-foreground w-12">最小</Label>
            <input
              type="number"
              :value="localConfig.font.minSize"
              @input="updateFontMinSize"
              min="6"
              max="50"
              class="text-input flex-1"
            />
          </div>
          <div class="flex items-center gap-2">
            <Label class="text-xs text-muted-foreground w-12">最大</Label>
            <input
              type="number"
              :value="localConfig.font.maxSize"
              @input="updateFontMaxSize"
              min="20"
              max="200"
              class="text-input flex-1"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 形状配置 -->
    <div class="config-section">
      <Label class="text-sm font-medium">形状设置</Label>
      <div class="space-y-3">
        <div class="flex items-center gap-3">
          <Label class="text-xs text-muted-foreground w-20">形状</Label>
          <Select
            :value="localConfig.shape.type"
            @update:value="updateShapeType"
          >
            <SelectTrigger class="flex-1">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="circle">圆形</SelectItem>
              <SelectItem value="cardioid">心形</SelectItem>
              <SelectItem value="diamond">菱形</SelectItem>
              <SelectItem value="triangle">三角形</SelectItem>
              <SelectItem value="pentagon">五角形</SelectItem>
              <SelectItem value="star">星形</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="flex items-center gap-3">
          <Label class="text-xs text-muted-foreground w-20">椭圆度</Label>
          <input
            type="range"
            :value="localConfig.shape.ellipticity || 0.65"
            @input="updateEllipticity"
            min="0.1"
            max="1"
            step="0.05"
            class="range-input flex-1"
          />
          <span class="text-xs text-muted-foreground w-12">
            {{ (localConfig.shape.ellipticity || 0.65).toFixed(2) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 旋转配置 -->
    <div class="config-section">
      <Label class="text-sm font-medium">旋转设置</Label>
      <div class="space-y-3">
        <div class="grid grid-cols-2 gap-3">
          <div class="flex items-center gap-2">
            <Label class="text-xs text-muted-foreground w-12">最小</Label>
            <input
              type="number"
              :value="Math.round((localConfig.rotation.minRotation || 0) * 180 / Math.PI)"
              @input="updateMinRotation"
              min="-180"
              max="180"
              class="text-input flex-1"
            />
            <span class="text-xs text-muted-foreground">°</span>
          </div>
          <div class="flex items-center gap-2">
            <Label class="text-xs text-muted-foreground w-12">最大</Label>
            <input
              type="number"
              :value="Math.round((localConfig.rotation.maxRotation || 0) * 180 / Math.PI)"
              @input="updateMaxRotation"
              min="-180"
              max="180"
              class="text-input flex-1"
            />
            <span class="text-xs text-muted-foreground">°</span>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <Label class="text-xs text-muted-foreground w-20">旋转比例</Label>
          <input
            type="range"
            :value="localConfig.rotation.rotateRatio || 0.3"
            @input="updateRotateRatio"
            min="0"
            max="1"
            step="0.1"
            class="range-input flex-1"
          />
          <span class="text-xs text-muted-foreground w-12">
            {{ Math.round((localConfig.rotation.rotateRatio || 0.3) * 100) }}%
          </span>
        </div>
      </div>
    </div>

    <!-- 性能配置 -->
    <div class="config-section">
      <Label class="text-sm font-medium">性能设置</Label>
      <div class="space-y-3">
        <div class="flex items-center gap-3">
          <Label class="text-xs text-muted-foreground w-20">网格大小</Label>
          <input
            type="range"
            :value="localConfig.gridSize || 8"
            @input="updateGridSize"
            min="2"
            max="20"
            step="1"
            class="range-input flex-1"
          />
          <span class="text-xs text-muted-foreground w-12">
            {{ localConfig.gridSize || 8 }}px
          </span>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <label class="flex items-center gap-2 text-xs">
            <input
              type="checkbox"
              :checked="shuffleEnabled"
              @change="updateShuffle"
              class="checkbox"
            />
            随机排列
          </label>
          <label class="flex items-center gap-2 text-xs">
            <input
              type="checkbox"
              :checked="shrinkToFitEnabled"
              @change="updateShrinkToFit"
              class="checkbox"
            />
            适应大小
          </label>
        </div>
      </div>
    </div>

    <!-- 预览区域 -->
    <div class="config-section">
      <Label class="text-sm font-medium">主题预览</Label>
      <div class="theme-preview-container">
        <div class="preview-backdrop" :style="{ backgroundColor: localConfig.background }">
          <div
            v-for="(color, index) in getThemePreviewColors(localConfig)"
            :key="index"
            class="preview-word"
            :style="{
              color: color,
              fontSize: `${14 + index * 2}px`,
              fontFamily: localConfig.font.family,
              fontWeight: localConfig.font.weight,
              transform: `rotate(${(index - 2) * 15}deg)`
            }"
          >
            词云{{ index + 1 }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

import type {
  WordCloudThemeConfig,
  WordCloudConfigProps
} from '@/types/wordCloudTypes'

import {
  PRESET_THEMES,
  THEME_COLORS,
  LIGHT_THEME_CONFIG,
  DARK_THEME_CONFIG
} from '@/constants/wordCloudDefaults'

import {
  getCurrentThemeMode,
  getThemeByName,
  getThemePreviewColors,
  createCustomTheme,
  exportThemeConfig
} from '@/utils/wordCloudThemes'

// Props定义
interface Props {
  config: WordCloudThemeConfig
  themes?: WordCloudThemeConfig[]
  onConfigChange?: (config: Partial<WordCloudThemeConfig>) => void
  onThemeChange?: (themeName: string) => void
  onReset?: () => void
  onExport?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  themes: () => PRESET_THEMES,
  onConfigChange: () => {},
  onThemeChange: () => {},
  onReset: () => {},
  onExport: () => {}
})

// 响应式数据
const localConfig = ref<WordCloudThemeConfig>({ ...props.config })
const availableThemes = computed(() => props.themes)
const selectedThemeName = computed(() => localConfig.value.name)

// 性能配置状态
const shuffleEnabled = ref(true)
const shrinkToFitEnabled = ref(true)

// 监听配置变化
watch(
  () => props.config,
  (newConfig) => {
    localConfig.value = { ...newConfig }
  },
  { deep: true }
)

watch(
  localConfig,
  (newConfig) => {
    props.onConfigChange(newConfig)
  },
  { deep: true }
)

// 主题相关方法
const handleThemeChange = (themeName: string) => {
  const theme = getThemeByName(themeName)
  localConfig.value = { ...theme }
  props.onThemeChange(themeName)
}

const getThemeDisplayName = (themeName: string): string => {
  const nameMap: Record<string, string> = {
    'light-green': '亮色淡绿',
    'dark-green': '暗色淡绿',
    'rainbow': '彩虹主题',
    'monochrome': '单色主题',
    'ocean': '海洋主题',
    'forest': '森林主题',
    'sunset': '日落主题'
  }
  return nameMap[themeName] || themeName
}

// 颜色相关方法
const updateColorType = (type: string) => {
  localConfig.value.colors.type = type as any
}

const updateStartColor = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.colors.startColor = hexToHsl(target.value)
}

const updateEndColor = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.colors.endColor = hexToHsl(target.value)
}

const addCustomColor = () => {
  if (!localConfig.value.colors.colors) {
    localConfig.value.colors.colors = []
  }
  localConfig.value.colors.colors.push(THEME_COLORS.LIGHT.PRIMARY)
}

const updateCustomColor = (index: number, event: Event) => {
  const target = event.target as HTMLInputElement
  if (localConfig.value.colors.colors) {
    localConfig.value.colors.colors[index] = hexToHsl(target.value)
  }
}

const removeCustomColor = (index: number) => {
  if (localConfig.value.colors.colors) {
    localConfig.value.colors.colors.splice(index, 1)
  }
}

// 字体相关方法
const updateFontFamily = (family: string) => {
  localConfig.value.font.family = family
}

const updateFontWeight = (weight: string) => {
  localConfig.value.font.weight = weight
}

const updateFontMinSize = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.font.minSize = parseInt(target.value)
}

const updateFontMaxSize = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.font.maxSize = parseInt(target.value)
}

// 形状相关方法
const updateShapeType = (type: string) => {
  localConfig.value.shape.type = type as any
}

const updateEllipticity = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.shape.ellipticity = parseFloat(target.value)
}

// 旋转相关方法
const updateMinRotation = (event: Event) => {
  const target = event.target as HTMLInputElement
  const degrees = parseInt(target.value)
  localConfig.value.rotation.minRotation = degrees * Math.PI / 180
}

const updateMaxRotation = (event: Event) => {
  const target = event.target as HTMLInputElement
  const degrees = parseInt(target.value)
  localConfig.value.rotation.maxRotation = degrees * Math.PI / 180
}

const updateRotateRatio = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.rotation.rotateRatio = parseFloat(target.value)
}

// 性能相关方法
const updateGridSize = (event: Event) => {
  const target = event.target as HTMLInputElement
  localConfig.value.gridSize = parseInt(target.value)
}

const updateShuffle = (event: Event) => {
  const target = event.target as HTMLInputElement
  shuffleEnabled.value = target.checked
}

const updateShrinkToFit = (event: Event) => {
  const target = event.target as HTMLInputElement
  shrinkToFitEnabled.value = target.checked
}

// 操作方法
const resetToDefaults = () => {
  const currentMode = getCurrentThemeMode()
  const defaultTheme = currentMode === 'dark' ? DARK_THEME_CONFIG : LIGHT_THEME_CONFIG
  localConfig.value = { ...defaultTheme }
  props.onReset()
}

const exportConfig = () => {
  const configJson = exportThemeConfig(localConfig.value)
  const blob = new Blob([configJson], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${localConfig.value.name}-theme.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  props.onExport()
}

// 颜色转换工具函数
const hslToHex = (hsl: string): string => {
  const match = hsl.match(/hsl\((\d+(?:\.\d+)?),\s*(\d+(?:\.\d+)?)%,\s*(\d+(?:\.\d+)?)%\)/)
  if (!match) return '#000000'

  const h = parseFloat(match[1]) / 360
  const s = parseFloat(match[2]) / 100
  const l = parseFloat(match[3]) / 100

  const hue2rgb = (p: number, q: number, t: number) => {
    if (t < 0) t += 1
    if (t > 1) t -= 1
    if (t < 1/6) return p + (q - p) * 6 * t
    if (t < 1/2) return q
    if (t < 2/3) return p + (q - p) * (2/3 - t) * 6
    return p
  }

  const q = l < 0.5 ? l * (1 + s) : l + s - l * s
  const p = 2 * l - q
  const r = hue2rgb(p, q, h + 1/3)
  const g = hue2rgb(p, q, h)
  const b = hue2rgb(p, q, h - 1/3)

  const toHex = (c: number) => {
    const hex = Math.round(c * 255).toString(16)
    return hex.length === 1 ? '0' + hex : hex
  }

  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

const hexToHsl = (hex: string): string => {
  const r = parseInt(hex.slice(1, 3), 16) / 255
  const g = parseInt(hex.slice(3, 5), 16) / 255
  const b = parseInt(hex.slice(5, 7), 16) / 255

  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  let h = 0
  let s = 0
  const l = (max + min) / 2

  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)

    switch (max) {
      case r: h = (g - b) / d + (g < b ? 6 : 0); break
      case g: h = (b - r) / d + 2; break
      case b: h = (r - g) / d + 4; break
    }
    h /= 6
  }

  return `hsl(${Math.round(h * 360)}, ${Math.round(s * 100)}%, ${Math.round(l * 100)}%)`
}
</script>

<style scoped>
.word-cloud-config {
  @apply space-y-6 p-4 bg-card border border-border rounded-lg;
}

.config-header {
  @apply flex items-center justify-between border-b border-border pb-3;
}

.config-section {
  @apply space-y-3;
}

.theme-preview {
  @apply flex gap-1;
}

.preview-color {
  @apply w-3 h-3 rounded-full border border-border;
}

.color-input {
  @apply w-8 h-8 rounded border border-border cursor-pointer;
}

.text-input {
  @apply px-2 py-1 text-sm border border-border rounded bg-background text-foreground;
}

.range-input {
  @apply accent-primary;
}

.checkbox {
  @apply w-4 h-4 accent-primary;
}

.custom-colors-list {
  @apply max-h-32 overflow-y-auto;
}

.theme-preview-container {
  @apply mt-3;
}

.preview-backdrop {
  @apply relative w-full h-24 rounded border border-border overflow-hidden flex items-center justify-center;
}

.preview-word {
  @apply absolute select-none pointer-events-none;
}

.preview-word:nth-child(1) { @apply top-2 left-4; }
.preview-word:nth-child(2) { @apply top-8 right-6; }
.preview-word:nth-child(3) { @apply bottom-2 left-8; }
.preview-word:nth-child(4) { @apply bottom-8 right-4; }
.preview-word:nth-child(5) { @apply top-4 left-1/2 transform -translate-x-1/2; }
</style>