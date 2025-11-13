// 技能数据丢失问题修复脚本
// 包含增强的数据验证、错误恢复和备份机制

Page({
  data: {
    resumeData: {},
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

  onShow: function () {
    // 每次页面显示时重新加载数据，确保数据同步
    this.loadResumeData();
  },

  // 增强的简历数据加载函数
  loadResumeData: function() {
    try {
      console.log('🚀 开始加载简历数据（增强版）');
      
      // 尝试多种数据源
      let storedData = this.getResumeDataFromStorage();
      let rawData = this.validateAndExtractData(storedData);
      
      console.log('📊 原始数据验证完成:', {
        hasSkills: !!rawData.skills,
        skillsLength: rawData.skills ? rawData.skills.length : 0,
        hasSkillsWithLevel: !!rawData.skillsWithLevel,
        skillsWithLevelLength: rawData.skillsWithLevel ? rawData.skillsWithLevel.length : 0
      });

      // 数据标准化
      const normalizedResumeData = this.normalizeResumeDataWithValidation(rawData);
      
      // 最终验证
      this.validateFinalData(normalizedResumeData);
      
      this.setData({
        resumeData: normalizedResumeData
      });
      
      console.log('✅ 简历数据加载完成');
      
    } catch (error) {
      console.error('❌ 简历数据加载失败:', error);
      this.handleLoadError(error);
    }
  },

  // 从存储获取数据（增强版）
  getResumeDataFromStorage: function() {
    let data = null;
    
    try {
      // 尝试主存储
      data = wx.getStorageSync('resumeData');
      console.log('📁 主存储数据:', data);
      
      // 如果主存储失败，尝试备份
      if (!data || !data.data) {
        console.warn('⚠️ 主存储无效，尝试备份数据');
        data = wx.getStorageSync('resumeData_backup');
        console.log('📁 备份存储数据:', data);
      }
      
      // 如果都失败，尝试临时存储
      if (!data || !data.data) {
        console.warn('⚠️ 备份存储无效，尝试临时存储');
        data = wx.getStorageSync('tempResumeInfo');
        console.log('📁 临时存储数据:', data);
      }
      
    } catch (error) {
      console.error('存储读取错误:', error);
    }
    
    return data;
  },

  // 数据验证和提取
  validateAndExtractData: function(storedData) {
    let rawData = null;
    
    if (storedData && storedData.data) {
      console.log('✅ 找到有效数据结构');
      rawData = storedData.data;
    } else if (storedData && !storedData.data) {
      console.log('✅ 找到扁平化数据结构');
      rawData = storedData;
    } else {
      console.warn('⚠️ 未找到有效数据，使用默认值');
      rawData = this.getDefaultData();
    }
    
    return this.ensureSkillsData(rawData);
  },

  // 确保技能数据存在
  ensureSkillsData: function(rawData) {
    if (!rawData.skills && !rawData.skillsWithLevel) {
      console.warn('⚠️ 未找到技能数据，创建默认技能');
      rawData.skills = ['JavaScript', 'Python', 'React'];
      rawData.skillsWithLevel = [
        { name: 'JavaScript', level: 4 },
        { name: 'Python', level: 3 },
        { name: 'React', level: 4 }
      ];
    } else if (rawData.skills && !rawData.skillsWithLevel) {
      console.log('✅ 有skills数据，生成skillsWithLevel');
      rawData.skillsWithLevel = rawData.skills.map(name => ({
        name: name,
        level: 3 // 默认中等水平
      }));
    } else if (!rawData.skills && rawData.skillsWithLevel) {
      console.log('✅ 有skillsWithLevel数据，生成skills');
      rawData.skills = rawData.skillsWithLevel.map(item => item.name);
    }
    
    return rawData;
  },

  // 带验证的数据标准化
  normalizeResumeDataWithValidation: function(rawData) {
    console.log('🔧 开始数据标准化（带验证）');
    
    let normalizedData = {};
    
    try {
      // 根据模板类型进行不同的标准化处理
      switch (this.data.templateId) {
        case 'template-four':
        case 'template-five':
        case 'template-six':
          normalizedData = this.normalizeTemplateFourFiveSix(rawData);
          break;
        case 'template-one':
          normalizedData = this.normalizeTemplateOne(rawData);
          break;
        default:
          normalizedData = this.normalizeDefaultTemplate(rawData);
      }
      
      // 通用字段处理
      normalizedData = this.addCommonFields(normalizedData, rawData);
      
      console.log('✅ 数据标准化完成');
      
    } catch (error) {
      console.error('数据标准化失败:', error);
      // 如果标准化失败，返回原始数据
      normalizedData = rawData;
    }
    
    return normalizedData;
  },

  // 模板4/5/6标准化（增强版）
  normalizeTemplateFourFiveSix: function(rawData) {
    console.log('🎨 处理模板4/5/6数据');
    
    let normalizedData = {};
    
    // 处理技能数据
    let skillsData = rawData.skills || rawData.skillsWithLevel;
    console.log('📝 技能数据源:', skillsData);
    
    if (skillsData && Array.isArray(skillsData)) {
      normalizedData.skills = skillsData.map((skill, index) => {
        if (typeof skill === 'string') {
          console.log(`转换技能 ${index}: ${skill} -> 对象格式`);
          return {
            name: skill,
            level: 80 // 默认熟练度80%
          };
        } else if (skill && skill.name) {
          console.log(`保持技能 ${index}: ${skill.name}`);
          return {
            name: skill.name,
            level: skill.level || 80
          };
        } else {
          console.warn(`⚠️ 无效技能项 ${index}:`, skill);
          return {
            name: '未知技能',
            level: 50
          };
        }
      });
    } else {
      console.warn('⚠️ 未找到有效技能数据，使用默认值');
      normalizedData.skills = [
        { name: 'JavaScript', level: 80 },
        { name: 'Python', level: 70 }
      ];
    }
    
    return normalizedData;
  },

  // 模板1标准化
  normalizeTemplateOne: function(rawData) {
    console.log('🎨 处理模板1数据');
    
    let normalizedData = {};
    
    // 处理技能数据
    if (rawData.skillsWithLevel && Array.isArray(rawData.skillsWithLevel)) {
      normalizedData.skillsWithLevel = rawData.skillsWithLevel.map(skill => ({
        ...skill,
        level: skill.level || 50
      }));
    } else if (rawData.skills && Array.isArray(rawData.skills)) {
      normalizedData.skillsWithLevel = rawData.skills.map(name => ({
        name: name,
        level: 50
      }));
    }
    
    return normalizedData;
  },

  // 默认模板标准化
  normalizeDefaultTemplate: function(rawData) {
    console.log('🎨 处理默认模板数据');
    return this.normalizeTemplateOne(rawData);
  },

  // 添加通用字段
  addCommonFields: function(normalizedData, rawData) {
    // 确保所有模板都能访问到skillsWithLevel和skills
    if (!normalizedData.skillsWithLevel && rawData.skillsWithLevel) {
      normalizedData.skillsWithLevel = rawData.skillsWithLevel;
    }
    if (!normalizedData.skills && rawData.skills) {
      normalizedData.skills = rawData.skills;
    }
    
    // 其他通用字段
    normalizedData.title = rawData.title || '我的简历';
    normalizedData.personalInfo = rawData.personalInfo || {};
    normalizedData.education = rawData.education || [];
    normalizedData.workExperience = rawData.workExperience || [];
    normalizedData.projectExperienceList = rawData.projectExperienceList || [];
    normalizedData.selfEvaluation = rawData.selfEvaluation || '';
    
    return normalizedData;
  },

  // 最终数据验证
  validateFinalData: function(normalizedData) {
    if (!normalizedData.skills || normalizedData.skills.length === 0) {
      console.warn('⚠️ 最终数据验证失败：缺少技能数据');
      throw new Error('技能数据丢失');
    }
    
    console.log('✅ 最终数据验证通过');
    console.log('- skills 数量:', normalizedData.skills.length);
    console.log('- skillsWithLevel 数量:', normalizedData.skillsWithLevel ? normalizedData.skillsWithLevel.length : 0);
  },

  // 处理加载错误
  handleLoadError: function(error) {
    console.error('处理加载错误:', error);
    
    // 使用默认数据
    const defaultData = this.getDefaultData();
    const normalizedData = this.normalizeResumeDataWithValidation(defaultData);
    
    this.setData({
      resumeData: normalizedData
    });
    
    // 显示错误提示
    wx.showToast({
      title: '数据加载失败，使用默认数据',
      icon: 'none',
      duration: 3000
    });
  },

  // 获取默认数据
  getDefaultData: function() {
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
      projectExperienceList: [],
      selfEvaluation: '请添加自我评价'
    };
  },

  // 使用模板
  useTemplate: function(e) {
    const templateId = e.currentTarget.dataset.templateId;
    const templateInfo = this.data.resumeData;
    
    console.log('使用模板:', templateId);
    
    // 保存模板信息到临时存储
    wx.setStorageSync('tempResumeInfo', {
      templateId: templateId,
      resumeInfo: templateInfo
    });
    
    wx.navigateTo({
      url: `/pages/template/edit/edit?templateId=${templateId}`
    });
  }
});