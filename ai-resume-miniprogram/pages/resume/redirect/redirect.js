// pages/resume/redirect/redirect.js
Page({
  /**
   * 页面的初始数据
   */
  data: {
    loading: true
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    console.log('简历跳转页面加载');
    this.handleResumeRedirect();
  },

  /**
   * 处理简历跳转逻辑
   */
  handleResumeRedirect: function() {
    console.log('开始处理简历跳转逻辑');
    
    try {
      // 1. 首先检查用户是否登录
      console.log('检查用户登录状态');
      const userInfoStr = wx.getStorageSync('userInfo');
      let userInfo = null;
      
      if (userInfoStr) {
        try {
          userInfo = JSON.parse(userInfoStr);
        } catch (e) {
          console.error('解析用户信息失败:', e);
        }
      }
      
      // 检查全局用户信息（作为备选）
      const app = getApp();
      const globalUserInfo = app && app.globalData && app.globalData.userInfo;
      const isLoggedIn = (userInfo || globalUserInfo) !== null;
      
      console.log('用户登录状态检查:');
      console.log('- 本地用户信息:', userInfo ? '存在' : '不存在');
      console.log('- 全局用户信息:', globalUserInfo ? '存在' : '不存在');
      console.log('- 是否已登录:', isLoggedIn);
      
      // 如果未登录，直接跳转到main页面，由main页面处理登录提示
      if (!isLoggedIn) {
        console.log('用户未登录，跳转到简历主页面');
        wx.reLaunch({
          url: '/pages/resume/main/main',
          success: () => {
            console.log('成功跳转到简历主页面');
          },
          fail: (err) => {
            console.error('跳转到简历主页面失败:', err);
            this.fallbackToMain();
          }
        });
        return;
      }
      
      // 2. 用户已登录，获取本地保存的简历数据
      const resumeData = wx.getStorageSync('resumeData');
      const tempResumeInfo = wx.getStorageSync('tempResumeInfo');
      const resumeId = wx.getStorageSync('resumeId');
      
      console.log('本地简历数据检查:');
      console.log('- resumeData:', resumeData);
      console.log('- tempResumeInfo:', tempResumeInfo);
      console.log('- resumeId:', resumeId);
      console.log('- userInfo.resumeCount:', userInfo ? userInfo.resumeCount : 'undefined');
      console.log('- globalUserInfo.resumeCount:', globalUserInfo ? globalUserInfo.resumeCount : 'undefined');
      
      // 3. 判断是否有简历数据
      let hasResume = false;
      
      // 方法1：优先检查用户信息中的简历计数（最可靠的来源）
      if (userInfo && userInfo.resumeCount !== undefined) {
        hasResume = userInfo.resumeCount > 0;
        console.log('用户信息中简历计数:', userInfo.resumeCount, '，判定', hasResume ? '有' : '无', '简历');
      } else if (globalUserInfo && globalUserInfo.resumeCount !== undefined) {
        hasResume = globalUserInfo.resumeCount > 0;
        console.log('全局用户信息中简历计数:', globalUserInfo.resumeCount, '，判定', hasResume ? '有' : '无', '简历');
      } else {
        console.log('用户信息中没有resumeCount字段，使用本地存储作为备选判断');
        // 方法2：当没有resumeCount信息时，使用本地存储作为备选判断
        const hasValidResumeData = resumeData && typeof resumeData === 'object' && Object.keys(resumeData).length > 0;
        const hasValidTempResume = tempResumeInfo && typeof tempResumeInfo === 'object' && Object.keys(tempResumeInfo).length > 0;
        hasResume = hasValidResumeData || hasValidTempResume;
        console.log('本地存储判断结果:', hasResume ? '检测到有效的本地简历数据' : '未检测到有效简历数据');
      }
      
      console.log('最终判断结果 - 是否有简历:', hasResume);
      
      // 4. 根据判断结果进行跳转
      if (hasResume) {
        console.log('用户已登录且有简历，跳转到预览页面');
        // 使用reLaunch确保清空页面栈，避免返回问题
        wx.reLaunch({
          url: '/pages/template/preview/preview',
          success: () => {
            console.log('成功跳转到预览页面');
          },
          fail: (err) => {
            console.error('跳转到预览页面失败:', err);
            this.fallbackToMain();
          }
        });
      } else {
        console.log('用户已登录但无简历，跳转到主页面');
        // 使用reLaunch确保清空页面栈
        wx.reLaunch({
          url: '/pages/resume/main/main',
          success: () => {
            console.log('成功跳转到主页面');
          },
          fail: (err) => {
            console.error('跳转到主页面失败:', err);
            this.fallbackToMain();
          }
        });
      }
      
    } catch (error) {
      console.error('处理简历跳转逻辑失败:', error);
      this.fallbackToMain();
    }
  },

  /**
   * 降级处理 - 跳转到主页面
   */
  fallbackToMain: function() {
    console.log('执行降级处理，跳转到主页面');
    wx.reLaunch({
      url: '/pages/resume/main/main',
      success: () => {
        console.log('降级跳转成功');
      },
      fail: (err) => {
        console.error('降级跳转也失败:', err);
        // 如果连降级都失败，显示错误提示
        wx.showToast({
          title: '页面跳转失败',
          icon: 'error',
          duration: 2000
        });
      }
    });
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide: function () {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload: function () {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {

  }
})