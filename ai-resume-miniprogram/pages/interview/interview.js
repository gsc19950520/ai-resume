// pages/interview/interview.js
const app = getApp();
import { get, post, requestStream, postStream } from '../../utils/request.js';

Page({
  data: {
    // 核心面试数据，按照要求的格式
    sessionId: "9eee6aff-5d56-4a59-9472-d7461f81db05",
    question: "请简单总结一下你在项目中的主要职责和贡献。",
    questionType: "first_question",
    feedback: null,
    nextQuestion: null,
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

  // 从会话中获取第一个问题（流式）
  fetchFirstQuestionStream(sessionId) {
    return new Promise((resolve, reject) => {
      try {
        this.setData({ isLoading: true });
        
        // 使用流式请求获取第一个问题
        requestStream(`/api/interview/get-first-question-stream/${sessionId}`, {
          onChunk: (chunk) => {
            // 处理流式数据
            console.log('收到问题流数据:', chunk);
            
            // 解析数据，支持多种格式
            try {
              if (typeof chunk === 'string' && chunk.startsWith('data:')) {
                // 处理SSE格式数据
                const data = chunk.substring(5).trim();
                if (data) {
                  const parsedData = JSON.parse(data);
                  if (parsedData.event === 'question') {
                    // 问题内容
                    this.setData({
                      question: parsedData.data
                    });
                  } else if (parsedData.event === 'metadata') {
                    // 元数据
                    console.log('收到元数据:', parsedData.data);
                    // 这里可以存储元数据，用于后续评分
                    this.setData({
                      currentQuestionMetadata: parsedData.data
                    });
                  } else if (parsedData.event === 'end') {
                    // 结束信号
                    this.setData({ isLoading: false });
                    resolve({
                      content: this.data.question,
                      depthLevel: this.data.currentQuestionMetadata?.depthLevel || 'usage'
                    });
                  }
                }
              } else if (typeof chunk === 'object') {
                // 直接处理对象格式
                if (chunk.event === 'question') {
                  this.setData({
                    question: chunk.data
                  });
                } else if (chunk.event === 'metadata') {
                  // 元数据
                  console.log('收到元数据:', chunk.data);
                  // 这里可以存储元数据，用于后续评分
                  this.setData({
                    currentQuestionMetadata: chunk.data
                  });
                } else if (chunk.event === 'end') {
                  this.setData({ isLoading: false });
                  resolve({
                    content: this.data.question,
                    depthLevel: this.data.currentQuestionMetadata?.depthLevel || 'usage'
                  });
                }
              }
            } catch (parseError) {
              console.error('解析流式数据失败:', parseError);
            }
          },
          onError: (error) => {
            console.error('获取第一个问题流式请求失败:', error);
            this.setData({ isLoading: false });
            reject(error);
          },
          onComplete: () => {
            console.log('第一个问题流式请求完成');
          }
        });
      } catch (error) {
        console.error('获取第一个问题失败:', error);
        this.setData({ isLoading: false });
        reject(error);
      }
    });
  },

  onLoad: async function(options) {
    try {
      console.log('interview页面接收到的options:', options);
      
      // 初始化页面数据
      const initialData = {
        formattedTimeRemaining: '10:00'
      };
      
      // 处理URL参数，如果有参数则覆盖默认值
      if (options.sessionId) {
        try {
          const sessionId = decodeURIComponent(options.sessionId);
          
          // 更新核心数据
          initialData.sessionId = sessionId;
          initialData.industryJobTag = options.industryJobTag ? 
                                     decodeURIComponent(options.industryJobTag) : 
                                     app.globalData.latestResumeData?.jobType || 
                                     app.globalData.latestResumeData?.occupation || 
                                     '技术面试';
          
          // 设置面试官语气风格
          initialData.toneStyle = options.persona ? decodeURIComponent(options.persona) : 'friendly';
          
          initialData.questionType = 'first_question';
          
          console.log('从URL参数获取的面试数据:', {
            sessionId: initialData.sessionId,
            industryJobTag: initialData.industryJobTag
          });
          
          // 设置初始数据（此时问题还未获取）
          this.setData(initialData);
          
          // 显示加载提示
          wx.showLoading({ title: '正在生成面试问题...' });
          
          // 使用流式请求获取第一个问题
          try {
            const firstQuestion = await this.fetchFirstQuestionStream(sessionId);
            
            wx.hideLoading();
            
            console.log('获取到第一个面试问题:', firstQuestion.content);
          } catch (error) {
            wx.hideLoading();
            console.error('获取第一个问题失败:', error);
            // 使用默认问题
            this.setData({
              question: '请简单介绍一下你自己，以及你为什么适合这个职位？'
            });
          }
          
        } catch (parseError) {
          console.error('处理参数失败:', parseError);
          wx.hideLoading();
        }
      } else {
        // 如果没有URL参数，尝试从/start接口获取数据
        try {
          wx.showLoading({ title: '正在加载面试数据...' });
          
          // 调用/start接口获取初始面试数据
          const response = await post('/api/interview/start', {
            resumeData: app.globalData.latestResumeData
          });
          
          wx.hideLoading();
          
          if (response.code === 0 || response.code === 200 || (response.message && response.message.toLowerCase() === 'success')) {
            const responseData = response.data || response;
            
            // 确保industryJobTag与question同级处理
            initialData.sessionId = responseData.sessionId;
            initialData.industryJobTag = responseData.industryJobTag || 
                                       app.globalData.latestResumeData?.jobType || 
                                       app.globalData.latestResumeData?.occupation || 
                                       '技术面试';
            initialData.questionType = 'first_question';
            
            // 设置初始数据（此时问题可能为null，需要轮询获取）
            this.setData(initialData);
            
            // 显示生成问题提示
            wx.showLoading({ title: '正在生成面试问题...' });
            
            // 使用流式请求获取第一个问题
          try {
            await this.fetchFirstQuestionStream(initialData.sessionId);
            
            wx.hideLoading();
            
            console.log('从/start接口获取的面试数据:', {
              sessionId: initialData.sessionId,
              industryJobTag: initialData.industryJobTag
            });
          } catch (error) {
            wx.hideLoading();
            console.error('获取第一个问题失败:', error);
            // 使用默认问题
            this.setData({
              question: '请简单介绍一下你自己，以及你为什么适合这个职位？'
            });
          }
          } else {
            wx.hideLoading();
            console.error('获取面试数据失败:', response.message);
            // 使用默认数据作为备选
            initialData.industryJobTag = app.globalData.latestResumeData?.jobType || 
                                       app.globalData.latestResumeData?.occupation || 
                                       '技术面试';
            initialData.question = '请简单介绍一下你自己，以及你为什么适合这个职位？';
          }
        } catch (error) {
          wx.hideLoading();
          console.error('调用/start接口失败:', error);
          // 使用默认数据作为备选
          initialData.industryJobTag = app.globalData.latestResumeData?.jobType || 
                                     app.globalData.latestResumeData?.occupation || 
                                     '技术面试';
          initialData.question = '请简单介绍一下你自己，以及你为什么适合这个职位？';
        }
      }
      
      // 确保question有值
      if (!this.data.question) {
        this.setData({
          question: '请简单介绍一下你自己，以及你为什么适合这个职位？'
        });
      }
      
      // 初始化问答历史，添加当前问题
      const now = new Date();
      const formattedTime = this.formatTime(now);
      
      this.setData({
        interviewHistory: [{
          id: 'q_' + Date.now(),
          type: 'question',
          content: this.data.question,
          timestamp: now.getTime(),
          formattedTime: formattedTime
        }]
      });
      
      console.log('初始化后的interviewHistory:', this.data.interviewHistory);
      console.log('当前面试数据:', {
        question: this.data.question,
        sessionId: this.data.sessionId
      });
      
      // 启动倒计时
      this.startCountdown();
      
    } catch (error) {
      console.error('初始化面试页面失败:', error);
      wx.showToast({
        title: '页面初始化失败，请重试',
        icon: 'none'
      });
      
      // 确保有默认问题
      if (!this.data.question) {
        this.setData({
          question: '请简单介绍一下你自己，以及你为什么适合这个职位？'
        });
      }
    }
  },

  onUnload: function() {
    // 清除定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
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

  // 提交回答（流式）
  submitAnswer: async function() {
    const { userAnswer, sessionId, answerStartTime, toneStyle } = this.data;
    
    // 检查是否有会话ID
    if (!sessionId) {
      wx.showToast({
        title: '面试会话已失效，请重新开始',
        icon: 'none'
      });
      return;
    }
    
    // 如果跳过问题，则允许空回答
    const isSkip = userAnswer === '';
    if (!isSkip && !userAnswer.trim()) {
      wx.showToast({
        title: '请输入您的回答',
        icon: 'none'
      });
      return;
    }
    
    // 计算回答时长（秒）
    let answerDuration = 0;
    if (answerStartTime) {
      answerDuration = Math.floor((Date.now() - answerStartTime) / 1000);
    }
    
    wx.showLoading({ title: '正在处理...' });
    
    try {
      // 更新问答历史，添加用户回答
      const now = new Date();
      const formattedTime = this.formatTime(now);
      
      const newHistoryItem = {
        id: 'a_' + Date.now(),
        type: 'answer',
        content: isSkip ? '跳过此问题' : userAnswer,
        timestamp: now.getTime(),
        formattedTime: formattedTime
      };
      
      const updatedHistory = [...this.data.interviewHistory, newHistoryItem];
      
      this.setData({
        interviewHistory: updatedHistory,
        userAnswer: null,
        // 重置回答时间
        answerStartTime: 0,
        answerDuration: 0
      });
      
      // 使用流式请求提交回答
      postStream('/api/interview/answer-stream', {
        sessionId: sessionId,
        userAnswerText: userAnswer,
        answerDuration: answerDuration,
        toneStyle: toneStyle
      }, {
        onChunk: (chunk) => {
          // 处理流式数据
          console.log('收到回答流数据:', chunk);
          
          // 解析数据
          try {
            if (typeof chunk === 'string' && chunk.startsWith('data:')) {
              // 处理SSE格式数据
              const data = chunk.substring(5).trim();
              if (data) {
                const parsedData = JSON.parse(data);
                if (parsedData.event === 'score') {
                  // 评分反馈
                  this.setData({
                    feedback: parsedData.data
                  });
                } else if (parsedData.event === 'metadata') {
                  // 元数据
                  console.log('收到元数据:', parsedData.data);
                  // 这里可以存储元数据，用于后续评分
                  this.setData({
                    currentQuestionMetadata: parsedData.data
                  });
                } else if (parsedData.event === 'question') {
                  // 下一个问题
                  this.setData({
                    question: parsedData.data
                  });
                } else if (parsedData.event === 'end') {
                  // 结束信号
                  wx.hideLoading();
                  
                  // 更新问题历史
                  const questionHistoryItem = {
                    id: 'q_' + Date.now(),
                    type: 'question',
                    content: this.data.question,
                    metadata: this.data.currentQuestionMetadata,
                    timestamp: now.getTime(),
                    formattedTime: formattedTime
                  };
                  
                  updatedHistory.push(questionHistoryItem);
                  
                  // 更新当前问题数据
                  this.setData({
                    questionType: 'follow_up',
                    interviewHistory: updatedHistory,
                    animationState: { questionCard: 'fade-in' }
                  });
                  
                  // 重置动画状态
                  setTimeout(() => {
                    this.setData({
                      animationState: { questionCard: 'idle' }
                    });
                  }, 1000);
                  
                  // 滚动到最新记录
                  this.scrollToBottom();
                } else if (parsedData.event === 'interview_end') {
                  // 面试结束
                  wx.hideLoading();
                  this.setData({
                    isCompleted: true
                  });
                  
                  wx.showToast({
                    title: '面试已结束',
                    icon: 'success'
                  });
                  
                  setTimeout(() => {
                    this.finishInterview();
                  }, 1500);
                }
              }
            }
          } catch (parseError) {
            console.error('解析流式数据失败:', parseError);
          }
        },
        onError: (error) => {
          console.error('提交回答流式请求失败:', error);
          wx.hideLoading();
          wx.showToast({
            title: '网络错误，请重试',
            icon: 'none'
          });
        },
        onComplete: () => {
          console.log('提交回答流式请求完成');
        }
      });
    } catch (error) {
      wx.hideLoading();
      console.error('提交回答失败:', error);
      wx.showToast({
        title: '网络错误，请重试',
        icon: 'none'
      });
    }
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