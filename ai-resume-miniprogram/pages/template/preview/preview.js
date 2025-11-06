// preview.js
const app = getApp()

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
        address: '上海市浦东新区'
      },
      education: [
        {
          school: '北京大学',
          major: '计算机科学与技术',
          degree: '本科',
          startDate: '2014-09',
          endDate: '2018-06'
        }
      ],
      workExperience: [
        {
          company: '科技有限公司',
          position: '前端开发工程师',
          startDate: '2018-07',
          endDate: '2022-03',
          description: '负责公司核心产品的前端开发，参与需求分析和技术方案制定，优化用户体验和性能。'
        },
        {
          company: '互联网创新公司',
          position: '高级前端开发工程师',
          startDate: '2022-04',
          endDate: '至今',
          description: '负责技术团队的管理和前端架构设计，主导多个重要项目的开发和上线。'
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
      skills: ['JavaScript', 'React', 'Vue', 'TypeScript', 'Webpack', 'Node.js', 'CSS3'],
      selfEvaluation: '5年前端开发经验，精通各种前端技术栈，具有良好的代码规范和团队协作能力。热爱技术，持续学习，追求卓越。'
    }
  },

  onLoad: function(options) {
    const { templateId, templateName } = options
    this.setData({
      templateId,
      templateName
    })
    
    // 根据模板ID获取模板配置
    this.getTemplateConfig(templateId)
  },

  // 获取模板配置
  getTemplateConfig: function(templateId) {
    // 根据不同模板ID返回不同的配置
    const templates = {
      '1': {
        id: '1',
        name: '简约专业模板',
        type: 'basic',
        styleClass: 'basic-template',
        colors: {
          primary: '#333333',
          secondary: '#666666',
          accent: '#0066cc',
          background: '#ffffff',
          border: '#e0e0e0'
        },
        // 定义模板的各个部分是否显示
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true
        }
      },
      '2': {
        id: '2',
        name: '技术人才模板',
        type: 'technical',
        styleClass: 'technical-template',
        colors: {
          primary: '#2c3e50',
          secondary: '#7f8c8d',
          accent: '#3498db',
          background: '#f8f9fa',
          border: '#dee2e6'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true
        }
      },
      '3': {
        id: '3',
        name: '创意设计模板',
        type: 'creative',
        styleClass: 'creative-template',
        colors: {
          primary: '#34495e',
          secondary: '#95a5a6',
          accent: '#e74c3c',
          background: '#ffffff',
          border: '#ecf0f1'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true
        }
      },
      '4': {
        id: '4',
        name: '学术研究模板',
        type: 'academic',
        styleClass: 'academic-template',
        colors: {
          primary: '#1a365d',
          secondary: '#718096',
          accent: '#2b6cb0',
          background: '#f7fafc',
          border: '#e2e8f0'
        },
        sections: {
          personalInfo: true,
          education: true,
          workExperience: true,
          projects: true,
          skills: true,
          selfEvaluation: true
        }
      }
    }

    this.setData({
      template: templates[templateId] || templates['1'] // 默认使用基础模板
    })
  },

  // 使用模板
  useTemplate: function() {
    const { templateId, templateName } = this.data
    
    // 保存选择的模板信息
    wx.setStorageSync('selectedTemplate', {
      id: templateId,
      name: templateName
    })
    
    wx.showToast({
      title: '已选择模板',
      icon: 'success'
    })
    
    // 跳转到创建简历页面，并带上模板信息
    setTimeout(() => {
      wx.navigateTo({
        url: '/pages/create/create?templateId=' + templateId
      })
    }, 1500)
  },

  // 返回模板列表
  backToList: function() {
    wx.navigateBack()
  }
})