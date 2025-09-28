/**
 * 测试数据生成器
 * 为性能测试和开发调试生成各种类型的词云测试数据
 */

import type { HotWordItem } from '@/types/statistics'

// ============= 预定义词库 =============

/** 中文高频词汇 */
const CHINESE_WORDS = [
  '人工智能', '机器学习', '深度学习', '神经网络', '数据挖掘',
  '大数据', '云计算', '区块链', '物联网', '5G网络',
  '前端开发', '后端开发', '全栈开发', '微服务', '容器化',
  'Vue.js', 'React', 'Angular', 'Node.js', 'Python',
  'JavaScript', 'TypeScript', 'Java', 'Go', 'Rust',
  '响应式设计', '用户体验', '界面设计', '交互设计', '可访问性',
  '性能优化', '内存管理', '缓存策略', '负载均衡', '高可用',
  '敏捷开发', 'DevOps', '持续集成', '持续部署', '版本控制',
  '软件架构', '设计模式', '代码重构', '单元测试', '集成测试',
  '网络安全', '数据安全', '隐私保护', '加密算法', '身份认证',
  '移动应用', '跨平台', '原生开发', '混合应用', 'PWA',
  '搜索引擎', '推荐系统', '个性化', '用户画像', '行为分析',
  '数据可视化', '图表库', '仪表板', '报表系统', '商业智能',
  '电子商务', '支付系统', '供应链', '库存管理', '客户关系',
  '社交网络', '内容管理', '媒体处理', '实时通信', '消息队列'
]

/** 英文技术词汇 */
const ENGLISH_WORDS = [
  'Algorithm', 'Database', 'Framework', 'Library', 'Component',
  'Function', 'Variable', 'Interface', 'Class', 'Object',
  'Authentication', 'Authorization', 'Middleware', 'Router', 'Controller',
  'Model', 'View', 'Template', 'State', 'Props',
  'Hook', 'Context', 'Provider', 'Consumer', 'Reducer',
  'Action', 'Dispatcher', 'Store', 'Observable', 'Promise',
  'Async', 'Await', 'Callback', 'Event', 'Listener',
  'DOM', 'Virtual', 'Shadow', 'Element', 'Attribute',
  'CSS', 'SCSS', 'Tailwind', 'Bootstrap', 'Flexbox',
  'Grid', 'Animation', 'Transition', 'Transform', 'Gradient',
  'API', 'REST', 'GraphQL', 'WebSocket', 'HTTP',
  'HTTPS', 'JSON', 'XML', 'YAML', 'Protocol',
  'Server', 'Client', 'Browser', 'Mobile', 'Desktop',
  'Progressive', 'Responsive', 'Adaptive', 'Accessibility', 'Performance',
  'Optimization', 'Bundle', 'Minify', 'Compress', 'Cache'
]

/** 随机形容词 */
const ADJECTIVES = [
  '高效', '智能', '快速', '安全', '稳定', '可靠', '灵活', '强大',
  '先进', '创新', '便捷', '实用', '精确', '优雅', '简洁', '复杂',
  '现代', '传统', '流行', '经典', '新兴', '成熟', '专业', '通用'
]

/** 随机名词 */
const NOUNS = [
  '系统', '平台', '工具', '服务', '应用', '软件', '硬件', '设备',
  '技术', '方案', '架构', '框架', '模块', '组件', '接口', '协议',
  '算法', '模型', '数据', '信息', '内容', '资源', '文件', '文档'
]

// ============= 数据生成器类 =============

export class TestDataGenerator {
  private usedWords = new Set<string>()

  /**
   * 生成基础测试数据
   */
  generateBasicData(count: number): HotWordItem[] {
    this.usedWords.clear()
    const words: HotWordItem[] = []

    // 混合中英文词汇
    const allWords = [...CHINESE_WORDS, ...ENGLISH_WORDS]

    for (let i = 0; i < count; i++) {
      let word: string
      let attempts = 0

      // 确保词语不重复
      do {
        if (attempts < allWords.length) {
          word = allWords[Math.floor(Math.random() * allWords.length)]
        } else {
          // 如果基础词库用完，生成组合词
          word = this.generateCombinedWord()
        }
        attempts++
      } while (this.usedWords.has(word) && attempts < 1000)

      if (attempts >= 1000) {
        // 避免无限循环
        word = `词语${i}`
      }

      this.usedWords.add(word)

      // 生成权重 (遵循帕累托分布)
      const weight = this.generateParetoWeight(i, count)

      words.push({
        text: word,
        weight: Math.round(weight * 100) / 100,
        extraData: {
          category: this.getCategoryForWord(word),
          frequency: weight,
          trending: Math.random() > 0.8
        }
      })
    }

    return words.sort((a, b) => b.weight - a.weight)
  }

  /**
   * 生成帕累托分布权重
   */
  private generateParetoWeight(index: number, total: number): number {
    // 80/20原则：前20%的词语占80%的权重
    const position = index / total

    if (position < 0.2) {
      // 前20%：权重在60-100之间
      return 60 + Math.random() * 40
    } else if (position < 0.5) {
      // 中间30%：权重在30-60之间
      return 30 + Math.random() * 30
    } else {
      // 后50%：权重在1-30之间
      return 1 + Math.random() * 29
    }
  }

  /**
   * 生成组合词语
   */
  private generateCombinedWord(): string {
    const adj = ADJECTIVES[Math.floor(Math.random() * ADJECTIVES.length)]
    const noun = NOUNS[Math.floor(Math.random() * NOUNS.length)]
    return `${adj}${noun}`
  }

  /**
   * 获取词语分类
   */
  private getCategoryForWord(word: string): string {
    if (CHINESE_WORDS.includes(word)) {
      if (word.includes('智能') || word.includes('学习') || word.includes('网络')) {
        return 'AI/ML'
      } else if (word.includes('开发') || word.includes('前端') || word.includes('后端')) {
        return '开发技术'
      } else if (word.includes('数据') || word.includes('分析') || word.includes('可视化')) {
        return '数据科学'
      } else {
        return '通用技术'
      }
    } else if (ENGLISH_WORDS.includes(word)) {
      return '英文术语'
    } else {
      return '组合词汇'
    }
  }

  /**
   * 生成大数据量测试数据
   */
  generateLargeDataset(count: number): HotWordItem[] {
    const words: HotWordItem[] = []
    this.usedWords.clear()

    // 分批生成，避免内存问题
    const batchSize = 1000
    const batches = Math.ceil(count / batchSize)

    for (let batch = 0; batch < batches; batch++) {
      const batchStart = batch * batchSize
      const batchEnd = Math.min(batchStart + batchSize, count)
      const batchCount = batchEnd - batchStart

      const batchWords = this.generateBatch(batchCount, batchStart)
      words.push(...batchWords)
    }

    return words
  }

  /**
   * 生成单个批次的数据
   */
  private generateBatch(count: number, startIndex: number): HotWordItem[] {
    const words: HotWordItem[] = []

    for (let i = 0; i < count; i++) {
      const globalIndex = startIndex + i
      let word: string

      if (globalIndex < CHINESE_WORDS.length + ENGLISH_WORDS.length) {
        // 使用预定义词汇
        const allWords = [...CHINESE_WORDS, ...ENGLISH_WORDS]
        word = allWords[globalIndex]
      } else {
        // 生成唯一词汇
        word = this.generateUniqueWord(globalIndex)
      }

      const weight = this.generateParetoWeight(globalIndex, startIndex + count)

      words.push({
        text: word,
        weight: Math.round(weight * 100) / 100,
        extraData: {
          category: this.getCategoryForWord(word),
          frequency: weight,
          batchIndex: Math.floor(startIndex / 1000),
          globalIndex
        }
      })
    }

    return words
  }

  /**
   * 生成唯一词汇
   */
  private generateUniqueWord(index: number): string {
    const prefixes = ['新', '智能', '云端', '移动', '安全', '高性能', '分布式', '实时']
    const suffixes = ['系统', '平台', '服务', '工具', '应用', '框架', '引擎', '算法']

    const prefix = prefixes[index % prefixes.length]
    const suffix = suffixes[Math.floor(index / prefixes.length) % suffixes.length]
    const number = Math.floor(index / (prefixes.length * suffixes.length))

    return number > 0 ? `${prefix}${suffix}${number}` : `${prefix}${suffix}`
  }

  /**
   * 生成实时流数据
   */
  generateStreamData(baseCount: number = 50): HotWordItem[] {
    const streamWords = [
      '实时数据', '流处理', '消息队列', '事件驱动', '异步处理',
      '数据流', '实时分析', '流式计算', '事件流', '数据管道',
      '实时监控', '动态更新', '增量数据', '时间序列', '实时推送'
    ]

    return streamWords.slice(0, Math.min(baseCount, streamWords.length)).map((word, index) => ({
      text: word,
      weight: Math.random() * 80 + 20, // 20-100的随机权重
      extraData: {
        category: '实时流',
        timestamp: Date.now() - Math.random() * 3600000, // 最近1小时内
        isStreaming: true,
        updateIndex: index
      }
    }))
  }

  /**
   * 生成性能测试数据
   */
  generatePerformanceTestData(count: number, complexity: 'low' | 'medium' | 'high' = 'medium'): HotWordItem[] {
    const words: HotWordItem[] = []

    for (let i = 0; i < count; i++) {
      let word: string
      let extraData: any = {
        category: '性能测试',
        testIndex: i
      }

      switch (complexity) {
        case 'low':
          // 简单短词
          word = `词${i}`
          break

        case 'medium':
          // 中等复杂度
          word = this.generateBasicData(1)[0].text
          extraData = {
            ...extraData,
            description: `这是第${i}个测试词语`,
            metadata: { complexity: 'medium' }
          }
          break

        case 'high':
          // 高复杂度，包含更多数据
          word = this.generateCombinedWord() + `_${i}`
          extraData = {
            ...extraData,
            description: `复杂测试词语 #${i}，包含丰富的元数据`,
            metadata: {
              complexity: 'high',
              tags: ['测试', '性能', '词云'],
              attributes: {
                length: word.length,
                hasNumber: /\d/.test(word),
                hasUnderscore: word.includes('_')
              },
              relations: Array.from({ length: 3 }, (_, idx) => `relation_${idx}`)
            }
          }
          break
      }

      words.push({
        text: word,
        weight: this.generateParetoWeight(i, count),
        extraData
      })
    }

    return words
  }
}

// ============= 便利函数 =============

const generator = new TestDataGenerator()

/**
 * 生成基础测试数据
 */
export function generateTestData(count: number = 100): HotWordItem[] {
  return generator.generateBasicData(count)
}

/**
 * 生成大数据量测试数据
 */
export function generateLargeTestData(count: number = 1000): HotWordItem[] {
  return generator.generateLargeDataset(count)
}

/**
 * 生成实时流测试数据
 */
export function generateStreamTestData(count: number = 50): HotWordItem[] {
  return generator.generateStreamData(count)
}

/**
 * 生成性能测试数据
 */
export function generatePerformanceTestData(
  count: number = 200,
  complexity: 'low' | 'medium' | 'high' = 'medium'
): HotWordItem[] {
  return generator.generatePerformanceTestData(count, complexity)
}

/**
 * 生成分层测试数据
 * 用于测试不同权重层级的渲染效果
 */
export function generateTieredTestData(totalCount: number = 100): HotWordItem[] {
  const tiers = [
    { name: '热门', count: Math.floor(totalCount * 0.1), weightRange: [80, 100] },
    { name: '常用', count: Math.floor(totalCount * 0.2), weightRange: [60, 80] },
    { name: '普通', count: Math.floor(totalCount * 0.3), weightRange: [40, 60] },
    { name: '少见', count: Math.floor(totalCount * 0.4), weightRange: [1, 40] }
  ]

  const words: HotWordItem[] = []
  let currentIndex = 0

  tiers.forEach((tier, tierIndex) => {
    for (let i = 0; i < tier.count; i++) {
      const weight = tier.weightRange[0] + Math.random() * (tier.weightRange[1] - tier.weightRange[0])

      words.push({
        text: `${tier.name}词汇${currentIndex + 1}`,
        weight: Math.round(weight * 100) / 100,
        extraData: {
          tier: tier.name,
          tierIndex,
          category: '分层测试'
        }
      })

      currentIndex++
    }
  })

  return words.sort((a, b) => b.weight - a.weight)
}

/**
 * 生成极端情况测试数据
 */
export function generateEdgeCaseTestData(): HotWordItem[] {
  return [
    // 极长文本
    {
      text: '这是一个非常非常非常长的词语用来测试渲染引擎对长文本的处理能力',
      weight: 90,
      extraData: { case: 'long_text' }
    },
    // 极短文本
    {
      text: '短',
      weight: 85,
      extraData: { case: 'short_text' }
    },
    // 数字和符号
    {
      text: '123.456%',
      weight: 80,
      extraData: { case: 'numbers_symbols' }
    },
    // 英文
    {
      text: 'VeryLongEnglishWordForTesting',
      weight: 75,
      extraData: { case: 'long_english' }
    },
    // 特殊字符
    {
      text: '特殊字符@#$%^&*()',
      weight: 70,
      extraData: { case: 'special_chars' }
    },
    // 空格
    {
      text: '包含 空格 的 词语',
      weight: 65,
      extraData: { case: 'with_spaces' }
    },
    // 混合文本
    {
      text: 'Mixed混合Text文本123',
      weight: 60,
      extraData: { case: 'mixed_text' }
    }
  ]
}

// TestDataGenerator已在上面导出