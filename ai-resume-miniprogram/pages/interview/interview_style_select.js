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
    previewQuestion: '' // 当前风格的预览问题
  },

  onLoad: function(options) {
    const app = getApp()
    
    // 获取全局中的最新简历数据
    const latestResumeData = app.globalData.latestResumeData
    
    // 获取简历ID和用户ID
    this.setData({
      resumeId: options.resumeId || latestResumeData?.id || '',
      userId: app.globalData.userInfo?.id || wx.getStorageSync('userId') || '0',
      industryJobTag: options.industryJobTag || latestResumeData?.occupation || '',
      // 新增：标记是否直接来自首页的简历数据
      hasResumeFromHome: !!latestResumeData || !!options.resumeId
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
        this.setData({
          personas: config.data.personas
        });
      } else {
        // 如果获取失败或没有数据，使用默认配置
        const defaultPersonas = this.getDefaultPersonas();
        this.setData({
          personas: defaultPersonas
        });
      }
    } catch (error) {
      console.error('获取面试官风格配置失败:', error);
      // 失败时使用默认配置
      const defaultPersonas = this.getDefaultPersonas();
      this.setData({
        personas: defaultPersonas
      });
    }
  },

  // 获取默认面试官风格
  getDefaultPersonas: function() {
    return [
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
        id: 'encouraging', 
        name: '鼓励型', 
        emoji: '🌱',
        description: '语气温和积极，注重引导思考与成长体验。',
        example: '你的思路挺好，可以再具体举个例子来支撑一下吗？'
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
        previewQuestion: selectedPersona.example
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

    wx.showLoading({ title: '正在准备面试...' });
    
    try {
      // 调用后端API生成第一个问题和会话，使用start接口
      this.data.jobTypeId = app.globalData.latestResumeData?.jobTypeId || 1
      const data = await this.generateFirstQuestion();
      
      // 隐藏加载提示
      wx.hideLoading();
      
      // 确保有question对象和sessionId
      if (!data.question || !data.sessionId) {
        throw new Error('返回数据不完整');
      }
      
      console.log('准备跳转到面试页面，传递参数:', {
        question: data.question,
        sessionId: data.sessionId
      });
      
      // 跳转到面试页面，传递第一个问题和会话ID
      wx.navigateTo({
        url: `/pages/interview/interview?firstQuestion=${encodeURIComponent(JSON.stringify(data.question))}&sessionId=${encodeURIComponent(data.sessionId)}`
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
  
  // 调用后端API生成面试会话并获取第一个问题
  generateFirstQuestion: function() {
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
        sessionSeconds: 600 // 默认面试时长10分钟
      })
      .then(resData => {
        clearTimeout(timeoutId);
        // 处理API返回数据，支持多种格式
        if (resData.code === 0 || resData.code === 200 || (resData.message && resData.message.toLowerCase() === 'success')) {
          const data = resData.data || resData;
          console.log('start接口返回数据:', data);
          
          resolve(data);
        } else {
          reject(new Error(resData.message || '创建面试会话失败'));
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

  // 分析简历
  analyzeResume: async function() {
    if (!this.data.selectedResume) {
      wx.showToast({
        title: '请先选择简历',
        icon: 'none'
      });
      return;
    }

    wx.showLoading({ title: '正在分析简历...' });
    
    try {
      // 调用后端API分析简历
      const analysisResult = await this.callAnalyzeResumeAPI();
      
      wx.hideLoading();
      
      // 跳转到简历分析结果页面
      wx.navigateTo({
        url: `/pages/interview/resume_analysis_result?analysisData=${encodeURIComponent(JSON.stringify(analysisResult))}`
      });
      
    } catch (error) {
      wx.hideLoading();
      wx.showToast({
        title: error.message || '简历分析失败',
        icon: 'none',
        duration: 2000
      });
      console.error('简历分析失败:', error);
    }
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