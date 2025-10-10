import { defineComponent, h, Teleport } from 'vue'

// 基本的Dialog组件
export const Dialog = defineComponent({
  name: 'Dialog',
  props: {
    open: Boolean,
    onOpenChange: Function
  },
  setup(props, { slots }) {
    return () => slots.default?.()
  }
})

// DialogRoot - 通常是Dialog的别名
export const DialogRoot = Dialog

export const DialogContent = defineComponent({
  name: 'DialogContent',
  props: {
    class: String
  },
  setup(props, { slots }) {
    return () => h(Teleport, { to: 'body' }, [
      h('div', {
        class: 'fixed inset-0 z-50 flex items-center justify-center bg-black/50'
      }, [
        h('div', {
          class: `bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4 ${props.class || ''}`
        }, slots.default?.())
      ])
    ])
  }
})

export const DialogHeader = defineComponent({
  name: 'DialogHeader',
  setup(props, { slots }) {
    return () => h('div', { class: 'mb-4' }, slots.default?.())
  }
})

export const DialogTitle = defineComponent({
  name: 'DialogTitle',
  setup(props, { slots }) {
    return () => h('h2', { class: 'text-lg font-semibold' }, slots.default?.())
  }
})

export const DialogDescription = defineComponent({
  name: 'DialogDescription',
  setup(props, { slots }) {
    return () => h('p', { class: 'text-sm text-gray-600' }, slots.default?.())
  }
})

export const DialogFooter = defineComponent({
  name: 'DialogFooter',
  setup(props, { slots }) {
    return () => h('div', { class: 'mt-6 flex justify-end gap-2' }, slots.default?.())
  }
})

export const DialogClose = defineComponent({
  name: 'DialogClose',
  setup(props, { slots }) {
    return () => slots.default?.()
  }
})

export const DialogOverlay = defineComponent({
  name: 'DialogOverlay',
  setup(props, { slots }) {
    return () => h('div', {
      class: 'fixed inset-0 z-50 bg-black/50'
    }, slots.default?.())
  }
})

export const DialogPortal = defineComponent({
  name: 'DialogPortal',
  setup(props, { slots }) {
    return () => h(Teleport as any, { to: 'body' }, slots.default)
  }
})

export const DialogTrigger = defineComponent({
  name: 'DialogTrigger',
  setup(props, { slots }) {
    return () => slots.default?.()
  }
})