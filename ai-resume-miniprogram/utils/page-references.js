// 页面引用声明文件
// 用于解决微信开发者工具代码依赖分析警告

const pageReferences = {
  // 明确声明 pages/resume/main/main 页面被使用
  '/pages/resume/main/main': {
    referencedIn: ['app.json', 'tabBar'],
    type: 'tabBar',
    isActive: true
  }
};

// 导出引用信息
module.exports = {
  pageReferences,
  
  // 获取页面引用信息
  getPageReference(pagePath) {
    return pageReferences[pagePath] || null;
  },
  
  // 检查页面是否被引用
  isPageReferenced(pagePath) {
    return !!pageReferences[pagePath];
  }
};