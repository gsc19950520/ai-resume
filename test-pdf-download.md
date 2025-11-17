# PDF下载功能改造说明

## 修改内容

我已经修改了 `preview.js` 文件中的 `downloadPdf` 函数，使其调用后端已实现的 `/export/pdf` 接口。

### 主要变更：

1. **URL变更**：
   - 从 `/resume/export/pdf` 或 `/template/{templateId}/generate-pdf` 
   - 改为统一调用 `/export/pdf`

2. **参数调整**：
   - 当存在 `resumeId` 时：同时传递 `resumeId` 和 `templateId`
   - 当不存在 `resumeId` 时：只传递 `templateId`

3. **认证头添加**：
   - 添加了 `Authorization` 头部，从本地存储获取token
   - 使用 `Bearer ${token}` 格式

### 修改后的逻辑：

```javascript
// 构建请求参数和URL - 使用后端已实现的/export/pdf接口
let url = '';
let data = {};

if (resumeId) {
  // 如果有resumeId，调用简历导出接口
  url = `${apiBaseUrl}/export/pdf`;
  data = { 
    resumeId: resumeId,
    templateId: templateId // 后端接口需要templateId参数
  };
} else {
  // 如果没有resumeId，仍然调用export/pdf接口，但需要提供templateId
  url = `${apiBaseUrl}/export/pdf`;
  data = { 
    templateId: templateId // 必须有templateId参数
  };
  console.warn('没有resumeId，将使用模板ID生成PDF，可能无法获取完整的简历数据');
}

// 获取Authorization token
const token = wx.getStorageSync('token');
const header = token ? { 'Authorization': `Bearer ${token}` } : {};
```

## 测试建议

1. **测试场景1**：有resumeId的情况
   - 预期：传递 `resumeId` 和 `templateId` 参数
   - 接口：`GET /export/pdf?resumeId=xxx&templateId=xxx`

2. **测试场景2**：无resumeId的情况
   - 预期：只传递 `templateId` 参数
   - 接口：`GET /export/pdf?templateId=xxx`

3. **认证测试**：
   - 确保本地存储中有有效的token
   - 检查请求头中包含 `Authorization: Bearer xxx`

## 注意事项

- 后端接口 `/export/pdf` 需要 `templateId` 参数（必填）
- `resumeId` 参数是可选的，但如果提供会优先使用
- 确保token在本地存储中可用，否则认证会失败