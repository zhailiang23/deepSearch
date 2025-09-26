<template>
  <div class="mapping-config-tab h-full flex flex-col">
    <!-- 工具栏 -->
    <div class="flex-shrink-0 p-3 sm:p-4 bg-muted/30 dark:bg-muted/30 border-b border-border">
      <!-- 移动端布局：垂直排列 -->
      <div class="block sm:hidden space-y-3">
        <div class="flex items-center justify-between">
          <h4 class="font-semibold text-foreground text-sm">
            JSON 映射配置
          </h4>
          <div class="flex items-center space-x-2">
            <button
              @click="handleReset"
              :disabled="!hasChanges || loading"
              class="px-2 py-1 text-xs font-medium text-muted-foreground bg-background border border-border rounded hover:bg-muted hover:text-foreground focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
            >
              重置
            </button>
            <button
              @click="handleSave"
              :disabled="!isValidJson || !hasChanges || loading"
              class="px-2 py-1 text-xs font-medium text-primary-foreground bg-primary border border-primary rounded hover:bg-primary/90 focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
            >
              <span class="flex items-center space-x-1">
                <span>{{ loading ? '保存中' : '保存' }}</span>
                <div
                  v-if="loading"
                  class="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin"
                />
              </span>
            </button>
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <div
            :class="[
              'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border',
              isValidJson
                ? 'bg-primary/10 text-primary border-primary/20 dark:bg-primary/20 dark:text-primary dark:border-primary/30'
                : 'bg-destructive/10 text-destructive border-destructive/20 dark:bg-destructive/20 dark:text-destructive dark:border-destructive/30'
            ]"
          >
            <div
              :class="[
                'w-1.5 h-1.5 rounded-full mr-1',
                isValidJson ? 'bg-primary' : 'bg-destructive'
              ]"
            />
            {{ isValidJson ? '有效' : '无效' }}
          </div>
          <div
            v-if="hasChanges"
            class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border bg-yellow-50 text-yellow-700 border-yellow-200 dark:bg-yellow-950/50 dark:text-yellow-300 dark:border-yellow-800/50"
          >
            <div class="w-1.5 h-1.5 rounded-full mr-1 bg-yellow-500 dark:bg-yellow-400" />
            未保存
          </div>
        </div>
      </div>

      <!-- 桌面端布局：水平排列 -->
      <div class="hidden sm:flex items-center justify-between">
        <div class="flex items-center space-x-4">
          <h4 class="font-semibold text-foreground">
            JSON 映射配置
          </h4>
          <div class="flex items-center space-x-2">
            <div
              :class="[
                'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border',
                isValidJson
                  ? 'bg-primary/10 text-primary border-primary/20 dark:bg-primary/20 dark:text-primary dark:border-primary/30'
                  : 'bg-destructive/10 text-destructive border-destructive/20 dark:bg-destructive/20 dark:text-destructive dark:border-destructive/30'
              ]"
            >
              <div
                :class="[
                  'w-2 h-2 rounded-full mr-1.5',
                  isValidJson ? 'bg-primary' : 'bg-destructive'
                ]"
              />
              {{ isValidJson ? '有效 JSON' : '无效 JSON' }}
            </div>
            <div
              v-if="hasChanges"
              class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border bg-yellow-50 text-yellow-700 border-yellow-200 dark:bg-yellow-950/50 dark:text-yellow-300 dark:border-yellow-800/50"
            >
              <div class="w-2 h-2 rounded-full mr-1.5 bg-yellow-500 dark:bg-yellow-400" />
              有未保存更改
            </div>
          </div>
        </div>

        <div class="flex items-center space-x-3">
          <button
            @click="handleReset"
            :disabled="!hasChanges || loading"
            class="px-4 py-2 text-sm font-medium text-muted-foreground bg-background border border-border rounded-md hover:bg-muted hover:text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 focus:ring-offset-background disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
          >
            重置
          </button>
          <button
            @click="handleSave"
            :disabled="!isValidJson || !hasChanges || loading"
            class="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary border border-primary rounded-md hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 focus:ring-offset-background disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-sm"
          >
            <span class="flex items-center space-x-2">
              <span>{{ loading ? '保存中...' : '保存配置' }}</span>
              <div
                v-if="loading"
                class="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin"
              />
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- 编辑器容器 -->
    <div class="flex-grow flex flex-col overflow-hidden">
      <div class="flex-grow relative">
        <div
          ref="editorContainer"
          class="absolute inset-0 border border-border bg-background rounded-none overflow-hidden"
        />
      </div>
    </div>

    <!-- 错误信息面板 -->
    <div
      v-if="!isValidJson && jsonError"
      class="flex-shrink-0 bg-destructive/5 dark:bg-destructive/10 border-t border-destructive/20 dark:border-destructive/30 p-3 sm:p-4"
    >
      <div class="flex items-start space-x-2 sm:space-x-3">
        <AlertCircle class="h-4 w-4 sm:h-5 sm:w-5 text-destructive flex-shrink-0 mt-0.5" />
        <div class="flex-grow min-w-0">
          <h5 class="text-xs sm:text-sm font-semibold text-destructive mb-2">
            JSON 格式错误
          </h5>
          <div class="bg-background/50 border border-destructive/20 rounded-md p-2 sm:p-3">
            <p class="text-xs sm:text-sm text-muted-foreground font-mono break-words whitespace-pre-wrap overflow-x-auto">
              {{ jsonError }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState, StateEffect } from '@codemirror/state'
import { json } from '@codemirror/lang-json'
import { oneDark } from '@codemirror/theme-one-dark'
import { AlertCircle } from 'lucide-vue-next'
import { useSearchSpaceStore } from '@/stores/searchSpace'

interface Props {
  /**
   * 搜索空间 ID（必需，用于获取和保存映射配置）
   */
  spaceId: number
  /**
   * 初始 JSON 配置内容（可选，如果不提供将自动从 API 加载）
   */
  modelValue?: string
  /**
   * 是否为只读模式
   */
  readonly?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'save', value: string): void
  (e: 'validate', isValid: boolean): void
  (e: 'load-error', error: string): void
  (e: 'save-error', error: string): void
  (e: 'save-success'): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  readonly: false
})

const emit = defineEmits<Emits>()

// Pinia store
const searchSpaceStore = useSearchSpaceStore()

// 编辑器相关
const editorContainer = ref<HTMLElement>()
let editorView: EditorView | null = null

// 状态管理
const currentValue = ref(props.modelValue)
const originalValue = ref(props.modelValue)
const isValidJson = ref(true)
const jsonError = ref('')
const isInitialized = ref(false)

// 计算属性
const hasChanges = computed(() => {
  return currentValue.value !== originalValue.value
})

const loading = computed(() => searchSpaceStore.mappingLoading)
const mappingError = computed(() => searchSpaceStore.mappingError)
const currentMapping = computed(() => searchSpaceStore.currentMapping)

// JSON 验证函数
const validateJson = (value: string): { isValid: boolean; error: string } => {
  if (!value.trim()) {
    return { isValid: true, error: '' }
  }

  try {
    JSON.parse(value)
    return { isValid: true, error: '' }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error'
    return { isValid: false, error: errorMessage }
  }
}

// 更新验证状态
const updateValidation = (value: string) => {
  const validation = validateJson(value)
  isValidJson.value = validation.isValid
  jsonError.value = validation.error
  emit('validate', validation.isValid)

  // 更新编辑器验证样式
  updateEditorValidationStyles(validation.isValid)
}

// 创建编辑器主题
const createEditorTheme = () => {
  const isDark = document.documentElement.classList.contains('dark')

  return EditorView.theme({
    '&': {
      height: '100%',
      fontSize: '14px',
      backgroundColor: isDark ? 'hsl(142.2 84% 4.9%)' : 'hsl(0 0% 100%)',
      color: isDark ? 'hsl(138 40% 98%)' : 'hsl(142.1 84% 4.9%)'
    },
    '.cm-content': {
      padding: '12px',
      minHeight: '100%',
      color: 'inherit',
      caretColor: isDark ? 'hsl(142.1 91.2% 59.8%)' : 'hsl(142.1 76.2% 36.3%)'
    },
    '@media (min-width: 640px)': {
      '.cm-content': {
        padding: '16px'
      }
    },
    '.cm-focused': {
      outline: 'none'
    },
    '.cm-editor': {
      height: '100%',
      backgroundColor: 'inherit'
    },
    '.cm-editor.cm-focused': {
      outline: `2px solid ${isDark ? 'hsl(142.1 91.2% 59.8%)' : 'hsl(142.1 76.2% 36.3%)'}`,
      outlineOffset: '-1px'
    },
    '.cm-scroller': {
      fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Monaco, Consolas, "Liberation Mono", "Menlo", monospace',
      backgroundColor: 'inherit'
    },
    // 光标样式 - 使用系统主色
    '.cm-cursor': {
      borderLeftColor: isDark ? 'hsl(142.1 91.2% 59.8%)' : 'hsl(142.1 76.2% 36.3%)',
      borderLeftWidth: '2px'
    },
    // 选择背景 - 使用系统主色的透明版本
    '.cm-selectionBackground': {
      backgroundColor: isDark ? 'hsl(142.1 91.2% 59.8% / 0.15)' : 'hsl(142.1 76.2% 36.3% / 0.15)'
    },
    '&.cm-focused .cm-selectionBackground': {
      backgroundColor: isDark ? 'hsl(142.1 91.2% 59.8% / 0.25)' : 'hsl(142.1 76.2% 36.3% / 0.25)'
    },
    // 当前行高亮
    '.cm-activeLine': {
      backgroundColor: isDark ? 'hsl(142.2 32.6% 17.5% / 0.3)' : 'hsl(138 62% 96% / 0.5)'
    },
    '.cm-activeLineGutter': {
      backgroundColor: isDark ? 'hsl(142.2 32.6% 17.5% / 0.5)' : 'hsl(138 62% 96% / 0.8)',
      color: isDark ? 'hsl(142.1 91.2% 59.8%)' : 'hsl(142.1 76.2% 36.3%)'
    },
    // 行号样式
    '.cm-gutters': {
      backgroundColor: isDark ? 'hsl(142.2 32.6% 17.5%)' : 'hsl(138 62% 96%)',
      color: isDark ? 'hsl(142 20.2% 65.1%)' : 'hsl(142.4 16.3% 46.9%)',
      border: 'none',
      borderRight: `1px solid ${isDark ? 'hsl(142.2 32.6% 17.5%)' : 'hsl(142.3 31.8% 91.4%)'}`
    },
    '.cm-lineNumbers .cm-gutterElement': {
      padding: '0 8px 0 12px',
      minWidth: '40px'
    },
    // 括号匹配
    '.cm-matchingBracket': {
      backgroundColor: isDark ? 'hsl(142.1 91.2% 59.8% / 0.2)' : 'hsl(142.1 76.2% 36.3% / 0.2)',
      outline: `1px solid ${isDark ? 'hsl(142.1 91.2% 59.8%)' : 'hsl(142.1 76.2% 36.3%)'}`,
      color: 'inherit'
    },
    '.cm-nonmatchingBracket': {
      backgroundColor: isDark ? 'hsl(0 62.8% 30.6% / 0.2)' : 'hsl(0 84.2% 60.2% / 0.2)',
      outline: `1px solid ${isDark ? 'hsl(0 62.8% 30.6%)' : 'hsl(0 84.2% 60.2%)'}`,
      color: 'inherit'
    },
    // 搜索匹配
    '.cm-searchMatch': {
      backgroundColor: isDark ? 'hsl(45 93% 47% / 0.3)' : 'hsl(45 93% 47% / 0.2)',
      outline: `1px solid ${isDark ? 'hsl(45 93% 47%)' : 'hsl(45 93% 47%)'}`
    },
    '.cm-searchMatch.cm-searchMatch-selected': {
      backgroundColor: isDark ? 'hsl(45 93% 47% / 0.5)' : 'hsl(45 93% 47% / 0.4)'
    },
    // 错误状态 - JSON 无效时的样式
    '&.json-invalid': {
      '.cm-content': {
        borderLeft: `3px solid ${isDark ? 'hsl(0 62.8% 30.6%)' : 'hsl(0 84.2% 60.2%)'}`
      }
    }
  }, {
    dark: isDark
  })
}

// 初始化编辑器
const initializeEditor = async () => {
  if (!editorContainer.value) return

  const extensions = [
    basicSetup,
    json(),
    createEditorTheme(),
    EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        const newValue = update.state.doc.toString()
        currentValue.value = newValue
        updateValidation(newValue)
        emit('update:modelValue', newValue)

        // 根据 JSON 有效性更新编辑器样式
        updateEditorValidationStyles(validateJson(newValue).isValid)
      }
    }),
    EditorState.readOnly.of(props.readonly)
  ]

  const state = EditorState.create({
    doc: currentValue.value,
    extensions
  })

  editorView = new EditorView({
    state,
    parent: editorContainer.value
  })

  // 初始验证和样式更新
  const validation = validateJson(currentValue.value)
  updateValidation(currentValue.value)
  updateEditorValidationStyles(validation.isValid)
}

// 更新编辑器验证样式
const updateEditorValidationStyles = (isValid: boolean) => {
  if (!editorContainer.value) return

  const editorElement = editorContainer.value.querySelector('.cm-editor')
  if (editorElement) {
    if (isValid) {
      editorElement.classList.remove('json-invalid')
    } else {
      editorElement.classList.add('json-invalid')
    }
  }
}

// 重新配置编辑器主题（用于主题切换）
const reconfigureEditorTheme = () => {
  if (!editorView) return

  editorView.dispatch({
    effects: StateEffect.reconfigure.of([
      basicSetup,
      json(),
      createEditorTheme(),
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          const newValue = update.state.doc.toString()
          currentValue.value = newValue
          updateValidation(newValue)
          emit('update:modelValue', newValue)
          updateEditorValidationStyles(validateJson(newValue).isValid)
        }
      }),
      EditorState.readOnly.of(props.readonly)
    ])
  })
}

// 更新编辑器内容
const updateEditorContent = (newValue: string) => {
  if (!editorView) return

  const transaction = editorView.state.update({
    changes: {
      from: 0,
      to: editorView.state.doc.length,
      insert: newValue
    }
  })

  editorView.dispatch(transaction)
}

// 处理保存
const handleSave = async () => {
  if (!isValidJson.value || currentValue.value === originalValue.value) {
    return
  }

  try {
    await searchSpaceStore.updateMapping(props.spaceId, currentValue.value)
    originalValue.value = currentValue.value
    emit('save', currentValue.value)
    emit('save-success')
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '保存失败'
    emit('save-error', errorMessage)
  }
}

// 处理重置
const handleReset = () => {
  currentValue.value = originalValue.value
  updateEditorContent(originalValue.value)
  updateValidation(originalValue.value)
}

// 从 store 加载 mapping 配置
const loadMappingFromStore = async () => {
  try {
    const mapping = await searchSpaceStore.fetchMapping(props.spaceId)
    if (mapping) {
      // 尝试格式化 JSON
      let formattedMapping = mapping
      try {
        const parsed = JSON.parse(mapping)
        formattedMapping = JSON.stringify(parsed, null, 2)
      } catch {
        // 如果格式化失败，使用原始内容
      }

      currentValue.value = formattedMapping
      originalValue.value = formattedMapping
      updateEditorContent(formattedMapping)
      updateValidation(formattedMapping)
    }
    isInitialized.value = true
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '加载 mapping 配置失败'
    emit('load-error', errorMessage)
    isInitialized.value = true
  }
}

// 监听 props 变化
watch(() => props.modelValue, (newValue) => {
  if (newValue !== currentValue.value && isInitialized.value) {
    currentValue.value = newValue
    originalValue.value = newValue
    updateEditorContent(newValue)
    updateValidation(newValue)
  }
})

watch(() => props.spaceId, (newSpaceId) => {
  if (newSpaceId && newSpaceId > 0) {
    loadMappingFromStore()
  }
}, { immediate: true })

watch(() => props.readonly, (newReadonly) => {
  if (editorView) {
    editorView.dispatch({
      effects: StateEffect.reconfigure.of([
        EditorState.readOnly.of(newReadonly)
      ])
    })
  }
})

// 主题变化监听器
let themeObserver: MutationObserver | null = null

// 生命周期
onMounted(async () => {
  await nextTick()
  initializeEditor()

  // 如果没有提供 modelValue 且有 spaceId，则从 API 加载
  if (!props.modelValue && props.spaceId && props.spaceId > 0) {
    loadMappingFromStore()
  } else {
    isInitialized.value = true
  }

  // 监听主题变化
  themeObserver = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
        // 主题发生变化，重新配置编辑器
        setTimeout(() => {
          reconfigureEditorTheme()
        }, 50) // 给一些时间让 DOM 更新
      }
    })
  })

  // 观察 html 元素的 class 变化（dark 类的添加/移除）
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class']
  })
})

onUnmounted(() => {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }

  // 清理主题监听器
  if (themeObserver) {
    themeObserver.disconnect()
    themeObserver = null
  }
})

// 暴露给父组件的方法
defineExpose({
  /**
   * 获取当前编辑器内容
   */
  getValue: () => currentValue.value,
  /**
   * 设置编辑器内容
   */
  setValue: (value: string) => {
    currentValue.value = value
    originalValue.value = value
    updateEditorContent(value)
    updateValidation(value)
  },
  /**
   * 获取验证状态
   */
  getValidation: () => ({
    isValid: isValidJson.value,
    error: jsonError.value
  }),
  /**
   * 格式化 JSON
   */
  formatJson: () => {
    if (isValidJson.value && currentValue.value.trim()) {
      try {
        const formatted = JSON.stringify(JSON.parse(currentValue.value), null, 2)
        currentValue.value = formatted
        updateEditorContent(formatted)
      } catch (error) {
        console.error('格式化失败:', error)
      }
    }
  },
  /**
   * 检查是否有未保存的更改
   */
  hasUnsavedChanges: () => hasChanges.value,
  /**
   * 重新加载 mapping 配置
   */
  reload: () => loadMappingFromStore(),
  /**
   * 手动保存配置
   */
  save: () => handleSave()
})
</script>