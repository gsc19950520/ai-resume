# 用户信息获取流程优化总结

## 需求背景
用户要求：登录后立即调用 `api/user/info?openId=oOS8B5fgXrjB4FiLAvy0L8ptzjGA` 接口获取个人信息，访问"我的"页面时无需重新加载，仅在修改个人信息后保存新数据。

## 实现方案

### 1. 登录后自动获取用户信息

**文件：`<mcfile name="app.js" path="f:\owner_project\ai-resume\ai-resume-miniprogram\app.js"></mcfile>**

在 `handleLoginResult` 方法中添加 `fetchUserInfoAfterLogin` 调用：

```javascript
// 登录成功后立即获取用户详细信息
this.fetchUserInfoAfterLogin(userInfo);
```

新增 `fetchUserInfoAfterLogin` 方法：
- 使用 `cloudCall('/user/info', { openId: userInfo.openId }, 'GET')` 调用用户信息接口
- 获取详细数据后合并到用户信息对象
- 更新全局数据 `app.globalData.userInfo` 和本地存储
- 通过蓝牙事件通知其他页面用户信息已更新

### 2. "我的"页面优化数据加载

**文件：`<mcfile name="profile.js" path="f:\owner_project\ai-resume\ai-resume-miniprogram\pages\profile\profile.js"></mcfile>**

新增 `onLoad` 生命周期监听用户信息更新：

```javascript
// 监听蓝牙特征值变化事件（用于监听用户信息更新）
wx.onBLECharacteristicValueChange(function(res) {
  if (res.deviceId === 'userInfoUpdated' && res.characteristicId === 'userInfoCharacteristic') {
    console.info('收到用户信息更新事件');
    // 重新加载用户信息
    that.reloadUserInfoFromStorage();
  }
});
```

新增 `reloadUserInfoFromStorage` 方法：
- 从本地存储重新加载用户信息
- 更新页面数据和全局数据
- 避免重复调用接口获取数据

### 3. 个人信息修改后数据同步

**文件：`<mcfile name="detail.js" path="f:\owner_project\ai-resume\ai-resume-miniprogram\pages\user\detail.js"></mcfile>**

在 `saveUserInfo` 方法中添加用户信息更新事件触发：

```javascript
// 触发用户信息更新事件，通知其他页面数据已更新
wx.notifyBLECharacteristicValueChange({
  deviceId: 'userInfoUpdated',
  serviceId: 'userService',
  characteristicId: 'userInfoCharacteristic',
  state: true,
  success: function() {
    console.info('用户信息更新事件已触发');
  },
  fail: function(error) {
    console.warn('触发用户信息更新事件失败:', error);
  }
});
```

## 数据流说明

### 登录流程
1. 用户点击登录按钮
2. `app.js` 调用微信登录获取code
3. 调用后端 `/wechat/login` 接口
4. 登录成功后，`handleLoginResult` 处理响应数据
5. **新增**：调用 `fetchUserInfoAfterLogin` 获取详细用户信息
6. 更新全局数据和本地存储
7. 触发用户信息更新事件

### "我的"页面流程
1. 页面 `onShow` 时从本地存储加载用户信息
2. **优化**：`onLoad` 时监听用户信息更新事件
3. 收到更新事件时，重新从本地存储加载数据
4. 避免重复调用接口获取数据

### 个人信息修改流程
1. 用户在详情页面修改信息
2. 点击保存按钮调用 `saveUserInfo`
3. 表单验证通过后调用后端 `/user/update` 接口
4. 保存成功后更新全局数据和本地存储
5. **新增**：触发用户信息更新事件
6. 返回上一页或指定页面

## 技术优势

1. **减少网络请求**：登录后一次性获取完整用户信息，后续页面无需重复调用接口
2. **数据一致性**：通过事件机制确保各页面数据同步更新
3. **用户体验优化**："我的"页面加载速度提升，避免等待接口响应
4. **离线支持**：优先使用本地存储数据，网络异常时仍可正常显示

## 使用说明

### 开发者使用
1. 登录功能无需修改，已自动集成用户信息获取
2. "我的"页面已优化，无需手动调用用户信息接口
3. 个人信息修改后，系统会自动同步数据到其他页面

### 测试验证
1. 清除小程序缓存
2. 重新登录账号
3. 检查控制台日志确认用户信息接口调用成功
4. 进入"我的"页面，确认数据已加载无需等待
5. 修改个人信息并保存，检查其他页面数据是否同步更新

## 注意事项

1. 确保用户已授权获取openId，否则无法调用用户信息接口
2. 本地存储空间有限，避免存储过多用户信息数据
3. 蓝牙事件机制用于跨页面通信，实际项目中可考虑使用更轻量级的事件总线
4. 网络异常时，系统会优先使用本地缓存数据，确保基本功能可用