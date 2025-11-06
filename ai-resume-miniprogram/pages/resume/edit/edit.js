Page({
  data: {
    currentSection: 'personal', // 当前编辑的部分
    sections: [
      { id: 'personal', name: '个人信息' },
      { id: 'education', name: '教育经历' },
      { id: 'work', name: '工作经历' },
      { id: 'projects', name: '项目经验' },
      { id: 'skills', name: '专业技能' },
      { id: 'self', name: '自我评价' }
    ],
    resumeInfo: {
      title: '',
      personalInfo: {
        name: '',
        jobTitle: '',
        phone: '',
        email: '',
        address: ''
      },
      education: [
        {
          id: 1,
          school: '',
          major: '',
          degree: '',
          startDate: '',
          endDate: ''
        }
      ],
      workExperience: [
        {
          id: 1,
          company: '',
          position: '',
          startDate: '',
          endDate: '',
          description: ''
        }
      ],
      projects: [
        {
          id: 1,
          name: '',
          role: '',
          startDate: '',
          endDate: '',
          description: ''
        }
      ],
      skills: '',
      selfEvaluation: ''
    }
  },

  onLoad: function(options) {
    // 从本地存储获取简历信息
    const tempResumeInfo = wx.getStorageSync('tempResumeInfo');
    if (tempResumeInfo) {
      this.setData({
        resumeInfo: tempResumeInfo
      });
    }

    // 获取模板ID
    const templateId = options.templateId || tempResumeInfo.templateId;
    console.log('编辑模板ID:', templateId);
  },

  // 切换编辑部分
  switchSection: function(e) {
    const sectionId = e.currentTarget.dataset.id;
    this.setData({
      currentSection: sectionId
    });
  },

  // 保存个人信息
  savePersonalInfo(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    this.setData({
      [`resumeInfo.personalInfo.${field}`]: value
    });
  },

  // 处理教育经历输入
  onEducationInput(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    this.setData({
      [`resumeInfo.education[${index}].${field}`]: value
    });
  },

  // 处理工作经历输入
  onWorkInput(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    this.setData({
      [`resumeInfo.workExperience[${index}].${field}`]: value
    });
  },

  // 处理项目经验输入
  onProjectInput(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    this.setData({
      [`resumeInfo.projects[${index}].${field}`]: value
    });
  },

  // 处理专业技能输入
  onSkillsInput(e) {
    this.setData({
      'resumeInfo.skills': e.detail.value
    });
  },

  // 处理自我评价输入
  onSelfEvaluationInput(e) {
    this.setData({
      'resumeInfo.selfEvaluation': e.detail.value
    });
  },

  // 添加教育经历
  addEducation: function() {
    const education = [...this.data.resumeInfo.education];
    education.push({
      id: Date.now(), // 添加唯一ID
      school: '',
      major: '',
      degree: '',
      startDate: '',
      endDate: ''
    });
    this.setData({
      'resumeInfo.education': education
    });
  },
  
  // 删除教育经历
  removeEducation: function(e) {
    const index = e.currentTarget.dataset.index;
    const education = [...this.data.resumeInfo.education];
    
    if (education.length > 1) {
      education.splice(index, 1);
      this.setData({
        'resumeInfo.education': education
      });
    } else {
      wx.showToast({
        title: '至少保留一条教育经历',
        icon: 'none'
      });
    }
  },

  // 添加工作经历
  addWorkExperience: function() {
    const workExperience = [...this.data.resumeInfo.workExperience];
    workExperience.push({
      id: Date.now(), // 添加唯一ID
      company: '',
      position: '',
      startDate: '',
      endDate: '',
      description: ''
    });
    this.setData({
      'resumeInfo.workExperience': workExperience
    });
  },
  
  // 删除工作经历
  removeWorkExperience: function(e) {
    const index = e.currentTarget.dataset.index;
    const workExperience = [...this.data.resumeInfo.workExperience];
    
    if (workExperience.length > 1) {
      workExperience.splice(index, 1);
      this.setData({
        'resumeInfo.workExperience': workExperience
      });
    } else {
      wx.showToast({
        title: '至少保留一条工作经历',
        icon: 'none'
      });
    }
  },

  // 添加项目经验
  addProject: function() {
    const projects = [...this.data.resumeInfo.projects];
    projects.push({
      id: Date.now(), // 添加唯一ID
      name: '',
      role: '',
      startDate: '',
      endDate: '',
      description: ''
    });
    this.setData({
      'resumeInfo.projects': projects
    });
  },
  
  // 删除项目经验
  removeProject: function(e) {
    const index = e.currentTarget.dataset.index;
    const projects = [...this.data.resumeInfo.projects];
    
    if (projects.length > 1) {
      projects.splice(index, 1);
      this.setData({
        'resumeInfo.projects': projects
      });
    } else {
      wx.showToast({
        title: '至少保留一个项目经验',
        icon: 'none'
      });
    }
  },

  // 保存简历
  saveResume: function() {
    wx.showLoading({
      title: '保存中...',
    });

    setTimeout(() => {
      wx.hideLoading();
      wx.showToast({
        title: '保存成功',
        icon: 'success',
      });
      
      // 保存到本地存储
      wx.setStorageSync('resumeData', {
        isAiGenerated: false,
        data: {
          title: this.data.resumeInfo.title,
          personalInfo: this.data.resumeInfo.personalInfo,
          education: this.data.resumeInfo.education,
          workExperience: this.data.resumeInfo.workExperience,
          projects: this.data.resumeInfo.projects,
          skills: this.data.resumeInfo.skills ? this.data.resumeInfo.skills.split(',').map(skill => skill.trim()) : [],
          selfEvaluation: this.data.resumeInfo.selfEvaluation
        }
      });
      
      // 跳转到简历预览页面
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/resume/view/view?source=template'
        });
      }, 1500);
    }, 1000);
  }
});