Page({
  data: {
    resumeInfo: null,
    source: 'template', // 'ai' 或 'template'
    resumeData: null
  },

  onLoad: function(options) {
    // 获取来源
    const source = options.source || 'template';
    this.setData({
      source: source
    });

    // 从本地存储获取简历信息
    const tempResumeInfo = wx.getStorageSync('tempResumeInfo');
    if (tempResumeInfo) {
      this.setData({
        resumeInfo: tempResumeInfo
      });

      // 根据来源加载不同的简历数据
      if (source === 'ai') {
        this.loadAiGeneratedResume(tempResumeInfo);
      } else {
        this.loadTemplateResume(tempResumeInfo);
      }
    }
  },

  // 加载AI生成的简历
  loadAiGeneratedResume: function(resumeInfo) {
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
        address: '北京市朝阳区'
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
      skills: this.extractSkillsFromExperience(resumeInfo.workExperience).split('、')
    };

    this.setData({
      resumeData: aiResumeData
    });
  },

  // 加载模板编辑的简历
  loadTemplateResume: function(resumeInfo) {
    // 模拟模板编辑的简历数据
    // 在实际项目中，这里应该使用用户实际填写的数据
    const templateResumeData = {
      title: resumeInfo.title,
      isAiGenerated: false,
      templateId: resumeInfo.templateId,
      personalInfo: {
        name: '',
        jobTitle: '',
        phone: '',
        email: '',
        address: ''
      },
      workExperience: [],
      education: [],
      skills: [],
      selfEvaluation: ''
    };

    this.setData({
      resumeData: templateResumeData
    });
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

  // 返回编辑
  goBackToEdit: function() {
    if (this.data.source === 'ai') {
      wx.navigateBack();
    } else {
      wx.navigateTo({
        url: `/pages/resume/edit/edit?templateId=${this.data.resumeInfo.templateId}`
      });
    }
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
    wx.showToast({
      title: '分享功能开发中',
      icon: 'none'
    });
  }
});