//index.js
const app = getApp()

Page({
  data: {
    userInfo: null,
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
    ]
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
      wx.hideLoading()
      if (res && res.code === 0 && res.data && res.data.list) {
        this.setData({
          recommendList: res.data.list
        })
      } else {
        console.log('获取推荐列表失败或返回格式异常，使用模拟数据')
        // 继续使用页面中定义的模拟数据，不进行修改
      }
    })
  
    // 无论API调用是否成功，都在2秒后隐藏加载提示
    setTimeout(() => {
      wx.hideLoading()
    }, 2000)
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
    wx.navigateTo({ url: '/pages/interview/interview' })
  },

  // 城市薪资匹配
  salaryMatcher: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/salary/matcher' })
  },

  // AI职业成长报告
  aiCareerReport: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/career/report' })
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