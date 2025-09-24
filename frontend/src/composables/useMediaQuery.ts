import { ref, onMounted, onUnmounted, computed } from 'vue'

/**
 * 媒体查询 composable
 * 用于响应式设计和断点监听
 */
export function useMediaQuery(query: string) {
  const matches = ref(false)
  let mediaQuery: MediaQueryList | null = null

  const updateMatches = (event: MediaQueryListEvent) => {
    matches.value = event.matches
  }

  onMounted(() => {
    if (typeof window !== 'undefined') {
      mediaQuery = window.matchMedia(query)
      matches.value = mediaQuery.matches

      // 现代浏览器使用 addEventListener
      if (mediaQuery.addEventListener) {
        mediaQuery.addEventListener('change', updateMatches)
      } else {
        // 兼容旧浏览器
        mediaQuery.addListener(updateMatches)
      }
    }
  })

  onUnmounted(() => {
    if (mediaQuery) {
      if (mediaQuery.removeEventListener) {
        mediaQuery.removeEventListener('change', updateMatches)
      } else {
        mediaQuery.removeListener(updateMatches)
      }
    }
  })

  return matches
}

/**
 * 预定义断点
 */
export const breakpoints = {
  mobile: '(max-width: 767px)',
  tablet: '(min-width: 768px) and (max-width: 1023px)',
  desktop: '(min-width: 1024px)',
  wide: '(min-width: 1440px)',
  // 更细粒度的断点
  xs: '(max-width: 479px)',
  sm: '(min-width: 480px) and (max-width: 767px)',
  md: '(min-width: 768px) and (max-width: 1023px)',
  lg: '(min-width: 1024px) and (max-width: 1439px)',
  xl: '(min-width: 1440px)',
  // 方向断点
  landscape: '(orientation: landscape)',
  portrait: '(orientation: portrait)',
  // 特殊场景
  touch: '(pointer: coarse)',
  mouse: '(pointer: fine)',
  highDPI: '(min-resolution: 2dppx)'
}

/**
 * 使用预定义断点的便捷方法
 */
export function useBreakpoints() {
  const isMobile = useMediaQuery(breakpoints.mobile)
  const isTablet = useMediaQuery(breakpoints.tablet)
  const isDesktop = useMediaQuery(breakpoints.desktop)
  const isWide = useMediaQuery(breakpoints.wide)

  const isXS = useMediaQuery(breakpoints.xs)
  const isSM = useMediaQuery(breakpoints.sm)
  const isMD = useMediaQuery(breakpoints.md)
  const isLG = useMediaQuery(breakpoints.lg)
  const isXL = useMediaQuery(breakpoints.xl)

  const isLandscape = useMediaQuery(breakpoints.landscape)
  const isPortrait = useMediaQuery(breakpoints.portrait)
  const isTouch = useMediaQuery(breakpoints.touch)
  const isMouse = useMediaQuery(breakpoints.mouse)
  const isHighDPI = useMediaQuery(breakpoints.highDPI)

  // 计算当前设备类型
  const deviceType = computed(() => {
    if (isMobile.value) return 'mobile'
    if (isTablet.value) return 'tablet'
    if (isDesktop.value) return 'desktop'
    if (isWide.value) return 'wide'
    return 'unknown'
  })

  // 计算当前尺寸级别
  const sizeLevel = computed(() => {
    if (isXS.value) return 'xs'
    if (isSM.value) return 'sm'
    if (isMD.value) return 'md'
    if (isLG.value) return 'lg'
    if (isXL.value) return 'xl'
    return 'unknown'
  })

  // 是否为小屏设备（移动端和小平板）
  const isSmallScreen = computed(() => isMobile.value || isXS.value || isSM.value)

  // 是否为大屏设备（桌面端）
  const isLargeScreen = computed(() => isDesktop.value || isWide.value || isLG.value || isXL.value)

  return {
    // 基础断点
    isMobile,
    isTablet,
    isDesktop,
    isWide,
    
    // 细粒度断点
    isXS,
    isSM,
    isMD,
    isLG,
    isXL,
    
    // 方向和输入方式
    isLandscape,
    isPortrait,
    isTouch,
    isMouse,
    isHighDPI,
    
    // 计算属性
    deviceType,
    sizeLevel,
    isSmallScreen,
    isLargeScreen
  }
}

/**
 * 响应式列数计算
 */
export function useResponsiveColumns(options: {
  mobile?: number
  tablet?: number
  desktop?: number
  wide?: number
} = {}) {
  const {
    mobile = 1,
    tablet = 2,
    desktop = 3,
    wide = 4
  } = options

  const { isMobile, isTablet, isDesktop, isWide } = useBreakpoints()

  const columns = computed(() => {
    if (isMobile.value) return mobile
    if (isTablet.value) return tablet
    if (isDesktop.value) return desktop
    if (isWide.value) return wide
    return desktop // 默认值
  })

  return columns
}

/**
 * 响应式表格列配置
 */
export function useResponsiveTableColumns<T extends { key: string; priority: number }>(
  allColumns: T[]
) {
  const { deviceType } = useBreakpoints()

  const visibleColumns = computed(() => {
    const maxColumns = {
      mobile: 3,
      tablet: 5,
      desktop: 8,
      wide: 12
    }

    const limit = maxColumns[deviceType.value as keyof typeof maxColumns] || 8
    
    return allColumns
      .sort((a, b) => a.priority - b.priority)
      .slice(0, limit)
  })

  return visibleColumns
}

/**
 * 响应式字体大小
 */
export function useResponsiveFontSize() {
  const { deviceType } = useBreakpoints()

  const fontSize = computed(() => {
    switch (deviceType.value) {
      case 'mobile':
        return {
          xs: '0.75rem',    // 12px
          sm: '0.875rem',   // 14px
          base: '1rem',     // 16px
          lg: '1.125rem',   // 18px
          xl: '1.25rem',    // 20px
          '2xl': '1.5rem'   // 24px
        }
      case 'tablet':
        return {
          xs: '0.8125rem',  // 13px
          sm: '0.9375rem',  // 15px
          base: '1.0625rem', // 17px
          lg: '1.1875rem',  // 19px
          xl: '1.375rem',   // 22px
          '2xl': '1.625rem' // 26px
        }
      default: // desktop & wide
        return {
          xs: '0.875rem',   // 14px
          sm: '1rem',       // 16px
          base: '1.125rem', // 18px
          lg: '1.25rem',    // 20px
          xl: '1.5rem',     // 24px
          '2xl': '1.75rem'  // 28px
        }
    }
  })

  return fontSize
}

/**
 * 响应式间距
 */
export function useResponsiveSpacing() {
  const { deviceType } = useBreakpoints()

  const spacing = computed(() => {
    const base = {
      mobile: 0.75,
      tablet: 0.875,
      desktop: 1,
      wide: 1.125
    }

    const multiplier = base[deviceType.value as keyof typeof base] || 1

    return {
      xs: `${0.25 * multiplier}rem`,
      sm: `${0.5 * multiplier}rem`,
      md: `${1 * multiplier}rem`,
      lg: `${1.5 * multiplier}rem`,
      xl: `${2 * multiplier}rem`,
      '2xl': `${3 * multiplier}rem`,
      '3xl': `${4 * multiplier}rem`
    }
  })

  return spacing
}

/**
 * 检测是否支持触摸
 */
export function useTouchSupport() {
  const hasTouch = ref(false)

  onMounted(() => {
    if (typeof window !== 'undefined') {
      hasTouch.value = 'ontouchstart' in window ||
        navigator.maxTouchPoints > 0 ||
        'msMaxTouchPoints' in navigator
    }
  })

  return hasTouch
}

/**
 * 检测是否为移动浏览器
 */
export function useMobileDetection() {
  const isMobileBrowser = ref(false)

  onMounted(() => {
    if (typeof window !== 'undefined' && navigator.userAgent) {
      const mobileRegex = /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i
      isMobileBrowser.value = mobileRegex.test(navigator.userAgent)
    }
  })

  return isMobileBrowser
}

/**
 * 响应式表格行高
 */
export function useResponsiveRowHeight() {
  const { deviceType } = useBreakpoints()

  const rowHeight = computed(() => {
    switch (deviceType.value) {
      case 'mobile':
        return 48 // 较小的行高适应触摸
      case 'tablet':
        return 56
      default:
        return 60
    }
  })

  return rowHeight
}