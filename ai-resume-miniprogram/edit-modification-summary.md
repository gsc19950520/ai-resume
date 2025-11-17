# AI简历小程序修改记录

## PDF导出参数修复

### 问题描述
preview.js中调用后端接口时使用了硬编码的URL `https://your-api-base-url`，这在微信小程序中不在合法域名列表内，导致请求失败。

### 修复方案
1. **修改`loadResumeDataFromBackend`方法**：
   - 将原来的`wx.request`调用改为使用`app.cloudCall`云托管方式
   - 移除硬编码的URL，改为使用相对路径`/api/resume/${resumeId}/full-data`

2. **更新配置**：
   - 将`apiBaseUrl`从`https://your-api-base-url/api`改为空字符串`''`
   - 更新`getApiBaseUrl`方法，优先使用云托管服务

### 修复代码示例

**修改前：**
```javascript
loadResumeDataFromBackend: function(resumeId) {
  wx.request({
    url: `https://your-api-base-url/api/resume/${resumeId}/full-data`,
    method: 'GET',
    success: (res) => {
      // ...
    }
  });
}
```

**修改后：**
```javascript
loadResumeDataFromBackend: function(resumeId) {
  const app = getApp();
  
  // 使用云托管方式调用后端接口
  app.cloudCall(`/api/resume/${resumeId}/full-data`, {}, 'GET')
    .then(res => {
      if (res && res.success && res.data) {
        console.log('从后端获取简历数据成功:', res.data);
        // 转换后端数据格式为前端需要的格式
        const formattedData = this.formatBackendResumeData(res.data);
        this.setData({
          resumeData: formattedData,
          loading: false
        });
      }
    })
    .catch(err => {
      console.error('请求后端失败:', err);
      this.handleResumeLoadError();
    });
}
```

### 修复后流程
1. 当用户进入预览页面时，系统会优先从URL参数或本地存储获取resumeId
2. 使用云托管方式调用后端接口获取完整简历数据
3. 成功获取数据后，转换为前端需要的格式并渲染页面
4. 如果获取失败，使用默认数据或本地缓存数据

## 获取最新简历数据接口修复

### 问题描述
`loadResumeDataFromBackend`方法原来调用的是`/api/resume/${resumeId}/full-data`接口，现在需要改为调用`/api/resume/getLatest`接口来获取用户最新的简历数据。

### 修复方案
1. **修改接口调用**：从`/api/resume/${resumeId}/full-data`改为`/api/resume/getLatest`
2. **修改请求参数**：从路径参数改为查询参数，传入`userId`
3. **添加认证头**：添加`Authorization`请求头，使用Bearer token认证
4. **增加参数验证**：在调用前验证userInfo和token是否存在

### 修复代码示例

**修改前：**
```javascript
loadResumeDataFromBackend: function(resumeId) {
  const app = getApp();
  
  // 使用云托管方式调用后端接口
  app.cloudCall(`/api/resume/${resumeId}/full-data`, {}, 'GET')
    .then(res => {
      if (res && res.success && res.data) {
        console.log('从后端获取简历数据成功:', res.data);
        // ...
      }
    });
}
```

**修改后：**
```javascript
loadResumeDataFromBackend: function(resumeId) {
  const app = getApp();
  
  // 获取用户信息
  const userInfo = this.data.userInfo;
  if (!userInfo || !userInfo.id) {
    console.error('用户信息不存在，无法获取最新简历数据');
    this.handleResumeLoadError();
    return;
  }
  
  // 获取token
  const token = wx.getStorageSync('token') || '';
  if (!token) {
    console.error('Token不存在，无法获取最新简历数据');
    this.handleResumeLoadError();
    return;
  }
  
  // 使用云托管方式调用后端接口获取用户最新简历数据
  app.cloudCall('/api/resume/getLatest', {
    userId: userInfo.id
  }, 'GET', {
    'Authorization': `Bearer ${token}`
  })
    .then(res => {
      if (res && res.success && res.data) {
        console.log('从后端获取用户最新简历数据成功:', res.data);
        // ...
      }
    });
}
```

### 修复后流程
1. 当用户进入预览页面时，系统会优先从URL参数或本地存储获取resumeId
2. 验证userInfo和token是否存在
3. 使用云托管方式调用`/api/resume/getLatest`接口，传入userId和Authorization头
4. 成功获取数据后，转换为前端需要的格式并渲染页面
5. 如果获取失败，使用默认数据或本地缓存数据

### 注意事项
- 确保小程序已配置云托管服务
- 确保后端接口在云托管环境中正确部署
- 确保userInfo中包含正确的用户ID
- 确保token已正确存储在本地