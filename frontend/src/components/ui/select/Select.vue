<script setup lang="ts">
import {
  SelectRoot,
  type SelectRootEmits,
  type SelectRootProps,
} from 'reka-ui'

export interface SelectProps extends SelectRootProps {}
export interface SelectEmits extends SelectRootEmits {}

const props = defineProps<SelectProps>()
const emits = defineEmits<SelectEmits>()
</script>

<template>
  <SelectRoot v-bind="props" v-on="emits">
    <slot />
  </SelectRoot>
</template>