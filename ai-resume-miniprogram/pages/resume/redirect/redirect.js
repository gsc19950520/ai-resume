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
      // 1. 获取本地保存的简历数据
      const resumeData = wx.getStorageSync('resumeData');
      const tempResumeInfo = wx.getStorageSync('tempResumeInfo');
      const resumeId = wx.getStorageSync('resumeId');
      
      // 2. 获取用户信息（注意：userInfo是JSON字符串，需要解析）
      const userInfoStr = wx.getStorageSync('userInfo');
      let userInfo = null;
      
      if (userInfoStr) {
        try {
          userInfo = JSON.parse(userInfoStr);
        } catch (e) {
          console.error('解析用户信息失败:', e);
        }
      }
      
      console.log('本地简历数据检查:');
      console.log('- resumeData:', resumeData);
      console.log('- tempResumeInfo:', tempResumeInfo);
      console.log('- resumeId:', resumeId);
      console.log('- userInfo字符串:', userInfoStr);
      console.log('- userInfo对象:', userInfo);
      console.log('- userInfo.resumeCount:', userInfo ? userInfo.resumeCount : 'undefined');
      
      // 3. 判断是否有简历数据
      let hasResume = false;
      
      // 方法1：检查本地存储的简历数据
      if (resumeData || tempResumeInfo || resumeId) {
        hasResume = true;
        console.log('检测到本地简历数据');
      }
      
      // 方法2：检查用户信息中的简历计数
      if (userInfo && userInfo.resumeCount !== undefined && userInfo.resumeCount > 0) {
        hasResume = true;
        console.log('用户信息中检测到简历计数:', userInfo.resumeCount);
      } else if (userInfo && userInfo.resumeCount !== undefined) {
        console.log('用户信息中resumeCount为:', userInfo.resumeCount);
      } else {
        console.log('用户信息中没有resumeCount字段');
      }
      
      // 方法3：检查全局用户信息（作为备选）
      const app = getApp();
      if (!hasResume && app && app.globalData && app.globalData.userInfo) {
        const globalUserInfo = app.globalData.userInfo;
        console.log('检查全局用户信息:', globalUserInfo);
        if (globalUserInfo.resumeCount !== undefined && globalUserInfo.resumeCount > 0) {
          hasResume = true;
          console.log('全局用户信息中检测到简历计数:', globalUserInfo.resumeCount);
        }
      }
      
      console.log('最终判断结果 - 是否有简历:', hasResume);
      
      // 4. 根据判断结果进行跳转
      if (hasResume) {
        console.log('有简历，跳转到预览页面');
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
        console.log('无简历，跳转到主页面');
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