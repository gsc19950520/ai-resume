//app.js
App({
  globalData: {
    userInfo: null,
    userProfile: null, // 用户头像和昵称等信息
    token: '',
    baseUrl: 'https://7465-test-env-55252f-1258669146.tcb.qcloud.la/api', // 云托管环境
    cloudBaseUrl: '', // 云托管服务地址，使用callContainer时无需配置
    useCloud: true, // 使用云托管服务（通过callContainer调用）
    cloudEnvId: 'prod-1gwm267i6a10e7cb' // 微信云托管环境ID
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
    
    wx.cloud.callContainer({
      config: {
        env: this.globalData.cloudEnvId
      },
      path: '/api/user/wechat-login',
      method: 'POST',
      header: {
        'X-WX-SERVICE': 'springboot-bq0e',
        'content-type': 'application/json'
      },
      data: requestData,
      success: result => {
        console.info('登录接口调用成功', result)
        this.handleLoginResult(result, callback)
      },
      fail: error => {
        console.error('云托管登录请求失败', error)
        console.log('环境ID:', this.globalData.cloudEnvId)
        console.log('服务名:', 'springboot-bq0e')
        console.log('请求路径:', '/api/user/wechat-login')
        wx.showToast({
          title: '登录失败，请重试',
          icon: 'none'
        })
        if (callback) callback(error)
      }
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

  // 退出登录
  logout: function() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    this.globalData.token = ''
    this.globalData.userInfo = null
  },
  // 统一请求方法（已迁移到utils/request.js中使用callContainer）
  request: function(url, method, data, callback) {
    // 注意：此方法已保留以兼容旧代码，新代码应使用utils/request.js中的方法
    // 确保callback存在，避免调用不存在的函数
    const safeCallback = callback || function() {};
    
    // 兼容对象参数形式
    let requestParams = {};
    if (typeof url === 'object') {
      requestParams = url;
      url = requestParams.url;
      method = requestParams.method;
      data = requestParams.data;
      callback = requestParams.success || safeCallback;
      // 同时支持对象参数中的fail和complete回调
      const failCallback = requestParams.fail || function() {};
      const completeCallback = requestParams.complete || function() {};
      
      // 使用Promise处理请求，确保能正确解析
      return new Promise((resolve, reject) => {
        try {
          // 确保URL是字符串且以/api开头，如果不是则添加
          const requestUrl = (typeof url === 'string' && url.startsWith('/api')) ? url : `/api${url}`;
          
          wx.cloud.callContainer({
            config: {
              env: this.globalData.cloudEnvId
            },
            path: requestUrl,
            method: method || 'GET',
            header: {
              'X-WX-SERVICE': 'springboot-bq0e', // 添加服务名
              'content-type': 'application/json',
              'token': this.globalData.token || ''
            },
            data: data || {},
            success: res => {
              // 确保返回的数据格式正确，即使API返回异常也不会导致页面错误
              const responseData = res.data && typeof res.data === 'object' ? res.data : { code: -1, message: '返回数据格式异常' };
              console.log('API响应数据:', responseData);
              // 调用用户传入的success回调
              callback(responseData);
              // 解析Promise，确保Promise链能继续执行
              resolve(responseData);
            },
            fail: err => {
              console.error('云托管调用失败', err);
              console.log('环境ID:', this.globalData.cloudEnvId);
              console.log('服务名:', 'springboot-bq0e');
              console.log('请求路径:', requestUrl);
              // 返回标准错误格式
              const errorData = { code: -1, message: '网络请求失败', error: err };
              // 调用用户传入的fail回调
              failCallback(errorData);
              // 拒绝Promise
              reject(errorData);
            },
            complete: res => {
              // 调用用户传入的complete回调
              completeCallback(res);
            }
          });
        } catch (e) {
          console.error('请求异常:', e);
          reject({ code: -1, message: '请求处理异常', error: e });
        }
      });
    }
    
    // 原始参数形式的处理（强制使用云托管调用）
    // 确保URL是字符串且以/api开头，如果不是则添加
    const requestUrl = (typeof url === 'string' && url.startsWith('/api')) ? url : `/api${url}`;
    
    return wx.cloud.callContainer({
      config: {
        env: this.globalData.cloudEnvId
      },
      path: requestUrl,
      method: method || 'GET',
      header: {
        'X-WX-SERVICE': 'springboot-bq0e', // 添加服务名
        'content-type': 'application/json',
        'token': this.globalData.token || ''
      },
      data: data || {},
      success: res => {
        // 确保返回的数据格式正确，即使API返回异常也不会导致页面错误
        const responseData = res.data && typeof res.data === 'object' ? res.data : { code: -1, message: '返回数据格式异常' };
        safeCallback(responseData)
      },
      fail: err => {
        console.error('云托管调用失败', err);
        console.log('环境ID:', this.globalData.cloudEnvId);
        console.log('服务名:', 'springboot-bq0e');
        console.log('请求路径:', requestUrl);
        // 返回标准错误格式
        safeCallback({ code: -1, message: '网络请求失败', error: err });
      }
    });
  },
})