// preview.js
const app = getApp()

// 手动定义__route__变量，解决框架错误
const __route__ = 'pages/template/preview/preview';

// 引入工具函数
import { getTemplateData, firstPersonCorrection } from '../../../utils/templateHelper';

Page({
  
  data: {
    templateId: '',
    templateName: '',
    template: null,
    // 预览数据 - 提供默认mock数据，确保页面加载时能正确显示模板内容
    resumeData: {
      name: '周星星',
      age: '28',
      gender: '男',
      location: '上海',
      position: '平面设计师',
      expectedSalary: '15000/月',
      availableTime: '一个月内到岗',
      phone: '16888888888',
      email: 'jianli@qq.com',
      wechat: 'wx123456',
      avatarUrl: '/images/avatar.png',
      
      // 教育背景
      education: [
        {
          school: '上海设计大学',
          major: '艺术学院',
          degree: '本科',
          startDate: '2018.09',
          endDate: '2022.06',
          description: '专业成绩：广告设计，色彩构成，素描基础；获奖情况：校二等奖学金\n主修课程：广告设计、色彩构成、素描基础、平面设计、photoshop、CorelDraw、Illustrator、AutoCAD、广告原理、字体设计、平面广告设计与工艺课程'
        }
      ],
      
      // 工作经历
      workExperience: [
        {
          company: '上海品牌设计公司',
          position: '平面设计师',
          startDate: '2024.01',
          endDate: '至今',
          description: '负责产品包装、标志设计、说明书、挂卡等设计排版，负责产品宣传板、挂历等宣传材料设计；\n公司交办的其他设计任务；负责公司海报、推广手册、吊牌等相关平面设计工作，负责新产品的开发处理；\n协助参加产品的广告设计、参加公司新产品的广告宣传设计等；\n负责企业重要资料的设计、制作与拍摄；也是应对企业广告平面设计、制作以及其他设计类工作'
        },
        {
          company: '创意广告有限公司',
          position: '助理设计师',
          startDate: '2022.07',
          endDate: '2023.12',
          description: '协助设计师完成各类设计任务；负责基础的设计排版工作；参与市场调研和创意讨论；负责素材收集和整理'
        }
      ],
      
      // 项目经验
      projects: [
        {
          name: 'XX品牌视觉形象升级',
          role: '主设计师',
          startDate: '2024.03',
          endDate: '2024.06',
          description: '负责品牌标志重新设计、VI系统升级，提高了品牌识别度和市场影响力'
        },
        {
          name: '年度产品包装设计系列',
          role: '设计师',
          startDate: '2023.09',
          endDate: '2023.12',
          description: '设计了12款产品的包装方案，获得了客户高度认可，销售额提升了15%'
        }
      ],
      
      // 专业技能
      skills: [
        {
          name: '设计软件',
          description: '精通Photoshop、Illustrator、CorelDraw、AutoCAD等设计软件'
        },
        {
          name: '设计能力',
          description: '具有优秀的平面设计、UI设计、品牌设计能力'
        },
        {
          name: '创意思维',
          description: '具备丰富的创意灵感和优秀的审美能力'
        }
      ],
      
      // 荣誉证书
      certifications: [
        {
          name: '广告设计师证',
          year: '2022'
        },
        {
          name: '大学英语六级证书',
          year: '2021'
        },
        {
          name: '全国计算机二级证书',
          year: '2020'
        }
      ],
      
      // 兴趣爱好
      interests: ['摄影', '绘画', '健身', '旅行', '跑步'],
      
      // 自我评价
      selfEvaluation: '工作积极一丝不苟，认真负责，熟练运用专业软件，踏实肯干，动手能力强，有很强的自驱力，坚韧不拔的精神，喜欢迎接新挑战，做好自己，努力最好。'
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
    
    // 加载模板数据
    this.loadTemplateData()
    // 加载简历数据
    this.loadResumeData()
  },
  
  // 加载模板数据
  loadTemplateData: function() {
    const { templateId } = this.data;
    
    // 获取模板数据
    const templateData = getTemplateData(templateId);
    
    // 对数据进行第一人称修正
    const correctedData = firstPersonCorrection(templateData);
    
    // 尝试从全局状态或缓存获取真实数据
    try {
      const cachedData = wx.getStorageSync('resumeData');
      if (cachedData && JSON.stringify(cachedData) !== '{}') {
        // 合并缓存数据和默认数据，确保数据结构完整
        const mergedData = this.mergeData(correctedData, cachedData);
        this.setData({
          resumeData: mergedData
        });
        return;
      }
    } catch (error) {
      console.log('获取缓存数据失败:', error);
    }
    
    // 使用修正后的模板数据
    this.setData({
      resumeData: correctedData
    });
  },
  // 加载简历数据
  loadResumeData: function() {
    // 尝试从全局状态或缓存获取真实数据
    try {
      const cachedData = wx.getStorageSync('resumeData');
      if (cachedData && JSON.stringify(cachedData) !== '{}') {
        // 合并缓存数据和默认数据，确保数据结构完整
        const mergedData = this.mergeData(this.data.resumeData, cachedData);
        this.setData({
          resumeData: mergedData
        });
        return;
      }
    } catch (error) {
      console.log('获取缓存数据失败:', error);
    }
    
    // 如果没有缓存数据，使用默认的mock数据（已在data中定义）
  },
  
  // 合并数据函数，确保默认字段不被覆盖
  mergeData: function(defaultData, newData) {
    const merged = { ...defaultData };
    
    // 合并一级字段
    for (const key in newData) {
      if (newData.hasOwnProperty(key)) {
        // 如果是数组，保留非空数组
        if (Array.isArray(newData[key]) && newData[key].length > 0) {
          merged[key] = newData[key];
        }
        // 如果是对象且非空
        else if (typeof newData[key] === 'object' && newData[key] !== null && Object.keys(newData[key]).length > 0) {
          merged[key] = { ...merged[key], ...newData[key] };
        }
        // 如果是非空基本类型
        else if (newData[key] !== null && newData[key] !== undefined && newData[key] !== '') {
          merged[key] = newData[key];
        }
      }
    }
    
    return merged;
  },

  // 获取模板配置
  getTemplateConfig: function(templateId) {
    // 根据不同模板ID返回不同的配置
    const templates = {
      'template-two': {
        id: 'template-two',
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
      'template-one': {
        id: 'template-one',
        name: '多功能通用模板',
        type: 'universal',
        styleClass: 'template-one',
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
          technicalStack: true,
          achievements: true,
          creativeApproach: true,
          interests: true,
          researchInterests: true,
          publications: true
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
      'template-three': {
        id: 'template-three',
        name: '左侧导航型模板',
        type: 'modern',
        styleClass: 'template-three',
        colors: {
          primary: '#165dff',
          secondary: '#e6f7ff',
          accent: '#09389f',
          background: '#ffffff',
          border: '#d9e9ff'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true,
          interests: true
        },
        layout: {
          type: 'leftNavigation',
          personalInfoPosition: 'left',
          skillsPosition: 'left',
          interestsPosition: 'left',
          educationPosition: 'right',
          workExperiencePosition: 'right',
          projectsPosition: 'right',
          columnRatio: {
            left: 0.3,
            right: 0.7
          }
        }
      },
      'template-four': {
        id: 'template-four',
        name: '卡片式布局模板',
        type: 'card',
        styleClass: 'template-four',
        colors: {
          primary: '#36b37e',
          secondary: '#e6f7ef',
          accent: '#0b8050',
          background: '#ffffff',
          border: '#b7eb8f'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true,
          interests: true
        },
        layout: {
          type: 'cardLayout',
          personalInfoPosition: 'top',
          educationPosition: 'left',
          projectsPosition: 'left',
          workExperiencePosition: 'right',
          skillsPosition: 'right',
          interestsPosition: 'right',
          columnRatio: {
            top: 0.3,
            left: 0.48,
            right: 0.48
          }
        }
      },
      'template-five': {
        id: 'template-five',
        name: '深色主题模板',
        type: 'dark',
        styleClass: 'template-five',
        colors: {
          primary: '#7b61ff',
          secondary: '#2d284a',
          accent: '#5e46ff',
          background: '#1a1924',
          border: '#3e3d53'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true,
          interests: true
        },
        layout: {
          type: 'darkTheme',
          personalInfoPosition: 'top',
          educationPosition: 'left',
          workExperiencePosition: 'left',
          projectsPosition: 'right',
          skillsPosition: 'right',
          interestsPosition: 'right',
          selfEvaluationPosition: 'right',
          columnRatio: {
            top: 0.25,
            left: 0.5,
            right: 0.5
          }
        }
      },
      'template-six': {
        id: 'template-six',
        name: '时尚简约模板',
        type: 'fashion',
        styleClass: 'template-six',
        colors: {
          primary: '#667eea',
          secondary: '#f0f0f5',
          accent: '#764ba2',
          background: '#ffffff',
          border: '#eee'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true,
          interests: true
        },
        layout: {
          type: 'fashionSimple',
          personalInfoPosition: 'top',
          educationPosition: 'left',
          workExperiencePosition: 'left',
          projectsPosition: 'left',
          skillsPosition: 'right',
          expertisePosition: 'right',
          interestsPosition: 'right',
          selfEvaluationPosition: 'right',
          columnRatio: {
            top: 0.3,
            left: 0.5,
            right: 0.5
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
    const { templateId, templateName, template, resumeData } = this.data
    
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
      layout: templateWithLayout.layout, // 保存布局配置
      resumeData: resumeData // 同时保存预览数据
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