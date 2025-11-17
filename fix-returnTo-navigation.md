# 个人信息编辑页面跳转问题修复

## 问题描述
从完善个人信息页面跳转到个人编辑页面，保存完成后没有跳转到指定的页面。

传入参数：`{returnTo: "%2Fpages%2Fresume%2Fedit%2Fedit"}`

## 问题分析

1. **URL编码问题**：传入的returnTo参数是URL编码格式，需要在接收端进行解码
2. **路径格式问题**：需要确保跳转路径格式正确
3. **错误处理不足**：缺少对跳转失败的降级处理

## 修复方案

### 1. 完善个人信息页面 (complete-profile.js)
- 添加了参数接收日志
- 添加了returnTo参数传递日志
- 确保正确编码参数

```javascript
onLoad: function (options) {
  console.log('完善个人信息页面接收参数:', options)
  const returnTo = options.returnTo || '/pages/resume/edit/edit';
  console.log('设置returnTo为:', returnTo)
  // ...
}

goToEditProfile: function() {
  console.log('跳转到个人信息编辑页面');
  const { returnTo } = this.data;
  console.log('准备传递的returnTo参数:', returnTo)
  const encodedReturnTo = encodeURIComponent(returnTo)
  console.log('编码后的returnTo参数:', encodedReturnTo)
  // ...
}
```

### 2. 个人信息编辑页面 (detail.js)
- 添加URL解码功能
- 添加路径格式清理
- 添加跳转失败降级处理
- 添加详细日志

```javascript
onLoad: function (options) {
  console.log('个人信息编辑页面加载', options)
  // 优先从options获取returnTo参数，并进行URL解码
  let returnToPage = options.returnTo ? decodeURIComponent(options.returnTo) : ''
  // ...
  this.returnToPage = returnToPage
  console.log('设置returnToPage为:', this.returnToPage)
}

// 保存成功后的跳转逻辑
setTimeout(() => {
  if (this.returnToPage) {
    console.log('跳转到指定返回页面:', this.returnToPage)
    // 确保路径格式正确，移除可能的多余斜杠
    const cleanPath = this.returnToPage.replace(/^\//, '').replace(/\/$/, '')
    const targetUrl = '/' + cleanPath
    console.log('清理后的跳转路径:', targetUrl)
    
    wx.navigateTo({
      url: targetUrl,
      success: (res) => {
        console.log('跳转成功:', res)
      },
      fail: (err) => {
        console.error('跳转失败:', err)
        // 如果navigateTo失败，尝试使用redirectTo
        wx.redirectTo({
          url: targetUrl,
          success: (res) => {
            console.log('redirectTo跳转成功:', res)
          },
          fail: (err2) => {
            console.error('redirectTo也失败:', err2)
            // 最后降级到返回上一页
            wx.navigateBack()
          }
        })
      }
    })
  } else {
    console.log('没有指定返回页面，返回上一页')
    wx.navigateBack()
  }
}, 1500)
```

## 测试验证

### 测试步骤：
1. 从完善个人信息页面跳转到个人编辑页面
2. 在个人编辑页面填写信息并保存
3. 观察控制台日志，确认：
   - 参数是否正确传递
   - URL解码是否正常
   - 跳转是否成功

### 预期结果：
保存成功后应该跳转到 `/pages/resume/edit/edit` 页面

## 注意事项

1. **日志监控**：添加了详细的控制台日志，便于调试
2. **错误处理**：添加了多级降级处理，确保跳转失败时不会卡住
3. **路径清理**：自动清理路径格式，避免因格式问题导致跳转失败