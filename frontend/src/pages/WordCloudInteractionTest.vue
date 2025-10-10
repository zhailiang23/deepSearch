<template>
  <div class="word-cloud-test-page">
    <div class="page-header">
      <h1 class="page-title">词云交互功能测试</h1>
      <p class="page-description">测试词云组件的点击、悬停、缩放等交互功能</p>
    </div>

    <div class="test-container">
      <!-- 控制面板 -->
      <div class="control-panel">
        <div class="panel-section">
          <h3 class="section-title">测试数据</h3>
          <div class="control-group">
            <label class="control-label">数据集</label>
            <select v-model="selectedDataset" @change="loadDataset" class="select-input">
              <option value="small">小数据集 (20个词)</option>
              <option value="medium">中等数据集 (50个词)</option>
              <option value="large">大数据集 (100个词)</option>
              <option value="custom">自定义数据</option>
            </select>
          </div>

          <div v-if="selectedDataset === 'custom'" class="control-group">
            <label class="control-label">自定义词语数据</label>
            <textarea
              v-model="customWordsText"
              @input="parseCustomWords"
              placeholder="输入格式: 词语,权重 (每行一个)"
              rows="5"
              class="textarea-input"
            ></textarea>
          </div>
        </div>

        <div class="panel-section">
          <h3 class="section-title">尺寸配置</h3>
          <div class="control-group">
            <label class="control-label">宽度: {{ canvasWidth }}</label>
            <input
              type="range"
              v-model.number="canvasWidth"
              min="400"
              max="1200"
              step="50"
              class="range-input"
            />
          </div>
          <div class="control-group">
            <label class="control-label">高度: {{ canvasHeight }}</label>
            <input
              type="range"
              v-model.number="canvasHeight"
              min="300"
              max="800"
              step="50"
              class="range-input"
            />
          </div>
        </div>

        <div class="panel-section">
          <h3 class="section-title">交互测试</h3>
          <div class="test-buttons">
            <button @click="testWordClick" class="test-btn">测试词语点击</button>
            <button @click="testHoverEffect" class="test-btn">测试悬停效果</button>
            <button @click="testZoomFeature" class="test-btn">测试缩放功能</button>
            <button @click="resetAll" class="test-btn danger">重置所有</button>
          </div>
        </div>

        <div class="panel-section">
          <h3 class="section-title">事件日志</h3>
          <div class="event-log">
            <div
              v-for="(event, index) in eventLog"
              :key="index"
              class="log-entry"
              :class="event.type"
            >
              <span class="log-time">{{ event.time }}</span>
              <span class="log-type">{{ event.type }}</span>
              <span class="log-message">{{ event.message }}</span>
            </div>
          </div>
          <button @click="clearLog" class="clear-log-btn">清空日志</button>
        </div>
      </div>

      <!-- 词云展示区域 -->
      <div class="chart-area">
        <HotWordCloudChart
          ref="chartRef"
          :words="testWords"
          :width="canvasWidth"
          :height="canvasHeight"
          :interactive="true"
          :show-toolbar="true"
          :show-interaction-controls="true"
          :show-interaction-status="true"
          :show-tooltip="true"
          :show-debug-info="true"
          title="交互测试词云"
          @word-click="handleWordClick"
          @word-hover="handleWordHover"
          @render-start="handleRenderStart"
          @render-complete="handleRenderComplete"
          @render-error="handleRenderError"
          @zoom="handleZoom"
          @drag="handleDrag"
        />

        <!-- 交互信息显示 -->
        <div class="interaction-info">
          <div class="info-section">
            <h4>当前状态</h4>
            <div class="status-grid">
              <div class="status-item">
                <span class="status-label">悬停词语:</span>
                <span class="status-value">{{ currentHoveredWord || '无' }}</span>
              </div>
              <div class="status-item">
                <span class="status-label">点击次数:</span>
                <span class="status-value">{{ clickCount }}</span>
              </div>
              <div class="status-item">
                <span class="status-label">缩放级别:</span>
                <span class="status-value">{{ Math.round(currentZoom * 100) }}%</span>
              </div>
              <div class="status-item">
                <span class="status-label">拖拽距离:</span>
                <span class="status-value">{{ Math.round(totalDragDistance) }}px</span>
              </div>
            </div>
          </div>

          <div class="info-section">
            <h4>性能统计</h4>
            <div class="stats-grid" v-if="renderStats">
              <div class="stat-item">
                <span class="stat-label">渲染时间:</span>
                <span class="stat-value">{{ renderStats.renderTime }}ms</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">总词数:</span>
                <span class="stat-value">{{ renderStats.totalWords }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">成功渲染:</span>
                <span class="stat-value">{{ renderStats.renderedWords }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import HotWordCloudChart from '@/components/statistics/HotWordCloudChart.vue'
import type { WordCloudItem, WordCloudStats } from '@/types/statistics'

// 测试数据 - 使用 HotWordItem 格式 {text, weight}
const testDatasets = {
  small: [
    { text: 'Vue', weight: 80 }, { text: 'React', weight: 75 }, { text: 'Angular', weight: 60 }, { text: 'JavaScript', weight: 90 },
    { text: 'TypeScript', weight: 70 }, { text: 'HTML', weight: 65 }, { text: 'CSS', weight: 55 }, { text: 'Node.js', weight: 85 },
    { text: 'Express', weight: 50 }, { text: 'MongoDB', weight: 45 }, { text: 'PostgreSQL', weight: 40 }, { text: 'Redis', weight: 35 },
    { text: 'Docker', weight: 60 }, { text: 'Git', weight: 70 }, { text: 'Webpack', weight: 45 }, { text: 'Vite', weight: 55 },
    { text: 'ESLint', weight: 30 }, { text: 'Prettier', weight: 25 }, { text: 'Jest', weight: 40 }, { text: 'Cypress', weight: 35 }
  ] as WordCloudItem[],

  medium: [
    { text: 'JavaScript', weight: 100 }, { text: 'TypeScript', weight: 95 }, { text: 'Python', weight: 90 }, { text: 'Java', weight: 85 }, { text: 'React', weight: 80 },
    { text: 'Vue', weight: 75 }, { text: 'Angular', weight: 70 }, { text: 'Node.js', weight: 85 }, { text: 'Express', weight: 65 }, { text: 'Django', weight: 60 },
    { text: 'Spring', weight: 55 }, { text: 'MongoDB', weight: 70 }, { text: 'PostgreSQL', weight: 75 }, { text: 'MySQL', weight: 65 }, { text: 'Redis', weight: 60 },
    { text: 'Docker', weight: 80 }, { text: 'Kubernetes', weight: 70 }, { text: 'AWS', weight: 75 }, { text: 'Azure', weight: 60 }, { text: 'GCP', weight: 55 },
    { text: 'Git', weight: 90 }, { text: 'GitHub', weight: 85 }, { text: 'GitLab', weight: 60 }, { text: 'Webpack', weight: 55 }, { text: 'Vite', weight: 65 },
    { text: 'ESLint', weight: 45 }, { text: 'Prettier', weight: 40 }, { text: 'Jest', weight: 50 }, { text: 'Cypress', weight: 45 }, { text: 'Playwright', weight: 40 },
    { text: 'HTML', weight: 70 }, { text: 'CSS', weight: 65 }, { text: 'SCSS', weight: 55 }, { text: 'TailwindCSS', weight: 60 }, { text: 'Bootstrap', weight: 50 },
    { text: 'RESTful', weight: 55 }, { text: 'GraphQL', weight: 50 }, { text: 'Apollo', weight: 40 }, { text: 'Prisma', weight: 45 }, { text: 'Sequelize', weight: 35 },
    { text: 'Socket.io', weight: 40 }, { text: 'WebRTC', weight: 30 }, { text: 'WebSocket', weight: 35 }, { text: 'OAuth', weight: 45 }, { text: 'JWT', weight: 50 },
    { text: 'CORS', weight: 35 }, { text: 'HTTPS', weight: 40 }, { text: 'SSL', weight: 35 }, { text: 'Nginx', weight: 50 }, { text: 'Apache', weight: 40 }
  ] as WordCloudItem[],

  large: [] as WordCloudItem[]
}

// 生成大数据集
const generateLargeDataset = (): WordCloudItem[] => {
  const words = [
    'JavaScript', 'TypeScript', 'Python', 'Java', 'C++', 'C#', 'Go', 'Rust', 'Swift', 'Kotlin',
    'React', 'Vue', 'Angular', 'Svelte', 'jQuery', 'Express', 'Koa', 'Fastify', 'NestJS', 'Django',
    'Flask', 'FastAPI', 'Spring', 'Hibernate', 'MyBatis', 'Laravel', 'Symfony', 'CodeIgniter', 'Rails', 'Sinatra',
    'MongoDB', 'PostgreSQL', 'MySQL', 'SQLite', 'Redis', 'Elasticsearch', 'Cassandra', 'DynamoDB', 'Neo4j', 'InfluxDB',
    'Docker', 'Kubernetes', 'Jenkins', 'GitLab', 'GitHub', 'Bitbucket', 'Travis', 'CircleCI', 'TeamCity', 'Bamboo',
    'AWS', 'Azure', 'GCP', 'Heroku', 'Vercel', 'Netlify', 'DigitalOcean', 'Linode', 'Vultr', 'Hetzner',
    'Webpack', 'Vite', 'Rollup', 'Parcel', 'ESLint', 'Prettier', 'Husky', 'Lint-staged', 'Commitizen', 'Semantic-release',
    'Jest', 'Mocha', 'Cypress', 'Playwright', 'Puppeteer', 'Selenium', 'TestCafe', 'Vitest', 'Karma', 'Jasmine',
    'HTML', 'CSS', 'SCSS', 'Less', 'Stylus', 'TailwindCSS', 'Bootstrap', 'Bulma', 'Foundation', 'Materialize',
    'GraphQL', 'Apollo', 'Relay', 'Prisma', 'Sequelize', 'Mongoose', 'TypeORM', 'Knex', 'Bookshelf', 'Objection'
  ]

  return words.map((word) => ({
    text: word,
    weight: Math.floor(Math.random() * 100) + 1
  }))
}

testDatasets.large = generateLargeDataset()

// 响应式数据
const selectedDataset = ref<'small' | 'medium' | 'large' | 'custom'>('medium')
const customWordsText = ref('')
const canvasWidth = ref(800)
const canvasHeight = ref(600)

const testWords = ref<WordCloudItem[]>([...testDatasets.medium])
const chartRef = ref<InstanceType<typeof HotWordCloudChart> | null>(null)

// 交互状态
const currentHoveredWord = ref<string | null>(null)
const clickCount = ref(0)
const currentZoom = ref(1)
const totalDragDistance = ref(0)
const renderStats = ref<WordCloudStats | null>(null)

// 事件日志
interface LogEntry {
  time: string
  type: 'click' | 'hover' | 'zoom' | 'drag' | 'render' | 'error'
  message: string
}

const eventLog = ref<LogEntry[]>([])

const addLogEntry = (type: LogEntry['type'], message: string) => {
  const time = new Date().toLocaleTimeString()
  eventLog.value.unshift({ time, type, message })

  // 限制日志数量
  if (eventLog.value.length > 100) {
    eventLog.value = eventLog.value.slice(0, 100)
  }
}

// 方法
const loadDataset = () => {
  if (selectedDataset.value !== 'custom') {
    testWords.value = [...testDatasets[selectedDataset.value]]
    addLogEntry('render', `加载${selectedDataset.value}数据集，共${testWords.value.length}个词`)
  }
}

const parseCustomWords = () => {
  try {
    const lines = customWordsText.value.split('\n').filter(line => line.trim())
    const words: WordCloudItem[] = []

    for (const line of lines) {
      const [word, weightStr] = line.split(',').map(s => s.trim())
      const weight = parseInt(weightStr) || 1
      if (word) {
        words.push({ text: word, weight })
      }
    }

    if (words.length > 0) {
      testWords.value = words
      addLogEntry('render', `解析自定义数据，共${words.length}个词`)
    }
  } catch (error) {
    addLogEntry('error', `自定义数据解析失败: ${error}`)
  }
}

const testWordClick = () => {
  addLogEntry('click', '触发词语点击测试 - 请点击词云中的词语')
}

const testHoverEffect = () => {
  addLogEntry('hover', '悬停效果测试 - 请将鼠标移动到词语上')
}

const testZoomFeature = () => {
  addLogEntry('zoom', '缩放功能测试 - 请使用鼠标滚轮缩放词云')
}

const resetAll = () => {
  currentHoveredWord.value = null
  clickCount.value = 0
  currentZoom.value = 1
  totalDragDistance.value = 0
  renderStats.value = null
  chartRef.value?.clear()
  addLogEntry('render', '重置所有状态和数据')
}

const clearLog = () => {
  eventLog.value = []
}

// 事件处理
const handleWordClick = (word: WordCloudItem, event: Event) => {
  clickCount.value++
  addLogEntry('click', `点击词语: ${word.text} (权重: ${word.weight})`)
}

const handleWordHover = (word: WordCloudItem, event: Event) => {
  currentHoveredWord.value = word.text
  if (word.text) {
    addLogEntry('hover', `悬停词语: ${word.text}`)
  }
}

const handleRenderStart = () => {
  addLogEntry('render', '开始渲染词云')
}

const handleRenderComplete = () => {
  // WordCloudStats 可能不包含 maxWeight,只保留基本统计
  const stats = {
    totalWords: testWords.value.length,
    renderedWords: testWords.value.length,
    renderTime: 0
  }
  renderStats.value = stats
  addLogEntry('render', `渲染完成 - 成功渲染${stats.renderedWords}个词`)
}

const handleRenderError = (errorMessage: string) => {
  addLogEntry('error', `渲染错误: ${errorMessage}`)
}

const handleZoom = (scale: number, event: WheelEvent) => {
  currentZoom.value = scale
  addLogEntry('zoom', `缩放至: ${Math.round(scale * 100)}%`)
}

const handleDrag = (deltaX: number, deltaY: number, event: MouseEvent) => {
  const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY)
  totalDragDistance.value += distance
  addLogEntry('drag', `拖拽距离: ${Math.round(distance)}px`)
}

// 生命周期
onMounted(() => {
  addLogEntry('render', '页面初始化完成')
})
</script>

<style scoped>
.word-cloud-test-page {
  @apply min-h-screen bg-background p-6;
}

.page-header {
  @apply text-center mb-8;
}

.page-title {
  @apply text-3xl font-bold text-foreground mb-2;
}

.page-description {
  @apply text-muted-foreground;
}

.test-container {
  @apply grid grid-cols-1 lg:grid-cols-4 gap-6;
}

.control-panel {
  @apply lg:col-span-1 space-y-6;
}

.chart-area {
  @apply lg:col-span-3 space-y-6;
}

.panel-section {
  @apply bg-card border border-border rounded-lg p-4;
}

.section-title {
  @apply text-lg font-semibold text-foreground mb-4;
}

.control-group {
  @apply space-y-2 mb-4;
}

.control-label {
  @apply block text-sm font-medium text-foreground;
}

.select-input {
  @apply w-full p-2 border border-border rounded bg-background text-foreground;
}

.textarea-input {
  @apply w-full p-2 border border-border rounded bg-background text-foreground resize-none;
}

.range-input {
  @apply w-full accent-primary;
}

.test-buttons {
  @apply grid grid-cols-1 gap-2;
}

.test-btn {
  @apply px-4 py-2 bg-primary text-primary-foreground rounded hover:bg-primary/90 transition-colors;
}

.test-btn.danger {
  @apply bg-destructive text-destructive-foreground hover:bg-destructive/90;
}

.event-log {
  @apply max-h-64 overflow-y-auto border border-border rounded bg-background/50 p-2 mb-2;
}

.log-entry {
  @apply text-xs border-l-2 pl-2 py-1 mb-1;
}

.log-entry.click {
  @apply border-l-blue-500;
}

.log-entry.hover {
  @apply border-l-green-500;
}

.log-entry.zoom {
  @apply border-l-purple-500;
}

.log-entry.drag {
  @apply border-l-orange-500;
}

.log-entry.render {
  @apply border-l-gray-500;
}

.log-entry.error {
  @apply border-l-red-500;
}

.log-time {
  @apply text-muted-foreground mr-2;
}

.log-type {
  @apply font-mono font-bold mr-2;
}

.log-message {
  @apply text-foreground;
}

.clear-log-btn {
  @apply w-full px-3 py-1 text-xs bg-secondary text-secondary-foreground rounded hover:bg-secondary/80;
}

.interaction-info {
  @apply grid grid-cols-1 md:grid-cols-2 gap-4;
}

.info-section {
  @apply bg-card border border-border rounded-lg p-4;
}

.info-section h4 {
  @apply text-base font-semibold text-foreground mb-3;
}

.status-grid,
.stats-grid {
  @apply grid grid-cols-1 gap-2;
}

.status-item,
.stat-item {
  @apply flex justify-between text-sm;
}

.status-label,
.stat-label {
  @apply text-muted-foreground;
}

.status-value,
.stat-value {
  @apply text-foreground font-mono;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .test-container {
    @apply grid-cols-1;
  }

  .control-panel {
    @apply order-2;
  }

  .chart-area {
    @apply order-1;
  }
}

@media (max-width: 640px) {
  .page-header {
    @apply mb-4;
  }

  .page-title {
    @apply text-2xl;
  }

  .test-container {
    @apply gap-4;
  }

  .panel-section {
    @apply p-3;
  }

  .interaction-info {
    @apply grid-cols-1;
  }
}
</style>