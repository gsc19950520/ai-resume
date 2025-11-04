//app.js
App({
  globalData: {
    userInfo: null,
    token: '',
    baseUrl: 'http://localhost:8080/api'
  },

  onLaunch: function () {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 检查登录状态
    this.checkLoginStatus()

    // 获取系统信息
    this.getSystemInfo()
  },

  checkLoginStatus: function() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = JSON.parse(userInfo)
    }
  },

  getSystemInfo: function() {
    const systemInfo = wx.getSystemInfoSync()
    this.globalData.systemInfo = systemInfo
  },

  // 登录方法
  login: function(callback) {
    wx.login({
      success: res => {
        if (res.code) {
          // 发送 res.code 到后端换取 openId, sessionKey, unionId
          wx.request({
            url: `${this.globalData.baseUrl}/user/login`,
            method: 'POST',
            data: {
              code: res.code
            },
            success: result => {
              if (result.data.code === 0) {
                this.globalData.token = result.data.data.token
                this.globalData.userInfo = result.data.data.userInfo
                wx.setStorageSync('token', result.data.data.token)
                wx.setStorageSync('userInfo', JSON.stringify(result.data.data.userInfo))
              }
              callback && callback(result.data)
            },
            fail: error => {
              console.error('登录失败', error)
              callback && callback({code: -1, message: '登录失败'})
            }
          })
        }
      }
    })
  },

  // 退出登录
  logout: function() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    this.globalData.token = ''
    this.globalData.userInfo = null
  },

  // 统一请求方法
  request: function(url, method, data, callback) {
    wx.request({
      url: `${this.globalData.baseUrl}${url}`,
      method: method || 'GET',
      data: data || {},
      header: {
        'content-type': 'application/json',
        'token': this.globalData.token || ''
      },
      success: res => {
        // 处理登录过期
        if (res.data.code === 401) {
          this.logout()
          wx.navigateTo({ url: '/pages/login/login' })
          return
        }
        callback && callback(res.data)
      },
      fail: error => {
        console.error('请求失败', error)
        wx.showToast({ title: '网络错误', icon: 'none' })
        callback && callback({code: -1, message: '网络错误'})
      }
    })
  }
})