// list.js
const app = getApp()

Page({
  data: {
    templateList: [],
    pageReady: false // 添加页面就绪状态标记
  },

  onLoad: function() {
    // 页面加载时不立即设置数据，避免提前渲染
    console.log('简历模板页面加载中...');
  },

  onReady: function() {
    // 页面渲染完成后再设置数据，确保页面切换时不会提前渲染不相关内容
    this.loadTemplateList();
  },

  // 加载模板列表数据
  loadTemplateList: function() {
    // 1. 先设置基础数据但不含图片地址，避免提前加载图片
    const defaultTemplates = [
      {
        id: 'template-one', 
        name: '专业简约模板',
        // 初始不设置preview，避免提前加载图片
        description: '清晰简洁的专业风格，适合各类职位申请',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-two',
        name: '创意设计模板',
        // 初始不设置preview
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-three',
        name: '创意设计模板',
        // 初始不设置preview
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-four',
        name: '创意设计模板',
        // 初始不设置preview
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-five',
        name: '创意设计模板',
        // 初始不设置preview
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      },
      {
        id: 'template-six',
        name: '创意设计模板',
        // 初始不设置preview
        description: '富有创意的设计风格模板',
        vipOnly: false,
        isFree: true
      }
    ];
    
    // 第一步：先设置基本数据和pageReady状态
    this.setData({
      templateList: defaultTemplates,
      pageReady: true // 标记页面已就绪
    });
    
    console.log('模板列表基础数据已设置，等待页面完全渲染后加载图片');
    
    // 2. 更大延迟后再设置图片URL，确保页面完全切换完成后才开始加载图片
    setTimeout(() => {
      // 创建英文数字映射数组
      const numberToEnglish = ['one', 'two', 'three', 'four', 'five', 'six'];
      
      const templatesWithImages = [...defaultTemplates].map((template, index) => {
        // 只在页面完全就绪后才设置preview图片地址，使用英文命名而非数字
        return {
          ...template,
          preview: `/images/template-${numberToEnglish[index]}.jpg`
        };
      });
      
      this.setData({
        templateList: templatesWithImages
      });
      
      console.log('模板列表图片URL已设置，开始加载图片');
    }, 500); // 更大的延迟确保页面完全切换后再加载图片
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
      url: `/pages/template/preview/imageView/preview-image?templateId=${templateId}&templateName=${encodeURIComponent(templateName)}`
    })
  }
})