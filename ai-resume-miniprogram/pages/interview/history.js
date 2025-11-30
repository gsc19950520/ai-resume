// pages/interview/history.js
const app = getApp()
import { get } from '../../utils/request.js'

Page({
  data: {
    interviewList: [],
    loading: true,
    hasMore: true
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

  // 查看面试报告
  viewReport: function(e) {
    const sessionId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/report/report?sessionId=${sessionId}`
    })
  },

  // 开始新面试
  startNewInterview: function() {
    wx.navigateTo({
      url: '/pages/interview/interview'
    })
  },

  // 下拉刷新
  onPullDownRefresh: function() {
    this.loadInterviewHistory()
    wx.stopPullDownRefresh()
  }
})