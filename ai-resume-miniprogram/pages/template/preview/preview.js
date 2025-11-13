// preview.js - 动态模板预览版本
Page({
  
  data: {
    templateId: 'template-one',  // 默认模板ID
    templateName: '技术人才模板',  // 默认模板名称
    imagePath: '/images/template-one.png',  // 默认图片路径
    templateUpdateTime: new Date().getTime(),  // 用于触发视图更新的时间戳
    resumeData: null,  // 简历数据
    loading: true  // 加载状态
  },

  /**
   * 生命周期函数--监听页面加载
   * @param {Object} options - 页面参数，包含templateId和templateName
   */
  onLoad: function(options) {
    console.log('预览页面接收到的参数:', options);
    
    // 有效的模板ID列表 - 确保与实际存在的图片文件匹配
    const validTemplateIds = ['template-one', 'template-two', 'template-three', 'template-four', 'template-five', 'template-six'];
    
    // 处理传入的参数
    if (options && options.templateId) {
      let templateId = options.templateId;
      
      // 防御性检查：如果传入的templateId无效，使用默认值
      if (!validTemplateIds.includes(templateId)) {
        console.warn('无效的模板ID:', templateId, '，使用默认模板');
        templateId = 'template-one'; // 使用默认模板ID
      }
      
      // 设置图片路径和模板ID
      const imagePath = `/images/${templateId}.png`;
      this.setData({
        templateId: templateId,
        imagePath: imagePath
      });
      
      // 动态加载对应的样式文件
      this.loadTemplateStyles(templateId);
      console.log('已设置模板ID:', templateId, '图片路径:', imagePath);
    }
    
    // 处理模板名称
    if (options && options.templateName) {
      try {
        this.setData({
          templateName: decodeURIComponent(options.templateName)
        });
        console.log('已设置模板名称:', this.data.templateName);
      } catch (e) {
        console.error('解码模板名称失败:', e);
      }
    }
    
    // 加载简历数据
    this.loadResumeData();
  },
  
  /**
   * 加载简历数据
   */
  loadResumeData: function() {
    try {
      console.log('开始加载简历数据');
      
      // 从本地存储获取简历数据
      let storedData = wx.getStorageSync('resumeData');
      let rawData = null;
      
      if (storedData && storedData.data) {
        console.log('成功从本地存储加载简历数据');
        rawData = storedData.data;
      } else {
        console.warn('未找到简历数据，使用默认数据');
        // 如果没有数据，使用默认的示例数据
        rawData = {
          personalInfo: {
            name: '刘豆豆',
            jobTitle: 'UI/UX 设计师',
            phone: '138-0000-0000',
            email: 'lisi@qq.com',
            address: '上海',
            birthDate: '1999.09',
            expectedSalary: '20000',
            startTime: '一周内'
          },
          education: [
            {
              school: '中央美术学院',
              degree: '本科',
              major: '视觉传达设计专业',
              startDate: '2014年9月',
              endDate: '2018年7月'
            }
          ],
          workExperience: [
            {
              company: '腾讯网络科技有限公司',
              position: 'UI设计师',
              startDate: '2018年8月',
              endDate: '至今',
              description: '负责移动端产品界面设计、交互优化，提升用户体验。'
            },
            {
              company: '字节跳动',
              position: 'UI实习生',
              startDate: '2017年7月',
              endDate: '2017年12月',
              description: '参与小程序界面与交互设计，提升组件一致性与美观性。'
            }
          ],
          skillsWithLevel: [
            { name: 'Photoshop', level: 4 },
            { name: 'Sketch', level: 3 },
            { name: 'Figma', level: 3 }
          ],
          hobbies: ['设计', '交互'],
          selfEvaluation: '热爱设计与交互，注重用户体验与界面细节，具备团队协作与独立思考能力。'
        };
      }
      
      // 使用规范化函数处理数据，确保数据结构与模板一致
      const normalizedResumeData = this.normalizeResumeData(rawData);
      console.log('规范化后的简历数据:', normalizedResumeData);
      
      this.setData({
        resumeData: normalizedResumeData,
        loading: false
      });
    } catch (error) {
      console.error('加载简历数据失败:', error);
      this.setData({
        loading: false
      });
    }
  },

  /**
   * 使用当前模板
   * 将选择的模板信息保存到本地存储，并跳转到简历编辑页面
   */
  useTemplate: function() {
    const { templateId, templateName } = this.data;
    
    // 保存选择的模板信息
    wx.setStorageSync('tempResumeInfo', {
      templateId: templateId,
      templateName: templateName,
      title: '我的新简历',
      isAiGenerated: false
    });
    
    wx.showToast({
      title: '正在准备模板...',
      icon: 'success'
    });
    
    // 跳转到简历编辑页面
    setTimeout(() => {
      wx.navigateTo({
        url: `/pages/resume/edit/edit?templateId=${templateId}`
      });
    }, 1000);
  },
  
  /**
   * 动态加载模板样式文件
   * @param {string} templateId - 模板ID
   */
  /**
   * 加载指定模板的样式
   * @param {string} templateId - 模板ID
   */
  loadTemplateStyles: function(templateId) {
    try {
      console.log('正在为模板:', templateId, '加载对应样式');
      
      // 在小程序中，模板样式通过WXML中的模板导入和wxss的@import来控制
      // 由于我们已经在preview.wxss中导入了所有模板样式，这里只需确保模板切换时视图正确更新
      this.setData({
        templateUpdateTime: new Date().getTime() // 强制触发视图更新，确保样式正确应用
      });
      
      console.log('模板样式已准备就绪:', templateId);
    } catch (error) {
      console.error('加载模板样式失败:', error);
    }
  },

  /**
   * 返回模板列表
   */
  backToList: function() {
    wx.navigateBack();
  },
  
  /**
   * 生命周期函数--监听页面显示
   * 每次显示页面时重新加载数据，确保数据同步
   */
  onShow: function() {
    this.loadResumeData();
  },
  
  /**
   * 跳转到测试页面
   */
  /**
   * 跳转到测试页面（暂未实现）
   */
  goToTest: function() {
    // 调用测试函数
    this.testTemplateRendering();
  },

  /**
   * 切换模板
   * @param {Object} e - 事件对象，包含模板ID
   */
  switchTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id;
    const templateNameMap = {
      'template-one': '技术人才模板',
      'template-two': '简约风格模板',
      'template-three': '设计师模板'
    };
    
    this.setData({
      templateId: templateId,
      templateName: templateNameMap[templateId] || templateId,
      imagePath: `/images/${templateId}.png`,
      templateUpdateTime: new Date().getTime()
    });
    
    console.log('已切换到模板:', templateId);
  },
  
  /**
   * 下载PDF文件
   * 调用后端API生成并下载PDF
   */
  downloadPdf: function() {
    wx.showToast({
      title: '功能开发中',
      icon: 'none',
      duration: 2000
    });
  },
  
  /**
   * 测试模板渲染功能
   * 生成模拟数据并更新到页面，用于测试所有模板的渲染效果
   */
  testTemplateRendering: function() {
    // 生成测试数据
    const testData = {
      avatar: '/images/avatar.png',
      name: '张三',
      position: '高级前端工程师',
      phone: '13800138000',
      email: 'zhangsan@example.com',
      website: 'www.example.com',
      education: [
        {
          school: '北京大学',
          major: '计算机科学与技术',
          degree: '本科',
          startDate: '2014-09',
          endDate: '2018-06',
          description: 'GPA 3.8/4.0，获得优秀毕业生称号'
        },
        {
          school: '清华大学',
          major: '软件工程',
          degree: '硕士',
          startDate: '2018-09',
          endDate: '2021-06',
          description: '研究方向：Web前端技术，发表2篇学术论文'
        }
      ],
      workExperience: [
        {
          company: '腾讯科技',
          position: '前端开发工程师',
          startDate: '2021-07',
          endDate: '2023-03',
          description: '负责微信小程序开发，参与了3个核心项目，优化了用户体验和页面性能'
        },
        {
          company: '阿里巴巴',
          position: '高级前端工程师',
          startDate: '2023-04',
          endDate: '至今',
          description: '负责电商平台前端架构设计，带领5人团队，提升了页面加载速度30%'
        }
      ],
      projectExperience: [
        {
          name: '电商小程序',
          role: '技术负责人',
          startDate: '2022-01',
          endDate: '2022-06',
          description: '设计并开发了一款电商小程序，实现了商品展示、购物车、订单等核心功能',
          technologies: '微信小程序、JavaScript、WXML、WXSS'
        },
        {
          name: '企业管理系统',
          role: '前端架构师',
          startDate: '2023-05',
          endDate: '2023-12',
          description: '重构了企业管理系统前端架构，采用组件化开发，提高了开发效率和代码质量',
          technologies: 'React、TypeScript、Ant Design、Webpack'
        }
      ],
      skills: [
        { name: 'HTML/CSS', level: 95 },
        { name: 'JavaScript/TypeScript', level: 90 },
        { name: 'React/Vue', level: 85 },
        { name: '微信小程序', level: 80 },
        { name: 'Node.js', level: 75 },
        { name: 'Webpack/Vite', level: 70 }
      ],
      interests: '摄影、跑步、读书、旅游',
      selfAssessment: '拥有5年前端开发经验，精通现代前端技术栈，善于解决复杂问题，具有良好的团队协作精神和沟通能力。'
    };
    
    // 规范化测试数据
    const normalizedData = this.normalizeResumeData(testData);
    
    // 更新数据到页面
    this.setData({
      resumeData: normalizedData,
      loading: false
    });
    
    wx.showToast({
      title: '测试数据已加载',
      icon: 'success',
      duration: 2000
    });
  },
  
  /**
   * 监听页面显示时同步数据
   * 确保从其他页面返回时，数据是最新的
   */
  onShow: function() {
    // 每次显示页面时重新加载数据，确保数据同步
    console.log('页面显示，重新加载简历数据');
    this.loadResumeData();
  },
  
  /**
   * 规范化简历数据结构
   * 根据当前使用的模板ID动态调整数据字段名称，确保所有模板都能正确显示数据
   * @param {Object} rawData - 原始简历数据
   * @returns {Object} 规范化后的数据结构
   */
  normalizeResumeData: function(rawData) {
    if (!rawData) return null;
    
    const { templateId } = this.data;
    const normalizedData = {};
    
    // 基础个人信息 - 支持所有模板
    normalizedData.personalInfo = {
      name: rawData.personalInfo?.name || rawData.name || '',
      // 支持template-one的jobTitle和template-two的position
      jobTitle: rawData.personalInfo?.jobTitle || rawData.personalInfo?.position || rawData.position || '',
      position: rawData.personalInfo?.position || rawData.personalInfo?.jobTitle || rawData.jobTitle || '',
      avatar: rawData.personalInfo?.avatar || '',
      // 支持template-one直接在personalInfo中访问联系方式
      phone: rawData.personalInfo?.phone || rawData.contact?.phone || rawData.phone || '',
      email: rawData.personalInfo?.email || rawData.contact?.email || rawData.email || '',
      address: rawData.personalInfo?.address || rawData.contact?.address || rawData.address || '',
      birthDate: rawData.personalInfo?.birthDate || '',
      expectedSalary: rawData.personalInfo?.expectedSalary || rawData.salary || '',
      // 支持不同的入职时间字段名
      startTime: rawData.personalInfo?.startTime || rawData.personalInfo?.startDate || '',
      availableTime: rawData.personalInfo?.availableTime || rawData.personalInfo?.startTime || rawData.personalInfo?.startDate || '',
      // 支持template-two中的兴趣爱好和自我评价字段
      interests: rawData.personalInfo?.interests || rawData.hobbies || rawData.interests || '',
      selfEvaluation: rawData.personalInfo?.selfEvaluation || rawData.selfEvaluation || rawData.summary || ''
    };
    
    // 根据模板ID添加特定字段
    switch (templateId) {
      case 'template-two':
      case 'template-three':
        // template-two和template-three特定字段
        normalizedData.contactInfo = {
          phone: rawData.contactInfo?.phone || rawData.personalInfo?.phone || rawData.contact?.phone || rawData.phone || '',
          email: rawData.contactInfo?.email || rawData.personalInfo?.email || rawData.contact?.email || rawData.email || '',
          address: rawData.contactInfo?.address || rawData.personalInfo?.address || rawData.contact?.address || rawData.address || ''
        };
        
        // template-two和template-three的教育经历格式
        const eduData = rawData.educationList || rawData.education || [];
        normalizedData.educationList = eduData.map(item => ({
          schoolName: item.schoolName || item.school || '',
          degree: item.degree || '',
          startTime: item.startTime || item.startDate || '',
          endTime: item.endTime || item.endDate || '',
          description: item.description || item.major || ''
        }));
        
        // template-two和template-three的工作经历格式
        const workData = rawData.workExperienceList || rawData.workExperience || rawData.work || [];
        normalizedData.workExperienceList = workData.map(item => ({
          companyName: item.companyName || item.company || '',
          position: item.position || '',
          startTime: item.startTime || item.startDate || '',
          endTime: item.endTime || item.endDate || '',
          description: item.description || item.details || ''
        }));
        
        // template-two和template-three的项目经验格式
        const projectData = rawData.projectExperienceList || rawData.projects || [];
        normalizedData.projectExperienceList = projectData.map(item => ({
          projectName: item.projectName || item.name || '',
          startTime: item.startTime || item.startDate || '',
          endTime: item.endTime || item.endDate || '',
          description: item.description || item.details || ''
        }));
        break;
        
      case 'template-four':
      case 'template-five':
      case 'template-six':
        // template-four/five/six特定格式 - 直接在resumeData根级别设置字段
        normalizedData.avatar = rawData.personalInfo?.avatar || rawData.avatar || '/images/avatar.jpg';
        normalizedData.name = rawData.personalInfo?.name || rawData.name || '';
        normalizedData.title = rawData.personalInfo?.jobTitle || rawData.personalInfo?.position || rawData.position || rawData.title || '';
        normalizedData.phone = rawData.personalInfo?.phone || rawData.contact?.phone || rawData.phone || '';
        normalizedData.email = rawData.personalInfo?.email || rawData.contact?.email || rawData.email || '';
        normalizedData.address = rawData.personalInfo?.address || rawData.contact?.address || rawData.address || '';
        normalizedData.birth = rawData.personalInfo?.birthDate || rawData.birth || '';
        normalizedData.salary = rawData.personalInfo?.expectedSalary || rawData.salary || '';
        normalizedData.entryTime = rawData.personalInfo?.startTime || rawData.personalInfo?.startDate || rawData.entryTime || '';
        
        // template-four/five/six的技能格式
        const skillsData = rawData.skills || rawData.skillsWithLevel || [];
        normalizedData.skills = skillsData.map(item => ({
          name: item.name || item.skillName || '',
          level: item.level || item.proficiency || 80 // 默认80%熟练度
        }));
        
        // template-four/five/six的教育经历格式
        const eduList = rawData.education || [];
        normalizedData.education = eduList.map(item => ({
          school: item.school || '',
          major: item.major || '',
          degree: item.degree || '',
          startDate: item.startDate || '',
          endDate: item.endDate || '',
          description: item.description || ''
        }));
        
        // template-four/five/six的工作经历格式
        const workList = rawData.workExperience || rawData.work || [];
        normalizedData.workExperience = workList.map(item => ({
          company: item.company || '',
          position: item.position || '',
          startDate: item.startDate || '',
          endDate: item.endDate || '',
          description: item.description || ''
        }));
        break;
        
      case 'template-one':
      default:
        // template-one和其他模板的标准格式
        normalizedData.education = this.normalizeEducationData(rawData);
        normalizedData.workExperience = this.normalizeWorkExperience(rawData);
        normalizedData.skillsWithLevel = this.normalizeSkillsData(rawData);
        break;
    }
    
    // 通用字段 - 确保所有模板都能访问
    normalizedData.education = normalizedData.education || this.normalizeEducationData(rawData);
    normalizedData.workExperience = normalizedData.workExperience || this.normalizeWorkExperience(rawData);
    normalizedData.skillsWithLevel = normalizedData.skillsWithLevel || this.normalizeSkillsData(rawData);
    normalizedData.skills = normalizedData.skills || this.normalizeSkillsData(rawData);
    normalizedData.hobbies = rawData.hobbies || rawData.interests || [];
    normalizedData.selfEvaluation = rawData.selfEvaluation || rawData.summary || '';
    normalizedData.contact = {
      phone: rawData.contact?.phone || rawData.personalInfo?.phone || rawData.phone || '',
      email: rawData.contact?.email || rawData.personalInfo?.email || rawData.email || '',
      address: rawData.contact?.address || rawData.personalInfo?.address || rawData.address || ''
    };
    normalizedData.work = this.normalizeWorkExperience(rawData);
    normalizedData.projects = this.normalizeProjectExperience(rawData);
    
    // 为template-four/five/six确保根级别字段存在
    normalizedData.avatar = normalizedData.avatar || rawData.personalInfo?.avatar || '/images/avatar.jpg';
    normalizedData.name = normalizedData.name || rawData.personalInfo?.name || rawData.name || '';
    normalizedData.title = normalizedData.title || rawData.personalInfo?.jobTitle || rawData.personalInfo?.position || rawData.position || '';
    normalizedData.phone = normalizedData.phone || rawData.personalInfo?.phone || rawData.contact?.phone || rawData.phone || '';
    normalizedData.email = normalizedData.email || rawData.personalInfo?.email || rawData.contact?.email || rawData.email || '';
    normalizedData.address = normalizedData.address || rawData.personalInfo?.address || rawData.contact?.address || rawData.address || '';
    normalizedData.birth = normalizedData.birth || rawData.personalInfo?.birthDate || '';
    normalizedData.salary = normalizedData.salary || rawData.personalInfo?.expectedSalary || rawData.salary || '';
    normalizedData.entryTime = normalizedData.entryTime || rawData.personalInfo?.startTime || rawData.personalInfo?.startDate || '';
    
    return normalizedData;
  },
  
  /**
   * 规范化教育经历数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的教育经历数组
   */
  normalizeEducationData: function(rawData) {
    const education = rawData.education || [];
    return education.map(item => ({
      school: item.school || '',
      degree: item.degree || '',
      major: item.major || '',
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      description: item.description || item.details || ''
    }));
  },
  
  /**
   * 规范化工作经历数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的工作经历数组
   */
  normalizeWorkExperience: function(rawData) {
    const work = rawData.work || rawData.workExperience || [];
    return work.map(item => ({
      company: item.company || '',
      position: item.position || '',
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      description: item.description || item.details || ''
    }));
  },
  
  /**
   * 规范化项目经验数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的项目经验数组
   */
  normalizeProjectExperience: function(rawData) {
    const projects = rawData.projects || [];
    return projects.map(item => ({
      name: item.name || item.projectName || '',
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      description: item.description || item.details || ''
    }));
  },
  
  /**
   * 规范化技能数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的技能数组
   */
  normalizeSkillsData: function(rawData) {
    const skills = rawData.skills || rawData.skillsWithLevel || [];
    return skills.map(item => ({
      name: item.name || item.skillName || '',
      level: item.level || item.proficiency || 50 // 默认50%熟练度
    }));
  }
})