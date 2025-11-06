// pages/salary/matcher.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    cities: ['北京', '上海', '广州', '深圳', '杭州', '成都'],
    selectedCity: '北京',
    experienceYears: ['应届', '1-3年', '3-5年', '5-10年', '10年以上'],
    selectedExperience: '1-3年',
    jobType: 'Java',
    salaryResult: null,
    loading: false
  },

  onLoad: function() {
    this.setData({
      userInfo: app.globalData.userInfo
    })
  },

  // 选择城市
  selectCity: function(e) {
    this.setData({
      selectedCity: e.currentTarget.dataset.city
    })
  },

  // 选择经验年限
  selectExperience: function(e) {
    this.setData({
      selectedExperience: e.currentTarget.dataset.experience
    })
  },

  // 输入职位类型
  inputJobType: function(e) {
    this.setData({
      jobType: e.detail.value
    })
  },

  // 匹配薪资
  matchSalary: function() {
    if (!this.data.jobType.trim()) {
      wx.showToast({
        title: '请输入职位类型',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    // 模拟API调用
    setTimeout(() => {
      // 模拟薪资数据
      const mockResult = {
        averageSalary: '15-25K',
        minSalary: '12K',
        maxSalary: '30K',
        positionCount: 258,
        trend: '上升',
        analysis: '根据当前市场数据分析，' + this.data.selectedCity + '地区' + this.data.jobType + '岗位在' + this.data.selectedExperience + '经验区间的薪资水平处于行业中上水平。'
      }

      this.setData({
        salaryResult: mockResult,
        loading: false
      })
    }, 1500)
  },

  // 重新匹配
  rematch: function() {
    this.setData({
      salaryResult: null
    })
  }
})