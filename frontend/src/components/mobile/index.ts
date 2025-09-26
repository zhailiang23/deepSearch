// 移动端搜索组件统一导出文件
export { default as MobileSearchApp } from './MobileSearchApp.vue'
export { default as SearchInput } from './SearchInput.vue'
export { default as SearchResults } from './SearchResults.vue'
export { default as SearchResultItem } from './SearchResultItem.vue'
export { default as LoadingState } from './LoadingState.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as ErrorState } from './ErrorState.vue'

// 保持现有的移动端组件导出
export { default as PhoneSimulator } from './PhoneSimulator.vue'
export { default as DeviceFrame } from './DeviceFrame.vue'
export { default as StatusBar } from './StatusBar.vue'

// 导出类型定义
export type { MobileSearchAppProps } from './MobileSearchApp.vue'
export type { SearchInputProps } from './SearchInput.vue'
export type { SearchResultsProps } from './SearchResults.vue'
export type { SearchResultItemProps } from './SearchResultItem.vue'
export type { LoadingStateProps } from './LoadingState.vue'
export type { EmptyStateProps } from './EmptyState.vue'
export type { ErrorStateProps } from './ErrorState.vue'
export type { SearchResult } from './MobileSearchApp.vue'