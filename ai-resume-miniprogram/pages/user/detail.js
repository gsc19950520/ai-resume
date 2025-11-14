// detail.js
const app = getApp()
// 使用正确的方式导入request模块
const request = require('../../utils/request').default

Page({
  data: {
    userInfo: {
      name: '',
      gender: 0,
      phone: '',
      email: '',
      birthday: '',
      city: '',
      avatarUrl: ''
    },
    genderIndex: 0,
    isLoading: false,
    loadingMessage: ''
  },

  onLoad: function (options) {
    console.log('个人信息编辑页面加载', options)
    // 优先从options获取returnTo参数
    let returnToPage = options.returnTo || ''
    
    // 如果options中没有returnTo参数，尝试从本地存储获取
    if (!returnToPage) {
      returnToPage = wx.getStorageSync('returnToAfterCompleteProfile') || ''
      // 获取后清除本地存储的参数，避免影响后续操作
      if (returnToPage) {
        wx.removeStorageSync('returnToAfterCompleteProfile')
      }
    }
    
    this.returnToPage = returnToPage
    this.loadUserInfo()
  },

  // 加载用户信息
  loadUserInfo: function() {
    // 优先从服务器获取最新数据
    this.fetchUserInfoFromServer()
  },

  // 从服务器获取用户信息
  fetchUserInfoFromServer: function() {
    const token = wx.getStorageSync('token')
    console.log('获取服务器用户信息：' + token)
    if (!token) {
      console.warn('未登录，无法获取用户信息')
      // 尝试从storage获取之前保存的数据
      this.loadUserInfoFromStorage()
      return
    }
    
    // 实际调用后端接口获取用户信息
    request.get('/user/info', {})
      .then(res => {
        console.log('从服务器获取用户信息成功:', res)
        if (res && res.success && res.data) {
          // 设置性别索引
          const genderIndex = res.data.gender === 1 ? 0 : 1 // 1表示男，0表示女
          
          // 更新数据
          this.setData({
            userInfo: res.data,
            genderIndex
          })
          
          // 保存到storage，以便后续使用
          wx.setStorageSync('userInfoLocal', JSON.stringify(res.data))
        } else {
          console.warn('服务器返回数据异常，尝试从storage获取')
          this.loadUserInfoFromStorage()
        }
      })
      .catch(error => {
        console.error('从服务器获取用户信息失败:', error)
        // 后端获取失败，从storage获取
        this.loadUserInfoFromStorage()
      })
  },
  
  // 从本地存储加载用户信息
  loadUserInfoFromStorage: function() {
    try {
      const storedUserInfo = wx.getStorageSync('userInfoLocal')
      if (storedUserInfo) {
        const parsedUserInfo = typeof storedUserInfo === 'string' ? JSON.parse(storedUserInfo) : storedUserInfo
        // 设置性别索引
        const genderIndex = parsedUserInfo.gender === 1 ? 0 : 1
        
        this.setData({
          userInfo: parsedUserInfo,
          genderIndex
        })
        console.log('从本地存储加载用户信息成功')
      }
    } catch (e) {
      console.error('从本地存储加载用户信息失败:', e)
    }
  },

  // 姓名输入
  onNameInput: function(e) {
    const name = e.detail.value
    this.setData({
      'userInfo.name': name
    })
  },

  // 性别选择
  onGenderChange: function(e) {
    const genderIndex = e.detail.value
    this.setData({
      genderIndex,
      'userInfo.gender': genderIndex === '0' ? 1 : 0 // 0表示男，1表示女
    })
  },

  // 手机号输入
  onPhoneInput: function(e) {
    const phone = e.detail.value
    this.setData({
      'userInfo.phone': phone
    })
  },

  // 邮箱输入
  onEmailInput: function(e) {
    const email = e.detail.value
    this.setData({
      'userInfo.email': email
    })
  },

  // 出生日期选择
  onBirthdayChange: function(e) {
    const birthday = e.detail.value
    this.setData({
      'userInfo.birthday': birthday
    })
  },

  // 城市输入
  onCityInput: function(e) {
    const city = e.detail.value
    this.setData({
      'userInfo.city': city
    })
  },

  // 保存用户信息
  saveUserInfo: function() {
    console.log('点击保存按钮')
    const token = wx.getStorageSync('token')
    let userInfo = wx.getStorageSync('userInfo')
    
    // 解析用户信息
    if (userInfo && typeof userInfo === 'string') {
      try {
        userInfo = JSON.parse(userInfo)
      } catch (e) {
        userInfo = null
      }
    }
    
    // 登录状态检查：只需要token和有效的用户信息
    if (!token || !userInfo || !userInfo.id) {
      wx.showModal({
        title: '提示',
        content: '请先登录',
        success: res => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/login/login'
            })
          }
        }
      })
      return
    }
    
    // 表单验证
    if (!this.validateForm()) {
      return
    }
    
    this.setData({ isLoading: true, loadingMessage: '保存中...' })
    
    // 使用云托管请求方式保存用户信息
    request.post('/user/update', {
      userId: userInfo.id,
      ...this.data.userInfo
    }).then(res => {
        console.log('保存用户信息返回结果:', res)
        
        if (res && res.success) {
          wx.showToast({
            title: '保存成功',
            icon: 'success'
          })
          
          // 更新全局用户信息
          app.globalData.userInfo = this.data.userInfo
          wx.setStorageSync('userInfo', JSON.stringify(this.data.userInfo))
          // 同时保存到本地存储专用键，用于离线使用
          wx.setStorageSync('userInfoLocal', JSON.stringify(this.data.userInfo))
          
          // 根据是否有返回路径参数决定跳转方式
          setTimeout(() => {
            if (this.returnToPage) {
              console.log('跳转到指定返回页面:', this.returnToPage)
              wx.navigateTo({
                url: this.returnToPage
              })
            } else {
              // 没有指定返回页面时，返回上一页
              wx.navigateBack()
            }
          }, 1500)
        } else {
          wx.showToast({
            title: res && res.message || '保存失败，请重试',
            icon: 'none'
          })
          console.error('保存用户信息失败:', res)
        }
        this.setData({ isLoading: false })
      }).catch(error => {
        console.error('保存用户信息异常:', error)
        wx.setStorageSync('userInfoLocal', JSON.stringify(this.data.userInfo))
        this.setData({ isLoading: false })
      })
  },

  // 表单验证
  validateForm: function() {
    const { name, gender, phone, email, birthday, city, avatarUrl } = this.data.userInfo
    
    // 姓名验证（必填）
    if (!name || name.trim() === '') {
      wx.showToast({
        title: '请输入姓名',
        icon: 'none'
      })
      return false
    }
    
    // 性别验证（必填）
    if (gender === undefined || gender === null) {
      wx.showToast({
        title: '请选择性别',
        icon: 'none'
      })
      return false
    }
    
    // 手机号验证（必填且格式正确）
    if (!phone || phone.trim() === '') {
      wx.showToast({
        title: '请输入手机号',
        icon: 'none'
      })
      return false
    } else if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return false
    }
    
    // 邮箱验证（必填且格式正确）
    if (!email || email.trim() === '') {
      wx.showToast({
        title: '请输入邮箱',
        icon: 'none'
      })
      return false
    } else if (!/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(email)) {
      wx.showToast({
        title: '请输入正确的邮箱',
        icon: 'none'
      })
      return false
    }
    
    // 出生日期验证（必填）
    if (!birthday || birthday.trim() === '') {
      wx.showToast({
        title: '请选择出生日期',
        icon: 'none'
      })
      return false
    }
    
    // 城市验证（必填）
    if (!city || city.trim() === '') {
      wx.showToast({
        title: '请输入城市',
        icon: 'none'
      })
      return false
    }
    
    return true
  }
})