// request.js - 针对云托管服务优化
const app = getApp()

/**
 * 封装云托管容器调用（使用callContainer方法）
 * @param {string} path - 请求路径
 * @param {object} data - 请求参数
 * @param {string} method - 请求方法
 * @param {object} header - 请求头
 * @returns {Promise} 返回Promise对象
 */
const cloudCall = (path, data = {}, method = 'GET', header = {}) => {
  return new Promise((resolve, reject) => {
    // 确保云环境ID已配置
    if (!app.globalData.cloudEnvId) {
      console.error('云托管环境ID未配置，请在app.js中设置正确的cloudEnvId')
      reject(new Error('云托管环境ID未配置'))
      return
    }
    
    wx.cloud.callContainer({
      config: {
        env: app.globalData.cloudEnvId // 云托管环境ID
      },
      // 确保路径格式正确，避免重复添加/api前缀
      path: path.startsWith('/api') ? path : `/api${path}`,
      method: method,
      header: {
        'content-type': 'application/json',
        'token': app.globalData.token || '',
        'X-WX-SERVICE': 'springboot-bq0e', // 添加服务名
        ...header
      },
      data,
      success: res => {
        resolve(res.data)
      },
      fail: err => {
        console.error('云托管调用失败', err)
        reject(err)
      }
    })
  })
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
  if (app.globalData.useCloud) {
    console.log(`云托管请求: ${method} ${url}`)
    return cloudCall(url, data, method, header)
      .then(res => {
        // 业务状态码处理
        if (res.code === 0) {
          return res.data
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
      })
      .catch(error => {
        console.error('云托管请求失败', error)
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        })
        throw error
      })
  }
  
  // 使用传统的wx.request方式
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      data,
      method,
      header: {
        'content-type': 'application/json',
        'token': app.globalData.token || '',
        ...header
      },
      success: (res) => {
        // 根据后端返回的状态码进行处理
        if (res.statusCode === 200) {
          const data = res.data
          // 业务状态码处理
          if (data.code === 0) {
            resolve(data.data)
          } else if (data.code === 401) {
            // 登录过期，需要重新登录
            app.logout()
            wx.navigateTo({ url: '/pages/login/login' })
            reject(new Error('登录已过期，请重新登录'))
          } else {
            // 其他错误，提示错误信息
            wx.showToast({
              title: data.message || '请求失败',
              icon: 'none'
            })
            reject(new Error(data.message || '请求失败'))
          }
        } else {
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(new Error('网络请求失败'))
        }
      },
      fail: (error) => {
        console.error('请求失败', error)
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        })
        reject(error)
      },
      complete: () => {
        // 可以在这里添加请求完成后的操作，如隐藏加载动画
      }
    })
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