{
  "type": "static", // 部署模式为静态网站
  "envId": "YOUR_ACTUAL_CLOUD_ENV_ID", // 请替换为您实际的微信云托管环境ID
  "client": {
    "sourceDir": ".", // 源码目录
    "outputDir": "dist", // 输出目录，构建后的文件存放位置
    "errorPage": "index.html", // 错误页面
    "ignore": [
      "node_modules",
      ".git",
      ".svn",
      "*.log",
      "*.md",
      ".vscode",
      ".gitignore",
      "project.config.json",
      "project.private.config.json"
    ],
    "uploadRule": {
      "index.html": {
        "cacheControl": "no-cache"
      }
    }
  }
}
