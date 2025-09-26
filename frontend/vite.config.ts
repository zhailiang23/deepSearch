import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// 性能监控插件
function performanceMonitorPlugin() {
  let startTime: number

  return {
    name: 'performance-monitor',
    buildStart() {
      console.log('🚀 构建开始，启动性能监控...')
      startTime = Date.now()
    },
    buildEnd() {
      const buildTime = Date.now() - startTime
      console.log(`⏱️  构建完成，耗时: ${buildTime}ms`)

      if (buildTime > 30000) { // 30秒
        console.warn('⚠️  构建时间过长，建议优化构建配置')
      }
    },
    generateBundle(options: any, bundle: any) {
      // 分析bundle大小
      const bundleInfo: { [key: string]: number } = {}
      let totalSize = 0

      Object.keys(bundle).forEach(fileName => {
        const file = bundle[fileName]
        if (file.type === 'chunk') {
          const size = file.code ? file.code.length : 0
          bundleInfo[fileName] = size
          totalSize += size
        }
      })

      console.log('\n📦 Bundle 分析:')
      console.log(`总大小: ${(totalSize / 1024 / 1024).toFixed(2)}MB`)

      // 找出最大的chunk
      const sortedChunks = Object.entries(bundleInfo)
        .sort(([,a], [,b]) => (b as number) - (a as number))
        .slice(0, 5)

      console.log('最大的5个chunk:')
      sortedChunks.forEach(([name, size]) => {
        console.log(`  ${name}: ${((size as number) / 1024).toFixed(2)}KB`)
      })

      // 大小警告
      if (totalSize > 2 * 1024 * 1024) { // 2MB
        console.warn('⚠️  Bundle大小过大，建议进行代码分割')
      }
    }
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    performanceMonitorPlugin(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    // 性能优化配置
    target: 'esnext',
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 生产环境移除console
        drop_debugger: true,
      }
    },
    // 代码分割优化
    rollupOptions: {
      output: {
        // 手动分割chunk
        manualChunks: {
          // Vue相关
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // UI组件库
          'ui-vendor': ['reka-ui', 'lucide-vue-next'],
          // 工具库
          'utils-vendor': ['axios', '@vueuse/core'],
          // CodeMirror编辑器
          'codemirror-vendor': [
            'codemirror',
            '@codemirror/view',
            '@codemirror/state',
            '@codemirror/commands',
            '@codemirror/language',
            '@codemirror/lang-json',
            '@codemirror/theme-one-dark'
          ],
        },
        // 文件命名
        chunkFileNames: (chunkInfo) => {
          const facadeModuleId = chunkInfo.facadeModuleId
          if (facadeModuleId) {
            const fileName = facadeModuleId.split('/').pop()?.replace('.vue', '') || 'chunk'
            return `js/[name]-${fileName}-[hash].js`
          }
          return 'js/[name]-[hash].js'
        },
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]'
      }
    },
    // 压缩配置
    reportCompressedSize: true,
    chunkSizeWarningLimit: 1000, // 1MB警告阈值
  },
  // 性能优化
  esbuild: {
    drop: ['console', 'debugger'],
  },
  // 依赖优化
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'axios',
      '@vueuse/core',
      'reka-ui',
      'lucide-vue-next',
      'codemirror',
      '@codemirror/view',
      '@codemirror/state',
      '@codemirror/commands',
      '@codemirror/language',
      '@codemirror/lang-json',
      '@codemirror/theme-one-dark'
    ]
  },
  // 缓存目录
  cacheDir: 'node_modules/.vite',

  // 开发时性能配置
  define: {
    // 性能监控标志
    __PERFORMANCE_MONITORING__: JSON.stringify(process.env.NODE_ENV === 'production'),
    __DEV__: JSON.stringify(process.env.NODE_ENV === 'development'),
  }
})
