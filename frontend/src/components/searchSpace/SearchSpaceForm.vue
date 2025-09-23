<template>
  <form @submit.prevent="handleSubmit" class="space-y-6">
    <!-- 名称 -->
    <div class="space-y-2">
      <label for="name" class="text-sm font-medium text-gray-900 dark:text-gray-100">
        名称 <span class="text-red-500">*</span>
      </label>
      <input
        id="name"
        v-model="formData.name"
        type="text"
        required
        placeholder="请输入搜索空间名称"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
        :class="{ 'border-red-500': errors.name }"
      />
      <p v-if="errors.name" class="text-sm text-red-600 dark:text-red-400">
        {{ errors.name }}
      </p>
    </div>

    <!-- 代码 -->
    <div class="space-y-2">
      <label for="code" class="text-sm font-medium text-gray-900 dark:text-gray-100">
        代码 <span class="text-red-500">*</span>
      </label>
      <input
        id="code"
        v-model="formData.code"
        type="text"
        required
        :disabled="isEdit"
        placeholder="请输入搜索空间代码"
        pattern="^[a-z][a-z0-9_]*$"
        title="代码只能包含小写字母、数字和下划线，且必须以字母开头"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white disabled:bg-gray-100 disabled:cursor-not-allowed dark:disabled:bg-gray-700"
        :class="{ 'border-red-500': errors.code }"
        @blur="validateCode"
      />
      <p v-if="errors.code" class="text-sm text-red-600 dark:text-red-400">
        {{ errors.code }}
      </p>
      <p v-if="!isEdit" class="text-xs text-gray-500 dark:text-gray-400">
        代码只能包含小写字母、数字和下划线，且必须以字母开头，创建后不可修改
      </p>
    </div>

    <!-- 描述 -->
    <div class="space-y-2">
      <label for="description" class="text-sm font-medium text-gray-900 dark:text-gray-100">
        描述
      </label>
      <textarea
        id="description"
        v-model="formData.description"
        rows="3"
        placeholder="请输入搜索空间描述（可选）"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white resize-none"
        :class="{ 'border-red-500': errors.description }"
      />
      <p v-if="errors.description" class="text-sm text-red-600 dark:text-red-400">
        {{ errors.description }}
      </p>
    </div>

    <!-- 状态（仅编辑时显示） -->
    <div v-if="isEdit" class="space-y-2">
      <label for="status" class="text-sm font-medium text-gray-900 dark:text-gray-100">
        状态
      </label>
      <select
        id="status"
        v-model="formData.status"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
      >
        <option value="ACTIVE">活跃</option>
        <option value="INACTIVE">停用</option>
        <option value="MAINTENANCE">维护中</option>
        <option value="DELETED">已删除</option>
      </select>
    </div>

    <!-- 按钮 -->
    <div class="flex justify-end space-x-3 pt-4 border-t border-gray-200 dark:border-gray-700">
      <button
        type="button"
        @click="$emit('cancel')"
        class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 dark:bg-gray-800 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-700"
      >
        取消
      </button>
      <button
        type="submit"
        :disabled="loading || !isFormValid"
        class="px-4 py-2 text-sm font-medium text-white bg-green-500 border border-transparent rounded-md shadow-sm hover:bg-green-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-400 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {{ loading ? '保存中...' : (isEdit ? '更新' : '创建') }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useSearchSpaceStore } from '@/stores/searchSpace'
import type { SearchSpace, CreateSearchSpaceRequest, UpdateSearchSpaceRequest, SearchSpaceStatus } from '@/types/searchSpace'

interface Props {
  searchSpace?: SearchSpace | null
  loading?: boolean
}

interface Emits {
  (e: 'submit', data: CreateSearchSpaceRequest | UpdateSearchSpaceRequest): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  searchSpace: null,
  loading: false
})

const emit = defineEmits<Emits>()

const searchSpaceStore = useSearchSpaceStore()

// 表单数据
const formData = ref({
  name: '',
  code: '',
  description: '',
  status: 'ACTIVE' as SearchSpaceStatus
})

// 错误信息
const errors = ref({
  name: '',
  code: '',
  description: ''
})

// 计算属性
const isEdit = computed(() => !!props.searchSpace)

const isFormValid = computed(() => {
  return formData.value.name.trim() &&
         formData.value.code.trim() &&
         !errors.value.name &&
         !errors.value.code &&
         !errors.value.description
})

// 验证函数
const validateName = () => {
  const name = formData.value.name.trim()
  if (!name) {
    errors.value.name = '名称不能为空'
  } else if (name.length > 50) {
    errors.value.name = '名称长度不能超过50字符'
  } else if (/[<>"&]/.test(name)) {
    errors.value.name = '名称不能包含特殊字符: < > " &'
  } else {
    errors.value.name = ''
  }
}

const validateCode = async () => {
  const code = formData.value.code.trim()
  if (!code) {
    errors.value.code = '代码不能为空'
    return
  }

  if (code.length > 100) {
    errors.value.code = '代码长度不能超过100字符'
    return
  }

  if (!/^[a-z][a-z0-9_]*$/.test(code)) {
    errors.value.code = '代码只能包含小写字母、数字和下划线，且必须以字母开头'
    return
  }

  // 检查保留关键字
  const reservedKeywords = [
    'system', 'admin', 'root', 'config', 'settings',
    'api', 'www', 'mail', 'ftp', 'localhost',
    'elasticsearch', 'kibana', 'logstash'
  ]
  if (reservedKeywords.includes(code)) {
    errors.value.code = '代码不能使用保留关键字'
    return
  }

  // 检查代码唯一性（仅新建时）
  if (!isEdit.value) {
    try {
      const available = await searchSpaceStore.checkCodeAvailability(code)
      if (!available) {
        errors.value.code = '代码已被使用'
      } else {
        errors.value.code = ''
      }
    } catch (error) {
      // 忽略检查错误，让后端处理
      errors.value.code = ''
    }
  } else {
    errors.value.code = ''
  }
}

const validateDescription = () => {
  const description = formData.value.description.trim()
  if (description && description.length > 500) {
    errors.value.description = '描述长度不能超过500字符'
  } else {
    errors.value.description = ''
  }
}

// 监听表单变化
watch(() => formData.value.name, validateName)
watch(() => formData.value.description, validateDescription)

// 提交表单
const handleSubmit = () => {
  validateName()
  validateDescription()

  if (!isFormValid.value) {
    return
  }

  if (isEdit.value) {
    const updateData: UpdateSearchSpaceRequest = {
      name: formData.value.name.trim(),
      description: formData.value.description.trim() || undefined,
      status: formData.value.status
    }
    emit('submit', updateData)
  } else {
    const createData: CreateSearchSpaceRequest = {
      name: formData.value.name.trim(),
      code: formData.value.code.trim(),
      description: formData.value.description.trim() || undefined
    }
    emit('submit', createData)
  }
}

// 初始化表单数据
const initFormData = () => {
  if (props.searchSpace) {
    formData.value = {
      name: props.searchSpace.name,
      code: props.searchSpace.code,
      description: props.searchSpace.description || '',
      status: props.searchSpace.status
    }
  } else {
    formData.value = {
      name: '',
      code: '',
      description: '',
      status: 'ACTIVE'
    }
  }

  // 清空错误信息
  errors.value = {
    name: '',
    code: '',
    description: ''
  }
}

// 监听 props 变化
watch(() => props.searchSpace, initFormData, { immediate: true })

onMounted(() => {
  initFormData()
})
</script>