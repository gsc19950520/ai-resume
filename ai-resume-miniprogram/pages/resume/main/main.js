// pages/resume/main/main.js
import UserService from '../../../services/user';

Page({
  /**
   * 页面的初始数据
   */
  data: {
    hasResume: false,
    resumeData: null,
    loading: true,
    templateId: 'template-one', // 默认使用template-one模板
    templateClass: '',
    // 模板列表相关数据
    templateList: [],
    pageReady: false // 模板列表页面就绪状态标记
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function() {
    console.log('简历页面加载');
    this.checkUserResumeStatus();
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function() {
    // 每次页面显示时检查用户简历状态
    this.checkUserResumeStatus();
  },

  /**
   * 加载模板列表数据
   */
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
      
    }, 500); // 更大的延迟确保页面完全切换后再加载图片
  },

  /**
   * 选择模板
   */
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

  /**
   * 预览模板
   */
  previewTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id
    const templateName = e.currentTarget.dataset.name
    
    // 跳转到模板预览页面 - 使用模板字符串便于代码依赖分析
    wx.navigateTo({
      url: `/pages/template/preview/imageView/preview-image?templateId=${templateId}&templateName=${encodeURIComponent(templateName)}`
    })
  },

  /**
   * 检查用户是否有简历
   */
  async checkUserResumeStatus() {
    try {
      this.setData({ loading: true });
      
      // 从storage中获取userinfo
      const userInfo = wx.getStorageSync('userInfo');
      if (!userInfo) {
        console.error('未找到用户信息');
        this.setData({ 
          hasResume: false,
          loading: false 
        });
        return;
      }

      console.log('用户信息:', userInfo);

      // 检查用户是否有简历数据
      // 根据简历数量来判断是否存在简历
      if (userInfo && userInfo.resumeCount && userInfo.resumeCount > 0) {
        this.setData({
          hasResume: true,
          resumeData: userInfo.resumeData || null,
          loading: false
        });
        // 应用模板样式
        this.applyTemplateStyle();
      } else {
        this.setData({
          hasResume: false,
          loading: false
        });
        // 加载模板列表
        this.loadTemplateList();
      }
    } catch (error) {
      console.error('检查用户简历状态失败:', error);
      this.setData({ 
        hasResume: false,
        loading: false 
      });
      wx.showToast({
        title: '获取简历信息失败',
        icon: 'none'
      });
    }
  },

  /**
   * 应用模板样式
   */
  applyTemplateStyle() {
    const templateId = this.data.templateId;
    let templateClass = '';

    // 根据模板ID应用不同的样式
    switch (templateId) {
      case 'template-one':
        templateClass = 'template-one';
        break;
      case 'template-two':
        templateClass = 'template-two';
        break;
      case 'template-three':
        templateClass = 'template-three';
        break;
      case 'template-four':
        templateClass = 'template-four';
        break;
      case 'template-five':
        templateClass = 'template-five';
        break;
      default:
        templateClass = 'template-one';
    }

    this.setData({
      templateClass: templateClass
    });
  },

  /**
   * 跳转到模板列表页面
   */
  goToTemplateList() {
    // 不再跳转到单独的模板列表页面，因为已经内嵌在当前页面
    // 可以移除这个方法，或者保留作为兼容性处理
    console.log('模板列表已内嵌在当前页面');
  },

  /**
   * 跳转到简历编辑页面
   */
  editResume() {
    wx.navigateTo({
      url: '/pages/resume/edit/edit'
    });
  },

  /**
   * 创建新简历
   */
  createResume() {
    wx.navigateTo({
      url: '/pages/create/create'
    });
  },

  /**
   * 分享简历
   */
  shareResume() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  /**
   * 导出为PDF
   */
  exportToPdf() {
    // 这里可以复用现有的导出PDF功能
    wx.navigateTo({
      url: `/pages/resume/view/view?templateId=${this.data.templateId}&source=template`
    });
  }
});
