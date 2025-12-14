// pages/report/report.js
const app = getApp();
import { post, get } from '../../utils/request';

Page({
  /**
   * 页面的初始数据
   */
  data: {
    sessionId: '',
    reportContent: '',
    isLoading: true,
    isComplete: false,
    nextContent: '', // 用于流式展示，逐字/逐词添加
    isSaved: false, // 报告是否已保存
    reportData: null // 解析后的报告数据
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 获取路由参数中的sessionId和lastAnswer
    const sessionId = options.sessionId || '';
    let lastAnswer = options.lastAnswer || '';
    
    // 对lastAnswer进行URL解码处理
    if (lastAnswer) {
      try {
        lastAnswer = decodeURIComponent(lastAnswer);
      } catch (e) {
        console.warn('解码lastAnswer失败:', e);
        // 解码失败时使用原始值
      }
    }
    
    if (!sessionId) {
      wx.showToast({
        title: '缺少sessionId参数',
        icon: 'none'
      });
      return;
    }

    this.setData({
      sessionId: sessionId,
      lastAnswer: lastAnswer
    });

    // 初始化轮询相关变量
    this.lastIndex = -1;
    this.reportId = null;
    this.pollingInterval = null;

    // 首先检查是否已有保存的报告
    this.checkSavedReport(sessionId);
  },

  /**
   * 检查是否已有保存的报告
   * @param {string} sessionId - 会话ID
   */
  checkSavedReport(sessionId) {
    // 初始化状态
    this.setData({
      isLoading: true,
      nextContent: '',
      reportContent: ''
    });

    try {
      // 调用获取报告详情接口
      get(`/api/interview/report/${sessionId}`)
        .then(res => {
          if (res && res.success && res.data) {
            console.log('已找到保存的报告，直接显示:', res.data);
            
            // 解析保存的报告数据并显示
            this.displaySavedReport(res.data);
          } else {
            // 没有保存的报告，开始生成
            console.log('未找到保存的报告，开始生成');
            this.startReportGeneration(sessionId);
          }
        })
        .catch(error => {
          console.error('检查保存报告失败:', error);
          // 检查失败，开始生成报告
          this.startReportGeneration(sessionId);
        });
    } catch (error) {
      console.error('发起检查报告请求失败:', error);
      // 请求发起失败，开始生成报告
      this.startReportGeneration(sessionId);
    }
  },
  
  /**
   * 显示已保存的报告
   * @param {object} reportData - 报告数据
   */
  displaySavedReport(reportData) {
    try {
      // 确保 strengths 和 improvements 是数组格式
      if (reportData.strengths && typeof reportData.strengths === 'string') {
        reportData.strengths = JSON.parse(reportData.strengths);
      }
      if (reportData.improvements && typeof reportData.improvements === 'string') {
        reportData.improvements = JSON.parse(reportData.improvements);
      }
      
      // 为已保存的报告数据应用Markdown加粗转换
      reportData.overallFeedback = this.convertMarkdownBold(reportData.overallFeedback);
      reportData.strengths = reportData.strengths.map(strength => this.convertMarkdownBold(strength));
      reportData.improvements = reportData.improvements.map(improvement => this.convertMarkdownBold(improvement));
      reportData.techDepthEvaluation = this.convertMarkdownBold(reportData.techDepthEvaluation);
      reportData.logicExpressionEvaluation = this.convertMarkdownBold(reportData.logicExpressionEvaluation);
      reportData.communicationEvaluation = this.convertMarkdownBold(reportData.communicationEvaluation);
      reportData.answerDepthEvaluation = this.convertMarkdownBold(reportData.answerDepthEvaluation);
      reportData.detailedImprovementSuggestions = this.convertMarkdownBold(reportData.detailedImprovementSuggestions);
      
      // 更新报告数据，直接传递给新的UI结构
      this.setData({
        reportData: reportData,
        isLoading: false,
        isComplete: true,
        isSaved: true
      });
    } catch (error) {
      console.error('显示保存的报告失败:', error);
      // 显示失败，开始生成新报告
      this.startReportGeneration(this.data.sessionId);
    }
  },
  
  /**
   * 开始生成报告
   * @param {string} sessionId - 会话ID
   */
  startReportGeneration(sessionId) {
    // 初始化状态
    this.setData({
      isLoading: true,
      nextContent: '',
      reportContent: ''
    });

    try {
      // 调用开始生成报告接口，使用POST方法传递sessionId和lastAnswer
      post('/api/interview/start-report', {
        sessionId: sessionId,
        lastAnswer: this.data.lastAnswer
      })
        .then(res => {
          if (res && res.success && res.data) {
            this.reportId = res.data;
            console.log('报告生成已开始，reportId:', this.reportId);
            // 延迟3秒后开始轮询，确保报告分片有完整的数据
            setTimeout(() => {
              this.startPolling();
            }, 3000);
          } else {
            throw new Error('获取reportId失败');
          }
        })
        .catch(error => {
          console.error('开始生成报告失败:', error);
          this.setData({ isLoading: false, isComplete: true });
          wx.showToast({ title: '生成报告失败', icon: 'none' });
        });
    } catch (error) {
      console.error('发起生成报告请求失败:', error);
      this.setData({ isLoading: false, isComplete: true });
      wx.showToast({ title: '生成报告失败', icon: 'none' });
    }
  },

  /**
   * 开始轮询获取报告内容
   */
  startPolling() {
    // 每秒轮询一次
    this.pollingInterval = setInterval(() => {
      this.fetchReportChunks();
    }, 1000);
  },

  /**
   * 获取报告内容块
   */
  fetchReportChunks() {
    if (!this.reportId) return;

    try {
      get('/api/interview/get-report-chunks', {
        reportId: this.reportId,
        lastIndex: this.lastIndex
      })
        .then(res => {
          if (res && res.success && res.data) {
            const data = res.data;
            const chunks = data.chunks || [];
            
            console.log('获取到报告块:', chunks);
            
            // 如果有新的内容块，添加到报告中
            if (chunks.length > 0) {
              // 将新的内容块拼接起来
              const newContent = chunks.map(chunk => chunk.content).join('');
              
              // 更新报告内容
              const updatedReportContent = this.data.reportContent + newContent;
              this.setData({
                nextContent: this.data.nextContent + newContent,
                reportContent: updatedReportContent
              });
              
              // 更新lastIndex
              this.lastIndex = data.lastIndex || 0;
              
              // 实时解析报告内容并更新UI
              const parsedReportData = this.parseReportContent(updatedReportContent);
              this.setData({ reportData: parsedReportData });
            }
            
            // 检查报告是否完成
            if (data.completed) {
              this.setData({
                isLoading: false,
                isComplete: true,
                nextContent: '',
                isSaved: true // 报告已生成完成，后端会自动保存
              });
              
              // 停止轮询
              this.stopPolling();
            }
            
            // 检查报告生成是否失败
            if (data.status === 'FAILED') {
              this.setData({
                isLoading: false,
                isComplete: true
              });
              wx.showToast({ 
                title: data.errorMessage || '生成报告失败', 
                icon: 'none' 
              });
              
              // 停止轮询
              this.stopPolling();
            }
          } else {
            // 接口返回数据格式不正确，停止轮询
            console.error('获取报告内容块返回数据格式错误:', res);
            this.setData({
              isLoading: false,
              isComplete: true
            });
            wx.showToast({ 
              title: '获取报告内容失败', 
              icon: 'none' 
            });
            this.stopPolling();
          }
        })
        .catch(error => {
          console.error('获取报告内容块失败:', error);
          // 接口调用失败，停止轮询
          this.setData({
            isLoading: false,
            isComplete: true
          });
          wx.showToast({ 
            title: '获取报告内容失败', 
            icon: 'none' 
          });
          this.stopPolling();
        });
    } catch (error) {
      console.error('发起获取报告内容块请求失败:', error);
      // 请求发起失败，停止轮询
      this.setData({
        isLoading: false,
        isComplete: true
      });
      wx.showToast({ 
        title: '获取报告内容失败', 
        icon: 'none' 
      });
      this.stopPolling();
    }
  },

  /**
   * 停止轮询
   */
  stopPolling() {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
  },



  
  /**
   * 保存报告
   */
  saveReport: function() {
    const { sessionId, reportContent } = this.data;
    
    wx.showLoading({ title: '正在保存报告...' });
    
    // 解析报告内容，提取各个部分
    const reportData = this.parseReportContent(reportContent);
    
    // 调用后端API保存报告
    post('/api/interview/save-report', {
      sessionId: sessionId,
      reportData: reportData
    })
      .then(res => {
        wx.hideLoading();
        if (res && res.success) {
          // 保存成功
          this.setData({ isSaved: true });
          wx.showToast({ title: '报告保存成功', icon: 'success' });
          
          // 清空缓存数据
          this.clearReportCache();
        } else {
          // 保存失败
          wx.showToast({ title: res.message || '报告保存失败', icon: 'none' });
        }
      })
      .catch(error => {
        wx.hideLoading();
        console.error('保存报告失败:', error);
        wx.showToast({ title: '报告保存失败', icon: 'none' });
      });
  },
  
  /**
   * 将Markdown格式转换为HTML标签
   * @param {string} text - 原始文本
   * @returns {string} 转换后的HTML文本
   */
  convertMarkdownBold: function(text) {
    if (!text) return '';
    
    let html = text;
    
    // 1. 转换二级标题 ## 标题
    html = html.replace(/^##\s(.*?)$/gm, '<h2 class="markdown-h2">$1</h2>');
    
    // 2. 转换无序列表 - 列表项
    // 先将连续的列表项分组
    const lines = html.split('\n');
    const result = [];
    let inList = false;
    
    lines.forEach(line => {
      if (line.match(/^-\s/)) {
        if (!inList) {
          inList = true;
          result.push('<ul class="markdown-ul">');
        }
        // 移除列表标记并添加列表项
        result.push('<li class="markdown-li">' + line.replace(/^-\s/, '') + '</li>');
      } else {
        if (inList) {
          inList = false;
          result.push('</ul>');
        }
        result.push(line);
      }
    });
    
    // 关闭最后一个未关闭的列表
    if (inList) {
      result.push('</ul>');
    }
    
    html = result.join('\n');
    
    // 3. 转换加粗格式 **text**
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    
    return html;
  },

  /**
   * 解析报告内容
   * @param {string} content - 报告内容
   * @returns {object} 解析后的报告数据
   */
  parseReportContent: function(content) {
    // 解析报告内容，提取各个部分
    const reportData = {
      totalScore: 0,
      overallFeedback: '',
      strengths: [],
      improvements: [],
      techDepthEvaluation: '',
      logicExpressionEvaluation: '',
      communicationEvaluation: '',
      answerDepthEvaluation: '',
      detailedImprovementSuggestions: ''
    };
    
    // 根据DeepSeek prompt的规范，报告结构为：
    // ## 总体评价和总分
    // 内容
    // ## 优势分析
    // - 优势1
    // - 优势2
    // ## 改进点
    // - 改进点1
    // - 改进点2
    // ## 技术深度评价
    // 内容
    // ## 逻辑表达评价
    // 内容
    // ## 沟通表达评价
    // 内容
    // ## 回答深度评价
    // 内容
    // ## 针对候选人的详细改进建议
    // 内容
    
    // 提取总分
    const scoreMatch = content.match(/总分：(\d+\.\d+)/);
    if (scoreMatch) {
      reportData.totalScore = parseFloat(scoreMatch[1]);
    }
    
    // 提取总体评价
    const overallMatch = content.match(/## 总体评价和总分[\s\S]*?(?=## 优势分析)/);
    if (overallMatch) {
      const text = overallMatch[0].replace(/## 总体评价和总分/, '').trim();
      console.log('总体评价和总分原始文本:', text);
      reportData.overallFeedback = this.convertMarkdownBold(text);
      console.log('总体评价和总分转换后的HTML:', reportData.overallFeedback);
    }
    
    // 提取优势
    const strengthsMatch = content.match(/## 优势分析[\s\S]*?(?=## 改进点)/);
    if (strengthsMatch) {
      const strengthsContent = strengthsMatch[0].replace(/## 优势分析/, '').trim();
      reportData.strengths = strengthsContent.split(/^- /gm)
        .filter(line => line.trim())
        .map(line => this.convertMarkdownBold(line.trim()));
    }
    
    // 提取改进点
    const improvementsMatch = content.match(/## 改进点[\s\S]*?(?=## 技术深度评价)/);
    if (improvementsMatch) {
      const improvementsContent = improvementsMatch[0].replace(/## 改进点/, '').trim();
      reportData.improvements = improvementsContent.split(/^- /gm)
        .filter(line => line.trim())
        .map(line => this.convertMarkdownBold(line.trim()));
    }
    
    // 提取技术深度评价
    const techDepthMatch = content.match(/## 技术深度评价[\s\S]*?(?=## 逻辑表达评价)/);
    if (techDepthMatch) {
      console.log('技术深度评价原始文本:', techDepthMatch[0]);
      const text = techDepthMatch[0].replace(/## 技术深度评价/, '').trim();
      reportData.techDepthEvaluation = this.convertMarkdownBold(text);
      console.log('技术深度评价转换后的HTML:', reportData.techDepthEvaluation);
    }
    
    // 提取逻辑表达评价
    const logicExpressionMatch = content.match(/## 逻辑表达评价[\s\S]*?(?=## 沟通表达评价)/);
    if (logicExpressionMatch) {
      const text = logicExpressionMatch[0].replace(/## 逻辑表达评价/, '').trim();
      reportData.logicExpressionEvaluation = this.convertMarkdownBold(text);
    }
    
    // 提取沟通表达评价
    const communicationMatch = content.match(/## 沟通表达评价[\s\S]*?(?=## 回答深度评价)/);
    if (communicationMatch) {
      const text = communicationMatch[0].replace(/## 沟通表达评价/, '').trim();
      reportData.communicationEvaluation = this.convertMarkdownBold(text);
    }
    
    // 提取回答深度评价
    const answerDepthMatch = content.match(/## 回答深度评价[\s\S]*?(?=## 针对候选人的详细改进建议)/);
    if (answerDepthMatch) {
      const text = answerDepthMatch[0].replace(/## 回答深度评价/, '').trim();
      reportData.answerDepthEvaluation = this.convertMarkdownBold(text);
    }
    
    // 提取针对候选人的详细改进建议
    const detailedImprovementMatch = content.match(/## 针对候选人的详细改进建议[\s\S]*/);
    if (detailedImprovementMatch) {
      const text = detailedImprovementMatch[0].replace(/## 针对候选人的详细改进建议/, '').trim();
      reportData.detailedImprovementSuggestions = this.convertMarkdownBold(text);
    }
    
    return reportData;
  },
  
  /**
   * 清空报告缓存
   */
  clearReportCache: function() {
    // 清空页面数据
    this.setData({
      reportContent: '',
      nextContent: ''
    });
    
    // 清空全局缓存（如果有）
    if (getApp().globalData.reportCache) {
      delete getApp().globalData.reportCache;
    }
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})