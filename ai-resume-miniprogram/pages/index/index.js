//index.js
const app = getApp()
import { get } from '../../utils/request.js'

Page({
  data: {
    userInfo: { nickName: '用户' } // 默认用户信息，确保页面不会空白
  },

  onLoad: function () {
    // 获取用户信息，确保是对象类型
    let userInfo = app.globalData.userInfo;
    // 如果全局数据不存在，尝试从本地存储获取
    if (!userInfo || typeof userInfo !== 'object') {
      try {
        const userInfoStr = wx.getStorageSync('userInfo');
        if (userInfoStr) {
          userInfo = JSON.parse(userInfoStr);
          // 再次检查是否为有效对象
          if (typeof userInfo !== 'object' || userInfo === null) {
            userInfo = null;
          }
        }
      } catch (e) {
        console.error('解析用户信息失败:', e);
        userInfo = null;
      }
    }
    
    this.setData({
      userInfo: userInfo || this.data.userInfo // 如果获取失败，保持默认数据
    })

    // 如果未登录且没有默认昵称，保持默认值
    if (!this.data.userInfo || !this.data.userInfo.nickName) {
      console.log('未登录或用户昵称不存在，使用默认值')
      // 不立即提示登录，让用户可以看到默认的"用户"文本
    } else {
      // 如果已登录，调用接口获取最新个人信息
      this.fetchUserInfo()
    }
  },

  onShow: function() {
    // 页面显示时更新用户信息，添加错误处理
    try {
      const userInfoStr = wx.getStorageSync('userInfo');
      let userInfo = null;
      if (userInfoStr) {
        userInfo = JSON.parse(userInfoStr);
        // 确保userInfo是对象类型
        if (typeof userInfo !== 'object' || userInfo === null) {
          userInfo = null;
        }
      }
      this.setData({
        userInfo: userInfo || this.data.userInfo // 如果获取失败，保持原有数据
      })
    } catch (e) {
      console.error('解析用户信息失败:', e);
      // 解析失败时保持原有数据
    }
    
    // 如果已登录，刷新用户信息
    if (this.data.userInfo && this.data.userInfo.openId) {
      this.fetchUserInfo()
    }
  },

  // 获取最新用户信息
  fetchUserInfo: function() {
    // 获取用户信息（优先使用全局数据，其次使用本地存储）
    let userInfo = app.globalData.userInfo;
    
    // 如果全局数据不存在，尝试从本地存储获取
    if (!userInfo || typeof userInfo !== 'object') {
      const userInfoStr = wx.getStorageSync('userInfo');
      if (userInfoStr) {
        try {
          userInfo = JSON.parse(userInfoStr);
        } catch (e) {
          console.error('解析本地用户信息失败:', e);
          userInfo = null;
        }
      }
    }
    
    if (!userInfo || !userInfo.openId) {
      console.log('没有有效的用户信息或openId，无法获取用户信息')
      return
    }
    
    // 使用统一的get方法调用接口获取用户信息
    get('/api/user/info', { openId: userInfo.openId })
      .then(result => {
        // 处理响应数据
        let responseData;
        if (result && result.data) {
          responseData = result.data;
        } else {
          console.error('获取用户信息返回格式无效:', result)
          return;
        }
        
        // 检查响应是否成功
        if (responseData && result.success === true) {
          // 确保responseData是对象类型
          if (typeof responseData !== 'object' || responseData === null) {
            console.error('返回的用户信息不是有效对象:', responseData);
            return;
          }
          
          // 更新页面数据（直接使用对象，不转成字符串）
          this.setData({
            userInfo: responseData
          })
          
          // 更新全局用户信息
          app.globalData.userInfo = responseData
          
          // 更新本地存储（存储JSON字符串）
          const updatedUserInfoStr = JSON.stringify(responseData);
          wx.setStorageSync('userInfo', updatedUserInfoStr)
          if (responseData.id) {
            wx.setStorageSync('userId', responseData.id)
          }
          if (responseData.openId) {
            wx.setStorageSync('openId', responseData.openId)
          }
        } else {
          console.log('获取用户信息失败或返回格式异常:', responseData)
        }
      })
      .catch(error => {
        console.error('获取用户信息失败:', error)
      })
  },

  // 显示登录提示
  showLoginTip: function() {
    wx.showModal({
      title: '提示',
      content: '请先登录，以使用完整功能',
      confirmText: '去登录',
      success: res => {
        if (res.confirm) {
          wx.navigateTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },

  // AI模拟面试
  aiInterview: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    
    // 检查用户是否有简历并获取最新简历数据
    this.checkUserResumeAndGetLatest()
  },
  
  // 检查用户简历状态并获取最新简历数据
  checkUserResumeAndGetLatest: function() {
    wx.showLoading({ title: '检查简历中...' })
    
    const app = getApp()
    const userInfo = JSON.parse(wx.getStorageSync('userInfo'))
    const userId = userInfo.userId
    
    // 先调用/getLatest接口获取用户最新简历
    get('/resume/getLatest', { userId: userId })
      .then(res => {
        wx.hideLoading()
        if (res && res.success && res.data) {
          // 有简历，将最新简历数据存储到全局
          app.globalData.latestResumeData = res.data
          
          // 跳转到风格和简历选择页面
          wx.navigateTo({ 
            url: `/pages/interview/interview_style_select?resumeId=${res.data.id || ''}` 
          })
        } else {
          // 无简历，提示用户创建或上传简历
          this.showNoResumePrompt()
        }
      })
      .catch(error => {
        wx.hideLoading()
        console.error('获取最新简历失败:', error)
        
        // 如果是登录过期错误，不显示创建简历提示，因为request.js已经处理了登录过期逻辑
        if (error.message && error.message.includes('登录已过期')) {
          console.log('检测到登录过期错误，不显示创建简历提示')
        } else {
          // 其他错误，提示用户创建简历
          this.showNoResumePrompt()
        }
      })
  },
  
  // 显示无简历提示
  showNoResumePrompt: function() {
    console.log('showNoResumePrompt方法被调用');
    
    // 使用更简洁的配置，确保confirmText不超过4个中文字符
    wx.showModal({
      title: '提示',
      content: '请先创建简历后再进行AI模拟面试',
      showCancel: true,
      cancelText: '稍后',
      confirmText: '创建', // 修改为不超过4个中文字符
      success: function(res) {
        console.log('弹窗操作结果:', res);
        if (res.confirm) {
          // 跳转到模板选择页面（main页面）
          wx.navigateTo({ 
            url: '/pages/template/list/list',
            success: function() {
              console.log('成功跳转到创建简历页面');
            },
            fail: function(err) {
              console.error('跳转失败:', err)
            }
          })
        } else if (res.cancel) {
          console.log('用户点击了取消');
        }
      },
      fail: function(err) {
        console.error('显示弹窗失败:', err);
      }
    })
  }
})