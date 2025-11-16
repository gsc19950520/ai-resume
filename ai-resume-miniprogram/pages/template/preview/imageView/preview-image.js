// 图片预览页面逻辑
import request from '../../../../utils/request.js'

Page({
  data: {
    loading: false,
    showMode: 'image',
    imagePath: '',
    templateId: ''
  },

  onLoad: function(options) {
    // 设置为图片预览模式
    this.setData({
      loading: true,
      showMode: 'image'
    });

    // 从参数中获取templateId
    const templateId = options.templateId || '';
    this.setData({ templateId });

    // 构建图片路径，根据templateId加载对应的模板图片
    // 图片存放在images目录下，命名为template-one.jpg, template-two.jpg等
    const imagePath = `/images/${templateId}.jpg`;
    
    this.setData({
      imagePath: imagePath,
      loading: false
    });
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
        request.get(`/user/${userId}`, {})
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
   * 使用当前模板
   * 先从后端获取最新简历数据，失败才使用本地存储数据，然后跳转到简历编辑页面
   */
  useTemplate: function() {
    const { templateId, templateName, resumeData } = this.data;
    
    wx.showLoading({
      title: '正在准备模板...',
    });
    
    // 加载用户信息并检查完整性
    this.loadUserInfo().then(userInfo => {
      // 检查用户信息是否完整
      if (!this.checkUserInfoComplete(userInfo)) {
        wx.hideLoading();
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
        // 用户信息完整，尝试从后端获取最新简历数据
        const userId = userInfo.id || wx.getStorageSync('userId');
        
        if (userId) {
          // 调用后端API获取最新简历数据
          this.fetchLatestResumeData(userId).then(latestResumeData => {
            wx.hideLoading();
            // 准备模板信息
            this.prepareTemplateInfo(templateId, templateName, latestResumeData || resumeData);
          }).catch(err => {
            console.error('获取最新简历数据失败，尝试使用本地存储:', err);
            // 后端获取失败，尝试从本地存储获取
            this.fetchLocalResumeData().then(localResumeData => {
              wx.hideLoading();
              this.prepareTemplateInfo(templateId, templateName, localResumeData || resumeData);
            }).catch(localErr => {
              wx.hideLoading();
              // 本地也没有数据，使用默认数据
              this.prepareTemplateInfo(templateId, templateName, resumeData);
            });
          });
        } else {
          wx.hideLoading();
          console.error('未找到用户ID');
          // 直接使用现有数据
          this.prepareTemplateInfo(templateId, templateName, resumeData);
        }
      }
    }).catch(err => {
      wx.hideLoading();
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
   * 从后端获取最新简历数据
   * @param {string} userId - 用户ID
   * @returns {Promise<Object>} - 最新简历数据的Promise
   */
  fetchLatestResumeData: function(userId) {
    return new Promise((resolve, reject) => {
      // 使用云托管方式请求最新简历数据
      request.get('/api/resume/user', { userId: userId })
        .then(res => {
          if (res && res.data && Array.isArray(res.data) && res.data.length > 0) {
            // 获取最新的一份简历数据（通常是数组的第一个）
            const latestResume = res.data[0];
            console.log('从后端获取最新简历数据成功:', latestResume);
            resolve(latestResume);
          } else {
            console.log('未找到简历数据');
            resolve(null);
          }
        })
        .catch(err => {
          console.error('请求最新简历数据失败:', err);
          reject(err);
        });
    });
  },
  
  /**
   * 从本地存储获取简历数据
   * @returns {Promise<Object>} - 本地简历数据的Promise
   */
  fetchLocalResumeData: function() {
    return new Promise((resolve, reject) => {
      try {
        // 尝试从本地存储获取最近保存的简历数据
        const savedResumeData = wx.getStorageSync('resumeData');
        if (savedResumeData && savedResumeData.data) {
          console.log('从本地存储获取简历数据成功');
          resolve(savedResumeData.data);
        } else {
          // 尝试获取临时保存的简历数据
          const tempResumeInfo = wx.getStorageSync('tempResumeInfo');
          if (tempResumeInfo) {
            console.log('从临时存储获取简历数据成功');
            resolve(tempResumeInfo);
          } else {
            console.log('本地存储中无简历数据');
            reject(new Error('未找到本地简历数据'));
          }
        }
      } catch (e) {
        console.error('获取本地简历数据失败:', e);
        reject(e);
      }
    });
  },
  
  /**
   * 准备模板信息
   * @param {string} templateId - 模板ID
   * @param {string} templateName - 模板名称
   * @param {Object} resumeData - 简历数据
   */
  prepareTemplateInfo: function(templateId, templateName, resumeData) {
    // 保存选择的模板信息和当前简历数据
    const templateInfo = {
      templateId: templateId,
      templateName: templateName,
      title: '我的新简历',
      isAiGenerated: false
    };
    
    // 如果有简历数据，合并到模板信息中
    if (resumeData) {
      // 进行数据格式转换，确保前端数据结构兼容性
      const compatibleData = this.convertResumeDataFormat(resumeData);
      Object.assign(templateInfo, compatibleData);
    }
    
    wx.setStorageSync('tempResumeInfo', templateInfo);
    
    wx.showToast({
      title: '模板准备完成',
      icon: 'success'
    });
    
    // 跳转到简历编辑页面
    setTimeout(() => {
      wx.navigateTo({
        url: `/pages/resume/edit/edit?templateId=${templateId}`
      });
    }, 1000);
  },
  
  /**
   * 转换简历数据格式，确保前后端数据结构兼容性
   * @param {Object} resumeData - 原始简历数据
   * @returns {Object} - 转换后的兼容格式数据
   */
  convertResumeDataFormat: function(resumeData) {
    // 创建深拷贝，避免修改原始数据
    const convertedData = JSON.parse(JSON.stringify(resumeData));
    
    // 处理不同格式的数据，确保结构一致
    // 例如处理后端返回的数据字段可能与前端期望的字段名称不同的情况
    
    // 确保personalInfo对象存在
    if (!convertedData.personalInfo) {
      convertedData.personalInfo = {};
    }
    
    // 处理技能字段，确保兼容前端格式
    if (convertedData.skills && !convertedData.skillsWithLevel) {
      // 如果只有skills数组，转换为skillsWithLevel格式
      convertedData.skillsWithLevel = convertedData.skills.map(skill => ({
        name: skill, 
        level: 0 // 默认等级
      }));
    }
    
    // 处理项目经验字段，确保使用统一的字段名
    if (convertedData.projects && !convertedData.projectExperienceList) {
      convertedData.projectExperienceList = convertedData.projects;
    }
    
    return convertedData;
  }
});