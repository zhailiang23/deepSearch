<template>
  <div class="p-4">
    <h2 class="text-lg font-semibold mb-4">CodeMirror 测试组件</h2>
    <div class="border rounded-lg overflow-hidden">
      <div ref="editorRef" class="min-h-[300px]"></div>
    </div>
    <div class="mt-4">
      <button
        @click="getValue"
        class="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
      >
        获取编辑器内容
      </button>
      <button
        @click="setValue"
        class="ml-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
      >
        设置示例JSON
      </button>
    </div>
    <div v-if="editorValue" class="mt-4 p-4 bg-gray-100 rounded">
      <h3 class="font-semibold mb-2">编辑器内容:</h3>
      <pre class="whitespace-pre-wrap">{{ editorValue }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { EditorView } from '@codemirror/view'
import { basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { json } from '@codemirror/lang-json'
import { oneDark } from '@codemirror/theme-one-dark'

const editorRef = ref<HTMLElement>()
const editorValue = ref('')
let editorView: EditorView | null = null

const sampleJSON = {
  "name": "CodeMirror 测试",
  "version": "1.0.0",
  "description": "这是一个 CodeMirror 编辑器测试",
  "features": [
    "JSON 语法高亮",
    "深色主题",
    "Vue 3 集成"
  ],
  "config": {
    "theme": "oneDark",
    "language": "json",
    "lineNumbers": true
  }
}

onMounted(() => {
  if (editorRef.value) {
    const state = EditorState.create({
      doc: JSON.stringify(sampleJSON, null, 2),
      extensions: [
        basicSetup,
        json(),
        oneDark,
        EditorView.theme({
          '&': {
            height: '300px'
          },
          '.cm-content': {
            padding: '12px'
          },
          '.cm-focused': {
            outline: 'none'
          },
          '.cm-scroller': {
            fontFamily: 'Monaco, Consolas, "Courier New", monospace'
          }
        })
      ]
    })

    editorView = new EditorView({
      state,
      parent: editorRef.value
    })
  }
})

onBeforeUnmount(() => {
  editorView?.destroy()
})

const getValue = () => {
  if (editorView) {
    editorValue.value = editorView.state.doc.toString()
  }
}

const setValue = () => {
  if (editorView) {
    const newContent = JSON.stringify(sampleJSON, null, 2)
    editorView.dispatch({
      changes: {
        from: 0,
        to: editorView.state.doc.length,
        insert: newContent
      }
    })
    editorValue.value = newContent
  }
}
</script>

<style scoped>
/* 确保编辑器样式正确渲染 */
:deep(.cm-editor) {
  border-radius: 0.5rem;
}

:deep(.cm-focused) {
  outline: none !important;
}
</style>