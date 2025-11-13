// list.js
const app = getApp()

Page({
  data: {
    templateList: []
  },

  onLoad: function() {
    // 直接设置固定的模板列表，只显示template-one和template-two
    const defaultTemplates = [
      {
        id: 'template-one', 
        name: '专业简约模板',
        preview: '/images/template-one.png',
        description: '清晰简洁的专业风格，适合各类职位申请',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-two',
        name: '创意设计模板',
        preview: '/images/template-two.png',
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-three',
        name: '创意设计模板',
        preview: '/images/template-three.png',
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-four',
        name: '创意设计模板',
        preview: '/images/template-four.png',
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-five',
        name: '创意设计模板',
        preview: '/images/template-five.png',
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-six',
        name: '创意设计模板',
        preview: '/images/template-six.png',
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      }
    ];
    
    this.setData({ templateList: defaultTemplates });
    console.log('模板列表已设置:', defaultTemplates);
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