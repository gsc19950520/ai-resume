Page({
  data: {
    resumeInfo: null,
    source: 'template', // 'ai' 或 'template'
    resumeData: null,
    templateId: null,
    renderedHtml: null,
    templateClass: ''
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function(options) {
    console.log('onLoad view page with options:', options);
    
    // 有效的模板ID列表 - 确保与实际存在的图片文件匹配
    const validTemplateIds = ['template-one', 'template-two', 'template-three', 'template-four', 'template-five'];
    
    // 处理模板ID优先级：1. 从options获取 2. 从本地存储获取
    let templateId = options.templateId || wx.getStorageSync('tempResumeInfo')?.templateId || '';
    
    // 防御性检查：如果传入的templateId无效，使用默认值
    if (templateId && !validTemplateIds.some(validId => String(templateId).toLowerCase().includes(validId))) {
      console.warn('无效的模板ID:', templateId, '，使用默认模板');
      templateId = 'template-one'; // 使用默认模板ID
    }
    
    this.templateId = templateId;
    console.log('最终使用的templateId:', this.templateId);
    
    // 保存templateId到页面数据
    this.setData({
      templateId: this.templateId,
      source: options.source || 'template'
    });
    
    // 根据模板ID应用样式
    this.applyTemplateStyle();
    
    // 根据来源加载不同的简历数据
    if (options.source === 'ai') {
      this.loadAiGeneratedResume();
    } else {
      this.loadTemplateResume();
    }
  },

  // 加载AI生成的简历
  loadAiGeneratedResume: function() {
    this.setData({ loading: true });
    wx.showLoading({
      title: '正在生成简历...',
    });
    
    try {
      // 从本地存储获取AI生成的简历数据
      const resumeInfo = wx.getStorageSync('resumeInfo') || {};
      
      // 模拟AI生成的简历数据
      // 在实际项目中，这里应该使用deepseek API生成的真实数据
      const aiResumeData = {
        title: resumeInfo.title,
        isAiGenerated: true,
        personalInfo: {
          name: '张先生',
          jobTitle: resumeInfo.occupation,
          phone: '138****1234',
          email: 'example@email.com',
          address: '北京市朝阳区',
          birthDate: ''
        },
        summary: '拥有丰富的' + resumeInfo.occupation + '经验，专注于' + this.extractSkillsFromExperience(resumeInfo.workExperience) + '。' + resumeInfo.workExperience.substring(0, 100) + '...',
        workExperience: this.parseWorkExperience(resumeInfo.workExperience),
        education: [
          {
            school: '北京大学',
            degree: '本科',
            major: '计算机科学与技术',
            period: '2015.09 - 2019.06'
          }
        ],
        skills: this.extractSkillsFromExperience(resumeInfo.workExperience).split('、'),
        selfEvaluation: ''
      };

      this.setData({
        resumeData: aiResumeData
      });
    } catch (error) {
      console.error('加载AI生成简历时发生异常:', error);
      wx.showToast({
        title: '加载失败，请重试',
        icon: 'none'
      });
    } finally {
      wx.hideLoading();
      this.setData({ loading: false });
    }
  },

  /**
   * 加载模板简历数据
   */
  loadTemplateResume: function() {
    console.log('开始加载模板简历数据');
    
    // 优先从本地存储获取简历数据
    const resumeInfo = wx.getStorageSync('resumeInfo') || {};
    const savedRenderedHtml = wx.getStorageSync('renderedResumeHtml') || '';
    
    // 确保templateId的优先级
    const templateId = this.templateId || resumeInfo.templateId || '';
    
    console.log('使用templateId:', templateId, 'resumeInfo:', JSON.stringify(resumeInfo).substring(0, 100) + '...');
    
    // 构建传递给后端的简历数据
    const templateResumeData = {
      id: resumeInfo.id || '',
      name: resumeInfo.personalInfo?.name || '',
      email: resumeInfo.personalInfo?.email || '',
      phone: resumeInfo.personalInfo?.phone || '',
      address: resumeInfo.personalInfo?.address || '',
      birthDate: resumeInfo.personalInfo?.birthDate || '',
      jobType: resumeInfo.jobTitle || '',
      education: JSON.stringify(resumeInfo.education || []),
      workExperience: JSON.stringify(resumeInfo.workExperience || []),
      skills: JSON.stringify(resumeInfo.skills || []),
      projects: JSON.stringify(resumeInfo.projects || []),
      objective: resumeInfo.selfEvaluation || '',
      profile: resumeInfo.summary || '',
      // 新增字段
      expectedSalary: resumeInfo.personalInfo?.expectedSalary || '',
      startTime: resumeInfo.personalInfo?.startTime || '',
      hobbies: JSON.stringify(resumeInfo.hobbies || []),
      skillsWithLevel: JSON.stringify(resumeInfo.skillsWithLevel || [])
    };
    
    // 先使用本地保存的HTML（如果有）
    if (savedRenderedHtml) {
      console.log('使用本地保存的渲染结果');
      this.setData({
        renderedHtml: savedRenderedHtml
      });
    }
    
    // 只有在有templateId时才调用后端API
    if (templateId) {
      this.setData({ loading: true });
      wx.showLoading({
        title: '正在应用模板样式...',
      });
      
      try {
        console.log('调用后端渲染API，templateId:', templateId);
        
        // 不再调用后端渲染接口，直接使用本地数据
        console.log('使用本地模板数据，不调用渲染接口');
        
        wx.hideLoading();
        this.setData({
          loading: false,
          hasResumeData: true
        });
        
        // 如果没有本地保存的HTML，设置一个状态以显示提示
        if (!savedRenderedHtml) {
          wx.showToast({
            title: '使用本地模板数据',
            icon: 'none'
          });
        }
      } catch (error) {
        console.error('加载模板简历时发生异常:', error);
        wx.hideLoading();
        this.setData({ loading: false });
        wx.showToast({
          title: '加载异常，请重试',
          icon: 'none'
        });
      }
    } else {
      console.warn('未指定模板ID，无法加载特定模板');
      wx.showToast({
        title: '请先选择模板',
        icon: 'none'
      });
      this.setData({ loading: false });
    }
  },
  
  // 根据模板ID应用相应的样式
  applyTemplateStyle: function() {
    const templateId = this.data.templateId;
    if (!templateId) {
      console.log('未指定模板ID，使用默认样式');
      this.setData({
        templateClass: 'default-template-style'
      });
      return;
    }
    
    console.log('应用模板样式，模板ID:', templateId);
    
    // 根据模板ID设置不同的样式类，支持多种格式的模板ID
    let templateClass = 'default-template-style';
    
    // 处理不同的模板ID格式，包括文件名、路径和简单ID
    const templateIdStr = String(templateId).toLowerCase();
    
    if (templateIdStr.includes('template_one') || 
        templateIdStr.includes('template-one') || 
        templateIdStr === '1' || 
        templateIdStr.includes('one')) {
      templateClass = 'template-one-style';
    } else if (templateIdStr.includes('template_two') || 
               templateIdStr.includes('template-two') || 
               templateIdStr === '2' || 
               templateIdStr.includes('two')) {
      templateClass = 'template-two-style';
    }
    
    this.setData({
      templateClass: templateClass
    });
    
    console.log('应用样式类:', templateClass);
  },

  // 从工作经历中提取技能关键词
  extractSkillsFromExperience: function(experience) {
    // 这里只是简单的模拟，实际项目中应该使用更复杂的NLP技术
    const skillsMap = {
      '前端': 'JavaScript、HTML、CSS、React、Vue',
      '后端': 'Java、Python、Spring Boot、MySQL',
      '设计': 'UI设计、Figma、Photoshop、Sketch',
      '销售': '客户沟通、谈判技巧、市场分析',
      '管理': '团队管理、项目协调、资源分配'
    };

    for (const key in skillsMap) {
      if (experience.includes(key)) {
        return skillsMap[key];
      }
    }
    return '专业技能';
  },

  // 解析工作经历
  parseWorkExperience: function(experience) {
    // 简单模拟解析
    return [
      {
        company: '知名科技公司',
        position: this.data.resumeInfo.occupation,
        period: '2021.01 - 至今',
        description: experience.substring(0, 150) + '...'
      }
    ];
  },

  /**
   * 跳转到编辑页面
   */
  editResume: function() {
    const source = this.data.source;
    const templateId = this.data.templateId;
    
    // 构建跳转参数
    const queryParams = {
      source: source,
      templateId: templateId
    };
    
    // 跳转到编辑页面
    wx.navigateTo({
      url: '/pages/resume/edit/edit?' + Object.keys(queryParams)
        .map(key => `${key}=${encodeURIComponent(queryParams[key])}`)
        .join('&')
    });
  },
  
  /**
   * 跳转到模板选择页面
   */
  selectTemplate: function() {
    wx.navigateTo({
      url: '/pages/template/list/list'
    });
  },
  
  // 分享简历
  onShareAppMessage: function() {
    return {
      title: '我的专业简历',
      path: '/pages/resume/view/view?templateId=' + this.data.templateId
    };
  },

  // 保存到我的简历
  saveToMyResumes: function() {
    wx.showLoading({
      title: '保存中...',
    });

    // 模拟保存过程
    setTimeout(() => {
      wx.hideLoading();
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      });

      // 跳转到简历列表页面
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/resume/list/list'
        });
      }, 1500);
    }, 1000);
  },

  // 分享简历
  shareResume: function() {
    // 调用小程序原生分享功能
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  // 导出PDF
  exportToPdf: function() {
    this.setData({ loading: true });
    wx.showLoading({
      title: '生成PDF中...',
    });

    try {
      // 导入request工具
      const app = getApp();
      
      // 使用app.js中定义的cloudCall方法
      if (!app.cloudCall) {
        // 如果app.js中没有定义cloudCall，使用默认实现
        app.cloudCall = function(path, data = {}, method = 'GET', header = {}) {
          return new Promise((resolve, reject) => {
            wx.cloud.callContainer({
              config: {
                env: app.globalData.cloudEnvId
              },
              path: path.startsWith('/api') ? path : `/api${path}`,
              method: method,
              header: {
                'content-type': 'application/json',
                'token': app.globalData.token || '',
                'X-WX-SERVICE': 'springboot-bq0e',
                ...header
              },
              data,
              success: res => resolve(res.data),
              fail: err => reject(err)
            });
          });
        };
      }

      // 从本地存储获取简历ID或使用模板ID
      const resumeInfo = wx.getStorageSync('resumeInfo') || {};
      const resumeId = resumeInfo.id || this.data.templateId || 1;
      
      console.log('导出PDF，使用resumeId:', resumeId);
      
      app.cloudCall(`/api/resume/export/pdf?resumeId=${resumeId}`, {}, 'GET')
        .then(res => {
          console.log('PDF导出成功，响应:', res);
          
          // 如果返回的是文件流，使用wx.downloadFile下载
          // 这里简化处理，实际项目中需要根据后端返回的数据格式进行调整
          wx.showToast({
            title: 'PDF生成成功',
            icon: 'success'
          });
        })
        .catch(err => {
          console.error('PDF导出失败:', err);
          wx.showToast({
            title: 'PDF生成失败，请重试',
            icon: 'none'
          });
        })
        .finally(() => {
          wx.hideLoading();
          this.setData({ loading: false });
        });
    } catch (error) {
      console.error('导出PDF时发生异常:', error);
      wx.hideLoading();
      this.setData({ loading: false });
      wx.showToast({
        title: '操作异常，请重试',
        icon: 'none'
      });
    }
  }
});