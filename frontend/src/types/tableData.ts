// 表格数据相关类型定义
export interface TableColumn {
  key: string
  label: string
  type: 'text' | 'keyword' | 'date' | 'number' | 'boolean' | 'object' | 'nested'
  sortable: boolean
  filterable: boolean
  width?: number
  minWidth?: number
  maxWidth?: number
  fixed?: 'left' | 'right'
  visible: boolean
  resizable: boolean
  align?: 'left' | 'center' | 'right'
  // ES mapping 相关信息
  esField?: string
  esType?: string
  format?: string // 用于日期格式化等
}

export interface TableRow {
  _id: string
  _score?: number
  _source: Record<string, any>
  _index: string
  _type?: string
}

export interface TableData {
  columns: TableColumn[]
  rows: TableRow[]
  total: number
  pageSize: number
  currentPage: number
  loading: boolean
}

export interface SortConfig {
  field: string
  order: 'asc' | 'desc'
}

export interface FilterConfig {
  field: string
  value: any
  operator: 'eq' | 'contains' | 'startsWith' | 'endsWith' | 'range' | 'in'
}

export interface PaginationConfig {
  page: number
  size: number
  total: number
  showSizeChanger: boolean
  showQuickJumper: boolean
  showTotal: boolean
}

export interface VirtualScrollConfig {
  itemHeight: number
  visibleCount: number
  bufferSize: number
  enabled: boolean
}

export interface TableSettings {
  columns: TableColumn[]
  sort?: SortConfig
  filters: FilterConfig[]
  pagination: PaginationConfig
  virtualScroll: VirtualScrollConfig
}

// ES Mapping 相关类型
export interface ESFieldMapping {
  type: string
  format?: string
  properties?: Record<string, ESFieldMapping>
  fields?: Record<string, ESFieldMapping>
}

export interface ESIndexMapping {
  mappings: {
    properties: Record<string, ESFieldMapping>
  }
}

// 响应式断点配置
export interface ResponsiveBreakpoints {
  mobile: number
  tablet: number
  desktop: number
  wide: number
}

export interface ResponsiveColumnConfig {
  mobile: string[] // 在移动端显示的列
  tablet: string[] // 在平板端显示的列
  desktop: string[] // 在桌面端显示的列
}

// 表格性能配置
export interface PerformanceConfig {
  debounceTime: number
  throttleTime: number
  cacheEnabled: boolean
  cacheSize: number
  virtualScrollEnabled: boolean
  lazyLoadEnabled: boolean
}