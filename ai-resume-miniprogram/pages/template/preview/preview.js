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
    enableMock: true // 是否启用模拟PDF下载功能
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
        name: backendData.userInfo?.name || backendData.name || '',
        jobTitle: backendData.jobTitle || backendData.userInfo?.jobTitle || '',
        phone: backendData.userInfo?.phone || backendData.phone || '',
        email: backendData.userInfo?.email || backendData.email || '',
        address: backendData.userInfo?.address || backendData.address || '',
        birthDate: backendData.userInfo?.birthDate || backendData.birthDate || '',
        expectedSalary: backendData.expectedSalary || backendData.userInfo?.expectedSalary || '',
        startTime: backendData.startTime || backendData.userInfo?.startTime || '',
        avatar: backendData.userInfo?.avatarUrl || backendData.avatar || '',
        selfEvaluation: backendData.selfEvaluation || backendData.userInfo?.selfEvaluation || '',
        nickname: backendData.userInfo?.nickname || '',
        gender: backendData.userInfo?.gender || '',
        country: backendData.userInfo?.country || '',
        province: backendData.userInfo?.province || '',
        city: backendData.userInfo?.city || ''
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
  }
})