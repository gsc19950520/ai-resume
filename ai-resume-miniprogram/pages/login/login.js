//login.js
const app = getApp()

Page({
  data: {
    canIUse: wx.canIUse('button.open-type.getUserInfo'),
    isLoading: false
  },

  onLoad: function () {
    // 检查是否已经登录
    if (app.globalData.userInfo) {
      this.navigateToIndex()
    }
  },

  // 获取用户信息
  onGetUserInfo: function(e) {
    if (e.detail.userInfo) {
      // 用户同意授权
      this.handleLogin(e.detail.userInfo)
    } else {
      // 用户拒绝授权
      wx.showToast({
        title: '需要授权才能使用完整功能',
        icon: 'none'
      })
    }
  },

  // 处理登录逻辑
  handleLogin: function(userInfo) {
    const that = this
    that.setData({ isLoading: true })
    
    // 调用app.js中的login方法
    app.login(res => {
      that.setData({ isLoading: false })
      
      if (res.code === 0) {
        // 登录成功
        wx.showToast({
          title: '登录成功',
          icon: 'success',
          duration: 1500,
          success: function() {
            setTimeout(() => {
              that.navigateToIndex()
            }, 1500)
          }
        })
      } else {
        // 登录失败
        wx.showToast({
          title: res.message || '登录失败，请重试',
          icon: 'none'
        })
      }
    })
  },

  // 跳转到首页
  navigateToIndex: function() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      // 如果有上一页，返回上一页
      wx.navigateBack()
    } else {
      // 否则跳转到首页
      wx.switchTab({
        url: '/pages/index/index'
      })
    }
  },

  // 查看隐私政策
  viewPrivacy: function() {
    wx.navigateTo({
      url: '/pages/privacy/privacy'
    })
  },

  // 查看用户协议
  viewTerms: function() {
    wx.navigateTo({
      url: '/pages/terms/terms'
    })
  }
})