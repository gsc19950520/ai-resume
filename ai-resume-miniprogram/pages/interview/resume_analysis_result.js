// resume_analysis_result.js
const app = getApp();
import { get, post } from '../../utils/request.js';

Page({
  data: {
    analysisData: null,
    loading: true
  },

  onLoad: function(options) {
    // 从参数中获取分析数据
    if (options.analysisData) {
      try {
        const analysisData = JSON.parse(decodeURIComponent(options.analysisData));
        this.setData({
          analysisData: analysisData,
          loading: false
        });
      } catch (error) {
        console.error('解析分析数据失败:', error);
        this.showError('数据加载失败');
      }
    } else {
      this.showError('未找到分析数据');
    }
  },

  // 显示错误信息
  showError: function(message) {
    this.setData({
      loading: false
    });
    wx.showToast({
      title: message,
      icon: 'none',
      duration: 2000
    });
    setTimeout(() => {
      wx.navigateBack();
    }, 2000);
  },

  // 返回上一页
  goBack: function() {
    wx.navigateBack();
  },

  // 分享分析报告
  shareAnalysis: function() {
    if (!this.data.analysisData) {
      wx.showToast({
        title: '暂无数据可分享',
        icon: 'none'
      });
      return;
    }

    // 生成分享内容
    const shareContent = this.generateShareContent();
    
    wx.showActionSheet({
      itemList: ['复制报告摘要', '生成分享图片', '发送给朋友'],
      success: (res) => {
        switch (res.tapIndex) {
          case 0:
            this.copyToClipboard(shareContent.text);
            break;
          case 1:
            this.generateShareImage();
            break;
          case 2:
            this.shareToFriend(shareContent);
            break;
        }
      }
    });
  },

  // 生成分享内容
  generateShareContent: function() {
    const data = this.data.analysisData;
    const text = `简历分析报告\n\n` +
      `候选人：${data.candidateInfo.name}\n` +
      `工作年限：${data.candidateInfo.workingYears}年\n` +
      `经验级别：${data.candidateInfo.experienceLevel}\n\n` +
      `技术栈亮点：${data.technicalAnalysis.analysis}\n\n` +
      `项目经验：${data.projectAnalysis.totalProjects}个项目，${data.projectAnalysis.highlights}\n\n` +
      `面试建议：${data.summary.suggestions}`;
    
    return {
      text: text,
      title: `${data.candidateInfo.name}的简历分析报告`,
      path: `/pages/interview/resume_analysis_result?analysisData=${encodeURIComponent(JSON.stringify(data))}`
    };
  },

  // 复制到剪贴板
  copyToClipboard: function(text) {
    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({
          title: '已复制到剪贴板',
          icon: 'success'
        });
      }
    });
  },

  // 生成分享图片（简化版）
  generateShareImage: function() {
    wx.showToast({
      title: '分享图片功能开发中',
      icon: 'none'
    });
  },

  // 分享给朋友
  shareToFriend: function(shareContent) {
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },

  // 从分析结果开始面试
  startInterviewFromAnalysis: function() {
    if (!this.data.analysisData) {
      wx.showToast({
        title: '暂无数据',
        icon: 'none'
      });
      return;
    }

    const data = this.data.analysisData;
    
    // 将分析结果存储到全局数据，便于面试页面使用
    app.globalData.resumeAnalysisData = data;
    
    // 跳转到面试页面，携带分析结果
    wx.navigateTo({
      url: `/pages/interview/interview?resumeId=${data.candidateInfo.resumeId}&fromAnalysis=true`
    });
  },

  // 页面分享设置
  onShareAppMessage: function() {
    if (this.data.analysisData) {
      const shareContent = this.generateShareContent();
      return {
        title: shareContent.title,
        path: shareContent.path,
        success: () => {
          wx.showToast({
            title: '分享成功',
            icon: 'success'
          });
        }
      };
    }
    return {
      title: '简历分析报告',
      path: '/pages/interview/resume_analysis_result'
    };
  },

  // 分享到朋友圈
  onShareTimeline: function() {
    if (this.data.analysisData) {
      const data = this.data.analysisData;
      return {
        title: `${data.candidateInfo.name}的简历分析报告`,
        query: `analysisData=${encodeURIComponent(JSON.stringify(data))}`
      };
    }
    return {
      title: '简历分析报告'
    };
  }
});