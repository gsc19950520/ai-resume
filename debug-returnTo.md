# 个人信息编辑页面跳转问题分析

## 问题描述
从完善个人信息页面跳转到个人编辑页面，保存完成之后没有跳转到指定的页面。

传入参数：`{returnTo: "%2Fpages%2Fresume%2Fedit%2Fedit"}`

## 当前跳转流程分析

### 1. 完善个人信息页面 (complete-profile.js)
```javascript
onLoad: function (options) {
  // 保存returnTo参数，默认为简历编辑页面
  const returnTo = options.returnTo || '/pages/resume/edit/edit';
  this.setData({
    returnTo: returnTo
  });
  // 将returnTo保存到全局，以便在个人信息编辑页面使用
  wx.setStorageSync('returnToAfterCompleteProfile', returnTo);
}
```

### 2. 跳转到个人信息编辑页面
```javascript
goToEditProfile: function() {
  const { returnTo } = this.data;
  wx.navigateTo({
    url: `/pages/user/detail?returnTo=${encodeURIComponent(returnTo)}`,
  });
}
```

### 3. 个人信息编辑页面 (detail.js) 接收参数
```javascript
onLoad: function (options) {
  console.log('个人信息编辑页面加载', options)
  // 优先从options获取returnTo参数
  let returnToPage = options.returnTo || ''
  
  // 如果options中没有returnTo参数，尝试从本地存储获取
  if (!returnToPage) {
    returnToPage = wx.getStorageSync('returnToAfterCompleteProfile') || ''
    // 获取后清除本地存储的参数，避免影响后续操作
    if (returnToPage) {
      wx.removeStorageSync('returnToAfterCompleteProfile')
    }
  }
  
  this.returnToPage = returnToPage
}
```

### 4. 保存完成后的跳转逻辑
```javascript
// 根据是否有返回路径参数决定跳转方式
setTimeout(() => {
  if (this.returnToPage) {
    console.log('跳转到指定返回页面:', this.returnToPage)
    wx.navigateTo({
      url: this.returnToPage
    })
  } else {
    // 没有指定返回页面时，返回上一页
    wx.navigateBack()
  }
}, 1500)
```

## 问题分析

传入的returnTo参数是URL编码格式：`%2Fpages%2Fresume%2Fedit%2Fedit`

解码后应该是：`/pages/resume/edit/edit`

但问题可能出现在：

1. **参数传递问题**：从complete-profile到detail页面的参数传递是否正确
2. **URL编码问题**：是否需要解码returnTo参数
3. **页面路径问题**：返回的路径是否正确

## 解决方案

### 方案1：在detail.js中解码returnTo参数
```javascript
onLoad: function (options) {
  console.log('个人信息编辑页面加载', options)
  // 优先从options获取returnTo参数，并进行URL解码
  let returnToPage = options.returnTo ? decodeURIComponent(options.returnTo) : ''
  
  // 如果options中没有returnTo参数，尝试从本地存储获取
  if (!returnToPage) {
    returnToPage = wx.getStorageSync('returnToAfterCompleteProfile') || ''
    // 获取后清除本地存储的参数，避免影响后续操作
    if (returnToPage) {
      wx.removeStorageSync('returnToAfterCompleteProfile')
    }
  }
  
  this.returnToPage = returnToPage
}
```

### 方案2：检查complete-profile.js的参数编码
确保在传递参数时正确编码：
```javascript
goToEditProfile: function() {
  const { returnTo } = this.data;
  wx.navigateTo({
    url: `/pages/user/detail?returnTo=${encodeURIComponent(returnTo)}`,
  });
}
```

## 建议测试步骤

1. 在complete-profile.js的onLoad中打印接收到的returnTo参数
2. 在goToEditProfile中打印要传递的returnTo参数
3. 在detail.js的onLoad中打印接收到的options.returnTo参数
4. 检查控制台日志，确认参数传递是否正确