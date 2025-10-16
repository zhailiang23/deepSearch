<template>
  <div class="p-4 border rounded-lg" :class="colorClasses">
    <div class="flex items-center justify-between mb-2">
      <span class="text-2xl">{{ icon }}</span>
      <span class="text-2xl font-bold" :class="valueColorClass">{{ value }}</span>
    </div>
    <div class="text-sm font-medium" :class="labelColorClass">{{ label }}</div>
    <div v-if="subtitle" class="text-xs mt-1" :class="subtitleColorClass">
      {{ subtitle }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  label: string
  value: string | number
  icon: string
  color?: 'blue' | 'green' | 'purple' | 'red' | 'orange' | 'emerald'
  subtitle?: string
}

const props = withDefaults(defineProps<Props>(), {
  color: 'blue'
})

const colorClasses = computed(() => {
  const colorMap = {
    blue: 'bg-blue-50 border-blue-200',
    green: 'bg-green-50 border-green-200',
    purple: 'bg-purple-50 border-purple-200',
    red: 'bg-red-50 border-red-200',
    orange: 'bg-orange-50 border-orange-200',
    emerald: 'bg-emerald-50 border-emerald-200'
  }
  return colorMap[props.color]
})

const valueColorClass = computed(() => {
  const colorMap = {
    blue: 'text-blue-700',
    green: 'text-green-700',
    purple: 'text-purple-700',
    red: 'text-red-700',
    orange: 'text-orange-700',
    emerald: 'text-emerald-700'
  }
  return colorMap[props.color]
})

const labelColorClass = computed(() => {
  const colorMap = {
    blue: 'text-blue-600',
    green: 'text-green-600',
    purple: 'text-purple-600',
    red: 'text-red-600',
    orange: 'text-orange-600',
    emerald: 'text-emerald-600'
  }
  return colorMap[props.color]
})

const subtitleColorClass = computed(() => {
  const colorMap = {
    blue: 'text-blue-500',
    green: 'text-green-500',
    purple: 'text-purple-500',
    red: 'text-red-500',
    orange: 'text-orange-500',
    emerald: 'text-emerald-500'
  }
  return colorMap[props.color]
})
</script>
