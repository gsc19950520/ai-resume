// pages/interview/interview.js
const app = getApp();
import { get, post, postStream, getStream } from '../../utils/request';

Page({
  data: {
    // 核心面试数据，按照要求的格式
    sessionId: "9eee6aff-5d56-4a59-9472-d7461f81db05",
    question: "请简单总结一下你在项目中的主要职责和贡献。",
    questionType: "first_question",
    feedback: '', // 初始化为空字符串以便流式拼接
    nextQuestion: '', // 初始化为空字符串以便流式拼接
    isCompleted: false,
    hasQuestion: false,
    
    // 页面状态数据（保留必要的界面相关数据）
    industryJobTag: '',
    sessionTimeRemaining: 600, // 默认10分钟，单位秒
    sessionSeconds: 600,
    formattedTimeRemaining: '10:00', // 格式化的剩余时间
    isPaused: false,
    showFeedback: false,
    userAnswer: null,
    isLoading: false, // 用于控制加载状态
    
    // 回答时间计算
    answerStartTime: 0, // 记录回答开始时间
    answerDuration: 0, // 回答时长（秒）
    
    // 面试官语气风格
    toneStyle: 'friendly', // 默认语气风格
    
    // 动画状态
    animationState: {
      questionCard: 'idle'
    },
    
    // 问答历史记录
    interviewHistory: [],
    isHistoryExpanded: false, // 问答记录展开状态，默认为收起
    hasFetchedHistory: false, // 是否已获取历史记录
    
    // 显示时间警告
    showTimeWarning: false,
    // 显示时间警告覆盖层
    showTimeWarningOverlay: false,
    
    // 滚动控制
    scrollIntoView: '', // 用于scroll-view自动滚动
    
    // 倒计时定时器（非数据绑定字段，在onUnload中清除）
    // timer: null - 移到实例属性中
  },

  onLoad: function(options) {
    console.log('onLoad options:', options);
    
    // 从URL参数获取sessionId
    let sessionId = options.sessionId || this.data.sessionId;
    
    // 从URL参数获取toneStyle，如果有则更新
    let toneStyle = options.toneStyle || this.data.toneStyle;
    
    // 从URL参数获取industryJobTag，如果有则更新，并解码
    let industryJobTag = options.industryJobTag ? decodeURIComponent(options.industryJobTag) : this.data.industryJobTag;
    
    // 从URL参数获取剩余时间，如果有则使用，否则使用默认值
    let sessionTimeRemaining = options.sessionTimeRemaining ? parseInt(options.sessionTimeRemaining) : this.data.sessionTimeRemaining;
    
    // 更新sessionId、toneStyle、industryJobTag和剩余时间
    this.setData({
      sessionId: sessionId,
      toneStyle: toneStyle,
      industryJobTag: industryJobTag,
      sessionTimeRemaining: sessionTimeRemaining,
      formattedTimeRemaining: this.formatRemainingTime(sessionTimeRemaining)
    });
    
    // 获取后端配置（包括默认会话时长）
    this.fetchBackendConfig().then(() => {
      // 获取面试会话详情，包括剩余时间和历史记录
      this.fetchSessionDetail(sessionId);
    });
    
    // 启动每隔5秒更新后端剩余时间的定时器
    this.backendTimeUpdateTimer = setInterval(() => {
      this.updateRemainingTimeToServer();
    }, 5000);
  },
  
  /**
   * 获取后端配置（包括默认会话时长）
   */
  fetchBackendConfig: function() {
    return get('/api/interview/get-config')
      .then(res => {
        if (res && res.success && res.data) {
          const config = res.data;
          console.log('获取到后端配置:', config);
          
          // 如果返回了默认会话时长，则更新页面数据
          if (config.defaultSessionSeconds) {
            this.setData({
              sessionSeconds: config.defaultSessionSeconds
            });
          }
        }
      })
      .catch(error => {
        console.error('获取后端配置失败:', error);
        // 失败时不影响页面加载，继续使用默认值
      });
  },
  
  /**
   * 获取面试会话详情
   * @param {string} sessionId - 会话ID
   */
  fetchSessionDetail: function(sessionId) {
    this.setData({ isLoading: true });
    
    get(`/api/interview/detail/${sessionId}`)
      .then(res => {
        if (res && res.success && res.data) {
          const sessionDetail = res.data;
          console.log('获取到会话详情:', sessionDetail);
          
          // 更新剩余时间
          const timeRemaining = sessionDetail.sessionTimeRemaining || 600;
          this.setData({
            sessionTimeRemaining: timeRemaining,
            sessionSeconds: sessionDetail.sessionSeconds || 600,
            formattedTimeRemaining: this.formatRemainingTime(timeRemaining)
          });
          
          // 启动倒计时
          this.startCountdown();
          
          // 检查是否已经有问题
          if (sessionDetail.hasQuestion) {
            // 已经有问题，获取当前问题和历史记录
            this.setData({
              question: sessionDetail.currentQuestion || '',
              questionType: 'continue_question',
              hasQuestion: true
            });
            
            // 获取面试历史记录
            this.fetchInterviewHistory(sessionId);
          } else {
            // 没有问题，获取第一个问题
            this.fetchFirstQuestionStream(sessionId);
          }
        } else {
          // 获取会话详情失败，使用默认值并获取第一个问题
          this.startCountdown();
          this.fetchFirstQuestionStream(sessionId);
        }
      })
      .catch(error => {
        console.error('获取会话详情失败:', error);
        // 失败时使用默认值并获取第一个问题
        this.startCountdown();
        this.fetchFirstQuestionStream(sessionId);
      })
      .finally(() => {
        this.setData({ isLoading: false });
      });
  },
  
  /**
   * 获取面试历史记录
   * @param {string} sessionId - 会话ID
   */
  fetchInterviewHistory: function(sessionId) {
    get(`/api/interview/history/${sessionId}`)
      .then(res => {
        if (res && res.success && res.data) {
          const history = res.data;
          console.log('获取到面试历史记录:', history);
          
          // 格式化历史记录，确保与前端添加的记录格式一致
          const formattedHistory = history.map(item => ({
            id: `history-${item.id}`, // 使用后端ID但保持前端ID格式
            type: item.type,
            content: item.content,
            formattedTime: item.formattedTime,
            feedback: item.feedback,
            techScore: item.techScore,
            logicScore: item.logicScore,
            clarityScore: item.clarityScore,
            depthScore: item.depthScore,
            roundNumber: item.roundNumber
          }));
          
          // 过滤掉当前问题，避免在历史记录中重复显示
          const filteredHistory = formattedHistory.filter(item => {
            // 如果当前有问题，并且历史记录中的问题与当前问题内容相同，则过滤掉
            if (this.data.question && item.type === 'question' && item.content === this.data.question) {
              return false;
            }
            return true;
          });
          
          // 更新面试历史记录
          this.setData({
            interviewHistory: filteredHistory,
            hasFetchedHistory: true
          });
        }
      })
      .catch(error => {
        console.error('获取面试历史记录失败:', error);
      });
  },
  
  onUnload: function() {
    // 清除定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    
    // 清除后端时间更新定时器
    if (this.backendTimeUpdateTimer) {
      clearInterval(this.backendTimeUpdateTimer);
      this.backendTimeUpdateTimer = null;
    }
  },
  
  /**
   * 更新后端session表中的剩余时间
   */
  updateRemainingTimeToServer: function() {
    const { sessionId, sessionTimeRemaining } = this.data;
    
    // 只有在页面未暂停且面试未完成时才更新
    if (!this.data.isPaused && !this.data.isCompleted) {
      // 调用接口更新后端剩余时间
      post('/api/interview/update-remaining-time', {
        sessionId: sessionId,
        sessionTimeRemaining: sessionTimeRemaining
      })
      .then(res => {
        if (res && res.success) {
          console.log('后端剩余时间更新成功:', res.data);
        } else {
          console.error('后端剩余时间更新失败:', res.message || '未知错误');
        }
      })
      .catch(error => {
        console.error('更新后端剩余时间请求失败:', error);
      });
    }
  },
  
  // 从会话中获取第一个问题（流式）
  fetchFirstQuestionStream(sessionId) {
    // 添加实例变量用于累积SSE数据
    if (!this.currentEvent) this.currentEvent = '';
    if (!this.currentData) this.currentData = '';
    
    return new Promise((resolve, reject) => {
      try {
      // 开始流式请求前清空question字段，设置isLoading为true
      this.setData({ isLoading: true, question: '' });
        
        // 使用流式请求获取第一个问题
        getStream(`/api/interview/get-first-question-stream/${sessionId}`, {}, {
          onChunk: (chunk) => {
            // 处理流式数据
            console.log('收到问题流数据:', chunk);
            
            // 解析数据，支持多种格式
            try {
              if (typeof chunk === 'string') {
                // 处理字符串格式数据，每条chunk是SSE的一行
                const trimmedChunk = chunk.trim();
                
                if (trimmedChunk.startsWith('event:')) {
                  // 新的event，先处理之前的event和data（如果有）
                  if (this.currentEvent === 'question' && this.currentData) {
                    // 累积问题内容（流式展示，逐字/逐词添加）
                    this.setData({
                      // 当isLoading为true时，wxml显示的是nextQuestion，所以需要更新nextQuestion
                      nextQuestion: this.data.nextQuestion + this.currentData
                    });
                    // 重置当前data
                    this.currentData = '';
                  }
                  // 更新当前event
                  this.currentEvent = trimmedChunk.substring(6).trim();
                } else if (trimmedChunk.startsWith('data:')) {
                  // 提取data内容，去除前面的'data:'前缀
                  const dataContent = trimmedChunk.substring(5).trim();
                  // 检查是否是结束标记
                  if (dataContent === 'end' && this.currentEvent === 'question') {
                    // 处理完所有数据，等待event:end
                    return;
                  }
                  // 累积data内容
                  this.currentData = dataContent;
                } else if (trimmedChunk === '') {
                  // 空行表示当前SSE消息结束，处理累积的event和data
                  if (this.currentEvent === 'question' && this.currentData) {
                    // 累积问题内容（流式展示，逐字/逐词添加）
                    this.setData({
                      // 当isLoading为true时，wxml显示的是nextQuestion，所以需要更新nextQuestion
                      nextQuestion: this.data.nextQuestion + this.currentData
                    });
                    // 重置当前data
                    this.currentData = '';
                  } else if (this.currentEvent === 'end') {
                    // 结束信号，将nextQuestion的值赋给question，然后清空nextQuestion
                    this.setData({ 
                      isLoading: false,
                      question: this.data.nextQuestion,
                      nextQuestion: ''
                    });
                    resolve({
                      content: this.data.question
                    });
                    // 重置状态
                    this.currentEvent = '';
                    this.currentData = '';
                  }
                }
              } else if (typeof chunk === 'object') {
                // 如果是对象格式，直接处理
                if (chunk.event === 'question' && chunk.data) {
                  this.setData({
                    question: this.data.question + chunk.data
                  });
                } else if (chunk.event === 'end') {
                  this.setData({ isLoading: false });
                  resolve({
                    content: this.data.question
                  });
                }
              }
            } catch (parseError) {
              console.error('解析流式数据失败:', parseError);
            }
          },
          onError: (error) => {
            console.error('获取第一个问题失败:', error);
            this.setData({ isLoading: false });
            reject(error);
            // 显示错误提示
            wx.showToast({
              title: '获取面试问题失败',
              icon: 'none'
            });
          },
          onComplete: () => {
            console.log('获取第一个问题流式请求完成');
            // 确保即使没有收到end事件，也能正确处理
            this.setData({ isLoading: false });
            resolve({
              content: this.data.question
            });
            // 重置状态
            this.currentEvent = '';
            this.currentData = '';
          }
        });
      } catch (error) {
        console.error('发起获取第一个问题请求失败:', error);
        this.setData({ isLoading: false });
        reject(error);
        // 显示错误提示
        wx.showToast({
          title: '获取面试问题失败',
          icon: 'none'
        });
      }
    });
  },

  // 启动倒计时
  startCountdown: function() {
    // 如果已经有定时器在运行，先清除
    if (this.timer) {
      clearInterval(this.timer);
    }
    
    // 立即更新一次时间显示
    this.setData({
      formattedTimeRemaining: this.formatRemainingTime(this.data.sessionTimeRemaining)
    });
    
    // 使用实例属性而非data属性存储定时器
    this.timer = setInterval(() => {
      if (!this.data.isPaused) {
        let remaining = this.data.sessionTimeRemaining - 1;
        
        // 先更新剩余时间数据
        this.setData({
          sessionTimeRemaining: remaining,
          formattedTimeRemaining: this.formatRemainingTime(remaining)
        });
        
        if (remaining <= 0) {
          // 面试时间结束
          clearInterval(this.timer);
          this.finishInterview();
        } else if (remaining <= 300 && !this.data.showTimeWarning) { // 5分钟警告
          this.setData({
            showTimeWarning: true
          });
        }
        
        // 当剩余时间恰好等于60秒时，显示警告覆盖层1秒后自动隐藏，只显示一次
        if (remaining === 60) {
          this.setData({
            showTimeWarningOverlay: true
          });
          // 1秒后隐藏警告覆盖层
          setTimeout(() => {
            this.setData({
              showTimeWarningOverlay: false
            });
          }, 1000);
        }
      }
    }, 1000);
  },

  // 格式化剩余时间
  formatRemainingTime: function(seconds) {
    try {
      // 确保seconds是数字
      seconds = parseInt(seconds) || 0;
      const minutes = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    } catch (error) {
      console.error('格式化时间失败:', error);
      return '00:00';
    }
  },

  // 格式化时间
  formatTime: function(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  // 返回上一页
  goBack: function() {
    // 清除所有定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    
    if (this.backendTimeUpdateTimer) {
      clearInterval(this.backendTimeUpdateTimer);
      this.backendTimeUpdateTimer = null;
    }
    
    wx.navigateBack();
  },

  // 用户回答输入
  onUserAnswerInput: function(e) {
    const value = e.detail.value;
    
    // 如果是第一次输入，记录回答开始时间
    if (!this.data.answerStartTime && value) {
      this.setData({
        answerStartTime: Date.now()
      });
    }
    
    this.setData({
      userAnswer: value || null // 如果输入为空字符串，则设置为null
    });
  },
  
  // 重置回答时间
  resetAnswerTime: function() {
    this.setData({
      answerStartTime: 0,
      answerDuration: 0
    });
  },

  // 暂停/继续面试
  togglePause: function() {
    this.setData({
      isPaused: !this.data.isPaused
    });
    
    wx.showToast({
      title: this.data.isPaused ? '面试已暂停' : '面试已继续',
      icon: 'none',
      duration: 1500
    });
  },

  // 结束面试
  finishInterview: function() {
    const { sessionId, interviewHistory } = this.data;
    
    // 清除所有定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    
    if (this.backendTimeUpdateTimer) {
      clearInterval(this.backendTimeUpdateTimer);
      this.backendTimeUpdateTimer = null;
    }
    
    // 检查是否有问答记录
    if (interviewHistory.length === 0) {
      // 没有问答记录，直接返回首页
      wx.redirectTo({
        url: '/pages/index/index'
      });
      return;
    }
    
    // 有问答记录，生成报告
    wx.showLoading({ title: '正在生成报告...' });
    
    // 获取最后一题的回答内容
    const lastAnswer = this.data.userAnswer || '';
    
    // 跳转到报告页面，并传递sessionId和最后一题的回答内容，报告页面将使用流式API获取报告内容
    wx.redirectTo({
      url: `/pages/report/report?sessionId=${encodeURIComponent(sessionId)}&useStream=true&lastAnswer=${encodeURIComponent(lastAnswer)}`
    });
  },

  // 提交用户回答，获取下一个问题（流式）
  submitAnswer: function() {
    const { sessionId, userAnswer } = this.data;
    
    // 验证输入
    if (!userAnswer) {
      wx.showToast({
        title: '请输入回答内容',
        icon: 'none'
      });
      return;
    }
    
    // 计算回答时长
    let answerDuration = 0;
    if (this.data.answerStartTime) {
      answerDuration = Math.floor((Date.now() - this.data.answerStartTime) / 1000);
    }
    
    // 准备请求数据
    const requestData = {
      sessionId: sessionId,
      userAnswerText: userAnswer,
      answerDuration: answerDuration,
      toneStyle: this.data.toneStyle
    };
    
    // 添加实例变量用于累积SSE数据
    if (!this.currentEvent) this.currentEvent = '';
    if (!this.currentData) this.currentData = '';
    
    // 开始请求前的状态设置
    this.setData({
      isLoading: true,
      nextQuestion: '', // 清空下一个问题
      feedback: '' // 清空反馈，初始化为空字符串以便流式拼接
    });
    
    try {
      // 使用流式请求提交回答
      postStream('/api/interview/answer-stream', requestData, {
        onChunk: (chunk) => {
          // 处理流式数据
          console.log('收到回答流数据:', chunk);
          
          // 解析数据，支持多种格式
          try {
            if (typeof chunk === 'string') {
              // 处理字符串格式数据，每条chunk是SSE的一行
              const trimmedChunk = chunk.trim();
              
              if (trimmedChunk.startsWith('event:')) {
                  // 新的event，先处理之前的event和data（如果有）
                  if (this.currentEvent === 'feedback' && this.currentData) {
                    // 累积反馈内容
                    this.setData({
                      feedback: this.data.feedback + this.currentData
                    });
                    // 重置当前data
                    this.currentData = '';
                  } else if (this.currentEvent === 'question' && this.currentData) {
                    // 累积下一个问题内容（流式展示，逐字/逐词添加）
                    this.setData({
                      nextQuestion: this.data.nextQuestion + this.currentData
                    });
                    // 重置当前data
                    this.currentData = '';
                  }
                  // 更新当前event
                  this.currentEvent = trimmedChunk.substring(6).trim();
                } else if (trimmedChunk.startsWith('data:')) {
                  // 提取data内容，去除前面的'data:'前缀
                  const dataContent = trimmedChunk.substring(5).trim();
                  // 检查是否是结束标记
                  if (dataContent === 'end') {
                    // 处理完所有数据，等待event:end
                    return;
                  }
                  // 累积data内容
                  this.currentData = dataContent;
                } else if (trimmedChunk === '') {
                  // 空行表示当前SSE消息结束，处理累积的event和data
                  if (this.currentEvent === 'feedback' && this.currentData) {
                    // 累积反馈内容
                    this.setData({
                      feedback: this.data.feedback + this.currentData
                    });
                    // 重置当前data
                    this.currentData = '';
                  } else if (this.currentEvent === 'question' && this.currentData) {
                    // 累积下一个问题内容（流式展示，逐字/逐词添加）
                    this.setData({
                      nextQuestion: this.data.nextQuestion + this.currentData
                    });
                    // 重置当前data
                    this.currentData = '';
                  }
                }
            } else if (typeof chunk === 'object') {
              // 如果是对象格式，直接处理
              if (chunk.event === 'feedback' && chunk.data) {
                this.setData({
                  feedback: this.data.feedback + chunk.data
                });
              } else if (chunk.event === 'question' && chunk.data) {
                this.setData({
                  nextQuestion: this.data.nextQuestion + chunk.data
                });
              }
            }
          } catch (parseError) {
            console.error('解析流式数据失败:', parseError);
          }
        },
        onError: (error) => {
          console.error('提交回答失败:', error);
          this.setData({ isLoading: false });
          // 显示错误提示
          wx.showToast({
            title: '提交回答失败',
            icon: 'none'
          });
        },
        onComplete: () => {
          console.log('提交回答流式请求完成');
          // 确保即使没有收到end事件，也能正确处理
          this.setData({ isLoading: false });
          // 重置状态
          this.currentEvent = '';
          this.currentData = '';
          
          // 重置回答时间
          this.resetAnswerTime();
          
          // 将当前问答添加到历史记录
          this.addToInterviewHistory();
          
        }
      });
    } catch (error) {
      console.error('发起提交回答请求失败:', error);
      this.setData({ isLoading: false });
      // 显示错误提示
      wx.showToast({
        title: '提交回答失败',
        icon: 'none'
      });
    }
  },
  
  // 将当前问答添加到历史记录
  addToInterviewHistory: function() {
    const { question, userAnswer, feedback } = this.data;
    const now = new Date().toISOString();
    const formattedTime = this.formatHistoryTime(now);
    
    // 创建历史记录数组，包含问题和回答
    const newHistoryItems = [];
    
    // 添加问题记录
    newHistoryItems.push({
      id: `history-${Date.now()}-question`,
      type: 'question',
      content: question,
      formattedTime: formattedTime,
      timestamp: now
    });
    
    // 添加回答记录
    if (userAnswer) {
      newHistoryItems.push({
        id: `history-${Date.now()}-answer`,
        type: 'answer',
        content: userAnswer,
        formattedTime: formattedTime,
        timestamp: now
      });
    }
    
    // 更新历史记录
    const updatedHistory = [...this.data.interviewHistory, ...newHistoryItems];
    this.setData({
      interviewHistory: updatedHistory,
      // 更新当前问题为下一个问题
      question: this.data.nextQuestion,
      // 清空用户回答
      userAnswer: null,
      // 清空下一个问题
      nextQuestion: ''
    });
    
    // 滚动到历史记录底部
    this.scrollToBottom();
  },
  
  // 切换问答记录展开/收起状态
  toggleHistory: function() {
    const isExpanded = this.data.isHistoryExpanded;
    
    // 切换展开状态
    this.setData({
      isHistoryExpanded: !isExpanded
    });
  },
  
  
  // 格式化面试记录
  formatInterviewHistory: function(historyData) {
    // 这里根据实际返回的数据结构进行格式化
    // 假设返回的数据是包含question和answer的数组
    const formatted = [];
    let idCounter = 0;
    
    // 遍历历史记录，转换为需要的格式
    historyData.forEach(item => {
      // 添加问题记录
      formatted.push({
        id: `history-${idCounter++}`,
        type: 'question',
        content: item.question,
        formattedTime: this.formatHistoryTime(item.timestamp)
      });
      
      // 添加回答记录
      if (item.userAnswer) {
        formatted.push({
          id: `history-${idCounter++}`,
          type: 'answer',
          content: item.userAnswer,
          formattedTime: this.formatHistoryTime(item.answerTimestamp)
        });
      }
    });
    
    return formatted;
  },
  
  // 格式化历史记录时间
  formatHistoryTime: function(timestamp) {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    
    return `${hours}:${minutes}`;
  },
  
  // 滚动到历史记录底部
  scrollToBottom: function() {
    try {
      // 获取最新一条记录的id
      const historyLength = this.data.interviewHistory.length;
      if (historyLength > 0) {
        const latestRecord = this.data.interviewHistory[historyLength - 1];
        if (latestRecord && latestRecord.id) {
          // 设置scrollIntoView为最新记录的id，scroll-view会自动滚动到该元素
          this.setData({
            scrollIntoView: latestRecord.id
          });
        }
      }
    } catch (error) {
      console.error('滚动到底部失败:', error);
    }
  }
});