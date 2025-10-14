<template>
  <el-dialog
    v-model="dialogVisible"
    title="编辑活动信息"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      label-width="100px"
      label-position="left"
    >
      <el-form-item label="活动名称" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="请输入活动名称"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="活动描述" prop="descript">
        <el-input
          v-model="formData.descript"
          type="textarea"
          :rows="4"
          placeholder="请输入活动描述"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="活动链接" prop="link">
        <el-input
          v-model="formData.link"
          placeholder="请输入活动链接"
          maxlength="500"
        />
      </el-form-item>

      <el-form-item label="开始日期" prop="startDate">
        <el-input
          v-model="formData.startDate"
          placeholder="格式: YYYY-MM-DD"
          maxlength="10"
        />
      </el-form-item>

      <el-form-item label="结束日期" prop="endDate">
        <el-input
          v-model="formData.endDate"
          placeholder="格式: YYYY-MM-DD"
          maxlength="10"
        />
      </el-form-item>

      <el-form-item label="活动状态" prop="status">
        <el-select v-model="formData.status" placeholder="请选择活动状态" style="width: 100%">
          <el-option label="进行中" value="进行中" />
          <el-option label="即将开始" value="即将开始" />
          <el-option label="已结束" value="已结束" />
          <el-option label="已取消" value="已取消" />
          <el-option label="未知" value="未知" />
        </el-select>
      </el-form-item>

      <el-form-item label="所有文本" prop="recognizedText">
        <el-input
          v-model="formData.recognizedText"
          type="textarea"
          :rows="6"
          placeholder="识别到的所有文本内容"
          readonly
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave" :disabled="!hasChanges">
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElButton } from 'element-plus'
import type { ImageRecognitionResult } from '@/api/batchConversion'

interface Props {
  open: boolean
  result: ImageRecognitionResult | null
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'save', result: ImageRecognitionResult): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

console.log('ActivityEditDialog 组件加载')

const formRef = ref()
const formData = ref({
  name: '',
  descript: '',
  link: '',
  startDate: '',
  endDate: '',
  status: '进行中',
  recognizedText: ''
})

const originalData = ref({
  name: '',
  descript: '',
  link: '',
  startDate: '',
  endDate: '',
  status: '进行中',
  recognizedText: ''
})

const dialogVisible = computed({
  get: () => {
    console.log('ActivityEditDialog dialogVisible getter, props.open =', props.open)
    return props.open
  },
  set: (value) => {
    console.log('ActivityEditDialog dialogVisible setter, value =', value)
    emit('update:open', value)
  }
})

const hasChanges = computed(() => {
  return (
    formData.value.name !== originalData.value.name ||
    formData.value.descript !== originalData.value.descript ||
    formData.value.link !== originalData.value.link ||
    formData.value.startDate !== originalData.value.startDate ||
    formData.value.endDate !== originalData.value.endDate ||
    formData.value.status !== originalData.value.status
  )
})

// 初始化表单数据
function initForm() {
  if (!props.result) return

  const data = {
    name: props.result.name || '',
    descript: props.result.descript || '',
    link: props.result.link || '',
    startDate: props.result.startDate || '',
    endDate: props.result.endDate || '',
    status: props.result.status || '进行中',
    recognizedText: props.result.recognizedText || ''
  }

  formData.value = { ...data }
  originalData.value = { ...data }
}

// 保存修改
function handleSave() {
  if (!props.result || !hasChanges.value) return

  // 创建更新后的结果对象
  const updatedResult: ImageRecognitionResult = {
    ...props.result,
    name: formData.value.name,
    descript: formData.value.descript,
    link: formData.value.link,
    startDate: formData.value.startDate,
    endDate: formData.value.endDate,
    status: formData.value.status
  }

  emit('save', updatedResult)
  ElMessage.success('修改已保存')
  handleClose()
}

// 关闭对话框
function handleClose() {
  dialogVisible.value = false
}

// 监听结果变化，重新初始化表单
watch(() => props.result, (newResult) => {
  if (newResult && props.open) {
    initForm()
  }
}, { immediate: true, deep: true })

// 监听对话框打开，重新初始化表单
watch(() => props.open, (open) => {
  if (open && props.result) {
    initForm()
  }
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
