<template>
  <span
    :class="[
      'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
      statusClasses
    ]"
  >
    <component
      :is="statusIcon"
      class="w-3 h-3 mr-1.5 flex-shrink-0"
      v-if="statusIcon"
    />
    {{ statusText }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CheckCircle, XCircle, PauseCircle, Trash2 } from 'lucide-vue-next'
import type { ChannelStatus } from '@/types/channel'

interface Props {
  status: ChannelStatus
}

const props = defineProps<Props>()

const statusConfig = computed(() => {
  switch (props.status) {
    case 'ACTIVE':
      return {
        text: '激活',
        icon: CheckCircle,
        classes: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
      }
    case 'INACTIVE':
      return {
        text: '未激活',
        icon: XCircle,
        classes: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400'
      }
    case 'SUSPENDED':
      return {
        text: '暂停',
        icon: PauseCircle,
        classes: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400'
      }
    case 'DELETED':
      return {
        text: '已删除',
        icon: Trash2,
        classes: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400'
      }
    default:
      return {
        text: '未知',
        icon: null,
        classes: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400'
      }
  }
})

const statusText = computed(() => statusConfig.value.text)
const statusIcon = computed(() => statusConfig.value.icon)
const statusClasses = computed(() => statusConfig.value.classes)
</script>