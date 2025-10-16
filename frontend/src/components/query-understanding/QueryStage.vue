<template>
  <div class="flex items-center gap-3 p-3 border rounded-lg" :class="colorClasses">
    <div class="flex-shrink-0 w-24 text-sm font-medium" :class="labelColorClass">
      {{ label }}
    </div>
    <div class="flex-1 font-medium text-gray-900">
      {{ value }}
    </div>
    <div class="flex-shrink-0">
      <svg class="w-5 h-5" :class="arrowColorClass" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  label: string
  value: string
  color?: 'blue' | 'green' | 'purple' | 'orange' | 'emerald'
}

const props = withDefaults(defineProps<Props>(), {
  color: 'blue'
})

const colorClasses = computed(() => {
  const colorMap = {
    blue: 'bg-blue-50 border-blue-200',
    green: 'bg-green-50 border-green-200',
    purple: 'bg-purple-50 border-purple-200',
    orange: 'bg-orange-50 border-orange-200',
    emerald: 'bg-emerald-50 border-emerald-200'
  }
  return colorMap[props.color]
})

const labelColorClass = computed(() => {
  const colorMap = {
    blue: 'text-blue-700',
    green: 'text-green-700',
    purple: 'text-purple-700',
    orange: 'text-orange-700',
    emerald: 'text-emerald-700'
  }
  return colorMap[props.color]
})

const arrowColorClass = computed(() => {
  const colorMap = {
    blue: 'text-blue-500',
    green: 'text-green-500',
    purple: 'text-purple-500',
    orange: 'text-orange-500',
    emerald: 'text-emerald-500'
  }
  return colorMap[props.color]
})
</script>
