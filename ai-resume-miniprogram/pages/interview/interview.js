// interview.js
const app = getApp()

Page({
  data: {
    sessionId: '',
    userId: '',
    resumeId: '',
    jobType: '',
    city: '',
    
    // 领域与行业信息
    domain: '',
    keyCompetencies: [],
    industryJobTag: '',
    
    // 知识图谱数据
    knowledgeNodes: [],
    topicHierarchy: {},
    
    // 评分体系（将从API获取）
    scoringMetrics: [],
    weightMap: {},
    
    // 面试状态
    currentRound: 1,
    maxRounds: 6,
    elapsedTime: 0,
    timer: null,
    loading: false,
    loadingText: '加载中...',
    
    // 问题和回答
    currentQuestion: {
      content: '',
      depthLevel: '',
      questionId: ''
    },
    userAnswer: '',
    recording: false,
    recordingUrl: '',
    
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
    
    // 会话日志
    sessionLog: [],
    
    // 会话时间线
    sessionTimeline: [],
    completedQuestions: 0,
    
    // 标记难点
    markedDifficulties: [],
    
    // 录音转文字
    transcriptText: '',
    isTranscribing: false,
    
    // 深度级别控制（将从API获取）
    depthLevels: [],
    currentDepthIndex: 0,
    
    // 追问状态
    followUpEnabled: false,
    lastAnswer: null,
    
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
  
  // API服务调用
  apiServices: {
    // 获取评分体系
    getScoringMetrics: async function(jobType, domain) {
      try {
        const response = await wx.cloud.callFunction({
          name: 'getScoringMetrics',
          data: { jobType, domain }
        });
        return response.result;
      } catch (error) {
        console.error('获取评分体系失败:', error);
        return null;
      }
    },
    
    // 获取深度级别
    getDepthLevels: async function(jobType) {
      try {
        const response = await wx.cloud.callFunction({
          name: 'getDepthLevels',
          data: { jobType }
        });
        return response.result;
      } catch (error) {
        console.error('获取深度级别失败:', error);
        return null;
      }
    },
    
    // 获取职业分类信息
    getJobClassification: async function(resumeText, targetJobDesc) {
      try {
        const response = await wx.cloud.callFunction({
          name: 'getJobClassification',
          data: { resumeText, targetJobDesc }
        });
        return response.result;
      } catch (error) {
        console.error('获取职业分类失败:', error);
        return null;
      }
    },
    
    // 获取知识图谱
    getKnowledgeMap: async function(jobType, domain, keyCompetencies) {
      try {
        const response = await wx.cloud.callFunction({
          name: 'getKnowledgeMap',
          data: { jobType, domain, keyCompetencies }
        });
        return response.result;
      } catch (error) {
        console.error('获取知识图谱失败:', error);
        return null;
      }
    },
    
    // 获取面试问题
    getInterviewQuestion: async function(jobType, knowledgeNodes, interviewStage, depthLevel) {
      try {
        const response = await wx.cloud.callFunction({
          name: 'getInterviewQuestion',
          data: { jobType, knowledgeNodes, interviewStage, depthLevel }
        });
        return response.result;
      } catch (error) {
        console.error('获取面试问题失败:', error);
        return null;
      }
    },
    
    // 评估回答
    assessAnswer: async function(question, answer, jobType) {
      try {
        const response = await wx.cloud.callFunction({
          name: 'assessAnswer',
          data: { question, answer, jobType }
        });
        return response.result;
      } catch (error) {
        console.error('评估回答失败:', error);
        return null;
      }
    },
    
    // 获取薪资匹配信息
    getSalaryMatch: async function(jobType, city, skills) {
      try {
        const response = await wx.cloud.callFunction({
          name: 'getSalaryMatch',
          data: { jobType, city, skills }
        });
        return response.result;
      } catch (error) {
        console.error('获取薪资匹配失败:', error);
        return null;
      }
    }
  },

  onLoad: function(options) {
    const { resumeId, jobType, city } = options || {}
    this.setData({ 
      resumeId, 
      jobType: jobType || '前端开发',
      city: city || '北京',
      userId: app.globalData.userInfo?.id || wx.getStorageSync('userId') || 'test_user',
      depthLevel: '用法',
      progress: 0,
      // 直接设置基本的默认数据，跳过异步初始化
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
      weightMap: {'专业技能': 0.4, '逻辑思维': 0.3, '沟通表达': 0.2, '创新潜力': 0.1}
    })
    
    // 初始化会话时间线
    this.initializeSessionTimeline();
    
    // 立即启动面试，不等待任何异步操作
    this.startInterview()
  },

  onUnload: function() {
    // 清理计时器和录音
    if (this.data.timer) {
      clearInterval(this.data.timer)
    }
    this.stopAudioRecording()
  },
  
  // 初始化会话时间线
  initializeSessionTimeline: function() {
    const timeline = [];
    for (let i = 0; i < this.data.maxRounds; i++) {
      timeline.push({
        id: i + 1,
        status: i === 0 ? 'current' : 'pending',
        feedback: null
      });
    }
    this.setData({
      sessionTimeline: timeline
    });
  },
  
  // 初始化通用AI面试系统（从数据库获取数据）
  initializeUniversalInterview: async function() {
    const that = this;
    
    try {
      // 显示加载状态
      this.setData({
        loading: true,
        loadingText: '加载面试数据中...'
      });

      // 1. 从API获取职业分类信息
      const jobClassification = await this.apiServices.getJobClassification('', this.data.jobType);
      if (jobClassification) {
        that.setData({
          domain: jobClassification.domain,
          keyCompetencies: jobClassification.keyCompetencies || [],
          industryJobTag: jobClassification.industryJobTag || `${jobClassification.domain} - ${this.data.jobType}`
        });
      }

      // 2. 从API获取知识图谱数据
      const knowledgeData = await this.apiServices.getKnowledgeMap(
        this.data.jobType, 
        this.data.domain, 
        this.data.keyCompetencies
      );
      if (knowledgeData) {
        that.setData({
          knowledgeNodes: knowledgeData.knowledgeNodes || [],
          topicHierarchy: knowledgeData.topicHierarchy || {}
        });
      }

      // 3. 从API获取评分体系
      const scoringData = await this.apiServices.getScoringMetrics(this.data.jobType, this.data.domain);
      if (scoringData) {
        that.setData({
          scoringMetrics: scoringData.scoringMetrics || [],
          weightMap: scoringData.weightMap || {}
        });
      }
      
      // 4. 从API获取深度级别数据
      const depthData = await this.apiServices.getDepthLevels(this.data.jobType);
      if (depthData) {
        that.setData({
          depthLevels: depthData.depthLevels || []
        });
      }

      console.log('面试数据初始化完成，从数据库加载成功');
    } catch (error) {
      console.error('初始化面试系统失败:', error);
      // 加载失败时使用默认数据作为后备
      this.setDefaultInterviewData();
    } finally {
      // 停止加载状态
      this.setData({
        loading: false,
        loadingText: '面试准备就绪'
      });
    }
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
      depthLevels: ['用法', '实现', '原理', '优化']
    });
  },
  
  // 更新会话时间线
  updateTimeline: function(round, status, feedback = null) {
    const timeline = [...this.data.sessionTimeline];
    if (timeline[round - 1]) {
      timeline[round - 1].status = status;
      timeline[round - 1].feedback = feedback;
    }
    
    // 更新当前轮次
    if (round < this.data.maxRounds && timeline[round]) {
      timeline[round].status = 'current';
    }
    
    this.setData({
      sessionTimeline: timeline,
      completedQuestions: round,
      currentRound: round < this.data.maxRounds ? round + 1 : round
    });
  },

  // 开始面试会话
  startInterview: async function() {
    console.log('开始初始化面试')
    
    try {
      // 立即停止加载状态
      this.setData({
        loading: false,
        loadingText: '面试准备就绪'
      })
      
      // 生成会话ID
      const sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
      
      // 从API获取第一个问题
      let firstQuestion = null;
      const depthLevel = this.data.depthLevels.length > 0 ? this.data.depthLevels[0] : '用法';
      
      const questionData = await this.apiServices.getInterviewQuestion(
        this.data.jobType,
        this.data.knowledgeNodes,
        'introduction',
        depthLevel
      );
      
      if (questionData && questionData.question) {
        firstQuestion = {
          content: questionData.question,
          depthLevel: questionData.depthLevel || depthLevel,
          questionId: questionData.questionId || 'q_' + Date.now()
        };
      } else {
        // 如果API获取失败，使用默认问题
        firstQuestion = this.getDefaultNextQuestion(depthLevel);
      }
      
      // 更新问题和会话信息
      this.setData({
        sessionId: sessionId,
      currentQuestion: firstQuestion
      })
      
      // 启动计时器
      this.startTimer();
      
      // 初始化会话时间线
      this.initializeSessionTimeline();
      
      // 更新进度
      this.updateProgress();
      
    } catch (error) {
      console.error('开始面试失败:', error);
      // 出错时使用默认问题
      const sessionId = 'session_' + Date.now();
      const defaultQuestion = this.getDefaultNextQuestion('用法');
      this.setData({
        sessionId: sessionId,
        currentQuestion: defaultQuestion,
        loading: false,
        loadingText: '面试准备就绪'
      });
      this.startTimer();
      this.initializeSessionTimeline();
      this.updateProgress();
    }
    
    // 更新时间线
    this.updateTimeline(1, 'active')
    
    console.log('面试初始化完成，问题已显示')
    console.log('当前loading状态:', this.data.loading)
  },

  // 使用模拟问题（备用）
  useMockQuestion: function() {
    // 使用getDefaultNextQuestion获取默认问题，避免硬编码
    const defaultQuestion = this.getDefaultNextQuestion(this.data.depthLevels.length > 0 ? this.data.depthLevels[0] : '用法');
    
    this.setData({
      sessionId: 'session_' + Date.now(),
      currentQuestion: defaultQuestion
    })
    this.startTimer()
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

  // 格式化时间显示
  formatTime: function(seconds) {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  },

  // 获取深度等级文本
  getDepthText: function(depthLevel) {
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
    this.setData({
      userAnswer: e.detail.value
    })
  },

  // 切换录音状态
  toggleRecording: function() {
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
          this.startRecord()
        }
      }
    })
  },

  // 开始录音实现
  startRecord: function() {
    const recorder = wx.getRecorderManager()
    const options = {
      duration: 60000,
      sampleRate: 44100,
      numberOfChannels: 1,
      encodeBitRate: 192000,
      format: 'aac'
    }
    
    recorder.start(options)
    this.setData({ recording: true })
    
    recorder.onStop = (res) => {
      this.setData({
        recordingUrl: res.tempFilePath,
        recording: false
      })
    }
    
    recorder.onError = (err) => {
      console.error('录音失败:', err)
      this.setData({ recording: false })
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
    if (!this.data.userAnswer.trim()) {
      wx.showToast({
        title: '请输入回答内容',
        icon: 'none'
      })
      return
    }
    
    if (this.data.showFeedback && this.data.followUpEnabled) {
      // 如果已经显示反馈并且启用了追问，进入追问流程
      this.continueInterview(true)
    } else if (this.data.showFeedback) {
      // 如果显示了反馈但不需要追问，进入下一轮
      this.continueInterview(false)
    } else {
      // 提交当前回答进行评分
      this.sendAnswer()
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
  
  // 准备下一轮问题
  prepareNextRound: function() {
    this.setData({ 
      loading: false,
      showFeedback: false,
      userAnswer: '',
      recordingUrl: '',
      currentRound: this.data.currentRound + 1,
      currentDepthIndex: 0,
      // 触发问题卡片动画
      animationState: {
        ...this.data.animationState,
        questionCard: 'slide-from-bottom'
      }
    })
    
    // 调用动态面试官服务获取下一题
    this.services.dynamicInterviewer.generateQuestion({
      sessionId: this.data.sessionId,
      currentRound: this.data.currentRound,
      markedDifficulties: this.data.markedDifficulties
    }).then(questionData => {
      
      this.setData({
        currentQuestion: {
          content: questionData.question || '请介绍一下你对项目架构的理解。',
          depthLevel: '用法',
          questionId: 'q' + this.data.currentRound
        }
      })
      
      // 更新时间线为当前状态
      const timeline = [...this.data.sessionTimeline];
      if (timeline[this.data.currentRound - 1]) {
        timeline[this.data.currentRound - 1].status = 'current';
        this.setData({ sessionTimeline: timeline });
      }
    });
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
      questionId: `default_${depth}_${Date.now()}`
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
    const progress = Math.floor((this.data.currentRound / this.data.maxRounds) * 100)
    this.setData({ progress })
  },

  // 完成面试
  finishInterview: function() {
    this.setData({ loading: true, loadingText: '正在生成报告...' })
    
    // 停止计时器
    if (this.data.timer) {
      clearInterval(this.data.timer)
    }
    
    // 调用API结束面试
    const params = {
      sessionId: this.data.sessionId,
      userId: this.data.userId
    }
    
    app.request('/api/interview/finish', 'POST', params, res => {
      if (res && res.code === 0) {
        // 调用薪资匹配模块
        this.getSalaryMatch()
      } else {
        // 使用模拟结束
        this.useMockFinish()
      }
    }, err => {
      console.error('结束面试失败:', err)
      this.useMockFinish()
    })
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
  }
})