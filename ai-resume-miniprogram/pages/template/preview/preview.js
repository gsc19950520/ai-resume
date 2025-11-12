// preview.js - 动态模板预览版本
Page({
  
  data: {
    templateId: 'template-one',  // 默认模板ID
    templateName: '技术人才模板',  // 默认模板名称
    imagePath: '/images/template-one.png',  // 默认图片路径
    templateUpdateTime: new Date().getTime()  // 用于触发视图更新的时间戳
  },

  /**
   * 生命周期函数--监听页面加载
   * @param {Object} options - 页面参数，包含templateId和templateName
   */
  onLoad: function(options) {
    console.log('预览页面接收到的参数:', options);
    
    // 有效的模板ID列表 - 确保与实际存在的图片文件匹配
    const validTemplateIds = ['template-one', 'template-two', 'template-three', 'template-four', 'template-five'];
    
    // 处理传入的参数
    if (options && options.templateId) {
      let templateId = options.templateId;
      
      // 防御性检查：如果传入的templateId无效，使用默认值
      if (!validTemplateIds.includes(templateId)) {
        console.warn('无效的模板ID:', templateId, '，使用默认模板');
        templateId = 'template-one'; // 使用默认模板ID
      }
      
      // 设置图片路径和模板ID
      const imagePath = `/images/${templateId}.png`;
      this.setData({
        templateId: templateId,
        imagePath: imagePath
      });
      
      // 动态加载对应的样式文件
      this.loadTemplateStyles(templateId);
      console.log('已设置模板ID:', templateId, '图片路径:', imagePath);
    }
    
    // 处理模板名称
    if (options && options.templateName) {
      try {
        this.setData({
          templateName: decodeURIComponent(options.templateName)
        });
        console.log('已设置模板名称:', this.data.templateName);
      } catch (e) {
        console.error('解码模板名称失败:', e);
      }
    }
  },

  /**
   * 使用当前模板
   * 将选择的模板信息保存到本地存储，并跳转到简历编辑页面
   */
  useTemplate: function() {
    const { templateId, templateName } = this.data;
    
    // 保存选择的模板信息
    wx.setStorageSync('tempResumeInfo', {
      templateId: templateId,
      templateName: templateName,
      title: '我的新简历',
      isAiGenerated: false
    });
    
    wx.showToast({
      title: '正在准备模板...',
      icon: 'success'
    });
    
    // 跳转到简历编辑页面
    setTimeout(() => {
      wx.navigateTo({
        url: `/pages/resume/edit/edit?templateId=${templateId}`
      });
    }, 1000);
  },
  
  /**
   * 动态加载模板样式文件
   * @param {string} templateId - 模板ID
   */
  /**
   * 加载指定模板的样式
   * @param {string} templateId - 模板ID
   */
  loadTemplateStyles: function(templateId) {
    try {
      console.log('正在为模板:', templateId, '加载对应样式');
      
      // 在小程序中，模板样式通过WXML中的模板导入和wxss的@import来控制
      // 由于我们已经在preview.wxss中导入了所有模板样式，这里只需确保模板切换时视图正确更新
      this.setData({
        templateUpdateTime: new Date().getTime() // 强制触发视图更新，确保样式正确应用
      });
      
      console.log('模板样式已准备就绪:', templateId);
    } catch (error) {
      console.error('加载模板样式失败:', error);
    }
  },

  /**
   * 返回模板列表
   */
  backToList: function() {
    wx.navigateBack();
  },
  
  /**
   * 跳转到测试页面
   */
  goToTest: function() {
    wx.navigateTo({
      url: '/pages/template/preview/simple-test'
    });
  },

  /**
   * 切换模板
   * @param {Object} e - 事件对象，包含模板ID
   */
  switchTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id;
    const templateNameMap = {
      'template-one': '技术人才模板',
      'template-two': '简约风格模板',
      'template-three': '设计师模板'
    };
    
    this.setData({
      templateId: templateId,
      templateName: templateNameMap[templateId] || templateId,
      imagePath: `/images/${templateId}.png`,
      templateUpdateTime: new Date().getTime()
    });
    
    console.log('已切换到模板:', templateId);
  }
})