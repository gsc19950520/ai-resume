//app.js
App({
  globalData: {
    userInfo: null,
    token: '',
    baseUrl: 'http://localhost:8080/api', // 本地开发环境
    cloudBaseUrl: '', // 云托管服务地址，使用callContainer时无需配置
    useCloud: true, // 使用云托管服务（通过callContainer调用）
    cloudEnvId: 'prod-1gwm267i6a10e7cb' // 微信云托管环境ID
  },
  
  // 初始化云开发环境
  initCloud: function() {
    if (this.globalData.useCloud) {
      wx.cloud.init({
        env: this.globalData.cloudEnvId,
        traceUser: true
      });
    }
  },

  onLaunch: function () {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 初始化云开发环境
    this.initCloud()
    
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
          // 确保云环境ID已配置
          if (!this.globalData.cloudEnvId || this.globalData.cloudEnvId === 'YOUR_ACTUAL_CLOUD_ENV_ID') {
            console.error('云托管环境ID未配置，请在app.js中设置正确的cloudEnvId')
            wx.showToast({
              title: '云环境配置错误',
              icon: 'none'
            })
            if (callback) callback(new Error('云环境配置错误'))
            return
          }
          
          // 使用云托管callContainer调用登录接口
          wx.cloud.callContainer({
            config: {
              env: this.globalData.cloudEnvId
            },
            path: '/api/user/login',
            method: 'POST',
            header: {
              'X-WX-SERVICE': 'ai-resume-service',
              'content-type': 'application/json'
            },
            data: {
              code: res.code
            },
            success: result => {
              this.handleLoginResult(result, callback)
            },
            fail: error => {
              console.error('云托管登录请求失败', error)
              wx.showToast({
                title: '登录失败，请重试',
                icon: 'none'
              })
              if (callback) callback(error)
            }
          })
        } else {
          // 登录失败情况
          console.error('登录失败，无法获取code', res)
          wx.showToast({
            title: '登录失败，请重试',
            icon: 'none'
          })
          if (callback) callback(new Error('无法获取登录code'))
        }
      },
      fail: error => {
        console.error('wx.login调用失败', error)
        wx.showToast({
          title: '登录接口调用失败',
          icon: 'none'
        })
        if (callback) callback(error)
      }
    })
  },
  
  // 处理登录结果
  handleLoginResult: function(result, callback) {
    // 检查响应结构是否正确
    if (!result || !result.data) {
      console.error('登录结果格式错误', result)
      wx.showToast({
        title: '登录信息解析失败',
        icon: 'none'
      })
      if (callback) callback(new Error('登录结果格式错误'))
      return
    }
    // 注意：云托管调用和普通请求的数据结构可能略有不同
    let responseData = result.data || result;
    
    if (responseData.code === 0) {
      this.globalData.token = responseData.data.token
      this.globalData.userInfo = responseData.data.userInfo
      wx.setStorageSync('token', responseData.data.token)
      wx.setStorageSync('userInfo', JSON.stringify(responseData.data.userInfo))
      
      // 获取用户信息
      this.getUserInfo(() => {
        if (callback) callback(null)
      })
    } else {
      wx.showToast({
        title: responseData.message || '登录失败',
        icon: 'none'
      })
      if (callback) callback(new Error(responseData.message || '登录失败'))
    }
  },

  // 退出登录
  logout: function() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    this.globalData.token = ''
    this.globalData.userInfo = null
  },
  // 统一请求方法（已迁移到utils/request.js中使用callContainer）
  request: function(url, method, data, callback) {
    // 注意：此方法已保留以兼容旧代码，新代码应使用utils/request.js中的方法
    if (this.globalData.useCloud) {
      wx.cloud.callContainer({
        config: {
          env: this.globalData.cloudEnvId
        },
        path: `/api${url}`,
        method: method || 'GET',
        header: {
          'content-type': 'application/json',
          'token': this.globalData.token || ''
        },
        data: data || {},
        success: res => {
          callback && callback(res.data)
        },
        fail: err => {
          console.error('云托管调用失败', err)
          callback && callback({code: -1, message: '云托管调用失败'})
        }
      })
    } else {
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
  }
})