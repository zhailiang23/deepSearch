/**
 * wordcloud2.js 库的类型声明
 */

declare module 'wordcloud' {
  export interface WordCloudOptions {
    list?: [string, number, any?][]
    fontFamily?: string
    fontWeight?: string | ((word: string, weight: number, fontSize: number, extraData?: any) => string)
    color?: string | ((word: string, weight: number, fontSize: number, distance: number, theta: number) => string)
    minSize?: number
    weightFactor?: number | ((size: number) => number)
    backgroundColor?: string
    clearCanvas?: boolean
    gridSize?: number
    origin?: [number, number]
    drawOutOfBound?: boolean
    shrinkToFit?: boolean
    minRotation?: number
    maxRotation?: number
    rotationSteps?: number
    rotateRatio?: number
    shuffle?: boolean
    shape?: string | ((theta: number) => number)
    ellipticity?: number
    wait?: number
    abortThreshold?: number
    abort?: () => void
    click?: (item: any, dimension: any, event: Event) => void
    hover?: (item: any, dimension: any, event: Event) => void
    width?: number
    height?: number
    density?: number
    mask?: HTMLImageElement | string
    extraWebFontCSS?: string
    drawProgress?: (progress: number) => void
  }

  export interface WordCloudStatic {
    (element: HTMLCanvasElement | HTMLElement, options: WordCloudOptions): void
    isSupported: boolean
    minFontSize: number
    stop(): void
  }

  const WordCloud: WordCloudStatic
  export default WordCloud
}

declare global {
  interface Window {
    WordCloud: import('wordcloud').WordCloudStatic
  }
}