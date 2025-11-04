//profile.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    resumeCount: 0,
    templateCount: 0,
    favCount: 0
  },

  onShow: function() {
    // 每次显示页面时更新用户信息
    this.setData({
      userInfo: app.globalData.userInfo
    })
    
    // 如果已登录，加载用户数据
    if (this.data.userInfo) {
      this.loadUserData()
    }
  },

  // 点击用户信息区域
  onUserInfoTap: function() {
    if (!this.data.userInfo) {
      // 未登录则跳转到登录页
      wx.navigateTo({
        url: '/pages/login/login'
      })
    } else {
      // 已登录可以跳转到详细信息页
      wx.navigateTo({
        url: '/pages/user/detail'
      })
    }
  },

  // 加载用户数据
  loadUserData: function() {
    wx.showLoading({ title: '加载中' })
    
    // 模拟加载数据
    setTimeout(() => {
      // 调用API获取用户统计数据
      app.request('/user/stats', 'GET', {}, res => {
        wx.hideLoading()
        if (res.code === 0) {
          this.setData({
            resumeCount: res.data.resumeCount || 0,
            templateCount: res.data.templateCount || 0,
            favCount: res.data.favCount || 0
          })
        }
      })
    }, 500)
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

  // 设置
  settings: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({
      url: '/pages/settings/settings'
    })
  },

  // 意见反馈
  feedback: function() {
    wx.navigateTo({
      url: '/pages/feedback/feedback'
    })
  },

  // 关于我们
  about: function() {
    wx.navigateTo({
      url: '/pages/about/about'
    })
  },

  // 退出登录
  logout: function() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: res => {
        if (res.confirm) {
          // 调用app.js中的退出登录方法
          app.logout()
          
          // 更新页面状态
          this.setData({
            userInfo: null,
            resumeCount: 0,
            templateCount: 0,
            favCount: 0
          })
          
          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          })
        }
      }
    })
  },

  // 显示登录提示
  showLoginTip: function() {
    wx.showToast({
      title: '请先登录',
      icon: 'none',
      complete: () => {
        setTimeout(() => {
          wx.navigateTo({
            url: '/pages/login/login'
          })
        }, 1500)
      }
    })
  }
})