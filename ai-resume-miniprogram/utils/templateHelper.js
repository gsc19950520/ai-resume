// templateHelper.js

/**
 * 根据模板ID获取对应的模板数据
 * @param {string} templateId - 模板ID
 * @returns {object} 模板数据对象
 */
export function getTemplateData(templateId) {
  // 为每个模板准备不同的数据
  const templateDatas = {
    'template-one': {
      // 简约专业模板数据
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
          description: '专业成绩优异，获得校二等奖学金。主修课程包括广告设计、色彩构成、素描基础等。'
        }
      ],
      
      // 工作经历
      workExperience: [
        {
          company: '上海品牌设计公司',
          position: '平面设计师',
          startDate: '2024.01',
          endDate: '至今',
          description: '负责产品包装、标志设计等工作，提升品牌视觉形象。设计宣传材料，参与重要项目的创意构思和执行。'
        },
        {
          company: '创意广告有限公司',
          position: '助理设计师',
          startDate: '2022.07',
          endDate: '2023.12',
          description: '协助设计师完成各类设计任务，负责基础排版工作，参与市场调研和创意讨论。'
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
        }
      ],
      
      // 自我评价
      selfEvaluation: '工作积极一丝不苟，认真负责，熟练运用专业软件，踏实肯干，动手能力强，有很强的自驱力，坚韧不拔的精神，喜欢迎接新挑战。'
    },
    'template-two': {
      // 技术人才模板数据
      name: '周星星',
      age: '28',
      gender: '男',
      location: '上海',
      position: '前端开发工程师',
      expectedSalary: '25000/月',
      availableTime: '立即到岗',
      phone: '16888888888',
      email: 'dev@qq.com',
      wechat: 'dev123456',
      avatarUrl: '/images/avatar.png',
      
      // 教育背景
      education: [
        {
          school: '上海科技大学',
          major: '计算机科学与技术',
          degree: '本科',
          startDate: '2018.09',
          endDate: '2022.06',
          description: '主修软件工程、数据结构、算法分析等课程，GPA 3.8/4.0，获得优秀毕业生称号。'
        }
      ],
      
      // 工作经历
      workExperience: [
        {
          company: '科技互联网公司',
          position: '前端开发工程师',
          startDate: '2024.01',
          endDate: '至今',
          description: '负责公司核心产品的前端架构设计和开发，优化性能，提升用户体验。使用React、Vue等主流框架构建响应式Web应用。'
        },
        {
          company: '软件开发有限公司',
          position: '前端开发',
          startDate: '2022.07',
          endDate: '2023.12',
          description: '参与多个企业级应用的开发，负责组件设计和实现，解决跨浏览器兼容性问题，优化加载速度。'
        }
      ],
      
      // 专业技能
      skills: [
        {
          name: '前端技术栈',
          description: '精通HTML5、CSS3、JavaScript/ES6+，熟悉React、Vue、Angular等主流框架'
        },
        {
          name: '工程能力',
          description: '熟悉Webpack、Rollup等构建工具，掌握前端工程化和自动化测试'
        },
        {
          name: '性能优化',
          description: '具备丰富的前端性能优化经验，了解浏览器渲染原理'
        }
      ],
      
      // 技术栈
      technicalStack: {
        languages: ['JavaScript', 'TypeScript', 'HTML5', 'CSS3'],
        frameworks: ['React', 'Vue', 'Angular'],
        tools: ['Webpack', 'Git', 'Docker'],
        databases: ['MongoDB', 'MySQL']
      },
      
      // 成就
      achievements: [
        '主导重构公司核心产品前端架构，页面加载速度提升40%',
        '开发多个可复用组件库，提高团队开发效率30%',
        '成功解决多个复杂的跨浏览器兼容性问题'
      ],
      
      // 自我评价
      selfEvaluation: '热爱技术，持续学习，具有良好的团队协作精神和解决问题的能力。追求代码质量，注重用户体验，善于分析和解决复杂问题。'
    },
    '3': {
      // 创意设计模板数据
      name: '周星星',
      age: '28',
      gender: '男',
      location: '上海',
      position: 'UI/UX设计师',
      expectedSalary: '20000/月',
      availableTime: '两周内到岗',
      phone: '16888888888',
      email: 'design@qq.com',
      wechat: 'design123456',
      avatarUrl: '/images/avatar.png',
      
      // 教育背景
      education: [
        {
          school: '上海艺术学院',
          major: '视觉传达设计',
          degree: '本科',
          startDate: '2018.09',
          endDate: '2022.06',
          description: '专注于用户界面设计和交互设计，在校期间获得多个设计奖项。'
        }
      ],
      
      // 工作经历
      workExperience: [
        {
          company: '创意设计工作室',
          position: 'UI/UX设计师',
          startDate: '2024.01',
          endDate: '至今',
          description: '负责移动应用和网站的用户界面设计，创建用户流程和交互原型，进行用户测试并迭代优化设计方案。'
        },
        {
          company: '互联网产品公司',
          position: 'UI设计师',
          startDate: '2022.07',
          endDate: '2023.12',
          description: '设计产品界面，制作视觉规范，与开发团队协作实现设计效果，参与产品需求讨论。'
        }
      ],
      
      // 专业技能
      skills: [
        {
          name: '设计工具',
          description: '精通Figma、Sketch、Adobe XD、Photoshop、Illustrator等设计工具'
        },
        {
          name: '设计能力',
          description: '具有出色的UI设计、UX设计、交互设计能力，注重用户体验'
        },
        {
          name: '创意思维',
          description: '拥有丰富的创意和独特的设计视角，能够创造出美观且实用的设计方案'
        }
      ],
      
      // 创意方法
      creativeApproach: '注重用户体验和解决问题，通过深入理解用户需求，结合美学原则和交互设计最佳实践，创造出既美观又实用的设计方案。擅长将复杂的功能以简洁直观的方式呈现给用户。',
      
      // 兴趣爱好
      interests: ['摄影', '绘画', '旅行', '阅读', '健身'],
      
      // 自我评价
      selfEvaluation: '充满创意的UI/UX设计师，热爱设计，追求卓越。具有良好的审美能力和创意思维，善于理解用户需求，创造出既美观又实用的设计方案。'
    },
    '4': {
      // 学术研究模板数据
      name: '周星星',
      age: '28',
      gender: '男',
      location: '上海',
      position: '研究助理',
      expectedSalary: '20000/月',
      availableTime: '一个月内到岗',
      phone: '16888888888',
      email: 'research@qq.com',
      wechat: 'research123456',
      avatarUrl: '/images/avatar.png',
      
      // 教育背景
      education: [
        {
          school: '上海大学',
          major: '计算机科学',
          degree: '硕士',
          startDate: '2020.09',
          endDate: '2023.06',
          description: '研究方向：人工智能、机器学习。发表多篇学术论文，参与国家级科研项目。'
        },
        {
          school: '上海大学',
          major: '计算机科学与技术',
          degree: '本科',
          startDate: '2016.09',
          endDate: '2020.06',
          description: '优秀毕业生，GPA 3.9/4.0，多次获得一等奖学金。'
        }
      ],
      
      // 工作经历
      workExperience: [
        {
          company: '上海研究院',
          position: '研究助理',
          startDate: '2023.07',
          endDate: '至今',
          description: '参与人工智能相关研究项目，负责实验设计、数据分析和论文撰写。与国内外高校和企业合作开展研究工作。'
        },
        {
          company: '科技创新实验室',
          position: '实习生',
          startDate: '2022.07',
          endDate: '2023.06',
          description: '参与机器学习算法研究和实现，协助开发实验系统，整理研究数据。'
        }
      ],
      
      // 专业技能
      skills: [
        {
          name: '编程语言',
          description: '精通Python、Java、C++，熟悉R、MATLAB等科研工具'
        },
        {
          name: '研究能力',
          description: '具备扎实的科研基础和创新能力，熟悉人工智能、机器学习、数据挖掘等领域'
        },
        {
          name: '实验设计',
          description: '能够独立设计实验方案，进行数据分析和结果评估'
        }
      ],
      
      // 研究兴趣
      researchInterests: '人工智能、机器学习、深度学习、自然语言处理、计算机视觉、数据挖掘、知识图谱等领域。特别关注AI在医疗、教育、金融等领域的应用研究。',
      
      // 发表论文
      publications: [
        {
          title: '基于深度学习的图像识别技术研究',
          journal: '计算机科学',
          year: '2023',
          authors: '周星星, 李教授'
        },
        {
          title: '自然语言处理在智能客服中的应用',
          conference: '人工智能学术会议',
          year: '2022',
          authors: '周星星, 王教授, 张教授'
        }
      ],
      
      // 自我评价
      selfEvaluation: '热爱科研工作，具有扎实的专业基础和创新精神。善于独立思考和团队协作，能够承受科研压力，追求学术卓越。希望在学术研究领域继续深造和贡献自己的力量。'
    }
  };
  
  // 返回对应模板的数据，如果不存在则返回默认数据
  return templateDatas[templateId] || templateDatas['template-one'];
}

/**
 * 第一人称修正函数
 * 将描述性文本转换为第一人称表述
 * @param {object} data - 需要修正的数据对象
 * @returns {object} 修正后的数据对象
 */
export function firstPersonCorrection(data) {
  const correctedData = JSON.parse(JSON.stringify(data)); // 深拷贝数据
  
  // 修正工作经历描述
  if (correctedData.workExperience && Array.isArray(correctedData.workExperience)) {
    correctedData.workExperience = correctedData.workExperience.map(exp => {
      if (exp.description) {
        exp.description = convertToFirstPerson(exp.description);
      }
      return exp;
    });
  }
  
  // 修正项目经验描述（如果存在）
  if (correctedData.projects && Array.isArray(correctedData.projects)) {
    correctedData.projects = correctedData.projects.map(project => {
      if (project.description) {
        project.description = convertToFirstPerson(project.description);
      }
      return project;
    });
  }
  
  // 修正教育背景描述
  if (correctedData.education && Array.isArray(correctedData.education)) {
    correctedData.education = correctedData.education.map(edu => {
      if (edu.description) {
        edu.description = convertToFirstPerson(edu.description);
      }
      return edu;
    });
  }
  
  return correctedData;
}

/**
 * 将文本转换为第一人称表述
 * @param {string} text - 需要转换的文本
 * @returns {string} 转换后的文本
 */
function convertToFirstPerson(text) {
  // 替换常见的第三人称描述为第一人称
  const replacements = [
    // 处理直接以动词开头的句子
    [/(^|。|；|\n)\s*负责/g, '$1我负责'],
    [/(^|。|；|\n)\s*参与/g, '$1我参与'],
    [/(^|。|；|\n)\s*设计/g, '$1我设计'],
    [/(^|。|；|\n)\s*开发/g, '$1我开发'],
    [/(^|。|；|\n)\s*优化/g, '$1我优化'],
    [/(^|。|；|\n)\s*提升/g, '$1我提升'],
    [/(^|。|；|\n)\s*解决/g, '$1我解决'],
    [/(^|。|；|\n)\s*创建/g, '$1我创建'],
    [/(^|。|；|\n)\s*制作/g, '$1我制作'],
    [/(^|。|；|\n)\s*进行/g, '$1我进行'],
    [/(^|。|；|\n)\s*完成/g, '$1我完成'],
    [/(^|。|；|\n)\s*主导/g, '$1我主导'],
    [/(^|。|；|\n)\s*协助/g, '$1我协助'],
    
    // 处理其他情况
    [/负责/g, '我负责'],
    [/参与/g, '我参与'],
    [/设计/g, '我设计'],
    [/开发/g, '我开发'],
    [/优化/g, '我优化'],
    [/提升/g, '我提升'],
    [/解决/g, '我解决'],
    [/创建/g, '我创建'],
    [/制作/g, '我制作'],
    [/进行/g, '我进行'],
    [/完成/g, '我完成'],
    [/主导/g, '我主导'],
    [/协助/g, '我协助'],
  ];
  
  let result = text;
  
  // 应用替换规则
  replacements.forEach(([pattern, replacement]) => {
    const regex = new RegExp(pattern, 'g');
    result = result.replace(regex, replacement);
  });
  
  return result;
}

/**
 * 格式化日期函数
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化后的日期
 */
export function formatDate(dateStr) {
  if (!dateStr) return '';
  // 确保日期格式一致性，转换为月.日格式
  if (dateStr.includes('-')) {
    return dateStr.replace(/-/g, '.');
  }
  return dateStr;
}

/**
 * 验证必填字段
 * @param {object} data - 需要验证的数据
 * @param {array} requiredFields - 必填字段列表
 * @returns {object} 验证结果
 */
export function validateRequiredFields(data, requiredFields) {
  const missingFields = [];
  
  requiredFields.forEach(field => {
    if (!data[field] || (typeof data[field] === 'string' && data[field].trim() === '')) {
      missingFields.push(field);
    }
  });
  
  return {
    isValid: missingFields.length === 0,
    missingFields
  };
}