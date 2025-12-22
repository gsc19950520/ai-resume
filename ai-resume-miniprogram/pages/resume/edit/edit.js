const app = getApp()
// 导入云托管请求工具
const request = require('../../../utils/request').default
Page({
  data: {
    currentSection: 'personal', // 当前编辑的部分
    sections: [
      { id: 'personal', name: '个人信息' },
      { id: 'education', name: '教育经历' },
      { id: 'work', name: '工作经历' },
      { id: 'projects', name: '项目经验' },
      { id: 'skills', name: '专业技能' },
      { id: 'interests', name: '兴趣爱好' },
      { id: 'self', name: '自我评价' }
    ],
    // 技能等级
    skillLevels: ['了解', '掌握', '熟练', '精通', '专业'],
    jobTypes: [], // 职位类型列表
    userInfo: {}, // 用户基本信息，从User表获取

    resumeInfo: {
      title: '',
      personalInfo: {
        jobTitle: '',
        expectedSalary: '',
        startTime: '',
        jobTypeId: '' // 新增字段，关联职位类型
      },
      interestsText: '',
      // 技能评分列表
      skillsWithLevel: [
        { name: '', level: 0 }
      ],
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
      projectExperienceList: [
        {
          id: 1,
          name: '',
          description: '',
          startDate: '',
          endDate: ''
        }
      ],

      skills: '',
      selfEvaluation: ''
    },
    
    // 标记是否已从后端成功加载简历数据
    hasLoadedFromBackend: false
  },

  onLoad: function(options) {
    try {
      // 优先从options获取模板ID和resumeId
      let templateId = options.templateId || this.data.resumeInfo.templateId;
      let resumeId = options.resumeId;
      
      // 检查是否有从预览页面传递过来的编辑数据
      const app = getApp();
      if (app.globalData.editResumeData && app.globalData.editTemplateId === templateId) {
        console.log('检测到从预览页面传递的编辑数据');
        
        // 使用预览页面传递的数据
        const editData = app.globalData.editResumeData;
        const editTemplateId = app.globalData.editTemplateId;
        
        // 清空全局编辑数据，避免重复加载
        app.globalData.editResumeData = null;
        app.globalData.editTemplateId = null;
        app.globalData.editResumeId = null;
        
        // 格式化并填充数据
        this.fillResumeDataFromBackend(editData, editTemplateId);
        
        // 标记已从后端加载数据
        this.setData({
          hasLoadedFromBackend: true
        });
        
        console.log('成功加载预览页面传递的编辑数据');
        
        // 加载职位类型和用户信息
        this.loadJobTypes();
        this.loadUserInfo();
        return;
      }
      
      // 先调用后端接口获取用户最新简历数据（异步操作）
      this.loadLatestResumeData(templateId);
      
      // 如果没有模板ID，尝试加载临时保存的数据（初始加载）
      if (!templateId) {
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
        console.log('未找到模板ID');
      }
    } catch (error) {
      console.error('加载简历信息异常:', error);
    }
    
    // 加载职位类型数据
    this.loadJobTypes();
    
    // 加载用户信息
    this.loadUserInfo();
  },
  
  
  // 检查用户信息是否完整
  checkUserInfoComplete: function(userInfo) {
    // 根据业务需求定义用户信息的完整性检查标准
    // 例如：检查姓名、手机号、邮箱等必要字段
    const requiredFields = ['name', 'phone', 'email'];
    const isComplete = requiredFields.every(field => 
      userInfo && userInfo[field] && userInfo[field].trim() !== ''
    );
    
    return isComplete;
  },
  
  // 加载用户信息并进行完整性检查
  loadUserInfo: function() {
    const userId = wx.getStorageSync('userId');
    if (userId) {
      // 使用云托管方式请求用户信息
      request.get(`/user/${userId}`, {})
        .then(res => {
          if (res && res.success) {
            this.setData({
              userInfo: res.data
            });
            console.log('用户信息加载成功:', res.data);
            
            // 检查用户信息是否完整
            if (!this.checkUserInfoComplete(res.data)) {
              console.log('用户信息不完整，跳转到提示页面');
              // 跳转到完善个人信息提示页面
              wx.navigateTo({
                url: '/pages/profile/complete-profile/complete-profile'
              });
            }
          } else {
            console.error('加载用户信息失败:', res);
            // 尝试从本地存储获取用户信息（适用于模拟环境）
            this.loadUserInfoFromStorage();
          }
        })
        .catch(err => {
          console.error('请求用户信息失败:', err);
          // 尝试从本地存储获取用户信息（适用于模拟环境）
          this.loadUserInfoFromStorage();
        });
    } else {
      // 尝试从本地存储获取用户信息（适用于模拟环境）
      this.loadUserInfoFromStorage();
    }
  },
  
  // 从本地存储加载用户信息（适用于模拟环境）
  loadUserInfoFromStorage: function() {
    try {
      const userInfo = wx.getStorageSync('userInfo');
      if (userInfo) {
        const parsedUserInfo = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo;
        this.setData({
          userInfo: parsedUserInfo
        });
        console.log('从本地存储加载用户信息成功:', parsedUserInfo);
        
        // 检查用户信息是否完整
        if (!this.checkUserInfoComplete(parsedUserInfo)) {
          console.log('用户信息不完整，跳转到提示页面');
          wx.navigateTo({
            url: '/pages/profile/complete-profile/complete-profile'
          });
        }
      } else {
        console.log('未找到用户ID且本地存储中无用户信息，跳转到提示页面');
        wx.navigateTo({
          url: '/pages/profile/complete-profile/complete-profile'
        });
      }
    } catch (e) {
      console.error('解析本地存储的用户信息失败:', e);
    }
  },
  
  // 页面显示时重新加载用户信息，确保显示最新数据
  onShow: function() {
    this.loadUserInfo();
  },
  
  // 加载职位类型数据
  loadJobTypes: function() {
    // 使用云托管方式请求职位类型
    request.get('/job-types', {})
      .then(res => {
        // 正确解析后端返回的BaseResponseVO结构
        const jobTypeList = res && res.success && res.data ? res.data : [];
        this.setData({
          jobTypes: jobTypeList
        });
        console.log('加载职位类型成功:', jobTypeList);
      })
      .catch(error => {
        console.error('请求职位类型失败:', error);
      });
  },
  
  // 选择职位类型
  onJobTypeChange: function(e) {
    const index = e.detail.value;
    const selectedJobType = this.data.jobTypes[index];
    
    if (selectedJobType) {
      this.setData({
        [`resumeInfo.personalInfo.jobTypeId`]: selectedJobType.id,
        [`resumeInfo.personalInfo.jobTitle`]: selectedJobType.jobName,
        selectedJobTypeName: selectedJobType.jobName,
        jobTypeIndex: index
      });
      
      console.log('选择职位类型:', {jobTypeId: selectedJobType.id, jobTitle: selectedJobType.jobName});
      // 即时保存数据到本地存储
      wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
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
    
    if (!validated.projectExperienceList || !Array.isArray(validated.projectExperienceList)) {
      validated.projectExperienceList = defaultInfo.projectExperienceList;
    }
    
    // 确保个人信息字段存在
    if (!validated.interestsText) {
      validated.interestsText = resumeInfo.interestsText || '';
    }
    
    if (!validated.selfEvaluation) {
      validated.selfEvaluation = resumeInfo.selfEvaluation || '';
    }
    
    // 确保personalInfo对象存在，只保留可编辑字段
    if (!validated.personalInfo || typeof validated.personalInfo !== 'object') {
      validated.personalInfo = defaultInfo.personalInfo;
    } else {
      // 过滤personalInfo，只保留需要的字段
      const allowedFields = ['jobTitle', 'expectedSalary', 'startTime', 'jobTypeId'];
      const filteredPersonalInfo = {};
      allowedFields.forEach(field => {
        filteredPersonalInfo[field] = validated.personalInfo[field] || '';
      });
      validated.personalInfo = filteredPersonalInfo;
    }
    
    return validated;
  },

  // 加载用户最新简历数据
  loadLatestResumeData: function(templateId) {
    const userInfoStr = wx.getStorageSync('userInfo');
    const userInfo = JSON.parse(userInfoStr);
    // 同时检查id和userId字段，确保能正确获取用户ID
    const userId = userInfo ? (userInfo.id || userInfo.userId) : null;
    if (!userId) {
      console.log('用户ID不存在，跳过加载最新简历数据');
      // 用户ID不存在时，使用本地存储作为后备方案
      this.loadLocalResumeData(templateId);
      return;
    }

    // 调用后端接口获取用户最新简历数据，传递userId参数
    request.get('/resume/getLatest', { userId: userId })
      .then(res => {
        if (res && res.success && res.data) {
          console.log('成功获取用户最新简历数据:', res.data);
          
          // 解析后端返回的数据并填充到页面
          const success = this.fillResumeDataFromBackend(res.data, templateId);
          
          // 如果后端没有返回有效数据，使用本地存储作为后备方案
          if (!success) {
            this.loadLocalResumeData(templateId);
          }
        } else {
          console.log('用户暂无简历数据或获取失败:', res);
          // 后端没有数据时，使用本地存储作为后备方案
          this.loadLocalResumeData(templateId);
        }
      })
      .catch(err => {
        console.error('获取用户最新简历数据失败:', err);
        // 接口调用失败时，使用本地存储作为后备方案
        this.loadLocalResumeData(templateId);
      });
  },
  
  // 加载本地存储的简历数据（后备方案）
  loadLocalResumeData: function(templateId) {
    try {
      // 如果已经从后端成功加载数据，就不需要再加载本地数据
      if (this.data.hasLoadedFromBackend) {
        return;
      }
      
      if (templateId) {
        // 如果有模板ID，尝试从本地存储加载对应模板的数据
        
        // 先尝试加载已保存的完整简历数据
        const savedResumeData = wx.getStorageSync('resumeData');
        if (savedResumeData && savedResumeData.templateId === templateId && savedResumeData.data) {
          
          // 从保存的数据中提取需要的信息
          const savedData = savedResumeData.data;
          const resumeInfoCopy = {
            ...this.data.resumeInfo,
            title: savedData.title || '',
            personalInfo: savedData.personalInfo || this.data.resumeInfo.personalInfo,
            education: savedData.education || this.data.resumeInfo.education,
            workExperience: savedData.workExperience || this.data.resumeInfo.workExperience,
            skills: savedData.skills ? savedData.skills.join('、') : '',
            templateId: templateId
          };
          
          // 确保personalInfo对象存在
          if (!resumeInfoCopy.personalInfo) {
            resumeInfoCopy.personalInfo = {};
          }
          
          // 处理兴趣爱好数据，考虑不同的数据存储位置
          if (savedData.interests && Array.isArray(savedData.interests)) {
            resumeInfoCopy.interestsText = savedData.interests.join('\n');
          } else if (savedData.personalInfo && savedData.personalInfo.interests) {
            // 处理旧格式数据，interests存储在personalInfo中
            resumeInfoCopy.interestsText = savedData.personalInfo.interests.join('\n');
          } else if (savedData.interestsText) {
            // 处理直接存储的interestsText
            resumeInfoCopy.interestsText = savedData.interestsText;
          } else {
            resumeInfoCopy.interestsText = '';
          }
          
          // 处理自我评价数据，确保能从不同位置获取
          if (savedData.selfEvaluation) {
            resumeInfoCopy.selfEvaluation = savedData.selfEvaluation;
          } else if (savedData.personalInfo && savedData.personalInfo.selfEvaluation) {
            // 处理旧格式数据，selfEvaluation存储在personalInfo中
            resumeInfoCopy.selfEvaluation = savedData.personalInfo.selfEvaluation;
          } else {
            resumeInfoCopy.selfEvaluation = '';
          }
          
          // 处理技能评分数据
          if (savedData.skillsWithLevel && Array.isArray(savedData.skillsWithLevel)) {
            resumeInfoCopy.skillsWithLevel = savedData.skillsWithLevel;
          }
          
          // 处理项目经验数据
          if (savedData.projectExperienceList && Array.isArray(savedData.projectExperienceList)) {
            resumeInfoCopy.projectExperienceList = savedData.projectExperienceList;
          }
          
          // 验证并修复数据结构完整性
          const validatedResumeInfo = this.validateResumeInfo(resumeInfoCopy);
          
          this.setData({
            resumeInfo: validatedResumeInfo,
            'resumeInfo.templateId': templateId
          });
          
          console.log('成功从本地存储加载简历数据');
          
        } else {
          // 如果没有找到对应模板的数据，尝试加载临时保存的数据
          const tempResumeInfo = wx.getStorageSync('tempResumeInfo');
          if (tempResumeInfo) {
            try {
              // 进行深拷贝，避免引用问题
              const resumeInfoCopy = JSON.parse(JSON.stringify(tempResumeInfo));
              
              // 验证并修复数据结构完整性
              const validatedResumeInfo = this.validateResumeInfo(resumeInfoCopy);
              
              this.setData({
                resumeInfo: validatedResumeInfo,
                'resumeInfo.templateId': templateId
              });
              console.log('成功从本地存储恢复临时数据:', JSON.stringify({educationLength: validatedResumeInfo.education.length, workExperienceLength: validatedResumeInfo.workExperience.length}));
            } catch (parseError) {
              console.error('解析本地存储数据失败:', parseError);
              // 保留默认数据结构，只设置模板ID
              this.setData({
                'resumeInfo.templateId': templateId
              });
            }
          } else {
            // 只设置模板ID
            this.setData({
              'resumeInfo.templateId': templateId
            });
          }
        }
      }
    } catch (error) {
      console.error('加载本地简历数据失败:', error);
    }
  },

  // 将后端返回的简历数据填充到页面
  fillResumeDataFromBackend: function(backendData, templateId) {
    try {
      const resumeInfo = this.data.resumeInfo;
      
      // 填充基本信息（适配后端数据结构）
      if (backendData.userInfo) {
        // 个人信息从userInfo对象中获取
        resumeInfo.personalInfo = {
          name: backendData.userInfo.name || '',
          email: backendData.userInfo.email || '',
          phone: backendData.userInfo.phone || '',
          address: backendData.userInfo.address || '',
          birthDate: backendData.userInfo.birthDate || '',
          nickname: backendData.userInfo.nickname || '',
          avatarUrl: backendData.userInfo.avatarUrl || '',
          gender: backendData.userInfo.gender || '',
          country: backendData.userInfo.country || '',
          province: backendData.userInfo.province || '',
          city: backendData.userInfo.city || ''
        };
      }
      
      // 填充求职相关信息（直接从根对象获取）
      if (backendData.jobTitle || backendData.expectedSalary || backendData.startTime || backendData.jobTypeId) {
        resumeInfo.personalInfo = {
          ...resumeInfo.personalInfo,
          jobTitle: backendData.jobTitle || '',
          expectedSalary: backendData.expectedSalary || '',
          startTime: backendData.startTime || '',
          jobTypeId: backendData.jobTypeId || ''
        };
      }
      
      // 确保jobTypeId字段正确设置，同时从jobTypes列表中查找对应的职位名称更新jobTitle
      if (resumeInfo.personalInfo.jobTypeId && this.data.jobTypes && this.data.jobTypes.length > 0) {
        const jobTypeIdStr = String(resumeInfo.personalInfo.jobTypeId);
        const selectedJobType = this.data.jobTypes.find(type => String(type.id) === jobTypeIdStr);
        if (selectedJobType && selectedJobType.jobName) {  // 修改为jobName
          resumeInfo.personalInfo.jobTitle = selectedJobType.jobName;  // 修改为jobName
          console.log('根据jobTypeId更新jobTitle:', selectedJobType.jobName);  // 修改为jobName
        }
      }
      
      // 填充教育经历
      if (backendData.educationList && Array.isArray(backendData.educationList)) {
        resumeInfo.education = backendData.educationList.map((edu, index) => ({
          id: index + 1,
          school: edu.school || '',
          major: edu.major || '',
          degree: edu.degree || '',
          startDate: edu.startDate || '',
          endDate: edu.endDate || '',
          description: edu.description || ''
        }));
      } else if (backendData.educations && Array.isArray(backendData.educations)) {
        resumeInfo.education = backendData.educations.map((edu, index) => ({
          id: index + 1,
          school: edu.school || '',
          major: edu.major || '',
          degree: edu.degree || '',
          startDate: edu.startDate || '',
          endDate: edu.endDate || '',
          description: edu.description || ''
        }));
      }
      
      // 填充工作经历
      if (backendData.workExperienceList && Array.isArray(backendData.workExperienceList)) {
        resumeInfo.workExperience = backendData.workExperienceList.map((work, index) => ({
          id: index + 1,
          company: work.companyName || '',
          position: work.positionName || '',
          startDate: work.startDate || '',
          endDate: work.endDate || '',
          description: work.description || ''
        }));
      } else if (backendData.works && Array.isArray(backendData.works)) {
        resumeInfo.workExperience = backendData.works.map((work, index) => ({
          id: index + 1,
          company: work.companyName || '',
          position: work.positionName || '',
          startDate: work.startDate || '',
          endDate: work.endDate || '',
          description: work.description || ''
        }));
      }
      
      // 填充项目经验
      if (backendData.projectList && Array.isArray(backendData.projectList)) {
        resumeInfo.projectExperienceList = backendData.projectList.map((project, index) => ({
          id: index + 1,
          name: project.projectName || '',
          description: project.description || '',
          startDate: project.startDate || '',
          endDate: project.endDate || ''
        }));
      } else if (backendData.projects && Array.isArray(backendData.projects)) {
        resumeInfo.projectExperienceList = backendData.projects.map((project, index) => ({
          id: index + 1,
          name: project.projectName || '',
          description: project.description || '',
          startDate: project.startDate || '',
          endDate: project.endDate || ''
        }));
      }
      
      // 填充技能
      if (backendData.skillList && Array.isArray(backendData.skillList)) {
        resumeInfo.skillsWithLevel = backendData.skillList.map((skill, index) => ({
          name: skill.name || '',
          level: skill.level || 1
        }));
      } else if (backendData.skills && Array.isArray(backendData.skills)) {
        resumeInfo.skillsWithLevel = backendData.skills.map((skill, index) => ({
          name: skill.name || '',
          level: skill.level || 1
        }));
      }
      
      // 填充兴趣爱好
      if (backendData.interests) {
        if (Array.isArray(backendData.interests)) {
          resumeInfo.interestsText = backendData.interests.join('\n');
        } else {
          resumeInfo.interestsText = backendData.interests || '';
        }
      }
      
      // 填充自我评价
      if (backendData.selfEvaluation) {
        resumeInfo.selfEvaluation = backendData.selfEvaluation;
      }
      
      // 设置模板ID，优先使用传入的templateId
      resumeInfo.templateId = templateId || backendData.templateId || resumeInfo.templateId;
      
      // 更新页面数据
      this.setData({
        resumeInfo: resumeInfo,
        hasLoadedFromBackend: true  // 标记已从后端成功加载数据
      });
      
      console.log('简历数据已成功填充到页面');
      
      // 如果后端有数据，就不再使用本地存储的数据
      return true;
    } catch (error) {
      console.error('填充简历数据失败:', error);
      return false;
    }
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

  // 保存个人信息（只保存可编辑字段）
  savePersonalInfo(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    
    // 只允许修改特定字段
    const editableFields = ['jobTitle', 'expectedSalary', 'startTime'];
    if (editableFields.includes(field)) {
      this.setData({
        [`resumeInfo.personalInfo.${field}`]: value
      });
      
      // 即时保存数据到本地存储
      wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
    }
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



  // 添加新技能
  onAddSkill: function() {
    const skills = this.data.resumeInfo.skillsWithLevel;
    skills.push({ name: '', level: 1 });
    this.setData({
      'resumeInfo.skillsWithLevel': skills
    });
    // 保存到本地存储
    wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
  },
  
  // 删除技能
  onDeleteSkill: function(e) {
    const index = e.currentTarget.dataset.index;
    const skills = this.data.resumeInfo.skillsWithLevel;
    if (skills.length > 1) {
      skills.splice(index, 1);
      this.setData({
        'resumeInfo.skillsWithLevel': skills
      });
      // 保存到本地存储
      wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
    } else {
      wx.showToast({
        title: '至少保留一项技能',
        icon: 'none'
      });
    }
  },
  
  // 修改技能名称
  onSkillNameInput: function(e) {
    const index = e.currentTarget.dataset.index;
    const value = e.detail.value;
    const skills = this.data.resumeInfo.skillsWithLevel;
    skills[index].name = value;
    this.setData({
      'resumeInfo.skillsWithLevel': skills
    });
    // 保存到本地存储
    wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
  },
  
  // 修改技能等级
  onSkillLevelChange: function(e) {
    const skillIndex = e.currentTarget.dataset.skillIndex;
    const level = parseInt(e.currentTarget.dataset.level);
    const skills = [...this.data.resumeInfo.skillsWithLevel];
    skills[skillIndex].level = level;
    this.setData({
      'resumeInfo.skillsWithLevel': skills
    });
    // 保存到本地存储
    wx.setStorageSync('tempResumeInfo', this.data.resumeInfo);
  },
  
  // 处理兴趣爱好输入
  onInterestsInput: function(e) {
    this.setData({
      'resumeInfo.interestsText': e.detail.value
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

  // 处理项目经验输入
  onProjectInput(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    
    try {
      // 确保projectExperienceList存在且为数组，并且索引有效
      if (!this.data.resumeInfo.projectExperienceList || !Array.isArray(this.data.resumeInfo.projectExperienceList) || index < 0 || index >= this.data.resumeInfo.projectExperienceList.length) {
        console.error('项目经验数据异常或索引无效');
        return;
      }
      
      // 创建新的projectExperienceList数组副本进行更新
      const updatedProjectExperienceList = [...this.data.resumeInfo.projectExperienceList];
      if (!updatedProjectExperienceList[index]) {
        updatedProjectExperienceList[index] = {};
      }
      updatedProjectExperienceList[index][field] = value;
      
      this.setData({
        'resumeInfo.projectExperienceList': updatedProjectExperienceList
      });
      
      // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
      const resumeInfoString = JSON.stringify(this.data.resumeInfo);
      wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
      console.log('项目经验数据更新并保存:', {index, field, value});
    } catch (error) {
      console.error('处理项目经验输入异常:', error);
    }
  },

  // 添加项目经验
  addProjectExperience: function() {
    try {
      // 确保projectExperienceList存在且为数组
      if (!this.data.resumeInfo.projectExperienceList || !Array.isArray(this.data.resumeInfo.projectExperienceList)) {
        this.setData({
          'resumeInfo.projectExperienceList': []
        });
      }
      const projectExperienceList = [...this.data.resumeInfo.projectExperienceList];
      projectExperienceList.push({
        id: Date.now(), // 添加唯一ID
        name: '',
        description: '',
        startDate: '',
        endDate: ''
      });
      this.setData({
        'resumeInfo.projectExperienceList': projectExperienceList
      });
      
      // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
      const resumeInfoString = JSON.stringify(this.data.resumeInfo);
      wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
      console.log('添加项目经验成功，当前数量:', projectExperienceList.length);
    } catch (error) {
      console.error('添加项目经验异常:', error);
      wx.showToast({
        title: '添加失败，请重试',
        icon: 'none'
      });
    }
  },
  
  // 删除项目经验
  removeProjectExperience: function(e) {
    const index = e.currentTarget.dataset.index;
    
    try {
      // 确保projectExperienceList存在且为数组
      if (!this.data.resumeInfo.projectExperienceList || !Array.isArray(this.data.resumeInfo.projectExperienceList) || this.data.resumeInfo.projectExperienceList.length === 0) {
        wx.showToast({
          title: '数据异常',
          icon: 'none'
        });
        return;
      }
      
      const projectExperienceList = [...this.data.resumeInfo.projectExperienceList];
      
      if (projectExperienceList.length > 1) {
        projectExperienceList.splice(index, 1);
        this.setData({
          'resumeInfo.projectExperienceList': projectExperienceList
        });
        
        // 即时保存数据到本地存储，使用JSON序列化确保复杂数据结构正确存储
        const resumeInfoString = JSON.stringify(this.data.resumeInfo);
        wx.setStorageSync('tempResumeInfo', JSON.parse(resumeInfoString));
        console.log('删除项目经验成功，当前数量:', projectExperienceList.length);
      } else {
        wx.showToast({
          title: '至少保留一个项目经验',
          icon: 'none'
        });
      }
    } catch (error) {
      console.error('删除项目经验异常:', error);
      wx.showToast({
        title: '删除失败，请重试',
        icon: 'none'
      });
    }
  },

  // 保存简历信息到本地存储
  saveResumeToStorage: function(resumeInfo) {
    try {
      // 确保必要的数据结构存在
      if (!resumeInfo.education) {
        resumeInfo.education = [];
      }
      if (!resumeInfo.workExperience) {
        resumeInfo.workExperience = [];
      }
      if (!resumeInfo.projectExperienceList) {
        resumeInfo.projectExperienceList = [];
      }
      if (!resumeInfo.skillsWithLevel) {
        resumeInfo.skillsWithLevel = [];
      }
      // 确保兴趣爱好和自我评价字段存在
      if (resumeInfo.interestsText === undefined) {
        resumeInfo.interestsText = '';
      }
      if (resumeInfo.selfEvaluation === undefined) {
        resumeInfo.selfEvaluation = '';
      }
      
      // 保存到本地存储
      wx.setStorageSync('tempResumeInfo', resumeInfo);
      
      // 同时保存到resumeData，便于其他页面使用
      const resumeData = {
        templateId: resumeInfo.templateId || 'template-one',
        data: {
          personalInfo: resumeInfo.personalInfo,
          education: resumeInfo.education,
          workExperience: resumeInfo.workExperience,
          projectExperienceList: resumeInfo.projectExperienceList,
          skillsWithLevel: resumeInfo.skillsWithLevel,
          interests: resumeInfo.interestsText ? resumeInfo.interestsText.split('\n').filter(h => h.trim()) : [],
          selfEvaluation: resumeInfo.selfEvaluation
        }
      };
      
      wx.setStorageSync('resumeData', resumeData);
      
      return true;
    } catch (error) {
      console.error('保存简历信息失败:', error);
      return false;
    }
  },
  
  // 将前端简历数据转换为后端需要的结构化格式
  convertToBackendFormat: function(resumeInfo) {
    // 将兴趣爱好文本转换为数组，使用换行符分割，与onLoad和saveResume保持一致
    const interests = resumeInfo.interestsText ? resumeInfo.interestsText.split('\n').filter(h => h.trim()) : [];
    
    return {
      // 移除个人信息相关字段，这些信息现在存储在User表中
      // 只保留简历特有的字段
      personalInfo: {
        jobTitle: resumeInfo.personalInfo.jobTitle,
        expectedSalary: resumeInfo.personalInfo.expectedSalary,
        startTime: resumeInfo.personalInfo.startTime,
        jobTypeId: resumeInfo.personalInfo.jobTypeId,
        interests: interests,
        selfEvaluation: resumeInfo.personalInfo.selfEvaluation || resumeInfo.selfEvaluation
      },
      // 移除contact字段，这些信息现在存储在User表中
      
      education: resumeInfo.education.map((edu, index) => ({
        school: edu.school,
        degree: edu.degree,
        major: edu.major,
        startDate: edu.startDate,
        endDate: edu.endDate,
        description: edu.description || '',
        gpa: edu.gpa || '',
        orderIndex: index
      })),
      workExperience: resumeInfo.workExperience.map((work, index) => ({
        companyName: work.company,
        positionName: work.position,
        startDate: work.startDate,
        endDate: work.endDate,
        description: work.description,
        orderIndex: index
      })),
      projects: resumeInfo.projectExperienceList.map((project, index) => ({
        projectName: project.name,
        role: project.role || '',
        startDate: project.startDate,
        endDate: project.endDate,
        description: project.description,
        achievements: project.achievements || '',
        technologies: project.technologies || '',
        orderIndex: index
      })),
      // 使用skillsWithLevel替换skills字段
      skillsWithLevel: resumeInfo.skillsWithLevel.map((skill, index) => ({
        name: skill.name,
        level: skill.level || 1,
        category: skill.category || '专业技能',
        orderIndex: index
      })),

    };
  },
  
  // 保存简历数据到后端（使用云托管方式）
  saveResumeToBackend: function(resumeId, userId, resumeData) {
    console.log('调用云托管接口保存简历数据:', { resumeId, userId });
    return new Promise((resolve, reject) => {
      // 判断是创建新简历还是更新现有简历
      if (resumeId === 'new' || !resumeId) {
        // 创建新简历
        request.post('/resume/', resumeData)
          .then(res => {
            if (res && res.success) {
              resolve(res);
            } else {
              // 处理后端返回的错误信息
              const errorMessage = res && res.message || '保存失败，请检查简历信息是否完整';
              wx.showToast({
                title: errorMessage,
                icon: 'none',
                duration: 3000
              });
              reject(new Error(errorMessage));
            }
          })
          .catch(err => {
            // 捕获网络错误或其他错误
            let errorMessage = '网络错误，请稍后重试';
            // 尝试从错误响应中获取后端错误信息
            if (err.response && err.response.data && err.response.data.message) {
              errorMessage = err.response.data.message;
            } else if (err.message) {
              errorMessage = err.message;
            }
            
            wx.showToast({
              title: errorMessage,
              icon: 'none',
              duration: 3000
            });
            reject(err);
          });
      } else {
        // 更新现有简历
        request.put(`/resume/${resumeId}`, resumeData)
          .then(res => {
            if (res && res.success) {
              resolve(res);
            } else {
              // 处理后端返回的错误信息
              const errorMessage = res && res.message || '保存失败，请检查简历信息是否完整';
              wx.showToast({
                title: errorMessage,
                icon: 'none',
                duration: 3000
              });
              reject(new Error(errorMessage));
            }
          })
          .catch(err => {
            // 捕获网络错误或其他错误
            let errorMessage = '网络错误，请稍后重试';
            // 尝试从错误响应中获取后端错误信息
            if (err.response && err.response.data && err.response.data.message) {
              errorMessage = err.response.data.message;
            } else if (err.message) {
              errorMessage = err.message;
            }
            
            wx.showToast({
              title: errorMessage,
              icon: 'none',
              duration: 3000
            });
            reject(err);
          });
      }
    });
  },

  // 保存简历
  saveResume: function() {
    try {
      const { resumeInfo } = this.data;
      
      // 检查是否有模板ID
      if (!resumeInfo.templateId) {
        wx.showToast({
          title: '请先选择简历模板',
          icon: 'none'
        });
        return;
      }
      const personalInfo = resumeInfo.personalInfo
      // 2. 期望薪资必填校验
      if (!personalInfo.expectedSalary || personalInfo.expectedSalary.trim() === '') {
        wx.showToast({
          title: '请填写期望薪资',
          icon: 'none'
        });
        return;
      }
      
      // 3. 到岗时间必填校验
      if (!personalInfo.startTime || personalInfo.startTime.trim() === '') {
        wx.showToast({
          title: '请填写到岗时间',
          icon: 'none'
        });
        return;
      }
      
      // 4. 职位必填校验
      if (!personalInfo.jobTitle || personalInfo.jobTitle.trim() === '') {
        wx.showToast({
          title: '请填写职位',
          icon: 'none'
        });
        return;
      }
      
      // 5. 教育经历最少一个完整项校验
      const educationList = resumeInfo.education || [];
      const hasValidEducation = educationList.some(edu => 
        edu.school && edu.school.trim() !== '' &&
        edu.major && edu.major.trim() !== '' &&
        edu.degree && edu.degree.trim() !== '' &&
        edu.startDate && edu.startDate.trim() !== '' &&
        edu.endDate && edu.endDate.trim() !== ''
      );
      if (!hasValidEducation) {
        wx.showToast({
          title: '请至少填写一个完整的教育经历（学校、专业、学历、起止时间都不能为空）',
          icon: 'none',
          duration: 3000
        });
        return;
      }
      
      // 6. 工作经历最少一个完整项校验
      const workExperienceList = resumeInfo.workExperience || [];
      const hasValidWorkExperience = workExperienceList.some(work => 
        work.company && work.company.trim() !== '' &&
        work.position && work.position.trim() !== '' &&
        work.startDate && work.startDate.trim() !== '' &&
        work.endDate && work.endDate.trim() !== '' &&
        work.description && work.description.trim() !== ''
      );
      if (!hasValidWorkExperience) {
        wx.showToast({
          title: '请至少填写一个完整的工作经历（公司、职位、起止时间，描述都不能为空）',
          icon: 'none',
          duration: 3000
        });
        return;
      }
      
      // 7. 项目经验最少一个完整项校验
      const projectExperienceList = resumeInfo.projectExperienceList || [];
      const hasValidProjectExperience = projectExperienceList.some(project => 
        project.name && project.name.trim() !== '' &&
        project.startDate && project.startDate.trim() !== '' &&
        project.endDate && project.endDate.trim() !== '' && 
        project.description && project.description.trim() !== ''
      );
      if (!hasValidProjectExperience) {
        wx.showToast({
          title: '请至少填写一个完整的项目经验（项目名称、起止时间、描述都不能为空）',
          icon: 'none',
          duration: 3000
        });
        return;
      }
      
      // 8. 专业技能最少填写1个校验
      const skillsWithLevel = resumeInfo.skillsWithLevel || [];
      const validSkills = skillsWithLevel.filter(skill => 
        skill.name && skill.name.trim() !== ''
      );
      if (validSkills.length === 0) {
        wx.showToast({
          title: '请至少填写1个专业技能',
          icon: 'none'
        });
        return;
      }
      
      // 9. 兴趣爱好必填校验
      if (!resumeInfo.interestsText || resumeInfo.interestsText.trim() === '') {
        wx.showToast({
          title: '请填写兴趣爱好',
          icon: 'none'
        });
        return;
      }
      
      // 10. 自我评价必填校验
      const selfEvaluation = resumeInfo.personalInfo.selfEvaluation || resumeInfo.selfEvaluation;
      if (!selfEvaluation || selfEvaluation.trim() === '') {
        wx.showToast({
          title: '请填写自我评价',
          icon: 'none'
        });
        return;
      }

      wx.showLoading({
        title: '保存并生成简历中...',
      });

      // 保存到本地存储
      const saved = this.saveResumeToStorage(resumeInfo);
      
      if (saved) {
        // 准备简历数据，包含模板ID
        // 处理兴趣爱好，将文本转换为数组，使用换行符分割，与onLoad保持一致
        const interests = resumeInfo.interestsText ? 
          resumeInfo.interestsText.split('\n').filter(hobby => hobby.trim()) : [];
        
        // 处理技能评分数据，过滤掉空技能名称
        const skillsWithLevel = resumeInfo.skillsWithLevel.filter(skill => skill.name.trim() !== '');
        
        // 只使用skillsWithLevel，不生成冗余的skills数组
        
        // 处理项目经验数据，过滤掉空项目名称
        const projectExperienceList = resumeInfo.projectExperienceList.filter(project => project.name.trim() !== '');
        
        // 确保personalInfo对象存在并包含interests和selfEvaluation
        const personalInfo = {
          ...resumeInfo.personalInfo,
          interests: interests,
          selfEvaluation: resumeInfo.personalInfo.selfEvaluation || resumeInfo.selfEvaluation
        };
        
        const resumeData = {
          isAiGenerated: false,
          templateId: resumeInfo.templateId,
          data: {
            title: resumeInfo.title,
            personalInfo: personalInfo,
            education: resumeInfo.education,
            workExperience: resumeInfo.workExperience,
            projectExperienceList: projectExperienceList,
            skillsWithLevel: skillsWithLevel
          }
        };
        
        // 保存到本地存储
        wx.setStorageSync('resumeData', resumeData);
        
        // 获取resumeId和userId，加强日志记录
        const resumeId = resumeInfo.id || wx.getStorageSync('currentResumeId');
        const userInfoStr = wx.getStorageSync('userInfo');
        const userInfo = JSON.parse(userInfoStr);
        // 同时检查id和userId字段，确保能正确获取用户ID
        const userId = userInfo ? (userInfo.id || userInfo.userId) : null;
        
        console.log('准备保存到后端:', { resumeId, userId, hasUserInfo: !!userInfo, hasTemplateId: !!resumeInfo.templateId });
        
        // 即使没有resumeId也尝试调用后端，让后端处理新建或更新逻辑
        if (userId) { // 至少需要userId才能保存到后端
          // 转换为后端需要的格式
          const backendData = this.convertToBackendFormat(resumeInfo);
          
          // 保存到后端，优化Promise处理和错误信息展示
          this.saveResumeToBackend(resumeId || 'new', userId, backendData)
            .then((response) => {
              console.log('后端保存成功，响应数据:', response);
              wx.hideLoading();
              wx.showToast({
                title: '保存成功并同步到云端',
                icon: 'success',
                duration: 2000
              });
              
              // 如果是新建简历，保存返回的resumeId
              if (response && response.resumeId && !resumeId) {
                console.log('保存新简历ID:', response.resumeId);
                this.setData({
                  'resumeInfo.id': response.resumeId
                });
                wx.setStorageSync('currentResumeId', response.resumeId);
              }
              
              // 跳转到简历预览页面，传递模板ID和简历ID
              setTimeout(() => {
                const resumeId = response.resumeId || this.data.resumeInfo.id;
                wx.navigateTo({
                  url: `/pages/template/preview/preview?templateId=${resumeInfo.templateId}&resumeId=${resumeId}`
                });
              }, 1500);
            })
            .catch((error) => {
              console.error('后端保存失败:', error);
              wx.hideLoading();
              // 本地保存成功但云端同步失败，仍然提示保存成功
              wx.showToast({
                title: '本地保存成功，但云端同步失败',
                icon: 'success',
                duration: 2000
              });
              
              // 跳转到简历预览页面，传递模板ID和简历ID
              setTimeout(() => {
                const resumeId = this.data.resumeInfo.id || wx.getStorageSync('currentResumeId');
                wx.navigateTo({
                  url: `/pages/template/preview/preview?templateId=${resumeInfo.templateId}&resumeId=${resumeId}`
                });
              }, 1500);
            });
        } else {
          console.log('没有用户ID，无法保存到后端');
          wx.hideLoading();
          wx.showToast({
            title: '保存成功',
            icon: 'success',
            duration: 2000
          });
          
          // 跳转到简历预览页面，传递模板ID和简历ID
              setTimeout(() => {
                const resumeId = this.data.resumeInfo.id || wx.getStorageSync('currentResumeId');
                wx.navigateTo({
                  url: `/pages/template/preview/preview?templateId=${resumeInfo.templateId}&resumeId=${resumeId}`
                });
              }, 1500);
        }
      } else {
        wx.hideLoading();
        wx.showToast({
          title: '保存失败',
          icon: 'none',
          duration: 2000
        });
      }
    } catch (error) {
      console.error('保存简历异常:', error);
      wx.hideLoading();
      wx.showToast({
        title: '保存失败，请重试',
        icon: 'none',
        duration: 2000
      });
    }
  }
});