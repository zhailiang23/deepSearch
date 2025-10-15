<template>
  <el-dialog
    v-model="dialogVisible"
    title="识别提示词配置"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-if="loading" class="flex items-center justify-center py-8">
      <el-icon class="is-loading text-2xl">
        <span class="loading-icon">⟳</span>
      </el-icon>
      <span class="ml-2 text-gray-600">加载中...</span>
    </div>

    <el-form
      v-else
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      class="mt-4"
    >
      <el-form-item label="配置名称" prop="configName">
        <el-input
          v-model="formData.configName"
          placeholder="请输入配置名称"
          :disabled="true"
        />
      </el-form-item>

      <el-form-item label="配置键名" prop="configKey">
        <el-input
          v-model="formData.configKey"
          placeholder="请输入配置键名"
          :disabled="true"
        />
      </el-form-item>

      <el-form-item label="提示词内容" prop="promptContent">
        <el-input
          v-model="formData.promptContent"
          type="textarea"
          :rows="15"
          placeholder="请输入提示词内容"
          style="font-family: 'Monaco', 'Menlo', 'Consolas', monospace; font-size: 13px;"
        />
      </el-form-item>

      <el-form-item label="配置描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入配置描述（可选）"
        />
      </el-form-item>

      <el-form-item label="启用状态" prop="enabled">
        <el-switch v-model="formData.enabled" />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-between items-center">
        <el-button
          type="info"
          plain
          @click="handleReset"
        >
          恢复默认
        </el-button>
        <div>
          <el-button @click="handleClose">取消</el-button>
          <el-button
            type="primary"
            :loading="saving"
            @click="handleSave"
          >
            保存
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getPromptConfigByKey, updatePromptConfig, type PromptConfig } from '@/api/promptConfig'

const props = defineProps<{
  visible: boolean
  configKey?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'saved': []
}>()

const dialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const DEFAULT_CONFIG_KEY = 'image_recognition_default'

const formData = reactive<PromptConfig>({
  configKey: DEFAULT_CONFIG_KEY,
  configName: '图片识别默认提示词',
  promptContent: '',
  description: '',
  enabled: true
})

const defaultPromptContent = `你是一个专业的图片文字识别助手。请仔细识别图片中的所有文字内容,并严格按照以下JSON格式返回结果。

**重要要求:**
1. 必须返回有效的JSON格式数据
2. 不要添加任何解释性文字
3. 不要使用markdown代码块标记
4. 直接返回纯JSON对象

**JSON格式要求:**
{
  "name": "从图片中识别到的活动名称,如果无法识别则填写'未识别到活动名称'",
  "descript": "活动描述,如果无法直接识别则根据活动名称和其他文字信息生成简洁描述(不超过100字)",
  "link": "活动链接地址,如果无法识别则填写空字符串",
  "startDate": "活动开始时间,格式YYYY-MM-DD,如果无法识别则填写空字符串",
  "endDate": "活动结束时间,格式YYYY-MM-DD,如果无法识别则填写空字符串",
  "status": "活动状态(如:进行中、即将开始、已结束、已取消),如果无法识别则填写'未知'",
  "all": "图片中识别到的所有文字内容,保持原始格式和换行"
}

请严格按照上述JSON格式返回识别结果:`

const rules: FormRules = {
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' }
  ],
  configKey: [
    { required: true, message: '请输入配置键名', trigger: 'blur' }
  ],
  promptContent: [
    { required: true, message: '请输入提示词内容', trigger: 'blur' }
  ]
}

watch(() => props.visible, async (val) => {
  console.log('PromptConfigDialog: props.visible changed to', val)
  dialogVisible.value = val
  if (val) {
    console.log('PromptConfigDialog: loading config...')
    await loadConfig()
  }
}, { immediate: true })

watch(dialogVisible, (val) => {
  if (!val) {
    emit('update:visible', false)
  }
})

const loadConfig = async () => {
  loading.value = true
  try {
    const configKey = props.configKey || DEFAULT_CONFIG_KEY
    const config = await getPromptConfigByKey(configKey)

    formData.id = config.id
    formData.configKey = config.configKey
    formData.configName = config.configName
    formData.promptContent = config.promptContent
    formData.description = config.description
    formData.enabled = config.enabled
  } catch (error: any) {
    console.error('加载配置失败:', error)
    ElMessage.error(error.response?.data || '加载配置失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formData.promptContent = defaultPromptContent
  ElMessage.success('已恢复为默认提示词')
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      if (!formData.id) {
        ElMessage.error('配置ID不存在')
        return
      }

      await updatePromptConfig(formData.id, formData)
      ElMessage.success('保存成功')
      emit('saved')
      handleClose()
    } catch (error: any) {
      console.error('保存配置失败:', error)
      ElMessage.error(error.response?.data || '保存配置失败')
    } finally {
      saving.value = false
    }
  })
}

const handleClose = () => {
  dialogVisible.value = false
}
</script>

<style scoped>
:deep(.el-textarea__inner) {
  line-height: 1.6;
}
</style>
