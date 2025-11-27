// pages/growth/report.js
const app = getApp()
import { get, post } from '../../utils/request.js';

Page({
  data: {
    loading: true,
    loadingText: '正在生成成长报告...',
    reportData: null,
    error: false,
    errorMessage: '',
    userInfo: null,
    chartData: {
      dates: [],
      scores: {
        total: [],
        tech: [],
        logic: [],
        clarity: [],
        depth: []
      }
    },
    weakPoints: [],
    recommendedDirection: '',
    growthPlan: []
  },

  onLoad: function() {
    this.setData({
      userInfo: app.globalData.userInfo
    })
    this.loadGrowthReport()
  },

  // 加载成长报告数据
  loadGrowthReport: function() {
    this.setData({ loading: true })
    
    // 调用历史分析API
    get('/api/user/history/analyze')
      .then(res => {
        if (res && res.code === 0) {
          // 调用成长规划API
          this.getGrowthPlan(res.data)
        } else {
          // 使用模拟数据
          this.useMockData()
        }
      })
      .catch(() => {
        // 使用模拟数据
        this.useMockData()
      })
  },

  // 获取成长规划
  getGrowthPlan: function(historyData) {
    post('/api/user/growth/plan', historyData)
      .then(res => {
        if (res && res.code === 0) {
          this.renderReport(res.data)
        } else {
          // 使用模拟数据
          this.useMockData()
        }
      })
      .catch(() => {
        // 使用模拟数据
        this.useMockData()
      })
  },

  // 渲染报告
  renderReport: function(data) {
    this.setData({
      reportData: data,
      chartData: data.chartData,
      weakPoints: data.weakPoints || [],
      recommendedDirection: data.recommendedDirection || '',
      growthPlan: data.growthPlan || [],
      loading: false
    })
    
    // 绘制成长曲线图
    if (data.chartData) {
      this.drawGrowthChart()
    }
  },

  // 绘制成长曲线图
  drawGrowthChart: function() {
    const ctx = wx.createCanvasContext('growthChart')
    const { dates, scores } = this.data.chartData
    
    // 模拟绘制逻辑
    ctx.setFillStyle('#4A90E2')
    ctx.fillRect(50, 50, 200, 150)
    ctx.draw()
  },

  // 使用模拟数据
  useMockData: function() {
    const mockData = {
      title: 'AI职业成长分析报告',
      generateDate: new Date().toLocaleDateString(),
      userStats: {
        totalInterviews: 8,
        avgTotalScore: 78,
        bestScore: 85,
        growthRate: 12
      },
      chartData: {
        dates: ['01/15', '02/20', '03/10', '04/05', '05/25', '06/15', '07/10', '08/01'],
        scores: {
          total: [65, 68, 70, 72, 75, 76, 79, 81],
          tech: [6.2, 6.5, 6.8, 7.0, 7.3, 7.5, 7.7, 7.9],
          logic: [6.0, 6.3, 6.7, 7.2, 7.4, 7.6, 7.8, 8.0],
          clarity: [6.5, 6.7, 6.9, 7.1, 7.3, 7.4, 7.6, 7.7],
          depth: [5.8, 6.0, 6.3, 6.5, 6.8, 7.0, 7.2, 7.4]
        }
      },
      weakPoints: [
        { name: '技术深度', score: 7.4, improvement: 27.6 },
        { name: '项目架构设计', score: 7.0, improvement: 16.7 },
        { name: '团队协作能力', score: 7.2, improvement: 20.0 }
      ],
      recommendedDirection: '从后端开发工程师向技术架构师发展',
      growthPlan: [
        {
          period: '近期目标（3-6个月）',
          color: '#50E3C2',
          goals: [
            '深化核心技术栈，重点提升架构设计能力',
            '学习大型项目的性能优化技术',
            '提升团队协作和沟通表达能力'
          ]
        },
        {
          period: '中期目标（6-12个月）',
          color: '#F5A623',
          goals: [
            '参与至少一个大型项目的架构设计',
            '培养技术分享习惯，提升影响力',
            '学习项目管理知识，尝试带领小团队'
          ]
        },
        {
          period: '长期目标（1-3年）',
          color: '#D0021B',
          goals: [
            '成长为技术架构师，负责复杂系统设计',
            '建立个人技术品牌，成为行业专家',
            '具备独立负责产品线技术方向的能力'
          ]
        }
      ],
      resourceRecommendations: [
        '《大型网站技术架构》',
        '《设计模式》',
        '《重构：改善既有代码的设计》',
        '架构师相关在线课程',
        '行业技术会议和沙龙'
      ]
    }
    
    this.renderReport(mockData)
  },

  // 下载PDF报告
  downloadPDFReport: function() {
    wx.showLoading({
      title: '正在生成PDF...',
    })
    
    wx.request({
      url: '/api/report/growth/pdf',
      method: 'POST',
      data: { reportData: this.data.reportData },
      success: (res) => {
        if (res.data && res.data.code === 0) {
          const pdfUrl = res.data.data.pdfUrl
          wx.downloadFile({
            url: pdfUrl,
            success: (downloadRes) => {
              if (downloadRes.statusCode === 200) {
                wx.openDocument({
                  filePath: downloadRes.tempFilePath,
                  showMenu: true
                })
              }
            }
          })
        }
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },

  // 分享报告
  shareReport: function() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
  },

  // 更新报告
  refreshReport: function() {
    this.loadGrowthReport()
  },

  // 分享到朋友圈
  onShareAppMessage: function() {
    return {
      title: '我的AI职业成长分析报告',
      path: '/pages/growth/report',
      imageUrl: '/assets/images/share-cover.png'
    }
  }
})