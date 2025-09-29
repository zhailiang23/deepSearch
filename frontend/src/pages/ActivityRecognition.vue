<template>
  <div class="container mx-auto px-4">
    <!-- 主要内容区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-8 gap-6 min-h-[calc(100vh-200px)]">
      <!-- 左侧区域 -->
      <div class="lg:col-span-2 space-y-6">
        <!-- 图片上传控件 -->
        <div class="bg-background border border-border rounded-lg p-6">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-medium text-foreground">上传图片</h3>
            <button
              @click="triggerFileInput"
              class="inline-flex items-center px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors"
            >
              <Upload class="h-4 w-4 mr-2" />
              选择图片
            </button>
          </div>
          <input
            ref="fileInput"
            type="file"
            class="hidden"
            accept="image/*"
            @change="handleFileSelect"
          />
        </div>

        <!-- 已上传的活动图片预览 -->
        <div class="bg-background border border-border rounded-lg p-6">
          <h3 class="text-lg font-medium text-foreground mb-4">已上传的活动图片预览</h3>
          <div v-if="uploadedImage" class="space-y-4">
            <!-- 手机比例的图片展示区域 (9:16 约等于 0.5625) -->
            <div class="relative bg-gray-100 dark:bg-gray-800 rounded-lg overflow-hidden" style="aspect-ratio: 9/16; max-height: 400px;">
              <img
                :src="uploadedImage.url"
                :alt="uploadedImage.name"
                class="w-full h-full object-contain"
              />
              <button
                @click="removeImage"
                class="absolute top-2 right-2 p-1 bg-red-600 text-white rounded-full hover:bg-red-700 transition-colors shadow-lg"
              >
                <X class="h-4 w-4" />
              </button>
            </div>
            <button
              @click="recognizeText"
              :disabled="isRecognizing"
              class="w-full px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <div class="flex items-center justify-center">
                <Eye class="h-4 w-4 mr-2" />
                {{ isRecognizing ? '识别中...' : '开始识别' }}
              </div>
            </button>
          </div>
          <div v-else class="text-center py-16 text-muted-foreground">
            <ImageIcon class="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p>暂无上传的图片</p>
          </div>
        </div>
      </div>

      <!-- 中间区域 - 识别文字展示 -->
      <div class="lg:col-span-3">
        <div class="bg-background border border-border rounded-lg p-6 min-h-[600px]">
          <h3 class="text-lg font-medium text-foreground mb-4">从活动图片中识别出来的文字</h3>
          <div v-if="recognizedText" class="space-y-4 h-full flex flex-col">
            <div class="bg-muted/50 rounded-lg p-4 flex-1">
              <div class="flex items-center mb-3">
                <FileText class="h-5 w-5 text-green-600 mr-2" />
                <span class="font-medium text-foreground">识别结果</span>
              </div>
              <div class="whitespace-pre-wrap text-sm text-foreground leading-relaxed border-l-4 border-green-500 pl-4">
                {{ recognizedText }}
              </div>
            </div>
            <div class="flex flex-col space-y-3">
              <span class="text-sm text-muted-foreground">识别完成时间: {{ recognitionTime }}</span>
              <button
                @click="autoFillForm"
                class="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
              >
                <Wand2 class="h-4 w-4 mr-2 inline" />
                智能填充表单
              </button>
            </div>
          </div>
          <div v-else-if="isRecognizing" class="flex flex-col items-center justify-center h-full">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600 mb-4"></div>
            <p class="text-muted-foreground">正在识别图片中的文字...</p>
          </div>
          <div v-else class="flex flex-col items-center justify-center h-full text-muted-foreground">
            <FileText class="h-12 w-12 mb-4 opacity-50" />
            <p>请先上传图片并开始识别</p>
          </div>
        </div>
      </div>

      <!-- 右侧区域 - 活动创建表单 -->
      <div class="lg:col-span-3">
        <div class="bg-background border border-border rounded-lg p-6">
          <form @submit.prevent="saveActivity" class="space-y-3">
            <!-- 活动名称 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-2">活动名称</label>
              <input
                v-model="activityForm.name"
                type="text"
                placeholder="请输入活动名称"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>


            <!-- 活动描述 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-2">活动描述</label>
              <textarea
                v-model="activityForm.descript"
                rows="4"
                placeholder="请输入活动描述"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground resize-none"
              ></textarea>
            </div>

            <!-- 活动链接 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-2">活动链接</label>
              <input
                v-model="activityForm.link"
                type="text"
                placeholder="请输入活动链接，如：/activity/spring-finance"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>

            <!-- 开始日期 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-2">开始日期</label>
              <input
                v-model="activityForm.startDate"
                type="date"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>

            <!-- 结束日期 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-2">结束日期</label>
              <input
                v-model="activityForm.endDate"
                type="date"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>

            <!-- 活动状态 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-2">活动状态</label>
              <select
                v-model="activityForm.status"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              >
                <option value="进行中">进行中</option>
                <option value="即将开始">即将开始</option>
                <option value="已结束">已结束</option>
                <option value="已取消">已取消</option>
              </select>
            </div>

            <!-- 操作按钮 -->
            <div class="flex space-x-3 pt-4">
              <button
                type="button"
                @click="resetForm"
                class="flex-1 px-4 py-2 border border-border text-foreground bg-background hover:bg-accent rounded-md transition-colors"
              >
                重置
              </button>
              <button
                type="submit"
                :disabled="!isFormValid"
                class="flex-1 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <Save class="h-4 w-4 mr-2 inline" />
                保存活动
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 成功通知 -->
    <div
      v-if="showSuccessNotification"
      class="fixed top-4 right-4 bg-green-600 text-white p-4 rounded-lg shadow-lg z-50 flex items-center"
    >
      <CheckCircle class="h-5 w-5 mr-2" />
      <span>活动保存成功！</span>
      <button @click="showSuccessNotification = false" class="ml-4">
        <X class="h-4 w-4" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Upload,
  X,
  Eye,
  ImageIcon,
  FileText,
  Wand2,
  Save,
  CheckCircle
} from 'lucide-vue-next'

// 响应式数据
const fileInput = ref<HTMLInputElement>()
const uploadedImage = ref<{
  name: string
  size: number
  url: string
  file: File
} | null>(null)

const isRecognizing = ref(false)
const recognizedText = ref('')
const recognitionTime = ref('')
const showSuccessNotification = ref(false)

// 活动表单数据
const activityForm = ref({
  name: '',
  type: '活动',
  descript: '',
  link: '',
  startDate: '',
  endDate: '',
  status: '进行中'
})

// 模拟识别的文字内容
const mockRecognizedTexts = [
  `新春理财节
活动类型：理财活动
活动描述：新春特惠理财产品，年化收益率高达5.8%，限时抢购
活动时间：2024年2月1日 - 2024年2月29日
活动链接：/activity/spring-finance
活动状态：进行中`,

  `数字化转型实战培训
活动类型：培训
活动描述：企业数字化转型专业培训，由资深技术专家团队授课
活动时间：2024年5月20日 - 2024年5月22日
活动链接：/activity/digital-training
活动状态：即将开始`,

  `绿色技术创新研讨会
活动类型：会议
活动描述：可持续发展与绿色技术创新学术研讨会，汇聚国内外知名学者
活动时间：2024年6月8日 - 2024年6月10日
活动链接：/activity/green-tech-seminar
活动状态：即将开始`
]

// 计算属性
const isFormValid = computed(() => {
  return activityForm.value.name &&
         activityForm.value.descript
})

// 方法
const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    processFile(files[0])
  }
}

const processFile = (file: File) => {
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  const url = URL.createObjectURL(file)
  uploadedImage.value = {
    name: file.name,
    size: file.size,
    url,
    file
  }
}

const removeImage = () => {
  if (uploadedImage.value) {
    URL.revokeObjectURL(uploadedImage.value.url)
    uploadedImage.value = null
    recognizedText.value = ''
    recognitionTime.value = ''
  }
}


const recognizeText = async () => {
  if (!uploadedImage.value) return

  isRecognizing.value = true

  // 模拟API调用延迟
  await new Promise(resolve => setTimeout(resolve, 2000))

  // 随机选择一个模拟文字
  const randomIndex = Math.floor(Math.random() * mockRecognizedTexts.length)
  recognizedText.value = mockRecognizedTexts[randomIndex]
  recognitionTime.value = new Date().toLocaleString()

  isRecognizing.value = false
}

const autoFillForm = () => {
  if (!recognizedText.value) return

  // 简单的文字解析逻辑，实际项目中会使用更复杂的NLP
  const text = recognizedText.value

  // 提取活动名称（从第一行提取）
  const firstLine = text.split('\n')[0]
  if (firstLine && !firstLine.includes('活动类型') && !firstLine.includes('活动描述')) {
    activityForm.value.name = firstLine.trim()
  }


  // 提取活动描述
  const descriptMatch = text.match(/活动描述[:：]\s*(.+)/m)
  if (descriptMatch) {
    activityForm.value.descript = descriptMatch[1].trim()
  }

  // 提取活动链接
  const linkMatch = text.match(/活动链接[:：]\s*(.+)/m)
  if (linkMatch) {
    activityForm.value.link = linkMatch[1].trim()
  }

  // 提取活动时间（开始和结束日期）
  const timeMatch = text.match(/活动时间[:：]\s*(\d{4})年(\d{1,2})月(\d{1,2})日\s*-\s*(\d{4})年(\d{1,2})月(\d{1,2})日/m)
  if (timeMatch) {
    const [, startYear, startMonth, startDay, endYear, endMonth, endDay] = timeMatch
    activityForm.value.startDate = `${startYear}-${startMonth.padStart(2, '0')}-${startDay.padStart(2, '0')}`
    activityForm.value.endDate = `${endYear}-${endMonth.padStart(2, '0')}-${endDay.padStart(2, '0')}`
  }

  // 提取活动状态
  const statusMatch = text.match(/活动状态[:：]\s*(.+)/m)
  if (statusMatch) {
    activityForm.value.status = statusMatch[1].trim()
  }
}

const resetForm = () => {
  activityForm.value = {
    name: '',
    type: '活动',
    descript: '',
    link: '',
    startDate: '',
    endDate: '',
    status: '进行中'
  }
}

const saveActivity = () => {
  if (!isFormValid.value) return

  // 模拟保存操作
  console.log('保存活动:', activityForm.value)

  // 显示成功通知
  showSuccessNotification.value = true
  setTimeout(() => {
    showSuccessNotification.value = false
  }, 3000)

  // 重置表单
  resetForm()
}
</script>

<style scoped>
.container {
  max-width: 1400px;
}
</style>