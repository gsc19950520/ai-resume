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
    // 模拟加载数据
    wx.showLoading({ title: '加载中' })
    setTimeout(() => {
      wx.hideLoading()
    }, 1000)
  },

  // 创建简历
  createResume: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({
      url: '/pages/create/create'
    })
  },

  // 我的简历
  myResumes: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({
      url: '/pages/resume/list'
    })
  },

  // 简历模板
  resumeTemplates: function() {
    wx.navigateTo({
      url: '/pages/template/list'
    })
  },

  // 跳转到详情页
  goToDetail: function(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/detail/detail?id=${id}`
    })
  },

  // 下拉刷新
  onPullDownRefresh: function() {
    this.loadData()
    wx.stopPullDownRefresh()
  }
})