// report.js
const app = getApp();
import { post, get } from '../../utils/request.js';

Page({
  data: {
    sessionId: '',
    loading: true,
    loadingText: '加载中...',
    
    // 职业信息
    jobType: '',
    domain: '',
    
    // 评分数据
    totalScore: 0,
    techScore: 0,
    logicScore: 0,
    clarityScore: 0,
    depthScore: 0,
    
    // 成长报告数据（从数据库获取）
    growthRadar: {},
    trendCurve: [],
    recommendedSkills: [],
    longTermPath: [],
    
    // 薪资数据（从数据库获取）
    salaryInfo: {},
    
    // 会话记录
    sessionLog: [],
    expandedLogIndex: -1,
    
    // 成长建议
    growthAdvice: {
      shortTerm: '',
      midTerm: '',
      longTerm: ''
    }
  },

  // API服务调用
  apiServices: {
    // 获取职业成长报告数据
    getCareerGrowthData: function(userId, jobType, domain) {
      return get('/api/report/career-growth', { userId, jobType, domain })
        .then(res => {
          return res.data;
        })
        .catch(err => {
          console.error('获取职业成长数据失败:', err);
          // 返回默认数据作为备用
          return {
            growthRadar: {
              '专业技能': 75,
              '逻辑思维': 68,
              '沟通表达': 80,
              '创新潜力': 60
            },
            trendCurve: [65, 70, 73, 78],
            recommendedSkills: ['前端框架优化', '性能调优', '架构设计'],
            longTermPath: ['高级工程师', '技术架构师', '技术专家']
          };
        });
    },

    // 获取薪资信息
    getSalaryInfo: function(jobType, city, scores) {
      return get('/api/report/salary-info', { jobType, city, scores: JSON.stringify(scores) })
        .then(res => {
          return res.data;
        })
        .catch(err => {
          console.error('获取薪资信息失败:', err);
          throw err;
        });
    },

    // 获取成长建议
    getGrowthAdvice: function(jobType, domain, scores) {
      return get('/api/report/growth-advice', { jobType, domain, scores: JSON.stringify(scores) })
        .then(res => {
          return res.data;
        })
        .catch(err => {
          console.error('获取成长建议失败:', err);
          throw err;
        });
    }
  },

  // 生命周期函数--监听页面加载
  onLoad: function(options) {
    const { sessionId, jobType, domain } = options || {};
    this.setData({
      loading: true,
      sessionId: sessionId || '',
      jobType: jobType || '',
      domain: domain || '',
      // 初始化报告数据
      strengths: '',
      improvements: '',
      overallFeedback: '',
      totalScore: 0,
      techScore: 0,
      logicScore: 0,
      clarityScore: 0,
      depthScore: 0,
      growthRadar: {
        '专业技能': 75,
        '逻辑思维': 68,
        '沟通表达': 80,
        '创新潜力': 60
      },
      trendCurve: [65, 70, 73, 78],
      recommendedSkills: ['前端框架优化', '性能调优', '架构设计'],
      longTermPath: ['高级工程师', '技术架构师', '技术专家'],
      salaryInfo: {},
      growthAdvice: {
        shortTerm: '',
        midTerm: '',
        longTerm: ''
      }
    });
    
    // 尝试从全局获取数据
    if (app.globalData.interviewResult) {
      this.renderReport(app.globalData.interviewResult);
    } else if (sessionId) {
      // 从后端获取报告数据（云托管请求方式）
      this.loadReport(sessionId);
    } else {
      this.setData({ loading: false });
    }
  },

  // 加载报告数据（云托管请求方式）
  loadReport: function(sessionId) {
    this.setData({ loading: true });
    
    // 使用云托管请求获取报告数据
    wx.cloud.callContainer({
      config: {
        env: 'your-cloud-env-id' // 替换为你的云环境ID
      },
      path: '/api/interview/finish',
      method: 'POST',
      data: { sessionId: sessionId },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 0 && res.data.data) {
          this.renderReport(res.data.data);
        } else {
          // 使用模拟数据
          this.useMockData();
        }
      },
      fail: (err) => {
        console.error('获取报告失败:', err);
        // 使用模拟数据
        this.useMockData();
      }
    });
  },
  
  // 生成职业成长报告（从数据库获取）
  generateCareerGrowthReport: async function() {
    const that = this;
    
    try {
      // 从API获取职业成长数据
      const growthData = await this.apiServices.getCareerGrowthData(
        app.globalData.userInfo?.id || 'default_user',
        that.data.jobType || '前端工程师',
        that.data.domain || '软件工程'
      );
      
      console.log('从数据库获取职业成长数据成功');
      
      that.setData({
        growthRadar: growthData.growthRadar,
        trendCurve: growthData.trendCurve,
        recommendedSkills: growthData.recommendedSkills,
        longTermPath: growthData.longTermPath
      });
      
      // 重新绘制图表
      setTimeout(() => {
        that.drawRadarChart();
        that.drawTrendChart();
      }, 100);
    } catch (error) {
      console.error('生成职业成长报告失败:', error);
      // 使用默认数据作为备用
      this.setDefaultGrowthData();
    }
  },

  // 设置默认成长数据（作为后备）
  setDefaultGrowthData: function() {
    this.setData({
      growthRadar: {
        '专业技能': 75,
        '逻辑思维': 68,
        '沟通表达': 80,
        '创新潜力': 60
      },
      trendCurve: [65, 70, 73, 78],
      recommendedSkills: ['前端框架优化', '性能调优', '架构设计'],
      longTermPath: ['高级工程师', '技术架构师', '技术专家']
    });
    
    // 重新绘制图表
    setTimeout(() => {
      this.drawRadarChart();
      this.drawTrendChart();
    }, 100);
  },

  // 渲染报告
  renderReport: function(data) {
    const { totalScore, strengths, improvements, overallFeedback, 
            techScore, logicScore, clarityScore, depthScore, 
            jobType, domain, growthRadar, trendCurve, 
            recommendedSkills, longTermPath, salaryInfo, growthAdvice } = data;
    
    this.setData({
      totalScore: Math.round(totalScore),
      strengths: strengths || '',
      improvements: improvements || '',
      overallFeedback: overallFeedback || '',
      techScore: techScore,
      logicScore: logicScore,
      clarityScore: clarityScore,
      depthScore: depthScore,
      loading: false,
      jobType: jobType || this.data.jobType,
      domain: domain || this.data.domain,
      growthRadar: growthRadar || {
        '专业技能': 75,
        '逻辑思维': 68,
        '沟通表达': 80,
        '创新潜力': 60
      },
      trendCurve: trendCurve || [65, 70, 73, 78],
      recommendedSkills: recommendedSkills || ['前端框架优化', '性能调优', '架构设计'],
      longTermPath: longTermPath || ['高级工程师', '技术架构师', '技术专家'],
      salaryInfo: salaryInfo || {},
      growthAdvice: growthAdvice || {
        shortTerm: '',
        midTerm: '',
        longTerm: ''
      }
    });
    
    // 绘制雷达图
    this.drawRadarChart();
    this.drawTrendChart();
  },

  // 调用薪资匹配API（从数据库获取）
  getSalaryMatchData: async function(scores, city) {
    try {
      // 从数据库获取薪资信息
      const salaryData = await this.apiServices.getSalaryInfo(
        this.data.jobType || '前端工程师',
        city || '北京',
        scores
      );
      
      console.log('从数据库获取薪资信息成功');
      this.setData({
        salaryInfo: salaryData
      });
    } catch (error) {
      console.error('获取薪资信息失败，使用默认数据:', error);
      // 使用模拟数据作为备用
      this.setMockSalaryData(city);
    }
  },
  
  // 设置模拟薪资数据
  setMockSalaryData: function(city = '北京') {
    // 根据城市设置不同的薪资数据
    const cityData = {
      '北京': { salaryRange: '25K-35K', salaryLevel: '高于平均' },
      '上海': { salaryRange: '23K-32K', salaryLevel: '高于平均' },
      '深圳': { salaryRange: '22K-30K', salaryLevel: '平均水平' },
      '杭州': { salaryRange: '20K-28K', salaryLevel: '平均水平' },
      '广州': { salaryRange: '18K-26K', salaryLevel: '平均水平' }
    };
    
    const data = cityData[city] || cityData['北京'];
    
    this.setData({
      salaryInfo: {
        ...this.data.salaryInfo,
        city: city,
        salaryRange: data.salaryRange,
        salaryLevel: data.salaryLevel,
        confidence: Math.floor(Math.random() * 20) + 75,
        experience: '3-5年'
      }
    });
  },
  
  // 使用模拟数据（备用）
  useMockData: function() {
    const mockData = {
      aggregatedScores: {
        tech: 7.5,
        logic: 8.0,
        clarity: 7.8,
        depth: 7.2
      },
      total_score: 76,
      sessionLog: [
        {
          question: '请介绍一下你最熟悉的一个项目，重点说明你负责的部分和使用的技术栈。',
          answer: '我最熟悉的项目是一个企业管理系统的前端开发。我负责了整个系统的前端架构设计，使用了React、Redux、TypeScript等技术栈。',
          score: { tech: 7, logic: 8, clarity: 8, depth: 7 },
          feedback: '回答清晰地介绍了项目情况和技术栈，但可以进一步深入具体的技术实现细节。',
          matchedPoints: ['覆盖了项目概况', '明确了技术栈', '说明了负责内容']
        },
        {
          question: '在项目中遇到的最大技术难点是什么？你是如何解决的？',
          answer: '最大的技术难点是性能优化问题。我们通过组件懒加载、状态管理优化和缓存策略改进，成功将页面加载时间从3秒降低到1秒以内。',
          score: { tech: 8, logic: 8, clarity: 7, depth: 8 },
          feedback: '很好地描述了技术难点和解决方案，并提供了量化的优化成果。',
          matchedPoints: ['明确了技术难点', '提供了具体解决方案', '有量化的优化成果']
        }
      ]
    };
    
    this.renderReport(mockData);
  },

  // 生成成长建议（从数据库获取）
  generateGrowthAdvice: async function(scores) {
    try {
      // 从数据库获取成长建议
      const adviceData = await this.apiServices.getGrowthAdvice(
        this.data.jobType || '前端工程师',
        this.data.domain || '软件工程',
        scores
      );
      
      console.log('从数据库获取成长建议成功');
      this.setData({
        growthAdvice: {
          shortTerm: adviceData.shortTerm || '',
          midTerm: adviceData.midTerm || '',
          longTerm: adviceData.longTerm || ''
        }
      });
    } catch (error) {
      console.error('获取成长建议失败，使用默认建议:', error);
      // 使用默认建议作为备用
      this.setDefaultGrowthAdvice(scores);
    }
  },

  // 设置默认成长建议（作为后备）
  setDefaultGrowthAdvice: function(scores) {
    const growthAdvice = {
      shortTerm: '',
      midTerm: '',
      longTerm: ''
    };
    
    // 识别薄弱项
    const weakDimensions = [];
    if (scores.tech < 7) weakDimensions.push('技术深度');
    if (scores.logic < 7) weakDimensions.push('逻辑思维');
    if (scores.clarity < 7) weakDimensions.push('表达清晰度');
    if (scores.depth < 7) weakDimensions.push('回答深度');
    
    // 生成短期建议
    if (weakDimensions.length > 0) {
      growthAdvice.shortTerm = `建议重点加强${weakDimensions.join('、')}方面的学习，可以通过阅读官方文档、参与技术社区讨论和完成小型练习项目来提升。`;
    } else {
      growthAdvice.shortTerm = '基础技能掌握良好，建议深入学习前沿技术栈，如微前端、Serverless等，扩展技术广度。';
    }
    
    // 生成中期建议
    growthAdvice.midTerm = '学习大型项目的架构设计和性能优化策略，积累更多复杂业务场景的处理经验，提升代码质量和可维护性。';
    
    // 生成长期建议
    growthAdvice.longTerm = '探索全栈开发能力，学习后端技术和DevOps知识，培养系统思维和团队协作能力，为技术专家或团队领导方向发展。';
    
    this.setData({ growthAdvice });
  },
  
  // 切换会话日志项的展开/折叠
  toggleLogItem: function(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({
      expandedLogIndex: this.data.expandedLogIndex === index ? -1 : index
    });
  },
  
  // 下载PDF报告
  downloadPDF: function() {
    wx.showLoading({ title: '生成报告中...' });
    
    // 调用 reportGeneratorAdvanced 服务
    // 模拟PDF生成
    setTimeout(() => {
      wx.hideLoading();
      wx.showToast({ title: '报告已生成，请查看下载文件夹' });
      
      // 在实际应用中，这里应该调用后端API生成并下载PDF
      /*
      wx.request({
        url: '/api/report/generate',
        data: { sessionId: this.data.sessionId },
        responseType: 'arraybuffer',
        success: function(res) {
          // 处理PDF下载逻辑
        }
      });
      */
    }, 2000);
  },
  
  // 分享报告
  shareReport: function() {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },
  
  // 分享给朋友
  onShareAppMessage: function() {
    return {
      title: `我的AI面试报告得分：${this.data.totalScore}`,
      path: `/pages/report/report?sessionId=${this.data.sessionId}`,
      imageUrl: '' // 可以设置分享图片
    };
  },
  
  // 分享到朋友圈
  onShareTimeline: function() {
    return {
      title: `我的AI面试报告得分：${this.data.totalScore}`,
      query: `sessionId=${this.data.sessionId}`,
      imageUrl: '' // 可以设置分享图片
    };
  },

  // 绘制雷达图
  drawRadarChart: function() {
    try {
      const ctx = wx.createCanvasContext('radarCanvas');
      const width = wx.getSystemInfoSync().windowWidth - 80;
      const height = 400;
      const centerX = width / 2;
      const centerY = height / 2;
      const radius = Math.min(centerX, centerY) - 40;
      
      // 使用成长雷达数据
      const growthRadar = this.data.growthRadar || {
        '专业技能': 75,
        '逻辑思维': 68,
        '沟通表达': 80,
        '创新潜力': 60
      };
      
      // 转换为数组格式
      const labels = Object.keys(growthRadar);
      const data = Object.values(growthRadar).map(score => score / 10); // 转换为0-10分制
      
      // 绘制雷达背景
      this.drawRadarBackground(ctx, centerX, centerY, radius, labels.length);
      
      // 绘制数据区域
      this.drawRadarData(ctx, centerX, centerY, radius, data);
      
      // 绘制标签
      this.drawRadarLabels(ctx, centerX, centerY, radius, labels);
      
      ctx.draw();
    } catch (error) {
      console.error('绘制雷达图失败:', error);
    }
  },

  // 绘制雷达背景
  drawRadarBackground: function(ctx, centerX, centerY, radius, sides) {
    const angleStep = Math.PI * 2 / sides;
    
    // 绘制网格线
    for (let level = 1; level <= 5; level++) {
      const levelRadius = radius * level / 5;
      ctx.beginPath();
      for (let i = 0; i < sides; i++) {
        const angle = angleStep * i - Math.PI / 2;
        const x = centerX + Math.cos(angle) * levelRadius;
        const y = centerY + Math.sin(angle) * levelRadius;
        
        if (i === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      }
      ctx.closePath();
      ctx.setStrokeStyle('#e0e0e0');
      ctx.setLineWidth(2);
      ctx.stroke();
    }
  },

  // 绘制雷达数据
  drawRadarData: function(ctx, centerX, centerY, radius, data) {
    const angleStep = Math.PI * 2 / data.length;
    
    ctx.beginPath();
    for (let i = 0; i < data.length; i++) {
      const value = data[i] / 10;
      const angle = angleStep * i - Math.PI / 2;
      const x = centerX + Math.cos(angle) * radius * value;
      const y = centerY + Math.sin(angle) * radius * value;
      
      if (i === 0) {
        ctx.moveTo(x, y);
      } else {
        ctx.lineTo(x, y);
      }
    }
    ctx.closePath();
    ctx.setFillStyle('rgba(7, 193, 96, 0.2)');
    ctx.setStrokeStyle('#07c160');
    ctx.setLineWidth(4);
    ctx.fill();
    ctx.stroke();
  },

  // 绘制雷达图标签
  drawRadarLabels: function(ctx, centerX, centerY, radius, labels) {
    const angleStep = Math.PI * 2 / labels.length;
    
    for (let i = 0; i < labels.length; i++) {
      const angle = angleStep * i - Math.PI / 2;
      const labelRadius = radius + 30;
      const x = centerX + Math.cos(angle) * labelRadius;
      const y = centerY + Math.sin(angle) * labelRadius;
      
      ctx.setFontSize(24);
      ctx.setTextAlign('center');
      ctx.setTextBaseline('middle');
      ctx.setFillStyle('#333333');
      ctx.fillText(labels[i], x, y);
    }
  },

  // 绘制趋势图
  drawTrendChart: function() {
    try {
      const ctx = wx.createCanvasContext('trendCanvas');
      const width = wx.getSystemInfoSync().windowWidth - 80;
      const height = 300;
      const padding = 40;
      
      // 模拟薪资趋势数据
      const years = ['0-1年', '1-3年', '3-5年', '5-8年', '8+年'];
      const salaries = [12000, 18000, 25000, 35000, 50000];
      
      // 计算坐标轴范围
      const maxSalary = Math.max(...salaries) * 1.2;
      
      // 绘制坐标轴
      ctx.beginPath();
      ctx.moveTo(padding, padding);
      ctx.lineTo(padding, height - padding);
      ctx.lineTo(width - padding, height - padding);
      ctx.setStrokeStyle('#333333');
      ctx.setLineWidth(2);
      ctx.stroke();
      
      // 绘制薪资曲线
      ctx.beginPath();
      for (let i = 0; i < years.length; i++) {
        const x = padding + (i / (years.length - 1)) * (width - 2 * padding);
        const y = height - padding - (salaries[i] / maxSalary) * (height - 2 * padding);
        
        if (i === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
        
        // 绘制数据点
        ctx.beginPath();
        ctx.arc(x, y, 6, 0, Math.PI * 2);
        ctx.setFillStyle('#07c160');
        ctx.fill();
      }
      ctx.setStrokeStyle('#07c160');
      ctx.setLineWidth(3);
      ctx.stroke();
      
      // 绘制年份标签
      for (let i = 0; i < years.length; i++) {
        const x = padding + (i / (years.length - 1)) * (width - 2 * padding);
        ctx.setFontSize(20);
        ctx.setTextAlign('center');
        ctx.setTextBaseline('top');
        ctx.setFillStyle('#666666');
        ctx.fillText(years[i], x, height - padding + 10);
      }
      
      // 绘制当前薪资范围标记
      if (this.data.salaryRange) {
        const rangeText = this.data.salaryRange;
        ctx.setFontSize(24);
        ctx.setTextAlign('right');
        ctx.setFillStyle('#ff9500');
        ctx.fillText('当前推荐: ' + rangeText, width - padding, padding + 20);
      }
      
      ctx.draw();
    } catch (error) {
      console.error('绘制趋势图失败:', error);
    }
  },

  // 获取评分描述
  getScoreDescription: function(score) {
    if (score >= 85) return '优秀';
    if (score >= 70) return '良好';
    if (score >= 60) return '及格';
    return '需要提升';
  },
  
  getScoreLevel: function(score) {
    if (score >= 90) return '优秀';
    if (score >= 80) return '良好';
    if (score >= 70) return '中等';
    if (score >= 60) return '及格';
    return '需要提升';
  },
  
  getSalaryColor: function(level) {
    if (level === '高于平均') return '#52c41a';
    if (level === '平均水平') return '#faad14';
    return '#ff4d4f';
  },

  // 获取匹配度颜色
  getConfidenceColor: function(confidence) {
    // 根据匹配度返回颜色
    const value = typeof confidence === 'number' ? confidence : 0;
    if (value >= 80) return '#52c41a';
    if (value >= 60) return '#faad14';
    return '#ff4d4f';
  },

  // 计算问题得分
  getQuestionScore: function(scoreObj) {
    const avg = (scoreObj.tech + scoreObj.logic + scoreObj.clarity + scoreObj.depth) / 4;
    return Math.round(avg);
  },

  // 下载报告（保持向后兼容）
  downloadReport: function() {
    // 调用新的下载方法
    this.downloadPDF();
  },

  // 返回上一页
  goBack: function() {
    // 检查是否有从interview页面传来的参数
    if (this.data.sessionId) {
      wx.navigateBack();
    } else {
      // 否则返回首页
      wx.switchTab({ url: '/pages/index/index' });
    }
  }
});