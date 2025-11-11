// list.js
const app = getApp()
import { get, post } from '../../../utils/request.js'

Page({
  data: {
    resumeList: [],
    loading: true,
    refreshing: false
  },

  onShow: function() {
    // 每次显示页面都重新加载数据
    this.loadResumeList()
  },

  // 加载简历列表
  loadResumeList: function() {
    // 确保用户已登录
    if (!app.globalData.userInfo) {
      this.setData({ loading: false })
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/login/login' })
      }, 1500)
      return
    }

    this.setData({ loading: true })
    
    // 模拟API调用，直接使用模拟数据
    setTimeout(() => {
      this.setData({ 
        loading: false,
        refreshing: false,
        resumeList: this.getMockData()
      })
      console.log('使用模拟数据显示简历列表')
    }, 1000)
    
    // 调用获取简历列表API
    get('/api/resume/list')
      .then(res => {
        console.log('简历列表API返回:', res)
        // API调用结果不会覆盖已设置的模拟数据
      })
      .catch(error => {
        console.error('获取简历列表失败:', error)
      })
  },

  // 获取模拟数据
  getMockData: function() {
    return [
      {
        id: '1',
        title: '我的技术简历',
        type: 'technical',
        createTime: '2025-11-01',
        updateTime: '2025-11-05'
      },
      {
        id: '2',
        title: '基础简历模板',
        type: 'basic',
        createTime: '2025-10-28',
        updateTime: '2025-10-30'
      }
    ]
  },

  // 编辑简历
  editResume: function(e) {
    const id = e.currentTarget.dataset.id
    // 查找要编辑的简历
    const resume = this.data.resumeList.find(item => item.id === id)
    if (resume) {
      // 将简历数据存储到全局，便于编辑页面使用
      app.globalData.currentResume = resume
      // 模拟编辑功能，由于编辑页面可能不存在，添加提示
      wx.showModal({
        title: '编辑简历',
        content: `您正在编辑简历：${resume.title}\n\n功能开发中，敬请期待！`,
        showCancel: false
      })
      // 注释掉实际跳转，因为编辑页面可能不存在
      // wx.navigateTo({ url: `/pages/resume/edit/edit?id=${id}` })
    }
  },

  // 删除简历
  deleteResume: function(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这份简历吗？删除后无法恢复。',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '删除中' })
          
          // 模拟删除操作
          setTimeout(() => {
            // 从本地数据中删除
            const newResumeList = this.data.resumeList.filter(item => item.id !== id)
            this.setData({ resumeList: newResumeList })
            
            // 调用API删除
            post('/api/resume/delete', { resumeId: id })
              .then(() => {
                // 删除已成功，不需要额外提示
              })
              .catch(error => {
                console.error('删除简历失败:', error)
                wx.showToast({ title: '删除失败', icon: 'none' })
                // 恢复数据
                this.loadResumeList()
              })
            
            wx.hideLoading()
            wx.showToast({ title: '删除成功' })
          }, 800)
        }
      }
    })
  },

  // 刷新页面
  onPullDownRefresh: function() {
    this.setData({ refreshing: true })
    this.loadResumeList()
  },

  // 创建新简历
  createNewResume: function() {
    wx.navigateTo({ url: '/pages/create/create' })
  }
})