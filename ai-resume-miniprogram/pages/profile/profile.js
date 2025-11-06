//profile.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    resumeCount: 0,
    interviewCount: 0
  },

  onShow: function() {
    // 每次显示页面时更新用户信息
    this.setData({
      userInfo: app.globalData.userInfo
    })
    
    // 如果已登录，加载用户数据
    if (this.data.userInfo) {
      this.loadUserData()
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
    // 检查用户是否已登录
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo) {
      this.setData({ userInfo: null });
      return;
    }

    this.setData({ userInfo });
    
    // 加载用户统计数据
    wx.request({
      url: '/api/user/stats',
      method: 'GET',
      header: {
        'content-type': 'application/json',
        'Authorization': 'Bearer ' + userInfo.token
      },
      success: (res) => {
        if (res.data.code === 0) {
          const { resumeCount, interviewCount } = res.data.data;
          this.setData({ resumeCount, interviewCount });
        }
      },
      fail: () => {
        wx.showToast({
          title: '获取用户数据失败',
          icon: 'none'
        });
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