<template>
  <div class="code-block" :class="{ collapsible: collapsible }">
    <!-- 头部工具栏 -->
    <div v-if="showHeader" class="code-header">
      <div class="code-info">
        <span v-if="language" class="language-tag">{{ languageDisplay }}</span>
        <span v-if="title" class="code-title">{{ title }}</span>
      </div>
      <div class="code-actions">
        <button
          v-if="collapsible"
          @click="toggleCollapse"
          class="action-btn"
          :title="isCollapsed ? '展开' : '收起'"
        >
          <svg
            class="w-4 h-4 transition-transform"
            :class="{ 'rotate-180': isCollapsed }"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
        <button
          v-if="copyable"
          @click="copyCode"
          class="action-btn"
          :title="copySuccess ? '已复制' : '复制代码'"
        >
          <svg
            v-if="!copySuccess"
            class="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
          </svg>
          <svg
            v-else
            class="w-4 h-4 text-green-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 代码内容 -->
    <div v-show="!isCollapsed" class="code-content">
      <pre ref="codeRef" :class="codeClass"><code ref="codeElementRef" :class="languageClass">{{ formattedCode }}</code></pre>
    </div>

    <!-- 行数指示器 -->
    <div v-if="showLineNumbers && !isCollapsed" class="line-numbers">
      <span
        v-for="lineNum in lineCount"
        :key="lineNum"
        class="line-number"
      >
        {{ lineNum }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { cn } from '@/lib/utils'

interface Props {
  // 代码内容
  code: string
  // 编程语言
  language?: string
  // 标题
  title?: string
  // 是否显示行号
  showLineNumbers?: boolean
  // 是否可复制
  copyable?: boolean
  // 是否可折叠
  collapsible?: boolean
  // 默认是否折叠
  defaultCollapsed?: boolean
  // 最大高度（超出时显示滚动条）
  maxHeight?: string
  // 主题
  theme?: 'light' | 'dark' | 'auto'
  // 自定义类名
  class?: string
  // 是否显示头部
  showHeader?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showLineNumbers: false,
  copyable: true,
  collapsible: false,
  defaultCollapsed: false,
  theme: 'auto',
  showHeader: true,
  maxHeight: '400px'
})

// 响应式数据
const isCollapsed = ref(props.defaultCollapsed)
const copySuccess = ref(false)
const codeRef = ref<HTMLElement>()
const codeElementRef = ref<HTMLElement>()

// 计算属性
const formattedCode = computed(() => {
  if (!props.code) return ''

  try {
    // 如果是JSON，尝试格式化
    if (props.language === 'json') {
      const parsed = JSON.parse(props.code)
      return JSON.stringify(parsed, null, 2)
    }
    return props.code
  } catch {
    return props.code
  }
})

const lineCount = computed(() => {
  return formattedCode.value.split('\n').length
})

const languageDisplay = computed(() => {
  const languageMap: Record<string, string> = {
    'javascript': 'JavaScript',
    'typescript': 'TypeScript',
    'vue': 'Vue',
    'html': 'HTML',
    'css': 'CSS',
    'json': 'JSON',
    'xml': 'XML',
    'sql': 'SQL',
    'bash': 'Bash',
    'shell': 'Shell',
    'python': 'Python',
    'java': 'Java',
    'go': 'Go',
    'rust': 'Rust',
    'php': 'PHP',
    'ruby': 'Ruby',
    'c': 'C',
    'cpp': 'C++',
    'csharp': 'C#',
    'swift': 'Swift',
    'kotlin': 'Kotlin'
  }
  return languageMap[props.language || ''] || props.language?.toUpperCase() || 'TEXT'
})

const languageClass = computed(() => {
  return props.language ? `language-${props.language}` : ''
})

const codeClass = computed(() => {
  return cn(
    'code-block-pre',
    `theme-${props.theme}`,
    props.class
  )
})

// 方法
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const copyCode = async () => {
  try {
    await navigator.clipboard.writeText(formattedCode.value)
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (error) {
    console.error('复制失败:', error)
    // 降级方案：选择文本
    if (codeElementRef.value) {
      const range = document.createRange()
      range.selectNodeContents(codeElementRef.value)
      const selection = window.getSelection()
      selection?.removeAllRanges()
      selection?.addRange(range)
    }
  }
}

// 生命周期
onMounted(() => {
  // 这里可以集成代码高亮库，如 Prism.js 或 highlight.js
  // 为了简化，暂时使用基础样式
  nextTick(() => {
    // 可以在这里添加语法高亮逻辑
  })
})
</script>

<style scoped>
.code-block {
  @apply relative bg-gray-50 border border-gray-200 rounded-lg overflow-hidden;
}

.code-block.collapsible {
  @apply transition-all duration-200;
}

.code-header {
  @apply flex items-center justify-between px-4 py-2 bg-gray-100 border-b border-gray-200;
}

.code-info {
  @apply flex items-center space-x-2;
}

.language-tag {
  @apply inline-flex items-center px-2 py-1 text-xs font-medium bg-green-100 text-green-800 rounded-md;
}

.code-title {
  @apply text-sm font-medium text-gray-700;
}

.code-actions {
  @apply flex items-center space-x-1;
}

.action-btn {
  @apply p-1 text-gray-500 hover:text-gray-700 hover:bg-gray-200 rounded transition-colors;
}

.code-content {
  @apply relative overflow-auto;
  max-height: v-bind(maxHeight);
}

.code-block-pre {
  @apply p-4 m-0 text-sm font-mono bg-transparent overflow-auto whitespace-pre-wrap break-words;
  line-height: 1.5;
}

.line-numbers {
  @apply absolute left-0 top-0 flex flex-col items-end pt-4 pr-2 text-xs font-mono text-gray-400 bg-gray-100 border-r border-gray-200;
  width: 3rem;
}

.line-number {
  @apply block leading-6;
}

/* 当显示行号时，调整代码内容的左边距 */
.code-block:has(.line-numbers) .code-block-pre {
  @apply pl-16;
}

/* 主题样式 */
.theme-light {
  @apply bg-white text-gray-900;
}

.theme-dark {
  @apply bg-gray-900 text-gray-100;
}

.theme-dark .code-block {
  @apply bg-gray-900 border-gray-700;
}

.theme-dark .code-header {
  @apply bg-gray-800 border-gray-700;
}

.theme-dark .language-tag {
  @apply bg-green-900 text-green-300;
}

.theme-dark .code-title {
  @apply text-gray-300;
}

.theme-dark .action-btn {
  @apply text-gray-400 hover:text-gray-200 hover:bg-gray-700;
}

.theme-dark .line-numbers {
  @apply text-gray-500 bg-gray-800 border-gray-700;
}

/* 自动主题（根据系统偏好） */
@media (prefers-color-scheme: dark) {
  .theme-auto {
    @apply bg-gray-900 text-gray-100;
  }

  .theme-auto .code-block {
    @apply bg-gray-900 border-gray-700;
  }

  .theme-auto .code-header {
    @apply bg-gray-800 border-gray-700;
  }

  .theme-auto .language-tag {
    @apply bg-green-900 text-green-300;
  }

  .theme-auto .code-title {
    @apply text-gray-300;
  }

  .theme-auto .action-btn {
    @apply text-gray-400 hover:text-gray-200 hover:bg-gray-700;
  }

  .theme-auto .line-numbers {
    @apply text-gray-500 bg-gray-800 border-gray-700;
  }
}

/* 语法高亮基础样式 */
:deep(.language-json) {
  @apply text-blue-600;
}

:deep(.language-javascript),
:deep(.language-typescript) {
  @apply text-yellow-600;
}

:deep(.language-html) {
  @apply text-red-600;
}

:deep(.language-css) {
  @apply text-purple-600;
}

:deep(.language-vue) {
  @apply text-green-600;
}

/* 滚动条样式 */
.code-content::-webkit-scrollbar {
  @apply w-2 h-2;
}

.code-content::-webkit-scrollbar-track {
  @apply bg-gray-100;
}

.code-content::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded;
}

.code-content::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}

/* 深色主题滚动条 */
.theme-dark .code-content::-webkit-scrollbar-track,
.theme-auto .code-content::-webkit-scrollbar-track {
  @apply bg-gray-800;
}

.theme-dark .code-content::-webkit-scrollbar-thumb,
.theme-auto .code-content::-webkit-scrollbar-thumb {
  @apply bg-gray-600;
}

.theme-dark .code-content::-webkit-scrollbar-thumb:hover,
.theme-auto .code-content::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-500;
}
</style>