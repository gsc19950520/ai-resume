{
  "type": "run",
  "envId": "prod-1gwm267i6a10e7cb",
  "serverPath": ".",
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
      "SERVER_PORT": "8080",
      "DEEPSEEK_API_KEY": "sk-3949685c1e23448b96e017f93f1501d5"
    }
  }
}
