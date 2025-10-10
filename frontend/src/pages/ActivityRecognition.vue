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
              <span class="text-sm text-green-600">已自动填充表单字段</span>
            </div>
          </div>
          <div v-else-if="isRecognizing" class="flex flex-col items-center justify-center h-full">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600 mb-4"></div>
            <p class="text-muted-foreground">正在识别图片中的文字...</p>
          </div>
          <div v-else-if="recognitionError" class="flex flex-col items-center justify-center h-full text-red-600">
            <p class="text-center mb-4">{{ recognitionError }}</p>
            <button
              @click="recognizeText"
              class="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors"
            >
              重试识别
            </button>
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
            <!-- 搜索空间 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-1">搜索空间</label>
              <select
                v-model="activityForm.searchSpaceId"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              >
                <option value="">选择搜索空间</option>
                <option
                  v-for="space in searchSpaces"
                  :key="space.id"
                  :value="space.id"
                >
                  {{ space.name }}
                </option>
              </select>
            </div>

            <!-- 活动名称 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-1">活动名称</label>
              <input
                v-model="activityForm.name"
                type="text"
                placeholder="请输入活动名称"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>


            <!-- 活动描述 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-1">活动描述</label>
              <textarea
                v-model="activityForm.descript"
                rows="4"
                placeholder="请输入活动描述"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground resize-none"
              ></textarea>
            </div>

            <!-- 活动链接 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-1">活动链接</label>
              <input
                v-model="activityForm.link"
                type="text"
                placeholder="请输入活动链接，如：/activity/spring-finance"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>

            <!-- 开始日期 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-1">开始日期</label>
              <input
                v-model="activityForm.startDate"
                type="date"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>

            <!-- 结束日期 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-1">结束日期</label>
              <input
                v-model="activityForm.endDate"
                type="date"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 bg-background text-foreground"
              />
            </div>

            <!-- 活动状态 -->
            <div>
              <label class="block text-sm font-medium text-foreground mb-1">活动状态</label>
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

            <!-- 错误提示 -->
            <div v-if="saveError" class="bg-red-50 border border-red-200 rounded-md p-3">
              <div class="flex items-center">
                <div class="text-red-600 text-sm">{{ saveError }}</div>
              </div>
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
                :disabled="!isFormValid || isSaving"
                class="flex-1 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                <div class="flex items-center justify-center">
                  <div v-if="isSaving" class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  <Save v-else class="h-4 w-4 mr-2" />
                  {{ isSaving ? '保存中...' : '保存活动' }}
                </div>
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
import http from '@/utils/http'
import {
  Upload,
  X,
  Eye,
  ImageIcon,
  FileText,
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
const recognitionError = ref('')
const activityData = ref<any>(null)
const isSaving = ref(false)
const saveError = ref('')

// 搜索空间相关数据
const searchSpaces = ref<Array<{id: string; name: string}>>([])

interface SearchSpace {
  id: string
  name: string
}

// 活动表单数据
const activityForm = ref({
  searchSpaceId: '',
  name: '',
  type: '活动',
  descript: '',
  link: '',
  startDate: '',
  endDate: '',
  status: '进行中'
})


// 计算属性
const isFormValid = computed(() => {
  return activityForm.value.searchSpaceId &&
         activityForm.value.name &&
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
    recognitionError.value = ''
    activityData.value = null
  }
}


const recognizeText = async () => {
  if (!uploadedImage.value) return

  isRecognizing.value = true
  recognitionError.value = ''

  try {
    // 构建 FormData 对象
    const formData = new FormData()
    formData.append('file', uploadedImage.value.file)

    // 调用后端图片识别 API
    const response = await http.post('/image/recognize', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 300000 // 5分钟超时
    })

    // http.post 已返回解包后的数据
    if (response) {
      activityData.value = response
      // 显示识别到的所有文字
      recognizedText.value = response.all || '未识别到文字内容'
      recognitionTime.value = new Date().toLocaleString()

      // 自动填充表单
      autoFillForm()
    } else {
      recognitionError.value = '图片识别失败'
      recognizedText.value = ''
    }
  } catch (error: any) {
    console.error('图片识别API调用失败:', error)
    if (error.status) {
      // 服务器返回错误响应
      recognitionError.value = error.message || `服务器错误 (${error.status})`
    } else {
      // 网络错误或其他错误
      recognitionError.value = error.message || '图片识别失败，请稍后重试'
    }
    recognizedText.value = ''
  } finally {
    isRecognizing.value = false
  }
}

const autoFillForm = () => {
  if (!activityData.value) return

  // 直接使用后端识别返回的结构化数据填充表单
  const data = activityData.value

  if (data.name) {
    activityForm.value.name = data.name
  }

  if (data.descript) {
    activityForm.value.descript = data.descript
  }

  if (data.link) {
    activityForm.value.link = data.link
  }

  if (data.startDate) {
    activityForm.value.startDate = data.startDate
  }

  if (data.endDate) {
    activityForm.value.endDate = data.endDate
  }

  if (data.status) {
    activityForm.value.status = data.status
  }
}

const resetForm = () => {
  activityForm.value = {
    searchSpaceId: '',
    name: '',
    type: '活动',
    descript: '',
    link: '',
    startDate: '',
    endDate: '',
    status: '进行中'
  }
  saveError.value = ''
}

const saveActivity = async () => {
  if (!isFormValid.value) return

  isSaving.value = true
  saveError.value = ''

  try {
    // 构建活动数据，按照后端要求的格式
    const activityData = [{
      id: `activity_${Date.now()}`, // 生成唯一ID
      name: activityForm.value.name,
      type: activityForm.value.type,
      descript: activityForm.value.descript,
      link: activityForm.value.link || '',
      startDate: activityForm.value.startDate || '',
      endDate: activityForm.value.endDate || '',
      status: activityForm.value.status
    }]

    // 调用导入API
    const response = await http.post(`/search-spaces/${activityForm.value.searchSpaceId}/import-json-content`, activityData)

    // http.post 已返回解包后的数据
    if (response) {
      // 显示成功通知
      showSuccessNotification.value = true
      setTimeout(() => {
        showSuccessNotification.value = false
      }, 3000)

      // 重置表单
      resetForm()
      console.log('活动保存成功:', response)
    } else {
      saveError.value = '保存失败'
    }
  } catch (error: any) {
    console.error('保存活动失败:', error)
    saveError.value = error.message || '网络错误，保存失败'
  } finally {
    isSaving.value = false
  }
}

// 加载搜索空间列表
const loadSearchSpaces = async () => {
  try {
    const result = await http.get('/search-spaces', {
      params: {
        page: 0,
        size: 100
      }
    })

    // http.get 已返回解包后的数据
    if (result && result.content && Array.isArray(result.content)) {
      searchSpaces.value = result.content.map((space: any) => ({
        id: space.id.toString(),
        name: space.name
      }))
    } else {
      searchSpaces.value = []
    }
  } catch (error) {
    console.error('加载搜索空间列表失败:', error)
    searchSpaces.value = []
  }
}

// 组件挂载时加载搜索空间列表
import { onMounted } from 'vue'

onMounted(() => {
  loadSearchSpaces()
})
</script>

<style scoped>
.container {
  max-width: 1400px;
}
</style>