// 前端配置服务
// 此文件用于小程序端获取面试相关配置

/**
 * 获取动态面试配置
 * 调用后端API获取配置信息
 */
async function getDynamicConfig() {
  try {
    // 调用后端API获取配置
    const response = await wx.request({
      url: '/api/interview/get-config',
      method: 'GET',
      dataType: 'json'
    });
    
    if (response.statusCode === 200 && response.data.code === 0) {
      // 返回后端API提供的配置
      return response.data.data;
    } else {
      console.error('获取配置失败:', response.data.message || '未知错误');
      // 返回默认配置作为降级方案
      return getDefaultConfig();
    }
  } catch (error) {
    console.error('获取动态配置失败:', error);
    // 返回默认配置作为降级方案
    return getDefaultConfig();
  }
}

/**
 * 获取默认配置
 * 当无法从后端获取配置时使用
 */
function getDefaultConfig() {
  return {
    personas: [
      {
        id: 'friendly',
        name: '友好面试官',
        description: '亲切友好的交流方式',
        emoji: '😊',
        enabled: true
      },
      {
        id: 'technical',
        name: '技术面试官',
        description: '注重技术深度的提问方式',
        emoji: '👨‍💻',
        enabled: true
      },
      {
        id: 'manager',
        name: '管理者面试官',
        description: '关注全局和领导力',
        emoji: '👔',
        enabled: true
      }
    ],
    depthLevels: [
      {
        id: 'usage',
        name: '基础应用',
        text: '基础',
        description: '了解技术的基本概念和使用方法'
      },
      {
        id: 'principle',
        name: '原理理解',
        text: '进阶',
        description: '理解技术的工作原理和设计思想'
      },
      {
        id: 'optimization',
        name: '优化实践',
        text: '深入',
        description: '能够优化和解决复杂问题'
      }
    ],
    defaultSessionSeconds: 600,
    defaultPersona: 'friendly'
  };
}

module.exports = {
  getDynamicConfig
};