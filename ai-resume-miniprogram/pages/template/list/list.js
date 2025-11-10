// list.js
const app = getApp()

Page({
  data: {
    templateList: [],
    loading: true
  },

  onLoad: function() {
    this.loadTemplateList()
  },

  // 加载模板列表
  loadTemplateList: function() {
    this.setData({ loading: true })
    
    // 模拟API调用，直接使用模拟数据
    setTimeout(() => {
      this.setData({ 
        loading: false,
        templateList: this.getMockData()
      })
      console.log('使用模拟数据显示简历模板')
    }, 1000)
    
    /* 保留原始API调用，但优先使用模拟数据
    // 调用获取模板列表API
    app.request('/api/template/list', 'GET', {}, res => {
      console.log('模板列表API返回:', res)
      // API调用结果不会覆盖已设置的模拟数据
    })
    */
  },

  // 获取模拟数据
  getMockData: function() {
    return [
      {
        id: '1',
        name: '简约专业模板',
        preview: '/images/template-basic.png',
        type: 'basic',
        description: '清晰简洁的专业风格，适合各类职位申请'
      },
      {
        id: '2', 
        name: '技术人才模板',
        preview: '/images/template-technical.png',
        type: 'technical',
        description: '突出技能和项目经验的技术导向模板'
      }
    ]
  },

  // 选择模板
  selectTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id
    const templateName = e.currentTarget.dataset.name
    
    // 保存选择的模板信息
    wx.setStorageSync('tempResumeInfo', {
      title: templateName + '简历',
      templateId: templateId,
      isAiGenerated: false
    })
    
    wx.showToast({
      title: '正在准备模板...',
      icon: 'success'
    })
    
    // 直接跳转到简历编辑页面，减少用户操作步骤
    setTimeout(() => {
      wx.navigateTo({
        url: `/pages/resume/edit/edit?templateId=${templateId}`
      })
    }, 1000)
  },

  // 预览模板
  previewTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id
    const templateName = e.currentTarget.dataset.name
    
    // 跳转到模板预览页面 - 使用模板字符串便于代码依赖分析
    wx.navigateTo({
      url: `/pages/template/preview/preview?templateId=${templateId}&templateName=${encodeURIComponent(templateName)}`
    })
  }
})