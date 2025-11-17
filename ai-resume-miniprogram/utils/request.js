// request.js - 针对云托管服务优化
const app = getApp()

/**
 * 直接使用app.js中已定义的cloudCall方法
 * 确保配置一致性，避免重复代码
 */
const cloudCall = (path, data = {}, method = 'GET', header = {}) => {
  return app.cloudCall(path, data, method, header)
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
  // 强制使用云托管调用（callContainer方式）
  return cloudCall(url, data, method, header)
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
            app.logout()
            wx.navigateTo({ url: '/pages/login/login' })
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

// 导出GET请求方法
export const get = (url, data = {}, header = {}) => {
  return request(url, data, 'GET', header)
}

// 导出POST请求方法
export const post = (url, data = {}, header = {}) => {
  return request(url, data, 'POST', header)
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
  put,
  del,
  delete: del
}