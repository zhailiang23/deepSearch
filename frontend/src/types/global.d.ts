/**
 * 全局类型声明
 */

// 扩展 Window 接口
interface Window {
  gc?: () => void
}

// NodeJS 命名空间
declare namespace NodeJS {
  type Timeout = number
}

// 只读类型辅助
declare type Readonly<T> = {
  readonly [P in keyof T]: T[P]
}
