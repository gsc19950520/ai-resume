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
      birthDate: '',
      city: '',
      address: '',
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
    
    // 检查登录状态
    if (!this.isLoggedIn()) {
      console.log('用户未登录，提示用户登录')
      this.showLoginTip()
    } else {
      console.log('用户已登录，加载用户信息')
      this.loadUserInfo()
    }
  },

  // 检查用户是否已登录
  isLoggedIn: function() {
    // 检查全局数据中是否有有效的用户信息
    if (app.globalData.userInfo && app.globalData.userInfo.id && app.globalData.userInfo.id !== 'guest') {
      return true
    }
    
    // 检查本地存储中是否有用户信息
    try {
      const storedUserInfo = wx.getStorageSync('userInfo')
      if (storedUserInfo) {
        const userInfo = typeof storedUserInfo === 'string' ? JSON.parse(storedUserInfo) : storedUserInfo
        if (userInfo && userInfo.id && userInfo.id !== 'guest') {
          return true
        }
      }
    } catch (e) {
      console.error('检查登录状态时解析本地存储失败:', e)
    }
    
    return false
  },

  // 显示登录提示并跳转到登录页
  showLoginTip: function() {
    wx.showModal({
      title: '请先登录',
      content: '您需要登录后才能编辑个人信息',
      showCancel: false,
      success: () => {
        // 保存返回页面信息
        wx.setStorageSync('returnToAfterLogin', '/pages/user/detail')
        // 跳转到登录页面
        wx.navigateTo({
          url: '/pages/login/login'
        })
      }
    })
  },

  // 加载用户信息（仅从本地数据获取）
  loadUserInfo: function() {
    console.log('loadUserInfo调用，从本地数据加载用户信息')
    let hasLoadedInfo = false
    
    // 首先尝试从全局数据获取用户信息
    if (app.globalData.userInfo && app.globalData.userInfo.id !== 'guest') {
      console.log('从全局数据获取用户信息')
      const userInfo = app.globalData.userInfo
      const genderIndex = userInfo.gender === 1 ? 0 : 1 // 1表示男，0表示女
      
      this.setData({
        userInfo,
        genderIndex
      })
      hasLoadedInfo = true
    }
    
    // 如果全局数据中没有，尝试从本地存储获取用户信息
    if (!hasLoadedInfo) {
      try {
        const storedUserInfo = wx.getStorageSync('userInfo')
        if (storedUserInfo) {
          const userInfo = typeof storedUserInfo === 'string' ? JSON.parse(storedUserInfo) : storedUserInfo
          if (userInfo && userInfo.id !== 'guest') {
            console.log('从本地存储获取用户信息')
            const genderIndex = userInfo.gender === 1 ? 0 : 1
            
            this.setData({
              userInfo,
              genderIndex
            })
            hasLoadedInfo = true
          }
        }
      } catch (e) {
        console.error('解析本地存储用户信息失败:', e)
      }
    }
    
    // 如果还是没有加载到信息，尝试从userInfoLocal获取
    if (!hasLoadedInfo) {
      this.loadUserInfoFromStorage()
    }
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
  onBirthDateChange: function(e) {
    const birthDate = e.detail.value
    this.setData({
      'userInfo.birthDate': birthDate
    })
  },

  // 城市输入
  onCityInput: function(e) {
    const city = e.detail.value
    this.setData({
      'userInfo.city': city
    })
  },

  // 选择头像
  chooseAvatar: function() {
    const that = this
    
    wx.showActionSheet({
      itemList: ['从相册选择', '拍照'],
      success: function(res) {
        const sourceType = res.tapIndex === 0 ? ['album'] : ['camera']
        
        wx.chooseMedia({
          count: 1,
          mediaType: ['image'],
          sourceType: sourceType,
          sizeType: ['compressed'], // 使用压缩图片
          success: function(res) {
            const tempFilePath = res.tempFiles[0].tempFilePath
            
            // 显示加载状态
            that.setData({
              isLoading: true,
              loadingMessage: '头像上传中...'
            })
            
            // 将图片转换为base64
            wx.getFileSystemManager().readFile({
              filePath: tempFilePath,
              encoding: 'base64',
              success: function(base64Res) {
                // 获取图片格式
                const fileExt = tempFilePath.match(/\.([^.]+)$/)?.[1] || 'jpeg'
                const base64Data = `data:image/${fileExt};base64,${base64Res.data}`
                
                // 先更新本地显示
                that.setData({
                  'userInfo.avatarUrl': base64Data
                })
                
                // 上传到后端
                that.uploadAvatarToServer(base64Data)
              },
              fail: function(error) {
                console.error('图片转base64失败:', error)
                that.setData({
                  isLoading: false
                })
                wx.showToast({
                  title: '图片处理失败',
                  icon: 'none'
                })
              }
            })
          },
          fail: function(error) {
            console.log('选择图片失败:', error)
          }
        })
      },
      fail: function(error) {
        console.log('取消选择')
      }
    })
  },

  // 上传头像到后端
  uploadAvatarToServer: function(base64Data) {
    const that = this
    
    // 获取用户信息
    let userInfo = app.globalData.userInfo
    if (!userInfo || !userInfo.openId) {
      try {
        const storedUserInfo = wx.getStorageSync('userInfo')
        if (storedUserInfo) {
          userInfo = typeof storedUserInfo === 'string' ? JSON.parse(storedUserInfo) : storedUserInfo
        }
      } catch (e) {
        console.error('解析本地存储用户信息失败:', e)
      }
    }
    
    if (!userInfo || !userInfo.openId) {
      that.setData({ isLoading: false })
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      return
    }
    
    // 导入UserService
    const UserService = require('../../services/user.js').default
    
    // 调用UserService上传头像
    UserService.uploadAvatar(userInfo.openId, base64Data)
      .then(res => {
        that.setData({ isLoading: false })
        
        wx.showToast({
          title: '头像上传成功',
          icon: 'success',
          duration: 2000
        })
        
        // 更新全局用户信息
        const updatedUserInfo = {
          ...userInfo,
          avatarUrl: base64Data
        }
        app.globalData.userInfo = updatedUserInfo
        wx.setStorageSync('userInfo', JSON.stringify(updatedUserInfo))
        wx.setStorageSync('userInfoLocal', JSON.stringify(updatedUserInfo))
        
      })
      .catch(error => {
        console.error('头像上传失败:', error)
        that.setData({ isLoading: false })
        
        // 上传失败，恢复原来的头像
        const originalAvatar = userInfo.avatarUrl || '/images/avatar.jpg'
        that.setData({
          'userInfo.avatarUrl': originalAvatar
        })
        
        wx.showToast({
          title: error.message || '头像上传失败',
          icon: 'none'
        })
      })
  },

  // 保存用户信息
  saveUserInfo: function() {
    console.log('点击保存按钮')
    const token = wx.getStorageSync('token')
    
    // 尝试从全局数据获取用户信息
    let userInfo = app.globalData.userInfo
    
    // 如果全局数据中没有，则尝试从本地存储获取
    if (!userInfo || !userInfo.id || userInfo.id === 'guest') {
      try {
        const storedUserInfo = wx.getStorageSync('userInfo')
        if (storedUserInfo) {
          userInfo = typeof storedUserInfo === 'string' ? JSON.parse(storedUserInfo) : storedUserInfo
        }
      } catch (e) {
        console.error('解析本地存储用户信息失败:', e)
        userInfo = null
      }
    }
    
    // 登录状态检查：需要token和有效的用户信息
    if (!token || !userInfo || !userInfo.id || userInfo.id === 'guest') {
      console.warn('登录状态检查失败:', { token: !!token, hasUserInfo: !!userInfo, userId: userInfo?.id })
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
    
    // 准备保存数据，实现address为空时使用city值的逻辑
    const saveData = {
      userId: userInfo.id,
      ...this.data.userInfo,
      // 当address为空或未定义时，使用city的值作为address
      address: this.data.userInfo.address?.trim() || this.data.userInfo.city
    }
    
    // 使用云托管请求方式保存用户信息
    request.post('/user/update', saveData).then(res => {
        console.log('保存用户信息返回结果:', res)
        
        if (res && res.success) {
          wx.showToast({
            title: '保存成功',
            icon: 'success'
          })
          
          // 更新全局用户信息，保留原始用户ID，并确保address字段使用city值（如果address为空）
          const updatedUserInfo = {
            ...this.data.userInfo,
            id: userInfo.id, // 保留原始的用户ID
            userId: userInfo.id, // 确保userId字段也保持一致
            // 确保address字段使用city值（如果address为空）
            address: this.data.userInfo.address?.trim() || this.data.userInfo.city
          }
          app.globalData.userInfo = updatedUserInfo
          wx.setStorageSync('userInfo', JSON.stringify(updatedUserInfo))
          // 同时保存到本地存储专用键，用于离线使用，同样保留原始用户ID
            wx.setStorageSync('userInfoLocal', JSON.stringify(updatedUserInfo))
          
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
    const { name, gender, phone, email, birthDate, city, avatarUrl } = this.data.userInfo
    
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
    if (!birthDate || birthDate.trim() === '') {
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