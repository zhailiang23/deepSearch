import { ref } from 'vue'

interface Toast {
  id: string
  title: string
  description?: string
  variant?: 'default' | 'destructive' | 'success'
}

const toasts = ref<Toast[]>([])

export function useToast() {
  const toast = ({ title, description, variant = 'default' }: Omit<Toast, 'id'>) => {
    const id = Date.now().toString()
    const newToast = { id, title, description, variant }
    toasts.value.push(newToast)

    // 3秒后自动移除
    setTimeout(() => {
      dismiss(id)
    }, 3000)

    return { id }
  }

  const dismiss = (toastId: string) => {
    const index = toasts.value.findIndex(t => t.id === toastId)
    if (index > -1) {
      toasts.value.splice(index, 1)
    }
  }

  return {
    toast,
    dismiss,
    toasts
  }
}

export { toasts }