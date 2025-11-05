{
  "type": "static", // 部署模式为静态网站
  "envId": "prod-1gwm267i6a10e7cb", // 微信云托管环境ID
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
