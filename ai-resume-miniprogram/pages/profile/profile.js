//profile.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    resumeCount: 0,
    interviewCount: 0
  },

  onShow: function() {
    const app = getApp();
    // 每次显示页面时，从storage重新加载用户信息，确保数据最新
    const token = wx.getStorageSync('token');
    const userInfoStr = wx.getStorageSync('userInfo');
    
    if (token && userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        
        // 确保用户对象包含id字段，如果没有则生成临时ID
        if (!userInfo.id) {
          console.warn('用户信息缺少ID，尝试从token生成');
          // 使用token生成临时ID
          userInfo.id = token.substring(0, 10);
          // 保存更新后的用户信息
          wx.setStorageSync('userInfo', JSON.stringify(userInfo));
          app.globalData.userInfo = userInfo;
        }
        
        this.setData({ userInfo });
        // 更新全局用户信息
        app.globalData.userInfo = userInfo;
        app.globalData.token = token;
        // 加载用户数据
        this.loadUserData();
      } catch (e) {
        console.error('解析用户信息失败:', e);
        // 出错时，尝试从token创建一个临时用户对象
        if (token) {
          const tempUserInfo = { id: token.substring(0, 10) };
          this.setData({ userInfo: tempUserInfo });
          app.globalData.userInfo = tempUserInfo;
          console.info('创建临时用户对象以确保页面正常显示');
        } else {
          this.setData({ userInfo: null });
        }
      }
    } else {
      this.setData({ userInfo: null });
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
    const app = getApp();
    // 检查用户是否已登录
    const token = wx.getStorageSync('token');
    let userInfo = this.data.userInfo || app.globalData.userInfo;
    
    // 检查token有效性
    if (!token || token === 'mock_token') {
      console.warn('无效的token，不加载用户数据:', token);
      this.setData({ userInfo: null });
      return;
    }
    
    // 如果没有用户信息但有token，尝试从token创建一个临时用户对象
    if (!userInfo && token) {
      // 避免使用无效的token作为ID
      let tempId = token.substring(0, 10);
      if (tempId === 'mock_token') {
        tempId = 'temp_' + Date.now();
        console.warn('使用时间戳作为临时用户ID，避免使用无效token');
      }
      userInfo = { id: tempId };
      this.setData({ userInfo });
      app.globalData.userInfo = userInfo;
      console.info('从token创建临时用户对象，ID:', tempId);
    }
    
    if (!token || !userInfo) {
      this.setData({ userInfo: null });
      return;
    }
    
    // 优先使用系统内部userId，其次使用id，确保不使用mock_token
    let userId = userInfo.userId || userInfo.id;
    
    // 验证userId是否有效，避免使用mock_token或无效ID
    if (!userId || userId === 'mock_token' || userId.toString().includes('mock_token')) {
      console.warn('发现无效的用户ID，生成临时ID用于统计');
      userId = 'temp_' + new Date().getTime();
    }
    
    console.info('使用用户ID加载统计数据:', userId);
    
    // 更新用户对象中的ID
    if (userInfo.userId !== userId && userInfo.id !== userId) {
      userInfo.id = userId;
      this.setData({ userInfo });
      app.globalData.userInfo = userInfo;
    }
    
    // 加载用户统计数据，使用云托管调用方式
    wx.cloud.callContainer({
      config: {
        env: app.globalData.cloudEnvId
      },
      path: `/api/statistics/user/${userId}`,
      method: 'GET',
      header: {
        'X-WX-SERVICE': 'springboot-bq0e',
        'content-type': 'application/json',
        'token': token
      },
      success: (res) => {
        const responseData = res.data || {};
        if (responseData.code === 0 || responseData.resumeCount !== undefined) {
          // 兼容不同的响应格式
          const statsData = responseData.data || responseData;
          const { resumeCount, interviewCount } = statsData || {};
          this.setData({ resumeCount, interviewCount });
        } else {
          console.error('获取用户统计数据失败:', responseData);
          // 不显示错误提示，避免影响用户体验
        }
      },
      fail: (error) => {
        console.error('云托管调用失败:', error);
        // 不显示错误提示，允许页面继续显示
      }
    });
    
    // 无需加载详细数据，简化为只显示统计信息
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
  showLoginTip: function(callback) {
    if (!this.data.userInfo) {
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
      return false;
    }
    if (callback && typeof callback === 'function') {
      callback();
    }
    return true;
  }
})