# 修复云托管PDF下载功能

## 问题描述
PDF下载功能报错：`request:fail url not in domain list`，错误信息显示 `https://your-api-base-url` 不在微信小程序的合法域名列表中。

## 问题分析

1. **域名配置问题**：`preview.js` 文件中仍然使用默认的API基础URL `https://your-api-base-url/api`
2. **调用方式错误**：使用了传统的 `wx.request` 方式，而不是云托管的 `callContainer` 方法
3. **二进制数据处理**：PDF下载需要处理二进制数据，需要特殊的响应类型配置

## 修复方案

### 1. 更新PDF下载函数 (preview.js)

将原来的 `wx.request` 调用改为使用云托管的 `cloudCallBinary` 方法：

```javascript
downloadPdf: function() {
  const { templateId, resumeData, enableMock } = this.data;
  
  wx.showLoading({
    title: '正在生成PDF...',
    mask: true
  });
  
  const options = wx.getStorageSync('previewOptions') || {};
  const resumeId = options.resumeId;
  const app = getApp();
  
  // 构建请求参数
  let data = {};
  
  if (resumeId) {
    data = { 
      resumeId: resumeId,
      templateId: templateId
    };
  } else {
    data = { 
      templateId: templateId
    };
    console.warn('没有resumeId，将使用模板ID生成PDF');
  }
  
  console.log('调用云托管PDF下载接口:', '/export/pdf', data);
  
  // 使用云托管的cloudCallBinary方法处理二进制数据
  app.cloudCallBinary('/export/pdf', data, 'GET')
    .then(pdfData => {
      console.log('PDF下载请求成功，数据类型:', typeof pdfData, '数据长度:', pdfData ? pdfData.byteLength : 0);
      
      if (pdfData && pdfData.byteLength > 0) {
        // 处理返回的二进制数据
        this.handlePdfData(pdfData);
      } else {
        wx.hideLoading();
        console.warn('后端返回空的PDF数据，切换到模拟模式');
        this.setData({ enableMock: true });
        this.mockPdfDownload();
      }
    })
    .catch(err => {
      wx.hideLoading();
      console.error('PDF下载请求失败，切换到模拟模式:', err);
      this.setData({ enableMock: true });
      this.mockPdfDownload();
    });
}
```

### 2. 添加云托管二进制数据调用方法 (app.js)

新增 `cloudCallBinary` 方法专门处理二进制数据：

```javascript
// 云托管二进制数据调用方法（用于PDF下载等）
cloudCallBinary: function(path, data = {}, method = 'GET', header = {}) {
  return new Promise((resolve, reject) => {
    wx.cloud.callContainer({
      config: {
        env: this.globalData.cloudEnvId
      },
      path: path.startsWith('/api') ? path : `/api${path}`,
      method: method,
      header: {
        'content-type': 'application/json',
        'Authorization': this.globalData.token ? `Bearer ${this.globalData.token}` : '',
        'X-WX-SERVICE': this.globalData.cloudServiceName,
        ...header
      },
      data,
      responseType: 'arraybuffer', // 设置响应类型为arraybuffer以处理二进制数据
      success: res => {
        console.log('云托管二进制调用成功，响应类型:', typeof res.data, '数据长度:', res.data ? res.data.byteLength : 0);
        resolve(res.data);
      },
      fail: err => reject(err)
    });
  });
}
```

### 3. 更新getApiBaseUrl方法 (preview.js)

当使用云托管时返回空字符串：

```javascript
getApiBaseUrl: function() {
  // 使用云托管服务时，不需要具体的API基础URL
  const app = getApp();
  if (app.globalData && app.globalData.useCloud) {
    console.log('使用云托管服务，getApiBaseUrl返回空字符串');
    return '';
  }
  // 优先从全局配置中获取，如果没有则使用data中的默认值
  return app.globalData && app.globalData.apiBaseUrl || this.data.apiBaseUrl;
}
```

## 云托管配置

确保在 `app.js` 中正确配置了云托管参数：

```javascript
globalData: {
  userInfo: null,
  userProfile: null,
  token: '',
  baseUrl: '', // 使用云托管时无需配置具体URL
  cloudBaseUrl: '', // 云托管服务地址，使用callContainer时无需配置
  useCloud: true, // 使用云托管服务（通过callContainer调用）
  cloudEnvId: 'prod-1gwm267i6a10e7cb', // 微信云托管环境ID
  cloudServiceName: 'springboot-bq0e' // 云托管服务名称
}
```

## 测试验证

### 测试步骤：
1. 进入模板预览页面
2. 点击"下载PDF"按钮
3. 观察控制台日志，确认：
   - 是否使用云托管调用
   - 是否正确获取二进制数据
   - PDF生成是否成功

### 预期结果：
- 不再出现域名不在列表中的错误
- 能够成功调用云托管的 `/export/pdf` 接口
- 正确处理返回的二进制PDF数据

## 注意事项

1. **云托管环境**：确保云托管服务已正确部署并运行
2. **接口路径**：确认后端 `/export/pdf` 接口已正确实现
3. **权限配置**：确保小程序已配置云开发权限
4. **调试模式**：如果后端接口有问题，会自动切换到模拟模式

## 优势

1. **无需域名配置**：使用云托管无需配置request合法域名
2. **自动认证**：云托管调用自动携带微信认证信息
3. **二进制支持**：专门的二进制数据处理方法
4. **错误处理**：完善的错误处理和降级机制