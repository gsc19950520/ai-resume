// report.js
const app = getApp()

Page({
  data: {
    sessionId: '',
    loading: true,
    
    // 评分数据
    totalScore: 0,
    techScore: 0,
    logicScore: 0,
    clarityScore: 0,
    depthScore: 0,
    
    // 薪资数据
    estimatedYears: '',
    salaryRange: '',
    confidence: 0,
    city: '北京',
    jobType: '前端开发',
    
    // 会话记录
    sessionLog: [],
    
    // 成长建议
    growthAdvice: ''
  },

  onLoad: function(options) {
    const { sessionId } = options || {}
    this.setData({ sessionId })
    
    // 尝试从全局获取数据
    if (app.globalData.interviewResult) {
      this.renderReport(app.globalData.interviewResult)
    } else {
      // 从后端获取报告数据
      this.loadReport()
    }
  },

  // 加载报告数据
  loadReport: function() {
    this.setData({ loading: true })
    
    // 调用后端API获取报告
    app.request('/api/interview/finish', 'POST', { sessionId: this.data.sessionId }, res => {
      if (res && res.code === 0 && res.data) {
        this.renderReport(res.data)
      } else {
        // 使用模拟数据
        this.useMockData()
      }
    }, err => {
      console.error('加载报告失败:', err)
      // 使用模拟数据
      this.useMockData()
    })
  },

  // 渲染报告
  renderReport: function(data) {
    const { aggregatedScores, total_score, salaryInfo, sessionLog } = data
    
    this.setData({
      totalScore: Math.round(total_score),
      techScore: aggregatedScores.tech,
      logicScore: aggregatedScores.logic,
      clarityScore: aggregatedScores.clarity,
      depthScore: aggregatedScores.depth,
      estimatedYears: salaryInfo.ai_estimated_years,
      salaryRange: salaryInfo.ai_salary_range,
      confidence: salaryInfo.confidence,
      sessionLog: sessionLog || [],
      loading: false
    })
    
    // 生成成长建议
    this.generateGrowthAdvice(aggregatedScores)
    
    // 延迟绘制图表，确保DOM已渲染
    setTimeout(() => {
      this.drawRadarChart()
      this.drawTrendChart()
    }, 100)
  },

  // 使用模拟数据（备用）
  useMockData: function() {
    const mockData = {
      aggregatedScores: {
        tech: 7.5,
        logic: 8.0,
        clarity: 7.8,
        depth: 7.2
      },
      total_score: 76,
      salaryInfo: {
        ai_estimated_years: '3-5年',
        ai_salary_range: '20K-28K',
        confidence: 0.85
      },
      sessionLog: [
        {
          question: '请介绍一下你最熟悉的一个项目，重点说明你负责的部分和使用的技术栈。',
          answer: '我最熟悉的项目是一个企业管理系统的前端开发。我负责了整个系统的前端架构设计，使用了React、Redux、TypeScript等技术栈。',
          score: { tech: 7, logic: 8, clarity: 8, depth: 7 },
          feedback: '回答清晰地介绍了项目情况和技术栈，但可以进一步深入具体的技术实现细节。',
          matchedPoints: ['覆盖了项目概况', '明确了技术栈', '说明了负责内容']
        },
        {
          question: '在项目中遇到的最大技术难点是什么？你是如何解决的？',
          answer: '最大的技术难点是性能优化问题。我们通过组件懒加载、状态管理优化和缓存策略改进，成功将页面加载时间从3秒降低到1秒以内。',
          score: { tech: 8, logic: 8, clarity: 7, depth: 8 },
          feedback: '很好地描述了技术难点和解决方案，并提供了量化的优化成果。',
          matchedPoints: ['明确了技术难点', '提供了具体解决方案', '有量化的优化成果']
        }
      ]
    }
    
    this.renderReport(mockData)
  },

  // 生成成长建议
  generateGrowthAdvice: function(scores) {
    let advice = ''
    
    if (scores.tech < 7) {
      advice += '建议加强技术深度学习，尤其是核心技术栈的底层原理；\n'
    }
    if (scores.logic < 7) {
      advice += '可以多练习结构化思维，提升问题分析和解决能力；\n'
    }
    if (scores.clarity < 7) {
      advice += '表达能力还有提升空间，建议多练习技术分享和演讲；\n'
    }
    if (scores.depth < 7) {
      advice += '回答深度需要加强，尝试从多角度思考技术问题；\n'
    }
    
    if (!advice) {
      advice = '您的表现已经相当出色！建议继续保持学习热情，关注行业前沿技术，并在实践中不断积累经验。'
    }
    
    this.setData({ growthAdvice: advice })
  },

  // 绘制雷达图
  drawRadarChart: function() {
    try {
      const ctx = wx.createCanvasContext('radarCanvas')
      const { techScore, logicScore, clarityScore, depthScore } = this.data
      
      const data = [techScore, logicScore, clarityScore, depthScore]
      const labels = ['技术深度', '逻辑表达', '表达清晰度', '回答深度']
      const maxValue = 10
      
      const width = wx.getSystemInfoSync().windowWidth - 80
      const height = 400
      const centerX = width / 2
      const centerY = height / 2
      const radius = Math.min(centerX, centerY) - 40
      
      // 绘制雷达背景
      this.drawRadarBackground(ctx, centerX, centerY, radius, 4)
      
      // 绘制数据区域
      this.drawRadarData(ctx, centerX, centerY, radius, data)
      
      // 绘制标签
      this.drawRadarLabels(ctx, centerX, centerY, radius, labels)
      
      ctx.draw()
    } catch (error) {
      console.error('绘制雷达图失败:', error)
    }
  },

  // 绘制雷达背景
  drawRadarBackground: function(ctx, centerX, centerY, radius, sides) {
    const angleStep = Math.PI * 2 / sides
    
    // 绘制网格线
    for (let level = 1; level <= 5; level++) {
      const levelRadius = radius * level / 5
      ctx.beginPath()
      for (let i = 0; i < sides; i++) {
        const angle = angleStep * i - Math.PI / 2
        const x = centerX + Math.cos(angle) * levelRadius
        const y = centerY + Math.sin(angle) * levelRadius
        
        if (i === 0) {
          ctx.moveTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
      }
      ctx.closePath()
      ctx.setStrokeStyle('#e0e0e0')
      ctx.setLineWidth(2)
      ctx.stroke()
    }
  },

  // 绘制雷达数据
  drawRadarData: function(ctx, centerX, centerY, radius, data) {
    const angleStep = Math.PI * 2 / data.length
    
    ctx.beginPath()
    for (let i = 0; i < data.length; i++) {
      const value = data[i] / 10
      const angle = angleStep * i - Math.PI / 2
      const x = centerX + Math.cos(angle) * radius * value
      const y = centerY + Math.sin(angle) * radius * value
      
      if (i === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
    }
    ctx.closePath()
    ctx.setFillStyle('rgba(7, 193, 96, 0.2)')
    ctx.setStrokeStyle('#07c160')
    ctx.setLineWidth(4)
    ctx.fill()
    ctx.stroke()
  },

  // 绘制雷达图标签
  drawRadarLabels: function(ctx, centerX, centerY, radius, labels) {
    const angleStep = Math.PI * 2 / labels.length
    
    for (let i = 0; i < labels.length; i++) {
      const angle = angleStep * i - Math.PI / 2
      const labelRadius = radius + 30
      const x = centerX + Math.cos(angle) * labelRadius
      const y = centerY + Math.sin(angle) * labelRadius
      
      ctx.setFontSize(24)
      ctx.setTextAlign('center')
      ctx.setTextBaseline('middle')
      ctx.setFillStyle('#333333')
      ctx.fillText(labels[i], x, y)
    }
  },

  // 绘制趋势图
  drawTrendChart: function() {
    try {
      const ctx = wx.createCanvasContext('trendCanvas')
      const width = wx.getSystemInfoSync().windowWidth - 80
      const height = 300
      const padding = 40
      
      // 模拟薪资趋势数据
      const years = ['0-1年', '1-3年', '3-5年', '5-8年', '8+年']
      const salaries = [12000, 18000, 25000, 35000, 50000]
      
      // 计算坐标轴范围
      const maxSalary = Math.max(...salaries) * 1.2
      
      // 绘制坐标轴
      ctx.beginPath()
      ctx.moveTo(padding, padding)
      ctx.lineTo(padding, height - padding)
      ctx.lineTo(width - padding, height - padding)
      ctx.setStrokeStyle('#333333')
      ctx.setLineWidth(2)
      ctx.stroke()
      
      // 绘制薪资曲线
      ctx.beginPath()
      for (let i = 0; i < years.length; i++) {
        const x = padding + (i / (years.length - 1)) * (width - 2 * padding)
        const y = height - padding - (salaries[i] / maxSalary) * (height - 2 * padding)
        
        if (i === 0) {
          ctx.moveTo(x, y)
        } else {
          ctx.lineTo(x, y)
        }
        
        // 绘制数据点
        ctx.beginPath()
        ctx.arc(x, y, 6, 0, Math.PI * 2)
        ctx.setFillStyle('#07c160')
        ctx.fill()
      }
      ctx.setStrokeStyle('#07c160')
      ctx.setLineWidth(3)
      ctx.stroke()
      
      // 绘制年份标签
      for (let i = 0; i < years.length; i++) {
        const x = padding + (i / (years.length - 1)) * (width - 2 * padding)
        ctx.setFontSize(20)
        ctx.setTextAlign('center')
        ctx.setTextBaseline('top')
        ctx.setFillStyle('#666666')
        ctx.fillText(years[i], x, height - padding + 10)
      }
      
      // 绘制当前薪资范围标记
      if (this.data.salaryRange) {
        const rangeText = this.data.salaryRange
        ctx.setFontSize(24)
        ctx.setTextAlign('right')
        ctx.setFillStyle('#ff9500')
        ctx.fillText('当前推荐: ' + rangeText, width - padding, padding + 20)
      }
      
      ctx.draw()
    } catch (error) {
      console.error('绘制趋势图失败:', error)
    }
  },

  // 获取评分描述
  getScoreDescription: function(score) {
    if (score >= 90) return '优秀 - 面试表现出色'
    if (score >= 80) return '良好 - 有竞争力的表现'
    if (score >= 70) return '中等 - 表现尚可'
    if (score >= 60) return '及格 - 有提升空间'
    return '需加强 - 建议多练习'
  },

  // 获取匹配度颜色
  getConfidenceColor: function(confidence) {
    if (confidence >= 0.9) return '#07c160'
    if (confidence >= 0.7) return '#34c759'
    if (confidence >= 0.5) return '#ff9500'
    return '#ff3b30'
  },

  // 计算问题得分
  getQuestionScore: function(scoreObj) {
    const avg = (scoreObj.tech + scoreObj.logic + scoreObj.clarity + scoreObj.depth) / 4
    return Math.round(avg)
  },

  // 下载报告
  downloadReport: function() {
    wx.showLoading({ title: '生成报告中...' })
    
    // 模拟下载过程
    setTimeout(() => {
      wx.hideLoading()
      wx.showModal({
        title: '报告生成成功',
        content: 'PDF报告已生成，是否下载？',
        success: res => {
          if (res.confirm) {
            wx.showToast({
              title: '开始下载',
              icon: 'success'
            })
            // 这里应该调用实际的下载API
          }
        }
      })
    }, 2000)
  },

  // 返回上一页
  goBack: function() {
    wx.navigateBack()
  }
})