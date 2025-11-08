// interview_style_select.js
const app = getApp();

Page({
  data: {
    resumeId: '',
    userId: '',
    industryJobTag: '',
    resumeList: [], // 用户简历列表
    resumeIndex: 0, // 当前选中的简历索引
    selectedResume: null, // 选中的简历对象
    personas: [], // 面试官风格列表
    selectedPersona: '', // 选中的面试官风格ID
    previewQuestion: '' // 当前风格的预览问题
  },

  onLoad: function(options) {
    // 获取简历ID和用户ID
    this.setData({
      resumeId: options.resumeId || '',
      userId: app.globalData.userInfo?.id || wx.getStorageSync('userId') || '0',
      industryJobTag: options.industryJobTag || ''
    });

    // 加载用户简历列表
    this.loadUserResumes();
    
    // 加载面试官风格配置
    this.loadPersonaConfigs();
  },
  
  // 加载用户简历列表
  loadUserResumes: async function() {
    wx.showLoading({ title: '加载简历中...' })
    
    try {
      // 调用API获取真实简历列表
      const resumeList = await this.fetchUserResumes();
      
      wx.hideLoading()
      
      if (!resumeList || resumeList.length === 0) {
        wx.showToast({
          title: '暂无可用简历',
          icon: 'none',
          duration: 2000
        });
      }
      
      // 如果传入了简历ID，自动选中对应简历
      const selectedResume = this.data.resumeId ? resumeList.find(r => r.id === this.data.resumeId) : null;
      const resumeIndex = this.data.resumeId ? resumeList.findIndex(r => r.id === this.data.resumeId) : 0;
      
      this.setData({
        resumeList: resumeList,
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
      // 不设置任何数据，保留空状态
    }
  },
  
  // 从API获取用户简历列表
  fetchUserResumes: function() {
    return new Promise((resolve, reject) => {
      // 设置超时处理
      const timeoutId = setTimeout(() => {
        reject(new Error('获取简历超时'));
      }, 5000);
      
      app.request({
        url: '/api/resume/user-resumes',
        method: 'GET',
        data: {
          userId: this.data.userId
        },
        success: (res) => {
          clearTimeout(timeoutId);
          if (res.code === 0 && res.data) {
            resolve(res.data);
          } else {
            reject(new Error(res.message || '获取简历失败'));
          }
        },
        fail: (error) => {
          clearTimeout(timeoutId);
          reject(error);
        },
        complete: () => {
          clearTimeout(timeoutId);
        }
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
      
      if (config && config.personas && config.personas.length > 0) {
        this.setData({
          personas: config.personas
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
    ];
  },

  // 从后端获取面试官风格配置
  fetchPersonaConfigs: function() {
    return new Promise((resolve, reject) => {
      // 设置超时处理
      const timeoutId = setTimeout(() => {
        reject(new Error('获取面试官风格配置超时'));
      }, 5000);
      
      app.request({
        url: '/api/config/interview',
        method: 'GET',
        success: (res) => {
          clearTimeout(timeoutId);
          if (res.code === 0 && res.data) {
            resolve(res.data);
          } else {
            reject(new Error(res.message || '获取配置失败'));
          }
        },
        fail: (error) => {
          clearTimeout(timeoutId);
          reject(error);
        },
        complete: () => {
          clearTimeout(timeoutId);
        }
      });
    });
  },

  // 获取动态配置的API调用
  getDynamicConfig: function() {
    return new Promise((resolve, reject) => {
      app.request({
        url: '/api/config/interview',
        method: 'GET',
        success: (res) => {
          if (res.code === 0 && res.data) {
            resolve(res.data);
          } else {
            reject(new Error('获取配置失败'));
          }
        },
        fail: (error) => {
          reject(error);
        }
      });
    });
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

    wx.showLoading({ title: '正在生成第一个问题...' });
    
    try {
      // 调用后端API生成第一个问题，现在会直接返回数据或抛出异常
      const data = await this.generateFirstQuestion();
      
      // 如果成功，直接使用返回的数据
      wx.navigateTo({
        url: `/pages/interview/interview?resumeId=${this.data.resumeId}&persona=${this.data.selectedPersona}&industryJobTag=${this.data.industryJobTag}&firstQuestion=${encodeURIComponent(data.question)}`
      });
    } catch (error) {
      // 立即显示错误提示
      wx.showToast({
        title: error.message || '服务器异常，请稍后重试',
        icon: 'none',
        duration: 2000
      });
      console.error('生成问题失败:', error);
    } finally {
      // 确保loading状态被隐藏
      wx.hideLoading();
    }
  },
  
  // 调用后端API生成第一个问题
  generateFirstQuestion: function() {
    const app = getApp();
    
    return new Promise((resolve, reject) => {
      // 添加超时处理，缩短超时时间为5秒
      const timeoutId = setTimeout(() => {
        reject(new Error('请求超时，请检查网络连接'));
      }, 5000); // 5秒超时
      
      app.request({
        url: '/api/interview/generate-first-question',
        method: 'POST',
        data: {
          resumeId: this.data.resumeId,
          personaId: this.data.selectedPersona,
          industryJobTag: this.data.industryJobTag
        },
        success: (res) => {
          clearTimeout(timeoutId);
          if (res && (res.code === 0 || res.success) && res.data && res.data.question) {
            resolve(res.data);
          } else {
            // 立即抛出异常，不再返回对象
            reject(new Error(res.message || '服务器异常，请稍后重试'));
          }
        },
        fail: (error) => {
          clearTimeout(timeoutId);
          console.error('API请求失败:', error);
          // 立即抛出异常
          reject(new Error('网络连接异常，请检查网络后重试'));
        },
        complete: () => {
          clearTimeout(timeoutId);
        }
      });
    });
  },

  // 返回上一页
  goBack: function() {
    wx.navigateBack();
  }
});