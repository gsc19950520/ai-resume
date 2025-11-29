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
    
    // 更新sessionId和toneStyle
    this.setData({
      sessionId: sessionId,
      toneStyle: toneStyle
    });
    
    // 启动倒计时
    this.startCountdown();
    
    // 调用方法获取第一个问题（流式）
    this.fetchFirstQuestionStream(sessionId);
  },
  
  onUnload: function() {
    // 清除定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
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
                      question: this.data.question + this.currentData
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
                      question: this.data.question + this.currentData
                    });
                    // 重置当前data
                    this.currentData = '';
                  } else if (this.currentEvent === 'end') {
                    // 结束信号
                    this.setData({ isLoading: false });
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
        
        if (remaining <= 0) {
          // 面试时间结束
          clearInterval(this.timer);
          this.finishInterview();
        } else if (remaining <= 300 && !this.data.showTimeWarning) { // 5分钟警告
          this.setData({
            showTimeWarning: true
          });
        }
        
        // 当剩余时间少于60秒时，显示警告覆盖层1秒后自动隐藏
        if (remaining <= 60 && !this.data.showTimeWarningOverlay) {
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
        
        this.setData({
          sessionTimeRemaining: remaining,
          formattedTimeRemaining: this.formatRemainingTime(remaining)
        });
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
    const { sessionId } = this.data;
    
    wx.showLoading({ title: '正在生成报告...' });
    
    // 清除定时器
    if (this.data.timer) {
      clearInterval(this.data.timer);
    }
    
    // 调用API结束面试并生成报告
    post('/api/interview/finish', { sessionId: sessionId })
      .then(response => {
        wx.hideLoading();
        
        if (response.code === 0 || response.code === 200 || (response.message && response.message.toLowerCase() === 'success')) {
          // 跳转到面试报告页面
          wx.redirectTo({
            url: `/pages/interview/report?sessionId=${encodeURIComponent(sessionId)}`
          });
        } else {
          wx.showToast({
            title: response.message || '生成报告失败',
            icon: 'none'
          });
        }
      })
      .catch(error => {
        wx.hideLoading();
        console.error('结束面试失败:', error);
        wx.showToast({
          title: '网络错误，请重试',
          icon: 'none'
        });
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
    
    // 创建历史记录项
    const historyItem = {
      id: `history-${Date.now()}`,
      question: question,
      userAnswer: userAnswer,
      feedback: feedback,
      timestamp: new Date().toISOString()
    };
    
    // 更新历史记录
    const updatedHistory = [...this.data.interviewHistory, historyItem];
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