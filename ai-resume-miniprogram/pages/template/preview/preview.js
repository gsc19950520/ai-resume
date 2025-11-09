// preview.js
const app = getApp()

// 手动定义__route__变量，解决框架错误
const __route__ = 'pages/template/preview/preview';

Page({
  
  data: {
    templateId: '',
    templateName: '',
    template: null,
    previewData: {
      // 模拟数据用于模板预览
      personalInfo: {
        name: '张三',
        jobTitle: '高级前端开发工程师',
        phone: '138****1234',
        email: 'zhangsan@example.com',
        address: '上海市浦东新区',
        summary: '5年前端开发经验，精通各种前端技术栈，擅长大型项目架构设计和性能优化。'
      },
      education: [
        {
          school: '北京大学',
          major: '计算机科学与技术',
          degree: '本科',
          startDate: '2014-09',
          endDate: '2018-06',
          achievements: 'GPA 3.8/4.0，获得优秀毕业生称号，参与国家级科研项目'
        },
        {
          school: '清华大学',
          major: '软件工程',
          degree: '硕士',
          startDate: '2018-09',
          endDate: '2021-06',
          achievements: '获得国家奖学金，发表3篇学术论文'
        }
      ],
      workExperience: [
        {
          company: '科技有限公司',
          position: '前端开发工程师',
          startDate: '2018-07',
          endDate: '2022-03',
          description: '负责公司核心产品的前端开发，参与需求分析和技术方案制定，优化用户体验和性能。主导重构项目，将页面加载速度提升40%。'
        },
        {
          company: '互联网创新公司',
          position: '高级前端开发工程师',
          startDate: '2022-04',
          endDate: '至今',
          description: '负责技术团队的管理和前端架构设计，主导多个重要项目的开发和上线。建立前端工程化体系，提高团队开发效率60%。'
        }
      ],
      projects: [
        {
          name: '企业管理系统',
          role: '技术负责人',
          startDate: '2020-01',
          endDate: '2020-08',
          description: '设计并实现了基于React的企业管理系统前端架构，提升了开发效率30%。'
        }
      ],
      skills: ['JavaScript', 'React', 'Vue', 'TypeScript', 'Webpack', 'Node.js', 'CSS3', 'HTML5', 'Git', '前端架构'],
      selfEvaluation: '5年前端开发经验，精通各种前端技术栈，具有良好的代码规范和团队协作能力。热爱技术，持续学习，追求卓越。善于解决复杂问题，具备良好的沟通能力和项目管理经验。',
      // 新增板块数据
      certifications: [
        { name: 'AWS认证解决方案架构师', date: '2022-05' },
        { name: 'Google认证前端开发专家', date: '2021-12' },
        { name: '微软MCSD认证', date: '2020-08' }
      ],
      languages: [
        { name: '英语', proficiency: '专业熟练（听说读写流利）' },
        { name: '日语', proficiency: '基础水平' }
      ],
      technicalStack: [
        { category: '前端框架', skills: 'React, Vue, Angular, Next.js' },
        { category: '编程语言', skills: 'JavaScript, TypeScript, Python' },
        { category: '开发工具', skills: 'Webpack, Git, Docker, Jest' },
        { category: '数据库', skills: 'MySQL, MongoDB, Redis' }
      ],
      achievements: [
        '主导的项目获得公司年度最佳产品奖',
        '优化系统性能，将页面加载时间减少60%',
        '培训新员工10名，建立前端技术分享机制',
        '贡献开源项目3个，获得社区好评'
      ],
      creativeApproach: [
        '注重用户体验设计，追求简洁直观的界面',
        '善于将复杂功能简化为易用的交互',
        '结合数据分析优化设计决策',
        '不断探索新技术应用于创意实现'
      ],
      interests: [
        '开源项目贡献',
        '技术博客写作',
        '户外运动（登山、骑行）',
        '摄影艺术'
      ],
      researchInterests: [
        '前端性能优化与工程化',
        'WebAssembly在前端的应用',
        '人工智能在前端开发中的应用',
        '跨平台开发技术研究'
      ],
      publications: [
        {
          title: '基于React的大型应用架构设计研究',
          journal: '计算机科学与应用',
          year: '2021',
          authors: '张三，李四'
        },
        {
          title: '前端性能优化策略与实践',
          conference: '全国前端技术大会',
          year: '2020',
          authors: '张三'
        }
      ]
    }
  },

  onLoad: function(options) {
    const { templateId, templateName } = options
    // 解码templateName参数，避免乱码问题
    const decodedTemplateName = decodeURIComponent(templateName || '')
    this.setData({
      templateId,
      templateName: decodedTemplateName
    })
    
    // 根据模板ID获取模板配置
    this.getTemplateConfig(templateId)
  },

  // 获取模板配置
  getTemplateConfig: function(templateId) {
    // 根据不同模板ID返回不同的配置
    const templates = {
      '1': {
        id: 'basic',
        name: '简约专业模板',
        type: 'professional',
        styleClass: 'template-professional',
        colors: {
          primary: '#2563eb',
          secondary: '#93c5fd',
          accent: '#1d4ed8',
          background: '#ffffff',
          border: '#e3f2fd'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: false,
          skills: true,
          selfEvaluation: true,
          certifications: true, // 新增证书板块
          languages: true // 新增语言技能板块
        },
        layout: {
          type: 'enhancedTwoColumn',
          personalInfoPosition: 'left',
          skillsPosition: 'left',
          certificationsPosition: 'left',
          languagesPosition: 'left',
          columnRatio: {
            left: 0.32,
            right: 0.68
          }
        }
      },
      '2': {
        id: 'technical',
        name: '技术人才模板',
        type: 'technical',
        styleClass: 'template-technical',
        colors: {
          primary: '#059669',
          secondary: '#6ee7b7',
          accent: '#065f46',
          background: '#f8fafc',
          border: '#d1fae5'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: false,
          skills: true,
          selfEvaluation: true,
          technicalStack: true, // 新增技术栈板块
          achievements: true // 新增成就板块
        },
        layout: {
          type: 'skillsFocusThreeColumn',
          personalInfoPosition: 'top',
          skillsPosition: 'center',
          technicalStackPosition: 'center',
          achievementsPosition: 'right',
          workExperiencePosition: 'fullWidth',
          columnRatio: {
            top: 0.25,
            left: 0.3,
            center: 0.4,
            right: 0.3
          }
        }
      },
      '3': {
        id: 'creative',
        name: '创意设计模板',
        type: 'creative',
        styleClass: 'template-creative',
        colors: {
          primary: '#dc2626',
          secondary: '#fca5a5',
          accent: '#b91c1c',
          background: '#ffffff',
          border: '#fca5a5'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: false,
          skills: true,
          selfEvaluation: true,
          creativeApproach: true, // 新增创意方法板块
          interests: true // 新增兴趣爱好板块
        },
        layout: {
          type: 'verticalLayersCreative',
          personalInfoPosition: 'hero',
          skillsPosition: 'sidebar',
          creativeApproachPosition: 'sidebar',
          interestsPosition: 'sidebar',
          contentPosition: 'main',
          columnRatio: {
            hero: 0.3,
            sidebar: 0.25,
            main: 0.75
          }
        }
      },
      '4': {
        id: 'academic',
        name: '学术研究模板',
        type: 'academic',
        styleClass: 'template-academic',
        colors: {
          primary: '#6d28d9',
          secondary: '#c4b5fd',
          accent: '#5b21b6',
          background: '#f8fafc',
          border: '#ddd6fe'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: false,
          skills: true,
          selfEvaluation: true,
          researchInterests: true, // 新增研究兴趣板块
          publications: true // 新增发表论文板块
        },
        layout: {
          type: 'academicGrid',
          personalInfoPosition: 'header',
          educationPosition: 'left',
          researchInterestsPosition: 'left',
          publicationsPosition: 'left',
          workExperiencePosition: 'right',
          skillsPosition: 'right',
          selfEvaluationPosition: 'right',
          columnRatio: {
            header: 0.15,
            left: 0.4,
            right: 0.6
          }
        }
      }
    }

    this.setData({
      template: templates[templateId] || templates['1'] // 默认使用基础模板
    })
  },

  // 使用模板
  useTemplate: function() {
    const { templateId, templateName, template } = this.data
    
    // 确保模板包含完整的布局配置
    const templateWithLayout = {
      ...template,
      layout: template.layout || {
        type: 'twoColumnStandard',
        personalInfoPosition: 'left',
        skillsPosition: 'right',
        columnRatio: {
          left: 0.25,
          right: 0.75
        }
      }
    };
    
    // 保存选择的模板信息，使用与简历编辑页面一致的存储键
    wx.setStorageSync('tempResumeInfo', {
      templateId: templateId,
      templateName: templateName,
      title: '我的新简历',
      isAiGenerated: false,
      layout: templateWithLayout.layout // 保存布局配置
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
  
  // 应用模板特定样式
  applyTemplateStyles: function(template) {
    // 根据模板类型应用不同的样式增强
    switch (template.id) {
      case '1': // 简约专业
        // 已在WXML和WXSS中定义
        break;
      case '2': // 技术人才
        // 已在WXML和WXSS中定义
        break;
      case '3': // 创意设计
        // 已在WXML和WXSS中定义
        break;
      case '4': // 学术研究
        // 已在WXML和WXSS中定义
        break;
    }
  },

  // 返回模板列表
  backToList: function() {
    wx.navigateBack()
  }
})