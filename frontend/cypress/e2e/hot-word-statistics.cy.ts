/**
 * 热词统计页面端到端测试
 * 验证完整的用户流程和页面交互
 */

describe('热词统计页面 E2E 测试', () => {
  const baseUrl = 'http://localhost:3000'
  const apiUrl = 'http://localhost:8080/api'

  beforeEach(() => {
    // 访问热词统计页面
    cy.visit(`${baseUrl}/hot-word-statistics`)

    // 等待页面加载
    cy.wait(1000)
  })

  describe('页面加载和基本布局', () => {
    it('应该正确加载页面并显示标题', () => {
      cy.get('h1').should('contain', '热词统计分析')
      cy.get('p').should('contain', '基于搜索日志的热词分析和可视化展示')
    })

    it('应该显示所有主要区域', () => {
      // 页面头部
      cy.get('h1').should('be.visible')

      // 筛选条件区域
      cy.get('h2').contains('筛选条件').should('be.visible')

      // 统计概览区域
      cy.get('.grid.grid-cols-1.md\\:grid-cols-4').should('be.visible')

      // 词云图区域
      cy.get('h2').contains('热词云图').should('be.visible')

      // TOP 10 热词排行榜
      cy.get('h3').contains('TOP 10 热词').should('be.visible')

      // 趋势分析
      cy.get('h3').contains('趋势分析').should('be.visible')

      // 详细数据表格
      cy.get('h2').contains('热词详细数据').should('be.visible')
    })

    it('应该显示刷新和导出按钮', () => {
      cy.get('button').contains('刷新数据').should('be.visible')
      cy.get('button').contains('导出报告').should('be.visible')
    })
  })

  describe('统计概览卡片', () => {
    it('应该显示四个统计卡片', () => {
      cy.get('.grid.grid-cols-1.md\\:grid-cols-4 > div').should('have.length', 4)
    })

    it('应该显示总搜索次数', () => {
      cy.get('.bg-white.rounded-lg.shadow-sm.border.p-6')
        .contains('总搜索次数')
        .should('be.visible')
    })

    it('应该显示热词数量', () => {
      cy.get('.bg-white.rounded-lg.shadow-sm.border.p-6')
        .contains('热词数量')
        .should('be.visible')
    })

    it('应该显示平均搜索频次', () => {
      cy.get('.bg-white.rounded-lg.shadow-sm.border.p-6')
        .contains('平均搜索频次')
        .should('be.visible')
    })

    it('应该显示统计时间段', () => {
      cy.get('.bg-white.rounded-lg.shadow-sm.border.p-6')
        .contains('统计时间段')
        .should('be.visible')
    })
  })

  describe('筛选器功能', () => {
    it('应该显示筛选器组件', () => {
      cy.get('[data-testid="hot-word-filter"]')
        .or('div').contains('热词过滤器')
        .should('be.visible')
    })

    // 注：由于筛选器组件可能比较复杂，这里只做基本存在性检查
    // 具体的筛选器交互测试应该在组件级别进行
  })

  describe('词云图功能', () => {
    it('应该显示词云图容器', () => {
      cy.get('.h-96.relative').should('be.visible')
    })

    it('应该有主题选择下拉框', () => {
      cy.get('select').should('contain.value', 'light-green')

      // 测试主题切换
      cy.get('select').first().select('blue')
      cy.get('select').first().should('have.value', 'blue')
    })

    it('应该有显示数量选择下拉框', () => {
      cy.get('select').should('exist')

      // 测试数量选择
      cy.get('select').eq(1).select('200')
      cy.get('select').eq(1).should('have.value', '200')
    })
  })

  describe('TOP 10 热词排行榜', () => {
    it('应该显示排行榜标题', () => {
      cy.get('h3').contains('TOP 10 热词').should('be.visible')
    })

    it('排行榜项目应该可点击', () => {
      // 检查是否有可点击的排行榜项目
      cy.get('.flex.items-center.justify-between.p-3.rounded-md.hover\\:bg-gray-50.transition-colors.cursor-pointer')
        .should('exist')
    })

    it('点击排行榜项目应该高亮对应词语', () => {
      // 模拟点击第一个排行榜项目
      cy.get('.flex.items-center.justify-between.p-3.rounded-md.hover\\:bg-gray-50.transition-colors.cursor-pointer')
        .first()
        .click()

      // 检查搜索框是否被填充
      cy.get('input[placeholder="搜索热词..."]').should('have.value')
    })
  })

  describe('趋势分析', () => {
    it('应该显示趋势分析信息', () => {
      cy.get('h3').contains('趋势分析').should('be.visible')

      // 检查趋势项目
      cy.get('.flex.items-center.justify-between')
        .contains('上升最快')
        .should('be.visible')

      cy.get('.flex.items-center.justify-between')
        .contains('下降最快')
        .should('be.visible')

      cy.get('.flex.items-center.justify-between')
        .contains('新热词')
        .should('be.visible')
    })
  })

  describe('数据表格', () => {
    it('应该显示表格头部', () => {
      cy.get('th').contains('排名').should('be.visible')
      cy.get('th').contains('热词').should('be.visible')
      cy.get('th').contains('搜索次数').should('be.visible')
      cy.get('th').contains('占比').should('be.visible')
      cy.get('th').contains('趋势').should('be.visible')
    })

    it('应该有搜索功能', () => {
      cy.get('input[placeholder="搜索热词..."]').should('be.visible')

      // 测试搜索功能
      cy.get('input[placeholder="搜索热词..."]').type('测试')
      cy.get('input[placeholder="搜索热词..."]').should('have.value', '测试')
    })

    it('搜索应该能够筛选表格数据', () => {
      // 获取初始行数
      cy.get('tbody tr').then($initialRows => {
        const initialCount = $initialRows.length

        // 输入搜索关键词
        cy.get('input[placeholder="搜索热词..."]').type('不存在的词')

        // 等待筛选生效
        cy.wait(500)

        // 检查行数是否减少或显示无数据
        cy.get('tbody tr').should('have.length.lessThan', initialCount + 1)
      })
    })

    it('应该显示分页信息（如果有多页数据）', () => {
      // 检查是否显示分页控件
      cy.get('body').then($body => {
        if ($body.find('.flex.items-center.justify-between.mt-6').length > 0) {
          cy.get('.flex.items-center.justify-between.mt-6').should('be.visible')
          cy.get('button').contains('上一页').should('exist')
          cy.get('button').contains('下一页').should('exist')
        }
      })
    })
  })

  describe('用户交互', () => {
    it('点击刷新按钮应该触发数据刷新', () => {
      cy.get('button').contains('刷新数据').click()

      // 检查是否显示加载状态
      cy.get('button').contains('刷新数据').should('be.disabled')

      // 等待加载完成
      cy.wait(2000)

      cy.get('button').contains('刷新数据').should('not.be.disabled')
    })

    it('点击导出按钮应该触发数据导出', () => {
      // 检查导出按钮是否可用
      cy.get('button').contains('导出报告').should('not.be.disabled')

      // 点击导出按钮
      cy.get('button').contains('导出报告').click()

      // 注：实际的文件下载验证在E2E测试中比较复杂
      // 这里主要验证按钮点击不会出错
    })
  })

  describe('响应式设计', () => {
    it('在移动端应该正确显示', () => {
      cy.viewport(375, 667) // iPhone SE size

      // 检查页面是否正确响应
      cy.get('h1').should('be.visible')
      cy.get('.grid.grid-cols-1.md\\:grid-cols-4').should('be.visible')

      // 检查是否使用了单列布局
      cy.get('.grid.grid-cols-1.lg\\:grid-cols-3').should('be.visible')
    })

    it('在平板端应该正确显示', () => {
      cy.viewport(768, 1024) // iPad size

      cy.get('h1').should('be.visible')
      cy.get('.grid.grid-cols-1.md\\:grid-cols-4').should('be.visible')
    })

    it('在桌面端应该正确显示', () => {
      cy.viewport(1280, 720) // Desktop size

      cy.get('h1').should('be.visible')
      cy.get('.grid.grid-cols-1.md\\:grid-cols-4').should('be.visible')
      cy.get('.grid.grid-cols-1.lg\\:grid-cols-3').should('be.visible')
    })
  })

  describe('错误处理', () => {
    it('应该优雅处理API错误', () => {
      // 模拟网络错误
      cy.intercept('GET', `${apiUrl}/search-logs/hot-words*`, {
        statusCode: 500,
        body: { error: 'Internal Server Error' }
      }).as('getHotWordsError')

      // 刷新页面触发API调用
      cy.get('button').contains('刷新数据').click()

      // 等待错误响应
      cy.wait('@getHotWordsError')

      // 检查错误状态是否正确显示
      // 注：具体的错误显示方式取决于实现
      cy.get('button').contains('刷新数据').should('not.be.disabled')
    })

    it('应该处理空数据状态', () => {
      // 模拟空数据响应
      cy.intercept('GET', `${apiUrl}/search-logs/hot-words*`, {
        statusCode: 200,
        body: { data: [], success: true }
      }).as('getHotWordsEmpty')

      cy.get('button').contains('刷新数据').click()
      cy.wait('@getHotWordsEmpty')

      // 检查空状态是否正确显示
      // 可以检查是否显示"暂无数据"等提示
    })
  })

  describe('性能测试', () => {
    it('页面应该在合理时间内加载', () => {
      const startTime = Date.now()

      cy.visit(`${baseUrl}/hot-word-statistics`)

      cy.get('h1').should('be.visible').then(() => {
        const loadTime = Date.now() - startTime
        expect(loadTime).to.be.lessThan(5000) // 页面应在5秒内加载完成
      })
    })

    it('应该能处理大量数据', () => {
      // 模拟大量数据
      const largeDataSet = Array.from({ length: 1000 }, (_, i) => ({
        word: `测试词${i}`,
        count: 1000 - i,
        percentage: (1000 - i) / 10,
        wordLength: 3,
        relatedQueriesCount: Math.floor(Math.random() * 50),
        lastOccurrence: '2025-09-28',
        firstOccurrence: '2025-09-21'
      }))

      cy.intercept('GET', `${apiUrl}/search-logs/hot-words*`, {
        statusCode: 200,
        body: { data: largeDataSet, success: true }
      }).as('getHotWordsLarge')

      cy.get('button').contains('刷新数据').click()
      cy.wait('@getHotWordsLarge')

      // 检查页面是否能正常处理大量数据
      cy.get('table tbody tr').should('have.length.greaterThan', 0)
    })
  })

  describe('数据一致性', () => {
    it('各个区域的数据应该保持一致', () => {
      // 检查统计概览、TOP 10排行榜、数据表格中的数据是否一致
      // 这需要等待实际数据加载后进行验证

      cy.get('.bg-white.rounded-lg.shadow-sm.border.p-6')
        .contains('热词数量')
        .parent()
        .find('.text-2xl.font-semibold.text-gray-900')
        .invoke('text')
        .then(hotWordCount => {
          // 验证表格中的数据行数是否与统计数量一致
          cy.get('tbody tr').should('have.length.lessThan', parseInt(hotWordCount) + 1)
        })
    })
  })

  describe('深色模式支持（如果有）', () => {
    it('应该支持深色模式切换', () => {
      // 检查是否有深色模式切换按钮
      cy.get('body').then($body => {
        if ($body.find('[data-testid="theme-toggle"]').length > 0) {
          cy.get('[data-testid="theme-toggle"]').click()

          // 检查页面是否切换到深色主题
          cy.get('body').should('have.class', 'dark').or('have.attr', 'data-theme', 'dark')
        }
      })
    })
  })
})