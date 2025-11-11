// preview.js - 动态模板预览版本
Page({
  
  data: {
    templateId: 'template-one',  // 默认模板ID
    templateName: '技术人才模板',  // 默认模板名称
    imagePath: '/images/template-one.png'  // 默认图片路径
  },

  /**
   * 生命周期函数--监听页面加载
   * @param {Object} options - 页面参数，包含templateId和templateName
   */
  onLoad: function(options) {
    console.log('预览页面接收到的参数:', options);
    
    // 处理传入的参数
    if (options && options.templateId) {
      const templateId = options.templateId;
      // 设置图片路径和模板ID
      this.setData({
        templateId: templateId,
        imagePath: `/images/${templateId}.png`
      });
      console.log('已设置模板ID:', templateId, '图片路径:', this.data.imagePath);
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
  }
})