<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import {
  SelectValue,
  type SelectValueProps,
} from 'reka-ui'
import { cn } from '@/lib/utils'

interface Props extends SelectValueProps {
  class?: HTMLAttributes['class']
}

const props = defineProps<Props>()
</script>

<template>
  <SelectValue
    v-bind="props"
    :class="cn('text-sm', props.class)"
  />
</template>