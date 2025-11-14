// pages/profile/complete-profile/complete-profile.js
Page({
  /**
   * 页面的初始数据
   */
  data: {
    returnTo: '' // 保存需要返回的页面路径
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    console.log('加载完善个人信息提示页面', options);
    // 保存returnTo参数，默认为简历编辑页面
    const returnTo = options.returnTo || '/pages/resume/edit/edit';
    this.setData({
      returnTo: returnTo
    });
    // 将returnTo保存到全局，以便在个人信息编辑页面使用
    wx.setStorageSync('returnToAfterCompleteProfile', returnTo);
  },

  /**
   * 跳转到个人信息编辑页面
   */
  goToEditProfile: function() {
    console.log('跳转到个人信息编辑页面');
    const { returnTo } = this.data;
    wx.navigateTo({
      url: `/pages/user/detail?returnTo=${encodeURIComponent(returnTo)}`,
      success: function(res) {
        console.log('跳转到个人信息编辑页面成功');
      },
      fail: function(error) {
        console.error('跳转到个人信息编辑页面失败:', error);
      }
    });
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {
    return {
      title: '完善个人信息',
      path: '/pages/profile/complete-profile/complete-profile'
    }
  }
})
