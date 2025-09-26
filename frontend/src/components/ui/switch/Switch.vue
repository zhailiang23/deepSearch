<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import {
  SwitchRoot,
  SwitchThumb,
  type SwitchRootEmits,
  type SwitchRootProps,
} from 'reka-ui'
import { cn } from '@/lib/utils'

interface Props extends SwitchRootProps {
  class?: HTMLAttributes['class']
  checked?: boolean
}

const props = defineProps<Props>()

const emits = defineEmits<SwitchRootEmits & {
  'update:checked': [value: boolean]
}>()
</script>

<template>
  <SwitchRoot
    v-bind="{ ...props, modelValue: props.checked ?? props.modelValue }"
    :class="cn(
      'peer inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-emerald-600 data-[state=unchecked]:bg-input',
      props.class,
    )"
    @update:modelValue="(value) => {
      emits('update:modelValue', value)
      emits('update:checked', value)
    }"
  >
    <SwitchThumb
      :class="cn(
        'pointer-events-none block h-4 w-4 rounded-full bg-background shadow-lg ring-0 transition-transform data-[state=checked]:translate-x-4 data-[state=unchecked]:translate-x-0',
      )"
    />
  </SwitchRoot>
</template>