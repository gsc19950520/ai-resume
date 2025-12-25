// preview.js - 简化版本，只保留模板预览和下载PDF功能
const { request } = require('../../../utils/request');

Page({
  
  data: {
    templateId: 'template-one',  // 默认模板ID
    templateName: '技术人才模板',  // 默认模板名称
    imagePath: '/images/template-one.jpg',  // 默认图片路径
    templateUpdateTime: new Date().getTime(),  // 用于触发视图更新的时间戳
    resumeData: null,  // 简历数据
    userInfo: null,  // 用户信息
    loading: true,  // 加载状态
    apiBaseUrl: '', // 使用云托管服务，不需要硬编码URL
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
      const imagePath = `/images/${templateId}.jpg`;
      this.setData({
        templateId: templateId,
        imagePath: imagePath
      });
      
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
    
    // 立即设置一个默认的userInfo，确保页面渲染时不会出现undefined
    this.setData({
      userInfo: { name: '用户姓名' }
    });
    
    // 加载简历数据
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
  },
  
  /**
   * 加载简历数据
   * 优先从后端API获取用户最新简历数据，失败时使用本地数据
   */
  loadResumeData: function() {
    try {
      // 首先尝试从后端API获取用户最新简历数据
      this.loadResumeDataFromBackend();
      
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
   * 从后端API加载简历数据
   * 调用/api/resume/getLatest?userId=接口获取用户最新简历数据
   */
  loadResumeDataFromBackend: function() {
    const app = getApp();
    
    // 获取当前用户信息
    const userInfoStr = wx.getStorageSync('userInfo');
    const userInfo = JSON.parse(userInfoStr);
    if (!userInfo || !userInfo.id) {
      console.warn('用户ID不存在，使用本地数据');
      this.loadResumeDataFromLocal();
      return;
    }
    
    // 显示加载中
    this.setData({ loading: true });
    
    // 使用云托管调用后端API
    if (app.cloudCall) {
      // 检查云环境配置
      if (!app.globalData.cloudEnvId || !app.globalData.cloudServiceName) {
        console.warn('云托管环境配置不完整，使用本地数据');
        this.loadResumeDataFromLocal();
        return;
      }
      
      // 设置超时机制，10秒后自动降级到本地数据
      const timeoutPromise = new Promise((resolve) => {
        setTimeout(() => {
          console.warn('API调用超时，降级到本地数据');
          resolve(null); // 返回null表示超时
        }, 10000); // 10秒超时
      });
      
      const apiPromise = app.cloudCall(
        `/resume/getLatest?userId=${userInfo.id}`,
        {}, // data参数
        'GET', // method参数
        {
          'Authorization': `Bearer ${app.globalData.token || ''}`
        } // header参数
      );
      
      // 使用Promise.race实现超时控制
      Promise.race([apiPromise, timeoutPromise]).then(res => {
        
        // 检查是否超时
        if (res === null) {
          console.warn('API调用超时，使用本地数据');
          this.loadResumeDataFromLocal();
          return;
        }
        
        if (res && res.data) {
          // 使用后端返回的简历数据
          const backendData = this.formatBackendResumeData(res.data);
          
          // 如果有resumeId，保存到本地存储
          if (backendData.id) {
            wx.setStorageSync('resumeId', backendData.id);
          }
          
          // 设置简历数据
          this.setData({
            resumeData: backendData,
            loading: false,
            userInfo: this.data.userInfo || { name: '用户姓名' }
          });
        } else {
          console.log('后端未返回简历数据，使用本地数据');
          this.loadResumeDataFromLocal();
        }
        
      }).catch(err => {
        console.error('从后端API获取简历数据失败:', err);
        console.error('错误详情:', {
          message: err.message,
          stack: err.stack,
          err: JSON.stringify(err)
        });
        // API调用失败，显示错误提示并使用本地数据
        wx.showToast({
          title: '网络错误，使用本地数据',
          icon: 'none',
          duration: 2000
        });
        this.loadResumeDataFromLocal();
      });
    } else {
      console.warn('云托管调用方法不可用，使用本地数据');
      this.loadResumeDataFromLocal();
    }
  },

  /**
   * 从本地存储加载简历数据（后备方案）
   */
  loadResumeDataFromLocal: function() {
    try {
      console.log('从本地存储加载简历数据');
      
      // 优先尝试从tempResumeInfo获取用户填写的数据
      let tempResumeInfo = wx.getStorageSync('tempResumeInfo');
      let rawData = null;
      
      if (tempResumeInfo && (tempResumeInfo.personalInfo || tempResumeInfo.name)) {
        // 如果tempResumeInfo中有用户数据，使用这些数据
        console.log('从tempResumeInfo获取用户数据:', tempResumeInfo);
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
   * 格式化后端简历数据
   * 将后端返回的数据格式转换为前端模板所需的格式
   * @param {Object} backendData - 后端返回的简历数据
   * @returns {Object} 格式化后的简历数据
   */
  formatBackendResumeData: function(backendData) {
    
    // 根据后端接口返回的新数据结构进行格式化
    const formattedData = {
      // 简历基本信息
      id: backendData.id || '',
      userId: backendData.userId || '',
      jobTitle: backendData.jobTitle || '',
      jobTypeId: backendData.jobTypeId || '',  // 添加jobTypeId字段
      expectedSalary: backendData.expectedSalary || '',
      startTime: backendData.startTime || '',
      interests: backendData.interests || '',
      selfEvaluation: backendData.selfEvaluation || '',
      templateId: backendData.templateId || '',
      status: backendData.status || '',
      createTime: backendData.createTime || '',
      updateTime: backendData.updateTime || '',
      
      // 个人信息（从userInfo对象中获取）
      personalInfo: {
        name: backendData.userInfo?.name || '',
        email: backendData.userInfo?.email || '',
        phone: backendData.userInfo?.phone || '',
        address: backendData.userInfo?.address || '',
        birthDate: backendData.userInfo?.birthDate || '',
        nickname: backendData.userInfo?.nickname || '',
        avatarUrl: backendData.userInfo?.avatarUrl || '',
        gender: backendData.userInfo?.gender || '',
        country: backendData.userInfo?.country || '',
        province: backendData.userInfo?.province || '',
        city: backendData.userInfo?.city || '',
        avatar: backendData.userInfo?.avatarUrl || ''  // 兼容原有的avatar字段
      },
      
      // 关联数据
      education: backendData.educationList || [],
      educationList: backendData.educationList || [],
      workExperience: backendData.workExperienceList || [],
      workExperienceList: backendData.workExperienceList || [],
      projectList: backendData.projectList || [],
      skillList: backendData.skillList || [],
      skillsWithLevel: backendData.skillList || [],  // 兼容原有的skillsWithLevel字段
      
      // 其他字段
      hobbies: backendData.interests ? [backendData.interests] : [],
      contact: backendData.userInfo || {}
    };
    
    // 检查是否有templateId字段，用于后续模板选择
    if (backendData.templateId) {
      console.log('后端数据包含templateId:', backendData.templateId);
      this.handleTemplateSelection(backendData.templateId);
    }
    
    console.log('格式化完成的后端简历数据:', formattedData);
    return formattedData;
  },

  /**
   * 处理模板选择逻辑
   * 根据后端返回的templateId更新当前模板
   * @param {string} templateId - 模板ID
   */
  handleTemplateSelection: function(templateId) {
    console.log('处理模板选择，templateId:', templateId);
    
    // 有效的模板ID列表
    const validTemplateIds = ['template-one', 'template-two', 'template-three', 'template-four', 'template-five', 'template-six'];
    
    // 如果templateId为空或无效，使用默认模板
    if (!templateId || !validTemplateIds.includes(templateId)) {
      console.log('templateId为空或无效，使用默认模板template-one');
      templateId = 'template-one';
    }
    
    // 更新模板ID和图片路径
    const imagePath = `/images/${templateId}.jpg`;
    
    this.setData({
      templateId: templateId,
      imagePath: imagePath,
      templateUpdateTime: new Date().getTime() // 更新时间戳触发视图更新
    });
    
    console.log('模板选择完成，当前模板ID:', templateId, '图片路径:', imagePath);
  },

  /**
   * 获取resumeId，采用多层次获取策略
   * @returns {string} 获取到的resumeId
   */
  getResumeId: function() {
    let resumeId = '';
    
    // 第1层：从当前简历数据中获取
    if (this.data.resumeData && this.data.resumeData.id) {
      resumeId = this.data.resumeData.id;
      console.log('从当前简历数据中获取resumeId:', resumeId);
      return resumeId;
    }
    
    // 第2层：从全局数据中获取
    const app = getApp();
    if (app.globalData && app.globalData.resumeData && app.globalData.resumeData.id) {
      resumeId = app.globalData.resumeData.id;
      console.log('从全局数据中获取resumeId:', resumeId);
      return resumeId;
    }
    
    // 第3层：从本地存储中获取
    resumeId = wx.getStorageSync('resumeId') || '';
    if (resumeId) {
      console.log('从本地存储中获取resumeId:', resumeId);
      return resumeId;
    }
    
    // 第4层：从后端返回的完整简历数据中解析
    if (this.data.resumeData && this.data.resumeData.resumeId) {
      resumeId = this.data.resumeData.resumeId;
      console.log('从后端简历数据中解析resumeId:', resumeId);
      return resumeId;
    }
    
    console.log('未找到任何resumeId');
    return '';
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
   * 切换模板
   * @param {Object} e - 事件对象，包含目标模板ID
   */
  switchTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id;
    const validTemplateIds = ['template-one', 'template-two', 'template-three', 'template-four', 'template-five', 'template-six'];
    
    if (!templateId || !validTemplateIds.includes(templateId)) {
      console.warn('无效的模板ID:', templateId);
      return;
    }
    
    console.log('切换模板:', templateId);
    
    // 更新模板ID和图片路径
    const imagePath = `/images/${templateId}.jpg`;
    this.setData({
      templateId: templateId,
      imagePath: imagePath,
      templateUpdateTime: new Date().getTime()  // 更新时间戳以触发视图更新
    });
    
    // 显示切换成功提示
    wx.showToast({
      title: '模板切换成功',
      icon: 'success',
      duration: 1000
    });
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

    // 获取resumeId，采用多层次获取策略
    let resumeId = this.getResumeId();
    console.log('最终获取到的resumeId:', resumeId);

    // 如果仍然没有resumeId，生成临时ID
    if (!resumeId) {
      console.warn('未找到resumeId，生成临时ID用于PDF生成');
      // 生成基于用户ID和时间戳的临时resumeId
      const app = getApp();
      const userId = app.globalData.userInfo?.id || 'temp_user';
      resumeId = `temp_${userId}_${Date.now()}`;
      console.log('生成的临时resumeId:', resumeId);
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
      app.cloudCallBinary(
        `/resume/export/pdf?resumeId=${resumeId}&templateId=${templateId}`,
        {}, // data参数
        'GET', // method参数
        {
          'Authorization': `Bearer ${app.globalData.token || ''}`
        } // header参数
      ).then(pdfBuffer => {
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
      });
    } else {
      wx.hideLoading();
      console.error('云托管调用方法不可用');
      wx.showToast({
        title: '系统错误',
        icon: 'none'
      });
    }
  },

  /**
   * 编辑简历
   * 跳转到编辑页面并回显数据
   */
  editResume: function() {
    const { resumeData, templateId } = this.data;
    
    if (!resumeData) {
      wx.showToast({
        title: '暂无简历数据',
        icon: 'none'
      });
      return;
    }

    // 获取resumeId
    let resumeId = this.getResumeId();
    
    // 如果仍然没有resumeId，生成临时ID
    if (!resumeId) {
      console.warn('未找到resumeId，生成临时ID');
      const app = getApp();
      const userId = app.globalData.userInfo?.id || 'temp_user';
      resumeId = `temp_${userId}_${Date.now()}`;
    }

    console.log('跳转到编辑页面，resumeId:', resumeId, 'templateId:', templateId);

    // 将当前简历数据存储到全局变量中，以便编辑页面使用
    const app = getApp();
    app.globalData.editResumeData = resumeData;
    app.globalData.editTemplateId = templateId;
    app.globalData.editResumeId = resumeId;

    // 跳转到编辑页面
    wx.navigateTo({
      url: `/pages/resume/edit/edit?resumeId=${resumeId}&templateId=${templateId}`,
      success: function() {
        console.log('跳转到编辑页面成功');
      },
      fail: function(err) {
        console.error('跳转到编辑页面失败:', err);
        wx.showToast({
          title: '跳转失败',
          icon: 'none'
        });
      }
    });
  }
})