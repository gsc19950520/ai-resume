// pages/interview/interview.js
const app = getApp();
import { post } from '../../utils/request.js';

Page({
  data: {
    // 核心面试数据，按照要求的格式
    sessionId: "9eee6aff-5d56-4a59-9472-d7461f81db05",
    question: "请简单总结一下你在项目中的主要职责和贡献。",
    questionType: "first_question",
    feedback: null,
    nextQuestion: null,
    isCompleted: false,
    
    // 页面状态数据（保留必要的界面相关数据）
    industryJobTag: '',
    sessionTimeRemaining: 600, // 默认10分钟，单位秒
    sessionSeconds: 600,
    formattedTimeRemaining: '10:00', // 格式化的剩余时间
    isPaused: false,
    showFeedback: false,
    userAnswer: '',
    
    // 动画状态
    animationState: {
      questionCard: 'idle'
    },
    
    // 问答历史记录
    interviewHistory: [],
    
    // 显示时间警告
    showTimeWarning: false,
    
    // 倒计时定时器（非数据绑定字段，在onUnload中清除）
    // timer: null - 移到实例属性中
  },

  onLoad: function(options) {
    try {
      console.log('interview页面接收到的options:', options);
      
      // 初始化页面数据
      const initialData = {
        industryJobTag: app.globalData.latestResumeData?.jobType || 
                        app.globalData.latestResumeData?.occupation || 
                        '技术面试',
        formattedTimeRemaining: '10:00'
      };
      
      // 处理URL参数，如果有参数则覆盖默认值
      if (options.firstQuestion && options.sessionId) {
        try {
          // 尝试解析问题数据
          let firstQuestion;
          try {
            firstQuestion = JSON.parse(decodeURIComponent(options.firstQuestion));
          } catch (parseError) {
            // 如果解析失败，将其作为问题文本
            const questionText = decodeURIComponent(options.firstQuestion);
            firstQuestion = { content: questionText };
          }
          
          const sessionId = decodeURIComponent(options.sessionId);
          
          // 更新核心数据
          initialData.sessionId = sessionId;
          initialData.question = firstQuestion.content || 
                               firstQuestion.question || 
                               firstQuestion || 
                               '请简单介绍一下你自己。';
          initialData.questionType = 'first_question';
          
          console.log('更新后的面试数据:', {
            sessionId: initialData.sessionId,
            question: initialData.question
          });
          
        } catch (parseError) {
          console.error('处理参数失败:', parseError);
        }
      } else {
        console.log('使用默认面试数据');
      }
      
      // 设置初始数据
      this.setData(initialData);
      
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

  // 获取面试官风格文本
  getPersonaText: function(persona) {
    const personaMap = {
      'technical': '技术面试官',
      'hr': 'HR面试官',
      'manager': '业务经理',
      'architect': '架构师',
      'cto': 'CTO'
    };
    return personaMap[persona] || '面试官';
  },

  // 返回上一页
  goBack: function() {
    wx.navigateBack();
  },

  // 用户回答输入
  onUserAnswerInput: function(e) {
    this.setData({
      userAnswer: e.detail.value
    });
  },

  // 提交回答
  submitAnswer: async function() {
    const { userAnswer, sessionId } = this.data;
    
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
    
    wx.showLoading({ title: '正在处理...' });
    
    try {
      // 调用API提交回答
      const response = await post('/api/interview/submit-answer', {
        sessionId: sessionId,
        answer: userAnswer
        // 移除questionId参数，因为新的数据结构中没有这个字段
      });
      
      wx.hideLoading();
      
      if (response.code === 0 || response.code === 200 || (response.message && response.message.toLowerCase() === 'success')) {
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
        
        // 获取下一个问题，处理可能的不同数据结构
        const responseData = response.data || response;
        const nextQuestion = responseData.nextQuestion || responseData.question;
        
        if (nextQuestion) {
          // 处理下一个问题，支持多种格式
          const nextQuestionContent = typeof nextQuestion === 'string' ? nextQuestion : 
                                     nextQuestion.content || nextQuestion.question || '';
          
          // 更新问题历史
          const questionHistoryItem = {
            id: 'q_' + Date.now(),
            type: 'question',
            content: nextQuestionContent,
            timestamp: now.getTime(),
            formattedTime: formattedTime
          };
          
          updatedHistory.push(questionHistoryItem);
          
          // 更新当前问题数据（使用新的数据结构）
          this.setData({
            question: nextQuestionContent,
            questionType: 'follow_up',
            feedback: responseData.feedback || null,
            nextQuestion: null, // 将在下一轮回答后设置
            userAnswer: '',
            interviewHistory: updatedHistory,
            animationState: { questionCard: 'fade-in' }
          });
          
          console.log('更新后的面试数据:', {
            question: this.data.question,
            feedback: this.data.feedback
          });
          
          // 重置动画状态
          setTimeout(() => {
            this.setData({
              animationState: { questionCard: 'idle' }
            });
          }, 1000);
        } else {
          // 没有更多问题，结束面试
          this.setData({
            userAnswer: '',
            interviewHistory: updatedHistory,
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
        
        // 滚动到最新记录
        this.scrollToBottom();
      } else {
        wx.showToast({
          title: response.message || '提交失败，请重试',
          icon: 'none'
        });
      }
    } catch (error) {
      wx.hideLoading();
      console.error('提交回答失败:', error);
      wx.showToast({
        title: '网络错误，请重试',
        icon: 'none'
      });
    }
  },

  // 跳过问题
  skipQuestion: function() {
    wx.showModal({
      title: '确认跳过',
      content: '确定要跳过这个问题吗？',
      success: (res) => {
        if (res.confirm) {
          // 清除用户输入，然后提交（将作为跳过处理）
          this.setData({ userAnswer: '' });
          this.submitAnswer();
        }
      }
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

  // 滚动到历史记录底部
  scrollToBottom: function() {
    try {
      // 简化的滚动逻辑
      setTimeout(() => {
        wx.createSelectorQuery().in(this)
          .select('#history-list-content')
          .scrollTo({
            scrollTop: 99999,
            animated: true
          });
      }, 100); // 添加小延迟以确保DOM已更新
    } catch (error) {
      console.error('滚动到底部失败:', error);
    }
  }
});