/**
 * 日期格式化工具函数
 */

/**
 * 格式化日期时间为本地化字符串
 */
export function formatDateTime(dateString: string): string {
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) {
      return dateString
    }

    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  } catch (error) {
    console.warn('日期格式化失败:', error)
    return dateString
  }
}

/**
 * 格式化日期为本地化字符串
 */
export function formatDate(dateString: string): string {
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) {
      return dateString
    }

    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  } catch (error) {
    console.warn('日期格式化失败:', error)
    return dateString
  }
}

/**
 * 格式化时间为本地化字符串
 */
export function formatTime(dateString: string): string {
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) {
      return dateString
    }

    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
  } catch (error) {
    console.warn('时间格式化失败:', error)
    return dateString
  }
}

/**
 * 格式化相对时间（如：2小时前）
 */
export function formatRelativeTime(dateString: string): string {
  try {
    const date = new Date(dateString)
    const now = new Date()
    const diff = now.getTime() - date.getTime()

    if (isNaN(date.getTime())) {
      return dateString
    }

    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)

    if (days > 0) {
      return `${days}天前`
    } else if (hours > 0) {
      return `${hours}小时前`
    } else if (minutes > 0) {
      return `${minutes}分钟前`
    } else {
      return '刚刚'
    }
  } catch (error) {
    console.warn('相对时间格式化失败:', error)
    return dateString
  }
}

/**
 * 检查日期是否为今天
 */
export function isToday(dateString: string): boolean {
  try {
    const date = new Date(dateString)
    const today = new Date()

    return date.toDateString() === today.toDateString()
  } catch (error) {
    return false
  }
}

/**
 * 检查日期是否为昨天
 */
export function isYesterday(dateString: string): boolean {
  try {
    const date = new Date(dateString)
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)

    return date.toDateString() === yesterday.toDateString()
  } catch (error) {
    return false
  }
}