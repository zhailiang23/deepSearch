<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
    <div class="bg-background rounded-lg shadow-xl w-full max-w-md">
      <!-- 表单头部 -->
      <div class="flex items-center justify-between p-6 border-b border-border">
        <h3 class="text-lg font-medium text-foreground">
          {{ isEdit ? '编辑热门话题' : '新建热门话题' }}
        </h3>
        <button
          @click="$emit('close')"
          class="p-1 text-muted-foreground hover:text-foreground rounded-md hover:bg-accent"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- 表单内容 -->
      <form @submit.prevent="handleSubmit" class="p-6 space-y-4">
        <!-- 话题名称 -->
        <div>
          <label for="name" class="block text-sm font-medium text-foreground mb-2">
            话题名称 <span class="text-red-500">*</span>
          </label>
          <input
            id="name"
            v-model="form.name"
            type="text"
            placeholder="请输入话题名称"
            maxlength="100"
            class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground placeholder:text-muted-foreground"
            :class="{ 'border-red-500': errors.name }"
            @blur="validateName"
          />
          <div v-if="errors.name" class="mt-1 text-sm text-red-500">
            {{ errors.name }}
          </div>
          <div class="mt-1 text-xs text-muted-foreground">
            {{ form.name.length }}/100
          </div>
        </div>

        <!-- 热度值 -->
        <div>
          <label for="popularity" class="block text-sm font-medium text-foreground mb-2">
            热度值 <span class="text-red-500">*</span>
          </label>
          <div class="relative">
            <TrendingUp class="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              id="popularity"
              v-model.number="form.popularity"
              type="number"
              min="0"
              max="999999999"
              placeholder="请输入热度值"
              class="w-full pl-10 pr-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground placeholder:text-muted-foreground"
              :class="{ 'border-red-500': errors.popularity }"
              @blur="validatePopularity"
            />
          </div>
          <div v-if="errors.popularity" class="mt-1 text-sm text-red-500">
            {{ errors.popularity }}
          </div>
          <div class="mt-1 text-xs text-muted-foreground">
            热度值用于排序显示，数值越大排名越靠前
          </div>
        </div>

        <!-- 可见性 -->
        <div>
          <label class="block text-sm font-medium text-foreground mb-3">
            可见状态 <span class="text-red-500">*</span>
          </label>
          <div class="space-y-2">
            <label class="flex items-center">
              <input
                v-model="form.visible"
                type="radio"
                :value="true"
                class="h-4 w-4 text-primary border-border focus:ring-ring"
              />
              <span class="ml-2 text-sm text-foreground flex items-center">
                <Eye class="h-4 w-4 mr-1 text-green-500" />
                可见 - 在前端显示此话题
              </span>
            </label>
            <label class="flex items-center">
              <input
                v-model="form.visible"
                type="radio"
                :value="false"
                class="h-4 w-4 text-primary border-border focus:ring-ring"
              />
              <span class="ml-2 text-sm text-foreground flex items-center">
                <EyeOff class="h-4 w-4 mr-1 text-gray-500" />
                隐藏 - 不在前端显示此话题
              </span>
            </label>
          </div>
          <div v-if="errors.visible" class="mt-1 text-sm text-red-500">
            {{ errors.visible }}
          </div>
        </div>

        <!-- 按钮组 -->
        <div class="flex items-center justify-end space-x-3 pt-4">
          <button
            type="button"
            @click="$emit('close')"
            class="px-4 py-2 text-sm font-medium text-muted-foreground bg-background border border-border rounded-md hover:bg-accent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring"
          >
            取消
          </button>
          <button
            type="submit"
            :disabled="loading || !isFormValid"
            class="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary border border-transparent rounded-md hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="loading" class="flex items-center">
              <RefreshCw class="animate-spin h-4 w-4 mr-2" />
              {{ isEdit ? '更新中...' : '创建中...' }}
            </span>
            <span v-else>
              {{ isEdit ? '更新' : '创建' }}
            </span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { X, TrendingUp, Eye, EyeOff, RefreshCw } from 'lucide-vue-next'
import { useHotTopicStore } from '@/stores/hotTopic'
import type { HotTopic, CreateHotTopicRequest, UpdateHotTopicRequest } from '@/types/hotTopic'

interface Props {
  hotTopic?: HotTopic
}

interface Emits {
  (e: 'close'): void
  (e: 'success', hotTopic: HotTopic): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const hotTopicStore = useHotTopicStore()

// 表单数据
const form = reactive({
  name: '',
  popularity: 0,
  visible: true as boolean
})

// 错误信息
const errors = reactive({
  name: '',
  popularity: '',
  visible: ''
})

// 状态
const loading = ref(false)

// 计算属性
const isEdit = computed(() => !!props.hotTopic)

const isFormValid = computed(() => {
  return form.name.trim() !== '' &&
         form.popularity >= 0 &&
         form.visible !== null &&
         !errors.name &&
         !errors.popularity &&
         !errors.visible
})

// 验证方法
const validateName = async () => {
  errors.name = ''

  if (!form.name.trim()) {
    errors.name = '话题名称不能为空'
    return
  }

  if (form.name.length > 100) {
    errors.name = '话题名称长度不能超过100字符'
    return
  }

  // 检查名称唯一性
  try {
    const excludeId = props.hotTopic?.id
    const available = await hotTopicStore.isNameAvailable(form.name.trim(), excludeId)
    if (!available) {
      errors.name = '话题名称已存在'
    }
  } catch (error) {
    console.error('检查名称可用性失败:', error)
  }
}

const validatePopularity = () => {
  errors.popularity = ''

  if (form.popularity === null || form.popularity === undefined || isNaN(form.popularity)) {
    errors.popularity = '热度值不能为空'
    return
  }

  if (form.popularity < 0) {
    errors.popularity = '热度值不能小于0'
    return
  }

  if (form.popularity > 999999999) {
    errors.popularity = '热度值超出范围'
    return
  }
}

const validateVisible = () => {
  errors.visible = ''

  if (form.visible === null || form.visible === undefined) {
    errors.visible = '请选择可见状态'
  }
}

// 表单提交
const handleSubmit = async () => {
  // 验证所有字段
  await validateName()
  validatePopularity()
  validateVisible()

  if (!isFormValid.value) {
    return
  }

  try {
    loading.value = true

    let result: HotTopic

    if (isEdit.value) {
      // 更新
      const updateData: UpdateHotTopicRequest = {
        name: form.name.trim(),
        popularity: form.popularity,
        visible: form.visible
      }
      result = await hotTopicStore.updateHotTopic(props.hotTopic!.id, updateData)
    } else {
      // 创建
      const createData: CreateHotTopicRequest = {
        name: form.name.trim(),
        popularity: form.popularity,
        visible: form.visible
      }
      result = await hotTopicStore.createHotTopic(createData)
    }

    emit('success', result)
    emit('close')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

// 初始化表单数据
const initForm = () => {
  if (props.hotTopic) {
    form.name = props.hotTopic.name
    form.popularity = props.hotTopic.popularity
    form.visible = props.hotTopic.visible
  } else {
    form.name = ''
    form.popularity = 0
    form.visible = true
  }

  // 清除错误信息
  errors.name = ''
  errors.popularity = ''
  errors.visible = ''
}

// 监听props变化
watch(() => props.hotTopic, initForm, { immediate: true })

// 组件挂载时初始化
onMounted(() => {
  initForm()
})
</script>

<style scoped>
/* 自定义单选按钮样式 */
input[type="radio"] {
  accent-color: hsl(var(--primary));
}

/* 数字输入框样式 */
input[type="number"]::-webkit-outer-spin-button,
input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

input[type="number"] {
  -moz-appearance: textfield;
}
</style>