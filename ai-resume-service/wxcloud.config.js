{
  "type": "run", // 云托管服务类型
  "envId": "your-env-id", // 请替换为您的环境ID
  "serverPath": ".", // 服务源码路径
  "functions": [],
  "triggerRules": [],
  "containerOptions": {
    "minNum": 0,
    "maxNum": 10,
    "cpu": 1,
    "mem": 2,
    "policyType": "cpu",
    "policyThreshold": 60,
    "port": 8080,
    "envParams": {
      "SPRING_PROFILES_ACTIVE": "prod",
      "SERVER_PORT": "8080"
    }
  }
}
