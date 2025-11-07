// 后端配置接口模拟实现
// 在实际环境中，这个文件应该放在后端服务器上

const express = require('express');
const router = express.Router();
const mysql = require('mysql2/promise');

// 数据库连接配置
const dbConfig = {
  host: 'localhost',
  user: 'your_username',
  password: 'your_password',
  database: 'resume_database'
};

/**
 * 获取动态面试配置
 * 从数据库的dynamic_config表中读取配置信息
 */
async function getDynamicConfig() {
  let connection;
  try {
    // 创建数据库连接
    connection = await mysql.createConnection(dbConfig);
    
    // 查询所有面试相关配置信息
    const [rows] = await connection.execute(
      'SELECT config_key, config_value FROM dynamic_config WHERE config_key IN (?, ?, ?)',
      ['interview_personas', 'default_interview_template', 'depth_levels']
    );
    
    // 构建配置对象
    const config = {};
    
    rows.forEach(row => {
      try {
        const value = JSON.parse(row.config_value);
        
        if (row.config_key === 'interview_personas') {
          config.personas = value.personas || [];
        } else if (row.config_key === 'default_interview_template') {
          config.defaultSessionSeconds = value.defaultSessionSeconds || 900;
          config.defaultPersona = value.defaultPersona || 'friendly';
          config.minSessionSeconds = value.minSessionSeconds || 600;
          config.maxSessionSeconds = value.maxSessionSeconds || 1800;
        } else if (row.config_key === 'depth_levels') {
          config.depthLevels = value.depthLevels || [];
        }
      } catch (parseError) {
        console.error(`解析配置 ${row.config_key} 失败:`, parseError);
      }
    });
    
    // 从数据库中读取深度级别配置
    // 如果没有从数据库获取到，保持现有的空数组，让前端使用默认配置
    
    return config;
  } catch (error) {
    console.error('获取动态配置失败:', error);
    // 返回空配置，让前端使用自己的useDefaultConfig方法
    return {
      personas: [],
      defaultSessionSeconds: 900,
      defaultPersona: 'friendly',
      depthLevels: []
    };
  } finally {
    if (connection) {
      await connection.end();
    }
  }
}

// 接口路由
router.get('/get-config', async (req, res) => {
  try {
    const config = await getDynamicConfig();
    res.json({
      code: 0,
      success: true,
      message: '获取配置成功',
      data: config
    });
  } catch (error) {
    res.json({
      code: -1,
      success: false,
      message: '获取配置失败',
      data: null
    });
  }
});

module.exports = router;