<template>
  <div
    data-slot="card"
    :class="cn(
      'bg-card text-card-foreground flex flex-col gap-6 rounded-xl border py-6 shadow-sm',
      $attrs.class as string
    )"
    v-bind="{ ...$attrs, class: undefined }"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { cn } from '@/lib/utils'
defineOptions({
  inheritAttrs: false
})
</script>