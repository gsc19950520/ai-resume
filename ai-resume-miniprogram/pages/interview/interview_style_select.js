// interview_style_select.js
const app = getApp();

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
      
      app.request({
        url: '/api/resume/user-resumes',
        method: 'GET',
        data: {
          userId: this.data.userId
        },
        success: (resData) => {
          clearTimeout(timeoutId);
          // request.js已经处理了res.code，这里直接使用返回的数据
          resolve(resData);
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
      
      app.request({
        url: '/api/interview/get-config',
        method: 'GET',
        success: (resData) => {
          clearTimeout(timeoutId);
          // request.js已经处理了res.code，这里直接使用返回的数据
          resolve(resData);
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
        url: '/api/interview/get-config',
        method: 'GET',
        success: (resData) => {
          // request.js已经处理了res.code，这里直接使用返回的数据
          resolve(resData);
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
      // 将encodeURIComponent移到模板字符串之外，便于代码依赖分析工具识别
      const encodedQuestion = encodeURIComponent(data.question);
      wx.navigateTo({
        url: `/pages/interview/interview?resumeId=${this.data.resumeId}&persona=${this.data.selectedPersona}&industryJobTag=${this.data.industryJobTag}&firstQuestion=${encodedQuestion}`
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
      // 添加超时处理，延长超时时间为15秒
      const timeoutId = setTimeout(() => {
        reject(new Error('请求超时，请检查网络连接'));
      }, 15000); // 15秒超时
      
      app.request({
        url: '/api/interview/generate-first-question',
        method: 'POST',
        data: {
          resumeId: this.data.resumeId,
          personaId: this.data.selectedPersona,
          industryJobTag: this.data.industryJobTag
        },
        success: (resData) => {
          clearTimeout(timeoutId);
          // request.js已经处理了res.code，这里直接使用返回的数据
          resolve(resData);
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
  },
  
  // 跳转到创建简历页面
  navigateToCreateResume() {
    wx.navigateTo({
      url: '/pages/create/create',
      fail: (err) => {
        console.error('跳转到创建简历页面失败:', err);
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