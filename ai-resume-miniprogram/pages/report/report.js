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
    renderedContent: '',
    isLoading: true,
    isComplete: false,
    nextContent: '', // 用于流式展示，逐字/逐词添加
    isSaved: false // 报告是否已保存
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 获取路由参数中的sessionId
    const sessionId = options.sessionId || '';
    if (!sessionId) {
      wx.showToast({
        title: '缺少sessionId参数',
        icon: 'none'
      });
      return;
    }

    this.setData({
      sessionId: sessionId
    });

    // 初始化轮询相关变量
    this.lastIndex = -1;
    this.reportId = null;
    this.pollingInterval = null;

    // 开始生成报告
    this.startReportGeneration(sessionId);
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
      // 调用开始生成报告接口，使用查询参数传递sessionId（后端接口已改为GET）
      get(`/api/interview/start-report?sessionId=${sessionId}`)
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
              
              // 实时转换为HTML并更新渲染内容
              const htmlContent = this.markdownToHtml(updatedReportContent);
              this.setData({ renderedContent: htmlContent });
              
              // 更新lastIndex
              this.lastIndex = data.lastIndex || 0;
            }
            
            // 检查报告是否完成
            if (data.completed) {
              this.setData({
                isLoading: false,
                isComplete: true,
                nextContent: ''
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
   * 将Markdown转换为HTML
   * 针对后端返回的简洁Markdown格式进行优化，确保页面展示美观
   * @param {string} markdown - Markdown文本
   * @returns {string} HTML文本
   */
  markdownToHtml(markdown) {
    if (!markdown) return '';
    
    let html = markdown;
    
    // 先处理转义的*，替换为临时标记
    html = html.replace(/\\\*/g, 'TEMP_ESCAPED_ASTERISK');
    
    // 处理二级标题（后端规定仅使用##）
    html = html.replace(/^## (.*$)/gm, '<h2 class="section-title">$1</h2>');
    
    // 处理粗体（仅支持**）
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong class="highlight">$1</strong>');
    
    // 处理无序列表（仅支持-）
    html = html.replace(/^- (.*$)/gm, '<ul class="content-list"><li class="list-item">$1</li></ul>');
    
    // 处理换行
    html = html.replace(/\n/g, '<br>');
    
    // 恢复转义的*，转换为HTML实体
    html = html.replace(/TEMP_ESCAPED_ASTERISK/g, '&#42;');
    
    return html;
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
      improvements: []
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
    
    // 提取总分
    const scoreMatch = content.match(/总分：(\d+\.\d+)/);
    if (scoreMatch) {
      reportData.totalScore = parseFloat(scoreMatch[1]);
    }
    
    // 提取总体评价
    const overallMatch = content.match(/## 总体评价和总分[\s\S]*?(?=## 优势分析)/);
    if (overallMatch) {
      reportData.overallFeedback = overallMatch[0].replace(/## 总体评价和总分/, '').trim();
    }
    
    // 提取优势
    const strengthsMatch = content.match(/## 优势分析[\s\S]*?(?=## 改进点|$)/);
    if (strengthsMatch) {
      const strengthsContent = strengthsMatch[0].replace(/## 优势分析/, '').trim();
      reportData.strengths = strengthsContent.split(/^- /gm)
        .filter(line => line.trim())
        .map(line => line.trim());
    }
    
    // 提取改进点
    const improvementsMatch = content.match(/## 改进点[\s\S]*/);
    if (improvementsMatch) {
      const improvementsContent = improvementsMatch[0].replace(/## 改进点/, '').trim();
      reportData.improvements = improvementsContent.split(/^- /gm)
        .filter(line => line.trim())
        .map(line => line.trim());
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