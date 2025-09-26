<template>
  <form @submit.prevent="handleSubmit" class="space-y-6">
    <div class="grid grid-cols-1 gap-6">
      <!-- 角色名称 -->
      <div class="space-y-2">
        <label for="name" class="block text-sm font-medium text-foreground">
          角色名称 <span class="text-red-500">*</span>
        </label>
        <input
          id="name"
          v-model="formData.name"
          type="text"
          required
          :disabled="loading"
          class="block w-full px-3 py-2 border border-border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground placeholder:text-muted-foreground disabled:opacity-50"
          placeholder="请输入角色名称"
        />
        <p v-if="errors.name" class="text-sm text-red-500">{{ errors.name }}</p>
      </div>

      <!-- 角色代码 -->
      <div class="space-y-2">
        <label for="code" class="block text-sm font-medium text-foreground">
          角色代码 <span class="text-red-500">*</span>
        </label>
        <input
          id="code"
          v-model="formData.code"
          type="text"
          required
          :disabled="loading || isEdit"
          class="block w-full px-3 py-2 border border-border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground placeholder:text-muted-foreground disabled:opacity-50 disabled:bg-muted"
          placeholder="请输入角色代码"
        />
        <p v-if="errors.code" class="text-sm text-red-500">{{ errors.code }}</p>
        <p class="text-xs text-muted-foreground">
          角色代码只能包含字母、数字和下划线
        </p>
      </div>

      <!-- 描述 -->
      <div class="space-y-2">
        <label for="description" class="block text-sm font-medium text-foreground">
          描述
        </label>
        <textarea
          id="description"
          v-model="formData.description"
          rows="4"
          :disabled="loading"
          class="block w-full px-3 py-2 border border-border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring bg-background text-foreground placeholder:text-muted-foreground disabled:opacity-50"
          placeholder="请输入角色描述"
        />
        <p v-if="errors.description" class="text-sm text-red-500">{{ errors.description }}</p>
      </div>
    </div>

    <!-- 按钮组 -->
    <div class="flex justify-end space-x-3 pt-4">
      <button
        type="button"
        @click="handleCancel"
        :disabled="loading"
        class="px-4 py-2 text-sm font-medium text-foreground bg-background border border-border rounded-md hover:bg-accent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring disabled:opacity-50"
      >
        取消
      </button>
      <button
        type="submit"
        :disabled="loading"
        class="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary border border-transparent rounded-md hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring disabled:opacity-50"
      >
        {{ loading ? '保存中...' : (isEdit ? '更新角色' : '创建角色') }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import type { Role, CreateRoleRequest, UpdateRoleRequest } from '@/types/role'

interface Props {
  role?: Role | null
  loading?: boolean
}

interface Emits {
  (e: 'submit', data: CreateRoleRequest | UpdateRoleRequest): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  role: null,
  loading: false
})

const emit = defineEmits<Emits>()

// 表单数据
const formData = reactive({
  name: '',
  code: '',
  description: ''
})

// 错误信息
const errors = reactive({
  name: '',
  code: '',
  description: ''
})

// 是否为编辑模式
const isEdit = computed(() => props.role !== null)

// 验证函数
const validateName = () => {
  errors.name = ''
  if (!formData.name.trim()) {
    errors.name = '角色名称不能为空'
    return false
  }
  if (formData.name.length < 2 || formData.name.length > 100) {
    errors.name = '角色名称长度必须在2-100个字符之间'
    return false
  }
  return true
}

const validateCode = () => {
  errors.code = ''
  if (!formData.code.trim()) {
    errors.code = '角色代码不能为空'
    return false
  }
  if (formData.code.length < 2 || formData.code.length > 50) {
    errors.code = '角色代码长度必须在2-50个字符之间'
    return false
  }
  if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(formData.code)) {
    errors.code = '角色代码必须以字母开头，只能包含字母、数字和下划线'
    return false
  }
  return true
}

const validateDescription = () => {
  errors.description = ''
  if (formData.description.length > 500) {
    errors.description = '描述长度不能超过500个字符'
    return false
  }
  return true
}

const validateForm = () => {
  const nameValid = validateName()
  const codeValid = validateCode()
  const descValid = validateDescription()
  return nameValid && codeValid && descValid
}

// 提交处理
const handleSubmit = () => {
  console.log('handleSubmit called', { formData: { ...formData } })

  if (!validateForm()) {
    console.log('Form validation failed')
    return
  }

  if (isEdit.value) {
    const updateData: UpdateRoleRequest = {
      name: formData.name,
      description: formData.description || undefined
    }
    console.log('Emitting update data:', updateData)
    emit('submit', updateData)
  } else {
    const createData: CreateRoleRequest = {
      name: formData.name,
      code: formData.code,
      description: formData.description || undefined
    }
    console.log('Emitting create data:', createData)
    emit('submit', createData)
  }
}

// 取消处理
const handleCancel = () => {
  emit('cancel')
}

// 重置表单
const resetForm = () => {
  formData.name = ''
  formData.code = ''
  formData.description = ''
  errors.name = ''
  errors.code = ''
  errors.description = ''
}

// 监听prop变化
watch(() => props.role, (newRole) => {
  if (newRole) {
    formData.name = newRole.name
    formData.code = newRole.code
    formData.description = newRole.description || ''
  } else {
    resetForm()
  }
}, { immediate: true })

// 暴露方法给父组件
defineExpose({
  resetForm,
  validateForm
})
</script>