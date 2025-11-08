// interview.js
const app = getApp()

Page({
  data: {
    sessionId: '',
    userId: '',
    resumeId: '',
    // 简历选择相关
    showResumeSelectModal: false, // 简历选择弹窗是否显示
    resumeList: [], // 用户的简历列表
    selectedResumeId: '', // 选中的简历ID
    
    // 动态面试配置 - 从数据库获取
    persona: '', // 面试官风格，默认为空，从配置获取
    personas: [], // 面试官风格列表，从数据库获取
    sessionSeconds: 900, // 默认值，将从数据库获取
    sessionTimeRemaining: 900, // 剩余时间（秒）
    showTimeWarning: false, // 时间警告显示状态
    
    // 领域与行业信息
    domain: '',
    keyCompetencies: [],
    industryJobTag: '',
    jobType: '', // 新增：岗位类型
    
    // 评分体系
    scoringMetrics: [],
    weightMap: {},
    
    // 面试状态
    currentRound: 1,
    consecutiveNoMatchCount: 0,
    stopReason: '',
    loading: false,
    loadingText: '加载中...',
    interviewStatus: 'initializing', // 新增：面试状态
    
    // 问题和回答
    currentQuestion: {
      content: '',
      depthLevel: '',
      questionId: '',
      expectedKeyPoints: []
    },
    userAnswer: '',
    answerDuration: 0,
    answerTimer: null,
    recording: false,
    recordingUrl: '',
    
    // 问题焦点与风格
    questionFocus: '', // 新增：问题焦点
    styleHint: '', // 新增：风格提示
    randomFactor: 0, // 新增：随机性因子
    
    // 反馈信息
    showFeedback: false,
    questionScore: {
      tech: 0,
      logic: 0,
      clarity: 0,
      depth: 0
    },
    feedbackText: '',
    matchedPoints: [],
    
    // 会话进度
    progress: 0,
    
    // 面试官风格选择
    personaSelected: false, // 是否已选择面试官风格
    previewQuestion: '', // 当前风格的预览问题
    
    // 会话日志
    sessionLog: [],
    
    // 录音转文字
    transcriptText: '',
    isTranscribing: false,
    
    // 深度级别控制
    depthLevels: [], // 将从数据库获取
    
    // 简历分析结果
    resumeAnalysis: { // 新增：简历分析结果
      jobType: '',
      techItems: [],
      projectSummaries: []
    },
    
    // 用户表现数据
    userPerformance: { // 新增：用户表现数据
      answers: [],
      avgScore: 0,
      lastAnswerQuality: 'good' // good, average, poor
    },
    
    // 薪资匹配结果
    salaryMatchResult: null,
    
    // 动画状态
    animationState: {
      questionCard: 'idle',
      feedbackPanel: 'idle',
      radarScore: 'idle',
      salaryCard: 'idle'
    }
  },
  
  // API服务调用 - 升级为动态面试系统
  apiServices: {
    // 获取动态配置 - 优先调用接口获取数据
    getDynamicConfig: async function() {
      return new Promise((resolve, reject) => {
        try {
          app.request({
            url: '/interview/get-config',
            method: 'GET',
            success: (res) => {
              console.log('获取动态配置响应:', res);
              if (res && (res.code === 0 || res.success)) {
                resolve(res.data || {});
              } else {
                // 接口调用失败时抛出错误，不再自动使用默认配置
                console.warn('接口调用失败:', res.data?.message || '配置获取失败');
                reject(new Error(res.data?.message || '面试官配置获取失败'));
              }
            },
            fail: (err) => {
              console.error('获取动态配置请求失败:', err);
              // 请求失败时抛出错误
              reject(new Error('面试官配置获取失败'));
            }
          });
        } catch (error) {
          console.error('获取配置过程异常:', error);
          reject(new Error('面试官配置获取异常'));
        }
      });
    },
    
    // 获取模拟的动态配置（开发和测试环境使用）
    getMockDynamicConfig: function() {
      return {
        personas: [
          { 
            id: 'colloquial', 
            name: '口语化', 
            emoji: '💬',
            description: '轻松自然，像朋友聊天一样。适合练习表达与思维。',
            example: '你平时在项目里主要怎么用这个框架的？讲讲你的思路。'
          },
          { 
            id: 'formal', 
            name: '正式面试', 
            emoji: '🎓',
            description: '逻辑清晰、专业正式，模拟真实企业面试场景。',
            example: '请详细说明你在该项目中负责的模块及技术实现。'
          },
          { 
            id: 'manager', 
            name: '主管语气', 
            emoji: '🧠',
            description: '偏重项目成果与业务价值，关注你的思考与协作方式。',
            example: '这个优化最终提升了什么指标？对团队交付有什么帮助？'
          },
          { 
            id: 'analytical', 
            name: '冷静分析型', 
            emoji: '🧊',
            description: '逻辑严谨、问题拆解式提问，适合技术深度练习。',
            example: '你认为这个算法的瓶颈在哪？能从复杂度角度分析一下吗？'
          },
          { 
            id: 'encouraging', 
            name: '鼓励型', 
            emoji: '🌱',
            description: '语气温和积极，注重引导思考与成长体验。',
            example: '你的思路挺好，可以再具体举个例子来支撑一下吗？'
          },
          { 
            id: 'pressure', 
            name: '压力面', 
            emoji: '🔥',
            description: '高强度提问，快速节奏模拟顶级面试场景。',
            example: '假设你的系统刚被打挂，你会在3分钟内做什么？'
          }
        ],
        defaultSessionSeconds: 900,
        defaultPersona: 'colloquial',
        minSessionSeconds: 600,
        maxSessionSeconds: 1800,
        depthLevels: [
          { id: 'basic', name: '用法', text: '基础', description: '基础应用' },
          { id: 'intermediate', name: '实现', text: '进阶', description: '实现细节' },
          { id: 'advanced', name: '原理', text: '深入', description: '原理机制' },
          { id: 'expert', name: '优化', text: '高级', description: '性能优化' }
        ]
      };
    },
    // 分析简历内容
    analyzeResume: async function(resumeId) {
      try {
        return new Promise((resolve, reject) => {
          app.request('/interview/analyze-resume', 'POST', { resumeId }, res => {
            if (res.code === 0 || res.success) {
              resolve(res);
            } else {
              console.error('分析简历失败:', res);
              resolve(null);
            }
          });
        });
      } catch (error) {
        console.error('分析简历失败:', error);
        return null;
      }
    },
    
    // 控制提问随机性与多样性
    getRandomDiversity: async function(techItems, projectSummaries, userPerformance) {
      try {
        // 参数验证
        const safeTechItems = Array.isArray(techItems) ? techItems : [];
        const safeProjectSummaries = Array.isArray(projectSummaries) ? projectSummaries : [];
        const safeUserPerformance = userPerformance || { answers: [], avgScore: 0, lastAnswerQuality: 'good' };
        
        // 尝试从服务器获取随机多样性控制
        try {
          const diversityResult = await new Promise((resolve, reject) => {
            app.request('/interview/random-diversity', 'POST', { 
              techItems: safeTechItems, 
              projectSummaries: safeProjectSummaries, 
              userPerformance: safeUserPerformance 
            }, res => {
              resolve(res);
            });
          });
          
          // 验证返回数据的有效性
          if (diversityResult && diversityResult.questionFocus && diversityResult.styleHint !== undefined) {
            if (app.globalData && app.globalData.debug) {
              console.log('成功获取随机多样性控制:', diversityResult);
            }
            return diversityResult;
          }
        } catch (apiError) {
          console.error('获取随机多样性控制API调用失败:', apiError);
        }
        
        // 本地备选逻辑：当API调用失败时生成合理的随机多样性控制
        return this.generateLocalDiversityControl(safeTechItems, safeProjectSummaries, safeUserPerformance);
      } catch (error) {
        console.error('随机多样性控制处理失败:', error);
        // 返回最基础的默认值
        return this.getDefaultDiversityControl();
      }
    },
    
    // 本地备选的随机多样性控制生成逻辑
    generateLocalDiversityControl: function(techItems, projectSummaries, userPerformance) {
      // 基于用户表现调整难度
      let questionFocus = '';
      const lastAnswerQuality = userPerformance.lastAnswerQuality || 'good';
      
      // 可选的问题焦点列表
      const focusOptions = ['技术深度', '项目经验', '问题解决', '基础知识', '实践能力', '架构设计'];
      
      // 可选的风格提示
      const styleOptions = ['引导式', '追问式', '案例式', '情景模拟', '理论探讨', '实践考察'];
      
      // 根据用户表现调整焦点选择策略
      if (lastAnswerQuality === 'poor') {
        // 回答质量差时，倾向于选择基础知识或技术深度
        const easyOptions = focusOptions.filter(f => ['基础知识', '技术深度'].includes(f));
        questionFocus = easyOptions[Math.floor(Math.random() * easyOptions.length)];
      } else if (lastAnswerQuality === 'excellent' && techItems.length > 0) {
        // 回答质量好且有技术项时，选择一个具体技术点深入
        const randomTech = techItems[Math.floor(Math.random() * techItems.length)];
        questionFocus = randomTech.name || randomTech || focusOptions[Math.floor(Math.random() * focusOptions.length)];
      } else {
        // 随机选择一个焦点
        questionFocus = focusOptions[Math.floor(Math.random() * focusOptions.length)];
      }
      
      // 根据用户表现调整风格
      let styleHint = '';
      const avgScore = userPerformance.avgScore || 0;
      
      if (avgScore < 6) {
        // 分数较低时，使用更友好的引导式或案例式
        styleHint = styleOptions[Math.floor(Math.random() * 2)]; // 0: 引导式, 1: 案例式
      } else if (avgScore > 8.5) {
        // 分数较高时，使用更具挑战性的追问式或情景模拟
        styleHint = styleOptions[2 + Math.floor(Math.random() * 2)]; // 2: 案例式, 3: 情景模拟
      } else {
        // 中等分数时，随机选择
        styleHint = styleOptions[Math.floor(Math.random() * styleOptions.length)];
      }
      
      // 计算随机性因子，基于用户表现动态调整
      const randomFactor = Math.min(0.8, Math.max(0.3, 0.5 + (userPerformance.answers?.length || 0) * 0.05));
      
      const result = {
        questionFocus,
        styleHint,
        randomFactor,
        // 添加来源标记，便于调试
        source: 'local_backup'
      };
      
      if (app.globalData && app.globalData.debug) {
        console.log('使用本地备选随机多样性控制:', result);
      }
      
      return result;
    },
    
    // 获取默认的多样性控制参数
    getDefaultDiversityControl: function() {
      return {
        questionFocus: '基础知识',
        styleHint: '引导式',
        randomFactor: 0.5,
        source: 'default'
      };
    },
    
    // 自然语言化AI提问引擎
    generateQuestion: async function(jobType, questionFocus, styleHint, persona, lastAnswer, randomFactor) {
      try {
        // 参数验证和默认值处理
        const safeJobType = jobType || '通用面试';
        const safeQuestionFocus = questionFocus || '基础知识';
        const safeStyleHint = styleHint || '引导式';
        const safePersona = persona || '正式面试';
        const safeLastAnswer = lastAnswer || '';
        const safeRandomFactor = randomFactor !== undefined ? randomFactor : 0.5; // 添加默认的随机性因子
        
        // 准备请求参数
        const requestData = {
          jobType: safeJobType,
          questionFocus: safeQuestionFocus,
          styleHint: safeStyleHint,
          persona: safePersona,
          lastAnswer: safeLastAnswer,
          randomFactor: safeRandomFactor
        };
        
        // 调用API生成问题
        try {
          const questionResult = await new Promise((resolve, reject) => {
            app.request('/interview/generate-question', 'POST', requestData, res => {
              resolve(res);
            });
          });
          
          // 验证返回数据的有效性
          if (questionResult && questionResult.question) {
            if (app.globalData && app.globalData.debug) {
              console.log('成功生成问题:', questionResult.question);
            }
            return questionResult.question;
          }
        } catch (error) {
          console.error('生成问题Promise处理失败:', error);
        }
      } catch (apiError) {
        console.error('生成问题API调用失败:', apiError);
      }
      
      // 本地备选逻辑：当API调用失败时生成默认问题
      return this.generateLocalFallbackQuestion(safeJobType, safeQuestionFocus, safePersona);
    },
    
    // 本地备选问题生成逻辑
    generateLocalFallbackQuestion: function(jobType, questionFocus, persona) {
      const defaultQuestions = {
        '前端开发': {
          '基础知识': [
            '请简述JavaScript的闭包概念及其应用场景。',
            '解释CSS盒模型以及标准盒模型与IE盒模型的区别。',
            '什么是原型链？在JavaScript中原型链的作用是什么？'
          ],
          '框架使用': [
            'React中虚拟DOM是如何工作的？它解决了什么问题？',
            'Vue中的生命周期钩子有哪些？分别在什么阶段执行？',
            '如何优化React应用的性能？'
          ],
          '性能优化': [
            '请描述前端性能优化的常用策略。',
            '如何减少首屏加载时间？',
            '浏览器的渲染过程是怎样的？如何避免重排和重绘？'
          ]
        },
        '后端开发': {
          '基础知识': [
            '请解释RESTful API的设计原则。',
            '什么是事务？事务的ACID特性是什么？',
            '请简述数据库索引的工作原理。'
          ],
          '框架使用': [
            'Spring Boot的核心特性有哪些？',
            'Node.js的事件循环机制是怎样的？',
            '如何设计一个高并发的后端服务？'
          ],
          '性能优化': [
            '后端服务性能优化的方法有哪些？',
            '如何进行数据库查询优化？',
            '缓存策略在后端系统中的应用。'
          ]
        },
        '通用面试': {
          '基础知识': [
            '请介绍一下你的技术栈和擅长领域。',
            '你如何处理工作中的压力和挑战？',
            '请描述一个你解决过的技术难题。'
          ]
        }
      };
      
      // 根据jobType和questionFocus选择问题集合
      const jobQuestions = defaultQuestions[jobType] || defaultQuestions['通用面试'];
      const focusQuestions = jobQuestions[questionFocus] || jobQuestions['基础知识'];
      
      // 随机选择一个问题
      const randomIndex = Math.floor(Math.random() * focusQuestions.length);
      
      return {
        content: focusQuestions[randomIndex],
        depthLevel: '用法',
        questionId: 'local_' + Date.now(),
        expectedKeyPoints: []
      };
    },
    
    // 评估回答
    assessAnswer: async function(question, userAnswer, expectedKeyPoints) {
      try {
        return new Promise((resolve, reject) => {
          app.request({
            url: '/interview/assess-answer',
            method: 'POST',
            data: { question, userAnswer, expectedKeyPoints },
            success: res => {
              // 根据响应格式返回数据
              if (res && (res.code === 0 || res.success) && res.data) {
                resolve(res.data);
              } else {
                resolve(res || {});
              }
            },
            fail: error => {
              console.error('评估回答失败:', error);
              resolve(null);
            }
          });
        });
      } catch (error) {
        console.error('评估回答异常:', error);
        return null;
      }
    },
    
    // 获取成长报告
    getGrowthReport: async function(userId, sessionHistory) {
      try {
        return new Promise((resolve, reject) => {
          app.request({
            url: '/interview/growth-report',
            method: 'POST',
            data: { userId, sessionHistory },
            header: { 'content-type': 'application/json' },
            success: res => {
              // 根据响应格式返回数据
              if (res && (res.code === 0 || res.success) && res.data) {
                resolve(res.data);
              } else {
                resolve(res || {});
              }
            },
            fail: error => {
              console.error('获取成长报告失败:', error);
              resolve(null);
            }
          });
        });
      } catch (error) {
        console.error('获取成长报告异常:', error);
        return null;
      }
    }
  },

  onLoad: async function(options) {
    // 不再自动加载和显示简历选择弹窗，由风格选择页面处理简历选择
    // 立即设置基本参数和默认数据，确保页面排版正确
    this.setData({
      resumeId: options.resumeId || '',
      userId: app.globalData.userInfo?.id || wx.getStorageSync('userId') || '0',
      sessionSeconds: parseInt(options.duration) || 900,
      sessionTimeRemaining: parseInt(options.duration) || 900
    });
    
    // 先使用默认配置，确保即使API调用失败也有完整的UI
    this.useDefaultConfig();
    this.setDefaultInterviewData();
    
    // 设置默认会话状态
    const hasPersona = !!options.persona;
    
    // 只有从风格选择页面跳转过来时（有persona参数）才设置面试问题和进度
    const initialData = {
      interviewStatus: hasPersona ? 'in_progress' : 'initialized',
      persona: options.persona || this.data.personas[0]?.id || '',
      personaSelected: hasPersona, // 只有URL参数中有persona才标记为已选择
      previewQuestion: this.data.personas[0]?.example || ''
    };
    
    // 从风格选择页面跳转过来时，设置面试问题和进度
    if (hasPersona) {
      // 检查是否有传递过来的第一个问题
      let mockQuestion = null;
      
      if (options.firstQuestion) {
        // 使用从风格选择页面传递过来的问题
        mockQuestion = {
          content: decodeURIComponent(options.firstQuestion),
          depthLevel: '用法',
          expectedKeyPoints: ['个人基本信息', '项目经验概述', '技术栈介绍']
        };
      } else {
        // 设置默认的模拟问题
        mockQuestion = {
          content: '请简单介绍一下你自己和你的项目经历。',
          depthLevel: '用法',
          expectedKeyPoints: ['个人基本信息', '项目经验概述', '技术栈介绍']
        };
      }
      
      initialData.currentQuestion = mockQuestion;
      initialData.currentRound = 1;
      initialData.completedQuestions = 0;
    }
    
    this.setData(initialData);
    
    // 初始化时间线
    this.initializeSessionTimeline();
    
    try {
      // 获取动态配置（尝试覆盖默认配置）
      const config = await this.apiServices.getDynamicConfig();
      
      // 如果获取到配置，则更新
      if (config) {
        const defaultSessionSeconds = config.defaultSessionSeconds || 900;
        const defaultPersona = config.defaultPersona || '';
        
        this.setData({
          // 优先级：URL参数 > 数据库配置 > 默认值
          persona: options.persona || defaultPersona || this.data.persona,
          sessionSeconds: parseInt(options.duration) || defaultSessionSeconds,
          sessionTimeRemaining: parseInt(options.duration) || defaultSessionSeconds,
          personas: config.personas || this.data.personas,
          depthLevels: config.depthLevels || this.data.depthLevels
        });
        
        // 更新预览问题
        if (this.data.personas.length > 0 && this.data.persona) {
          const selectedPersona = this.data.personas.find(p => p.id === this.data.persona);
          if (selectedPersona) {
            this.setData({
              previewQuestion: selectedPersona.example
            });
          }
        }
        
        // 确保从风格选择页面跳转过来时不显示风格选择器
        if (hasPersona) {
          this.setData({
            personaSelected: true,
            interviewStatus: 'in_progress'
          });
        }
      }
      
      // 只有从风格选择页面跳转过来时才初始化动态面试系统
      if (hasPersona) {
        // 尝试初始化动态面试系统 - 使用await确保异步操作正确处理
        await this.initializeDynamicInterview();
      }
    } catch (error) {
      console.error('加载页面配置失败:', error);
      // 已经设置了默认数据，这里只需更新加载状态
      this.setData({
        loading: false
      });
    }
  },
  
  // 使用默认配置作为后备
  // 加载用户简历列表 - 优先调用接口获取数据
  loadUserResumes: async function() {
    wx.showLoading({ title: '加载简历中...' })
    
    try {
      // 调用后端接口获取用户简历列表
      const res = await wx.request({
        url: '/api/resume/user-resumes',
        method: 'GET',
        header: {
          'content-type': 'application/json'
        }
      })
      
      wx.hideLoading()
      
      if (res.statusCode === 200 && res.data.success) {
        // 成功获取简历列表
        const resumeList = res.data.data || []
        
        if (resumeList.length === 0) {
          wx.showToast({
            title: '暂无简历，请先创建简历',
            icon: 'none'
          })
        } else {
          this.setData({
            resumeList: resumeList,
            showResumeSelectModal: true
          })
        }
      } else {
        // 接口调用失败
        throw new Error(res.data.message || '简历获取失败')
      }
    } catch (error) {
      wx.hideLoading()
      
      // 立即返回简历获取失败结果
      wx.showToast({
        title: error.message || '简历获取失败',
        icon: 'error'
      })
    }
  },
  
  // 选择简历
  selectResume: function(e) {
    const resumeId = e.currentTarget.dataset.id
    const resume = this.data.resumeList.find(item => item.id === resumeId)
    
    if (resume) {
      this.setData({
        selectedResumeId: resumeId,
        resumeId: resumeId,
        showResumeSelectModal: false
      })
      
      // 跳转到面试官风格选择页面
      wx.navigateTo({
        url: '/pages/interview/interview_style_select?resumeId=' + resumeId + '&industryJobTag=' + (resume.occupation || '技术面试')
      })
    }
  },
  
  // 分析简历中的职位信息
  analyzeResumeJob: function(resume) {
    wx.showLoading({ title: '分析职位中...' })
    
    // 模拟分析过程
    setTimeout(() => {
      wx.hideLoading()
      
      // 设置职位标签
      this.setData({
        industryJobTag: resume.occupation || '技术面试',
        jobType: resume.occupation || ''
      })
      
      wx.showToast({
        title: '职位分析完成',
        icon: 'success'
      })
    }, 1000)
  },
  
  useDefaultConfig: function() {
    this.setData({
      personas: [
        { 
          id: 'colloquial', 
          name: '口语化', 
          emoji: '💬',
          description: '轻松自然，像朋友聊天一样。适合练习表达与思维。',
          example: '你平时在项目里主要怎么用这个框架的？讲讲你的思路。'
        },
        { 
          id: 'formal', 
          name: '正式面试', 
          emoji: '🎓',
          description: '逻辑清晰、专业正式，模拟真实企业面试场景。',
          example: '请详细说明你在该项目中负责的模块及技术实现。'
        },
        { 
          id: 'manager', 
          name: '主管语气', 
          emoji: '🧠',
          description: '偏重项目成果与业务价值，关注你的思考与协作方式。',
          example: '这个优化最终提升了什么指标？对团队交付有什么帮助？'
        },
        { 
          id: 'analytical', 
          name: '冷静分析型', 
          emoji: '🧊',
          description: '逻辑严谨、问题拆解式提问，适合技术深度练习。',
          example: '你认为这个算法的瓶颈在哪？能从复杂度角度分析一下吗？'
        },
        { 
          id: 'encouraging', 
          name: '鼓励型', 
          emoji: '🌱',
          description: '语气温和积极，注重引导思考与成长体验。',
          example: '你的思路挺好，可以再具体举个例子来支撑一下吗？'
        },
        { 
          id: 'pressure', 
          name: '压力面', 
          emoji: '🔥',
          description: '高强度提问，快速节奏模拟顶级面试场景。',
          example: '假设你的系统刚被打挂，你会在3分钟内做什么？'
        }
      ],
      depthLevels: [
        { id: '用法', name: '基础', text: '用法', description: '基本概念和简单应用场景' },
        { id: '实现', name: '进阶', text: '实现', description: '内部工作原理和实现细节' },
        { id: '原理', name: '深入', text: '原理', description: '底层原理和设计思想' },
        { id: '优化', name: '高级', text: '优化', description: '性能优化和最佳实践' }
      ]
    });
  },

  // 选择面试官风格
  selectPersona: function(e) {
    const personaId = e.currentTarget.dataset.id;
    // 查找对应的persona对象
    const selectedPersona = this.data.personas.find(p => p.id === personaId);
    
    if (selectedPersona) {
      this.setData({
        persona: personaId,
        personaSelected: true,
        previewQuestion: selectedPersona.example
      });
      
      // 记录选择到会话日志
      this.data.sessionLog.push({
        type: 'persona_selected',
        value: personaId,
        timestamp: new Date().toISOString()
      });
    }
  },
  
  onUnload: function() {
    // 清除所有计时器
    if (this.data.timer) {
      clearInterval(this.data.timer)
      this.setData({ timer: null })
    }
    
    if (this.data.answerTimer) {
      clearInterval(this.data.answerTimer)
      this.setData({ answerTimer: null })
    }
    
    // 停止录音
    this.stopAudioRecording();
  },
  
  // 初始化会话时间线
  initializeSessionTimeline: function() {
    const timeline = [{
      id: 1,
      status: 'current',
      feedback: null
    }];
    this.setData({
      sessionTimeline: timeline
    });
  },
  
  // 初始化动态面试系统
  initializeDynamicInterview: async function() {
    try {
      // 先获取面试官配置 - 优先从接口获取
      const dynamicConfig = await this.apiServices.getDynamicConfig();
      
      // 更新面试官配置到页面数据
      if (dynamicConfig) {
        this.setData({
          personas: dynamicConfig.personas || this.data.personas,
          depthLevels: dynamicConfig.depthLevels || this.data.depthLevels,
          scoringMetrics: dynamicConfig.scoringMetrics || this.data.scoringMetrics,
          weightMap: dynamicConfig.weightMap || this.data.weightMap
        });
        console.log('成功获取面试官配置:', dynamicConfig);
      }
      
      // 1. 分析简历内容
      const resumeAnalysis = await this.apiServices.analyzeResume(this.data.resumeId);
      
      if (resumeAnalysis && resumeAnalysis.data) {
        this.setData({
          resumeAnalysis: resumeAnalysis.data
        });
        console.log('简历分析完成:', resumeAnalysis.data);
      } else {
        throw new Error('简历分析失败');
      }
      
      // 2. 准备面试会话
      const sessionConfig = {
        userId: this.data.userId,
        resumeId: this.data.resumeId,
        jobType: this.data.resumeAnalysis.jobType,
        persona: this.data.persona,
        duration: this.data.sessionSeconds
      };
      
      // 调用后端API创建面试会话 - 使用app.request代替直接wx.request
      const sessionResponse = await new Promise((resolve, reject) => {
        app.request({
          url: '/interview/start-session',
          method: 'POST',
          data: sessionConfig,
          success: res => resolve(res),
          fail: error => reject(error)
        });
      });
      
      if (sessionResponse && (sessionResponse.code === 0 || sessionResponse.success) && sessionResponse.data && sessionResponse.data.sessionId) {
        this.setData({
          sessionId: sessionResponse.data.sessionId
        });
        
        // 3. 生成第一个面试问题
        await this.generateFirstQuestion();
        
        // 4. 设置面试状态并开始计时器
        this.setData({
          interviewStatus: 'in_progress'
        });
        
        // 启动会话计时器
        this.startSessionTimer();
        
        // 启动回答计时器
        this.startAnswerTimer();
      } else {
        throw new Error('创建面试会话失败');
      }
    } catch (error) {
      console.error('初始化动态面试失败:', error);
      
      // 显示错误提示
      wx.showToast({
        title: error.message || '初始化面试失败',
        icon: 'error',
        duration: 3000
      });
      
      // 如果是面试官配置获取失败，显示特定提示
      if (error.message && error.message.includes('面试官配置获取')) {
        wx.showModal({
          title: '配置获取失败',
          content: '无法获取面试官配置，请检查网络连接后重试。',
          showCancel: false,
          success: (res) => {
            if (res.confirm) {
              // 用户确认后可以选择返回上一页
              this.goBack();
            }
          }
        });
      }
      
      // 设置默认面试数据，确保UI排版正确
      this.setDefaultInterviewData();
      
      // 设置默认模拟问题
      const mockQuestion = {
        content: '请简单介绍一下你自己和你的项目经历。',
        depthLevel: '用法',
        expectedKeyPoints: ['个人基本信息', '项目经验概述', '技术栈介绍']
      };
      
      // 设置默认会话时间线
      this.setData({
        currentQuestion: mockQuestion,
        currentRound: 1,
        completedQuestions: 0,
        interviewStatus: 'initialized'
      });
      
      // 初始化时间线
      this.initializeSessionTimeline();
      
      // 如果没有选择面试官风格，默认选择第一个
      if (!this.data.persona && this.data.personas && this.data.personas.length > 0) {
        this.setData({
          persona: this.data.personas[0].id,
          personaSelected: true,
          previewQuestion: this.data.personas[0].example
        });
      }
    }
  },
  
  // 生成第一个面试问题
  generateFirstQuestion: async function() {
    try {
      // 使用简历分析结果生成第一个问题
      const { jobType, techItems, projectSummaries } = this.data.resumeAnalysis;
      
      // 获取随机多样性控制
      const diversityResult = await this.apiServices.getRandomDiversity(
        techItems,
        projectSummaries,
        this.data.userPerformance
      );
      
      if (diversityResult) {
        const { questionFocus, styleHint, randomFactor } = diversityResult;
        
        this.setData({
          questionFocus,
          styleHint,
          randomFactor
        });
        
        // 生成第一个问题 - 传递所有必要参数
        const question = await this.apiServices.generateQuestion(
          jobType,
          questionFocus,
          styleHint,
          this.data.persona,
          '', // 第一次没有上一个回答
          randomFactor
        );
        
        if (question && question.content) {
          this.setData({
            currentQuestion: question,
            expectedKeyPoints: question.expectedKeyPoints || []
          });
          
          // 记录问题到历史
          this.addQuestionToHistory(question, styleHint);
        } else {
          // 如果生成失败，使用mock问题
          console.warn('API生成问题失败，使用备用问题');
          const mockQuestion = this.useMockQuestion();
          this.setData({
            currentQuestion: mockQuestion,
            expectedKeyPoints: []
          });
          this.addQuestionToHistory(mockQuestion, styleHint);
        }
      } else {
        // 如果获取多样性控制失败，使用默认问题
        console.warn('获取多样性控制失败，使用默认问题');
        const mockQuestion = this.useMockQuestion();
        this.setData({
          currentQuestion: mockQuestion,
          expectedKeyPoints: []
        });
        this.addQuestionToHistory(mockQuestion, '默认');
      }
    } catch (error) {
      console.error('生成第一个问题失败:', error);
      // 关闭loading状态
      this.setData({ loading: false });
      // 错误情况下使用mock问题确保面试能继续
      try {
        const mockQuestion = this.useMockQuestion();
        this.setData({
          currentQuestion: mockQuestion,
          expectedKeyPoints: []
        });
        this.addQuestionToHistory(mockQuestion, '错误恢复');
      } catch (mockError) {
        console.error('使用备用问题也失败:', mockError);
        throw error; // 仍然抛出原始错误
      }
    }
  },
  
  // 添加问题到历史记录
  addQuestionToHistory: function(question, styleHint) {
    const newQuestionEntry = {
      id: Date.now(),
      question,
      styleHint,
      timestamp: new Date().toISOString()
    };
    
    const updatedHistory = [...this.data.interviewHistory];
    updatedHistory.push(newQuestionEntry);
    
    this.setData({
      interviewHistory: updatedHistory
    });
  },

  // 设置默认面试数据（作为后备）
  setDefaultInterviewData: function() {
    this.setData({
      domain: '软件工程',
      keyCompetencies: ['JavaScript', 'React', 'CSS3', '性能优化'],
      industryJobTag: '软件工程 - 前端开发',
      knowledgeNodes: ['JavaScript基础', '前端框架', '性能优化', '用户体验'],
      topicHierarchy: {
        '基础': ['JavaScript语法', 'CSS布局', 'HTML语义化'],
        '进阶': ['React组件', '状态管理', '工程化'],
        '应用': ['性能优化实践', '架构设计']
      },
      scoringMetrics: ['专业技能', '逻辑思维', '沟通表达', '创新潜力'],
      weightMap: {'专业技能': 0.4, '逻辑思维': 0.3, '沟通表达': 0.2, '创新潜力': 0.1},
      depthLevels: [
        { id: '用法', name: '基础', text: '用法', description: '基本概念和简单应用场景' },
        { id: '实现', name: '进阶', text: '实现', description: '内部工作原理和实现细节' },
        { id: '原理', name: '深入', text: '原理', description: '底层原理和设计思想' },
        { id: '优化', name: '高级', text: '优化', description: '性能优化和最佳实践' }
      ]
    });
  },
  
  // 更新会话时间线
  updateTimeline: function(round, status, feedback = null) {
    let timeline = [...this.data.sessionTimeline];
    
    // 更新当前问题状态
    if (timeline[round - 1]) {
      timeline[round - 1].status = status;
      timeline[round - 1].feedback = feedback;
    }
    
    // 动态添加下一轮次（如果不存在）
    if (!timeline[round]) {
      timeline.push({
        id: round + 1,
        status: 'current',
        feedback: null
      });
    } else {
      // 更新下一轮状态为当前
      timeline[round].status = 'current';
    }
    
    this.setData({
      sessionTimeline: timeline,
      completedQuestions: round,
      currentRound: round + 1
    });
  },

  // 开始会话计时器
  startSessionTimer: function() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
      this.setData({ timer: null });
    }
    
    const timer = setInterval(() => {
      try {
        let remaining = this.data.sessionTimeRemaining - 1;
        const { sessionSeconds } = this.data;
        
        if (remaining <= 0) {
          remaining = 0;
          clearInterval(timer);
          this.setData({ 
            sessionTimeRemaining: 0,
            timer: null,
            progress: 100
          });
          // 时间到，结束面试
          this.finishInterview('time_up');
          return;
        }
        
        // 计算进度百分比
        const progress = (1 - remaining / sessionSeconds) * 100;
        
        this.setData({ 
          sessionTimeRemaining: remaining,
          progress: progress
        });
        
        // 剩余时间少于60秒且为整10秒时显示警告
        if (remaining <= 60 && remaining % 10 === 0 && !this.data.showTimeWarning) {
          this.showTimeWarningOverlay();
        }
        
        // 调试日志
        if (app.globalData && app.globalData.debug) {
          console.log(`面试剩余时间: ${remaining}秒, 进度: ${progress.toFixed(1)}%`);
        }
      } catch (error) {
        console.error('计时器更新错误:', error);
        clearInterval(timer);
        this.setData({ timer: null });
      }
    }, 1000);
    
    this.setData({ timer });
  },
  
  // 开始回答计时器
  startAnswerTimer: function() {
    if (this.data.answerTimer) {
      clearInterval(this.data.answerTimer);
    }
    
    this.setData({ answerDuration: 0 });
    
    const answerTimer = setInterval(() => {
      this.setData({ 
        answerDuration: this.data.answerDuration + 1
      });
    }, 1000);
    
    this.setData({ answerTimer });
  },
  
  // 停止回答计时器
  stopAnswerTimer: function() {
    if (this.data.answerTimer) {
      clearInterval(this.data.answerTimer);
    }
  },
  
  // 开始面试会话
  startInterview: function() {
    console.log('开始初始化面试');
    
    this.setData({ loading: true, loadingText: '正在准备面试...' });
    
    // 调用后端开始面试API
    wx.request({
      url: app.globalData.baseUrl + '/api/interview/start',
      method: 'POST',
      data: {
        userId: this.data.userId,
        resumeId: this.data.resumeId,
        persona: this.data.persona,
        sessionSeconds: this.data.sessionSeconds
      },
      success: (res) => {
        if (res.data && res.data.code === 0) {
          const data = res.data.data;
          this.setData({
            sessionId: data.sessionId,
            currentQuestion: {
              content: data.nextQuestion.content,
              depthLevel: data.nextQuestion.depthLevel,
              questionId: data.nextQuestion.questionId,
              expectedKeyPoints: data.nextQuestion.expectedKeyPoints || []
            },
            industryJobTag: data.position || '',
            keyCompetencies: data.techItems || [],
            loading: false
          });
          
          // 开始回答计时器
          this.startAnswerTimer();
          
          // 更新进度和时间线
          this.updateProgress();
          this.updateTimeline(1, 'active');
        } else {
          wx.showToast({ title: '开始面试失败', icon: 'none' });
          this.setData({ loading: false });
        }
      },
      fail: (error) => {
        console.error('开始面试失败:', error);
        wx.showToast({ title: '网络错误', icon: 'none' });
        this.setData({ loading: false });
      }
    });
  },

  // 使用模拟问题（备用）
  useMockQuestion: function() {
    // 使用getDefaultNextQuestion获取默认问题，避免硬编码
    const defaultQuestion = this.getDefaultNextQuestion(this.data.depthLevels.length > 0 ? this.data.depthLevels[0] : '用法');
    
    // 返回问题对象而不是设置页面数据
    // 页面数据的设置和计时器的启动由调用此方法的函数负责
    return defaultQuestion;
  },

  // 开始计时器
  startTimer: function() {
    const timer = setInterval(() => {
      this.setData({
        elapsedTime: this.data.elapsedTime + 1
      })
    }, 1000)
    this.setData({ timer })
  },

  // 格式化时间 - 剩余时间显示
  formatRemainingTime: function(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  },
  
  // 时间格式化函数
  formatTime: function(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  },
  
  // 格式化时间 - 回答时长显示
  formatAnswerDuration: function(seconds) {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    if (minutes > 0) {
      return `${minutes}分${secs}秒`;
    } else {
      return `${secs}秒`;
    }
  },
  
  // 获取面试官风格文本
  getPersonaText: function(persona) {
    // 从动态配置的personas数组中构建映射关系
    const personaObj = this.data.personas.find(p => p.id === persona);
    return personaObj ? personaObj.name : '友好型';
  },
  
  // 获取深度级别描述
  getDepthDescription: function(depthLevel) {
    // 从动态配置的depthLevels数组中获取深度描述
    if (this.data.depthLevels && this.data.depthLevels.length > 0) {
      const depthObj = this.data.depthLevels.find(d => d.id === depthLevel || d.name === depthLevel);
      if (depthObj) return depthObj.description;
    }
    
    // 兼容现有格式的回退方案
    const depthMap = {
      'basic': '基础应用',
      'intermediate': '实现细节',
      'advanced': '底层原理',
      '用法': '基础应用',
      '实现': '实现细节',
      '原理': '底层原理',
      '优化': '高级优化'
    };
    return depthMap[depthLevel] || '基础应用';
  },

  // 获取深度等级文本
  getDepthText: function(depthLevel) {
    // 从动态配置的depthLevels数组中获取深度文本
    if (this.data.depthLevels && this.data.depthLevels.length > 0) {
      const depthObj = this.data.depthLevels.find(d => d.id === depthLevel || d.name === depthLevel);
      if (depthObj) return depthObj.text || depthObj.name;
    }
    
    // 兼容现有格式的回退方案
    const depthMap = {
      'basic': '基础',
      'intermediate': '进阶',
      'advanced': '高级',
      '用法': '基础',
      '实现': '进阶',
      '原理': '深入',
      '优化': '深入'
    }
    return depthMap[depthLevel] || '基础'
  },
  
  // 获取深度描述
  getDepthDescription: function(depthLevel) {
    // 从动态配置的depthLevels数组中获取深度描述
    if (this.data.depthLevels && this.data.depthLevels.length > 0) {
      const depthObj = this.data.depthLevels.find(d => d.id === depthLevel || d.name === depthLevel);
      if (depthObj && depthObj.description) return depthObj.description;
    }
    
    // 兼容现有格式的回退方案
    const descriptionMap = {
      'basic': '基础应用',
      'intermediate': '实现细节',
      'advanced': '原理机制',
      '用法': '基础应用',
      '实现': '实现细节',
      '原理': '原理机制',
      '优化': '性能优化',
      1: '概念理解',
      2: '实践应用',
      3: '深入分析',
      4: '创新思考'
    }
    return descriptionMap[depthLevel] || '基础应用'
  },

  // 监听回答输入
  onAnswerInput: function(e) {
    const newAnswer = e.detail.value;
    
    // 如果是第一次输入回答内容，启动回答计时器
    if (!this.data.userAnswer && newAnswer) {
      this.startAnswerTimer();
    }
    
    this.setData({
      userAnswer: newAnswer
    })
  },

  // 切换录音状态
  toggleRecording: function() {
    if (this.data.loading) return; // 防止在加载时点击录音按钮
    
    if (this.data.recording) {
      this.stopAudioRecording()
    } else {
      this.startAudioRecording()
    }
  },

  // 开始录音
  startAudioRecording: function() {
    wx.getSetting({ 
      success: res => {
        if (!res.authSetting['scope.record']) {
          wx.authorize({
            scope: 'scope.record',
            success: () => {
              // 开始回答计时器
              this.startAnswerTimer();
              this.startRecord()
            },
            fail: () => {
              wx.showToast({
                title: '需要录音权限',
                icon: 'none'
              })
            }
          })
        } else {
          // 开始回答计时器
          this.startAnswerTimer();
          this.startRecord()
        }
      }
    })
  },

  // 开始录音实现
  startRecord: function() {
    // 清除之前可能存在的录音
    const recorder = wx.getRecorderManager();
    recorder.stop(); // 停止任何正在进行的录音
    
    // 开始回答计时器
    this.startAnswerTimer();
    
    const options = {
      duration: 60000,
      sampleRate: 44100,
      numberOfChannels: 1,
      encodeBitRate: 192000,
      format: 'aac'
    }
    
    recorder.start(options)
    this.setData({ 
      recording: true,
      transcriptText: '' // 清空之前的转录文本
    })
    
    recorder.onStop = (res) => {
      this.setData({
        recordingUrl: res.tempFilePath,
        recording: false
      })
      // 录音结束后停止回答计时器
      this.stopAnswerTimer();
    }
    
    recorder.onError = (err) => {
      console.error('录音失败:', err)
      this.setData({ recording: false })
      // 录音失败时停止回答计时器
      this.stopAnswerTimer();
      wx.showToast({
        title: '录音失败',
        icon: 'none'
      })
    }
  },

  // 停止录音
  stopAudioRecording: function() {
    const recorder = wx.getRecorderManager()
    recorder.stop()
  },

  // 播放录音
  playRecording: function() {
    if (!this.data.recordingUrl) return
    
    const innerAudioContext = wx.createInnerAudioContext()
    innerAudioContext.src = this.data.recordingUrl
    innerAudioContext.play()
    
    innerAudioContext.onError = (err) => {
      console.error('播放失败:', err)
      wx.showToast({
        title: '播放失败',
        icon: 'none'
      })
    }
  },

  // 提交回答
  submitAnswer: function() {
    // 检查回答内容是否为空
    if (!this.data.userAnswer.trim()) {
      wx.showToast({ title: '请输入您的回答', icon: 'none' });
      return;
    }
    
    // 提交回答时停止回答计时器
    this.stopAnswerTimer();
    
    // 设置加载状态
    this.setData({ loading: true, loadingText: '正在评估您的回答...' });
    
    // 1. 评估当前回答
    this.assessCurrentAnswer().then(assessmentResult => {
      if (!assessmentResult) {
        throw new Error('评估失败');
      }
      
      // 2. 更新用户表现数据
      this.updateUserPerformance(assessmentResult.score);
      
      // 3. 保存回答到历史
      this.saveAnswerToHistory(assessmentResult);
      
      // 4. 检查是否需要结束面试
      if (this.shouldFinishInterview()) {
        this.finishInterview();
      } else {
        // 5. 生成下一个问题
        this.generateNextQuestion().then(() => {
          this.setData({ loading: false });
        }).catch(() => {
          this.setData({ loading: false });
          wx.showToast({ title: '生成下一题失败，请重试', icon: 'none' });
        });
      }
    }).catch(error => {
      console.error('提交回答失败:', error);
      wx.showToast({ title: '处理失败，请重试', icon: 'none' });
      this.setData({ loading: false });
    });
  },
  
  // 评估当前回答
  assessCurrentAnswer: async function() {
    try {
      const result = await this.apiServices.assessAnswer(
        this.data.currentQuestion,
        this.data.userAnswer,
        this.data.expectedKeyPoints
      );
      
      if (result) {
        // 更新评分和反馈
        this.setData({
          currentScore: result.score || 0,
          currentFeedback: result.feedback || '',
          scoringBreakdown: result.scoringBreakdown || { technical: 0, logic: 0, clarity: 0 }
        });
        return result;
      }
      return null;
    } catch (error) {
      console.error('评估回答失败:', error);
      this.setData({ loading: false });
      return null;
    }
  },
  
  // 更新用户表现数据
  updateUserPerformance: function(score) {
    const answers = [...this.data.userPerformance.answers, score];
    const avgScore = answers.reduce((sum, s) => sum + s, 0) / answers.length;
    
    // 判断回答质量
    let lastAnswerQuality = 'average';
    if (score >= 8) lastAnswerQuality = 'good';
    else if (score <= 5) lastAnswerQuality = 'poor';
    
    // 检查连续无匹配次数
    if (score <= 5) {
      this.setData({
        consecutiveNoMatchCount: this.data.consecutiveNoMatchCount + 1
      });
    } else {
      this.setData({
        consecutiveNoMatchCount: 0
      });
    }
    
    this.setData({
      userPerformance: {
        answers,
        avgScore,
        lastAnswerQuality
      }
    });
  },
  
  // 保存回答到历史
  saveAnswerToHistory: function(assessmentResult) {
    const { resumeAnalysis, currentQuestion, userAnswer, styleHint, answerDuration } = this.data;
    
    const answerEntry = {
      question: currentQuestion,
      answer: userAnswer,
      score: assessmentResult.score,
      feedback: assessmentResult.feedback,
      styleHint,
      duration: answerDuration,
      timestamp: new Date().toISOString(),
      jobType: resumeAnalysis.jobType
    };
    
    // 更新历史记录
    const updatedHistory = [...this.data.interviewHistory];
    const lastQuestion = updatedHistory[updatedHistory.length - 1];
    if (lastQuestion) {
      lastQuestion.answer = answerEntry;
    }
    
    // 清空当前回答和计时器
    this.setData({
      interviewHistory: updatedHistory,
      userAnswer: '',
      answerDuration: 0
    });
  },
  
  // 判断是否需要结束面试
  shouldFinishInterview: function() {
    try {
      // 时间用尽
      if (this.data.sessionTimeRemaining <= 0) {
        return true;
      }
      
      // 连续两次回答质量差
      if (this.data.consecutiveNoMatchCount >= 2) {
        return true;
      }
      
      // 检查面试状态
      if (this.data.interviewStatus === 'finished') {
        return true;
      }
      
      return false;
    } catch (error) {
      console.error('检查面试结束条件错误:', error);
      return false;
    }
  },
  
  // 生成下一个问题
  generateNextQuestion: async function() {
    try {
      const { resumeAnalysis, userPerformance } = this.data;
      
      // 获取随机多样性控制
      let questionFocus, styleHint, randomFactor;
      
      try {
        const diversityResult = await this.apiServices.getRandomDiversity(
          resumeAnalysis.techItems,
          resumeAnalysis.projectSummaries,
          userPerformance
        );
        
        if (diversityResult) {
          questionFocus = diversityResult.questionFocus || '';
          styleHint = diversityResult.styleHint || '';
          randomFactor = diversityResult.randomFactor || 0.5;
        } else {
          // 如果获取多样性控制失败，使用默认值
          console.warn('未能获取多样性控制，使用默认值');
          questionFocus = '技术能力';
          styleHint = '标准';
          randomFactor = 0.5;
        }
      } catch (diversityError) {
        console.error('获取多样性控制失败:', diversityError);
        // 使用默认值继续
        questionFocus = '技术能力';
        styleHint = '标准';
        randomFactor = 0.5;
      }
      
      // 生成下一个问题 - 传递所有必要参数
      let question;
      
      try {
        question = await this.apiServices.generateQuestion(
          resumeAnalysis.jobType,
          questionFocus,
          styleHint,
          this.data.persona,
          this.data.userAnswer, // 上一个回答
          randomFactor
        );
        
        // 验证问题结果
        if (!question || !question.content) {
          console.warn('API返回的问题数据不完整，使用备用问题');
          question = this.useMockQuestion();
        }
      } catch (questionError) {
        console.error('生成问题失败:', questionError);
        // 使用本地备用问题
        question = this.useMockQuestion();
      }
      
      if (question && question.content) {
        this.setData({
          currentQuestion: question,
          expectedKeyPoints: question.expectedKeyPoints || [],
          questionFocus,
          styleHint,
          randomFactor
        });
        
        // 记录问题到历史
        this.addQuestionToHistory(question, styleHint);
        
        // 重新开始回答计时器
        this.startAnswerTimer();
      }
    } catch (error) {
      console.error('生成下一个问题失败:', error);
      this.setData({ loading: false });
      
      try {
        // 最后的备用方案：使用模拟问题确保面试继续
        const mockQuestion = this.useMockQuestion();
        if (mockQuestion && mockQuestion.content) {
          this.setData({
            currentQuestion: mockQuestion,
            expectedKeyPoints: []
          });
          
          this.addQuestionToHistory(mockQuestion, '备用模式');
          this.startAnswerTimer();
          console.log('已使用备用问题确保面试继续');
        }
      } catch (fallbackError) {
        console.error('备用方案也失败:', fallbackError);
        // 如果所有尝试都失败，抛出错误以通知上层处理
        throw error;
      }
    }
  },

  // 发送回答到后端
  sendAnswer: function() {
    this.setData({ loading: true, loadingText: 'AI评分中...' })
    
    // 调用AI评估服务
    this.services.aiAssessmentPerQuestion.assess(this.data.currentQuestion.content, this.data.userAnswer)
      .then(assessment => {
        console.log('AI评估结果:', assessment);
        
        const params = {
          sessionId: this.data.sessionId,
          questionId: this.data.currentQuestion.questionId,
          userAnswerText: this.data.userAnswer,
          userAnswerAudioUrl: this.data.recordingUrl || '',
          aiAssessment: assessment
        }
        
        // 调用后端API处理回答
        app.request('/api/interview/answer', 'POST', params, res => {
          if (res && res.code === 0 && res.data) {
            this.handleAnswerResponse(res.data)
          } else {
            // 使用模拟评分
            this.useMockScore(assessment);
          }
        }, err => {
          console.error('提交回答失败:', err)
          // 使用模拟评分
          this.useMockScore(assessment);
        })
      });
  },
  
  // 处理回答响应
  handleAnswerResponse: async function(data) {
    try {
      // 检查数据是否有效
      if (data && data.perQuestionScore) {
        // 保存评分和反馈
        const logEntry = {
          question: this.data.currentQuestion.content,
          answer: this.data.userAnswer,
          score: {
            tech: Math.floor(data.perQuestionScore.tech_score / 10),
            logic: Math.floor(data.perQuestionScore.logic_score / 10),
            clarity: Math.floor(data.perQuestionScore.expression_score / 10),
            depth: Math.floor(data.perQuestionScore.depth_score / 10)
          },
          feedback: data.feedback,
          matchedPoints: data.matchedPoints || []
        }
        
        const sessionLog = [...this.data.sessionLog, logEntry]
        
        // 更新评分和反馈
        this.setData({
          questionScore: {
            tech: Math.floor(data.perQuestionScore.tech_score / 10),
            logic: Math.floor(data.perQuestionScore.logic_score / 10),
            clarity: Math.floor(data.perQuestionScore.expression_score / 10),
            depth: Math.floor(data.perQuestionScore.depth_score / 10)
          },
          feedbackText: data.feedback,
          matchedPoints: data.matchedPoints || [],
          sessionLog,
          showFeedback: true,
          followUpEnabled: !data.stopFlag,
          lastAnswer: this.data.userAnswer,
          // 触发反馈动画
          animationState: {
            ...this.data.animationState,
            feedbackPanel: 'fade-in + pulse',
            radarScore: 'grow + rotate',
            salaryCard: 'idle'
          },
          loading: false
        })
        
        // 更新时间线和进度
        this.updateTimeline(this.data.currentRound, 'completed', this.data.questionScore);
        this.updateProgress()
        
        // 获取薪资匹配信息
        setTimeout(() => {
          this.getSalaryMatch();
        }, 1000);
        
        // 如果需要停止，自动进入下一个问题
        if (data.stopFlag) {
          setTimeout(() => {
            this.continueInterview(false)
          }, 3000)
        }
      } else {
        // 使用备用评分机制（异步）
        const result = await this.useMockScore(data);
        
        if (result) {
          const sessionLog = [...this.data.sessionLog, result.logEntry]
          
          this.setData({
            questionScore: result.mockScore,
            feedbackText: result.logEntry.feedback,
            matchedPoints: result.logEntry.matchedPoints,
            sessionLog,
            showFeedback: true,
            followUpEnabled: !result.stopFlag,
            lastAnswer: this.data.userAnswer,
            // 触发反馈动画
            animationState: {
              ...this.data.animationState,
              feedbackPanel: 'fade-in + pulse',
              radarScore: 'grow + rotate',
              salaryCard: 'idle'
            },
            loading: false
          })
          
          // 更新时间线和进度
          this.updateTimeline(this.data.currentRound, 'completed', result.mockScore);
          this.updateProgress()
          
          // 获取薪资匹配信息
          setTimeout(() => {
            this.getSalaryMatch();
          }, 1000);
          
          // 如果需要停止，自动进入下一个问题
          if (result.stopFlag) {
            setTimeout(() => {
              this.continueInterview(false)
            }, 3000)
          }
        }
      }
    } catch (error) {
      console.error('处理回答响应失败:', error);
      this.setData({ loading: false });
    }
  },

  // 使用模拟评分（备用）
  useMockScore: async function(assessment = null) {
    try {
      let mockScore, stopFlag, logEntry;
      
      try {
        // 尝试从API获取评分数据
        const assessmentData = await this.apiServices.assessAnswer(
          this.data.currentQuestion.content,
          this.data.userAnswer,
          this.data.jobType
        );
        
        mockScore = {
          tech: Math.floor(assessmentData.tech_score / 10),
          logic: Math.floor(assessmentData.logic_score / 10),
          clarity: Math.floor(assessmentData.expression_score / 10),
          depth: Math.floor(assessmentData.depth_score / 10)
        };
        
        stopFlag = assessmentData.stop_follow_up || false;
        
        logEntry = {
          question: this.data.currentQuestion.content,
          answer: this.data.userAnswer,
          score: mockScore,
          feedback: assessmentData.feedback,
          matchedPoints: assessmentData.matched_points || []
        };
      } catch (error) {
        console.error('获取评分数据失败，使用通用默认值:', error);
        
        // 使用更通用的默认评分逻辑
        mockScore = {
          tech: 8,
          logic: 8,
          clarity: 8,
          depth: 8
        };
        
        stopFlag = false;
        
        logEntry = {
          question: this.data.currentQuestion.content,
          answer: this.data.userAnswer,
          score: mockScore,
          feedback: '请详细分析您的回答并提供技术深度。',
          matchedPoints: []
        };
      }
      
      const sessionLog = [...this.data.sessionLog, logEntry];
      
      this.setData({
        showFeedback: true,
        questionScore: mockScore,
        feedbackText: assessment?.feedback || '回答整体不错，但可以进一步深入技术细节。',
        matchedPoints: ['覆盖了基本概念', '逻辑清晰'],
        sessionLog,
        followUpEnabled: !stopFlag,
        lastAnswer: this.data.userAnswer,
        // 触发反馈动画
        animationState: {
          ...this.data.animationState,
          feedbackPanel: 'fade-in + pulse',
          radarScore: 'grow + rotate'
        },
        loading: false
      })
      
      // 更新时间线和进度
      this.updateTimeline(this.data.currentRound, 'completed', mockScore);
      this.updateProgress()
      
      // 如果需要停止，自动进入下一个问题
      if (stopFlag) {
        setTimeout(() => {
          this.continueInterview(false)
        }, 3000)
      }
    } catch (error) {
      console.error('模拟评分过程中发生错误:', error);
      this.setData({ loading: false });
    }
  },

  // 继续面试
  continueInterview: function(isFollowUp = false) {
    // 如果是追问，更新深度级别
    if (isFollowUp) {
      this.setData({ loading: true, loadingText: 'AI正在准备下一个问题...' })
      
      // 更新深度级别
      const nextDepthIndex = Math.min(this.data.currentDepthIndex + 1, this.data.depthLevels.length - 1)
      const nextDepthLevel = this.data.depthLevels[nextDepthIndex]
      
      this.setData({ 
        currentDepthIndex: nextDepthIndex,
        // 触发问题卡片动画
        animationState: {
          ...this.data.animationState,
          questionCard: 'slide-from-bottom'
        }
      })
      
      // 调用API获取追问
      const params = {
        sessionId: this.data.sessionId,
        lastAnswer: this.data.lastAnswer,
        currentDepthLevel: nextDepthLevel
      }
      
      app.request('/api/interview/followup', 'POST', params, res => {
        if (res && res.code === 0 && res.data) {
          this.handleFollowUpResponse(res.data, nextDepthLevel)
        } else {
          this.useMockFollowUp(nextDepthLevel)
        }
      }, err => {
        console.error('获取追问失败:', err)
        this.useMockFollowUp(nextDepthLevel)
      })
    } else {
      // 检查是否达到最大轮数
      if (this.data.currentRound >= this.data.maxRounds) {
        this.finishInterview()
        return
      }
      
      // 准备下一轮问题
      this.prepareNextRound()
    }
  },
  
  // 处理追问响应
  handleFollowUpResponse: function(data, depthLevel) {
    this.setData({
      currentQuestion: {
        content: data.nextQuestion,
        depthLevel: depthLevel,
        questionId: 'q' + Date.now()
      },
      userAnswer: '',
      showFeedback: false,
      loading: false
    })
  },
  
  // 使用模拟追问
  useMockFollowUp: async function(depthLevel) {
    try {
      // 尝试从API获取跟进问题
      const followUpData = await this.apiServices.getInterviewQuestion(
        this.data.jobType,
        this.data.knowledgeNodes,
        'follow_up',
        depthLevel
      );
      
      let questionContent = '';
      if (followUpData && followUpData.question) {
        questionContent = followUpData.question;
      } else {
        // 使用更通用的默认跟进问题
        questionContent = `请详细展开你对${depthLevel}相关内容的理解和实践经验。`;
      }
      
      this.setData({
        currentQuestion: {
          content: questionContent,
          depthLevel: depthLevel,
          questionId: followUpData?.questionId || 'q' + Date.now()
        },
        userAnswer: '',
        showFeedback: false,
        loading: false
      });
    } catch (error) {
      console.error('获取跟进问题失败，使用默认跟进问题:', error);
      
      this.setData({
        currentQuestion: {
          content: `请详细展开你对${depthLevel}相关内容的理解和实践经验。`,
          depthLevel: depthLevel,
          questionId: 'q' + Date.now()
        },
        userAnswer: '',
        showFeedback: false,
        loading: false
      });
    }
  },
  
  // 准备下一个问题
  prepareNextQuestion: function(nextQuestion) {
    // 重置录音状态
    this.resetRecordingState();
    
    this.setData({ 
      showFeedback: false,
      userAnswer: '',
      recordingUrl: '',
      currentRound: this.data.currentRound + 1,
      consecutiveNoMatchCount: this.data.consecutiveNoMatchCount || 0,
      // 触发问题卡片动画
      animationState: {
        ...this.data.animationState,
        questionCard: 'slide-from-bottom'
      },
      currentQuestion: {
        content: nextQuestion.content,
        depthLevel: nextQuestion.depthLevel,
        questionId: nextQuestion.questionId,
        expectedKeyPoints: nextQuestion.expectedKeyPoints || []
      }
    });
    
    // 更新时间线
    this.updateTimeline(this.data.currentRound, 'active');
    
    // 开始新的回答计时器
    this.startAnswerTimer();
    
    console.log('准备新问题:', nextQuestion.content, '深度级别:', this.data.depthLevels[nextQuestion.depthIndex]);
  },

  // 获取下一个深度等级
  getNextDepth: function(currentDepth) {
    const depthOrder = ['basic', 'intermediate', 'advanced']
    const currentIndex = depthOrder.indexOf(currentDepth)
    return currentIndex < depthOrder.length - 1 ? depthOrder[currentIndex + 1] : currentDepth
  },

  // 获取模拟下一个问题（备用）
  getMockNextQuestion: async function(depth) {
    try {
      // 使用正确的API方法获取面试问题
      const questionData = await this.apiServices.getInterviewQuestion(
        this.data.jobType,
        this.data.knowledgeNodes,
        'technical',
        depth
      );
      
      if (questionData && questionData.question) {
        return {
          content: questionData.question,
          depthLevel: questionData.depthLevel || depth,
          questionId: questionData.questionId || 'q_' + Date.now()
        };
      }
      return this.getDefaultNextQuestion(depth);
    } catch (error) {
      console.error('获取下一个问题失败，使用默认问题:', error);
      return this.getDefaultNextQuestion(depth);
    }
  },

  // 获取默认下一个问题
  getDefaultNextQuestion: function(depth) {
    // 使用更通用的默认问题结构，避免硬编码特定技术问题
    const baseQuestion = {
      content: `请介绍一下${depth}相关的知识和实践经验。`,
      depthLevel: depth,
      questionId: `default_${depth}_${Date.now()}`,
      expectedKeyPoints: [] // 添加空的expectedKeyPoints数组，保持与currentQuestion数据结构一致
    }
    return baseQuestion
  },

  // 跳过当前问题
  skipQuestion: function() {
    wx.showModal({
      title: '跳过确认',
      content: '确定要跳过当前问题吗？',
      success: (res) => {
        if (res.confirm) {
          // 更新会话时间线
          this.updateTimeline(this.data.currentRound, 'skipped')
          
          // 直接进入下一轮
          this.setData({ 
            showFeedback: false,
            userAnswer: '',
            recordingUrl: '',
            currentDepthIndex: 0
          })
          this.prepareNextRound()
        }
      }
    })
  },
  
  // 标记难点
  markAsDifficult: function() {
    if (!this.data.userAnswer.trim()) {
      wx.showToast({ title: '请先输入回答', icon: 'none' });
      return;
    }
    
    const difficultyInfo = {
      round: this.data.currentRound,
      question: this.data.currentQuestion.content,
      timestamp: new Date().getTime()
    };
    
    const markedDifficulties = [...this.data.markedDifficulties, difficultyInfo];
    this.setData({ markedDifficulties });
    
    wx.showToast({ 
      title: '已标记为难点', 
      icon: 'success',
      success: () => {
        // 1秒后自动提交
        setTimeout(() => {
          this.submitAnswer();
        }, 1000);
      }
    });
  },
  
  // 录音转文字功能
  transcribeAudio: function() {
    if (!this.data.recordingUrl) {
      wx.showToast({ title: '请先录制音频', icon: 'none' });
      return;
    }
    
    this.setData({ isTranscribing: true });
    
    // 调用语音转文字服务
    this.services.audio2text.transcribe(this.data.recordingUrl)
      .then(result => {
        this.setData({
          transcriptText: result.text,
          userAnswer: this.data.userAnswer + result.text,
          isTranscribing: false
        });
        wx.showToast({ title: '转写完成', icon: 'success' });
      })
      .catch(err => {
        console.error('语音转文字失败:', err);
        // 使用模拟转写
        this.useMockTranscribe();
      });
  },
  
  // 使用模拟转写
  useMockTranscribe: function() {
    // 使用更通用的默认转录文本，避免硬编码特定内容
    const mockText = this.data.userAnswer || '这是系统生成的默认转录文本，请根据实际情况修改。';
    
    this.setData({
      transcriptText: mockText,
      userAnswer: mockText,
      isTranscribing: false
    });
    wx.showToast({ title: '转写完成', icon: 'success' });
  },

  // 更新进度
  updateProgress: function() {
    try {
      // 基于时间和已答题数的综合进度
      const { sessionSeconds, sessionTimeRemaining, currentRound } = this.data;
      
      // 时间进度
      const timeProgress = (1 - sessionTimeRemaining / sessionSeconds) * 100;
      
      // 答题进度（假设有10个问题为目标）
      const targetQuestions = 10;
      const questionProgress = Math.min((currentRound / targetQuestions) * 100, 100);
      
      // 综合进度，偏向时间进度
      const progress = Math.max(timeProgress, questionProgress * 0.8);
      
      this.setData({ 
        progress: Math.min(Math.floor(progress), 100) // 确保不超过100%并取整
      });
      
      // 检查是否需要显示时间警告
      if (sessionTimeRemaining <= 60 && !this.data.showTimeWarning) {
        this.showTimeWarningOverlay();
      }
      
      if (app.globalData && app.globalData.debug) {
        console.log(`更新进度 - 时间: ${timeProgress.toFixed(1)}%, 答题: ${questionProgress.toFixed(1)}%, 综合: ${progress.toFixed(1)}%`);
      }
    } catch (error) {
      console.error('更新进度错误:', error);
    }
  },

  // 结束面试
  finishInterview: function(stopReason = 'normal') {
    try {
      // 记录停止原因
      this.setData({ stopReason });
      
      // 停止所有计时器并设置为null
      if (this.data.timer) {
        clearInterval(this.data.timer);
        this.setData({ timer: null });
      }
      if (this.data.answerTimer) {
        clearInterval(this.data.answerTimer);
        this.setData({ answerTimer: null });
      }
      
      // 设置状态
      this.setData({
        interviewStatus: 'finished',
        loading: true,
        loadingText: '正在生成面试报告...',
        progress: 100 // 确保进度条显示100%
      });
      
      if (app.globalData && app.globalData.debug) {
        console.log(`面试结束 - 原因: ${stopReason}, 已答题数: ${this.data.currentRound}`);
      }
      
      // 1. 准备面试结果数据
      const interviewResult = this.prepareInterviewResult(stopReason);
      
      // 2. 调用后端API结束面试
      this.callFinishInterviewAPI(interviewResult).then(finishResult => {
        if (finishResult) {
          // 3. 保存面试结果到本地
          this.saveInterviewResult(interviewResult, finishResult);
          
          // 4. 生成成长报告
          this.generateGrowthReport(interviewResult).then(growthReport => {
            // 5. 跳转到报告页面，包含jobType参数
            const jobType = this.data.resumeAnalysis.jobType || this.data.jobType || '';
            wx.redirectTo({
              url: `/pages/report/report?sessionId=${this.data.sessionId}&totalScore=${interviewResult.totalScore}&jobType=${encodeURIComponent(jobType)}`
            });
          }).catch(error => {
            console.error('生成成长报告失败:', error);
            // 即使生成成长报告失败，也跳转到报告页面
            const jobType = this.data.resumeAnalysis.jobType || this.data.jobType || '';
            wx.redirectTo({
              url: `/pages/report/report?sessionId=${this.data.sessionId}&totalScore=${interviewResult.totalScore}&jobType=${encodeURIComponent(jobType)}`
            });
          });
        } else {
          // 处理API调用失败
          this.handleFinishError();
        }
      }).catch(error => {
        console.error('结束面试API调用失败:', error);
        this.handleFinishError();
      });
    } catch (error) {
      console.error('结束面试过程发生错误:', error);
      this.handleFinishError();
    }
  },
  
  // 准备面试结果数据
  prepareInterviewResult: function(stopReason) {
    try {
      const { sessionId, resumeId, resumeAnalysis, persona, userPerformance, interviewHistory, scoringBreakdown, userId } = this.data;
      
      // 计算总时长（秒）
      const sessionSeconds = this.data.sessionSeconds || 900;
      const sessionTimeRemaining = this.data.sessionTimeRemaining || 0;
      const totalDuration = sessionSeconds - sessionTimeRemaining;
      
      // 计算平均分数，确保有兜底值
      const totalScore = userPerformance?.avgScore || this.calculateTotalScore() || 0;
      
      // 获取jobType，确保有多重备选
      const jobType = resumeAnalysis?.jobType || this.data.jobType || '通用面试';
      
      // 获取techItems和projectSummaries用于报告生成
      const techItems = resumeAnalysis?.techItems || [];
      const projectSummaries = resumeAnalysis?.projectSummaries || [];
      
      // 获取面试历史，确保是数组
      const safeInterviewHistory = Array.isArray(interviewHistory) ? interviewHistory : [];
      
      const result = {
        sessionId,
        userId,
        resumeId,
        jobType,
        persona,
        totalScore,
        totalDuration,
        stopReason,
        startTime: safeInterviewHistory[0]?.timestamp || new Date().toISOString(),
        endTime: new Date().toISOString(),
        answerCount: userPerformance?.answers?.length || 0,
        interviewHistory: safeInterviewHistory,
        scoringBreakdown: scoringBreakdown || {},
        techItems,
        projectSummaries,
        // 添加更多有用信息
        currentRound: this.data.currentRound || 1,
        consecutiveNoMatchCount: this.data.consecutiveNoMatchCount || 0,
        questionFocus: this.data.questionFocus || '',
        styleHint: this.data.styleHint || ''
      };
      
      if (app.globalData && app.globalData.debug) {
        console.log('准备面试结果数据:', {
          sessionId,
          jobType,
          totalScore,
          answerCount: result.answerCount,
          stopReason
        });
      }
      
      return result;
    } catch (error) {
      console.error('准备面试结果数据失败:', error);
      // 返回基础数据，确保应用不会崩溃
      return {
        sessionId: this.data.sessionId || '',
        userId: this.data.userId || '',
        jobType: this.data.resumeAnalysis?.jobType || this.data.jobType || '通用面试',
        totalScore: 0,
        answerCount: 0,
        stopReason: 'error',
        startTime: new Date().toISOString(),
        endTime: new Date().toISOString()
      };
    }
  },
  
  // 调用结束面试API
  callFinishInterviewAPI: async function(interviewResult) {
    try {
      const response = await wx.request({
        url: app.globalData.baseUrl + '/api/interview/finish',
        method: 'POST',
        data: {
          sessionId: interviewResult.sessionId,
          stopReason: interviewResult.stopReason,
          totalScore: interviewResult.totalScore,
          totalDuration: interviewResult.totalDuration,
          answerCount: interviewResult.answerCount,
          interviewHistory: interviewResult.interviewHistory
        },
        header: { 'content-type': 'application/json' }
      });
      
      if (response.data && response.data.success) {
        return response.data;
      }
      return null;
    } catch (error) {
      console.error('调用结束面试API失败:', error);
      return null;
    }
  },
  
  // 保存面试结果到本地
  saveInterviewResult: function(interviewResult, finishResult) {
    // 添加报告URL等信息
    const resultToSave = {
      ...interviewResult,
      reportUrl: finishResult.reportUrl || '',
      certificateUrl: finishResult.certificateUrl || ''
    };
    
    // 保存到本地存储
    const history = wx.getStorageSync('interviewHistory') || [];
    history.unshift(resultToSave);
    wx.setStorageSync('interviewHistory', history);
    
    // 保存当前会话ID以便报告页面使用
    wx.setStorageSync(`interview_${interviewResult.sessionId}`, resultToSave);
  },
  
  // 生成成长报告
  generateGrowthReport: async function(interviewResult) {
    try {
      // 获取历史面试记录
      const userId = app.globalData.userInfo?.id || wx.getStorageSync('userId') || '0';
      const history = wx.getStorageSync('interviewHistory') || [];
      
      // 只获取同类型岗位的最近3次面试
      const relevantHistory = history
        .filter(record => record.jobType === interviewResult.jobType)
        .slice(0, 3)
        .map(record => ({
          sessionId: record.sessionId,
          totalScore: record.totalScore,
          timestamp: record.endTime,
          answerCount: record.answerCount
        }));
      
      // 调用后端生成成长报告
      const growthResult = await this.apiServices.getGrowthReport(userId, relevantHistory);
      
      if (growthResult) {
        // 保存成长报告数据
        wx.setStorageSync(`growth_report_${interviewResult.sessionId}`, growthResult);
        return growthResult;
      }
      return null;
    } catch (error) {
      console.error('生成成长报告失败:', error);
      return null;
    }
  },
  
  // 处理结束面试错误
  handleFinishError: function() {
    this.setData({ loading: false });
    wx.showToast({ title: '生成报告失败，将保存本地数据', icon: 'none' });
    
    // 即使API调用失败，也尝试保存本地数据并跳转
    setTimeout(() => {
      this.goBack();
    }, 2000);
  },
  
  // 获取薪资匹配数据
  getSalaryMatch: function() {
    const params = {
      jobType: this.data.jobType,
      city: this.data.city,
      skills: this.extractSkillsFromSession()
    }
    
    app.request('/api/salary/match', 'POST', params, res => {
      if (res && res.code === 0 && res.data) {
        // 保存薪资数据到全局，供报告页使用
        app.globalData.salaryMatchResult = res.data
        
        // 设置薪资匹配结果并触发动画
        this.setData({
          salaryMatchResult: res.data,
          animationState: {
            ...this.data.animationState,
            salaryCard: 'show'
          }
        });
        
        // 保存到用户历史
        this.saveToHistory()
      } else {
        this.setMockSalaryData()
        this.saveToHistory()
      }
    }, err => {
      console.error('获取薪资匹配失败:', err)
      this.setMockSalaryData()
      this.saveToHistory()
    })
  },
  
  // 设置模拟薪资数据
  setMockSalaryData: async function() {
    try {
      // 尝试从API获取薪资匹配数据
      const skills = this.extractSkillsFromSession();
      const salaryData = await this.apiServices.getSalaryMatch(
        this.data.jobType, 
        this.data.city, 
        skills
      );
      
      // 更新薪资匹配结果
      this.setData({
        salaryMatchResult: salaryData
      });
    } catch (error) {
      console.error('获取薪资数据失败，使用默认薪资数据:', error);
      this.setDefaultSalaryData();
    }
  },
  
  // 设置默认薪资数据（备用）
  setDefaultSalaryData: function() {
    // 使用更通用的默认薪资结构，避免硬编码特定行业数据
    const mockSalary = {
      expectedSalary: 10000, // 基础默认值
      marketAverage: 12000,  // 基础默认值
      matchRate: 80,         // 默认匹配度
      suggestion: '请参考市场数据调整您的薪资预期。',
      similarJobs: [],       // 空数组，避免硬编码
      salaryRange: {
        low: 8000,
        mid: 12000,
        high: 20000
      }
    };
    
    this.setData({
      salaryMatchResult: mockSalary
    });
  },
  
  // 提取会话中的技能
  extractSkillsFromSession: function() {
    // 从会话日志中提取技能关键词
    return ['JavaScript', 'Vue', 'React', '算法', '数据结构']
  },
  
  // 保存到用户历史
  saveToHistory: function() {
    const params = {
      userId: this.data.userId,
      sessionId: this.data.sessionId,
      jobType: this.data.jobType,
      city: this.data.city,
      duration: this.data.elapsedTime,
      completedQuestions: this.data.completedQuestions,
      score: this.calculateTotalScore()
    }
    
    // 跳转到报告页，传递jobType和domain
    const reportUrl = `/pages/report/report?sessionId=${this.data.sessionId}&jobType=${encodeURIComponent(this.data.jobType)}&domain=${encodeURIComponent(this.data.domain)}`
    
    app.request('/api/user/history/add', 'POST', params, res => {
      // 无论成功失败都跳转到报告页
      wx.redirectTo({ url: reportUrl })
    }, err => {
      console.error('保存历史失败:', err)
      wx.redirectTo({ url: reportUrl })
    })
  },
  
  // 计算总分
  calculateTotalScore: function() {
    const scores = this.data.sessionTimeline.filter(item => item.status === 'completed')
      .map(item => item.feedback)
      .reduce((total, score) => {
        return {
          tech: total.tech + score.tech,
          logic: total.logic + score.logic,
          clarity: total.clarity + score.clarity,
          depth: total.depth + score.depth
        }
      }, { tech: 0, logic: 0, clarity: 0, depth: 0 })
    
    const count = this.data.sessionTimeline.filter(item => item.status === 'completed').length || 1
    return {
      tech: Math.round(scores.tech / count),
      logic: Math.round(scores.logic / count),
      clarity: Math.round(scores.clarity / count),
      depth: Math.round(scores.depth / count),
      average: Math.round((scores.tech + scores.logic + scores.clarity + scores.depth) / (count * 4))
    }
  },

  // 使用模拟完成结果（备用）
  useMockFinish: function() {
    // 设置模拟薪资数据
    this.setMockSalaryData()
    
    // 计算总分
    const totalScore = this.calculateTotalScore()
    
    // 使用更通用的结构，避免硬编码特定内容
    const mockResult = {
      aggregatedScores: totalScore,
      total_score: totalScore.average * 10,
      sessionLog: this.data.sessionLog
    }
    
    // 保存到全局
    app.globalData.interviewResult = mockResult
    
    // 跳转到报告页面，传递jobType和domain
    wx.redirectTo({
      url: `/pages/report/report?sessionId=${this.data.sessionId}&jobType=${encodeURIComponent(this.data.jobType)}&domain=${encodeURIComponent(this.data.domain)}`
    })
  },

  // 返回上一页
  goBack: function() {
    wx.showModal({
      title: '确认退出',
      content: '退出将丢失当前面试进度，是否确认？',
      success: res => {
        if (res.confirm) {
          wx.navigateBack()
        }
      }
    })
  },
  
  // 显示时间警告覆盖层
  showTimeWarningOverlay: function() {
    this.setData({ showTimeWarning: true });
    // 3秒后自动隐藏警告
    setTimeout(() => {
      this.hideTimeWarningOverlay();
    }, 3000);
  },
  
  // 隐藏时间警告覆盖层
  hideTimeWarningOverlay: function() {
    this.setData({ showTimeWarning: false });
  },
  
  // 重置录音状态
  resetRecordingState: function() {
    // 停止任何正在进行的录音
    const recorder = wx.getRecorderManager();
    recorder.stop();
    
    this.setData({
      recording: false,
      recordingUrl: '',
      transcriptText: '',
      answerDuration: 0
    });
  }
})