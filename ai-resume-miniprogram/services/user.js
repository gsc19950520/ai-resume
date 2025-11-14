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
  static async getUserInfo(openId) {
    try {
      const response = await request.get('/api/user/info', {
        params: { openId }
      });
      // 处理后端返回的标准格式：{success: true, data: {用户信息和统计数据}}
      if (response && response.success === true && response.data) {
        return response.data;
      } else if (response && response.data) {
        // 兼容旧格式，直接返回data
        return response.data;
      } else {
        // 处理错误情况
        throw new Error(response?.message || '获取用户信息失败');
      }
    } catch (error) {
      console.error('获取用户信息出错:', error);
      throw error;
    }
  }

  /**
   * 更新用户信息
   * @param {object} userInfo - 用户信息
   * @returns {Promise} 返回更新结果
   */
  static async updateUserInfo(userInfo) {
    try {
      // 使用正确的接口路径和方法
      const response = await request.post('/api/user/update', userInfo);
      // 处理后端返回的标准格式
      if (response && response.success === true) {
        return response.data;
      } else {
        // 处理错误情况
        throw new Error(response?.message || '更新用户信息失败');
      }
    } catch (error) {
      console.error('更新用户信息出错:', error);
      throw error;
    }
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