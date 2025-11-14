// 图片预览页面逻辑
Page({
  data: {
    loading: false,
    showMode: 'image',
    imagePath: '',
    templateId: ''
  },

  onLoad: function(options) {
    // 设置为图片预览模式
    this.setData({
      loading: true,
      showMode: 'image'
    });

    // 从参数中获取templateId
    const templateId = options.templateId || '';
    this.setData({ templateId });

    // 构建图片路径，根据templateId加载对应的模板图片
    // 图片存放在images目录下，命名为template-one.jpg, template-two.jpg等
    const imagePath = `/images/${templateId}.jpg`;
    
    this.setData({
      imagePath: imagePath,
      loading: false
    });
  },

  // 使用模板按钮点击事件
  useTemplate: function() {
    const { templateId } = this.data;
    
    // 将选择的模板ID保存到本地存储
    wx.setStorageSync('selectedTemplateId', templateId);
    
    // 跳转到简历编辑页面
    wx.navigateTo({
      url: `/pages/resume/edit/edit?templateId=${templateId}`
    });
  },

  // 下载PDF按钮点击事件
  downloadPdf: function() {
    wx.showToast({
      title: '下载功能开发中',
      icon: 'none'
    });
  },

  // 加载测试数据按钮点击事件
  testTemplateRendering: function() {
    // 切换到常规预览页面进行测试
    wx.redirectTo({
      url: `/pages/template/preview/preview?templateId=${this.data.templateId}&test=1`
    });
  }
});