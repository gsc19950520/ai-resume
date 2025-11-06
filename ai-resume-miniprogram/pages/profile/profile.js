//profile.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    resumeCount: 0,
    templateCount: 0,
    favCount: 0,
    interviewCount: 0
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
    
    // 直接调用API获取用户统计数据
    app.request('/api/user/stats', 'GET', {}, res => {
      wx.hideLoading()
      if (res && res.code === 0 && res.data) {
        this.setData({
          resumeCount: res.data.resumeCount || 0,
          templateCount: res.data.templateCount || 0,
          favCount: res.data.favCount || 0,
          interviewCount: res.data.interviewCount || 0
        })
      } else {
        console.log('获取用户统计数据失败或返回格式异常')
      }
    })
  },

  // 我的简历
  myResumes: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/resume/list/list' })
  },

  // 面试历史
  interviewHistory: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/interview/history' })
  },

  // 设置
  settings: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    // 创建设置页面的临时实现
    wx.showModal({
      title: '设置',
      content: '功能开发中，敬请期待！',
      showCancel: false
    })
  },

  // 意见反馈
  feedback: function() {
    wx.showModal({
      title: '意见反馈',
      content: '如有问题或建议，请联系我们：support@airesume.com',
      showCancel: false
    })
  },

  // 关于我们
  about: function() {
    wx.showModal({
      title: '关于我们',
      content: 'AI简历助手 v1.0.0\n\n智能简历创建与优化工具，帮助您打造专业简历，提升求职成功率。\n\n© 2023 AI简历助手团队',
      showCancel: false
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