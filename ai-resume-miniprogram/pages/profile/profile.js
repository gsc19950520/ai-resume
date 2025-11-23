//profile.js
const app = getApp()
import UserService from '../../services/user.js'

Page({
  data: {
    userInfo: { id: 'guest', nickName: '游客', avatarUrl: '/images/avatar.jpg' }, // 默认游客信息，确保页面不会空白
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

  // 监听用户信息更新事件
  onLoad: function() {
    // 监听用户信息更新事件
    const app = getApp();
    const that = this;
    
    // 监听蓝牙特征值变化事件（这里用于监听用户信息更新）
    wx.onBLECharacteristicValueChange(function(res) {
      if (res.deviceId === 'userInfoUpdated' && res.characteristicId === 'userInfoCharacteristic') {
        console.info('收到用户信息更新事件');
        // 重新加载用户信息
        that.reloadUserInfoFromStorage();
      }
    });
  },

  // 从本地存储重新加载用户信息
  reloadUserInfoFromStorage: function() {
    const app = getApp();
    const userInfoStr = wx.getStorageSync('userInfo');
    
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr);
        this.setData({ userInfo });
        app.globalData.userInfo = userInfo;
        console.info('用户信息已从本地存储重新加载');
      } catch (e) {
        console.error('重新加载用户信息失败:', e);
      }
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

  // 加载用户信息和统计数据
  loadUserData: function() {
    const app = getApp();
    let userInfo = this.data.userInfo || app.globalData.userInfo || {};
    
    // 首先尝试从本地存储获取用户信息
    try {
      const storedUserInfo = wx.getStorageSync('userInfo');
      if (storedUserInfo) {
        const parsedUserInfo = JSON.parse(storedUserInfo);
        // 合并本地存储的用户信息
        userInfo = { ...userInfo, ...parsedUserInfo };
      }
      
      // 尝试从userInfoLocal获取openId
      const storedUserInfoLocal = wx.getStorageSync('userInfoLocal');
      if (storedUserInfoLocal) {
        const parsedUserInfoLocal = JSON.parse(storedUserInfoLocal);
        if (parsedUserInfoLocal.openId && !userInfo.openId) {
          userInfo.openId = parsedUserInfoLocal.openId;
          console.info('从userInfoLocal获取到openId');
        }
      }
    } catch (e) {
      console.error('读取本地用户信息失败:', e);
    }
    
    // 从全局数据获取openId（优先级：已有openId > 全局数据）
    if (!userInfo.openId && app.globalData.openId) {
      userInfo.openId = app.globalData.openId;
      console.info('从全局数据获取openId');
    }
    
    // 确保用户ID存在
    if (!userInfo.userId) {
      userInfo.userId = userInfo.id || 'temp_user_' + new Date().getTime();
      userInfo.id = userInfo.userId;
    }
    
    this.setData({ userInfo });
    
    // 调用用户服务获取详细信息和统计数据
    if (userInfo.openId) {
      // 使用app.cloudCall方法调用后端API，确保路径格式正确
      app.cloudCall('/user/info', { openId: userInfo.openId }, 'GET')
        .then(res => {
          try {
            const response = res;
            // 处理用户信息数据
            if (response.success && response.data) {
              const userData = response.data;
              
              // 更新用户信息
              const updatedUserInfo = { ...userInfo, ...userData };
              this.setData({ userInfo: updatedUserInfo });
              app.globalData.userInfo = updatedUserInfo;
              
              // 更新本地存储
              try {
                wx.setStorageSync('userInfo', JSON.stringify(updatedUserInfo));
                wx.setStorageSync('userInfoLocal', JSON.stringify(updatedUserInfo));
              } catch (e) {
                console.error('保存用户信息到本地存储失败:', e);
              }
              
              // 更新统计数据
              const resumeCount = userData.resumeCount || 0;
              const interviewCount = userData.interviewCount || 0;
              const optimizedCount = userData.optimizedCount || 0;
              const remainingOptimizeCount = userData.remainingOptimizeCount || 0;
              const vip = userData.vip || false;
              
              this.setData({ 
                resumeCount, 
                interviewCount,
                optimizedCount,
                remainingOptimizeCount,
                isVip: vip
              });
              
              // 记录用户VIP状态到全局数据
              app.globalData.isVip = vip;
            } else {
              console.error('获取用户信息失败:', response);
            }
          } catch (e) {
            console.error('解析响应数据失败:', e);
          }
        })
        .catch(err => {
          console.error('调用用户信息接口失败:', err);
        });
    } else {
      console.error('无法获取用户openId，无法调用用户信息接口');
    }
  },
  // 页面相关事件处理函数 - 监听用户下拉刷新
  onPullDownRefresh: function () {
    // 刷新时重新加载用户数据
    this.loadUserData();
    wx.stopPullDownRefresh();
  },
  
  

  // 我的简历
  myResumes: function() {
    if (!this.data.userInfo) {
      this.showLoginTip()
      return
    }
    wx.navigateTo({ url: '/pages/resume/list/list' })
  },
  
  // 个人信息编辑
  editProfile: function() {
    console.log('editProfile函数被调用，当前userInfo:', this.data.userInfo)
    wx.removeStorageSync('returnToAfterCompleteProfile')
    // 检查是否为游客状态
    if (!this.data.userInfo || this.data.userInfo.id === 'guest') {
      console.log('未登录或游客状态，显示登录提示')
      this.showLoginTip()
      return
    }
    
    console.log('已登录，尝试跳转到个人信息编辑页面')
    wx.navigateTo({
      url: '/pages/user/detail',
      success: function(res) {
        console.log('跳转到个人信息编辑页面成功', res)
      },
      fail: function(err) {
        console.error('跳转到个人信息编辑页面失败:', err)
      }
    })
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
    console.log('showLoginTip函数被调用')
    // 检查是否为游客状态或未登录
    if (!this.data.userInfo || this.data.userInfo.id === 'guest') {
      wx.showToast({
        title: '请先登录',
        icon: 'none',
        complete: () => {
          setTimeout(() => {
            wx.navigateTo({
              url: '/pages/login/login',
              success: function(res) {
                console.log('跳转到登录页面成功', res)
              },
              fail: function(err) {
                console.error('跳转到登录页面失败:', err)
              }
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