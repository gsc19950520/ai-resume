// 编辑页面技能数据丢失修复脚本
// 包含增强的数据验证、备份和错误处理

Page({
  data: {
    resumeInfo: {},
    templateId: '',
    debugMode: true
  },

  onLoad: function (options) {
    if (options.templateId) {
      this.setData({
        templateId: options.templateId
      });
    }
    this.loadResumeData();
  },

  // 增强的简历数据加载
  loadResumeData: function() {
    try {
      console.log('🚀 开始加载编辑页面数据（增强版）');
      
      let resumeInfo = this.getResumeDataFromStorage();
      
      // 验证和修复技能数据
      resumeInfo = this.validateAndFixSkills(resumeInfo);
      
      console.log('📊 最终简历数据:', {
        hasSkills: !!resumeInfo.skills,
        skillsLength: resumeInfo.skills ? resumeInfo.skills.length : 0,
        hasSkillsWithLevel: !!resumeInfo.skillsWithLevel,
        skillsWithLevelLength: resumeInfo.skillsWithLevel ? resumeInfo.skillsWithLevel.length : 0
      });
      
      this.setData({
        resumeInfo: resumeInfo
      });
      
    } catch (error) {
      console.error('❌ 编辑页面数据加载失败:', error);
      this.handleLoadError();
    }
  },

  // 从存储获取数据
  getResumeDataFromStorage: function() {
    let resumeInfo = {};
    
    try {
      // 尝试从临时存储获取
      const tempData = wx.getStorageSync('tempResumeInfo');
      console.log('📁 临时存储数据:', tempData);
      
      if (tempData && tempData.resumeInfo) {
        resumeInfo = tempData.resumeInfo;
        console.log('✅ 使用临时存储数据');
      } else {
        // 使用默认数据
        console.log('⚠️ 使用默认数据');
        resumeInfo = this.getDefaultResumeInfo();
      }
      
    } catch (error) {
      console.error('存储读取错误:', error);
      resumeInfo = this.getDefaultResumeInfo();
    }
    
    return resumeInfo;
  },

  // 验证和修复技能数据
  validateAndFixSkills: function(resumeInfo) {
    console.log('🔧 开始验证和修复技能数据');
    
    // 确保技能数据存在
    if (!resumeInfo.skills && !resumeInfo.skillsWithLevel) {
      console.warn('⚠️ 未找到任何技能数据，创建默认技能');
      resumeInfo.skills = ['JavaScript', 'Python'];
      resumeInfo.skillsWithLevel = [
        { name: 'JavaScript', level: 4 },
        { name: 'Python', level: 3 }
      ];
    }
    
    // 如果只有skills，生成skillsWithLevel
    if (resumeInfo.skills && !resumeInfo.skillsWithLevel) {
      console.log('✅ 有skills数据，生成skillsWithLevel');
      resumeInfo.skillsWithLevel = resumeInfo.skills.map(name => ({
        name: name,
        level: 3 // 默认中等水平
      }));
    }
    
    // 如果只有skillsWithLevel，生成skills
    if (!resumeInfo.skills && resumeInfo.skillsWithLevel) {
      console.log('✅ 有skillsWithLevel数据，生成skills');
      resumeInfo.skills = resumeInfo.skillsWithLevel.map(item => item.name);
    }
    
    // 验证数据一致性
    if (resumeInfo.skills && resumeInfo.skillsWithLevel) {
      const skillsFromNames = resumeInfo.skillsWithLevel.map(item => item.name);
      const skillsMatch = JSON.stringify(resumeInfo.skills.sort()) === JSON.stringify(skillsFromNames.sort());
      
      if (!skillsMatch) {
        console.warn('⚠️ 技能数据不一致，重新同步');
        resumeInfo.skills = skillsFromNames;
      }
    }
    
    return resumeInfo;
  },

  // 保存简历（增强版）
  saveResume: function() {
    try {
      console.log('🚀 开始保存简历（增强版）');
      
      // 获取当前数据
      const resumeInfo = this.data.resumeInfo;
      
      // 处理技能数据
      const processedSkills = this.processSkillsData(resumeInfo);
      
      // 创建完整的简历数据
      const resumeData = {
        isAiGenerated: false,
        templateId: this.data.templateId,
        data: {
          title: resumeInfo.title || '我的简历',
          personalInfo: resumeInfo.personalInfo || {},
          education: resumeInfo.education || [],
          workExperience: resumeInfo.workExperience || [],
          skills: processedSkills.skills,
          skillsWithLevel: processedSkills.skillsWithLevel,
          selfEvaluation: resumeInfo.selfEvaluation || ''
        }
      };
      
      console.log('📊 准备保存的简历数据:', {
        hasSkills: !!resumeData.data.skills,
        skillsLength: resumeData.data.skills ? resumeData.data.skills.length : 0,
        hasSkillsWithLevel: !!resumeData.data.skillsWithLevel,
        skillsWithLevelLength: resumeData.data.skillsWithLevel ? resumeData.data.skillsWithLevel.length : 0
      });
      
      // 保存到主存储
      wx.setStorageSync('resumeData', resumeData);
      console.log('✅ 主存储保存成功');
      
      // 保存到备份存储
      wx.setStorageSync('resumeData_backup', resumeData);
      console.log('✅ 备份存储保存成功');
      
      // 显示成功提示
      wx.showToast({
        title: '简历保存成功',
        icon: 'success',
        duration: 2000
      });
      
      // 延迟跳转，确保用户看到提示
      setTimeout(() => {
        wx.navigateTo({
          url: `/pages/template/preview/preview?templateId=${this.data.templateId}`
        });
      }, 2000);
      
    } catch (error) {
      console.error('❌ 简历保存失败:', error);
      
      wx.showToast({
        title: '保存失败，请重试',
        icon: 'error',
        duration: 3000
      });
    }
  },

  // 处理技能数据（增强版）
  processSkillsData: function(resumeInfo) {
    console.log('🔧 开始处理技能数据');
    
    let skills = [];
    let skillsWithLevel = [];
    
    // 优先使用skillsWithLevel
    if (resumeInfo.skillsWithLevel && Array.isArray(resumeInfo.skillsWithLevel)) {
      console.log('📋 原始skillsWithLevel数据:', resumeInfo.skillsWithLevel);
      
      // 过滤空技能名称
      skillsWithLevel = resumeInfo.skillsWithLevel.filter(skill => {
        const isValid = skill && skill.name && skill.name.trim() !== '';
        if (!isValid) {
          console.warn('⚠️ 过滤无效技能项:', skill);
        }
        return isValid;
      });
      
      console.log('✅ 过滤后的skillsWithLevel:', skillsWithLevel);
      
      // 生成skills数组
      skills = skillsWithLevel.map(skill => skill.name);
      console.log('✅ 提取的skills:', skills);
      
    } else if (resumeInfo.skills && Array.isArray(resumeInfo.skills)) {
      console.log('📋 使用skills数据:', resumeInfo.skills);
      
      // 过滤空技能名称
      skills = resumeInfo.skills.filter(name => name && name.trim() !== '');
      console.log('✅ 过滤后的skills:', skills);
      
      // 生成skillsWithLevel
      skillsWithLevel = skills.map(name => ({
        name: name,
        level: 3 // 默认中等水平
      }));
      console.log('✅ 生成的skillsWithLevel:', skillsWithLevel);
      
    } else {
      console.warn('⚠️ 未找到技能数据，使用默认值');
      skills = ['JavaScript', 'Python'];
      skillsWithLevel = [
        { name: 'JavaScript', level: 4 },
        { name: 'Python', level: 3 }
      ];
    }
    
    return {
      skills: skills,
      skillsWithLevel: skillsWithLevel
    };
  },

  // 处理加载错误
  handleLoadError: function() {
    console.error('处理编辑页面加载错误');
    
    const defaultResumeInfo = this.getDefaultResumeInfo();
    
    this.setData({
      resumeInfo: defaultResumeInfo
    });
    
    wx.showToast({
      title: '数据加载失败，使用默认数据',
      icon: 'none',
      duration: 3000
    });
  },

  // 获取默认简历信息
  getDefaultResumeInfo: function() {
    return {
      title: '我的简历',
      personalInfo: {
        name: '姓名',
        jobTitle: '职位',
        phone: '电话',
        email: '邮箱',
        address: '地址'
      },
      skills: ['JavaScript', 'Python', 'React'],
      skillsWithLevel: [
        { name: 'JavaScript', level: 4 },
        { name: 'Python', level: 3 },
        { name: 'React', level: 4 }
      ],
      education: [],
      workExperience: [],
      selfEvaluation: '请添加自我评价'
    };
  },

  // 技能输入处理（增强版）
  onSkillInput: function(e) {
    const index = e.currentTarget.dataset.index;
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    
    console.log(`技能输入: index=${index}, field=${field}, value=${value}`);
    
    let skillsWithLevel = [...this.data.resumeInfo.skillsWithLevel];
    
    if (field === 'name') {
      skillsWithLevel[index].name = value;
    } else if (field === 'level') {
      skillsWithLevel[index].level = parseInt(value) || 1;
    }
    
    // 同步更新skills数组
    const skills = skillsWithLevel.map(skill => skill.name);
    
    this.setData({
      'resumeInfo.skills': skills,
      'resumeInfo.skillsWithLevel': skillsWithLevel
    });
    
    console.log('技能数据更新完成:', {
      skills: skills,
      skillsWithLevel: skillsWithLevel
    });
  },

  // 添加技能
  addSkill: function() {
    let skillsWithLevel = [...this.data.resumeInfo.skillsWithLevel];
    skillsWithLevel.push({ name: '', level: 3 });
    
    const skills = skillsWithLevel.map(skill => skill.name);
    
    this.setData({
      'resumeInfo.skills': skills,
      'resumeInfo.skillsWithLevel': skillsWithLevel
    });
    
    console.log('添加新技能');
  },

  // 删除技能
  removeSkill: function(e) {
    const index = e.currentTarget.dataset.index;
    
    let skillsWithLevel = [...this.data.resumeInfo.skillsWithLevel];
    skillsWithLevel.splice(index, 1);
    
    const skills = skillsWithLevel.map(skill => skill.name);
    
    this.setData({
      'resumeInfo.skills': skills,
      'resumeInfo.skillsWithLevel': skillsWithLevel
    });
    
    console.log(`删除技能: index=${index}`);
  }
});