import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN.json'
import enUS from './en-US.json'

// 支持的语言列表
export const SUPPORTED_LOCALES = [
  {
    code: 'zh-CN',
    name: '中文',
    nativeName: '简体中文',
    flag: '🇨🇳'
  },
  {
    code: 'en-US',
    name: 'English',
    nativeName: 'English (US)',
    flag: '🇺🇸'
  }
] as const

export type SupportedLocale = typeof SUPPORTED_LOCALES[number]['code']

// 默认语言
const DEFAULT_LOCALE: SupportedLocale = 'zh-CN'
const FALLBACK_LOCALE: SupportedLocale = 'en-US'

// 从本地存储或浏览器设置获取语言
function getInitialLocale(): SupportedLocale {
  // 1. 检查本地存储
  const storedLocale = localStorage.getItem('language') as SupportedLocale
  if (storedLocale && SUPPORTED_LOCALES.some(locale => locale.code === storedLocale)) {
    return storedLocale
  }

  // 2. 检查浏览器语言设置
  const browserLocale = navigator.language
  const matchedLocale = SUPPORTED_LOCALES.find(locale =>
    browserLocale.startsWith(locale.code) ||
    browserLocale.startsWith(locale.code.split('-')[0])
  )

  if (matchedLocale) {
    return matchedLocale.code
  }

  // 3. 返回默认语言
  return DEFAULT_LOCALE
}

// 创建i18n实例
const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: FALLBACK_LOCALE,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  },
  // 开发环境下显示缺失翻译的警告
  silentTranslationWarn: import.meta.env.PROD,
  silentFallbackWarn: import.meta.env.PROD
})

// 语言切换工具函数
export function setLocale(locale: SupportedLocale) {
  if (!SUPPORTED_LOCALES.some(l => l.code === locale)) {
    console.warn(`Unsupported locale: ${locale}`)
    return
  }

  i18n.global.locale.value = locale
  localStorage.setItem('language', locale)

  // 更新HTML lang属性
  document.documentElement.lang = locale

  // 更新页面标题方向（RTL支持预留）
  document.documentElement.dir = getTextDirection(locale)
}

// 获取文本方向（为RTL语言预留）
function getTextDirection(locale: SupportedLocale): 'ltr' | 'rtl' {
  // 目前只有LTR语言，未来可扩展RTL支持
  return 'ltr'
}

// 获取当前语言信息
export function getCurrentLocaleInfo() {
  const currentLocale = i18n.global.locale.value as SupportedLocale
  return SUPPORTED_LOCALES.find(locale => locale.code === currentLocale)
}

// 获取可用语言列表
export function getAvailableLocales() {
  return SUPPORTED_LOCALES
}

// 检查是否为RTL语言
export function isRTL(locale?: SupportedLocale): boolean {
  const targetLocale = locale || i18n.global.locale.value as SupportedLocale
  return getTextDirection(targetLocale) === 'rtl'
}

// 格式化相对时间（考虑语言）
export function formatRelativeTime(value: number, unit: Intl.RelativeTimeFormatUnit): string {
  const locale = i18n.global.locale.value
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
  return rtf.format(value, unit)
}

// 格式化数字（考虑语言）
export function formatNumber(value: number, options?: Intl.NumberFormatOptions): string {
  const locale = i18n.global.locale.value
  return new Intl.NumberFormat(locale, options).format(value)
}

// 格式化日期（考虑语言）
export function formatDate(date: Date, options?: Intl.DateTimeFormatOptions): string {
  const locale = i18n.global.locale.value
  return new Intl.DateTimeFormat(locale, options).format(date)
}

// 初始化HTML属性
setLocale(getInitialLocale())

export default i18n