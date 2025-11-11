// list.js
const app = getApp()
import { get } from '../../../utils/request';

Page({
  data: {
    templateList: [],
    loading: true
  },

  onLoad: function() {
    this.loadTemplateList()
  },

  // 加载模板列表
  loadTemplateList: function() {
    this.setData({ loading: true })
    
    // 调用后端API获取模板列表
    console.log('开始调用模板列表API...');
    get('/api/template/all')
      .then(res => {
        console.log('从后端API获取模板列表成功，响应数据:', res);
        // 确保返回的数据是数组格式
        const templateList = Array.isArray(res) ? res : [];
        
        // 为每个模板添加预览图片路径（如果后端没有提供）
        const enhancedTemplates = templateList.map(template => {
        return {
          id: template.id || `template-${Math.random().toString(36).substr(2, 9)}`,
          name: template.name || '未知模板',
          preview: template.preview || `/images/${template.id || 'default-template'}.png`,
          description: template.description || '暂无描述',
          vipOnly: template.vip_only || false,
          isFree: template.is_free || false
        };
      });
        
        this.setData({
          loading: false,
          templateList: enhancedTemplates
        })
        console.log('从后端API获取模板列表成功:', enhancedTemplates);
      })
      .catch(err => {
        console.error('获取模板列表失败，错误详情:', err);
        console.error('错误类型:', typeof err);
        console.error('错误堆栈:', err.stack || '无堆栈信息');
        this.setData({ loading: false });
        
        // 出错时提供默认模板作为备用
        const defaultTemplates = [
          {
            id: 'template-one', 
            name: '专业简约模板',
            preview: '/images/template-one.png',
            description: '清晰简洁的专业风格，适合各类职位申请',
            vipOnly: false,
            isFree: true
          },
          {
            id: 'template-two',
            name: '创意设计模板',
            preview: '/images/template-two.png',
            description: '富有创意的设计风格模板',
            vipOnly: false,
            isFree: true
          }
        ];
        
        
        this.setData({ templateList: defaultTemplates });
        wx.showToast({
          title: '获取模板失败，显示默认模板',
          icon: 'none'
        });
      })
  },

  // 选择模板
  selectTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id
    const templateName = e.currentTarget.dataset.name
    
    // 保存选择的模板信息
    wx.setStorageSync('tempResumeInfo', {
      title: templateName + '简历',
      templateId: templateId,
      isAiGenerated: false
    })
    
    wx.showToast({
      title: '正在准备模板...',
      icon: 'success'
    })
    
    // 直接跳转到简历编辑页面，减少用户操作步骤
    setTimeout(() => {
      wx.navigateTo({
        url: `/pages/resume/edit/edit?templateId=${templateId}`
      })
    }, 1000)
  },

  // 预览模板
  previewTemplate: function(e) {
    const templateId = e.currentTarget.dataset.id
    const templateName = e.currentTarget.dataset.name
    
    // 跳转到模板预览页面 - 使用模板字符串便于代码依赖分析
    wx.navigateTo({
      url: `/pages/template/preview/preview?templateId=${templateId}&templateName=${encodeURIComponent(templateName)}`
    })
  }
})