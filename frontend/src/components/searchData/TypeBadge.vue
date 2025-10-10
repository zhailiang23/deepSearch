<template>
  <span
    :class="[
      'inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium',
      badgeClass
    ]"
    :title="typeDescription"
  >
    <component
      :is="typeIcon"
      class="w-3 h-3 mr-1"
    />
    {{ typeLabel }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { 
  Type, 
  Hash, 
  Calendar, 
  Hash as Number,
  ToggleLeft,
  Braces,
  List,
  FileText
} from 'lucide-vue-next'

interface Props {
  type: 'text' | 'keyword' | 'date' | 'number' | 'boolean' | 'object' | 'nested' | string
}

const props = defineProps<Props>()

// 类型样式映射
const badgeClasses = {
  text: 'bg-blue-100 text-blue-800 border border-blue-200',
  keyword: 'bg-purple-100 text-purple-800 border border-purple-200',
  date: 'bg-green-100 text-green-800 border border-green-200',
  number: 'bg-orange-100 text-orange-800 border border-orange-200',
  boolean: 'bg-yellow-100 text-yellow-800 border border-yellow-200',
  object: 'bg-indigo-100 text-indigo-800 border border-indigo-200',
  nested: 'bg-pink-100 text-pink-800 border border-pink-200',
  default: 'bg-gray-100 text-gray-800 border border-gray-200'
}

// 类型图标映射
const typeIcons = {
  text: FileText,
  keyword: Hash,
  date: Calendar,
  number: Hash,
  boolean: ToggleLeft,
  object: Braces,
  nested: List,
  default: Type
}

// 类型标签映射
const typeLabels = {
  text: '文本',
  keyword: '关键词',
  date: '日期',
  number: '数字',
  boolean: '布尔',
  object: '对象',
  nested: '嵌套',
  default: '未知'
}

// 类型描述映射
const typeDescriptions = {
  text: '全文索引字段，支持分词搜索',
  keyword: '精确匹配字段，不分词，支持聚合和排序',
  date: '日期时间字段，支持日期范围查询',
  number: '数值字段，支持数值范围查询和数学聚合',
  boolean: '布尔字段，true/false 值',
  object: '对象字段，包含嵌套的键值对',
  nested: '嵌套对象数组，支持复杂查询',
  default: '未知类型字段'
}

// 计算属性
const badgeClass = computed(() => {
  const key = props.type as keyof typeof badgeClasses
  return badgeClasses[key] || badgeClasses.default
})

const typeLabel = computed(() => {
  return typeLabels[props.type as keyof typeof typeLabels] || typeLabels.default
})

const typeIcon = computed(() => {
  return typeIcons[props.type as keyof typeof typeIcons] || typeIcons.default
})

const typeDescription = computed(() => {
  return typeDescriptions[props.type as keyof typeof typeDescriptions] || typeDescriptions.default
})
</script>

<style scoped>
/* 悬停效果 */
.inline-flex:hover {
  transform: scale(1.05);
  transition: transform 0.2s ease-in-out;
}

/* 动画效果 */
.inline-flex {
  transition: all 0.2s ease-in-out;
}

/* 针对不同类型的特殊样式 */
.bg-blue-100:hover {
  background-color: rgb(219 234 254);
}

.bg-purple-100:hover {
  background-color: rgb(243 232 255);
}

.bg-green-100:hover {
  background-color: rgb(220 252 231);
}

.bg-orange-100:hover {
  background-color: rgb(255 237 213);
}

.bg-yellow-100:hover {
  background-color: rgb(254 249 195);
}

.bg-indigo-100:hover {
  background-color: rgb(224 231 255);
}

.bg-pink-100:hover {
  background-color: rgb(252 231 243);
}

.bg-gray-100:hover {
  background-color: rgb(243 244 246);
}
</style>