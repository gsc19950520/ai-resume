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
          sessionId: item.uniqueSessionId
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
    wx.navigateTo({
      url: '/pages/interview/interview'
    })
  },

  // 滑动开始
  onTouchStart: function(e) {
    const startX = e.touches[0].clientX;
    const index = e.currentTarget.dataset.index;
    this.setData({
      startX: startX,
      currentIndex: index,
      moveDistance: 0
    });
  },

  // 滑动中
  onTouchMove: function(e) {
    const currentX = e.touches[0].clientX;
    const distance = currentX - this.data.startX;
    // 只允许向左滑动，最大滑动距离120rpx
    if (distance < 0 && Math.abs(distance) <= 120) {
      this.setData({
        moveDistance: distance
      });
      // 更新当前项的位置
      const interviewList = this.data.interviewList;
      const animation = wx.createAnimation({
        duration: 0,
        timingFunction: 'ease'
      });
      // 这里我们不直接使用动画对象，而是直接设置transform
      // 因为列表项可能有多个，我们需要为每个项单独设置
    }
  },

  // 滑动结束
  onTouchEnd: function(e) {
    const distance = this.data.moveDistance;
    // 如果滑动距离大于60rpx，则显示删除按钮
    if (Math.abs(distance) > 60) {
      // 更新列表，标记当前项为展开状态
      const interviewList = this.data.interviewList;
      const index = this.data.currentIndex;
      if (interviewList[index]) {
        interviewList[index].showDelete = true;
        this.setData({
          interviewList: interviewList
        });
      }
    } else {
      // 滑动距离不足，收起删除按钮
      this.resetSwipe();
    }
  },

  // 重置滑动状态
  resetSwipe: function() {
    const interviewList = this.data.interviewList;
    // 重置所有项的展开状态
    interviewList.forEach(item => {
      item.showDelete = false;
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