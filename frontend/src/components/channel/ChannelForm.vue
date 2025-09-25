<template>
  <form @submit.prevent="handleSubmit" class="space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <!-- 基本信息 -->
      <div class="space-y-4">
        <h4 class="text-lg font-medium text-gray-900 dark:text-white">基本信息</h4>

        <!-- 名称 -->
        <div class="space-y-2">
          <label for="name" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            渠道名称 <span class="text-red-500">*</span>
          </label>
          <input
            id="name"
            v-model="formData.name"
            type="text"
            required
            placeholder="请输入渠道名称"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
            :class="{ 'border-red-500': errors.name }"
          />
          <p v-if="errors.name" class="text-sm text-red-600 dark:text-red-400">
            {{ errors.name }}
          </p>
        </div>

        <!-- 代码 -->
        <div class="space-y-2">
          <label for="code" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            渠道代码 <span class="text-red-500">*</span>
          </label>
          <input
            id="code"
            v-model="formData.code"
            type="text"
            required
            :disabled="isEdit"
            placeholder="请输入渠道代码"
            pattern="^[a-z][a-z0-9_]*$"
            title="代码只能包含小写字母、数字和下划线，且必须以字母开头"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white disabled:bg-gray-100 disabled:cursor-not-allowed dark:disabled:bg-gray-700"
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

        <!-- 类型 -->
        <div class="space-y-2">
          <label for="type" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            渠道类型 <span class="text-red-500">*</span>
          </label>
          <select
            id="type"
            v-model="formData.type"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
          >
            <option value="">请选择渠道类型</option>
            <option value="ONLINE">线上渠道</option>
            <option value="OFFLINE">线下渠道</option>
            <option value="HYBRID">混合渠道</option>
            <option value="DISTRIBUTOR">分销商渠道</option>
          </select>
          <p v-if="errors.type" class="text-sm text-red-600 dark:text-red-400">
            {{ errors.type }}
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
            placeholder="请输入渠道描述（可选）"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white resize-none"
            :class="{ 'border-red-500': errors.description }"
          />
          <p v-if="errors.description" class="text-sm text-red-600 dark:text-red-400">
            {{ errors.description }}
          </p>
        </div>
      </div>

      <!-- 联系和业务信息 -->
      <div class="space-y-4">
        <h4 class="text-lg font-medium text-gray-900 dark:text-white">联系和业务信息</h4>

        <!-- 联系人 -->
        <div class="space-y-2">
          <label for="contactName" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            联系人姓名
          </label>
          <input
            id="contactName"
            v-model="formData.contactName"
            type="text"
            placeholder="请输入联系人姓名"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
          />
        </div>

        <!-- 联系电话 -->
        <div class="space-y-2">
          <label for="contactPhone" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            联系电话
          </label>
          <input
            id="contactPhone"
            v-model="formData.contactPhone"
            type="tel"
            placeholder="请输入联系电话"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
          />
        </div>

        <!-- 联系邮箱 -->
        <div class="space-y-2">
          <label for="contactEmail" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            联系邮箱
          </label>
          <input
            id="contactEmail"
            v-model="formData.contactEmail"
            type="email"
            placeholder="请输入联系邮箱"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
            :class="{ 'border-red-500': errors.contactEmail }"
          />
          <p v-if="errors.contactEmail" class="text-sm text-red-600 dark:text-red-400">
            {{ errors.contactEmail }}
          </p>
        </div>

        <!-- 地址 -->
        <div class="space-y-2">
          <label for="address" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            地址
          </label>
          <input
            id="address"
            v-model="formData.address"
            type="text"
            placeholder="请输入地址"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
          />
        </div>

        <!-- 网站 -->
        <div class="space-y-2">
          <label for="website" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            网站
          </label>
          <input
            id="website"
            v-model="formData.website"
            type="url"
            placeholder="请输入网站地址"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
            :class="{ 'border-red-500': errors.website }"
          />
          <p v-if="errors.website" class="text-sm text-red-600 dark:text-red-400">
            {{ errors.website }}
          </p>
        </div>
      </div>
    </div>

    <!-- 业务配置 -->
    <div class="space-y-4">
      <h4 class="text-lg font-medium text-gray-900 dark:text-white">业务配置</h4>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- 佣金率 -->
        <div class="space-y-2">
          <label for="commissionRate" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            佣金率 (%)
          </label>
          <input
            id="commissionRate"
            v-model.number="formData.commissionRate"
            type="number"
            min="0"
            max="100"
            step="0.01"
            placeholder="0.00"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
            :class="{ 'border-red-500': errors.commissionRate }"
          />
          <p v-if="errors.commissionRate" class="text-sm text-red-600 dark:text-red-400">
            {{ errors.commissionRate }}
          </p>
        </div>

        <!-- 月度目标 -->
        <div class="space-y-2">
          <label for="monthlyTarget" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            月度目标 (元)
          </label>
          <input
            id="monthlyTarget"
            v-model.number="formData.monthlyTarget"
            type="number"
            min="0"
            step="0.01"
            placeholder="0.00"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
            :class="{ 'border-red-500': errors.monthlyTarget }"
          />
          <p v-if="errors.monthlyTarget" class="text-sm text-red-600 dark:text-red-400">
            {{ errors.monthlyTarget }}
          </p>
        </div>

        <!-- 排序顺序 -->
        <div class="space-y-2">
          <label for="sortOrder" class="text-sm font-medium text-gray-900 dark:text-gray-100">
            排序顺序
          </label>
          <input
            id="sortOrder"
            v-model.number="formData.sortOrder"
            type="number"
            min="0"
            placeholder="0"
            class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 dark:bg-gray-800 dark:border-gray-600 dark:text-white"
          />
          <p class="text-xs text-gray-500 dark:text-gray-400">
            数值越小排序越靠前
          </p>
        </div>
      </div>
    </div>

    <!-- 按钮 -->
    <div class="flex justify-end space-x-3 pt-4 border-t border-gray-200 dark:border-gray-700">
      <button
        type="button"
        @click="$emit('cancel')"
        class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 dark:bg-gray-800 dark:text-gray-300 dark:border-gray-600 dark:hover:bg-gray-700"
      >
        取消
      </button>
      <button
        type="submit"
        :disabled="loading || !isFormValid"
        class="px-4 py-2 text-sm font-medium text-white bg-green-600 border border-transparent rounded-md shadow-sm hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {{ loading ? '保存中...' : (isEdit ? '更新' : '创建') }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useChannelStore } from '@/stores/channel'
import type { Channel, CreateChannelRequest, UpdateChannelRequest, ChannelType } from '@/types/channel'

interface Props {
  channel?: Channel | null
  loading?: boolean
}

interface Emits {
  (e: 'submit', data: CreateChannelRequest | UpdateChannelRequest): void
  (e: 'cancel'): void
}

const props = withDefaults(defineProps<Props>(), {
  channel: null,
  loading: false
})

const emit = defineEmits<Emits>()

const channelStore = useChannelStore()

// 表单数据
const formData = ref({
  name: '',
  code: '',
  description: '',
  type: '' as ChannelType | '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  website: '',
  commissionRate: 0,
  monthlyTarget: 0,
  sortOrder: 0
})

// 错误信息
const errors = ref({
  name: '',
  code: '',
  description: '',
  type: '',
  contactEmail: '',
  website: '',
  commissionRate: '',
  monthlyTarget: ''
})

// 计算属性
const isEdit = computed(() => !!props.channel)

const isFormValid = computed(() => {
  return formData.value.name.trim() &&
         formData.value.code.trim() &&
         formData.value.type &&
         !errors.value.name &&
         !errors.value.code &&
         !errors.value.description &&
         !errors.value.type &&
         !errors.value.contactEmail &&
         !errors.value.website &&
         !errors.value.commissionRate &&
         !errors.value.monthlyTarget
})

// 验证函数
const validateName = () => {
  const name = formData.value.name.trim()
  if (!name) {
    errors.value.name = '渠道名称不能为空'
  } else if (name.length > 100) {
    errors.value.name = '渠道名称长度不能超过100字符'
  } else if (/[<>"&]/.test(name)) {
    errors.value.name = '渠道名称不能包含特殊字符: < > " &'
  } else {
    errors.value.name = ''
  }
}

const validateCode = async () => {
  const code = formData.value.code.trim()
  if (!code) {
    errors.value.code = '渠道代码不能为空'
    return
  }

  if (code.length > 50) {
    errors.value.code = '渠道代码长度不能超过50字符'
    return
  }

  if (!/^[a-z][a-z0-9_]*$/.test(code)) {
    errors.value.code = '渠道代码只能包含小写字母、数字和下划线，且必须以字母开头'
    return
  }

  // 检查保留关键字
  const reservedKeywords = [
    'system', 'admin', 'root', 'config', 'settings',
    'api', 'www', 'mail', 'ftp', 'localhost',
    'channel', 'channels', 'default', 'test'
  ]
  if (reservedKeywords.includes(code)) {
    errors.value.code = '渠道代码不能使用保留关键字'
    return
  }

  // 检查代码唯一性（仅新建时）
  if (!isEdit.value) {
    try {
      const available = await channelStore.checkCodeAvailability(code)
      if (!available) {
        errors.value.code = '渠道代码已被使用'
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

const validateType = () => {
  if (!formData.value.type) {
    errors.value.type = '请选择渠道类型'
  } else {
    errors.value.type = ''
  }
}

const validateContactEmail = () => {
  const email = formData.value.contactEmail.trim()
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.value.contactEmail = '请输入有效的邮箱地址'
  } else {
    errors.value.contactEmail = ''
  }
}

const validateWebsite = () => {
  const website = formData.value.website.trim()
  if (website && !/^https?:\/\/.+/.test(website)) {
    errors.value.website = '请输入有效的网站地址（以http://或https://开头）'
  } else {
    errors.value.website = ''
  }
}

const validateCommissionRate = () => {
  const rate = formData.value.commissionRate
  if (rate < 0) {
    errors.value.commissionRate = '佣金率不能为负数'
  } else if (rate > 100) {
    errors.value.commissionRate = '佣金率不能超过100%'
  } else {
    errors.value.commissionRate = ''
  }
}

const validateMonthlyTarget = () => {
  const target = formData.value.monthlyTarget
  if (target < 0) {
    errors.value.monthlyTarget = '月度目标不能为负数'
  } else {
    errors.value.monthlyTarget = ''
  }
}

// 监听表单变化
watch(() => formData.value.name, validateName)
watch(() => formData.value.description, validateDescription)
watch(() => formData.value.type, validateType)
watch(() => formData.value.contactEmail, validateContactEmail)
watch(() => formData.value.website, validateWebsite)
watch(() => formData.value.commissionRate, validateCommissionRate)
watch(() => formData.value.monthlyTarget, validateMonthlyTarget)

// 提交表单
const handleSubmit = () => {
  // 执行所有验证
  validateName()
  validateDescription()
  validateType()
  validateContactEmail()
  validateWebsite()
  validateCommissionRate()
  validateMonthlyTarget()

  if (!isFormValid.value) {
    return
  }

  if (isEdit.value) {
    const updateData: UpdateChannelRequest = {
      name: formData.value.name.trim(),
      description: formData.value.description.trim() || undefined,
      type: formData.value.type as ChannelType,
      contactName: formData.value.contactName.trim() || undefined,
      contactPhone: formData.value.contactPhone.trim() || undefined,
      contactEmail: formData.value.contactEmail.trim() || undefined,
      address: formData.value.address.trim() || undefined,
      website: formData.value.website.trim() || undefined,
      commissionRate: formData.value.commissionRate,
      monthlyTarget: formData.value.monthlyTarget,
      sortOrder: formData.value.sortOrder
    }
    emit('submit', updateData)
  } else {
    const createData: CreateChannelRequest = {
      name: formData.value.name.trim(),
      code: formData.value.code.trim(),
      description: formData.value.description.trim() || undefined,
      type: formData.value.type as ChannelType,
      contactName: formData.value.contactName.trim() || undefined,
      contactPhone: formData.value.contactPhone.trim() || undefined,
      contactEmail: formData.value.contactEmail.trim() || undefined,
      address: formData.value.address.trim() || undefined,
      website: formData.value.website.trim() || undefined,
      commissionRate: formData.value.commissionRate,
      monthlyTarget: formData.value.monthlyTarget,
      sortOrder: formData.value.sortOrder
    }
    emit('submit', createData)
  }
}

// 初始化表单数据
const initFormData = () => {
  if (props.channel) {
    formData.value = {
      name: props.channel.name,
      code: props.channel.code,
      description: props.channel.description || '',
      type: props.channel.type,
      contactName: props.channel.contactName || '',
      contactPhone: props.channel.contactPhone || '',
      contactEmail: props.channel.contactEmail || '',
      address: props.channel.address || '',
      website: props.channel.website || '',
      commissionRate: props.channel.commissionRate || 0,
      monthlyTarget: props.channel.monthlyTarget || 0,
      sortOrder: props.channel.sortOrder || 0
    }
  } else {
    formData.value = {
      name: '',
      code: '',
      description: '',
      type: '' as ChannelType | '',
      contactName: '',
      contactPhone: '',
      contactEmail: '',
      address: '',
      website: '',
      commissionRate: 0,
      monthlyTarget: 0,
      sortOrder: 0
    }
  }

  // 清空错误信息
  errors.value = {
    name: '',
    code: '',
    description: '',
    type: '',
    contactEmail: '',
    website: '',
    commissionRate: '',
    monthlyTarget: ''
  }
}

// 监听 props 变化
watch(() => props.channel, initFormData, { immediate: true })

onMounted(() => {
  initFormData()
})
</script>