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
   * 上传头像到云存储并更新用户头像地址
   * @param {string} openId - 用户openId
   * @param {string} avatarBase64 - base64格式的头像数据
   * @returns {Promise} 返回上传结果
   */
  static async uploadAvatar(openId, avatarBase64) {
    try {
      // 检查base64数据格式
      if (!avatarBase64 || !avatarBase64.startsWith('data:image/') || !avatarBase64.includes(';base64,')) {
        throw new Error('头像数据格式错误');
      }
      
      // 检查数据长度，避免过大
      if (avatarBase64.length > 3 * 1024 * 1024) {
        throw new Error('头像文件过大，请压缩后上传');
      }
      
      console.log('开始上传头像到云存储，openId:', openId);
      
      // 调用微信小程序的云开发上传API，上传到对象存储
      // 将base64转换为临时文件
      const base64Data = avatarBase64.replace(/^data:image\/\w+;base64,/, '');
      const tempFilePath = `${wx.env.USER_DATA_PATH}/temp_avatar_${Date.now()}.jpg`;
      
      // 写入临时文件
      await wx.getFileSystemManager().writeFile({
        filePath: tempFilePath,
        data: base64Data,
        encoding: 'base64'
      });
      
      // 调用云托管存储上传接口
      // 这里使用云托管的请求方式，将文件上传到指定的存储桶
      const uploadResult = await wx.cloud.uploadFile({
        cloudPath: `${openId}/avatar_${Date.now()}.jpg`, // 以openId为文件夹名
        filePath: tempFilePath,
        config: {
          env: 'prod-1gwm267i6a10e7cb', // 云环境ID
          region: 'ap-shanghai' // 地域
        }
      });
      
      console.log('头像上传到云存储成功，fileID:', uploadResult.fileID);
      
      // 调用后端接口，传递图片地址
      const response = await request.post('/api/user/avatar', {
        openId,
        avatarUrl: uploadResult.fileID // 传递云存储的文件地址
      });
      
      // 处理后端返回的标准格式
      if (response && response.success === true) {
        return response.data;
      } else {
        // 处理错误情况
        throw new Error(response?.message || '头像更新失败');
      }
    } catch (error) {
      console.error('头像上传出错:', error);
      throw error;
    }
  }
}

export default UserService