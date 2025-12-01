// pages/report/report.js
const app = getApp();
import { postStream } from '../../utils/request';

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
    nextContent: '' // 用于流式展示，逐字/逐词添加
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

    // 添加实例变量用于累积SSE数据
    if (!this.currentEvent) this.currentEvent = '';
    if (!this.currentData) this.currentData = '';

    // 调用流式接口获取报告内容
    this.loadReportStream(sessionId);
  },

  /**
   * 加载报告数据（流式方式）
   * @param {string} sessionId - 会话ID
   */
  loadReportStream(sessionId) {
    const url = '/api/interview/finish';
    const data = {
      sessionId: sessionId
    };

    // 开始请求前的状态设置
    this.setData({
      isLoading: true,
      nextContent: '', // 清空下一个内容
      reportContent: '' // 清空报告内容，初始化为空字符串以便流式拼接
    });

    try {
      // 使用流式请求获取报告内容
      postStream(url, data, {
        onChunk: (chunk) => {
          // 处理流式数据
          console.log('收到报告流数据:', chunk);
          
          // 解析数据，支持多种格式
          try {
            if (typeof chunk === 'string') {
              // 处理字符串格式数据，每条chunk是SSE的一行
              const trimmedChunk = chunk.trim();
              
              if (trimmedChunk.startsWith('event:')) {
                // 新的event，先处理之前的event和data（如果有）
                if (this.currentEvent === 'report' && this.currentData) {
                  // 累积报告内容（流式展示，逐字/逐词添加）
                  const newNextContent = this.data.nextContent + this.currentData;
                  // 转换为HTML
                  const htmlContent = this.markdownToHtml(newNextContent);
                  this.setData({
                    nextContent: newNextContent,
                    renderedContent: htmlContent
                  });
                  // 重置当前data
                  this.currentData = '';
                }
                // 更新当前event
                this.currentEvent = trimmedChunk.substring(6).trim();
              } else if (trimmedChunk.startsWith('data:')) {
                // 提取data内容，去除前面的'data:'前缀
                const dataContent = trimmedChunk.substring(5).trim();
                // 检查是否是结束标记
                if (dataContent === 'end' && this.currentEvent === 'end') {
                  // 处理完所有数据
                  return;
                }
                // 累积data内容
                this.currentData = dataContent;
              } else if (trimmedChunk === '') {
                // 空行表示当前SSE消息结束，处理累积的event和data
                if (this.currentEvent === 'report' && this.currentData) {
                  // 累积报告内容（流式展示，逐字/逐词添加）
                  this.setData({
                    nextContent: this.data.nextContent + this.currentData
                  });
                  // 重置当前data
                  this.currentData = '';
                } else if (this.currentEvent === 'end') {
                  // 结束信号，将nextContent的值赋给reportContent，然后清空nextContent
                  this.setData({ 
                    isLoading: false,
                    isComplete: true,
                    reportContent: this.data.nextContent,
                    nextContent: ''
                  });
                  // 转换为HTML
                  const htmlContent = this.markdownToHtml(this.data.reportContent);
                  this.setData({
                    renderedContent: htmlContent
                  });
                  // 重置状态
                  this.currentEvent = '';
                  this.currentData = '';
                }
              }
            } else if (typeof chunk === 'object') {
              // 如果是对象格式，直接处理
              if (chunk.event === 'report' && chunk.data) {
                this.setData({
                  nextContent: this.data.nextContent + chunk.data
                });
              } else if (chunk.event === 'end') {
                this.setData({ 
                  isLoading: false,
                  isComplete: true,
                  reportContent: this.data.nextContent,
                  nextContent: ''
                });
                // 转换为HTML
                const htmlContent = this.markdownToHtml(this.data.reportContent);
                this.setData({
                  renderedContent: htmlContent
                });
              }
            }
          } catch (parseError) {
            console.error('解析流式数据失败:', parseError);
          }
        },
        onError: (error) => {
          console.error('获取报告失败:', error);
          this.setData({ isLoading: false, isComplete: true });
          // 显示错误提示
          wx.showToast({
            title: '获取报告失败',
            icon: 'none'
          });
        },
        onComplete: () => {
          console.log('获取报告流式请求完成');
          // 确保即使没有收到end事件，也能正确处理
          this.setData({ 
            isLoading: false,
            isComplete: true,
            reportContent: this.data.nextContent,
            nextContent: ''
          });
          // 转换为HTML
          const htmlContent = this.markdownToHtml(this.data.reportContent);
          this.setData({
            renderedContent: htmlContent
          });
          // 重置状态
          this.currentEvent = '';
          this.currentData = '';
        }
      });
    } catch (error) {
      console.error('发起获取报告请求失败:', error);
      this.setData({ isLoading: false, isComplete: true });
      // 显示错误提示
      wx.showToast({
        title: '获取报告失败',
        icon: 'none'
      });
    }
  },

  /**
   * 将Markdown转换为HTML
   * @param {string} markdown - Markdown文本
   * @returns {string} HTML文本
   */
  markdownToHtml(markdown) {
    let html = markdown;
    
    // 处理标题
    html = html.replace(/^# (.*$)/gm, '<h1>$1</h1>');
    html = html.replace(/^## (.*$)/gm, '<h2>$1</h2>');
    html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>');
    html = html.replace(/^#### (.*$)/gm, '<h4>$1</h4>');
    html = html.replace(/^##### (.*$)/gm, '<h5>$1</h5>');
    html = html.replace(/^###### (.*$)/gm, '<h6>$1</h6>');
    
    // 处理段落
    html = html.replace(/^(?!<h|<ul|<ol|<li|<pre|<blockquote|<p)(.*$)/gm, '<p>$1</p>');
    
    // 处理换行
    html = html.replace(/\n/g, '<br>');
    
    return html;
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