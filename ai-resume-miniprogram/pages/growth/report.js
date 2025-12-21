// pages/growth/report.js
const app = getApp()
import { get, post, getStream } from '../../utils/request.js';

Page({
  data: {
    loading: true,
    loadingText: '正在生成成长报告...',
    progressPercentage: 0,
    progressStage: '',
    reportData: null,
    error: false,
    errorMessage: '',
    userInfo: null,
    chartData: {
      dates: [],
      scores: {
        total: [],
        tech: [],
        depth: []
        // 已移除: logic, clarity
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
    this.setData({ loading: true, error: false, errorMessage: '' })
    
    // 调用历史分析API（流式请求）
    getStream('/api/user/history/analyze', {}, {
      onEvent: (event) => {
        // 处理不同类型的SSE事件
        switch (event.name) {
          case 'interview-count-error':
            // 用户面试次数不足
            this.setData({
              loading: false,
              error: true,
              errorMessage: event.data
            });
            break;
          
          case 'new-interview-found':
            // 发现新的面试记录，正在重新生成报告
            this.setData({
              loadingText: event.data
            });
            break;
          
          case 'report-found':
            // 找到现有报告
            this.setData({
              loadingText: event.data
            });
            break;
          
          case 'no-report-found':
            // 没有找到现有报告，正在生成新报告
            this.setData({
              loadingText: event.data
            });
            break;
          
          case 'report-content':
            // 收到报告内容
            this.renderReport(event.data);
            break;
          
          case 'report':
            // 处理流式报告内容（旧格式兼容）
            break;
          
          case 'progress':
            // 处理进度更新
            try {
              const progressData = JSON.parse(event.data);
              this.setData({
                progressPercentage: progressData.percentage || 0,
                progressStage: progressData.stage || ''
              });
            } catch (e) {
              console.error('解析进度数据失败:', e);
            }
            break;
          
          case 'error':
            // 发生错误
            this.setData({
              loading: false,
              error: true,
              errorMessage: '生成成长报告失败: ' + event.data
            });
            break;
        }
      },
      onComplete: () => {
        // 处理完成事件
      },
      onError: (error) => {
        console.error('获取成长报告失败:', error);
        this.setData({
          loading: false,
          error: true,
          errorMessage: '生成成长报告失败: 网络异常'
        });
      },
      onReconnect: (retryCount, maxRetries) => {
        // 显示重连提示
        this.setData({
          loadingText: `网络连接不稳定，正在重试 (${retryCount}/${maxRetries})...`,
          progressStage: '正在重新连接服务器...'
        });
      }
    });
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
    // 从后端返回的reportContent中提取需要的数据
    const reportContent = data.reportContent || {};
    
    // 数据映射：将后端字段映射到前端需要的结构
    const chartData = reportContent.visualizationData || {};
    const weakPoints = reportContent.improvements || [];
    const aiSuggestions = reportContent.aiSuggestions || [];
    
    // 从aiSuggestions中提取推荐方向和成长计划
    const recommendedDirection = aiSuggestions.length > 0 ? aiSuggestions[0] : '';
    const growthPlan = this.formatGrowthPlan(aiSuggestions);
    
    this.setData({
      reportData: data,
      chartData: chartData,
      weakPoints: weakPoints,
      recommendedDirection: recommendedDirection,
      growthPlan: growthPlan,
      loading: false
    })
    
    // 绘制成长曲线图
    if (chartData) {
      this.drawGrowthChart()
    }
  },

  // 绘制成长曲线图
  drawGrowthChart: function() {
    const ctx = wx.createCanvasContext('growthChart')
    const { dates, scores } = this.data.chartData
    
    // 模拟绘制逻辑 - 只处理总分、技术和深度三个数据系列
    ctx.setFillStyle('#4A90E2')
    ctx.fillRect(50, 50, 200, 150)
    ctx.draw()
  },
  
  // 格式化成长计划
  formatGrowthPlan: function(aiSuggestions) {
    // 从aiSuggestions数组中提取并构建成长计划结构
    // 第0项作为推荐方向，后面的项按时间维度分组
    const planItems = aiSuggestions.slice(1); // 排除第一个推荐方向项
    
    // 构建成长计划 - 按近期、中期、长期分组
    const growthPlan = [
      {
        period: '近期目标（3-6个月）',
        color: '#50E3C2',
        goals: planItems.slice(0, 3) // 前3项作为近期目标
      },
      {
        period: '中期目标（6-12个月）',
        color: '#F5A623',
        goals: planItems.slice(3, 6) // 中间3项作为中期目标
      },
      {
        period: '长期目标（1-3年）',
        color: '#D0021B',
        goals: planItems.slice(6) // 剩余项作为长期目标
      }
    ];
    
    return growthPlan;
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
          depth: [5.8, 6.0, 6.3, 6.5, 6.8, 7.0, 7.2, 7.4]
          // 已移除: logic, clarity
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
    
    post('/api/report/growth/pdf', { reportData: this.data.reportData })
      .then(res => {
        if (res && res.code === 0) {
          const pdfUrl = res.data.pdfUrl
          wx.downloadFile({
            url: pdfUrl,
            success: (downloadRes) => {
              if (downloadRes.statusCode === 200) {
                wx.openDocument({
                  filePath: downloadRes.tempFilePath,
                  showMenu: true
                })
              }
            },
            complete: () => {
              wx.hideLoading()
            }
          })
        } else {
          wx.hideLoading()
        }
      })
      .catch(err => {
        console.error('生成PDF失败:', err)
        wx.hideLoading()
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