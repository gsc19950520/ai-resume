// pages/career/report.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    currentCareer: '',
    targetCareer: '',
    experienceYears: '',
    skills: '',
    reportResult: null,
    loading: false
  },

  onLoad: function() {
    this.setData({
      userInfo: app.globalData.userInfo
    })
  },

  // 输入当前职业
  inputCurrentCareer: function(e) {
    this.setData({
      currentCareer: e.detail.value
    })
  },

  // 输入目标职业
  inputTargetCareer: function(e) {
    this.setData({
      targetCareer: e.detail.value
    })
  },

  // 输入工作年限
  inputExperienceYears: function(e) {
    this.setData({
      experienceYears: e.detail.value
    })
  },

  // 输入技能
  inputSkills: function(e) {
    this.setData({
      skills: e.detail.value
    })
  },

  // 生成报告
  generateReport: function() {
    // 验证输入
    if (!this.data.currentCareer.trim()) {
      wx.showToast({
        title: '请输入当前职业',
        icon: 'none'
      })
      return
    }

    if (!this.data.targetCareer.trim()) {
      wx.showToast({
        title: '请输入目标职业',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    // 模拟API调用
    setTimeout(() => {
      // 模拟报告数据
      const mockReport = {
        title: 'AI职业成长规划报告',
        currentCareer: this.data.currentCareer,
        targetCareer: this.data.targetCareer,
        strengths: ['技术基础扎实', '学习能力强', '项目经验丰富'],
        improvementPoints: ['管理能力需要提升', '前沿技术需要更新'],
        developmentPath: [
          {
            stage: '第一阶段（1年内）',
            tasks: ['深化核心技能', '参与大型项目', '建立行业影响力']
          },
          {
            stage: '第二阶段（1-2年）',
            tasks: ['转型管理岗位', '拓展技术广度', '培养团队协作能力']
          },
          {
            stage: '第三阶段（2-3年）',
            tasks: ['成为技术专家', '构建个人品牌', '引领技术方向']
          }
        ],
        skillRecommendation: '建议重点提升：领导力、系统架构设计、项目管理能力',
        resources: ['行业会议', '在线学习平台', '技术社区']
      }

      this.setData({
        reportResult: mockReport,
        loading: false
      })
    }, 2000)
  },

  // 重新生成
  regenerate: function() {
    this.setData({
      reportResult: null
    })
  },

  // 分享报告
  shareReport: function() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    })
  },

  // 分享到好友
  onShareAppMessage: function() {
    return {
      title: '我的AI职业成长报告',
      path: '/pages/career/report',
      imageUrl: '/images/share-cover.png'
    }
  }
})