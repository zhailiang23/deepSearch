<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
    <div class="bg-background rounded-lg shadow-xl w-full max-w-md">
      <!-- 头部 -->
      <div class="px-6 py-4 border-b border-border">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-medium text-foreground">
            {{ isEdit ? '编辑敏感词' : '创建敏感词' }}
          </h3>
          <button
            @click="handleClose"
            class="text-muted-foreground hover:text-foreground transition-colors"
          >
            ×
          </button>
        </div>
      </div>

      <!-- 表单内容 -->
      <form @submit.prevent="handleSubmit" class="px-6 py-4 space-y-4">
        <!-- 敏感词名称 -->
        <div>
          <label for="name" class="block text-sm font-medium text-foreground mb-1">
            敏感词名称 <span class="text-red-500">*</span>
          </label>
          <input
            id="name"
            v-model="formData.name"
            type="text"
            required
            maxlength="100"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            :class="{ 'border-red-500': errors.name }"
            placeholder="请输入敏感词名称"
            @blur="validateName"
          />
          <p v-if="errors.name" class="mt-1 text-sm text-red-500">{{ errors.name }}</p>
        </div>

        <!-- 危害等级 -->
        <div>
          <label for="harmLevel" class="block text-sm font-medium text-foreground mb-1">
            危害等级 <span class="text-red-500">*</span>
          </label>
          <select
            id="harmLevel"
            v-model.number="formData.harmLevel"
            required
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option :value="1">1级 - 低危</option>
            <option :value="2">2级 - 较低危</option>
            <option :value="3">3级 - 中危</option>
            <option :value="4">4级 - 较高危</option>
            <option :value="5">5级 - 高危</option>
          </select>
          <p class="mt-1 text-sm text-muted-foreground">
            选择敏感词的危害等级(1-5级,数字越大危害越高)
          </p>
        </div>

        <!-- 启用状态 -->
        <div>
          <label class="flex items-center space-x-2 cursor-pointer">
            <input
              v-model="formData.enabled"
              type="checkbox"
              class="w-4 h-4 text-primary border-border rounded focus:ring-2 focus:ring-primary"
            />
            <span class="text-sm font-medium text-foreground">启用该敏感词</span>
          </label>
          <p class="mt-1 text-sm text-muted-foreground">
            启用后该敏感词将生效
          </p>
        </div>

        <!-- 按钮组 -->
        <div class="flex items-center justify-end space-x-3 pt-4">
          <button
            type="button"
            @click="handleClose"
            class="px-4 py-2 text-sm font-medium text-muted-foreground bg-background border border-border rounded-md hover:bg-accent transition-colors"
          >
            取消
          </button>
          <button
            type="submit"
            :disabled="submitting"
            class="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary border border-transparent rounded-md hover:bg-primary/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ submitting ? '提交中...' : (isEdit ? '更新' : '创建') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useSensitiveWordStore } from '@/stores/sensitiveWord'
import type { SensitiveWord, CreateSensitiveWordRequest, UpdateSensitiveWordRequest } from '@/types/sensitiveWord'

const props = defineProps<{
  sensitiveWord?: SensitiveWord | null
}>()

const emit = defineEmits<{
  close: []
  success: [word: SensitiveWord]
}>()

const store = useSensitiveWordStore()

const isEdit = computed(() => !!props.sensitiveWord)

const formData = ref<CreateSensitiveWordRequest | UpdateSensitiveWordRequest>({
  name: '',
  harmLevel: 3,
  enabled: true
})

const errors = ref<Record<string, string>>({})
const submitting = ref(false)

// 初始化表单数据
watch(() => props.sensitiveWord, (newValue) => {
  if (newValue) {
    formData.value = {
      name: newValue.name,
      harmLevel: newValue.harmLevel,
      enabled: newValue.enabled
    }
  } else {
    formData.value = {
      name: '',
      harmLevel: 3,
      enabled: true
    }
  }
  errors.value = {}
}, { immediate: true })

const validateName = async () => {
  errors.value.name = ''

  if (!formData.value.name.trim()) {
    errors.value.name = '敏感词名称不能为空'
    return false
  }

  if (formData.value.name.length > 100) {
    errors.value.name = '敏感词名称长度不能超过100字符'
    return false
  }

  // 检查名称是否可用
  try {
    const excludeId = isEdit.value && props.sensitiveWord ? props.sensitiveWord.id : undefined
    const available = await store.checkNameAvailable(formData.value.name, excludeId)

    if (!available) {
      errors.value.name = '该敏感词名称已存在'
      return false
    }
  } catch (error) {
    console.error('验证敏感词名称失败:', error)
  }

  return true
}

const validateForm = async (): Promise<boolean> => {
  const nameValid = await validateName()

  if (!formData.value.harmLevel || formData.value.harmLevel < 1 || formData.value.harmLevel > 5) {
    errors.value.harmLevel = '危害等级必须在1-5之间'
    return false
  }

  return nameValid
}

const handleSubmit = async () => {
  const valid = await validateForm()
  if (!valid) {
    return
  }

  try {
    submitting.value = true

    let result: SensitiveWord
    if (isEdit.value && props.sensitiveWord) {
      result = await store.updateSensitiveWord(
        props.sensitiveWord.id,
        formData.value as UpdateSensitiveWordRequest
      )
    } else {
      result = await store.createSensitiveWord(formData.value as CreateSensitiveWordRequest)
    }

    emit('success', result)
    handleClose()
  } catch (error) {
    console.error('提交表单失败:', error)
    if (error instanceof Error) {
      errors.value.submit = error.message
    }
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  emit('close')
}
</script>