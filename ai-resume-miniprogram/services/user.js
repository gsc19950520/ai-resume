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
   * 上传头像（base64格式）
   * @param {string} openId - 用户openId
   * @param {string} avatarBase64 - base64格式的头像数据
   * @returns {Promise} 返回上传结果
   */
  static async uploadAvatar(openId, avatarBase64) {
    try {
      const response = await request.post('/api/user/avatar', {
        openId,
        avatarBase64
      });
      
      // 处理后端返回的标准格式
      if (response && response.success === true) {
        return response.data;
      } else {
        // 处理错误情况
        throw new Error(response?.message || '头像上传失败');
      }
    } catch (error) {
      console.error('头像上传出错:', error);
      throw error;
    }
  }
}

export default UserService