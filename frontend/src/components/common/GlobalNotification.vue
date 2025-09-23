<template>
  <Teleport to="body">
    <div
      v-if="notification.visible"
      class="fixed top-4 right-4 z-50 max-w-sm w-full"
    >
      <div
        :class="[
          'p-4 rounded-lg shadow-lg border',
          notification.type === 'error'
            ? 'bg-red-50 border-red-200 text-red-800 dark:bg-red-900 dark:border-red-700 dark:text-red-200'
            : notification.type === 'warning'
            ? 'bg-yellow-50 border-yellow-200 text-yellow-800 dark:bg-yellow-900 dark:border-yellow-700 dark:text-yellow-200'
            : 'bg-green-50 border-green-200 text-green-800 dark:bg-green-900 dark:border-green-700 dark:text-green-200'
        ]"
      >
        <div class="flex items-start">
          <div class="flex-shrink-0">
            <AlertCircle v-if="notification.type === 'error'" class="h-5 w-5" />
            <AlertTriangle v-else-if="notification.type === 'warning'" class="h-5 w-5" />
            <CheckCircle v-else class="h-5 w-5" />
          </div>
          <div class="ml-3 flex-1">
            <p class="text-sm font-medium">
              {{ notification.message }}
            </p>
          </div>
          <div class="ml-4 flex-shrink-0">
            <button
              @click="hideNotification"
              class="rounded-md inline-flex text-gray-400 hover:text-gray-600 focus:outline-none"
            >
              <X class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { AlertCircle, AlertTriangle, CheckCircle, X } from 'lucide-vue-next'

interface NotificationState {
  visible: boolean
  message: string
  type: 'error' | 'warning' | 'success'
}

const notification = ref<NotificationState>({
  visible: false,
  message: '',
  type: 'error'
})

let hideTimer: number | null = null

const showNotification = (message: string, type: 'error' | 'warning' | 'success' = 'error', duration = 5000) => {
  notification.value = {
    visible: true,
    message,
    type
  }

  if (hideTimer) {
    clearTimeout(hideTimer)
  }

  hideTimer = setTimeout(() => {
    hideNotification()
  }, duration)
}

const hideNotification = () => {
  notification.value.visible = false
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
}

const handleAuthError = (event: CustomEvent) => {
  const { type, message } = event.detail

  if (type === 'forbidden') {
    showNotification(message, 'error')
  }
}

onMounted(() => {
  window.addEventListener('auth-error', handleAuthError as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('auth-error', handleAuthError as EventListener)
  if (hideTimer) {
    clearTimeout(hideTimer)
  }
})

// 暴露方法供外部调用
defineExpose({
  showNotification,
  hideNotification
})
</script>