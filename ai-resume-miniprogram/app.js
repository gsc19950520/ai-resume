//app.js
const pageRefs = require('./utils/page-references.js');

App({
  globalData: {
    userInfo: null,
    userProfile: null, // 用户头像和昵称等信息
    token: '',
    baseUrl: '', // 使用云托管时无需配置具体URL
    cloudBaseUrl: '', // 云托管服务地址，使用callContainer时无需配置
    useCloud: true, // 使用云托管服务（通过callContainer调用）
    cloudEnvId: 'prod-1gwm267i6a10e7cb', // 微信云托管环境ID
    cloudServiceName: 'springboot-bq0e', // 云托管服务名称
    latestResumeData: null // 存储最新简历数据，用于面试页面
  },
  
  // 初始化云开发环境
  initCloud: function() {
    if (this.globalData.useCloud) {
      wx.cloud.init({
        env: this.globalData.cloudEnvId,
        traceUser: true
      });
    }
  },
  
  // 云托管调用方法
  cloudCall: function(path, data = {}, method = 'GET', header = {}, timeout = 15000) {
    return new Promise((resolve, reject) => {
      wx.cloud.callContainer({
        config: {
          env: this.globalData.cloudEnvId
        },
        path: path.startsWith('/api') ? path : `/api${path}`,
        method: method,
        header: {
          'content-type': 'application/json',
          'Authorization': this.globalData.token ? `Bearer ${this.globalData.token}` : '',
          'X-WX-SERVICE': this.globalData.cloudServiceName,
          ...header
        },
        data,
        timeout: timeout, // 允许自定义超时时间
        success: res => {
          // 检查是否有新的token（后端刷新token）
          if (res && res.header) {
            const newToken = res.header['X-New-Token'];
            const tokenRefreshed = res.header['X-Token-Refreshed'];
            
            if (newToken && tokenRefreshed === 'true') {
              console.log('检测到新的token，更新本地存储');
              // 更新全局token
              this.globalData.token = newToken;
              // 更新本地存储
              wx.setStorageSync('token', newToken);
            }
          }
          resolve(res.data);
        },
        fail: err => reject(err)
      });
    });
  },

  // 云托管二进制数据调用方法（用于PDF下载等）
  cloudCallBinary: function(path, data = {}, method = 'GET', header = {}) {
    return new Promise((resolve, reject) => {
      wx.cloud.callContainer({
        config: {
          env: this.globalData.cloudEnvId
        },
        path: path.startsWith('/api') ? path : `/api${path}`,
        method: method,
        header: {
          'content-type': 'application/json',
          'Authorization': this.globalData.token ? `Bearer ${this.globalData.token}` : '',
          'X-WX-SERVICE': this.globalData.cloudServiceName,
          ...header
        },
        data,
        timeout: 60000, // 二进制数据下载可以设置更长的超时时间
        responseType: 'arraybuffer', // 设置响应类型为arraybuffer以处理二进制数据
        success: res => {
          console.log('云托管二进制调用成功，响应类型:', typeof res.data, '数据长度:', res.data ? res.data.byteLength : 0);
          resolve(res.data);
        },
        fail: err => reject(err)
      });
    });
  },

  onLaunch: function () {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 初始化云开发环境
    this.initCloud()
    
    // 检查登录状态
    this.checkLoginStatus()

    // 获取系统信息
    this.getSystemInfo()
  },

  checkLoginStatus: function() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = JSON.parse(userInfo)
    }
  },

  getSystemInfo: function() {
    try {
      // 使用新的API替代废弃的getSystemInfoSync
      const windowInfo = wx.getWindowInfo()
      const appBaseInfo = wx.getAppBaseInfo()
      const deviceInfo = wx.getDeviceInfo()
      
      // 合并信息到systemInfo对象，保持向后兼容
      this.globalData.systemInfo = {
        ...windowInfo,
        ...appBaseInfo,
        ...deviceInfo
      }
    } catch (error) {
      console.error('获取系统信息失败:', error)
      // 降级方案：如果新API失败，尝试使用旧API
      try {
        const systemInfo = wx.getSystemInfoSync()
        this.globalData.systemInfo = systemInfo
      } catch (fallbackError) {
        console.error('降级获取系统信息也失败:', fallbackError)
      }
    }
  },

  // 登录方法
  login: function(userInfo, callback) {
    // 处理参数重载：如果第一个参数是函数，则表示没有传入userInfo
    if (typeof userInfo === 'function') {
      callback = userInfo
      userInfo = null
    }
    
    wx.login({
      success: res => {
        if (res.code) {
          // 确保云环境ID已配置
          if (!this.globalData.cloudEnvId) {
            console.error('云托管环境ID未配置，请在app.js中设置正确的cloudEnvId')
            wx.showToast({
              title: '云环境配置错误',
              icon: 'none'
            })
            if (callback) callback(new Error('云环境配置错误'))
            return
          }
          
          // 如果已经传入了用户信息，直接调用登录接口
          if (userInfo && Object.keys(userInfo).length > 0) {
            console.info('使用已获取的用户信息进行登录')
            this.wechatLogin(res.code, userInfo, callback)
          } else {
            // 否则获取用户信息授权
            console.info('需要获取用户信息授权')
            this.getUserProfileInfo(res.code, callback)
          }
        } else {
          // 登录失败情况
          console.error('登录失败，无法获取code', res)
          wx.showToast({
            title: '登录失败，请重试',
            icon: 'none'
          })
          if (callback) callback(new Error('无法获取登录code'))
        }
      },
      fail: error => {
        console.error('wx.login调用失败', error)
        wx.showToast({
          title: '登录接口调用失败',
          icon: 'none'
        })
        if (callback) callback(error)
      }
    })
  },
  
  // 获取用户信息并进行登录
  getUserProfileInfo: function(code, callback) {
    // 使用新版接口获取用户信息
    wx.getUserProfile({
      desc: '用于完善会员资料',
      success: (userProfileRes) => {
        console.info('获取用户信息成功', userProfileRes)
        const userInfo = userProfileRes.userInfo || {};
        
        // 调用登录接口，传递code和用户信息
        this.wechatLogin(code, userInfo, callback);
      },
      fail: (error) => {
        console.warn('用户拒绝授权获取用户信息', error);
        // 即使用户拒绝授权，也要继续登录流程，只是不传递用户信息
        this.wechatLogin(code, {}, callback);
      }
    })
  },
  
  // 微信登录接口调用
  wechatLogin: function(code, userInfo, callback) {
    // 处理参数重载：如果第二个参数是函数，则表示没有传入userInfo
    if (typeof userInfo === 'function') {
      callback = userInfo
      userInfo = null
    }
    
    // 构建请求数据
    const requestData = {
      code: code
    }
    
    // 只有在提供了userInfo且不为空时才添加到请求数据中
    if (userInfo && Object.keys(userInfo).length > 0) {
      requestData.userInfo = userInfo
    }
    
    // 使用统一的cloudCall方法
    this.cloudCall('/user/wechat-login', requestData, 'POST')
      .then(result => {
        console.info('登录接口调用成功', result)
        this.handleLoginResult({data: result}, callback)
      })
      .catch(error => {
        console.error('登录接口调用失败', error)
        wx.showToast({
          title: '登录失败，请重试',
          icon: 'none'
        })
        if (callback) callback(error)
      })
  },
  
  // 处理登录结果
  handleLoginResult: function(result, callback) {
    console.info('开始处理登录结果', { hasResult: !!result, resultType: typeof result })
    
    // 检查响应是否存在
    if (!result) {
      console.error('登录结果为空')
      wx.showToast({
        title: '登录信息解析失败',
        icon: 'none'
      })
      if (callback) callback(new Error('登录结果为空'))
      return
    }
    
    // 记录result的详细结构，便于调试
    console.info('登录结果详细信息:', {
      statusCode: result.statusCode,
      hasData: !!result.data,
      dataType: typeof result.data,
      dataKeys: result.data && typeof result.data === 'object' ? Object.keys(result.data) : null
    })
    
    // 云托管调用的数据结构可能是直接的响应体，或者在result.data中
    let responseData;
    // 尝试安全地访问响应数据
    if (result.data && typeof result.data === 'object') {
      // 如果result.data存在且是对象，使用它作为响应数据
      responseData = result.data;
      console.info('使用result.data作为响应数据')
    } else if (typeof result === 'object') {
      // 否则直接使用result
      responseData = result;
      console.info('直接使用result作为响应数据')
    } else {
      console.error('登录结果格式无效', { type: typeof result, value: result })
      wx.showToast({
        title: '登录信息格式错误',
        icon: 'none'
      })
      if (callback) callback(new Error('登录结果格式无效'))
      return
    }
    
    // 详细记录响应数据结构，便于调试
    console.info('处理后的响应数据结构:', {
      hasCode: 'code' in responseData,
      codeValue: responseData.code,
      hasSuccess: 'success' in responseData,
      successValue: responseData.success,
      hasData: 'data' in responseData,
      dataKeys: responseData.data && typeof responseData.data === 'object' ? Object.keys(responseData.data) : null
    })
    
    // 检查是否登录成功（code为0或success为true）
    const isSuccess = responseData.code === 0 || responseData.success === true;
    console.info('登录状态判断:', { isSuccess, code: responseData.code, success: responseData.success });
    
    if (isSuccess) {
      // 安全地获取token（兼容写法）
      let token = (responseData.data && responseData.data.token) || responseData.token;
      
      // 检查token是否有效
      if (token) {
        // 避免保存无效的mock_token
        if (token === 'mock_token') {
          console.warn('发现mock_token，不保存到storage');
          token = '';
        } else {
          this.globalData.token = token;
          wx.setStorageSync('token', token);
          console.info('Token保存成功');
        }
      } else {
        console.warn('登录成功但未获取到token');
      }
      
      // 尝试获取用户信息，并确保包含有效ID字段
      let userInfo = {};
      try {
        // 先尝试获取现有的userInfo对象
        userInfo = (responseData.data && responseData.data.userInfo) || responseData.userInfo || {};
        if (typeof userInfo !== 'object') {
          userInfo = {};
          console.warn('用户信息格式无效，已重置为空对象');
        }
        
        // 优先使用后端返回的userId（系统内部主键）
        const userId = (responseData.data && responseData.data.userId) || 
                      (responseData.data && responseData.data.id) || 
                      responseData.userId || 
                      responseData.id;
        console.info('后端返回的系统内部userId:', userId);
        
        // 确保用户对象包含id字段（系统内部主键）
        if (userId) {
          // 无论userInfo.id是否存在，优先使用后端返回的userId
          userInfo.id = userId;
          userInfo.userId = userId; // 明确设置userId字段
          console.info('使用后端返回的系统内部userId作为用户ID:', userId);
        } else if (!userInfo.id) {
          // 如果没有后端返回的userId，则使用openId或生成临时ID
          const openId = (responseData.data && responseData.data.openId) || responseData.openId;
          if (openId) {
            userInfo.id = openId;
            console.info('使用openId作为用户ID');
          } else if (token) {
            userInfo.id = token.substring(0, 10);
            console.info('使用token部分内容作为用户ID');
          } else {
            userInfo.id = 'user_' + Date.now();
            console.info('生成基于时间戳的临时用户ID');
          }
        }
        
        // 确保用户对象包含所有必要字段
        userInfo.userId = userInfo.id; // 明确设置userId字段，方便在业务中使用
        userInfo.openId = userInfo.openId || (responseData.data && responseData.data.openId) || responseData.openId || '';
        userInfo.nickName = userInfo.nickName || (responseData.data && responseData.data.nickName) || '';
        userInfo.avatarUrl = userInfo.avatarUrl || (responseData.data && responseData.data.avatarUrl) || '';
        userInfo.gender = userInfo.gender || (responseData.data && responseData.data.gender) || 0;
        userInfo.city = userInfo.city || (responseData.data && responseData.data.city) || '';
        userInfo.province = userInfo.province || (responseData.data && responseData.data.province) || '';
        userInfo.country = userInfo.country || (responseData.data && responseData.data.country) || '';
        userInfo.loginTime = new Date().toISOString();
      } catch (e) {
        console.error('解析用户信息时出错:', e);
        // 即使出错也要确保userInfo至少有一个id字段
        userInfo = { id: 'error_' + Date.now() };
      }
      
      this.globalData.userInfo = userInfo;
      try {
        wx.setStorageSync('userInfo', JSON.stringify(userInfo));
        console.info('用户信息保存成功');
      } catch (e) {
        console.error('保存用户信息到本地存储失败:', e);
      }
      
      // 登录成功后立即获取用户详细信息
      this.fetchUserInfoAfterLogin(userInfo);
      
      // 回调成功，返回标准格式的响应对象
      const successResponse = { code: 0, message: '登录成功' };
      console.info('登录成功处理完成，返回响应:', successResponse);
      if (callback) callback(successResponse);
    } else {
      const errorMsg = responseData.message || responseData.error || '登录失败';
      console.error('登录失败:', { message: errorMsg, responseData });
      wx.showToast({
        title: errorMsg,
        icon: 'none',
        duration: 3000 // 延长显示时间，确保用户能看到
      });
      if (callback) callback(new Error(errorMsg));
    }
  },
  
  // 登录成功后获取用户详细信息
  fetchUserInfoAfterLogin: function(userInfo) {
    console.info('开始获取用户详细信息，userInfo:', userInfo);
    
    if (!userInfo.openId) {
      console.warn('用户openId不存在，无法获取详细信息');
      return;
    }
    
    // 调用用户信息接口获取详细信息
    this.cloudCall('/user/info', { openId: userInfo.openId }, 'GET')
      .then(res => {
        console.info('获取用户详细信息成功，响应:', res);
        
        if (res && res.success && res.data) {
          const userData = res.data;
          
          // 合并详细信息到用户信息
          const updatedUserInfo = { ...userInfo, ...userData };
          
          // 更新全局用户信息
          this.globalData.userInfo = updatedUserInfo;
          
          // 更新本地存储
          try {
            wx.setStorageSync('userInfo', JSON.stringify(updatedUserInfo));
            wx.setStorageSync('userInfoLocal', JSON.stringify(updatedUserInfo));
            console.info('用户详细信息保存成功');
          } catch (e) {
            console.error('保存用户详细信息到本地存储失败:', e);
          }
          
          // 触发用户信息更新事件，供其他页面监听
          wx.notifyBLECharacteristicValueChange({
            deviceId: 'userInfoUpdated',
            serviceId: 'userService',
            characteristicId: 'userInfoCharacteristic',
            state: true,
            success: function() {
              console.info('用户信息更新事件已触发');
            },
            fail: function(error) {
              console.warn('触发用户信息更新事件失败:', error);
            }
          });
          
        } else {
          console.error('获取用户详细信息失败:', res);
        }
      })
      .catch(err => {
        console.error('获取用户详细信息接口调用失败:', err);
      });
  },

  // 退出登录
  logout: function() {
    console.log('退出登录');
    // 清除用户信息和token
    this.globalData.userInfo = null;
    this.globalData.token = '';
    // 清除本地存储的用户信息
    wx.removeStorageSync('userInfo');
    wx.removeStorageSync('token');
    console.log('已清除用户登录状态');
  },
})