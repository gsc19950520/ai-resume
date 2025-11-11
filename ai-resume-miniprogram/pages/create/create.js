// create.js
const app = getApp()

Page({
  data: {
    // 基础数据
    resumeTitle: '',
    loading: false,
    
    // 创建模式：'template' 或 'ai'
    createMode: 'template',
    
    // 模板模式相关数据
    selectedTemplateId: '',
    
    // AI生成模式相关数据
    occupation: '',
    workExperience: ''
  },

  onLoad: function() {
    // 确保用户已登录
    if (!app.globalData.userInfo) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/login/login' })
      }, 1500)
      return
    }
  },

  // 标题输入处理
  onTitleInput: function(e) {
    this.setData({
      resumeTitle: e.detail.value
    });
  },

  // 职业输入处理
  onOccupationInput: function(e) {
    this.setData({
      occupation: e.detail.value
    });
  },

  // 工作经历输入处理
  onWorkExperienceInput: function(e) {
    this.setData({
      workExperience: e.detail.value
    });
  },

  // 模式切换
  onModeChange: function(e) {
    this.setData({
      createMode: e.detail.value
    });
  },

  // 选择模板模式
  selectTemplateMode: function() {
    this.setData({
      createMode: 'template'
    });
  },

  // 选择AI模式
  selectAiMode: function() {
    this.setData({
      createMode: 'ai'
    });
  },



  // 选择模板
  selectTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id;
    this.setData({
      selectedTemplateId: templateId
    });
  },

  // 跳转到模板编辑页面
  goToTemplateEdit: function() {
    const { resumeTitle, selectedTemplateId } = this.data;
    
    if (!resumeTitle.trim()) {
      wx.showToast({
        title: '请输入简历标题',
        icon: 'none'
      });
      return;
    }
    
    if (!selectedTemplateId) {
      wx.showToast({
        title: '请选择模板',
        icon: 'none'
      });
      return;
    }
    
    // 保存简历基本信息到本地存储，以便在编辑页面使用
    wx.setStorageSync('tempResumeInfo', {
      title: resumeTitle,
      templateId: selectedTemplateId,
      isAiGenerated: false
    });
    
    // 跳转到模板编辑页面
    wx.navigateTo({
      url: `/pages/resume/edit/edit?templateId=${selectedTemplateId}`
    });
  },

  // AI生成简历
  generateResumeWithAI: function() {
    const { resumeTitle, occupation, workExperience } = this.data;
    
    // 验证输入
    if (!resumeTitle.trim()) {
      wx.showToast({
        title: '请输入简历标题',
        icon: 'none'
      });
      return;
    }
    
    if (!occupation.trim()) {
      wx.showToast({
        title: '请输入职业',
        icon: 'none'
      });
      return;
    }
    
    if (!workExperience.trim()) {
      wx.showToast({
        title: '请输入工作经历',
        icon: 'none'
      });
      return;
    }
    
    this.setData({
      loading: true
    });
    
    // 模拟AI生成过程
    wx.showLoading({
      title: 'AI正在生成简历...',
    });
    
    // 在实际项目中，这里应该调用deepseek API来生成简历
    setTimeout(() => {
      wx.hideLoading();
      
      // 保存简历基本信息到本地存储
      wx.setStorageSync('tempResumeInfo', {
        title: resumeTitle,
        occupation: occupation,
        workExperience: workExperience,
        isAiGenerated: true
      });
      
      wx.showToast({
        title: '简历生成成功',
        icon: 'success'
      });
      
      // 跳转到简历预览或编辑页面
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/resume/view/view?source=ai'
        });
      }, 1500);
    }, 3000); // 模拟3秒生成时间
  }
})