// user.js
import request from '../utils/request'

/**
 * 用户服务类
 */
class UserService {
  /**
   * 微信登录
   * @param {string} code - 微信登录code
   * @returns {Promise} 返回登录结果
   */
  static async login(code) {
    return request.post('/api/user/login', { code })
  }

  /**
   * 获取用户信息
   * @returns {Promise} 返回用户信息
   */
  static async getUserInfo() {
    return request.get('/api/user/info')
  }

  /**
   * 更新用户信息
   * @param {object} userInfo - 用户信息
   * @returns {Promise} 返回更新结果
   */
  static async updateUserInfo(userInfo) {
    return request.put('/api/user/info', userInfo)
  }

  /**
   * 获取用户统计数据
   * @returns {Promise} 返回统计数据
   */
  static async getStats() {
    return request.get('/api/user/stats')
  }

  /**
   * 登出
   * @returns {Promise} 返回登出结果
   */
  static async logout() {
    return request.post('/api/user/logout')
  }

  /**
   * 上传头像
   * @param {string} filePath - 文件路径
   * @returns {Promise} 返回上传结果
   */
  static async uploadAvatar(filePath) {
    return new Promise((resolve, reject) => {
      const app = getApp()
      wx.uploadFile({
        url: `${app.globalData.baseUrl}/api/user/avatar`,
        filePath,
        name: 'file',
        header: {
          'token': app.globalData.token || ''
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 0) {
              resolve(data.data)
            } else {
              wx.showToast({ title: data.message || '上传失败', icon: 'none' })
              reject(new Error(data.message || '上传失败'))
            }
          } catch (e) {
            reject(new Error('上传失败'))
          }
        },
        fail: (error) => {
          wx.showToast({ title: '上传失败，请稍后重试', icon: 'none' })
          reject(error)
        }
      })
    })
  }
}

export default UserService