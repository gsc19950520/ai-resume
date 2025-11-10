//index.js
const app = getApp()

Page({
  data: {
    userInfo: { nickName: '用户' }, // 默认用户信息，确保页面不会空白
    recommendList: [
      {
        id: 1,
        title: '如何打造优秀简历',
        image: '/images/recommend1.png'
      },
      {
        id: 2,
        title: '面试技巧指南',
        image: '/images/recommend2.png'
      },
      {
        id: 3,
        title: '热门行业分析',
        image: '/images/recommend3.png'
      }
    ],
    // 新增数据
    lastInterviewScore: 85, // 默认值，避免页面空白
    hasGrowthAdvice: true, // 默认显示推荐
    growthRecommendation: '建议提升项目管理能力，可通过参与更多团队项目来积累经验'
  },

  onLoad: function () {
    // 获取用户信息
    this.setData({
      userInfo: app.globalData.userInfo
    })

    // 如果未登录，提示用户登录
    if (!this.data.userInfo) {
      this.showLoginTip()
    }

    // 加载数据
    this.loadData()
  },

  onShow: function() {
    // 页面显示时更新用户信息
    this.setData({
      userInfo: app.globalData.userInfo
    })
  },

  // 显示登录提示
  showLoginTip: function() {
    wx.showModal({
      title: '提示',
      content: '请先登录，以使用完整功能',
      confirmText: '去登录',
      success: res => {
        if (res.confirm) {
          wx.navigateTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },

  // 加载数据
  loadData: function() {
    wx.showLoading({ title: '加载中' })
    // 实际调用API获取推荐列表
    app.request('/api/recommend/list', 'GET', {}, res => {
      if (res && res.code === 0 && res.data && res.data.list) {
        this.setData({
          recommendList: res.data.list
        })
      } else {
        console.log('获取推荐列表失败或返回格式异常，使用模拟数据')
        // 继续使用页面中定义的模拟数据，不进行修改
      }
    })
    
    // 获取用户上次面试数据和推荐
    this.getUserInterviewSummary()
  
    // 无论API调用是否成功，都在2秒后隐藏加载提示
    setTimeout(() => {
      wx.hideLoading()
    }, 2000)
  },
  
  // 获取用户面试摘要和推荐
  getUserInterviewSummary: function() {
    if (!this.data.userInfo) return;
    
    // 直接使用模拟数据，避免调用不存在的接口
    this.setMockInterviewSummary();
  },
  
  // 设置模拟面试摘要数据
  setMockInterviewSummary: function() {
    // 模拟数据
    this.setData({
      lastInterviewScore: 78,
      hasGrowthAdvice: true,
      growthRecommendation: '建议提升技术深度和表达清晰度，可通过多练习项目架构和表达技巧来改善'
    })
  },

  // 创建简历
  createResume: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/create/create' })
  },

  // 我的简历
  myResumes: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/resume/list/list' })
  },

  // AI模拟面试
  aiInterview: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    
    // 检查用户是否有简历
    this.checkUserResumeStatus()
  },
  
  // 检查用户简历状态
  checkUserResumeStatus: function() {
    wx.showLoading({ title: '检查简历中...' })
    
    // 模拟获取简历列表
    setTimeout(() => {
      wx.hideLoading()
      
      // 使用模拟数据检查简历状态
      const resumeList = this.getMockResumeList()
      
      if (resumeList && resumeList.length > 0) {
        // 有简历，跳转到风格和简历选择页面 - 使用模板字符串便于代码依赖分析
        wx.navigateTo({ url: `/pages/interview/interview_style_select` })
      } else {
        // 无简历，提示用户创建或上传简历
        wx.showModal({
          title: '提示',
          content: '您还没有创建简历，请先创建或上传一份简历后再进行AI模拟面试。',
          showCancel: true,
          cancelText: '稍后',
          confirmText: '去创建简历',
          success: (res) => {
            if (res.confirm) {
              // 跳转到创建简历页面
              wx.navigateTo({ url: '/pages/create/create' })
            }
          }
        })
      }
    }, 500)
  },
  
  // 获取模拟简历列表
  getMockResumeList: function() {
    // 这里可以根据实际情况获取真实的简历列表
    // 返回模拟数据用于测试
    return [
      {
        id: '1',
        title: '前端开发工程师简历',
        occupation: '前端开发工程师',
        updateTime: '2025-11-05'
      },
      {
        id: '2',
        title: '全栈开发工程师简历',
        occupation: '全栈开发工程师',
        updateTime: '2025-10-30'
      }
    ];
  },

  // 城市薪资匹配功能已整合到面试报告中

  // AI职业成长报告
  aiCareerReport: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/growth/report' })
  },
  
  // 简历优化功能
  resumeOptimization: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/resume/optimize' })
  },

  // 简历模板
  resumeTemplates: function() {
    wx.navigateTo({ url: '/pages/template/list/list' })
  },

  // 跳转到详情页（临时提示）
  goToDetail: function(e) {
    wx.showToast({
      title: '详情页功能开发中',
      icon: 'none'
    })
  },

  // 下拉刷新
  onPullDownRefresh: function() {
    this.loadData()
    wx.stopPullDownRefresh()
  }
})