// request.js - 针对云托管服务优化
const app = getApp()

/**
 * 直接使用app.js中已定义的cloudCall方法
 * 确保配置一致性，避免重复代码
 */
const cloudCall = (path, data = {}, method = 'GET', header = {}) => {
  return app.cloudCall(path, data, method, header)
    .then(res => {
      // 检查是否有新的token（后端刷新token）
      if (res && res.header) {
        const newToken = res.header['X-New-Token'];
        const tokenRefreshed = res.header['X-Token-Refreshed'];
        
        if (newToken && tokenRefreshed === 'true') {
          console.log('检测到新的token，更新本地存储');
          // 更新全局token
          app.globalData.token = newToken;
          // 更新本地存储
          wx.setStorageSync('token', newToken);
        }
      }
      return res;
    });
}

/**
 * 将对象转换为URL查询字符串
 * @param {object} params - 参数对象
 * @returns {string} 查询字符串
 */
const objToQueryString = (params) => {
  if (!params || typeof params !== 'object' || Object.keys(params).length === 0) {
    return ''
  }
  
  return '?' + Object.entries(params)
    .map(([key, value]) => {
      // 跳过值为undefined或null的参数
      if (value === undefined || value === null) {
        return ''
      }
      return `${encodeURIComponent(key)}=${encodeURIComponent(value)}`
    })
    .filter(Boolean)
    .join('&')
}

/**
 * 封装请求方法（主要使用云托管callContainer）
 * @param {string} url - 请求地址
 * @param {object} data - 请求参数
 * @param {string} method - 请求方法
 * @param {object} header - 请求头
 * @returns {Promise} 返回Promise对象
 */
const request = (url, data = {}, method = 'GET', header = {}) => {
  // 处理GET请求的查询参数
  let requestUrl = url
  let requestData = data
  
  if (method === 'GET') {
    // 对于GET请求，将参数拼接到URL上
    const queryString = objToQueryString(requestData)
    requestUrl = url + queryString
    // GET请求不发送请求体数据
    requestData = {}
  }
  
  // 强制使用云托管调用（callContainer方式）
  return cloudCall(requestUrl, requestData, method, header)
    .then(res => {
      // 业务状态码处理
      // 检查是否是标准的响应格式（包含code字段或success字段）
      if (res && typeof res === 'object') {
        // 优先使用success字段判断（适配BaseResponseVO格式）
        if (res.success === true) {
          return res
        }
        // 同时支持旧版code为0的格式
        if ('code' in res) {
          if (res.code === 0 || res.code === 200) { // 同时支持0和200作为成功状态码
            return res
          } else if (res.code === 401) {
            // 登录过期，需要重新登录
            // 显示错误提示
            wx.showModal({
              title: '登录失效',
              content: '您的登录已过期，请重新登录',
              showCancel: false,
              success: () => {
                // 清除登录状态
                app.logout()
                // 使用redirectTo替代navigateTo，防止用户返回上一页
                wx.redirectTo({ url: '/pages/login/login' })
              }
            })
            // 显示全局遮罩层，阻止用户任何操作
            wx.showLoading({
              title: '请重新登录',
              mask: true
            })
            throw new Error('登录已过期，请重新登录')
          } else {
            // 其他错误，提示错误信息
            wx.showToast({
              title: res.message || '请求失败',
              icon: 'none'
            })
            throw new Error(res.message || '请求失败')
          }
        }
      }
      return res
    })
    .catch(error => {
      console.error('云托管请求失败，错误详情:', error)
      console.error('错误类型:', typeof error)
      console.error('错误消息:', error.message || '无错误消息')
      console.error('错误堆栈:', error.stack || '无堆栈信息')
      
      wx.showToast({
        title: '网络错误，请稍后重试',
        icon: 'none'
      })
      
      // 返回一个拒绝状态的Promise，确保调用者能感知到错误
      // 但提供更友好的错误对象
      const friendlyError = new Error(error.message || '请求失败')
      friendlyError.originalError = error
      return Promise.reject(friendlyError)
    })
}

/**
 * 流式请求方法（使用云托管callContainer调用，模拟流式处理）
 * @param {string} url - 请求地址
 * @param {object} options - 请求选项
 * @returns {Promise} 返回Promise对象
 */
const requestStream = (url, options = {}) => {
  const {
    data = {},
    method = 'POST',
    header = {},
    onChunk,
    onEvent,
    onError,
    onComplete,
    onReconnect,
    maxRetries = 3,
    retryDelay = 2000
  } = options;
  
  // 记录重试次数和已接收的事件数据
  let retryCount = 0;
  let receivedEvents = [];
  let isCompleted = false;
  
  // 解析SSE事件格式
  const parseSSE = (eventText) => {
    if (!eventText || typeof eventText !== 'string') {
      return null;
    }
    
    try {
      // 简单处理：如果是JSON格式直接解析
      const parsed = JSON.parse(eventText);
      if (parsed.name && parsed.data) {
        return parsed;
      }
    } catch (e) {
      // 不是JSON格式，尝试解析SSE格式
      const lines = eventText.split('\n');
      let event = { name: '', data: '' };
      
      lines.forEach(line => {
        line = line.trim();
        if (line.startsWith('event:')) {
          event.name = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          event.data = line.substring(5).trim();
        }
      });
      
      return event.name || event.data ? event : null;
    }
    
    return null;
  };
  
  // 发送事件到回调函数
  const sendEvent = (event) => {
    if (event && onEvent && typeof onEvent === 'function') {
      onEvent(event);
      // 保存已接收的事件
      if (event.name !== 'progress') { // 不保存进度事件，避免重复
        receivedEvents.push(event);
      }
    } else if (onChunk && typeof onChunk === 'function') {
      onChunk(event);
    }
  };
  
  // 模拟流式处理响应数据
  const processResponse = (res) => {
    if (res && typeof res === 'string') {
      // 模拟流式处理：逐字符发送数据
      let index = 0;
      const sendNextChar = () => {
        if (index < res.length) {
          // 从当前位置查找是否有完整的事件行
          const endIndex = res.indexOf('\n\n', index); // 寻找SSE事件分隔符
          if (endIndex !== -1) {
            // 有完整的SSE事件，发送整个事件
            const eventText = res.substring(index, endIndex + 2);
            index = endIndex + 2;
            
            // 解析SSE事件
            const event = parseSSE(eventText);
            sendEvent(event);
            
            // 递归发送下一个事件
            setTimeout(sendNextChar, 10);
          } else {
            // 没有完整的事件，继续寻找
            const nextNewline = res.indexOf('\n', index);
            if (nextNewline !== -1) {
              // 有换行符，尝试解析部分内容
              const partialText = res.substring(index, nextNewline + 1);
              index = nextNewline + 1;
              
              if (onChunk && typeof onChunk === 'function') {
                onChunk(partialText);
              }
              
              setTimeout(sendNextChar, 10);
            } else {
              // 没有换行符，发送剩余部分
              const chunk = res.substring(index);
              index = res.length;
              
              if (onChunk && typeof onChunk === 'function') {
                onChunk(chunk);
              }
              
              setTimeout(sendNextChar, 10);
            }
          }
        } else {
          // 所有数据发送完毕
          isCompleted = true;
          if (onComplete && typeof onComplete === 'function') {
            onComplete();
          }
        }
      };
      // 开始发送数据
      sendNextChar();
    } else if (res && typeof res === 'object') {
      // 如果是对象格式，直接处理
      if (res.name && res.data) {
        // 已经是event格式
        sendEvent(res);
      } else {
        // 尝试作为report-content事件处理
        const event = { 
          name: 'report-content', 
          data: res 
        };
        sendEvent(event);
      }
      
      isCompleted = true;
      if (onComplete && typeof onComplete === 'function') {
        onComplete();
      }
    }
    
    return res;
  };
  
  // 执行请求的核心函数
  const executeRequest = () => {
    return new Promise((resolve, reject) => {
      // 使用云托管callContainer调用，设置60秒超时以适应长prompt
      app.cloudCall(url, data, method, header, 60000) // 使用60秒超时
        .then(res => {
          const processedRes = processResponse(res);
          resolve(processedRes);
        })
        .catch(err => {
          console.error('请求失败:', err);
          
          // 处理重试逻辑
          if (retryCount < maxRetries && !isCompleted) {
            retryCount++;
            console.log(`正在进行第${retryCount}次重试...`);
            
            // 调用重连回调
            if (onReconnect && typeof onReconnect === 'function') {
              onReconnect(retryCount, maxRetries);
            }
            
            // 延迟重试
            setTimeout(() => {
              executeRequest()
                .then(res => resolve(res))
                .catch(error => reject(error));
            }, retryDelay * retryCount); // 指数退避
          } else {
            // 重试失败，调用错误回调
            if (onError && typeof onError === 'function') {
              onError(err);
            }
            reject(err);
          }
        });
    });
  };
  
  return executeRequest();
}

// 导出GET请求方法
export const get = (url, data = {}, header = {}) => {
  return request(url, data, 'GET', header)
}

// 导出POST请求方法
export const post = (url, data = {}, header = {}) => {
  return request(url, data, 'POST', header)
}

// 导出流式请求方法
export const postStream = (url, data = {}, options = {}) => {
  return requestStream(url, {
    ...options,
    data,
    method: 'POST'
  })
}

// 导出GET流式请求方法
export const getStream = (url, data = {}, options = {}) => {
  return requestStream(url, {
    ...options,
    data,
    method: 'GET'
  })
}

// 导出PUT请求方法
export const put = (url, data = {}, header = {}) => {
  return request(url, data, 'PUT', header)
}

// 导出DELETE请求方法
export const del = (url, data = {}, header = {}) => {
  return request(url, data, 'DELETE', header)
}

// 导出默认对象
export default {
  get,
  post,
  postStream,
  getStream,
  put,
  del,
  delete: del
}