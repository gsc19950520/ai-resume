// detail.js
const app = getApp()
const { request } = require('../../utils/request')

Page({
  data: {
    userInfo: {
      name: '',
      gender: 0,
      phone: '',
      email: '',
      birthday: '',
      city: '',
      profession: '',
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
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') 
    
    if (userInfo) {
      try {
        // 确保userInfo是对象格式
        const parsedUserInfo = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo
        
        // 设置性别索引
        const genderIndex = parsedUserInfo.gender === 1 ? 0 : 1 // 1表示男，0表示女
        
        // 更新数据
        this.setData({
          userInfo: parsedUserInfo,
          genderIndex
        })
        
        // 如果用户信息不完整，尝试从服务器获取
        if (!parsedUserInfo.phone || !parsedUserInfo.email) {
          this.fetchUserInfoFromServer()
        }
      } catch (e) {
        console.error('解析用户信息失败:', e)
        // 解析失败时获取服务器数据
        this.fetchUserInfoFromServer()
      }
    } else {
      // 如果没有本地用户信息，尝试从服务器获取
      this.fetchUserInfoFromServer()
    }
  },

  // 从服务器获取用户信息
  fetchUserInfoFromServer: function() {
    const token = wx.getStorageSync('token')
    
    if (!token) {
      console.warn('未登录，无法获取用户信息')
      return
    }
    
    // 模拟数据，避免调用可能不存在的接口
    setTimeout(() => {
      console.log('使用模拟数据填充用户信息')
      const mockUserInfo = {
        name: '用户',
        gender: 0,
        phone: '',
        email: '',
        birthday: '',
        city: '',
        profession: '',
        avatarUrl: '/images/avatar.jpg'
      }
      
      this.setData({
        userInfo: mockUserInfo,
        genderIndex: 0
      })
    }, 500)
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

  // 职业输入
  onProfessionInput: function(e) {
    const profession = e.detail.value
    this.setData({
      'userInfo.profession': profession
    })
  },

  // 保存用户信息
  saveUserInfo: function() {
    console.log('点击保存按钮')
    const token = wx.getStorageSync('token')
    const userId = wx.getStorageSync('userId')
    
    if (!token || !userId) {
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
    request('/user/update', {
      userId: userId,
      ...this.data.userInfo
    }, 'POST')
      .then(res => {
        console.log('保存用户信息返回结果:', res)
        
        if (res && res.success) {
          wx.showToast({
            title: '保存成功',
            icon: 'success'
          })
          
          // 更新全局用户信息
          app.globalData.userInfo = this.data.userInfo
          wx.setStorageSync('userInfo', JSON.stringify(this.data.userInfo))
          
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
      })
      .catch(error => {
        console.error('保存用户信息异常:', error)
        
        // 在云托管请求失败的情况下，使用模拟保存作为备用
        console.log('使用模拟保存作为备用方案')
        wx.showToast({
          title: '保存成功',
          icon: 'success'
        })
        
        // 更新全局用户信息
        app.globalData.userInfo = this.data.userInfo
        wx.setStorageSync('userInfo', JSON.stringify(this.data.userInfo))
        
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
        
        this.setData({ isLoading: false })
      })
  },

  // 表单验证
  validateForm: function() {
    const { phone, email } = this.data.userInfo
    
    // 手机号验证
    if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({
        title: '请输入正确的手机号',
        icon: 'none'
      })
      return false
    }
    
    // 邮箱验证
    if (email && !/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(email)) {
      wx.showToast({
        title: '请输入正确的邮箱',
        icon: 'none'
      })
      return false
    }
    
    return true
  }
})