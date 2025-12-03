// pages/interview/history.js
const app = getApp()
import { get, del } from '../../utils/request.js'

Page({
  data: {
    interviewList: [],
    loading: true,
    hasMore: true,
    startX: 0,
    currentIndex: -1,
    moveDistance: 0
  },

  onLoad: function() {
    // 获取屏幕宽度，用于像素到rpx的转换
    const screenWidth = wx.getSystemInfoSync().screenWidth;
    this.setData({
      screenWidth: screenWidth
    });
    this.loadInterviewHistory()
  },

  // 加载面试历史记录
  loadInterviewHistory() {
    // 从全局数据或本地缓存获取用户ID，适配不同的存储方式
    const userId = app.globalData.userInfo?.id || app.globalData.userId || wx.getStorageSync('userId') || '53';
    if (!userId) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      return;
    }

    this.setData({
      loading: true
    });

    get('/api/interview/history', {
      userId
    }).then(res => {
      if (res && (res.success === true || res.code === 0 || res.code === 200) && res.data) {
        const interviewData = res.data.histories || [];
        const formattedList = interviewData.map(item => ({
          id: item.uniqueSessionId,
          title: item.title || 'AI模拟面试',
          createTime: item.endTime ? this.formatDate(item.endTime) : '',
          score: item.finalScore || 0,
          status: item.status,
          sessionId: item.uniqueSessionId,
          showDelete: false
        }));

        this.setData({
          interviewList: formattedList,
          hasMore: false,
          loading: false
        });
      } else {
        this.setData({
          loading: false
        });
        wx.showToast({
          title: '获取面试历史失败',
          icon: 'none'
        });
      }
    }).catch(error => {
      this.setData({
        loading: false
      });
      console.error('获取面试历史失败', error);
      wx.showToast({
        title: '获取面试历史失败',
        icon: 'none'
      });
    });
  },

  // 格式化日期
  formatDate: function(timestamp) {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  },

  // 查看面试报告或继续面试
  viewReport: function(e) {
    const sessionId = e.currentTarget.dataset.id;
    const item = this.data.interviewList.find(item => item.id === sessionId);
    
    if (item && item.status !== 'completed') {
      // 未完成面试，直接跳转到面试页面继续面试
      wx.navigateTo({
        url: `/pages/interview/interview?sessionId=${sessionId}`
      });
    } else {
      // 已完成面试，直接跳转到报告页面
      wx.navigateTo({
        url: `/pages/report/report?sessionId=${sessionId}`
      });
    }
  },

  // 开始新面试
  startNewInterview: function() {
    const app = getApp();
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo');
    
    if (!userInfo) {
      wx.showModal({
        title: '提示',
        content: '请先登录，以使用完整功能',
        showCancel: false,
        confirmText: '去登录',
        success: () => {
          wx.navigateTo({ url: '/pages/login/login' })
        }
      })
      return;
    }
    
    // 检查用户是否有简历
    this.checkUserHasResume(userInfo.id);
  },
  
  // 检查用户简历状态并获取最新简历数据
  checkUserHasResume: function(userId) {
    wx.showLoading({ title: '检查简历中...' })
    
    const app = getApp()
    
    // 先调用/getLatest接口获取用户最新简历
    get('/resume/getLatest', { userId: userId })
      .then(res => {
        wx.hideLoading()
        if (res && res.success && res.data) {
          // 有简历，将最新简历数据存储到全局
          app.globalData.latestResumeData = res.data
          
          // 跳转到风格和简历选择页面，设置强制新建面试标志
          wx.navigateTo({ 
            url: `/pages/interview/interview_style_select?resumeId=${res.data.id || ''}&forceNewInterview=true` 
          })
        } else {
          // 无简历，提示用户创建或上传简历
          this.showNoResumePrompt()
        }
      })
      .catch(error => {
        wx.hideLoading()
        console.error('获取最新简历失败:', error)
        
        // 错误，提示用户创建简历
        this.showNoResumePrompt()
      })
  },
  
  // 显示无简历提示
  showNoResumePrompt: function() {
    wx.showModal({
      title: '提示',
      content: '您还没有创建简历，为了开始面试模拟，您需要先创建一份简历',
      showCancel: false,
      confirmText: '去创建简历',
      success: () => {
        wx.navigateTo({ url: '/pages/template/template' })
      }
    })
  },

  // 滑动开始
  onTouchStart: function(e) {
    const startX = e.touches[0].pageX;
    const index = e.currentTarget.dataset.index;
    // 滑动开始前，先收起其他所有项
    this.resetSwipe();
    this.setData({
      startX: startX,
      currentIndex: index,
      moveDistance: 0
    });
  },

  // 滑动中
  onTouchMove: function(e) {
    const currentX = e.touches[0].pageX;
    const startX = this.data.startX;
    const index = this.data.currentIndex;
    const interviewList = this.data.interviewList;
    
    if (index === -1 || !interviewList[index]) return;
    
    // 计算滑动距离（直接使用像素距离）
    let distance = currentX - startX;
    // 获取屏幕宽度，用于px转rpx的转换
    const screenWidth = wx.getSystemInfoSync().screenWidth;
    // 计算120rpx对应的px值
    const maxSlideDistancePx = (120 * screenWidth) / 750;
    // 只允许向左滑动，最大滑动距离120rpx
    if (distance < 0 && Math.abs(distance) <= maxSlideDistancePx) {
      this.setData({
        moveDistance: distance
      });
      // 实时更新当前项的位置
      this.setData({
        [`interviewList[${index}].translateX`]: distance
      });
    }
  },

  // 滑动结束
  onTouchEnd: function(e) {
    const distance = this.data.moveDistance;
    const index = this.data.currentIndex;
    const interviewList = this.data.interviewList;
    
    if (index === -1 || !interviewList[index]) return;
    
    // 只要是向左滑动，就显示删除按钮
    if (distance < 0) {
      // 更新列表，标记当前项为展开状态
      interviewList[index].showDelete = true;
      // 获取屏幕宽度，用于px转rpx的转换
      const screenWidth = wx.getSystemInfoSync().screenWidth;
      // 计算120rpx对应的px值
      const maxSlideDistancePx = (120 * screenWidth) / 750;
      interviewList[index].translateX = -maxSlideDistancePx; // 固定显示删除按钮，距离为120rpx
      this.setData({
        interviewList: interviewList
      });
    } else {
      // 向右滑动或没有滑动，收起删除按钮
      this.resetSwipe();
    }
  },

  // 重置滑动状态
  resetSwipe: function() {
    const interviewList = this.data.interviewList;
    // 重置所有项的展开状态和translateX值
    interviewList.forEach(item => {
      item.showDelete = false;
      delete item.translateX;
    });
    this.setData({
      interviewList: interviewList,
      moveDistance: 0,
      currentIndex: -1
    });
  },

  // 删除面试记录
  deleteInterview: function(e) {
    const sessionId = e.currentTarget.dataset.id;
    const index = this.data.interviewList.findIndex(item => item.id === sessionId);
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条面试记录吗？删除后将无法恢复。',
      success: res => {
        if (res.confirm) {
          // 调用后端接口删除记录
          del(`/api/interview/delete/${sessionId}`)
            .then(res => {
              if (res && (res.success === true || res.code === 0 || res.code === 200)) {
                // 删除成功，更新列表
                const interviewList = this.data.interviewList;
                interviewList.splice(index, 1);
                this.setData({
                  interviewList: interviewList
                });
                wx.showToast({
                  title: '删除成功',
                  icon: 'success'
                });
              } else {
                wx.showToast({
                  title: '删除失败',
                  icon: 'none'
                });
              }
            })
            .catch(error => {
              console.error('删除面试记录失败', error);
              wx.showToast({
                title: '删除失败',
                icon: 'none'
              });
            });
        }
      }
    });
  },

  // 下拉刷新
  onPullDownRefresh: function() {
    this.loadInterviewHistory()
    wx.stopPullDownRefresh()
  }
})