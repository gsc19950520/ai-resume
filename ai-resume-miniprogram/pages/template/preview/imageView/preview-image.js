// 图片预览页面逻辑
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
  }
});