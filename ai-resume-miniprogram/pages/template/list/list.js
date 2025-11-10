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
        id: 'template-one', 
        name: '技术人才模板',
        preview: '/images/template-technical.png',
        type: 'technical',
        description: '突出技能和项目经验的技术导向模板'
      },
      {
        id: 'template-two',
        name: '简约专业模板',
        preview: '/images/template-basic.png',
        type: 'basic',
        description: '清晰简洁的专业风格，适合各类职位申请'
      },
      {
        id: 'template-three',
        name: '左侧导航型模板',
        preview: '/images/template-three.png',
        type: 'modern',
        description: '左侧导航设计，内容展示清晰有条理'
      },
      {
        id: 'template-four',
        name: '现代卡片式模板',
        preview: '/images/template-four.png',
        type: 'card',
        description: '时尚的卡片式布局，内容分区清晰'
      },
      {
        id: 'template-five',
        name: '深色主题模板',
        preview: '/images/template-five.png',
        type: 'dark',
        description: '专业的深色主题设计，现代感强'
      },
      {
        id: 'template-six',
        name: '时尚简约模板',
        preview: '/images/template-six.png',
        type: 'fashion',
        description: '时尚简约的设计风格，视觉效果出众'
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