# AI简历小程序修改记录

## PDF导出参数修复

### 问题描述
微信小程序在调用后端接口时，由于使用了硬编码的URL `https://your-api-base-url`，该域名不在微信小程序的合法域名列表中，导致请求失败。

### 修复方案
将硬编码URL改为使用云托管服务调用：

1. **修改loadResumeDataFromBackend方法**：将wx.request改为使用app.cloudCall云托管调用
2. **更新apiBaseUrl配置**：将data中的apiBaseUrl从硬编码URL改为空字符串

### 修复代码示例

**修改前：**
```javascript
wx.request({
  url: `${this.data.apiBaseUrl}/api/resume/${resumeId}/full-data`,
  // ...
})
```

**修改后：**
```javascript
const app = getApp();
app.cloudCall(`/api/resume/${resumeId}/full-data`, {}, 'GET')
  .then(res => {
    // 处理响应数据
  })
  .catch(err => {
    // 处理错误
  });
```

### 修复后流程
1. 从URL参数或本地存储获取resumeId
2. 使用云托管调用后端接口获取简历数据
3. 将获取的数据渲染到预览页面
4. 如果获取失败，使用默认数据或缓存数据

### 注意事项
- 确保小程序已配置云托管服务
- 后端接口已正确部署到云托管环境

## 获取最新简历数据接口修复

### 问题描述
原接口 `/api/resume/${resumeId}/full-data` 需要传入具体的resumeId参数，但用户需要获取的是当前用户的最新简历数据，而不是特定ID的简历数据。

### 修复方案
将接口从 `/api/resume/${resumeId}/full-data` 改为 `/api/resume/getLatest`，并添加用户认证信息：

1. **接口路径修改**：从路径参数改为查询参数
2. **请求参数修改**：从resumeId路径参数改为userId查询参数
3. **认证信息添加**：添加Authorization请求头
4. **参数验证增强**：增加用户信息和token验证
5. **数据结构适配**：根据后端接口返回结构更新前端数据映射

### 修复代码示例

**修改前：**
```javascript
// 原接口调用
const url = `/api/resume/${resumeId}/full-data`;
app.cloudCall(url, {}, 'GET')
```

**修改后：**
```javascript
// 新接口调用
const url = '/api/resume/getLatest';
const data = { userId: userInfo.id };
const header = { Authorization: `Bearer ${token}` };
app.cloudCall(url, data, 'GET', header)
```

### 数据结构映射更新
根据后端返回的数据结构，更新前端数据映射：

```javascript
// 保存后端返回的resumeId供PDF下载使用
this.setData({ 
  resumeId: backendData.id,
  resumeData: normalizedData 
});

// 更新previewOptions中的resumeId
const previewOptions = { resumeId: backendData.id };
wx.setStorageSync('previewOptions', previewOptions);
```

### PDF下载方法更新
确保PDF下载时使用从后端获取的最新resumeId：

```javascript
downloadPdf: function() {
  // 优先使用从后端获取的最新resumeId
  let resumeId = this.data.resumeId;
  if (!resumeId) {
    // 如果没有从后端获取到resumeId，尝试从本地存储获取
    const options = wx.getStorageSync('previewOptions') || {};
    resumeId = options.resumeId;
  }
  // ... 后续PDF下载逻辑
}
```

### 修复后流程
1. 从本地存储获取用户信息
2. 验证用户信息和token完整性
3. 调用云托管接口 `/api/resume/getLatest` 获取最新简历数据
4. 保存后端返回的resumeId到页面数据和本地存储
5. 使用获取的数据渲染预览页面
6. PDF下载时优先使用后端返回的resumeId
7. 处理错误情况，使用默认数据或缓存数据

## 数据映射修复

### 问题描述
页面上的姓名、职位、期望薪资和到岗时间字段显示为空

### 问题原因
1. `formatBackendResumeData`方法中数据映射逻辑错误
2. 期望薪资和到岗时间字段在后端返回的根对象中，但被错误映射到了`personalInfo`对象中
3. 姓名字段的映射优先级不正确

### 修复内容
1. **修复`formatBackendResumeData`方法**（`preview.js`第350-380行）：
   - 修正`personalInfo`对象的映射逻辑
   - 确保`expectedSalary`和`startTime`从后端根对象正确映射
   - 优化姓名字段的映射优先级
   - 添加调试日志以验证数据结构

2. **具体映射规则**：
   - `name`: `backendData.userInfo?.name || backendData.name || ''`
   - `jobTitle`: `backendData.jobTitle || backendData.userInfo?.jobTitle || ''`
   - `expectedSalary`: `backendData.expectedSalary || backendData.userInfo?.expectedSalary || ''`
   - `startTime`: `backendData.startTime || backendData.userInfo?.startTime || ''`

### 修复验证
- 添加调试日志输出后端返回的完整数据结构
- 验证关键字段的映射正确性

### 注意事项
- 确保小程序已配置云托管服务
- 后端 `/api/resume/getLatest` 接口已正确部署
- 用户信息和token已正确存储在本地
- PDF下载功能会使用后端返回的最新resumeId参数