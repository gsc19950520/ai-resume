// interview_style_select.js
const app = getApp();
import { get, post } from '../../utils/request.js';

// 静态引用标记 - 确保代码依赖分析工具能识别此文件
// eslint-disable-next-line
function __forceLoad__() {
  // 这个函数不会被调用，但它确保了文件被代码依赖分析工具识别
  return 'interview_style_select';
}

Page({
  data: {
    resumeId: '',
    userId: '',
    industryJobTag: '',
    jobTypeId: '',
    resumeList: [], // 用户简历列表
    resumeIndex: 0, // 当前选中的简历索引
    selectedResume: null, // 选中的简历对象
    personas: [], // 面试官风格列表
    selectedPersona: '', // 选中的面试官风格ID
    previewQuestion: '', // 当前风格的预览问题
    forceNewInterview: false // 是否强制创建新面试
  },

  onLoad: function(options) {
    const app = getApp()
    
    // 检查登录状态
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo')
    if (!userInfo) {
      wx.showModal({
        title: '提示',
        content: '请先登录，以使用完整功能',
        showCancel: false,
        confirmText: '去登录',
        success: () => {
          wx.navigateTo({ url: '/pages/login/login' })
        }
      })
      return
    }
    
    // 获取全局中的最新简历数据
    const latestResumeData = app.globalData.latestResumeData
    
    // 获取简历ID和用户ID
    this.setData({
      resumeId: options.resumeId || latestResumeData?.id || '',
      userId: userInfo.id || wx.getStorageSync('userId') || '0',
      industryJobTag: options.industryJobTag || latestResumeData?.occupation || '',
      // 新增：标记是否直接来自首页的简历数据
      hasResumeFromHome: !!latestResumeData || !!options.resumeId,
      // 新增：标记是否强制创建新面试
      forceNewInterview: options.forceNewInterview === 'true' || false
    })

    // 如果有全局简历数据或传入了resumeId，直接使用，否则加载用户简历列表
    if (latestResumeData) {
      this.processLatestResumeData(latestResumeData)
    } else if (options.resumeId) {
      // 如果有resumeId参数，构建单个简历数据
      this.processSingleResumeData(options.resumeId, options.title, options.occupation)
    } else {
      this.loadUserResumes()
    }
    
    // 加载面试官风格配置
    this.loadPersonaConfigs()
    
    // 如果不是强制新建面试，才检查是否有进行中的面试
    if (!this.data.forceNewInterview) {
      this.checkOngoingInterview()
    } else {
      console.log('强制新建面试，跳过进行中面试检查');
      // 清除全局会话信息，确保开始面试时创建新的会话
      if (getApp().globalData) {
        delete getApp().globalData.currentInterviewSessionId;
      }
      // 清除本地存储的会话信息
      wx.removeStorageSync('currentInterviewSessionId');
    }
  },

  // 处理单个简历数据（来自首页）
  processSingleResumeData: function(resumeId, title, occupation) {
    // 构建简历列表（只包含该简历）
    const resumeList = [{
      id: resumeId,
      title: title || '我的简历',
      occupation: occupation || '未设置职位'
    }]
    
    this.setData({
      resumeList: resumeList,
      resumeIndex: 0,
      selectedResume: resumeList[0],
      industryJobTag: occupation || this.data.industryJobTag
    })
  },
  
  // 处理最新简历数据
  processLatestResumeData: function(latestResumeData) {
    // 构建简历列表（只包含最新简历）
    const resumeList = [{
      id: latestResumeData.id,
      title: latestResumeData.title || '我的简历',
      occupation: latestResumeData.occupation || '未设置职位'
    }]
    
    this.setData({
      resumeList: resumeList,
      resumeIndex: 0,
      selectedResume: resumeList[0],
      industryJobTag: latestResumeData.occupation || this.data.industryJobTag
    })
  },
  
  // 加载用户简历列表
  loadUserResumes: async function() {
    wx.showLoading({ title: '加载简历中...' })
    
    try {
      // 调用API获取真实简历列表
      const resumeList = await this.fetchUserResumes();
      
      wx.hideLoading()
      
      // 确保resumeList是数组类型
      const safeResumeList = Array.isArray(resumeList) ? resumeList : [];
      
      if (safeResumeList.length === 0) {
        wx.showToast({
          title: '暂无可用简历',
          icon: 'none',
          duration: 2000
        });
      }
      
      // 如果传入了简历ID，自动选中对应简历
      const selectedResume = this.data.resumeId ? safeResumeList.find(r => r && r.id === this.data.resumeId) : null;
      const resumeIndex = this.data.resumeId ? safeResumeList.findIndex(r => r && r.id === this.data.resumeId) : 0;
      
      this.setData({
        resumeList: safeResumeList,
        resumeIndex: resumeIndex,
        selectedResume: selectedResume,
        industryJobTag: selectedResume?.occupation || this.data.industryJobTag
      })
    } catch (error) {
      wx.hideLoading()
      console.error('获取简历失败:', error);
      // 立即返回简历获取失败
      wx.showToast({
        title: '简历获取失败',
        icon: 'none',
        duration: 2000
      });
      // 确保resumeList设置为空数组而不是undefined
      this.setData({
        resumeList: []
      });
    }
  },
  
  // 从API获取用户简历列表
  fetchUserResumes: function() {
    return new Promise((resolve, reject) => {
      // 设置超时处理
      const timeoutId = setTimeout(() => {
        reject(new Error('获取简历超时'));
      }, 15000);
      
      get('/api/resume/user', {
        userId: this.data.userId
      })
      .then(resData => {
        clearTimeout(timeoutId);
        resolve(resData);
      })
      .catch(error => {
        clearTimeout(timeoutId);
        reject(error);
      })
      .finally(() => {
        clearTimeout(timeoutId);
      });
    });
  },
  
  // 选择简历
  selectResume: function(e) {
    const index = e.detail.value;
    const selectedResume = this.data.resumeList[index];
    
    if (selectedResume) {
      this.setData({
        resumeIndex: index,
        selectedResume: selectedResume,
        resumeId: selectedResume.id,
        industryJobTag: selectedResume.occupation
      });
    }
  },

  // 加载面试官风格配置
  loadPersonaConfigs: async function() {
    try {
      // 优先从后端获取配置
      const config = await this.fetchPersonaConfigs();
      if (config && config.data.personas && config.data.personas.length > 0) {
        // 获取默认配置用于映射emoji和example
        const defaultPersonas = this.getDefaultPersonas();
        
        // 为每个从后端获取的persona添加emoji和example字段
        const personas = config.data.personas.map(persona => {
          // 根据id查找默认配置
          const defaultPersona = defaultPersonas.find(p => p.id === persona.id);
          return {
            ...persona,
            emoji: defaultPersona ? defaultPersona.emoji : '👤',
            example: defaultPersona ? defaultPersona.example : ''
          };
        });
        
        this.setData({
          personas: personas
        });
        // 默认选中第一个风格
        if (personas.length > 0 && !this.data.selectedPersona) {
          this.setData({
            selectedPersona: personas[0].id,
            previewQuestion: personas[0].example || '请选择一种面试官风格体验不同的面试方式'
          });
        }
      } else {
        // 如果获取失败或没有数据，使用默认配置
        const defaultPersonas = this.getDefaultPersonas();
        this.setData({
          personas: defaultPersonas
        });
        // 默认选中第一个风格
        if (defaultPersonas.length > 0 && !this.data.selectedPersona) {
          this.setData({
            selectedPersona: defaultPersonas[0].id,
            previewQuestion: defaultPersonas[0].example || '请选择一种面试官风格体验不同的面试方式'
          });
        }
      }
    } catch (error) {
      console.error('获取面试官风格配置失败:', error);
      // 失败时使用默认配置
      const defaultPersonas = this.getDefaultPersonas();
      this.setData({
        personas: defaultPersonas
      });
      // 默认选中第一个风格
      if (defaultPersonas.length > 0 && !this.data.selectedPersona) {
        this.setData({
          selectedPersona: defaultPersonas[0].id,
          previewQuestion: defaultPersonas[0].example || '请选择一种面试官风格体验不同的面试方式'
        });
      }
    }
  },

  // 获取默认面试官风格
  getDefaultPersonas: function() {
    return [
      { 
        id: 'professional', 
        name: '专业严谨型', 
        emoji: '🎓',
        description: '逻辑清晰、专业正式，严格评估技术能力和项目经验，注重细节和方法论。',
        example: '请详细说明你在该项目中负责的模块架构设计及其技术选型理由。'
      },
      { 
        id: 'funny', 
        name: '搞怪幽默型', 
        emoji: '🤡',
        description: '轻松活泼，喜欢用幽默方式提问，让面试过程充满乐趣。',
        example: '如果让你用一个表情包形容你写的代码，你会选哪个？为什么？'
      },
      { 
        id: 'philosophical', 
        name: '抽象哲学型', 
        emoji: '🧠',
        description: '喜欢探讨技术背后的本质和意义，提问具有深度和哲理性。',
        example: '你认为技术创新的本质是什么？它如何影响人类的思维方式？'
      },
      { 
        id: 'crazy', 
        name: '抽风跳跃型', 
        emoji: '🐇',
        description: '思维跳跃，话题转换快，考验你的应变能力和知识面广度。',
        example: '先聊聊微服务架构，哦对了，你平时喜欢看什么电影？和编程有什么关联吗？'
      },
      { 
        id: 'anime', 
        name: '中二热血型', 
        emoji: '⚡',
        description: '充满激情和活力，喜欢用动漫风格的语言和比喻。',
        example: '作为一名开发者，你愿意成为拯救代码世界的英雄吗？请展示你的必杀技！'
      },
      { 
        id: 'healing', 
        name: '温柔治愈型', 
        description: '语气温和亲切，善于引导和鼓励，营造轻松的面试氛围。',
        emoji: '🌈',
        example: '你在项目中遇到过什么困难吗？当时你是怎么应对的？我相信你一定做得很好。'
      },
      { 
        id: 'sharp', 
        name: '毒舌犀利型', 
        emoji: '😏',
        description: '言辞犀利，直击要害，喜欢挑战你的观点和技术能力。',
        example: '这个方案漏洞百出，你真的觉得它能在实际环境中运行吗？'
      },
      { 
        id: 'retro', 
        name: '怀旧复古型', 
        emoji: '🕰️',
        description: '喜欢从历史角度看待技术发展，注重基础知识和经典技术。',
        example: '你了解计算机科学的经典算法吗？它们如何影响现代技术的发展？'
      }
    ];
  },

  // 从后端获取面试官风格配置
  fetchPersonaConfigs: function() {
    return new Promise((resolve, reject) => {
      // 设置超时处理
      const timeoutId = setTimeout(() => {
        reject(new Error('获取面试官风格配置超时'));
      }, 15000);
      
      get('/api/interview/get-config')
      .then(resData => {
        clearTimeout(timeoutId);
        resolve(resData);
      })
      .catch(error => {
        clearTimeout(timeoutId);
        reject(error);
      })
      .finally(() => {
        clearTimeout(timeoutId);
      });
    });
  },

  // 获取动态配置的API调用
  getDynamicConfig: function() {
    return get('/api/interview/get-config');
  },

  // 选择面试官风格
  selectPersona: function(e) {
    const personaId = e.currentTarget.dataset.id;
    const selectedPersona = this.data.personas.find(p => p.id === personaId);
    
    if (selectedPersona) {
      this.setData({
        selectedPersona: personaId,
        previewQuestion: selectedPersona.example || '请选择一种面试官风格体验不同的面试方式'
      });
    }
  },

  // 开始面试
  startInterview: async function() {
    // 验证是否已选择风格
    if (!this.data.selectedPersona || !this.data.selectedResume) {
      wx.showToast({
        title: '请先选择简历和面试官风格',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '正在初始化面试...' });
    
    try {
      // 调用后端API初始化面试会话
      this.data.jobTypeId = app.globalData.latestResumeData?.jobTypeId || 1
      const sessionInfo = await this.initInterviewSession();
      
      // 隐藏加载提示
      wx.hideLoading();
      
      console.log('准备跳转到面试页面，传递参数:', {
        sessionId: sessionInfo.sessionId
      });
      
      // 跳转到面试页面，传递会话ID、行业职位标签、面试官风格和剩余时间
      wx.navigateTo({
        url: `/pages/interview/interview?sessionId=${encodeURIComponent(sessionInfo.sessionId)}&industryJobTag=${encodeURIComponent(sessionInfo.industryJobTag || '')}&persona=${encodeURIComponent(this.data.selectedPersona)}&sessionTimeRemaining=${encodeURIComponent(sessionInfo.sessionTimeRemaining)}`
      });
    } catch (error) {
      wx.hideLoading();
      console.error('开始面试失败:', error);
      
      // 显示友好的错误提示
      wx.showToast({
        title: error.message || '开始面试失败，请重试',
        icon: 'none'
      });
    }
  },
  
  /**
   * 初始化面试会话
   */
  initInterviewSession: function() {
    return new Promise((resolve, reject) => {
      // 添加超时处理，延长超时时间为15秒
      const timeoutId = setTimeout(() => {
        reject(new Error('请求超时，请检查网络连接'));
      }, 15000); // 15秒超时
      
      post('/api/interview/start', {
        userId: this.data.userId,
        resumeId: this.data.resumeId,
        persona: this.data.selectedPersona,
        jobTypeId: this.data.jobTypeId,
        sessionSeconds: 600, // 默认面试时长10分钟
        forceNew: this.data.forceNewInterview
      })
      .then(resData => {
        clearTimeout(timeoutId);
        // 处理API返回数据，支持多种格式
        if (resData.code === 0 || resData.code === 200 || (resData.message && resData.message.toLowerCase() === 'success')) {
          const data = resData.data || resData;
          console.log('start接口返回数据:', data);
          
          // 保存会话ID到全局，用于后续问答
          if (app.globalData) {
            app.globalData.currentInterviewSessionId = data.sessionId;
          }
          
          resolve({
            sessionId: data.sessionId,
            industryJobTag: data.industryJobTag || '',
            sessionTimeRemaining: data.sessionTimeRemaining || 600 // 默认10分钟
          });
        } else {
          reject(new Error(resData.message || '初始化面试会话失败'));
        }
      })
      .catch(error => {
        clearTimeout(timeoutId);
        console.error('API请求失败:', error);
        reject(new Error('网络连接异常，请检查网络后重试'));
      })
      .finally(() => {
        clearTimeout(timeoutId);
      });
    });
  },
  
  // 调用分析简历API
  callAnalyzeResumeAPI: function() {
    return new Promise((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        reject(new Error('分析请求超时'));
      }, 30000); // 30秒超时，因为分析可能需要较长时间
      
      try {
        console.log('准备调用分析简历API，参数:', {
          resumeId: this.data.resumeId,
          jobType: this.data.industryJobTag,
          analysisDepth: 'comprehensive'
        });
        
        post('/api/interview/analyze-resume', {
          resumeId: this.data.resumeId,
          jobType: this.data.industryJobTag,
          analysisDepth: 'comprehensive' // 综合分析
        })
        .then(resData => {
          clearTimeout(timeoutId);
          // 打印完整的API响应数据
          console.log('分析简历API返回完整数据:', resData);
          
          // 支持多种成功判断条件：code为0或200，或者message为'success'
          if (resData.code === 0 || resData.code === 200 || (resData.message && resData.message.toLowerCase() === 'success')) {
            const resultToReturn = resData.data || resData;
            console.log('返回给页面的数据:', resultToReturn);
            resolve(resultToReturn);
          } else {
            console.log('API返回失败状态:', resData);
            reject(new Error(resData.message || '分析失败'));
          }
        })
        .catch(error => {
          clearTimeout(timeoutId);
          console.error('API请求失败:', error);
          console.log('错误详情:', error);
          // 特殊处理：如果错误消息是'success'，认为是成功的
          if (error.message && error.message.toLowerCase() === 'success') {
            console.log('检测到message为success，视为成功处理');
            const fallbackData = error.originalError?.data || {};
            console.log('使用的备用数据:', fallbackData);
            resolve(fallbackData);
          } else {
            reject(new Error('网络连接异常，请重试'));
          }
        });
      } catch (err) {
        clearTimeout(timeoutId);
        console.error('调用API过程中发生异常:', err);
        reject(new Error('调用过程异常，请重试'));
      }
    });
  },

  // 返回上一页
  goBack: function() {
    wx.navigateBack();
  },
  
  // 检查是否有进行中的面试
  checkOngoingInterview: function() {
    if (!this.data.userId) {
      return;
    }
    
    get('/api/interview/check-ongoing', {
      userId: this.data.userId
    })
    .then(resData => {
      // 处理API返回数据，支持多种格式
      if ((resData.code === 0 || resData.code === 200) && resData.data) {
        const ongoingInterview = resData.data;
        if (ongoingInterview) {
          // 有进行中的面试，显示弹窗提示
          this.showContinueInterviewModal(ongoingInterview);
        }
      }
    })
    .catch(error => {
      console.error('检查进行中面试失败:', error);
      // 检查失败不影响页面正常使用
    });
  },
  
  // 显示是否继续面试的弹窗
  showContinueInterviewModal: function(ongoingInterview) {
    wx.showModal({
      title: '发现进行中的面试',
      content: '您有一个正在进行的面试，是否继续？',
      confirmText: '继续面试',
      cancelText: '重新开始',
      success: (res) => {
        if (res.confirm) {
          // 用户选择继续面试，跳转到面试页面
          wx.navigateTo({
            url: `/pages/interview/interview?sessionId=${encodeURIComponent(ongoingInterview.sessionId)}`
          });
        } else if (res.cancel) {
          // 用户选择重新开始，清除全局会话信息，继续留在当前页面
          console.log('用户选择重新开始面试');
          // 清除全局会话信息，确保开始面试时创建新的会话
          if (getApp().globalData) {
            delete getApp().globalData.currentInterviewSessionId;
          }
          // 清除本地存储的会话信息
          wx.removeStorageSync('currentInterviewSessionId');
          // 设置强制创建新面试的标志
          this.setData({
            forceNewInterview: true
          });
        }
      }
    });
  },
  
  // 跳转到创建简历页面（模板选择页面）
  navigateToCreateResume() {
    wx.navigateTo({
      url: '/pages/template/list/list',
      fail: (err) => {
        console.error('跳转到模板选择页面失败:', err);
        wx.showToast({
          title: '页面跳转失败',
          icon: 'none'
        });
      }
    });
  },
  
  // 上传简历文件
  uploadResume() {
    const that = this;
    
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['docx', 'pdf'],
      success: function(res) {
        const tempFilePath = res.tempFiles[0].path;
        const fileName = res.tempFiles[0].name;
        
        wx.showLoading({
          title: '正在上传简历...',
        });
        
        // 调用微信小程序的上传API
        wx.uploadFile({
          url: app.globalData.baseUrl + '/api/resume/upload',
          filePath: tempFilePath,
          name: 'file',
          formData: {
            userId: that.data.userId,
            fileName: fileName
          },
          success: function(uploadRes) {
            try {
              const data = JSON.parse(uploadRes.data);
              if (data.code === 0) {
                wx.showToast({
                  title: '简历上传成功',
                  icon: 'success'
                });
                // 上传成功后重新加载简历列表
                setTimeout(() => {
                  that.loadUserResumes();
                }, 1000);
              } else {
                wx.showToast({
                  title: data.message || '上传失败',
                  icon: 'none'
                });
              }
            } catch (e) {
              wx.showToast({
                title: '上传失败，服务器响应异常',
                icon: 'none'
              });
            }
          },
          fail: function(err) {
            console.error('简历上传失败:', err);
            wx.showToast({
              title: '网络异常，请重试',
              icon: 'none'
            });
          },
          complete: function() {
            wx.hideLoading();
          }
        });
      },
      fail: function(err) {
        console.error('选择文件失败:', err);
        // 如果用户取消选择，不显示错误提示
        if (err.errMsg !== 'chooseMessageFile:fail cancel') {
          wx.showToast({
            title: '选择文件失败',
            icon: 'none'
          });
        }
      }
    });
  },
});