//login.js
const app = getApp()

Page({
  data: {
    isLoading: false
  },

  onLoad: function () {
    // 检查是否已经登录
    if (app.globalData.isLogin) {
      this.navigateToIndex()
    }
  },

  // 微信登录按钮点击事件 - 简化登录流程，不获取用户信息
  onGetUserInfo: function() {
    const that = this
    
    // 调用微信登录接口获取code
    wx.login({
      success: (loginRes) => {
        that.setData({ isLoading: true })
        
        // 直接调用wechatLogin接口进行登录（不获取用户信息）
        app.wechatLogin(loginRes.code, res => {
          that.setData({ isLoading: false })
          
          if (res && res.code === 0) {
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
              title: res?.message || '登录失败，请重试',
              icon: 'none',
              duration: 2000
            })
          }
        })
      },
      fail: (error) => {
        console.error('微信登录失败', error)
        wx.showToast({
          title: '微信登录失败',
          icon: 'none',
          duration: 2000
        })
      }
    })
  },

  // 跳转到首页
  navigateToIndex: function() {
    wx.switchTab({
      url: '/pages/index/index'
    })
  }
})