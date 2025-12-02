// pages/report/report.js
const app = getApp();
import { getStream } from '../../utils/request';

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
    const data = { sessionId };

    // 初始化状态
    this.setData({
      isLoading: true,
      nextContent: '',
      reportContent: ''
    });
    this.chunkBuffer = ''; // 临时缓存流数据
    this.flushTimer = null;

    try {
      getStream(url, data, {
        onChunk: (chunk) => {
          try {
            if (typeof chunk === 'string') {
              const trimmedChunk = chunk.trim();

              if (trimmedChunk.startsWith('event:')) {
                // 处理之前累积的 report 内容
                if (this.currentEvent === 'report' && this.currentData) {
                  this.chunkBuffer += this.currentData;
                  this.currentData = '';
                }
                this.currentEvent = trimmedChunk.substring(6).trim();

              } else if (trimmedChunk.startsWith('data:')) {
                const dataContent = trimmedChunk.substring(5).trim();
                if (dataContent === 'end' && this.currentEvent === 'end') {
                  return;
                }
                this.currentData = (this.currentData || '') + dataContent;

              } else if (trimmedChunk === '') {
                // SSE 消息结束
                if (this.currentEvent === 'report' && this.currentData) {
                  this.chunkBuffer += this.currentData;
                  this.currentData = '';
                } else if (this.currentEvent === 'end') {
                  // 完成
                  this.setData({ 
                    isLoading: false,
                    isComplete: true,
                    reportContent: this.data.nextContent + this.chunkBuffer,
                    nextContent: ''
                  });
                  const htmlContent = this.markdownToHtml(this.data.reportContent);
                  this.setData({ renderedContent: htmlContent });

                  this.currentEvent = '';
                  this.currentData = '';
                  this.chunkBuffer = '';
                }
              }

            } else if (typeof chunk === 'object') {
              if (chunk.event === 'report' && chunk.data) {
                this.chunkBuffer += chunk.data;
              } else if (chunk.event === 'end') {
                this.setData({ 
                  isLoading: false,
                  isComplete: true,
                  reportContent: this.data.nextContent + this.chunkBuffer,
                  nextContent: ''
                });
                const htmlContent = this.markdownToHtml(this.data.reportContent);
                this.setData({ renderedContent: htmlContent });

                this.currentEvent = '';
                this.currentData = '';
                this.chunkBuffer = '';
              }
            }

            // 定时 flush chunkBuffer 到 nextContent，减少 setData 次数
            if (!this.flushTimer && this.chunkBuffer) {
              this.flushTimer = setTimeout(() => {
                this.setData({ nextContent: this.data.nextContent + this.chunkBuffer });
                this.chunkBuffer = '';
                this.flushTimer = null;
              }, 50);
            }

          } catch (parseError) {
            console.error('解析流式数据失败:', parseError);
          }
        },

        onError: (error) => {
          console.error('获取报告失败:', error);
          this.setData({ isLoading: false, isComplete: true });
          wx.showToast({ title: '获取报告失败', icon: 'none' });
        },

        onComplete: () => {
          console.log('获取报告流式请求完成');
          if (!this.data.isComplete) {
            this.setData({ 
              isLoading: false,
              isComplete: true,
              reportContent: this.data.nextContent + this.chunkBuffer,
              nextContent: ''
            });
            const htmlContent = this.markdownToHtml(this.data.reportContent);
            this.setData({ renderedContent: htmlContent });

            this.currentEvent = '';
            this.currentData = '';
            this.chunkBuffer = '';
          }
        }
      });

    } catch (error) {
      console.error('发起获取报告请求失败:', error);
      this.setData({ isLoading: false, isComplete: true });
      wx.showToast({ title: '获取报告失败', icon: 'none' });
    }
  },


  /**
   * 将Markdown转换为HTML
   * @param {string} markdown - Markdown文本
   * @returns {string} HTML文本
   */
  markdownToHtml(markdown) {
    if (!markdown) return '';
    
    let html = markdown;
    
    // 处理标题
    html = html.replace(/^# (.*$)/gm, '<h1>$1</h1>');
    html = html.replace(/^## (.*$)/gm, '<h2>$1</h2>');
    html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>');
    
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