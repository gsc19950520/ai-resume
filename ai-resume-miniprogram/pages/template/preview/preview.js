// preview.js - 动态模板预览版本
const { request } = require('../../../utils/request');
Page({
  
  data: {
    templateId: 'template-one',  // 默认模板ID
    templateName: '技术人才模板',  // 默认模板名称
    imagePath: '/images/template-one.png',  // 默认图片路径
    templateUpdateTime: new Date().getTime(),  // 用于触发视图更新的时间戳
    resumeData: null,  // 简历数据
    loading: true,  // 加载状态
    apiBaseUrl: 'https://your-api-base-url/api', // API基础URL
    enableMock: true // 是否启用模拟PDF下载功能
  },
  
  /**
   * 获取配置的API基础URL
   */
  getApiBaseUrl: function() {
    // 优先从全局配置中获取，如果没有则使用data中的默认值
    const app = getApp();
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
    
    // 加载简历数据
    this.loadResumeData();
  },
  
  /**
   * 加载简历数据
   */
  loadResumeData: function() {
    try {
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
        console.log('从resumeData获取数据:', JSON.stringify(storedData, null, 2));
        
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
      console.log('最终使用的数据:', rawData);
      this.setData({
        resumeData: rawData,
        loading: false
      });
    } catch (error) {
      console.error('加载简历数据失败:', error);
      this.setData({
        loading: false
      });
    }
  },
  
  /**
   * 从后端获取完整的简历数据
   */
  loadResumeDataFromBackend: function(resumeId) {
    wx.request({
      url: `https://your-api-base-url/api/resume/${resumeId}/full-data`,
      method: 'GET',
      success: (res) => {
        if (res.data && !res.data.error) {
          console.log('从后端获取简历数据成功:', res.data);
          // 转换后端数据格式为前端需要的格式
          const formattedData = this.formatBackendResumeData(res.data);
          this.setData({
            resumeData: formattedData,
            loading: false
          });
        } else {
          console.error('获取简历数据失败:', res.data);
          this.handleResumeLoadError();
        }
      },
      fail: (err) => {
        console.error('请求后端失败:', err);
        this.handleResumeLoadError();
      }
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
   * 格式化后端返回的简历数据
   */
  formatBackendResumeData: function(backendData) {
    return {
      personalInfo: backendData.personalInfo || {
        name: backendData.name || '',
        jobTitle: backendData.jobTitle || '',
        phone: backendData.phone || '',
        email: backendData.email || '',
        address: backendData.address || '',
        birthDate: backendData.birthDate || '',
        expectedSalary: backendData.expectedSalary || '',
        startTime: backendData.startTime || '',
        avatar: backendData.avatar || '',
        selfEvaluation: backendData.selfEvaluation || ''
      },
      contact: backendData.contact || {},
      education: backendData.education || [],
      workExperience: backendData.workExperience || [],
      projectExperienceList: backendData.projects || [],
      skillsWithLevel: backendData.skills || [],
      hobbies: backendData.hobbies || [],
      selfEvaluation: backendData.selfEvaluation || ''
    };
  },

  /**
   * 检查用户信息是否完整
   * @param {Object} userInfo - 用户信息对象
   * @returns {boolean} - 用户信息是否完整
   */
  checkUserInfoComplete: function(userInfo) {
    // 根据业务需求定义用户信息的完整性检查标准
    // 检查姓名、手机号、邮箱等必要字段
    const requiredFields = ['name', 'phone', 'email'];
    const isComplete = requiredFields.every(field => 
      userInfo && userInfo[field] && userInfo[field].trim() !== ''
    );
    
    return isComplete;
  },
  
  /**
   * 加载用户信息
   * @returns {Promise<Object>} - 用户信息对象的Promise
   */
  loadUserInfo: function() {
    return new Promise((resolve, reject) => {
      const userId = wx.getStorageSync('userId');
      if (userId) {
        // 使用云托管方式请求用户信息
        request(`/user/${userId}`, {}, 'GET')
          .then(res => {
            if (res && res.success) {
              console.log('用户信息加载成功:', res.data);
              resolve(res.data);
            } else {
              console.error('加载用户信息失败:', res);
              // 尝试从本地存储获取用户信息（适用于模拟环境）
              this.loadUserInfoFromStorage().then(userInfo => {
                resolve(userInfo);
              }).catch(err => {
                reject(err);
              });
            }
          })
          .catch(err => {
            console.error('请求用户信息失败:', err);
            // 尝试从本地存储获取用户信息（适用于模拟环境）
            this.loadUserInfoFromStorage().then(userInfo => {
              resolve(userInfo);
            }).catch(err => {
              reject(err);
            });
          });
      } else {
        // 尝试从本地存储获取用户信息（适用于模拟环境）
        this.loadUserInfoFromStorage().then(userInfo => {
          resolve(userInfo);
        }).catch(err => {
          reject(err);
        });
      }
    });
  },
  
  /**
   * 从本地存储加载用户信息（适用于模拟环境）
   * @returns {Promise<Object>} - 用户信息对象的Promise
   */
  loadUserInfoFromStorage: function() {
    return new Promise((resolve, reject) => {
      try {
        const userInfo = wx.getStorageSync('userInfo');
        if (userInfo) {
          const parsedUserInfo = typeof userInfo === 'string' ? JSON.parse(userInfo) : userInfo;
          console.log('从本地存储加载用户信息成功:', parsedUserInfo);
          resolve(parsedUserInfo);
        } else {
          console.log('本地存储中无用户信息');
          reject(new Error('未找到用户信息'));
        }
      } catch (e) {
        console.error('解析本地存储的用户信息失败:', e);
        reject(e);
      }
    });
  },
  
  /**
   * 使用当前模板
   * 将选择的模板信息和当前简历数据保存到本地存储，并跳转到简历编辑页面
   */
  useTemplate: function() {
    const { templateId, templateName, resumeData } = this.data;
    
    // 加载用户信息并检查完整性
    this.loadUserInfo().then(userInfo => {
      // 检查用户信息是否完整
      if (!this.checkUserInfoComplete(userInfo)) {
        console.log('用户信息不完整，跳转到提示页面');
        // 保存当前页面信息，用于返回
        wx.setStorageSync('previewOptions', {
          templateId: templateId,
          templateName: templateName
        });
        // 跳转到完善个人信息提示页面
        wx.navigateTo({
          url: '/pages/profile/complete-profile/complete-profile?returnTo=/pages/template/preview/preview'
        });
      } else {
        // 用户信息完整，继续使用模板流程
        // 保存选择的模板信息和当前简历数据
        const templateInfo = {
          templateId: templateId,
          templateName: templateName,
          title: '我的新简历',
          isAiGenerated: false
        };
        
        // 如果有简历数据，合并到模板信息中
        if (resumeData) {
          Object.assign(templateInfo, resumeData);
        }
        
        wx.setStorageSync('tempResumeInfo', templateInfo);
        
        wx.showToast({
          title: '正在准备模板...',
          icon: 'success'
        });
        
        // 跳转到简历编辑页面
        setTimeout(() => {
          wx.navigateTo({
            url: `/pages/resume/edit/edit?templateId=${templateId}`
          });
        }, 1000);
      }
    }).catch(err => {
      console.error('检查用户信息失败:', err);
      // 保存当前页面信息，用于返回
      wx.setStorageSync('previewOptions', {
        templateId: templateId,
        templateName: templateName
      });
      // 出错时也跳转到完善个人信息页面
      wx.navigateTo({
        url: '/pages/profile/complete-profile/complete-profile?returnTo=/pages/template/preview/preview'
      });
    });
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
   * 调用后端API生成并下载PDF，支持模拟模式
   */
  downloadPdf: function() {
    const { templateId, resumeData, enableMock } = this.data;
    
    // 显示加载提示
    wx.showLoading({
      title: '正在生成PDF...',
      mask: true
    });
    
    // 如果启用了模拟模式，则直接使用模拟数据
    if (enableMock) {
      console.log('使用模拟PDF下载功能');
      this.mockPdfDownload();
      return;
    }
    
    // 检查是否有resumeId
    const options = wx.getStorageSync('previewOptions') || {};
    const resumeId = options.resumeId;
    const apiBaseUrl = this.getApiBaseUrl();
    
    // 构建请求参数和URL
    let url = '';
    let data = {};
    
    if (resumeId) {
      // 如果有resumeId，调用简历导出接口
      url = `${apiBaseUrl}/resume/export/pdf`;
      data = { resumeId: resumeId };
    } else {
      // 否则，使用模板ID调用模板生成PDF接口
      url = `${apiBaseUrl}/template/${templateId}/generate-pdf`;
      // 可以将当前简历数据一并发送给后端
      data = { resumeData: resumeData };
    }
    
    console.log('调用PDF下载接口:', url, data);
    
    // 调用后端API
    wx.request({
      url: url,
      method: 'GET',
      responseType: 'arraybuffer', // 设置响应类型为arraybuffer以处理二进制数据
      data: data,
      success: (res) => {
        console.log('PDF下载请求成功:', res.statusCode);
        
        if (res.statusCode === 200 && res.data) {
          // 检查返回的数据是否为空数组（后端可能返回空byte[]）
          if (res.data.byteLength === 0) {
            wx.hideLoading();
            console.warn('后端返回空的PDF数据，切换到模拟模式');
            this.setData({ enableMock: true });
            this.mockPdfDownload();
          } else {
            // 处理返回的二进制数据
            this.handlePdfData(res.data);
          }
        } else {
          wx.hideLoading();
          wx.showToast({
            title: 'PDF生成失败',
            icon: 'none',
            duration: 2000
          });
          console.error('PDF生成失败，状态码:', res.statusCode, '响应:', res.data);
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('PDF下载请求失败，切换到模拟模式:', err);
        // 如果请求失败，尝试使用模拟模式
        this.setData({ enableMock: true });
        this.mockPdfDownload();
      }
    });
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
   * 测试模板渲染功能
   * 生成模拟数据并更新到页面，用于测试所有模板的渲染效果
   */
  testTemplateRendering: function() {
    // 生成测试数据
    const testData = {
      avatar: '/images/avatar.png',
      name: '张三',
      position: '高级前端工程师',
      phone: '13800138000',
      email: 'zhangsan@example.com',
      website: 'www.example.com',
      education: [
        {
          school: '北京大学',
          major: '计算机科学与技术',
          degree: '本科',
          startDate: '2014-09',
          endDate: '2018-06',
          description: 'GPA 3.8/4.0，获得优秀毕业生称号'
        },
        {
          school: '清华大学',
          major: '软件工程',
          degree: '硕士',
          startDate: '2018-09',
          endDate: '2021-06',
          description: '研究方向：Web前端技术，发表2篇学术论文'
        }
      ],
      workExperience: [
        {
          company: '腾讯科技',
          position: '前端开发工程师',
          startDate: '2021-07',
          endDate: '2023-03',
          description: '负责微信小程序开发，参与了3个核心项目，优化了用户体验和页面性能'
        },
        {
          company: '阿里巴巴',
          position: '高级前端工程师',
          startDate: '2023-04',
          endDate: '至今',
          description: '负责电商平台前端架构设计，带领5人团队，提升了页面加载速度30%'
        }
      ],
      projectExperience: [
        {
          name: '电商小程序',
          role: '技术负责人',
          startDate: '2022-01',
          endDate: '2022-06',
          description: '设计并开发了一款电商小程序，实现了商品展示、购物车、订单等核心功能',
          technologies: '微信小程序、JavaScript、WXML、WXSS'
        },
        {
          name: '企业管理系统',
          role: '前端架构师',
          startDate: '2023-05',
          endDate: '2023-12',
          description: '重构了企业管理系统前端架构，采用组件化开发，提高了开发效率和代码质量',
          technologies: 'React、TypeScript、Ant Design、Webpack'
        }
      ],
      skillsWithLevel: [
        { name: 'HTML/CSS', level: 3 },
        { name: 'JavaScript/TypeScript', level: 4 },
        { name: 'React/Vue', level: 5 },
        { name: '微信小程序', level: 2 },
        { name: 'Node.js', level: 2 },
        { name: 'Webpack/Vite', level: 1 }
      ],
      interests: '摄影、跑步、读书、旅游',
      selfAssessment: '拥有5年前端开发经验，精通现代前端技术栈，善于解决复杂问题，具有良好的团队协作精神和沟通能力。'
    };
    
    // 更新数据到页面
    this.setData({
      resumeData: testData,
      loading: false
    });
    
    wx.showToast({
      title: '测试数据已加载',
      icon: 'success',
      duration: 2000
    });
  },
  
  /**
   * 监听页面显示时同步数据
   * 确保从其他页面返回时，数据是最新的
   */
  onShow: function() {
    // 每次显示页面时重新加载数据，确保数据同步
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