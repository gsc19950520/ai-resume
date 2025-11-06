// pages/interview/history.js
const app = getApp()

Page({
  data: {
    interviewList: [],
    loading: true,
    hasMore: true
  },

  onLoad: function() {
    this.loadInterviewHistory()
  },

  // 加载面试历史记录
  loadInterviewHistory: function() {
    wx.showLoading({ title: '加载中' })
    
    // 获取用户ID
    const userId = app.globalData.userInfo?.id || wx.getStorageSync('userId') || 'test_user'
    
    app.request('/api/interview/history', 'GET', { userId: userId }, res => {
      wx.hideLoading()
      this.setData({ loading: false })
      
      if (res && res.code === 0 && res.data) {
        // 适配后端返回的数据结构
        const listData = Array.isArray(res.data) ? res.data : (res.data.list || [])
        this.setData({
          interviewList: listData,
          hasMore: res.data.hasMore || false
        })
      } else {
        console.error('获取面试历史失败:', res)
        wx.showToast({
          title: '获取面试历史失败',
          icon: 'none'
        })
      }
    }, error => {
      wx.hideLoading()
      this.setData({ loading: false })
      console.error('请求面试历史失败:', error)
      wx.showToast({
        title: '网络请求失败',
        icon: 'none'
      })
    })
  },

  // 查看面试报告
  viewReport: function(e) {
    const sessionId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/report/report?sessionId=${sessionId}`
    })
  },

  // 开始新面试
  startNewInterview: function() {
    wx.navigateTo({
      url: '/pages/interview/interview'
    })
  },

  // 下拉刷新
  onPullDownRefresh: function() {
    this.loadInterviewHistory()
    wx.stopPullDownRefresh()
  }
})