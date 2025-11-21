// resume_analysis_result.js
const app = getApp();
import { get, post } from '../../utils/request.js';

Page({
  data: {
    analysisData: null,
    loading: true
  },

  onLoad: function(options) {
    
    // 检查是否有analysisData参数
    if (!options.analysisData) {
      console.error('没有收到analysisData参数');
      this.showError('获取分析数据失败');
      return;
    }
    
    try {
      const analysisData = JSON.parse(decodeURIComponent(options.analysisData));
      console.log('解析后的参数数据:', decodeURIComponent(options.analysisData));
      
      // 检查解析后的数据是否有效
      if (!analysisData) {
        console.error('解析后的分析数据为空');
        this.showError('获取分析数据失败');
        return;
      }
      
      // 处理数据，确保展示所需的字段都有值
      const processedData = this.processAnalysisData(analysisData);
      
      this.setData({
        analysisData: processedData,
        loading: false
      });
    } catch (error) {
      console.error('解析分析数据失败:', error);
      this.showError('获取分析数据失败');
    }
  },
  
  // 处理分析数据，确保数据结构完整
  processAnalysisData: function(data) {
    // 深拷贝数据
    const processed = JSON.parse(JSON.stringify(data));
    
    // 确保技术栈分析有techStack字段（用于wxml展示）
    if (!processed.technicalAnalysis.techStack) {
      processed.technicalAnalysis.techStack = [];
      
      // 从secondarySkills构建techStack
      if (processed.technicalAnalysis.secondarySkills && processed.technicalAnalysis.secondarySkills.length > 0) {
        const skillsByCategory = {
          "编程语言": []
        };
        
        processed.technicalAnalysis.secondarySkills.forEach(skill => {
          skillsByCategory["编程语言"].push(skill.name);
        });
        
        // 转换为wxml需要的格式
        Object.keys(skillsByCategory).forEach(category => {
          processed.technicalAnalysis.techStack.push({
            category: category,
            proficiency: this.calculateAverageProficiency(category, processed.technicalAnalysis),
            skills: skillsByCategory[category]
          });
        });
      }
    }
    
    // 确保技术栈分析有analysis字段
    if (!processed.technicalAnalysis.analysis) {
      processed.technicalAnalysis.analysis = this.generateTechnicalAnalysisSummary(processed);
    }
    
    // 确保项目分析有highlights字段
    if (!processed.projectAnalysis.highlights) {
      processed.projectAnalysis.highlights = this.generateProjectHighlights(processed);
    }
    
    // 确保summary有suggestions字段
    if (!processed.summary) {
      processed.summary = {};
    }
    if (!processed.summary.suggestions) {
      processed.summary.suggestions = this.generateInterviewSuggestions(processed);
    }
    
    return processed;
  },
  
  // 计算平均熟练度
  calculateAverageProficiency: function(category, technicalAnalysis) {
    const skillProficiency = technicalAnalysis.skillProficiency;
    if (!skillProficiency) return "未知";
    
    const skills = technicalAnalysis.secondarySkills || [];
    let totalScore = 0;
    let count = 0;
    
    skills.forEach(skill => {
      if (skillProficiency[skill.name]) {
        totalScore += skillProficiency[skill.name];
        count++;
      }
    });
    
    return count > 0 ? (totalScore / count).toFixed(1) : "未知";
  },
  
  // 生成技术分析摘要
  generateTechnicalAnalysisSummary: function(data) {
    const skills = data.technicalAnalysis.secondarySkills || [];
    const mainSkills = skills.slice(0, 3).map(s => s.name).join('、');
    return `${data.candidateInfo.name}主要掌握${mainSkills}等技术，整体技术水平为${data.experienceLevel}，建议加强技术深度和广度的提升。`;
  },
  
  // 生成项目亮点
  generateProjectHighlights: function(data) {
    const projects = data.projectAnalysis.keyProjects || [];
    if (projects.length > 0) {
      return `参与了${projects.length}个项目，主要项目为${projects[0].name}，复杂度${projects[0].complexity}。`;
    }
    return "暂无项目经验";
  },
  
  // 生成面试建议
  generateInterviewSuggestions: function(data) {
    return `基于您的简历分析，建议重点准备以下方面：1) 详细介绍您的${data.candidateInfo.jobTitle}相关技能和项目经验；2) 展示您解决问题的能力；3) 准备面向对象编程相关的理论知识；4) 说明您的团队协作经验。`;
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
      `工作年限：${data.candidateInfo.workYears || 0}年\n` +
      `经验级别：${data.experienceLevel}\n` +
      `总体评分：${data.overallScore || 0}\n\n` +
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
        title: `${data.candidateInfo.name}的简历分析报告 - 总体评分${data.overallScore || 0}分`,
        query: `analysisData=${encodeURIComponent(JSON.stringify(data))}`
      };
    }
    return {
      title: '简历分析报告'
    };
  }
});