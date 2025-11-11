Page({
  data: {
    currentSection: 'personal', // 当前编辑的部分
    sections: [
      { id: 'personal', name: '个人信息' },
      { id: 'education', name: '教育经历' },
      { id: 'work', name: '工作经历' },
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
        address: '',
        birthDate: ''
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

      skills: '',
      selfEvaluation: ''
    }
  },

  onLoad: function(options) {
    try {
      // 从本地存储获取简历信息
      const tempResumeInfo = wx.getStorageSync('tempResumeInfo');
      if (tempResumeInfo) {
        try {
          // 进行深拷贝，避免引用问题
          const resumeInfoCopy = JSON.parse(JSON.stringify(tempResumeInfo));
          
          // 验证并修复数据结构完整性
          const validatedResumeInfo = this.validateResumeInfo(resumeInfoCopy);
          
          this.setData({
            resumeInfo: validatedResumeInfo
          });
          console.log('成功从本地存储恢复数据:', JSON.stringify({educationLength: validatedResumeInfo.education.length, workExperienceLength: validatedResumeInfo.workExperience.length}));
        } catch (parseError) {
          console.error('解析本地存储数据失败:', parseError);
          // 保留默认数据结构
        }
      }
    } catch (error) {
      console.error('加载简历信息异常:', error);
    }

    // 优先从options获取模板ID
    if (options.templateId) {
      console.log('从options获取模板ID:', options.templateId);
      this.setData({
        'resumeInfo.templateId': options.templateId
      });
    } else if (this.data.resumeInfo.templateId) {
      console.log('从resumeInfo获取模板ID:', this.data.resumeInfo.templateId);
    } else {
      console.log('未找到模板ID');
    }
  },
  
  // 验证并修复简历数据结构完整性
  validateResumeInfo: function(resumeInfo) {
    const defaultInfo = this.data.resumeInfo;
    
    // 确保必要的字段存在
    const validated = { ...defaultInfo, ...resumeInfo };
    
    // 特别验证复杂数据结构
    if (!validated.education || !Array.isArray(validated.education)) {
      validated.education = defaultInfo.education;
    }
    
    if (!validated.workExperience || !Array.isArray(validated.workExperience)) {
      validated.workExperience = defaultInfo.workExperience;
    }
    
    // 确保personalInfo对象存在
    if (!validated.personalInfo || typeof validated.personalInfo !== 'object') {
      validated.personalInfo = defaultInfo.personalInfo;
    }
    
    return validated;
  },

  // 切换编辑部分
  switchSection: function(e) {
    const sectionId = e.currentTarget.dataset.id;
    
    try {
      // 切换前临时保存当前数据到本地存储，防止数据丢失
      // 使用JSON序列化确保复杂数据结构正确存储
      const resumeInfoString = JSON.stringify(this.data.resumeInfo);
      wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
      console.log('切换前保存数据:', {fromSection: this.data.currentSection, toSection: sectionId});
    } catch (error) {
      console.error('切换时保存数据失败:', error);
    }
    
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
    
    // 即时保存数据到本地存储
    wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
  },

  // 处理教育经历输入
  onEducationInput(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    
    try {
      // 确保education存在且为数组，并且索引有效
      if (!this.data.resumeInfo.education || !Array.isArray(this.data.resumeInfo.education) || index < 0 || index >= this.data.resumeInfo.education.length) {
        console.error('教育经历数据异常或索引无效');
        return;
      }
      
      // 创建新的education数组副本进行更新
      const updatedEducation = [...this.data.resumeInfo.education];
      if (!updatedEducation[index]) {
        updatedEducation[index] = {};
      }
      updatedEducation[index][field] = value;
      
      this.setData({
        'resumeInfo.education': updatedEducation
      });
      
      // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
      const resumeInfoString = JSON.stringify(this.data.resumeInfo);
      wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
      console.log('教育经历数据更新并保存:', {index, field, value});
    } catch (error) {
      console.error('处理教育经历输入异常:', error);
    }
  },

  // 处理工作经历输入
  onWorkInput(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    
    try {
      // 确保workExperience存在且为数组，并且索引有效
      if (!this.data.resumeInfo.workExperience || !Array.isArray(this.data.resumeInfo.workExperience) || index < 0 || index >= this.data.resumeInfo.workExperience.length) {
        console.error('工作经历数据异常或索引无效');
        return;
      }
      
      // 创建新的workExperience数组副本进行更新
      const updatedWorkExperience = [...this.data.resumeInfo.workExperience];
      if (!updatedWorkExperience[index]) {
        updatedWorkExperience[index] = {};
      }
      updatedWorkExperience[index][field] = value;
      
      this.setData({
        'resumeInfo.workExperience': updatedWorkExperience
      });
      
      // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
      const resumeInfoString = JSON.stringify(this.data.resumeInfo);
      wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
      console.log('工作经历数据更新并保存:', {index, field, value});
    } catch (error) {
      console.error('处理工作经历输入异常:', error);
    }
  },



  // 处理专业技能输入
  onSkillsInput(e) {
    this.setData({
      'resumeInfo.skills': e.detail.value
    });
    
    // 即时保存数据到本地存储
    wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
  },

  // 处理自我评价输入
  onSelfEvaluationInput(e) {
    this.setData({
      'resumeInfo.selfEvaluation': e.detail.value
    });
    
    // 即时保存数据到本地存储
    wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
  },

  // 添加教育经历
  addEducation: function() {
    try {
      // 确保resumeInfo和education存在且为数组
      if (!this.data.resumeInfo.education || !Array.isArray(this.data.resumeInfo.education)) {
        this.setData({
          'resumeInfo.education': []
        });
      }
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
      
      // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
      const resumeInfoString = JSON.stringify(this.data.resumeInfo);
      wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
      console.log('添加教育经历成功，当前数量:', education.length);
    } catch (error) {
      console.error('添加教育经历异常:', error);
      wx.showToast({
        title: '添加失败，请重试',
        icon: 'none'
      });
    }
  },
  
  // 删除教育经历
  removeEducation: function(e) {
    const index = e.currentTarget.dataset.index;
    
    try {
      // 确保education存在且为数组
      if (!this.data.resumeInfo.education || !Array.isArray(this.data.resumeInfo.education) || this.data.resumeInfo.education.length === 0) {
        wx.showToast({
          title: '数据异常',
          icon: 'none'
        });
        return;
      }
      
      const education = [...this.data.resumeInfo.education];
      
      if (education.length > 1) {
        education.splice(index, 1);
        this.setData({
          'resumeInfo.education': education
        });
        
        // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
        const resumeInfoString = JSON.stringify(this.data.resumeInfo);
        wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
        console.log('删除教育经历成功，当前数量:', education.length);
      } else {
        wx.showToast({
          title: '至少保留一条教育经历',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('删除教育经历异常:', error);
      wx.showToast({
        title: '删除失败，请重试',
        icon: 'none'
      });
    }
  },

  // 添加工作经历
  addWorkExperience: function() {
    try {
      // 确保workExperience存在且为数组
      if (!this.data.resumeInfo.workExperience || !Array.isArray(this.data.resumeInfo.workExperience)) {
        this.setData({
          'resumeInfo.workExperience': []
        });
      }
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
      
      // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
      const resumeInfoString = JSON.stringify(this.data.resumeInfo);
      wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
      console.log('添加工作经历成功，当前数量:', workExperience.length);
    } catch (error) {
      console.error('添加工作经历异常:', error);
      wx.showToast({
        title: '添加失败，请重试',
        icon: 'none'
      });
    }
  },
  
  // 删除工作经历
  removeWorkExperience: function(e) {
    const index = e.currentTarget.dataset.index;
    
    try {
      // 确保workExperience存在且为数组
      if (!this.data.resumeInfo.workExperience || !Array.isArray(this.data.resumeInfo.workExperience) || this.data.resumeInfo.workExperience.length === 0) {
        wx.showToast({
          title: '数据异常',
          icon: 'none'
        });
        return;
      }
      
      const workExperience = [...this.data.resumeInfo.workExperience];
      
      if (workExperience.length > 1) {
        workExperience.splice(index, 1);
        this.setData({
          'resumeInfo.workExperience': workExperience
        });
        
        // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
        const resumeInfoString = JSON.stringify(this.data.resumeInfo);
        wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
        console.log('删除工作经历成功，当前数量:', workExperience.length);
      } else {
        wx.showToast({
          title: '至少保留一条工作经历',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('删除工作经历异常:', error);
      wx.showToast({
        title: '删除失败，请重试',
        icon: 'none'
      });
    }
  },



  // 保存简历
  saveResume: function() {
    // 检查是否有模板ID
    if (!this.data.resumeInfo.templateId) {
      wx.showToast({
        title: '请先选择简历模板',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({
      title: '保存并生成简历中...',
    });

    try {
      // 准备简历数据，包含模板ID
      const resumeData = {
        isAiGenerated: false,
        templateId: this.data.resumeInfo.templateId,
        data: {
          title: this.data.resumeInfo.title,
          personalInfo: this.data.resumeInfo.personalInfo,
          education: this.data.resumeInfo.education,
          workExperience: this.data.resumeInfo.workExperience,
          skills: this.data.resumeInfo.skills ? this.data.resumeInfo.skills.split(',').map(skill => skill.trim()) : [],
          selfEvaluation: this.data.resumeInfo.selfEvaluation
        }
      };
      
      console.log('准备调用后端API，模板ID:', this.data.resumeInfo.templateId);
      console.log('传递的简历数据:', JSON.stringify(resumeData.data));
      
      // 调用后端API根据选定模板生成简历HTML
      const app = getApp();
      app.cloudCall('/api/resume/render', {
        templateId: this.data.resumeInfo.templateId,
        resumeData: resumeData.data
      }, 'POST')
      .then(res => {
        console.log('后端API调用成功:', res);
        
        // 将后端返回的HTML保存到数据中
        if (res && res.html) {
          resumeData.renderedHtml = res.html;
          console.log('成功获取后端渲染的HTML');
        } else {
          console.warn('后端未返回HTML内容');
        }
        
        // 保存到本地存储
        wx.setStorageSync('resumeData', resumeData);
        console.log('已保存简历数据到本地存储');
        
        wx.hideLoading();
        wx.showToast({
          title: '保存成功',
          icon: 'success',
        });
        
        // 跳转到简历预览页面，传递模板ID
        setTimeout(() => {
          wx.navigateTo({
            url: `/pages/resume/view/view?source=template&templateId=${this.data.resumeInfo.templateId}`
          });
        }, 1500);
      })
      .catch(err => {
        console.error('后端API调用失败:', err);
        
        // 即使API调用失败，也保存数据到本地，确保基本功能可用
        wx.setStorageSync('resumeData', resumeData);
        console.log('API调用失败，但已保存基本数据到本地');
        
        wx.hideLoading();
        wx.showToast({
          title: '简历已保存，但模板渲染失败',
          icon: 'none'
        });
        
        // 跳转到简历预览页面，传递模板ID
        setTimeout(() => {
          wx.navigateTo({
            url: `/pages/resume/view/view?source=template&templateId=${this.data.resumeInfo.templateId}`
          });
        }, 1500);
      });
    } catch (error) {
      console.error('保存简历异常:', error);
      wx.hideLoading();
      wx.showToast({
        title: '保存失败，请重试',
        icon: 'none'
      });
    }
  }
});