// preview.js - 动态模板预览版本
const { request } = require('../../../utils/request');

Page({
  
  data: {
    templateId: 'template-one',  // 默认模板ID
    templateName: '技术人才模板',  // 默认模板名称
    imagePath: '/images/template-one.png',  // 默认图片路径
    templateUpdateTime: new Date().getTime(),  // 用于触发视图更新的时间戳
    resumeData: null,  // 简历数据
    userInfo: null,  // 用户信息
    loading: true,  // 加载状态
    apiBaseUrl: '', // 使用云托管服务，不需要硬编码URL
    enableMock: true, // 是否启用模拟PDF下载功能
    canvasStyle: 'width: 750px; height: 1200px; position: absolute; left: -9999px; top: -9999px;' // Canvas样式
  },
  
  /**
   * 获取配置的API基础URL
   * 注意：使用云托管服务时，此方法返回空字符串，实际调用通过cloudCall完成
   */
  getApiBaseUrl: function() {
    // 使用云托管服务时，不需要具体的API基础URL
    const app = getApp();
    if (app.globalData && app.globalData.useCloud) {
      console.log('使用云托管服务，getApiBaseUrl返回空字符串');
      return '';
    }
    // 优先从全局配置中获取，如果没有则使用data中的默认值
    return app.globalData && app.globalData.apiBaseUrl || this.data.apiBaseUrl;
  },

  /**
   * 生命周期函数--监听页面加载
   * @param {Object} options - 页面参数，包含templateId和templateName
   */
  onLoad: function(options) {
    console.log('预览页面接收到的参数:', options);
    
    // 有效的模板ID列表 - 确保与实际存在的图片文件匹配
    const validTemplateIds = ['template-one', 'template-two', 'template-three', 'template-four', 'template-five', 'template-six'];
    
    // 处理传入的参数
    if (options && options.templateId) {
      let templateId = options.templateId;
      
      // 防御性检查：如果传入的templateId无效，使用默认值
      if (!validTemplateIds.includes(templateId)) {
        console.warn('无效的模板ID:', templateId, '，使用默认模板');
        templateId = 'template-one'; // 使用默认模板ID
      }
      
      // 设置图片路径和模板ID
      const imagePath = `/images/${templateId}.png`;
      this.setData({
        templateId: templateId,
        imagePath: imagePath
      });
      
      // 动态加载对应的样式文件
      this.loadTemplateStyles(templateId);
      console.log('已设置模板ID:', templateId, '图片路径:', imagePath);
    }
    
    // 处理模板名称
    if (options && options.templateName) {
      try {
        this.setData({
          templateName: decodeURIComponent(options.templateName)
        });
        console.log('已设置模板名称:', this.data.templateName);
      } catch (e) {
        console.error('解码模板名称失败:', e);
      }
    }
    
    // 处理resumeId参数 - 优先从URL参数获取，其次从本地存储获取
    if (options && options.resumeId) {
      // 保存resumeId到本地存储，供后续使用
      wx.setStorageSync('previewOptions', { 
        ...wx.getStorageSync('previewOptions'),
        resumeId: options.resumeId 
      });
      console.log('从URL参数获取resumeId:', options.resumeId);
    }
    
    // 立即设置一个默认的userInfo，确保页面渲染时不会出现undefined
    this.setData({
      userInfo: { name: '用户姓名' }
    });
    console.log('onLoad: 初始默认userInfo已设置');
    
    // 异步加载用户信息，确保userInfo在页面渲染时可用
    this.loadUserInfoFromStorage();
    
    // 然后加载简历数据
    this.loadResumeData();
    
    // 添加一个延时检查，确保userInfo最终被正确设置
    setTimeout(() => {
      this.verifyUserInfo();
    }, 100);
    
    // 初始化Canvas元素
    this.initializeCanvas();
  },
  
  /**
   * 验证userInfo是否正确设置
   */
  verifyUserInfo: function() {
    console.log('verifyUserInfo: 开始验证userInfo数据');
    
    if (!this.data.userInfo || typeof this.data.userInfo !== 'object') {
      console.warn('verifyUserInfo: userInfo不存在或不是对象，创建默认值');
      this.setData({
        userInfo: { name: '用户姓名' }
      });
    } else if (!this.data.userInfo.name) {
      console.warn('verifyUserInfo: userInfo存在但name字段为空，添加默认name');
      const updatedUserInfo = { ...this.data.userInfo, name: '用户姓名' };
      this.setData({
        userInfo: updatedUserInfo
      });
    }
    
    console.log('verifyUserInfo: 验证完成，最终userInfo:', this.data.userInfo);
  },
  
  /**
   * 加载简历数据
   */
  loadResumeData: function() {
    try {
      console.log('开始加载简历数据');
      // 确保userInfo已加载（冗余保障）
      if (!this.data.userInfo) {
        console.log('loadResumeData中检测到userInfo为空，重新加载用户信息');
        this.loadUserInfoFromStorage();
      }
      
      // 检查是否有resumeId参数
      const options = wx.getStorageSync('previewOptions') || {};
      if (options.resumeId) {
        console.log('检测到resumeId，尝试从后端获取完整简历数据:', options.resumeId);
        this.loadResumeDataFromBackend(options.resumeId);
        return;
      }
      
      // 优先尝试从tempResumeInfo获取用户填写的数据
      let tempResumeInfo = wx.getStorageSync('tempResumeInfo');
      let rawData = null;
      
      if (tempResumeInfo && (tempResumeInfo.personalInfo || tempResumeInfo.name)) {
        // 如果tempResumeInfo中有用户数据，使用这些数据
        console.log('从tempResumeInfo获取用户数据:', tempResumeInfo);
        
        // 标准化数据结构
        rawData = this.normalizeResumeData(tempResumeInfo);
        
        console.log('标准化后的用户数据:', rawData);
      } else {
        // 如果没有用户数据，尝试从resumeData获取
        let storedData = wx.getStorageSync('resumeData');
        console.log('从resumeData获取数据:', storedData);
        
        if (storedData && storedData.data) {
          rawData = storedData.data;
        } else {
          // 使用默认的示例数据
          rawData = {
            personalInfo: {
              name: '刘豆豆',
              jobTitle: 'UI/UX 设计师',
              phone: '138-0000-0000',
              email: 'lisi@qq.com',
              address: '上海',
              birthDate: '1999.09',
              expectedSalary: '20000',
              startTime: '一周内'
            },
            education: [
              {
                school: '中央美术学院',
                degree: '本科',
                major: '视觉传达设计专业',
                startDate: '2014年9月',
                endDate: '2018年7月'
              }
            ],
            workExperience: [
              {
                company: '腾讯网络科技有限公司',
                position: 'UI设计师',
                startDate: '2018年8月',
                endDate: '至今',
                description: '负责移动端产品界面设计、交互优化，提升用户体验。'
              },
              {
                company: '字节跳动',
                position: 'UI实习生',
                startDate: '2017年7月',
                endDate: '2017年12月',
                description: '参与小程序界面与交互设计，提升组件一致性与美观性。'
              }
            ],
            skillsWithLevel: [
              { name: 'Photoshop', level: 4 },
              { name: 'Sketch', level: 3 },
              { name: 'Figma', level: 3 }
            ],
            hobbies: ['设计', '交互'],
            selfEvaluation: '热爱设计与交互，注重用户体验与界面细节，具备团队协作与独立思考能力。'
          };
        }
      }
      
      // 使用规范化函数处理数据，确保数据结构与模板一致
      console.log('最终使用的简历数据:', rawData);
      console.log('当前userInfo数据:', this.data.userInfo);
      
      // 强制确保userInfo不为空
      if (!this.data.userInfo) {
        console.log('loadResumeData最后检查：userInfo仍为空，创建默认用户信息');
        this.setData({
          userInfo: { name: '用户姓名' }
        });
      }
      
      // 分两步设置数据，确保userInfo不会被覆盖
      // 第一步：设置resumeData和loading状态
      this.setData({
        resumeData: rawData,
        loading: false
      });
      
      // 第二步：单独确认userInfo已正确设置（即使在第一步中可能被覆盖，这里也会重新设置）
      this.setData({
        userInfo: this.data.userInfo || { name: '用户姓名' }
      });
      
      // 再次确认userInfo已正确设置
      console.log('loadResumeData完成后，userInfo数据:', this.data.userInfo);
      console.log('loadResumeData完成后，resumeData数据:', this.data.resumeData);
      
    } catch (error) {
      console.error('加载简历数据失败:', error);
      // 即使发生错误，也要确保userInfo存在
      this.setData({
        loading: false,
        userInfo: this.data.userInfo || { name: '用户姓名' }
      });
    }
  },
  
  /**
   * 从后端获取用户最新的简历数据（使用云托管方式）
   */
  loadResumeDataFromBackend: function(resumeId) {
    const app = getApp();
    
    // 获取用户信息
    const userInfo = this.data.userInfo;
    if (!userInfo || !userInfo.id) {
      console.error('用户信息不存在，无法获取最新简历数据');
      this.handleResumeLoadError();
      return;
    }
    
    // 获取token
    const token = wx.getStorageSync('token') || '';
    if (!token) {
      console.error('Token不存在，无法获取最新简历数据');
      this.handleResumeLoadError();
      return;
    }
    
    // 使用云托管方式调用后端接口获取用户最新简历数据
    app.cloudCall('/api/resume/getLatest', {
      userId: userInfo.id
    }, 'GET', {
      'Authorization': `Bearer ${token}`
    })
      .then(res => {
        if (res && res.success && res.data) {
          console.log('从后端获取用户最新简历数据成功:', res.data);
          // 转换后端数据格式为前端需要的格式
          const formattedData = this.formatBackendResumeData(res.data);
          this.setData({
            resumeData: formattedData,
            loading: false
          });
        } else {
          console.error('获取用户最新简历数据失败:', res);
          this.handleResumeLoadError();
        }
      })
      .catch(err => {
        console.error('请求后端失败:', err);
        this.handleResumeLoadError();
      });
  },
  
  /**
   * 处理简历加载错误
   */
  handleResumeLoadError: function() {
    // 加载失败时使用默认数据
    this.setData({
      resumeData: {
        personalInfo: {
          name: '加载失败',
          jobTitle: '请稍后重试',
          phone: '',
          email: '',
          address: ''
        },
        education: [],
        workExperience: [],
        skillsWithLevel: [],
        hobbies: [],
        selfEvaluation: '简历数据加载失败，请稍后重试'
      },
      loading: false
    });
  },
  
  /**
   * 标准化简历数据
   */
  normalizeResumeData: function(data) {
    return {
      personalInfo: data.personalInfo || {
        name: data.name || '',
        jobTitle: data.jobTitle || data.position || '',
        phone: data.phone || '',
        email: data.email || '',
        address: data.address || '',
        birthDate: data.birthDate || '',
        expectedSalary: data.expectedSalary || '',
        startTime: data.startTime || '',
        avatar: data.avatar || '',
        selfEvaluation: data.selfEvaluation || ''
      },
      contact: data.contact || {},
      education: data.education || [],
      workExperience: data.workExperience || data.work || [],
      projectExperienceList: data.projectExperienceList || data.projects || [],
      skillsWithLevel: data.skillsWithLevel || data.skills || [],
      hobbies: data.hobbies || [],
      selfEvaluation: data.selfEvaluation || data.selfAssessment || ''
    };
  },
  
  /**
   * 格式化后端返回的简历数据（根据后端getLatest接口的数据结构）
   */
  formatBackendResumeData: function(backendData) {
    // 保存resumeId供后续PDF下载使用
    if (backendData.id) {
      this.setData({
        resumeId: backendData.id
      });
      // 同时更新本地存储中的resumeId
      const options = wx.getStorageSync('previewOptions') || {};
      options.resumeId = backendData.id;
      wx.setStorageSync('previewOptions', options);
      console.log('保存resumeId到本地存储:', backendData.id);
    }
    
    // 添加调试日志，查看后端返回的数据结构
    console.log('后端返回数据结构:', JSON.stringify(backendData, null, 2));
    console.log('userInfo数据:', backendData.userInfo);
    console.log('expectedSalary字段:', backendData.expectedSalary);
    console.log('startTime字段:', backendData.startTime);
    console.log('jobTitle字段:', backendData.jobTitle);
    
    return {
      // 基本信息（来自User表）
      personalInfo: {
        name: (backendData.userInfo && backendData.userInfo.name) || backendData.name || '',
        jobTitle: backendData.jobTitle || (backendData.userInfo && backendData.userInfo.jobTitle) || '',
        phone: (backendData.userInfo && backendData.userInfo.phone) || backendData.phone || '',
        email: (backendData.userInfo && backendData.userInfo.email) || backendData.email || '',
        address: (backendData.userInfo && backendData.userInfo.address) || backendData.address || '',
        birthDate: (backendData.userInfo && backendData.userInfo.birthDate) || backendData.birthDate || '',
        expectedSalary: backendData.expectedSalary || (backendData.userInfo && backendData.userInfo.expectedSalary) || '',
        startTime: backendData.startTime || (backendData.userInfo && backendData.userInfo.startTime) || '',
        avatar: (backendData.userInfo && backendData.userInfo.avatarUrl) || backendData.avatar || '',
        selfEvaluation: backendData.selfEvaluation || (backendData.userInfo && backendData.userInfo.selfEvaluation) || '',
        nickname: (backendData.userInfo && backendData.userInfo.nickname) || '',
        gender: (backendData.userInfo && backendData.userInfo.gender) || '',
        country: (backendData.userInfo && backendData.userInfo.country) || '',
        province: (backendData.userInfo && backendData.userInfo.province) || '',
        city: (backendData.userInfo && backendData.userInfo.city) || ''
      },
      // 简历相关信息
      resumeInfo: {
        id: backendData.id,
        userId: backendData.userId,
        originalFilename: backendData.originalFilename,
        jobTypeId: backendData.jobTypeId,
        templateId: backendData.templateId,
        status: backendData.status,
        createTime: backendData.createTime,
        updateTime: backendData.updateTime
      },
      // 关联数据
      education: backendData.educationList || [],
      workExperience: backendData.workExperienceList || [],
      projectExperienceList: backendData.projectList || [],
      skillsWithLevel: backendData.skillList || [],
      hobbies: backendData.interests ? backendData.interests.split(',') : [],
      selfEvaluation: backendData.selfEvaluation || '',
      contact: {} // 预留联系信息字段
    };
  },
  
  /**
   * 初始化Canvas元素
   */
  initializeCanvas: function() {
    console.log('初始化Canvas元素');
    
    // 设置Canvas样式，确保元素存在
    this.setData({
      canvasStyle: 'width: 750px; height: 1200px; position: absolute; left: -9999px; top: -9999px;'
    });
    
    // 延时确保Canvas元素渲染完成
    setTimeout(() => {
      console.log('Canvas初始化完成');
      this.checkCanvasExists();
    }, 500);
  },
  
  /**
   * 检查Canvas元素是否存在
   */
  checkCanvasExists: function() {
    const query = wx.createSelectorQuery();
    query.select('#captureCanvas').boundingClientRect((rect) => {
      console.log('Canvas元素检查结果:', rect);
      if (!rect) {
        console.warn('Canvas元素未找到，重新初始化');
        // 如果Canvas不存在，重新设置样式
        this.setData({
          canvasStyle: 'width: 750px; height: 1200px; position: absolute; left: -9999px; top: -9999px;'
        });
      }
    }).exec();
  },
  
  /**
   * 动态加载模板样式文件
   * @param {string} templateId - 模板ID
   */
  /**
   * 加载指定模板的样式
   * @param {string} templateId - 模板ID
   */
  loadTemplateStyles: function(templateId) {
    try {
      console.log('正在为模板:', templateId, '加载对应样式');
      
      // 在小程序中，模板样式通过WXML中的模板导入和wxss的@import来控制
      // 由于我们已经在preview.wxss中导入了所有模板样式，这里只需确保模板切换时视图正确更新
      this.setData({
        templateUpdateTime: new Date().getTime() // 强制触发视图更新，确保样式正确应用
      });
      
      console.log('模板样式已准备就绪:', templateId);
    } catch (error) {
      console.error('加载模板样式失败:', error);
    }
  },

  /**
   * 返回模板列表
   */
  backToList: function() {
    wx.navigateBack();
  },
  
  /**
   * 生命周期函数--监听页面显示
   * 每次显示页面时重新加载数据，确保数据同步
   */
  onShow: function() {
    this.loadResumeData();
  },
  
  /**
   * 跳转到测试页面
   */
  /**
   * 跳转到测试页面（暂未实现）
   */
  goToTest: function() {
    // 调用测试函数
    this.testTemplateRendering();
  },

  /**
   * 切换模板
   * @param {Object} e - 事件对象，包含模板ID
   */
  switchTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id;
    const templateNameMap = {
      'template-one': '技术人才模板',
      'template-two': '简约风格模板',
      'template-three': '设计师模板',
      'template-four': '商务模板',
      'template-five': '创意模板',
      'template-six': '专业模板'
    };
    
    this.setData({
      templateId: templateId,
      templateName: templateNameMap[templateId] || templateId,
      imagePath: `/images/${templateId}.jpg`,
      templateUpdateTime: new Date().getTime()
    });
    
    console.log('已切换到模板:', templateId);
  },
  
  /**
   * 下载PDF文件
   * 调用后端API生成并下载PDF，使用云托管的/export/pdf接口
   */
  downloadPdf: function() {
    const { templateId, resumeData } = this.data;
    wx.showLoading({
      title: '正在生成PDF...',
    });

    // 获取resumeId，优先从后端数据中获取，其次从本地存储中获取
    let resumeId = '';
    if (this.data.resumeData && this.data.resumeData.id) {
      resumeId = this.data.resumeData.id;
    } else {
      resumeId = wx.getStorageSync('resumeId') || '';
    }

    // 验证必要参数
    if (!resumeId) {
      wx.hideLoading();
      wx.showToast({
        title: '请先保存简历',
        icon: 'none'
      });
      return;
    }

    if (!templateId) {
      wx.hideLoading();
      wx.showToast({
        title: '模板ID不能为空',
        icon: 'none'
      });
      return;
    }

    const app = getApp();
    
    // 使用云托管调用后端/export/pdf接口
    if (app.cloudCallBinary) {
      app.cloudCallBinary({
        url: `/resume/export/pdf?resumeId=${resumeId}&templateId=${templateId}`,
        method: 'GET',
        header: {
          'Authorization': `Bearer ${app.globalData.token || ''}`
        }
      }).then(pdfBuffer => {
        wx.hideLoading();
        console.log('PDF生成成功，数据长度:', pdfBuffer ? pdfBuffer.byteLength : 0);
        
        if (!pdfBuffer || pdfBuffer.byteLength === 0) {
          wx.showToast({
            title: 'PDF生成失败',
            icon: 'none'
          });
          return;
        }
        
        // 保存PDF到本地
        const fileName = `resume_${resumeId}_${templateId}.pdf`;
        const filePath = wx.env.USER_DATA_PATH + '/' + fileName;
        
        wx.getFileSystemManager().writeFile({
          filePath: filePath,
          data: pdfBuffer,
          encoding: 'binary',
          success: () => {
            console.log('PDF保存成功');
            // 打开PDF文件预览
            wx.openDocument({
              filePath: filePath,
              showMenu: true,
              fileType: 'pdf',
              success: function(res) {
                console.log('打开PDF成功', res);
              },
              fail: function(err) {
                console.error('打开PDF失败', err);
                wx.showToast({
                  title: '打开PDF失败',
                  icon: 'none'
                });
              }
            });
          },
          fail: (err) => {
            console.error('保存PDF失败:', err);
            wx.showToast({
              title: '保存PDF失败',
              icon: 'none'
            });
          }
        });
      }).catch(err => {
        wx.hideLoading();
        console.error('调用后端生成PDF失败:', err);
        wx.showToast({
          title: '生成PDF失败',
          icon: 'none'
        });
        
        // 失败时尝试使用模拟模式
        if (this.data.enableMock) {
          this.mockPdfDownload();
        }
      });
    } else {
      wx.hideLoading();
      console.error('云托管调用方法不可用');
      wx.showToast({
        title: '系统错误',
        icon: 'none'
      });
      
      // 失败时尝试使用模拟模式
      if (this.data.enableMock) {
        this.mockPdfDownload();
      }
    }
  },
  
  /**
   * 捕获简历页面截图
   * @returns {Promise} 返回包含截图临时路径的Promise
   */
  captureResumePage: function() {
    return new Promise((resolve, reject) => {
      // 获取页面节点信息
      wx.createSelectorQuery().select('#resume-container')
        .boundingClientRect()
        .exec(res => {
          if (!res || !res[0]) {
            // 如果找不到#resume-container节点，则尝试截取整个页面
            wx.createSelectorQuery().selectViewport()
              .boundingClientRect()
              .exec(viewportRes => {
                if (!viewportRes || !viewportRes[0]) {
                  reject(new Error('无法获取页面尺寸'));
                  return;
                }
                
                this.performCapture(viewportRes[0], resolve, reject);
              });
            return;
          }
          
          this.performCapture(res[0], resolve, reject);
        });
    });
  },
  
  /**
   * 执行截图操作
   * @param {Object} rect 要截取的区域尺寸信息
   * @param {Function} resolve Promise解析函数
   * @param {Function} reject Promise拒绝函数
   */
  performCapture: function(rect, resolve, reject) {
    // 创建canvas上下文
    const ctx = wx.createCanvasContext('captureCanvas', this);
    
    // 设置canvas尺寸
    const dpr = wx.getSystemInfoSync().pixelRatio;
    const canvasWidth = rect.width * dpr;
    const canvasHeight = rect.height * dpr;
    
    // 绘制页面到canvas
    wx.canvasToTempFilePath({
      canvasId: 'captureCanvas',
      x: 0,
      y: 0,
      width: rect.width,
      height: rect.height,
      destWidth: canvasWidth,
      destHeight: canvasHeight,
      quality: 1,
      success: res => {
        resolve(res.tempFilePath);
      },
      fail: err => {
        console.error('canvasToTempFilePath失败:', err);
        reject(err);
      }
    }, this);
  },
  
  /**
   * 模拟PDF下载功能
   * 用于开发和测试环境，当后端接口不可用或返回空数据时使用
   */
  mockPdfDownload: function() {
    console.log('开始模拟PDF生成和下载');
    
    // 模拟网络延迟
    setTimeout(() => {
      // 在实际场景中，这里应该生成一个简单的PDF文件
      // 由于小程序环境限制，我们直接创建一个简单的PDF文件内容
      // 在真实项目中，可以使用canvas绘制内容并导出为PDF
      
      // 模拟PDF文件的二进制数据（实际上这只是一个标识）
      const mockPdfData = new ArrayBuffer(100); // 创建一个100字节的模拟数据
      const view = new Uint8Array(mockPdfData);
      for (let i = 0; i < view.length; i++) {
        view[i] = i % 256; // 填充一些数据
      }
      
      // 模拟成功生成PDF
      wx.hideLoading();
      console.log('模拟PDF生成完成');
      
      // 显示提示信息
      wx.showModal({
        title: 'PDF预览',
        content: '在实际环境中，这里将打开生成的PDF文件。此为模拟演示，实际使用时需要后端正确实现PDF生成功能。',
        showCancel: false,
        success: () => {
          // 在实际环境中，这里会调用handlePdfData处理真实的PDF数据
          // 由于是模拟，我们只显示成功提示
          wx.showToast({
            title: 'PDF生成成功',
            icon: 'success',
            duration: 2000
          });
        }
      });
    }, 1500);
  },
  
  /**
   * 处理PDF二进制数据
   * @param {ArrayBuffer} pdfData - PDF文件的二进制数据
   */
  handlePdfData: function(pdfData) {
    try {
      // 验证返回的数据类型
      if (!pdfData || typeof pdfData !== 'object') {
        wx.hideLoading();
        wx.showToast({
          title: 'PDF数据格式错误',
          icon: 'none',
          duration: 2000
        });
        console.error('PDF数据类型错误:', typeof pdfData, pdfData);
        return;
      }
      
      // 保存PDF文件到本地
      const fileName = `resume-${new Date().getTime()}.pdf`;
      const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`;
      
      // 使用wx.getFileSystemManager写入文件
      const fs = wx.getFileSystemManager();
      fs.writeFile({
        filePath: filePath,
        data: pdfData,
        encoding: 'binary',
        success: () => {
          wx.hideLoading();
          console.log('PDF文件保存成功:', filePath);
          
          // 打开文件
          wx.openDocument({
            filePath: filePath,
            showMenu: true,
            fileType: 'pdf',
            success: (result) => {
              console.log('文档打开成功');
              wx.showToast({
                title: 'PDF生成成功',
                icon: 'success',
                duration: 2000
              });
            },
            fail: (err) => {
              console.error('文档打开失败:', err);
              wx.showToast({
                title: '文档打开失败',
                icon: 'none',
                duration: 2000
              });
              // 提供备选方案：提示用户文件已保存
              setTimeout(() => {
                wx.showModal({
                  title: '文件已保存',
                  content: 'PDF文件已成功保存到本地，您可以尝试通过文件管理器找到并打开它。',
                  showCancel: false
                });
              }, 2500);
            }
          });
        },
        fail: (err) => {
          wx.hideLoading();
          console.error('PDF文件写入失败:', err);
          wx.showToast({
            title: '文件保存失败',
            icon: 'none',
            duration: 2000
          });
          
          // 尝试使用wx.saveFile作为备选方案
          this.tryAlternativeSaveMethod(pdfData, fileName);
        }
      });
    } catch (error) {
      wx.hideLoading();
      console.error('处理PDF数据发生异常:', error);
      wx.showToast({
        title: '处理文件失败',
        icon: 'none',
        duration: 2000
      });
    }
  },
  
  /**
   * 从本地存储加载用户信息
   */
  loadUserInfoFromStorage: function() {
    try {
      console.log('开始加载用户信息');
      // 从本地存储获取用户信息
      const userInfo = wx.getStorageSync('userInfo');
      if (userInfo) {
        const parsedUserInfo = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo;
        console.log('从本地存储加载用户信息成功:', parsedUserInfo);
        
        // 确保userInfo对象结构完整，如果没有name字段，可以尝试从其他属性获取
        if (!parsedUserInfo.name && parsedUserInfo.nickName) {
          parsedUserInfo.name = parsedUserInfo.nickName;
          console.log('使用nickName作为name字段:', parsedUserInfo.name);
        }
        
        this.setData({
          userInfo: parsedUserInfo
        });
        console.log('设置userInfo后的数据:', this.data.userInfo);
      } else {
        console.log('本地存储中无用户信息');
        // 如果没有用户信息，尝试从app.globalData获取
        const app = getApp();
        if (app.globalData && app.globalData.userInfo) {
          console.log('从app.globalData获取用户信息:', app.globalData.userInfo);
          
          // 确保userInfo对象结构完整
          const globalUserInfo = app.globalData.userInfo;
          if (!globalUserInfo.name && globalUserInfo.nickName) {
            globalUserInfo.name = globalUserInfo.nickName;
            console.log('使用globalData中的nickName作为name字段:', globalUserInfo.name);
          }
          
          this.setData({
            userInfo: globalUserInfo
          });
          console.log('设置globalData的userInfo后的数据:', this.data.userInfo);
        } else {
          console.log('app.globalData中也无用户信息');
          // 如果都没有用户信息，创建一个默认的用户信息对象，确保name字段存在
          const defaultUserInfo = { name: '用户姓名' };
          console.log('创建默认用户信息:', defaultUserInfo);
          this.setData({
            userInfo: defaultUserInfo
          });
          console.log('设置默认userInfo后的数据:', this.data.userInfo);
        }
      }
    } catch (e) {
      console.error('解析本地存储的用户信息失败:', e);
      // 发生错误时，创建默认用户信息
      const defaultUserInfo = { name: '用户姓名' };
      this.setData({
        userInfo: defaultUserInfo
      });
      console.log('发生错误后设置默认userInfo:', this.data.userInfo);
    }
  },

  /**
   * 尝试使用备选方法保存PDF文件
   * @param {ArrayBuffer} pdfData - PDF文件的二进制数据
   * @param {string} fileName - 文件名
   */
  tryAlternativeSaveMethod: function(pdfData, fileName) {
    console.log('尝试使用备选方法保存PDF文件');
    
    // 创建临时文件路径
    const tempFilePath = `${wx.env.USER_DATA_PATH}/temp_${fileName}`;
    const fs = wx.getFileSystemManager();
    
    try {
      // 写入临时文件
      fs.writeFileSync(tempFilePath, pdfData, 'binary');
      
      // 然后使用wx.saveFile保存
      wx.saveFile({
        tempFilePath: tempFilePath,
        success: (res) => {
          console.log('备选方法保存成功:', res.savedFilePath);
          wx.showModal({
            title: '文件保存成功',
            content: 'PDF文件已保存，但可能需要通过文件管理器访问。',
            showCancel: false
          });
        },
        fail: (err) => {
          console.error('备选方法保存失败:', err);
          wx.showModal({
            title: '保存失败',
            content: '无法保存PDF文件，请检查存储空间。',
            showCancel: false
          });
        }
      });
    } catch (error) {
      console.error('备选保存方法异常:', error);
    }
  },
  
  /**
   * 监听页面显示时同步数据
   * 确保从其他页面返回时，数据是最新的
   */
  onShow: function() {
    // 每次显示页面时先重新加载用户信息
    console.log('页面显示，先加载用户信息');
    this.loadUserInfoFromStorage();
    // 然后重新加载简历数据
    console.log('页面显示，重新加载简历数据');
    this.loadResumeData();
  },
  
  /**
   * 规范化教育经历数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的教育经历数组
   */
  normalizeEducationData: function(rawData) {
    const education = rawData.education || [];
    return education.map(item => ({
      school: item.school || '',
      degree: item.degree || '',
      major: item.major || '',
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      description: item.description || item.details || ''
    }));
  },
  
  /**
   * 规范化工作经历数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的工作经历数组
   */
  normalizeWorkExperience: function(rawData) {
    const work = rawData.work || rawData.workExperience || [];
    return work.map(item => ({
      company: item.company || '',
      position: item.position || '',
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      description: item.description || item.details || ''
    }));
  },
  
  /**
   * 规范化项目经验数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的项目经验数组
   */
  normalizeProjectExperience: function(rawData) {
    const projects = rawData.projects || [];
    return projects.map(item => ({
      name: item.name || item.projectName || '',
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      description: item.description || item.details || ''
    }));
  },
  
  /**
   * 规范化技能数据
   * @param {Object} rawData - 原始数据
   * @returns {Array} 规范化的技能数组
   */
  normalizeSkillsData: function(rawData) {
    console.log('normalizeSkillsData - 原始数据:', rawData);
    console.log('normalizeSkillsData - rawData.skills:', rawData.skills);
    console.log('normalizeSkillsData - rawData.skillsWithLevel:', rawData.skillsWithLevel);
    
    const skills = rawData.skills || rawData.skillsWithLevel || [];
    console.log('normalizeSkillsData - 最终使用的 skills:', skills);
    console.log('normalizeSkillsData - skills 类型:', typeof skills);
    console.log('normalizeSkillsData - skills 是数组吗:', Array.isArray(skills));
    
    const result = skills.map(item => {
      console.log('处理技能项:', item);
      const processed = {
        name: item.name || item.skillName || '',
        level: item.level || item.proficiency || 50 // 默认50%熟练度
      };
      console.log('处理结果:', processed);
      return processed;
    });
    
    console.log('normalizeSkillsData - 最终结果:', result);
    return result;
  },

  /**
   * 测试简历截图功能
   * 使用原生canvas方式将当前简历页面转换为图片
   */
  takeScreenshot: function() {
    console.log('开始测试简历截图功能');
    console.log('当前模板ID:', this.data.templateId);
    
    wx.showLoading({
      title: '正在生成截图...',
    });

    // 首先确保Canvas初始化完成
    this.initializeCanvas();

    // 等待DOM更新完成后再执行截图
    setTimeout(() => {
      // 定义多个可能的选择器，按优先级排序
      const possibleSelectors = [
        `.template-${this.data.templateId}-container`,  // 模板专用容器（如 .template-four-container）
        `.template-content`,
        `.preview-container`,
        `.template-container`,
        `.resume-container`,
        `#template-${this.data.templateId}`,
        `#template-container`,
        `.template-one-container`,
        `.template-two-container`,
        `.template-three-container`,
        `.template-four-container`,
        `.template-five-container`,
        `.template-six-container`
      ];
      
      console.log('尝试的选择器列表:', possibleSelectors);
      
      // 使用递归方式尝试每个选择器
      this.trySelectors(possibleSelectors, 0)
        .then(result => {
          console.log('截图成功，图片路径:', result.imagePath);
          console.log('使用的选择器:', result.selector);
          this.handleScreenshotSuccess(result.imagePath);
        })
        .catch(error => {
          console.error('所有选择器都失败:', error);
          this.handleScreenshotError(error);
        });
        
    }, 1000); // 增加到1000ms延迟确保Canvas初始化完成
  },

  /**
   * 递归尝试多个选择器
   */
  trySelectors: function(selectors, index) {
    return new Promise((resolve, reject) => {
      if (index >= selectors.length) {
        reject(new Error('所有选择器都失败，无法找到模板容器'));
        return;
      }
      
      const selector = selectors[index];
      console.log(`尝试选择器 [${index + 1}/${selectors.length}]:`, selector);
      
      this.generateResumeScreenshot(selector)
        .then(imagePath => {
          resolve({
            imagePath: imagePath,
            selector: selector
          });
        })
        .catch(error => {
          console.log(`选择器 ${selector} 失败:`, error.message);
          // 尝试下一个选择器
          this.trySelectors(selectors, index + 1)
            .then(resolve)
            .catch(reject);
        });
    });
  },

  /**
   * 处理截图成功
   */
  handleScreenshotSuccess: function(imagePath) {
    wx.hideLoading();
    
    // 显示截图结果
    wx.previewImage({
      urls: [imagePath],
      current: imagePath,
      success: () => {
        console.log('截图预览成功');
        wx.showToast({
          title: '截图生成成功',
          icon: 'success',
          duration: 2000
        });
      },
      fail: (err) => {
        console.error('截图预览失败:', err);
        // 如果预览失败，显示保存选项
        this.showScreenshotSaveOption(imagePath);
      }
    });
  },

  /**
   * 处理截图错误
   */
  handleScreenshotError: function(error) {
    wx.hideLoading();
    console.error('截图生成失败:', error);
    
    let errorMessage = '截图生成失败';
    if (error && error.message) {
      if (error.message.includes('找不到')) {
        errorMessage = '未找到简历内容，请确保简历已加载完成';
      } else if (error.message.includes('canvas')) {
        errorMessage = '截图生成失败，请重试';
      }
    }
    
    wx.showToast({
      title: errorMessage,
      icon: 'none',
      duration: 3000
    });
  },

  /**
   * 生成简历截图
   * 使用原生canvas将简历内容绘制为图片
   * @param {string} selector - 要转换的元素选择器
   * @returns {Promise<string>} 返回图片路径的Promise
   */
  generateResumeScreenshot: function(selector) {
    return new Promise((resolve, reject) => {
      console.log('开始生成截图，选择器:', selector);
      console.log('当前模板ID:', this.data.templateId);
      console.log('尝试的选择器列表:', this.getCurrentTemplateSelectors());
      
      // 创建查询选择器
      const query = wx.createSelectorQuery();
      
      query.select(selector).boundingClientRect();
      query.selectViewport().scrollOffset();
      query.exec((res) => {
        console.log('选择器查询结果:', res);
        
        if (!res[0]) {
          console.error('找不到指定选择器的元素:', selector);
          console.log('将尝试查找所有可能的模板容器...');
          
          // 尝试查找所有可能的模板容器
          this.findAvailableTemplateContainer().then(containerInfo => {
            if (containerInfo) {
              console.log('找到可用容器:', containerInfo);
              console.log('使用模板:', this.data.templateId, '生成完整页面截图');
              this.generateScreenshotWithContainer(containerInfo, resolve, reject);
            } else {
              reject(new Error('找不到可用的模板容器'));
            }
          }).catch(reject);
          return;
        }

        console.log('找到指定元素，开始生成截图');
        this.generateScreenshotWithContainer(res[0], resolve, reject);
      });
    });
  },

  /**
   * 获取当前模板的选择器列表
   */
  getCurrentTemplateSelectors: function() {
    const currentTemplateId = this.data.templateId;
    
    // 根据模板ID返回特定的选择器列表
    const templateSelectors = {
      'template-one': [
        '.template-one-container',
        '.template-content',
        '.preview-container',
        '.template-container',
        '.resume-container'
      ],
      'template-two': [
        '.resume-page',  // template-two使用.resume-page作为根容器
        '.template-content',
        '.preview-container',
        '.template-container',
        '.resume-container'
      ],
      'template-three': [
        '.resume-page',  // template-three也使用.resume-page作为根容器
        '.template-content',
        '.preview-container',
        '.template-container',
        '.resume-container'
      ],
      'template-four': [
        '.template-four-container',
        '.template-content',
        '.preview-container',
        '.template-container',
        '.resume-container'
      ],
      'template-five': [
        '.template-five-container',
        '.template-content',
        '.preview-container',
        '.template-container',
        '.resume-container'
      ],
      'template-six': [
        '.template-six-container',
        '.template-content',
        '.preview-container',
        '.template-container',
        '.resume-container'
      ]
    };
    
    // 返回当前模板的选择器列表，如果不存在则使用通用列表
    return templateSelectors[currentTemplateId] || [
      `.template-${currentTemplateId}-container`,
      '.resume-page',
      '.template-content',
      '.preview-container',
      '.template-container',
      '.resume-container'
    ];
  },

  /**
   * 查找可用的模板容器
   */
  findAvailableTemplateContainer: function() {
    return new Promise((resolve) => {
      // 使用getCurrentTemplateSelectors获取当前模板的选择器列表
      const possibleSelectors = this.getCurrentTemplateSelectors();
      
      console.log('查找可用模板容器，尝试选择器:', possibleSelectors);
      
      const query = wx.createSelectorQuery();
      
      // 查询所有可能的选择器
      possibleSelectors.forEach(selector => {
        query.select(selector).boundingClientRect();
      });
      
      query.exec((results) => {
        // 找到第一个有效的容器
        for (let i = 0; i < results.length; i++) {
          if (results[i] && results[i].width > 0 && results[i].height > 0) {
            console.log('找到有效容器:', possibleSelectors[i], results[i]);
            resolve(results[i]);
            return;
          }
        }
        
        // 如果没有找到任何容器，使用默认尺寸
        console.log('未找到有效容器，使用默认尺寸');
        resolve({
          width: 375,  // 默认宽度（iPhone标准宽度）
          height: 1200, // 增加默认高度以容纳完整模板内容
          top: 0,
          left: 0
        });
      });
    });
  },

  /**
   * 使用找到的容器生成截图
   */
  generateScreenshotWithContainer: function(rect, resolve, reject) {
    // 获取整个页面的完整尺寸，确保截图包含所有内容
    const canvasWidth = Math.max(rect.width, 750); // 使用标准简历宽度750px
    const canvasHeight = Math.max(rect.height, 1200); // 使用标准简历高度1200px，确保包含所有内容
    
    console.log('原始容器尺寸:', rect);
    console.log('完整Canvas目标尺寸:', { width: canvasWidth, height: canvasHeight });
    console.log('截图将包含整个模板页面内容');

    try {
      // 先确保Canvas元素存在且尺寸正确
      this.ensureCanvasReady(canvasWidth, canvasHeight);
      
      // 创建canvas上下文
      const ctx = wx.createCanvasContext('captureCanvas', this);
      
      if (!ctx) {
        throw new Error('无法创建Canvas上下文');
      }
      
      // 重要：在绘制前先清空canvas并设置正确尺寸
      ctx.clearRect(0, 0, canvasWidth, canvasHeight);
      ctx.setFillStyle('#ffffff');
      ctx.fillRect(0, 0, canvasWidth, canvasHeight);

      // 获取页面样式信息 - 传入完整的Canvas尺寸和当前模板信息
      this.drawTemplateToCanvas(ctx, this.data.templateId, { 
        width: canvasWidth, 
        height: canvasHeight,
        fullPage: true // 标记需要绘制完整页面
      }).then(() => {
        console.log('完整模板页面绘制完成，准备转换图片...');
        console.log('绘制内容包括：个人信息、教育背景、工作经历、技能特长等所有区域');
        
        // 重要：在转换前增加额外延迟，确保Canvas内容完全渲染
        setTimeout(() => {
          console.log('延迟完成，开始完整页面转换...');
          this.convertCanvasToImage(canvasWidth, canvasHeight, rect, resolve, reject);
        }, 500); // 增加到500ms确保渲染完成
        
      }).catch(error => {
        console.error('绘制完整模板页面失败:', error);
        reject(error);
      });
    } catch (error) {
      console.error('生成截图容器失败:', error);
      // 如果Canvas创建失败，尝试系统截图
      this.trySystemScreenshot(rect, resolve, reject);
    }
  },

  /**
   * 确保Canvas准备就绪
   */
  ensureCanvasReady: function(width, height) {
    console.log('确保Canvas准备就绪，尺寸:', { width, height });
    
    // 重要：移除.in(this)限制，因为Canvas在页面根级别，不在组件内部
    const query = wx.createSelectorQuery();
    query.select('#captureCanvas').boundingClientRect((rect) => {
      if (rect) {
        console.log('Canvas元素存在:', rect);
        
        // 如果Canvas尺寸为0，尝试重新设置
        if (rect.width === 0 || rect.height === 0) {
          console.warn('Canvas尺寸为0，需要重新设置');
          
          // 使用setData更新Canvas样式
          this.setData({
            canvasStyle: `width: ${width}px; height: ${height}px; position: absolute; left: -9999px; top: -9999px;`
          });
        }
      } else {
        console.error('Canvas元素不存在，将尝试创建');
        
        // 如果Canvas不存在，立即设置样式
        this.setData({
          canvasStyle: `width: ${width}px; height: ${height}px; position: absolute; left: -9999px; top: -9999px;`
        });
      }
    }).exec();
  },

  /**
   * 转换Canvas为图片
   */
  convertCanvasToImage: function(canvasWidth, canvasHeight, rect, resolve, reject) {
    console.log('开始转换Canvas为图片...');
    console.log('Canvas尺寸:', { width: canvasWidth, height: canvasHeight });
    
    // 重要：在转换前检查Canvas内容
    this.checkCanvasContent(() => {
      // 额外增加延迟，确保背景完全渲染
      setTimeout(() => {
        console.log('开始Canvas转换，背景色应该已完全渲染');
        
        try {
          // 使用标准Canvas转换 - 重要：确保组件上下文正确
          wx.canvasToTempFilePath({
            canvasId: 'captureCanvas',
            x: 0,
            y: 0,
            width: canvasWidth,
            height: canvasHeight,
            destWidth: canvasWidth, // 使用原始尺寸避免缩放问题
            destHeight: canvasHeight,
            quality: 1,
            fileType: 'png', // 明确指定PNG格式
            success: (res) => {
              console.log('Canvas转图片成功:', res.tempFilePath);
              console.log('图片尺寸:', { width: canvasWidth, height: canvasHeight });
              
              // 验证图片是否有效
              this.validateGeneratedImage(res.tempFilePath, resolve, reject);
            },
            fail: (err) => {
              console.error('Canvas转图片失败:', err);
              console.error('失败详情:', {
                canvasId: 'captureCanvas',
                width: canvasWidth,
                height: canvasHeight,
                destWidth: canvasWidth,
                destHeight: canvasHeight,
                error: err
              });
              
              // 如果Canvas转换失败，尝试使用系统截图API
              this.trySystemScreenshot(rect, resolve, reject);
            }
          }, this); // 重要：必须传递this作为第二个参数
        } catch (error) {
          console.error('Canvas转换调用异常:', error);
          // 如果Canvas转换异常，尝试使用系统截图API
          this.trySystemScreenshot(rect, resolve, reject);
        }
      }, 300); // 额外300ms延迟确保背景渲染
    });
  },

  /**
   * 检查Canvas内容
   */
  checkCanvasContent: function(callback) {
    console.log('检查Canvas内容...');
    
    // 重要：移除.in(this)限制，因为Canvas在页面根级别
    const query = wx.createSelectorQuery();
    query.select('#captureCanvas').fields({
      node: true,
      size: true
    }).exec((res) => {
      if (res[0]) {
        console.log('Canvas节点信息:', res[0]);
        const canvas = res[0].node;
        if (canvas) {
          console.log('Canvas节点存在，尺寸:', res[0].width, 'x', res[0].height);
          
          // 尝试获取Canvas像素数据来验证内容
          try {
            const ctx = canvas.getContext('2d');
            if (ctx) {
              // 获取左上角像素颜色
              const imageData = ctx.getImageData(0, 0, 1, 1);
              console.log('左上角像素数据:', imageData.data);
              
              // 检查是否为黑色 (0,0,0) 或透明 (0,0,0,0)
              const isBlack = imageData.data[0] === 0 && imageData.data[1] === 0 && imageData.data[2] === 0;
              const isTransparent = imageData.data[3] === 0;
              
              if (isBlack || isTransparent) {
                console.warn('Canvas可能为黑色或透明，需要重新绘制');
              } else {
                console.log('Canvas像素数据正常');
              }
            }
          } catch (e) {
            console.log('无法获取像素数据:', e);
          }
        }
      } else {
        console.log('无法获取Canvas节点信息，Canvas可能不存在');
      }
      
      // 无论检查结果如何，继续转换流程
      callback();
    });
  },

  /**
   * 验证生成的图片是否有效
   */
  validateGeneratedImage: function(imagePath, resolve, reject) {
    console.log('验证生成的图片:', imagePath);
    
    // 使用getImageInfo检查图片
    wx.getImageInfo({
      src: imagePath,
      success: (info) => {
        console.log('图片信息:', info);
        
        // 检查图片尺寸是否合理
        if (info.width > 0 && info.height > 0) {
          console.log('图片尺寸有效:', { width: info.width, height: info.height });
          resolve(imagePath);
        } else {
          console.error('图片尺寸无效');
          reject(new Error('生成的图片尺寸无效'));
        }
      },
      fail: (err) => {
        console.error('获取图片信息失败:', err);
        // 即使验证失败，也返回原路径让用户尝试
        resolve(imagePath);
      }
    });
  },

  /**
   * 尝试使用系统截图API作为后备方案
   */
  trySystemScreenshot: function(rect, resolve, reject) {
    console.log('尝试使用系统截图API...');
    
    // 首先尝试使用页面截图API
    try {
      wx.canvasToTempFilePath({
        canvasId: 'captureCanvas',
        x: 0,
        y: 0,
        width: rect.width || 375,
        height: rect.height || 800,
        destWidth: (rect.width || 375) * 2,
        destHeight: (rect.height || 800) * 2,
        quality: 1,
        success: (res) => {
          console.log('系统截图成功:', res.tempFilePath);
          resolve(res.tempFilePath);
        },
        fail: (err) => {
          console.error('系统截图失败:', err);
          
          // 尝试使用页面截图API作为最终后备
          this.tryPageScreenshot(rect, resolve, reject);
        }
      }, this);
    } catch (error) {
      console.error('系统截图调用异常:', error);
      this.tryPageScreenshot(rect, resolve, reject);
    }
  },
  
  /**
   * 尝试使用页面截图API
   */
  tryPageScreenshot: function(rect, resolve, reject) {
    console.log('尝试使用页面截图API...');
    
    // 使用页面截图API
    wx.canvasGetImageData({
      canvasId: 'captureCanvas',
      x: 0,
      y: 0,
      width: Math.min(rect.width || 375, 1000),
      height: Math.min(rect.height || 800, 1000),
      success: (res) => {
        console.log('页面截图API成功，数据长度:', res.data.length);
        
        // 创建临时图片文件
        this.createImageFromData(res.data, res.width, res.height, resolve, reject);
      },
      fail: (err) => {
        console.error('页面截图API也失败:', err);
        
        // 最后尝试离屏Canvas
        this.tryOffscreenCanvas(rect, resolve, reject);
      }
    });
  },
  
  /**
   * 从图片数据创建文件
   */
  createImageFromData: function(imageData, width, height, resolve, reject) {
    console.log('从图片数据创建文件...');
    
    try {
      // 使用离屏Canvas创建图片
      const offscreenCanvas = wx.createOffscreenCanvas({
        type: '2d',
        width: width,
        height: height
      });
      
      const ctx = offscreenCanvas.getContext('2d');
      
      // 创建ImageData对象
      const imgData = ctx.createImageData(width, height);
      for (let i = 0; i < imageData.length; i++) {
        imgData.data[i] = imageData[i];
      }
      
      // 将ImageData绘制到Canvas
      ctx.putImageData(imgData, 0, 0);
      
      // 转换为临时文件
      wx.canvasToTempFilePath({
        canvas: offscreenCanvas,
        x: 0,
        y: 0,
        width: width,
        height: height,
        destWidth: width * 2,
        destHeight: height * 2,
        quality: 1,
        success: (res) => {
          console.log('从数据创建图片成功:', res.tempFilePath);
          resolve(res.tempFilePath);
        },
        fail: (err) => {
          console.error('从数据创建图片失败:', err);
          reject(err);
        }
      });
    } catch (error) {
      console.error('从数据创建图片异常:', error);
      reject(error);
    }
  },

  /**
   * 尝试使用离屏Canvas
   */
  tryOffscreenCanvas: function(rect, resolve, reject) {
    console.log('尝试使用离屏Canvas...');
    
    try {
      // 创建离屏Canvas
      const offscreenCanvas = wx.createOffscreenCanvas({
        type: '2d',
        width: rect.width || 375,
        height: rect.height || 800
      });
      
      if (!offscreenCanvas) {
        throw new Error('无法创建离屏Canvas');
      }
      
      const ctx = offscreenCanvas.getContext('2d');
      
      if (!ctx) {
        throw new Error('无法获取离屏Canvas上下文');
      }
      
      // 绘制背景
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, rect.width || 375, rect.height || 800);
      
      // 绘制模板内容
      this.drawTemplateToOffscreenCanvas(ctx, this.data.templateId, { 
        width: rect.width || 375, 
        height: rect.height || 800 
      }).then(() => {
        // 转换为图片
        wx.canvasToTempFilePath({
          canvas: offscreenCanvas,
          x: 0,
          y: 0,
          width: rect.width || 375,
          height: rect.height || 800,
          destWidth: (rect.width || 375) * 2,
          destHeight: (rect.height || 800) * 2,
          quality: 1,
          success: (res) => {
            console.log('离屏Canvas转图片成功:', res.tempFilePath);
            resolve(res.tempFilePath);
          },
          fail: (err) => {
            console.error('离屏Canvas转图片失败:', err);
            this.showFinalErrorFallback(reject);
          }
        });
      }).catch(error => {
        console.error('离屏Canvas绘制失败:', error);
        this.showFinalErrorFallback(reject);
      });
      
    } catch (error) {
      console.error('离屏Canvas创建失败:', error);
      this.showFinalErrorFallback(reject);
    }
  },

  /**
   * 显示最终错误回退
   */
  showFinalErrorFallback: function(reject) {
    console.error('所有截图方法都失败了');
    
    // 最后尝试使用页面截图API
    this.tryFinalPageScreenshot(reject);
  },
  
  /**
   * 最终页面截图尝试
   */
  tryFinalPageScreenshot: function(reject) {
    console.log('尝试最终页面截图方案...');
    
    try {
      // 使用页面截图API作为最后手段
      wx.canvasToTempFilePath({
        canvasId: 'captureCanvas',
        success: (res) => {
          console.log('最终页面截图成功:', res.tempFilePath);
          wx.hideLoading();
          
          // 显示截图结果
          wx.previewImage({
            urls: [res.tempFilePath],
            current: res.tempFilePath,
            success: () => {
              console.log('截图预览成功');
              wx.showToast({
                title: '截图生成成功',
                icon: 'success',
                duration: 2000
              });
            },
            fail: (err) => {
              console.error('截图预览失败:', err);
              wx.showToast({
                title: '截图生成成功',
                icon: 'success',
                duration: 2000
              });
            }
          });
        },
        fail: (err) => {
          console.error('最终页面截图也失败:', err);
          wx.hideLoading();
          wx.showToast({
            title: '截图失败，请重试',
            icon: 'none',
            duration: 2000
          });
          reject(new Error('截图失败，所有方法都不可用'));
        }
      }, this);
    } catch (error) {
      console.error('最终页面截图异常:', error);
      wx.hideLoading();
      wx.showToast({
        title: '截图失败，请重试',
        icon: 'none',
        duration: 2000
      });
      reject(new Error('截图失败，所有方法都不可用'));
    }
  },
  
  /**
   * 绘制模板到离屏Canvas
   */
  drawTemplateToOffscreenCanvas: function(ctx, templateId, dimensions) {
    console.log('开始绘制模板到离屏Canvas，模板ID:', templateId);
    console.log('简历数据:', this.data.resumeData);
    console.log('用户信息:', this.data.userInfo);
    
    return new Promise((resolve, reject) => {
      try {
        // 获取个人信息
        const personalInfo = this.getPersonalInfoData();
        console.log('整合的个人信息:', personalInfo);
        
        // 设置起始位置
        let currentY = 60;
        const lineHeight = 35;
        const leftMargin = 40;
        
        // 模板配色方案
        const colorSchemes = {
          'template-one': { bgColor: '#f8f9fa', primaryColor: '#2c3e50' },
          'template-two': { bgColor: '#ffffff', primaryColor: '#34495e' },
          'template-three': { bgColor: '#f5f7fa', primaryColor: '#409eff' },
          'template-four': { bgColor: '#ffffff', primaryColor: '#67c23a' },
          'template-five': { bgColor: '#fafafa', primaryColor: '#e6a23c' },
          'template-six': { bgColor: '#f0f2f5', primaryColor: '#f56c6c' }
        };
        
        const colors = colorSchemes[templateId] || colorSchemes['template-one'];
        
        // 绘制背景
        ctx.fillStyle = colors.bgColor;
        ctx.fillRect(0, 0, dimensions.width, dimensions.height);
        
        // 绘制标题
        ctx.fillStyle = colors.primaryColor;
        ctx.font = 'bold 24px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('个人简历', dimensions.width / 2, currentY);
        currentY += lineHeight * 1.5;
        
        // 绘制个人信息
        ctx.textAlign = 'left';
        ctx.font = '16px sans-serif';
        ctx.fillText(`姓名: ${personalInfo.name}`, leftMargin, currentY);
        currentY += lineHeight;
        ctx.fillText(`职位: ${personalInfo.jobTitle}`, leftMargin, currentY);
        currentY += lineHeight;
        ctx.fillText(`电话: ${personalInfo.phone}`, leftMargin, currentY);
        currentY += lineHeight;
        ctx.fillText(`邮箱: ${personalInfo.email}`, leftMargin, currentY);
        currentY += lineHeight * 1.5;
        
        // 绘制技能信息（如果有）
        if (this.data.resumeData.skills && this.data.resumeData.skills.length > 0) {
          ctx.font = 'bold 18px sans-serif';
          ctx.fillText('技能特长', leftMargin, currentY);
          currentY += lineHeight;
          
          ctx.font = '14px sans-serif';
          this.data.resumeData.skills.forEach(skill => {
            ctx.fillText(`• ${skill}`, leftMargin + 20, currentY);
            currentY += lineHeight;
          });
          currentY += lineHeight * 0.5;
        }
        
        // 绘制底部信息
        ctx.fillStyle = '#909399';
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('简历生成时间: ' + new Date().toLocaleString(), dimensions.width / 2, dimensions.height - 30);
        
        console.log('离屏Canvas模板绘制完成');
        resolve();
        
      } catch (error) {
        console.error('离屏Canvas绘制模板失败:', error);
        reject(error);
      }
    });
  },

  /**
   * 绘制template-one的完整布局（模拟两栏布局）
   */
  drawTemplateOneFullLayout: function(ctx, personalInfo, resumeData, colors, rect, startY, resolve, templateId) {
    console.log('绘制template-one完整两栏布局');
    
    const { leftMargin, rightMargin, lineHeight, primaryColor, textColor, secondaryColor, bgColor } = colors;
    const panelWidth = (rect.width - leftMargin - rightMargin - 20) / 2; // 两栏宽度
    const leftPanelX = leftMargin;
    const rightPanelX = leftMargin + panelWidth + 20;
    
    let leftY = startY;
    let rightY = startY;
    
    // 右侧：个人信息区域（模拟template-one的右侧面板）
    ctx.setFillStyle('#f8f9fa');
    ctx.fillRect(rightPanelX - 10, startY - 30, panelWidth + 20, rect.height - startY - 100);
    
    // 右侧：头像和基本信息
    if (personalInfo.name) {
      ctx.setFillStyle(textColor);
      ctx.setFontSize(24);
      ctx.fillText(personalInfo.name, rightPanelX, rightY);
      rightY += lineHeight;
      
      if (personalInfo.jobTitle) {
        ctx.setFontSize(18);
        ctx.setFillStyle(primaryColor);
        ctx.fillText(personalInfo.jobTitle, rightPanelX, rightY);
        rightY += lineHeight;
      }
      
      // 联系信息
      ctx.setFillStyle(secondaryColor);
      ctx.setFontSize(14);
      if (personalInfo.phone) {
        ctx.fillText(`📞 ${personalInfo.phone}`, rightPanelX, rightY);
        rightY += lineHeight * 0.8;
      }
      if (personalInfo.email) {
        ctx.fillText(`📧 ${personalInfo.email}`, rightPanelX, rightY);
        rightY += lineHeight * 0.8;
      }
      if (personalInfo.address) {
        ctx.fillText(`📍 ${personalInfo.address}`, rightPanelX, rightY);
        rightY += lineHeight * 0.8;
      }
    }
    
    // 左侧：教育经历
    if (resumeData && resumeData.education && resumeData.education.length > 0) {
      leftY += 20;
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(20);
      ctx.fillText('教育背景', leftPanelX, leftY);
      leftY += lineHeight;
      
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(leftPanelX, leftY - 5, 30, 2);
      leftY += 15;
      
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.education.forEach((edu) => {
        if (leftY < rect.height - 100) {
          const school = edu.school || '未知学校';
          const major = edu.major || '未知专业';
          const startDate = edu.startDate || '';
          const endDate = edu.endDate || '';
          
          ctx.fillText(`${school}`, leftPanelX, leftY);
          leftY += lineHeight * 0.8;
          
          if (major) {
            ctx.setFillStyle(secondaryColor);
            ctx.fillText(`${major}`, leftPanelX, leftY);
            leftY += lineHeight * 0.8;
            ctx.setFillStyle(textColor);
          }
          
          if (startDate || endDate) {
            ctx.setFillStyle(secondaryColor);
            ctx.fillText(`${startDate} - ${endDate}`, leftPanelX, leftY);
            leftY += lineHeight * 0.8;
            ctx.setFillStyle(textColor);
          }
          
          leftY += 10;
        }
      });
    }
    
    // 左侧：工作经历
    if (resumeData && resumeData.workExperience && resumeData.workExperience.length > 0) {
      leftY += 20;
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(20);
      ctx.fillText('工作经历', leftPanelX, leftY);
      leftY += lineHeight;
      
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(leftPanelX, leftY - 5, 30, 2);
      leftY += 15;
      
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.workExperience.forEach((work) => {
        if (leftY < rect.height - 100) {
          const company = work.company || '未知公司';
          const position = work.position || '未知职位';
          const startDate = work.startDate || '';
          const endDate = work.endDate || '';
          
          ctx.fillText(`${company} - ${position}`, leftPanelX, leftY);
          leftY += lineHeight * 0.8;
          
          if (startDate || endDate) {
            ctx.setFillStyle(secondaryColor);
            ctx.fillText(`${startDate} - ${endDate}`, leftPanelX, leftY);
            leftY += lineHeight * 0.8;
            ctx.setFillStyle(textColor);
          }
          
          if (work.description) {
            ctx.setFontSize(14);
            ctx.fillText(work.description.substring(0, 50) + '...', leftPanelX, leftY);
            leftY += lineHeight * 0.8;
            ctx.setFontSize(16);
          }
          
          leftY += 10;
        }
      });
    }
    
    // 右侧：技能特长
    if (resumeData && resumeData.skillsWithLevel && resumeData.skillsWithLevel.length > 0) {
      rightY += 40;
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(20);
      ctx.fillText('专业技能', rightPanelX, rightY);
      rightY += lineHeight;
      
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(rightPanelX, rightY - 5, 30, 2);
      rightY += 15;
      
      ctx.setFillStyle(textColor);
      ctx.setFontSize(14);
      
      resumeData.skillsWithLevel.forEach((skill) => {
        if (rightY < rect.height - 100) {
          const skillName = skill.name || '未知技能';
          const skillLevel = skill.level || 0;
          
          ctx.fillText(`• ${skillName}`, rightPanelX, rightY);
          
          // 技能进度条
          if (skillLevel > 0) {
            const barWidth = 60;
            const barHeight = 3;
            const filledWidth = (skillLevel / 100) * barWidth;
            
            ctx.setFillStyle('#e0e0e0');
            ctx.fillRect(rightPanelX + 80, rightY - 8, barWidth, barHeight);
            
            ctx.setFillStyle(primaryColor);
            ctx.fillRect(rightPanelX + 80, rightY - 8, filledWidth, barHeight);
          }
          
          rightY += lineHeight * 0.8;
        }
      });
    }
    
    // 底部信息
    const bottomY = rect.height - 40;
    ctx.setFillStyle('#e0e0e0');
    ctx.fillRect(leftMargin, bottomY - 20, rect.width - leftMargin - rightMargin, 1);
    
    ctx.setFillStyle(secondaryColor);
    ctx.setFontSize(12);
    ctx.fillText(`模板: ${templateId} | 生成时间: ${new Date().toLocaleString()}`, leftMargin, bottomY);
    
    console.log('template-one完整布局绘制完成');
  },

  /**
   * 绘制template-two的完整布局（顶部header + 分栏内容）
   */
  drawTemplateTwoFullLayout: function(ctx, personalInfo, resumeData, colors, rect, startY, resolve, templateId) {
    console.log('绘制template-two完整布局');
    
    const { leftMargin, rightMargin, lineHeight, primaryColor, textColor, secondaryColor, bgColor } = colors;
    
    // 顶部Header区域（模拟template-two的header样式）
    ctx.setFillStyle('#2c3e50');
    ctx.fillRect(0, startY - 20, rect.width, 180);
    
    let yPosition = startY;
    
    // Header内容
    ctx.setFillStyle('#ffffff');
    ctx.setFontSize(28);
    if (personalInfo.name) {
      ctx.fillText(personalInfo.name, leftMargin, yPosition);
    }
    
    if (personalInfo.jobTitle) {
      ctx.setFontSize(18);
      ctx.setFillStyle('#ecf0f1');
      ctx.fillText(personalInfo.jobTitle, leftMargin, yPosition + 35);
    }
    
    // Header右侧联系信息
    ctx.setFontSize(14);
    let contactY = yPosition;
    if (personalInfo.phone) {
      ctx.fillText(`📞 ${personalInfo.phone}`, rect.width - 200, contactY);
      contactY += 25;
    }
    if (personalInfo.email) {
      ctx.fillText(`📧 ${personalInfo.email}`, rect.width - 200, contactY);
      contactY += 25;
    }
    if (personalInfo.address) {
      ctx.fillText(`📍 ${personalInfo.address}`, rect.width - 200, contactY);
      contactY += 25;
    }
    
    yPosition += 140; // Header结束
    
    // 主要内容区域
    const sectionSpacing = 30;
    
    // 教育经历
    if (resumeData && resumeData.education && resumeData.education.length > 0) {
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(22);
      ctx.fillText('教育经历', leftMargin, yPosition);
      yPosition += lineHeight;
      
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(leftMargin, yPosition - 8, 50, 3);
      yPosition += 20;
      
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.education.forEach((edu) => {
        if (yPosition < rect.height - 100) {
          const school = edu.school || '未知学校';
          const major = edu.major || '未知专业';
          const degree = edu.degree || '学历';
          const startDate = edu.startDate || '';
          const endDate = edu.endDate || '';
          
          ctx.fillText(`${school} - ${major}`, leftMargin, yPosition);
          yPosition += lineHeight * 0.8;
          
          ctx.setFillStyle(secondaryColor);
          ctx.fillText(`${degree} | ${startDate} - ${endDate}`, leftMargin + 20, yPosition);
          yPosition += lineHeight * 0.8;
          
          ctx.setFillStyle(textColor);
          yPosition += 15;
        }
      });
    }
    
    yPosition += sectionSpacing;
    
    // 工作经历
    if (resumeData && resumeData.workExperience && resumeData.workExperience.length > 0) {
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(22);
      ctx.fillText('工作经历', leftMargin, yPosition);
      yPosition += lineHeight;
      
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(leftMargin, yPosition - 8, 50, 3);
      yPosition += 20;
      
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.workExperience.forEach((work) => {
        if (yPosition < rect.height - 100) {
          const company = work.company || '未知公司';
          const position = work.position || '未知职位';
          const startDate = work.startDate || '';
          const endDate = work.endDate || '';
          
          ctx.fillText(`${company} - ${position}`, leftMargin, yPosition);
          yPosition += lineHeight * 0.8;
          
          ctx.setFillStyle(secondaryColor);
          ctx.fillText(`${startDate} - ${endDate}`, leftMargin + 20, yPosition);
          yPosition += lineHeight * 0.8;
          
          if (work.description) {
            ctx.setFontSize(14);
            const desc = work.description.length > 80 ? work.description.substring(0, 80) + '...' : work.description;
            ctx.fillText(desc, leftMargin + 20, yPosition);
            yPosition += lineHeight * 0.8;
            ctx.setFontSize(16);
          }
          
          ctx.setFillStyle(textColor);
          yPosition += 20;
        }
      });
    }
    
    yPosition += sectionSpacing;
    
    // 项目经验
    if (resumeData && resumeData.projectExperienceList && resumeData.projectExperienceList.length > 0) {
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(22);
      ctx.fillText('项目经验', leftMargin, yPosition);
      yPosition += lineHeight;
      
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(leftMargin, yPosition - 8, 50, 3);
      yPosition += 20;
      
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.projectExperienceList.forEach((project) => {
        if (yPosition < rect.height - 100) {
          const name = project.name || project.projectName || '项目名称';
          const startDate = project.startDate || '';
          const endDate = project.endDate || '';
          
          ctx.fillText(name, leftMargin, yPosition);
          yPosition += lineHeight * 0.8;
          
          ctx.setFillStyle(secondaryColor);
          ctx.fillText(`${startDate} - ${endDate}`, leftMargin + 20, yPosition);
          yPosition += lineHeight * 0.8;
          
          if (project.description) {
            ctx.setFontSize(14);
            const desc = project.description.length > 80 ? project.description.substring(0, 80) + '...' : project.description;
            ctx.fillText(desc, leftMargin + 20, yPosition);
            yPosition += lineHeight * 0.8;
            ctx.setFontSize(16);
          }
          
          ctx.setFillStyle(textColor);
          yPosition += 20;
        }
      });
    }
    
    yPosition += sectionSpacing;
    
    // 技能
    if (resumeData && resumeData.skillsWithLevel && resumeData.skillsWithLevel.length > 0) {
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(22);
      ctx.fillText('专业技能', leftMargin, yPosition);
      yPosition += lineHeight;
      
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(leftMargin, yPosition - 8, 50, 3);
      yPosition += 20;
      
      // 技能标签云
      const skillTags = resumeData.skillsWithLevel.map(skill => skill.name || skill).filter(Boolean);
      if (skillTags.length > 0) {
        ctx.setFillStyle(textColor);
        ctx.setFontSize(14);
        
        let skillX = leftMargin;
        let skillY = yPosition;
        const tagPadding = 10;
        const tagHeight = 25;
        
        skillTags.forEach((skill, index) => {
          if (skillY < rect.height - 100) {
            const tagWidth = ctx.measureText(skill).width + tagPadding * 2;
            
            // 换行处理
            if (skillX + tagWidth > rect.width - rightMargin) {
              skillX = leftMargin;
              skillY += tagHeight + 5;
            }
            
            // 绘制技能标签背景
            ctx.setFillStyle('#f8f9fa');
            ctx.fillRect(skillX, skillY - 15, tagWidth, tagHeight);
            
            // 绘制技能文字
            ctx.setFillStyle(primaryColor);
            ctx.fillText(skill, skillX + tagPadding, skillY);
            
            skillX += tagWidth + 5;
          }
        });
        
        yPosition = skillY + tagHeight + 20;
      }
    }
    
    // 底部信息
    const bottomY = rect.height - 40;
    ctx.setFillStyle('#e0e0e0');
    ctx.fillRect(leftMargin, bottomY - 20, rect.width - leftMargin - rightMargin, 1);
    
    ctx.setFillStyle(secondaryColor);
    ctx.setFontSize(12);
    ctx.fillText(`模板: ${templateId} | 生成时间: ${new Date().toLocaleString()}`, leftMargin, bottomY);
    
    console.log('template-two完整布局绘制完成');
  },



  /**
   * 将模板内容绘制到canvas
   * @param {Object} ctx - canvas上下文
   * @param {string} templateId - 模板ID
   * @param {Object} rect - 容器尺寸信息
   * @returns {Promise<void>}
   */
  drawTemplateToCanvas: function(ctx, templateId, rect) {
    return new Promise((resolve) => {
      const resumeData = this.data.resumeData;
      const userInfo = this.data.userInfo;
      
      console.log('开始绘制模板到canvas，模板ID:', templateId);
      console.log('简历数据:', resumeData);
      console.log('用户信息:', userInfo);
      
      // 设置背景色 - 根据模板ID使用不同的背景色
      let bgColor = '#ffffff';
      let primaryColor = '#1677ff';
      let textColor = '#333333';
      let secondaryColor = '#666666';
      
      // 根据模板设置不同的配色方案
      switch(templateId) {
        case 'template-one':
          bgColor = '#f8f9fa';
          primaryColor = '#2c3e50';
          break;
        case 'template-two':
          bgColor = '#ffffff';
          primaryColor = '#e74c3c';
          break;
        case 'template-three':
          bgColor = '#f5f7fa';
          primaryColor = '#3498db';
          break;
        case 'template-four':
          bgColor = '#ffffff';
          primaryColor = '#9b59b6';
          break;
        case 'template-five':
          bgColor = '#f8f9fa';
          primaryColor = '#27ae60';
          break;
        case 'template-six':
          bgColor = '#ffffff';
          primaryColor = '#f39c12';
          break;
      }
      
      // 绘制背景 - 多重保障确保背景可见
      console.log('绘制背景色:', bgColor, '尺寸:', rect.width, 'x', rect.height);
      
      // 第一层：基础背景
      ctx.setFillStyle(bgColor);
      ctx.fillRect(0, 0, rect.width, rect.height);
      
      // 第二层：强制刷新背景色
      ctx.save();
      ctx.setFillStyle(bgColor);
      ctx.fillRect(0, 0, rect.width, rect.height);
      ctx.restore();
      
      // 第三层：添加白色边框确保可见性
      ctx.setStrokeStyle('#ffffff');
      ctx.setLineWidth(2);
      ctx.strokeRect(1, 1, rect.width - 2, rect.height - 2);
      
      console.log('背景绘制完成');
      
      // 根据是否完整页面绘制调整布局参数
      const isFullPage = rect.fullPage === true;
      let yPosition = isFullPage ? 80 : 60; // 完整页面时增加顶部边距
      const lineHeight = isFullPage ? 40 : 35; // 完整页面时增加行高
      const leftMargin = isFullPage ? 60 : 40; // 完整页面时增加左边距
      const rightMargin = isFullPage ? 60 : 40; // 完整页面时增加右边距
      const contentWidth = rect.width - leftMargin - rightMargin;
      
      console.log(`开始绘制完整模板内容，全页面模式: ${isFullPage}, 尺寸: ${rect.width}x${rect.height}`);
      
      // 绘制顶部装饰线
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(leftMargin, yPosition - 30, 60, 4);
      
      // 绘制标题 - 使用模板名称
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(32);
      ctx.fillText('个人简历', leftMargin, yPosition);
      yPosition += lineHeight * 1.2;
      
      // 获取个人信息数据
      const personalInfo = this.getPersonalInfoData();
      console.log('整合的个人信息:', personalInfo);
      
      // 根据模板类型绘制不同的布局
      if (isFullPage) {
        if (templateId === 'template-one') {
          // template-one 特有布局：左侧内容 + 右侧个人信息
          this.drawTemplateOneFullLayout(ctx, personalInfo, resumeData, {
            leftMargin, rightMargin, lineHeight, primaryColor, textColor, secondaryColor, bgColor
          }, rect, yPosition, resolve, templateId);
          return; // 使用专用布局方法，提前返回
        } else if (templateId === 'template-two') {
          // template-two 特有布局：顶部header + 分栏内容
          this.drawTemplateTwoFullLayout(ctx, personalInfo, resumeData, {
            leftMargin, rightMargin, lineHeight, primaryColor, textColor, secondaryColor, bgColor
          }, rect, yPosition, resolve, templateId);
          return; // 使用专用布局方法，提前返回
        } else if (templateId === 'template-four') {
          // template-four 特有布局：左侧栏（个人信息+技能）+ 右侧栏（教育/工作/项目）
          this.drawTemplateFourFullLayout(ctx, personalInfo, resumeData, {
            leftMargin, rightMargin, lineHeight, primaryColor, textColor, secondaryColor, bgColor
          }, rect, yPosition, resolve, templateId);
          return; // 使用专用布局方法，提前返回
        }
      }
      
      // 通用模板布局（适用于其他模板）
      // 绘制个人信息区域
      if (personalInfo.name) {
        // 姓名（大字体）
        ctx.setFillStyle(textColor);
        ctx.setFontSize(28);
        ctx.fillText(personalInfo.name, leftMargin, yPosition);
        yPosition += lineHeight;
        
        // 职位/标题
        if (personalInfo.jobTitle) {
          ctx.setFontSize(20);
          ctx.setFillStyle(primaryColor);
          ctx.fillText(personalInfo.jobTitle, leftMargin, yPosition);
          yPosition += lineHeight * 1.2;
        }
        
        // 联系信息（小字体，灰色）
        ctx.setFillStyle(secondaryColor);
        ctx.setFontSize(16);
        
        const contactLines = [];
        if (personalInfo.phone) contactLines.push(`📞 ${personalInfo.phone}`);
        if (personalInfo.email) contactLines.push(`📧 ${personalInfo.email}`);
        if (personalInfo.address) contactLines.push(`📍 ${personalInfo.address}`);
        
        contactLines.forEach(line => {
          if (yPosition < rect.height - 100) { // 确保不超出画布
            ctx.fillText(line, leftMargin, yPosition);
            yPosition += lineHeight * 0.8;
          }
        });
      }
      
      // 绘制技能信息
      if (resumeData && resumeData.skillsWithLevel && resumeData.skillsWithLevel.length > 0) {
        yPosition += 20;
        
        // 区域标题
        ctx.setFillStyle(primaryColor);
        ctx.setFontSize(22);
        ctx.fillText('专业技能', leftMargin, yPosition);
        yPosition += lineHeight * 0.8;
        
        // 标题下划线
        ctx.setFillStyle(primaryColor);
        ctx.fillRect(leftMargin, yPosition - 8, 40, 2);
        yPosition += 15;
        
        // 技能列表
        ctx.setFillStyle(textColor);
        ctx.setFontSize(16);
        
        resumeData.skillsWithLevel.slice(0, 6).forEach((skill, index) => {
          if (yPosition < rect.height - 100) {
            const skillName = skill.name || '未知技能';
            const skillLevel = skill.level || 0;
            
            // 技能名称
            ctx.fillText(`• ${skillName}`, leftMargin, yPosition);
            
            // 技能等级（进度条效果）
            if (skillLevel > 0) {
              const barWidth = 100;
              const barHeight = 4;
              const filledWidth = (skillLevel / 100) * barWidth;
              
              // 背景条
              ctx.setFillStyle('#e0e0e0');
              ctx.fillRect(leftMargin + 120, yPosition - 8, barWidth, barHeight);
              
              // 填充条
              ctx.setFillStyle(primaryColor);
              ctx.fillRect(leftMargin + 120, yPosition - 8, filledWidth, barHeight);
              
              // 等级文字
              ctx.setFillStyle(secondaryColor);
              ctx.setFontSize(12);
              ctx.fillText(`${skillLevel}%`, leftMargin + 230, yPosition);
              ctx.setFontSize(16);
              ctx.setFillStyle(textColor);
            }
            
            yPosition += lineHeight * 0.9;
          }
        });
      }
      
      // 绘制教育背景
      if (resumeData && resumeData.education && resumeData.education.length > 0) {
        yPosition += 20;
        
        // 区域标题
        ctx.setFillStyle(primaryColor);
        ctx.setFontSize(22);
        ctx.fillText('教育背景', leftMargin, yPosition);
        yPosition += lineHeight * 0.8;
        
        // 标题下划线
        ctx.setFillStyle(primaryColor);
        ctx.fillRect(leftMargin, yPosition - 8, 40, 2);
        yPosition += 15;
        
        // 教育经历列表
        ctx.setFillStyle(textColor);
        ctx.setFontSize(16);
        
        resumeData.education.slice(0, 3).forEach((edu, index) => {
          if (yPosition < rect.height - 100) {
            const school = edu.school || '未知学校';
            const major = edu.major || '未知专业';
            const startDate = edu.startDate || '';
            const endDate = edu.endDate || '';
            
            // 学校和专业
            ctx.setFontSize(16);
            ctx.fillText(`${school} - ${major}`, leftMargin, yPosition);
            yPosition += lineHeight * 0.8;
            
            // 时间段
            if (startDate || endDate) {
              ctx.setFillStyle(secondaryColor);
              ctx.setFontSize(14);
              ctx.fillText(`${startDate} - ${endDate}`, leftMargin, yPosition);
              yPosition += lineHeight * 0.8;
              ctx.setFillStyle(textColor);
            }
            
            // 添加间距
            if (index < resumeData.education.length - 1) {
              yPosition += 10;
            }
          }
        });
      }
      
      // 绘制工作经验
      if (resumeData && resumeData.workExperience && resumeData.workExperience.length > 0) {
        yPosition += 20;
        
        // 区域标题
        ctx.setFillStyle(primaryColor);
        ctx.setFontSize(22);
        ctx.fillText('工作经验', leftMargin, yPosition);
        yPosition += lineHeight * 0.8;
        
        // 标题下划线
        ctx.setFillStyle(primaryColor);
        ctx.fillRect(leftMargin, yPosition - 8, 40, 2);
        yPosition += 15;
        
        // 工作经历列表
        ctx.setFillStyle(textColor);
        ctx.setFontSize(16);
        
        resumeData.workExperience.slice(0, 3).forEach((work, index) => {
          if (yPosition < rect.height - 100) {
            const company = work.company || '未知公司';
            const position = work.position || '未知职位';
            const startDate = work.startDate || '';
            const endDate = work.endDate || '';
            
            // 公司和职位
            ctx.setFontSize(16);
            ctx.fillText(`${company} - ${position}`, leftMargin, yPosition);
            yPosition += lineHeight * 0.8;
            
            // 时间段
            if (startDate || endDate) {
              ctx.setFillStyle(secondaryColor);
              ctx.setFontSize(14);
              ctx.fillText(`${startDate} - ${endDate}`, leftMargin, yPosition);
              yPosition += lineHeight * 0.8;
              ctx.setFillStyle(textColor);
            }
            
            // 添加间距
            if (index < resumeData.workExperience.length - 1) {
              yPosition += 10;
            }
          }
        });
      }
      
      // 绘制底部信息
      const bottomY = rect.height - 40;
      if (yPosition < bottomY - 50) {
        ctx.setFillStyle('#e0e0e0');
        ctx.fillRect(leftMargin, bottomY - 20, contentWidth, 1);
        
        ctx.setFillStyle(secondaryColor);
        ctx.setFontSize(12);
        ctx.fillText(`模板: ${templateId} | 生成时间: ${new Date().toLocaleString()}`, leftMargin, bottomY);
      }
      
      // 重要：确保绘制完成后再resolve
      ctx.draw(false, () => {
        console.log('模板绘制完成回调触发');
        
        // 增加延迟确保绘制完全完成
        setTimeout(() => {
          console.log('延迟完成，resolve Promise');
          resolve();
        }, 100);
      });
    });
  },

  /**
   * 获取整合的个人信息数据
   */
  getPersonalInfoData: function() {
    const resumeData = this.data.resumeData;
    const userInfo = this.data.userInfo;
    
    // 优先使用userInfo中的数据，如果没有则使用resumeData中的数据
    return {
      name: (userInfo && userInfo.name) || (resumeData && resumeData.personalInfo && resumeData.personalInfo.name) || '',
      jobTitle: (userInfo && userInfo.jobTitle) || (resumeData && resumeData.personalInfo && resumeData.personalInfo.jobTitle) || '',
      phone: (resumeData && resumeData.personalInfo && resumeData.personalInfo.phone) || '',
      email: (resumeData && resumeData.personalInfo && resumeData.personalInfo.email) || '',
      address: (resumeData && resumeData.personalInfo && resumeData.personalInfo.address) || '',
      expectedSalary: (resumeData && resumeData.personalInfo && resumeData.personalInfo.expectedSalary) || ''
    };
  },

  /**
   * 显示截图保存选项
   * @param {string} imagePath - 图片路径
   */
  showScreenshotSaveOption: function(imagePath) {
    wx.showModal({
      title: '截图已生成',
      content: '截图生成成功！是否保存到相册？',
      success: (res) => {
        if (res.confirm) {
          // 保存图片到相册
          wx.saveImageToPhotosAlbum({
            filePath: imagePath,
            success: () => {
              wx.showToast({
                title: '已保存到相册',
                icon: 'success',
                duration: 2000
              });
            },
            fail: (err) => {
              console.error('保存到相册失败:', err);
              wx.showToast({
                title: '保存失败',
                icon: 'none',
                duration: 2000
              });
            }
          });
        }
      }
    });
  },

  /**
   * template-four 专用完整布局绘制方法
   * 左侧栏：个人信息 + 技能
   * 右侧栏：教育背景 + 工作经验 + 项目经验
   */
  drawTemplateFourFullLayout: function(ctx, personalInfo, resumeData, styleOptions, rect, startY, resolve, templateId) {
    const { leftMargin, rightMargin, lineHeight, primaryColor, textColor, secondaryColor, bgColor } = styleOptions;
    
    // 布局参数
    const leftColumnWidth = rect.width * 0.3; // 左侧栏宽度 30%
    const rightColumnWidth = rect.width * 0.7; // 右侧栏宽度 70%
    const leftColumnX = 0;
    const rightColumnX = leftColumnWidth;
    
    // 背景色填充
    ctx.setFillStyle('#2c3e50'); // 左侧栏深蓝色背景
    ctx.fillRect(0, 0, leftColumnWidth, rect.height);
    
    ctx.setFillStyle('#ffffff'); // 右侧栏白色背景
    ctx.fillRect(leftColumnWidth, 0, rightColumnWidth, rect.height);
    
    let leftY = 60; // 左侧栏起始Y坐标
    let rightY = 60; // 右侧栏起始Y坐标
    
    // === 左侧栏绘制 ===
    
    // 1. 个人信息区域
    if (personalInfo && personalInfo.name) {
      // 姓名
      ctx.setFillStyle('#ffffff');
      ctx.setFontSize(24);
      ctx.fillText(personalInfo.name, leftColumnX + 30, leftY);
      leftY += lineHeight * 1.2;
      
      // 职位
      if (personalInfo.jobTitle) {
        ctx.setFillStyle('#bdc3c7');
        ctx.setFontSize(16);
        ctx.fillText(personalInfo.jobTitle, leftColumnX + 30, leftY);
        leftY += lineHeight * 1.5;
      }
      
      // 联系信息
      ctx.setFillStyle('#ecf0f1');
      ctx.setFontSize(14);
      if (personalInfo.phone) {
        ctx.fillText(`📱 ${personalInfo.phone}`, leftColumnX + 30, leftY);
        leftY += lineHeight * 0.8;
      }
      if (personalInfo.email) {
        ctx.fillText(`📧 ${personalInfo.email}`, leftColumnX + 30, leftY);
        leftY += lineHeight * 0.8;
      }
      if (personalInfo.address) {
        ctx.fillText(`📍 ${personalInfo.address}`, leftColumnX + 30, leftY);
        leftY += lineHeight * 0.8;
      }
      if (personalInfo.expectedSalary) {
        ctx.fillText(`💰 ${personalInfo.expectedSalary}`, leftColumnX + 30, leftY);
        leftY += lineHeight * 0.8;
      }
    }
    
    // 2. 技能区域
    if (resumeData && resumeData.skills && resumeData.skills.length > 0) {
      leftY += 40; // 增加间距
      
      // 技能标题
      ctx.setFillStyle('#3498db');
      ctx.setFontSize(18);
      ctx.fillText('专业技能', leftColumnX + 30, leftY);
      leftY += lineHeight * 0.8;
      
      // 技能列表
      ctx.setFillStyle('#ecf0f1');
      ctx.setFontSize(14);
      
      resumeData.skills.slice(0, 8).forEach((skill, index) => {
        if (leftY < rect.height - 100) {
          const skillName = skill.name || '未知技能';
          const skillLevel = parseInt(skill.level) || 0;
          
          // 技能名称
          ctx.fillText(skillName, leftColumnX + 30, leftY);
          
          // 进度条背景
          const barWidth = 80;
          const barHeight = 4;
          const filledWidth = (skillLevel / 100) * barWidth;
          
          ctx.setFillStyle('#34495e');
          ctx.fillRect(leftColumnX + 120, leftY - 8, barWidth, barHeight);
          
          // 进度条填充
          ctx.setFillStyle('#3498db');
          ctx.fillRect(leftColumnX + 120, leftY - 8, filledWidth, barHeight);
          
          leftY += lineHeight * 0.9;
        }
      });
    }
    
    // === 右侧栏绘制 ===
    
    // 1. 教育背景
    if (resumeData && resumeData.education && resumeData.education.length > 0) {
      // 标题
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(22);
      ctx.fillText('教育背景', rightColumnX + 40, rightY);
      rightY += lineHeight * 0.8;
      
      // 标题下划线
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(rightColumnX + 40, rightY - 8, 40, 2);
      rightY += 20;
      
      // 教育经历
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.education.slice(0, 3).forEach((edu, index) => {
        if (rightY < rect.height - 200) {
          const school = edu.school || '未知学校';
          const major = edu.major || '未知专业';
          const startDate = edu.startDate || '';
          const endDate = edu.endDate || '';
          
          // 学校和专业
          ctx.setFontSize(16);
          ctx.fillText(`${school} - ${major}`, rightColumnX + 40, rightY);
          rightY += lineHeight * 0.8;
          
          // 时间段
          if (startDate || endDate) {
            ctx.setFillStyle(secondaryColor);
            ctx.setFontSize(14);
            ctx.fillText(`${startDate} - ${endDate}`, rightColumnX + 40, rightY);
            rightY += lineHeight * 0.8;
            ctx.setFillStyle(textColor);
          }
          
          // 添加间距
          if (index < resumeData.education.length - 1) {
            rightY += 10;
          }
        }
      });
    }
    
    // 2. 工作经验
    if (resumeData && resumeData.workExperience && resumeData.workExperience.length > 0) {
      rightY += 30;
      
      // 标题
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(22);
      ctx.fillText('工作经验', rightColumnX + 40, rightY);
      rightY += lineHeight * 0.8;
      
      // 标题下划线
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(rightColumnX + 40, rightY - 8, 40, 2);
      rightY += 20;
      
      // 工作经历
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.workExperience.slice(0, 4).forEach((work, index) => {
        if (rightY < rect.height - 100) {
          const company = work.company || '未知公司';
          const position = work.position || '未知职位';
          const startDate = work.startDate || '';
          const endDate = work.endDate || '';
          
          // 公司和职位
          ctx.setFontSize(16);
          ctx.fillText(`${company} - ${position}`, rightColumnX + 40, rightY);
          rightY += lineHeight * 0.8;
          
          // 时间段
          if (startDate || endDate) {
            ctx.setFillStyle(secondaryColor);
            ctx.setFontSize(14);
            ctx.fillText(`${startDate} - ${endDate}`, rightColumnX + 40, rightY);
            rightY += lineHeight * 0.8;
            ctx.setFillStyle(textColor);
          }
          
          // 添加间距
          if (index < resumeData.workExperience.length - 1) {
            rightY += 15;
          }
        }
      });
    }
    
    // 3. 项目经验
    if (resumeData && resumeData.projects && resumeData.projects.length > 0) {
      rightY += 30;
      
      // 标题
      ctx.setFillStyle(primaryColor);
      ctx.setFontSize(22);
      ctx.fillText('项目经验', rightColumnX + 40, rightY);
      rightY += lineHeight * 0.8;
      
      // 标题下划线
      ctx.setFillStyle(primaryColor);
      ctx.fillRect(rightColumnX + 40, rightY - 8, 40, 2);
      rightY += 20;
      
      // 项目经历
      ctx.setFillStyle(textColor);
      ctx.setFontSize(16);
      
      resumeData.projects.slice(0, 3).forEach((project, index) => {
        if (rightY < rect.height - 50) {
          const projectName = project.name || '未知项目';
          const role = project.role || '未知角色';
          const startDate = project.startDate || '';
          const endDate = project.endDate || '';
          
          // 项目名称和角色
          ctx.setFontSize(16);
          ctx.fillText(`${projectName} - ${role}`, rightColumnX + 40, rightY);
          rightY += lineHeight * 0.8;
          
          // 时间段
          if (startDate || endDate) {
            ctx.setFillStyle(secondaryColor);
            ctx.setFontSize(14);
            ctx.fillText(`${startDate} - ${endDate}`, rightColumnX + 40, rightY);
            rightY += lineHeight * 0.8;
            ctx.setFillStyle(textColor);
          }
          
          // 添加间距
          if (index < resumeData.projects.length - 1) {
            rightY += 15;
          }
        }
      });
    }
    
    // 绘制底部信息
    const bottomY = rect.height - 40;
    ctx.setFillStyle('#e0e0e0');
    ctx.fillRect(leftColumnWidth, bottomY - 20, rightColumnWidth, 1);
    
    ctx.setFillStyle(secondaryColor);
    ctx.setFontSize(12);
    ctx.fillText(`模板: ${templateId} | 生成时间: ${new Date().toLocaleString()}`, rightColumnX + 40, bottomY);
    
    // 重要：确保绘制完成后再resolve
    ctx.draw(false, () => {
      console.log('template-four 专用布局绘制完成回调触发');
      
      // 增加延迟确保绘制完全完成
      setTimeout(() => {
        console.log('template-four 延迟完成，resolve Promise');
        resolve();
      }, 100);
    });
  }
})