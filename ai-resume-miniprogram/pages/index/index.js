//index.js
const app = getApp()
import { get } from '../../utils/request.js'

Page({
  data: {
    userInfo: { nickName: '用户' }, // 默认用户信息，确保页面不会空白
    recommendList: [
      {
        id: 1,
        title: '如何打造优秀简历',
        image: '/images/recommend1.png'
      },
      {
        id: 2,
        title: '面试技巧指南',
        image: '/images/recommend2.png'
      },
      {
        id: 3,
        title: '热门行业分析',
        image: '/images/recommend3.png'
      }
    ],
    // 新增数据
    lastInterviewScore: 85, // 默认值，避免页面空白
    hasGrowthAdvice: true, // 默认显示推荐
    growthRecommendation: '建议提升项目管理能力，可通过参与更多团队项目来积累经验'
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

    // 加载数据
    this.loadData()
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
    
    // 使用统一的cloudCall方法调用接口获取用户信息
    app.cloudCall('/user/info', { openId: userInfo.openId }, 'GET')
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

  // 加载数据
  loadData: function() {
    wx.showLoading({ title: '加载中' })
    // 实际调用API获取推荐列表
    get('/api/recommend/list')
      .then(res => {
        console.log('推荐列表API返回:', res)
        // 适配BaseResponseVO格式：成功状态判断和数据提取
        if (res && (res.success === true || res.code === 200) && res.data) {
          // 检查数据结构，列表可能在data.list中
          if (res.data.list) {
            this.setData({
              recommendList: res.data.list
            })
          } else if (Array.isArray(res.data)) {
            this.setData({
              recommendList: res.data
            })
          }
        } else {
          console.log('获取推荐列表失败或返回格式异常，使用模拟数据')
          // 继续使用页面中定义的模拟数据，不进行修改
        }
      })
      .catch(error => {
        console.error('获取推荐列表失败:', error)
        // 使用模拟数据，不进行修改
      })
    
    // 获取用户上次面试数据和推荐
    this.getUserInterviewSummary()
  
    // 无论API调用是否成功，都在2秒后隐藏加载提示
    setTimeout(() => {
      wx.hideLoading()
    }, 2000)
  },
  
  // 获取用户面试摘要和推荐
  getUserInterviewSummary: function() {
    if (!this.data.userInfo) return;
    
    // 直接使用模拟数据，避免调用不存在的接口
    this.setMockInterviewSummary();
  },
  
  // 设置模拟面试摘要数据
  setMockInterviewSummary: function() {
    // 模拟数据
    this.setData({
      lastInterviewScore: 78,
      hasGrowthAdvice: true,
      growthRecommendation: '建议提升技术深度和表达清晰度，可通过多练习项目架构和表达技巧来改善'
    })
  },

  // 创建简历
  createResume: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/create/create' })
  },

  // 我的简历
  myResumes: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/resume/list/list' })
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
        
        // 如果获取失败，也提示用户创建简历
        this.showNoResumePrompt()
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
  },
  
  // 获取模拟简历列表
  getMockResumeList: function() {
    // 这里可以根据实际情况获取真实的简历列表
    // 返回模拟数据用于测试
    return [
      {
        id: '1',
        title: '前端开发工程师简历',
        occupation: '前端开发工程师',
        updateTime: '2025-11-05'
      },
      {
        id: '2',
        title: '全栈开发工程师简历',
        occupation: '全栈开发工程师',
        updateTime: '2025-10-30'
      }
    ];
  },

  // 城市薪资匹配功能已整合到面试报告中

  // AI职业成长报告
  aiCareerReport: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/growth/report' })
  },
  
  // 简历优化功能
  resumeOptimization: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/resume/optimize' })
  },

  // 简历模板
  resumeTemplates: function() {
    wx.navigateTo({ url: '/pages/template/list/list' })
  },

  // 跳转到详情页（临时提示）
  goToDetail: function(e) {
    wx.showToast({
      title: '详情页功能开发中',
      icon: 'none'
    })
  },

  // 下拉刷新
  onPullDownRefresh: function() {
    this.loadData()
    wx.stopPullDownRefresh()
  }
})