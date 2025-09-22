import { ref, computed, onMounted, onUnmounted } from 'vue'

export interface Breakpoints {
  xs: number  // 0px
  sm: number  // 320px
  md: number  // 768px
  lg: number  // 1024px
  xl: number  // 1280px
  '2xl': number // 1536px
}

export interface BreakpointState {
  current: string
  width: number
  height: number
  isXs: boolean
  isSm: boolean
  isMd: boolean
  isLg: boolean
  isXl: boolean
  is2Xl: boolean
  isMobile: boolean
  isTablet: boolean
  isDesktop: boolean
  isLandscape: boolean
  isPortrait: boolean
  isTouchDevice: boolean
}

const defaultBreakpoints: Breakpoints = {
  xs: 0,
  sm: 320,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536
}

export function useBreakpoints(customBreakpoints?: Partial<Breakpoints>) {
  const breakpoints = { ...defaultBreakpoints, ...customBreakpoints }

  const windowWidth = ref(0)
  const windowHeight = ref(0)
  const isTouchDevice = ref(false)

  const updateDimensions = () => {
    windowWidth.value = window.innerWidth
    windowHeight.value = window.innerHeight
  }

  const detectTouchDevice = () => {
    isTouchDevice.value = 'ontouchstart' in window ||
      navigator.maxTouchPoints > 0 ||
      (navigator as any).msMaxTouchPoints > 0
  }

  // 当前断点
  const current = computed(() => {
    const width = windowWidth.value

    if (width >= breakpoints['2xl']) return '2xl'
    if (width >= breakpoints.xl) return 'xl'
    if (width >= breakpoints.lg) return 'lg'
    if (width >= breakpoints.md) return 'md'
    if (width >= breakpoints.sm) return 'sm'
    return 'xs'
  })

  // 各断点状态
  const isXs = computed(() => windowWidth.value >= breakpoints.xs && windowWidth.value < breakpoints.sm)
  const isSm = computed(() => windowWidth.value >= breakpoints.sm && windowWidth.value < breakpoints.md)
  const isMd = computed(() => windowWidth.value >= breakpoints.md && windowWidth.value < breakpoints.lg)
  const isLg = computed(() => windowWidth.value >= breakpoints.lg && windowWidth.value < breakpoints.xl)
  const isXl = computed(() => windowWidth.value >= breakpoints.xl && windowWidth.value < breakpoints['2xl'])
  const is2Xl = computed(() => windowWidth.value >= breakpoints['2xl'])

  // 设备类型判断
  const isMobile = computed(() => windowWidth.value < breakpoints.md)
  const isTablet = computed(() => windowWidth.value >= breakpoints.md && windowWidth.value < breakpoints.lg)
  const isDesktop = computed(() => windowWidth.value >= breakpoints.lg)

  // 屏幕方向
  const isLandscape = computed(() => windowWidth.value > windowHeight.value)
  const isPortrait = computed(() => windowWidth.value <= windowHeight.value)

  // 响应式状态对象
  const state = computed<BreakpointState>(() => ({
    current: current.value,
    width: windowWidth.value,
    height: windowHeight.value,
    isXs: isXs.value,
    isSm: isSm.value,
    isMd: isMd.value,
    isLg: isLg.value,
    isXl: isXl.value,
    is2Xl: is2Xl.value,
    isMobile: isMobile.value,
    isTablet: isTablet.value,
    isDesktop: isDesktop.value,
    isLandscape: isLandscape.value,
    isPortrait: isPortrait.value,
    isTouchDevice: isTouchDevice.value
  }))

  // 断点匹配函数
  const matches = (breakpoint: keyof Breakpoints | string) => {
    if (typeof breakpoint === 'string' && breakpoint in breakpoints) {
      return windowWidth.value >= breakpoints[breakpoint as keyof Breakpoints]
    }
    return false
  }

  // 检查是否在指定断点范围内
  const between = (min: keyof Breakpoints, max: keyof Breakpoints) => {
    return windowWidth.value >= breakpoints[min] && windowWidth.value < breakpoints[max]
  }

  // 检查是否大于等于指定断点
  const greaterOrEqual = (breakpoint: keyof Breakpoints) => {
    return windowWidth.value >= breakpoints[breakpoint]
  }

  // 检查是否小于指定断点
  const smallerThan = (breakpoint: keyof Breakpoints) => {
    return windowWidth.value < breakpoints[breakpoint]
  }

  // 生命周期钩子
  onMounted(() => {
    updateDimensions()
    detectTouchDevice()
    window.addEventListener('resize', updateDimensions)
    window.addEventListener('orientationchange', updateDimensions)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateDimensions)
    window.removeEventListener('orientationchange', updateDimensions)
  })

  return {
    // 响应式状态
    state,
    breakpoints,

    // 单独的响应式属性
    current,
    width: windowWidth,
    height: windowHeight,
    isXs,
    isSm,
    isMd,
    isLg,
    isXl,
    is2Xl,
    isMobile,
    isTablet,
    isDesktop,
    isLandscape,
    isPortrait,
    isTouchDevice,

    // 工具函数
    matches,
    between,
    greaterOrEqual,
    smallerThan
  }
}

// 工厂函数，用于创建自定义断点的 composable
export function createBreakpoints(customBreakpoints: Partial<Breakpoints>) {
  return () => useBreakpoints(customBreakpoints)
}