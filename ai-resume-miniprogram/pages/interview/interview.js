// interview.js
const app = getApp()

Page({
  data: {
    sessionId: '',
    userId: '',
    resumeId: '',
    jobType: '',
    city: '',
    
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
      depthLevel: 'basic',
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
    sessionLog: []
  },

  onLoad: function(options) {
    const { resumeId, jobType, city } = options || {}
    this.setData({ 
      resumeId, 
      jobType: jobType || '前端开发',
      city: city || '北京',
      userId: app.globalData.userInfo?.id || wx.getStorageSync('userId') || 'test_user'
    })
    
    // 开始新的面试会话
    this.startInterview()
  },

  onUnload: function() {
    // 清理计时器和录音
    if (this.data.timer) {
      clearInterval(this.data.timer)
    }
    this.stopAudioRecording()
  },

  // 开始面试会话
  startInterview: function() {
    this.setData({ loading: true, loadingText: '初始化面试...' })
    
    const params = {
      userId: this.data.userId,
      resumeId: this.data.resumeId,
      jobType: this.data.jobType,
      city: this.data.city,
      sessionParams: {
        maxDepthPerPoint: 3,
        maxFollowups: 6,
        timeLimitSecs: 1200
      }
    }
    
    // 调用后端API开始面试
    app.request('/api/interview/start', 'POST', params, res => {
      if (res && res.code === 0 && res.data) {
        this.setData({
          sessionId: res.data.sessionId,
          currentQuestion: {
            content: res.data.firstQuestion,
            depthLevel: 'basic',
            questionId: 'q1'
          }
        })
        
        // 开始计时
        this.startTimer()
      } else {
        wx.showToast({
          title: '面试初始化失败',
          icon: 'none'
        })
        // 使用模拟问题
        this.useMockQuestion()
      }
    }, err => {
      console.error('开始面试失败:', err)
      wx.showToast({
        title: '网络错误，请重试',
        icon: 'none'
      })
      // 使用模拟问题
      this.useMockQuestion()
    }, () => {
      this.setData({ loading: false })
    })
  },

  // 使用模拟问题（备用）
  useMockQuestion: function() {
    this.setData({
      sessionId: 'mock_session_' + Date.now(),
      currentQuestion: {
        content: '请介绍一下你最熟悉的一个项目，重点说明你负责的部分和使用的技术栈。',
        depthLevel: 'basic',
        questionId: 'mock_q1'
      }
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
      'advanced': '高级'
    }
    return depthMap[depthLevel] || '基础'
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
    
    if (this.data.showFeedback) {
      // 如果已经显示反馈，进入下一轮追问
      this.continueInterview()
    } else {
      // 提交当前回答进行评分
      this.sendAnswer()
    }
  },

  // 发送回答到后端
  sendAnswer: function() {
    this.setData({ loading: true, loadingText: 'AI评分中...' })
    
    const params = {
      sessionId: this.data.sessionId,
      questionId: this.data.currentQuestion.questionId,
      userAnswerText: this.data.userAnswer,
      userAnswerAudioUrl: this.data.recordingUrl || ''
    }
    
    // 调用后端API处理回答
    app.request('/api/interview/answer', 'POST', params, res => {
      if (res && res.code === 0 && res.data) {
        // 保存评分和反馈
        const logEntry = {
          question: this.data.currentQuestion.content,
          answer: this.data.userAnswer,
          score: res.data.perQuestionScore,
          feedback: res.data.feedback,
          matchedPoints: res.data.matchedPoints || []
        }
        
        const sessionLog = [...this.data.sessionLog, logEntry]
        
        this.setData({
          showFeedback: true,
          questionScore: res.data.perQuestionScore,
          feedbackText: res.data.feedback,
          matchedPoints: res.data.matchedPoints || [],
          sessionLog,
          currentQuestion: {
            ...this.data.currentQuestion,
            nextQuestion: res.data.nextQuestion,
            questionId: 'q' + (this.data.currentRound + 1)
          }
        })
        
        // 更新进度
        this.updateProgress()
      } else {
        // 使用模拟评分
        this.useMockScore()
      }
    }, err => {
      console.error('提交回答失败:', err)
      // 使用模拟评分
      this.useMockScore()
    }, () => {
      this.setData({ loading: false })
    })
  },

  // 使用模拟评分（备用）
  useMockScore: function() {
    const mockScore = {
      tech: Math.floor(Math.random() * 5) + 5,
      logic: Math.floor(Math.random() * 5) + 5,
      clarity: Math.floor(Math.random() * 5) + 5,
      depth: Math.floor(Math.random() * 5) + 5
    }
    
    const logEntry = {
      question: this.data.currentQuestion.content,
      answer: this.data.userAnswer,
      score: mockScore,
      feedback: '回答整体不错，但可以进一步深入技术细节。',
      matchedPoints: ['覆盖了基本概念', '逻辑清晰']
    }
    
    const sessionLog = [...this.data.sessionLog, logEntry]
    
    this.setData({
      showFeedback: true,
      questionScore: mockScore,
      feedbackText: '回答整体不错，但可以进一步深入技术细节。',
      matchedPoints: ['覆盖了基本概念', '逻辑清晰'],
      sessionLog
    })
    
    this.updateProgress()
  },

  // 继续面试
  continueInterview: function() {
    // 检查是否达到最大轮数
    if (this.data.currentRound >= this.data.maxRounds) {
      this.finishInterview()
      return
    }
    
    // 准备下一题
    const nextDepth = this.getNextDepth(this.data.currentQuestion.depthLevel)
    
    this.setData({
      showFeedback: false,
      userAnswer: '',
      recordingUrl: '',
      currentRound: this.data.currentRound + 1,
      currentQuestion: {
        content: this.data.currentQuestion.nextQuestion || this.getMockNextQuestion(nextDepth),
        depthLevel: nextDepth,
        questionId: 'q' + (this.data.currentRound + 1)
      }
    })
  },

  // 获取下一个深度等级
  getNextDepth: function(currentDepth) {
    const depthOrder = ['basic', 'intermediate', 'advanced']
    const currentIndex = depthOrder.indexOf(currentDepth)
    return currentIndex < depthOrder.length - 1 ? depthOrder[currentIndex + 1] : currentDepth
  },

  // 获取模拟下一个问题
  getMockNextQuestion: function(depth) {
    const questions = {
      'basic': '请详细说明你在项目中使用的技术栈及其选择原因。',
      'intermediate': '你如何解决项目中遇到的技术难点？请举例说明。',
      'advanced': '你对这个技术的底层原理有什么理解？如何进行性能优化？'
    }
    return questions[depth] || '请继续深入说明你的项目经验。'
  },

  // 跳过当前问题
  skipQuestion: function() {
    wx.showModal({
      title: '确认跳过',
      content: '跳过当前问题将不会获得评分，是否确认？',
      success: res => {
        if (res.confirm) {
          this.continueInterview()
        }
      }
    })
  },

  // 更新进度
  updateProgress: function() {
    const progress = Math.floor((this.data.currentRound / this.data.maxRounds) * 100)
    this.setData({ progress })
  },

  // 完成面试
  finishInterview: function() {
    this.setData({ loading: true, loadingText: '生成报告中...' })
    
    // 调用后端API完成面试
    app.request('/api/interview/finish', 'POST', { sessionId: this.data.sessionId }, res => {
      if (res && res.code === 0 && res.data) {
        // 保存会话数据到全局
        app.globalData.interviewResult = res.data
        
        // 跳转到报告页面
        wx.navigateTo({
          url: '/pages/report/report?sessionId=' + this.data.sessionId
        })
      } else {
        // 使用模拟结果
        this.useMockFinish()
      }
    }, err => {
      console.error('完成面试失败:', err)
      // 使用模拟结果
      this.useMockFinish()
    }, () => {
      this.setData({ loading: false })
    })
  },

  // 使用模拟完成结果（备用）
  useMockFinish: function() {
    const mockResult = {
      aggregatedScores: {
        tech: 7.5,
        logic: 8.0,
        clarity: 7.8,
        depth: 7.2
      },
      total_score: 76,
      salaryInfo: {
        ai_estimated_years: '3-5年',
        ai_salary_range: '20K-28K',
        confidence: 0.85
      },
      sessionLog: this.data.sessionLog
    }
    
    // 保存到全局
    app.globalData.interviewResult = mockResult
    
    // 跳转到报告页面
    wx.navigateTo({
      url: '/pages/report/report?sessionId=' + this.data.sessionId
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